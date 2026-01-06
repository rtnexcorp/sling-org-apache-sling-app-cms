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
 */
@Component(
        service = Servlet.class,
        property = {
            "sling.servlet.methods=GET",
            "sling.servlet.methods=POST",
            "sling.servlet.paths=/bin/workflow/designer/save",
            "sling.servlet.paths=/bin/workflow/designer/load",
            "sling.servlet.paths=/bin/workflow/designer/deploy",
            "sling.servlet.paths=/bin/workflow/designer/delete"
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

        String path = request.getPathInfo();

        if (path.endsWith("/save")) {
            saveWorkflow(request, response);
        } else if (path.endsWith("/deploy")) {
            deployWorkflow(request, response);
        } else if (path.endsWith("/delete")) {
            deleteWorkflow(request, response);
        } else {
            response.sendError(SlingHttpServletResponse.SC_NOT_FOUND, "Unknown operation");
        }
    }

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getPathInfo();

        if (path.endsWith("/load")) {
            loadWorkflow(request, response);
        } else {
            response.sendError(SlingHttpServletResponse.SC_NOT_FOUND, "Unknown operation");
        }
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

        } catch (RepositoryException e) {
            log.error("Error saving workflow design", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error saving workflow: " + e.getMessage());
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
                response.getWriter().write("Workflow not found: " + key);
                return;
            }

            Node workflowNode = session.getNode(nodePath);

            String name = workflowNode.getProperty("name").getString();
            String bpmn = workflowNode.getProperty("bpmn").getString();

            // Build JSON response manually
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"name\":\"").append(escapeJson(name)).append("\",");
            json.append("\"key\":\"").append(escapeJson(key)).append("\",");
            json.append("\"bpmn\":\"").append(escapeJson(bpmn)).append("\"");
            json.append("}");

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter writer = response.getWriter();
            writer.write(json.toString());

            log.info("Loaded workflow design: {} ({})", name, key);

        } catch (RepositoryException e) {
            log.error("Error loading workflow design", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error loading workflow: " + e.getMessage());
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
                response.getWriter().write("Workflow not found: " + key);
                return;
            }

            Node workflowNode = session.getNode(nodePath);
            workflowNode.remove();
            session.save();

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\": true, \"message\": \"Workflow deleted successfully\"}");

            log.info("Deleted workflow design: {}", key);

        } catch (RepositoryException e) {
            log.error("Error deleting workflow design", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error deleting workflow: " + e.getMessage());
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
}
