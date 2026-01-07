/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.cms.workflow.servlets;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.cms.workflow.RepositoryService;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for workflow designer operations: save, load, deploy, and delete workflow designs.
 *
 * <p>Security:</p>
 * <ul>
 *   <li>Authentication: Requires authenticated user (configured in SlingAuthenticator)</li>
 *   <li>Authorization: Uses JCR permissions on /etc/workflow paths</li>
 *   <li>CSRF Protection: Validates Referer header for POST requests</li>
 * </ul>
 */
@Component(
        service = Servlet.class,
        property = {
            "sling.servlet.methods=GET",
            "sling.servlet.methods=POST",
            "sling.servlet.paths=/bin/workflow/designer",
            "sling.servlet.extensions=json"
        })
public class WorkflowDesignerServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(WorkflowDesignerServlet.class);

    private static final String DESIGNS_PATH = "/etc/workflow/designs";

    @Reference
    private RepositoryService repositoryService;

    @Override
    protected void doPost(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
            throws ServletException, IOException {

        // Basic CSRF protection - validate Referer header
        if (!isValidReferer(request)) {
            log.warn("Rejected request with invalid referer from: {}", request.getRemoteAddr());
            response.sendError(
                    SlingHttpServletResponse.SC_FORBIDDEN,
                    "Invalid referer. This request appears to be a CSRF attack.");
            return;
        }

        String operation = request.getParameter("operation");

        if ("save".equals(operation)) {
            saveWorkflow(request, response);
        } else if ("deploy".equals(operation)) {
            deployWorkflow(request, response);
        } else if ("delete".equals(operation)) {
            deleteWorkflow(request, response);
        } else {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Unknown operation: " + operation);
        }
    }

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
            throws ServletException, IOException {

        String operation = request.getParameter("operation");

        if ("load".equals(operation)) {
            loadWorkflow(request, response);
        } else if ("list".equals(operation)) {
            listWorkflows(request, response);
        } else {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Unknown operation: " + operation);
        }
    }

    /**
     * Basic CSRF protection by validating the Referer header.
     * Checks that the request comes from the same origin.
     */
    private boolean isValidReferer(SlingHttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isEmpty()) {
            // Allow requests without referer for now (can be made stricter in production)
            log.debug("Request without Referer header");
            return true;
        }

        // Check if referer starts with the request's scheme and host
        String expectedOrigin = request.getScheme() + "://" + request.getServerName();
        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            expectedOrigin += ":" + request.getServerPort();
        }

        boolean valid = referer.startsWith(expectedOrigin);
        if (!valid) {
            log.warn("Invalid referer: {} (expected: {})", referer, expectedOrigin);
        }
        return valid;
    }

    private void saveWorkflow(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String name = request.getParameter("name");
        String key = request.getParameter("key");
        String bpmn = request.getParameter("bpmn");

        if (name == null || key == null || bpmn == null) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing required parameters: name, key, bpmn");
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();
        Session session = resolver.adaptTo(Session.class);

        if (session == null) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Unable to get JCR session");
            return;
        }

        try {
            // Ensure designs path exists
            if (!session.nodeExists(DESIGNS_PATH)) {
                Node root = session.getRootNode();
                Node etc = root.hasNode("etc") ? root.getNode("etc") : root.addNode("etc", "nt:folder");
                Node workflow =
                        etc.hasNode("workflow") ? etc.getNode("workflow") : etc.addNode("workflow", "nt:folder");
                workflow.addNode("designs", "nt:folder");
                session.save();
            }

            Node designsNode = session.getNode(DESIGNS_PATH);

            // Create or update workflow design node
            Node workflowNode;
            if (designsNode.hasNode(key)) {
                workflowNode = designsNode.getNode(key);
            } else {
                workflowNode = designsNode.addNode(key, "nt:unstructured");
            }

            workflowNode.setProperty("name", name);
            workflowNode.setProperty("key", key);
            workflowNode.setProperty("bpmn", bpmn);
            workflowNode.setProperty("lastModified", Calendar.getInstance());

            session.save();

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\": true, \"message\": \"Workflow saved successfully\"}");

            log.info("Saved workflow design: {} ({})", name, key);

        } catch (javax.jcr.AccessDeniedException e) {
            log.warn("Access denied saving workflow design for user: {}", resolver.getUserID(), e);
            response.setStatus(SlingHttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter()
                    .write(
                            "{\"success\": false, \"message\": \"Access denied. You don't have permission to save workflow designs.\"}");
        } catch (RepositoryException e) {
            log.error("Error saving workflow design", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"success\": false, \"message\": \"Error saving workflow: " + escapeJson(e.getMessage())
                            + "\"}");
        }
    }

    private void loadWorkflow(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String key = request.getParameter("key");

        if (key == null) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing required parameter: key");
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();
        Session session = resolver.adaptTo(Session.class);

        if (session == null) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Unable to get JCR session");
            return;
        }

        try {
            String nodePath = DESIGNS_PATH + "/" + key;

            if (!session.nodeExists(nodePath)) {
                response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json");
                response.getWriter()
                        .write("{\"success\": false, \"message\": \"Workflow not found: " + escapeJson(key) + "\"}");
                return;
            }

            Node workflowNode = session.getNode(nodePath);

            String name = workflowNode.getProperty("name").getString();
            String bpmn = workflowNode.getProperty("bpmn").getString();

            // Build JSON response manually
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"success\": true,");
            json.append("\"name\":\"").append(escapeJson(name)).append("\",");
            json.append("\"key\":\"").append(escapeJson(key)).append("\",");
            json.append("\"bpmn\":\"").append(escapeJson(bpmn)).append("\"");
            json.append("}");

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter writer = response.getWriter();
            writer.write(json.toString());

            log.info("Loaded workflow design: {} ({})", name, key);

        } catch (javax.jcr.AccessDeniedException e) {
            log.warn("Access denied loading workflow design for user: {}", resolver.getUserID(), e);
            response.setStatus(SlingHttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter()
                    .write(
                            "{\"success\": false, \"message\": \"Access denied. You don't have permission to load workflow designs.\"}");
        } catch (RepositoryException e) {
            log.error("Error loading workflow design", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"success\": false, \"message\": \"Error loading workflow: " + escapeJson(e.getMessage())
                            + "\"}");
        }
    }

    private void deployWorkflow(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String name = request.getParameter("name");
        String key = request.getParameter("key");
        String bpmn = request.getParameter("bpmn");

        if (name == null || key == null || bpmn == null) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing required parameters: name, key, bpmn");
            return;
        }

        try {
            // Deploy to workflow engine
            InputStream bpmnStream = new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8));
            repositoryService.deploy(name, bpmnStream);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\": true, \"message\": \"Workflow deployed successfully\"}");

            log.info("Deployed workflow: {} ({})", name, key);

        } catch (Exception e) {
            log.error("Error deploying workflow", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error deploying workflow: " + e.getMessage());
        }
    }

    private void deleteWorkflow(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String key = request.getParameter("key");

        if (key == null) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing required parameter: key");
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();
        Session session = resolver.adaptTo(Session.class);

        if (session == null) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Unable to get JCR session");
            return;
        }

        try {
            String nodePath = DESIGNS_PATH + "/" + key;

            if (!session.nodeExists(nodePath)) {
                response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json");
                response.getWriter()
                        .write("{\"success\": false, \"message\": \"Workflow not found: " + escapeJson(key) + "\"}");
                return;
            }

            Node workflowNode = session.getNode(nodePath);
            workflowNode.remove();
            session.save();

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\": true, \"message\": \"Workflow deleted successfully\"}");

            log.info("Deleted workflow design: {}", key);

        } catch (javax.jcr.AccessDeniedException e) {
            log.warn("Access denied deleting workflow design for user: {}", resolver.getUserID(), e);
            response.setStatus(SlingHttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter()
                    .write(
                            "{\"success\": false, \"message\": \"Access denied. You don't have permission to delete workflow designs.\"}");
        } catch (RepositoryException e) {
            log.error("Error deleting workflow design", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"success\": false, \"message\": \"Error deleting workflow: " + escapeJson(e.getMessage())
                            + "\"}");
        }
    }

    /**
     * Escape special characters for JSON strings.
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * List all workflow designs.
     */
    private void listWorkflows(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        ResourceResolver resolver = request.getResourceResolver();
        Session session = resolver.adaptTo(Session.class);

        if (session == null) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\": false, \"message\": \"Unable to get JCR session\"}");
            return;
        }

        try {
            if (!session.nodeExists(DESIGNS_PATH)) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"workflows\": []}");
                return;
            }

            Node designsNode = session.getNode(DESIGNS_PATH);
            javax.jcr.NodeIterator nodes = designsNode.getNodes();

            StringBuilder json = new StringBuilder();
            json.append("{\"workflows\": [");

            boolean first = true;
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                if (node.hasProperty("name") && node.hasProperty("key")) {
                    if (!first) {
                        json.append(",");
                    }
                    json.append("{");
                    json.append("\"key\":\"")
                            .append(escapeJson(node.getProperty("key").getString()))
                            .append("\",");
                    json.append("\"name\":\"")
                            .append(escapeJson(node.getProperty("name").getString()))
                            .append("\"");
                    if (node.hasProperty("lastModified")) {
                        json.append(",\"lastModified\":\"")
                                .append(escapeJson(
                                        node.getProperty("lastModified").getString()))
                                .append("\"");
                    }
                    json.append("}");
                    first = false;
                }
            }

            json.append("]}");

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(json.toString());

            log.debug("Listed workflow designs");

        } catch (javax.jcr.AccessDeniedException e) {
            log.warn("Access denied listing workflow designs for user: {}", resolver.getUserID(), e);
            response.setStatus(SlingHttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter()
                    .write(
                            "{\"success\": false, \"message\": \"Access denied. You don't have permission to list workflow designs.\"}");
        } catch (RepositoryException e) {
            log.error("Error listing workflow designs", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"success\": false, \"message\": \"Error listing workflows: " + escapeJson(e.getMessage())
                            + "\"}");
        }
    }
}
