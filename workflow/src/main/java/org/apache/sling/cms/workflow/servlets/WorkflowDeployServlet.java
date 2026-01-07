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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to deploy BPMN workflow definitions from uploaded files to JCR.
 *
 * <p>POST: Deploy/update a workflow definition
 * <ul>
 *   <li>Reads BPMN file from request body or file parameter</li>
 *   <li>Extracts process key from BPMN</li>
 *   <li>Creates/updates node at /etc/workflow/definitions/{processKey}</li>
 *   <li>Stores BPMN content and metadata</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * curl -u admin:admin -X POST \
 *   -F "bpmn=@publishingWorkflow.bpmn" \
 *   http://localhost:8082/bin/workflow/deploy
 * </pre>
 */
@Component(
        service = Servlet.class,
        property = {"sling.servlet.paths=/bin/workflow/deploy", "sling.servlet.methods=POST"})
public class WorkflowDeployServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(WorkflowDeployServlet.class);

    private static final String DEFINITIONS_PATH = "/etc/workflow/definitions";

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        ResourceResolver resolver = request.getResourceResolver();
        String bpmnContent = null;
        String processKey = null;

        // Try to get BPMN from file upload parameter
        if (request.getRequestParameter("bpmn") != null) {
            try (InputStream is = request.getRequestParameter("bpmn").getInputStream()) {
                bpmnContent = IOUtils.toString(is, StandardCharsets.UTF_8);
            }
        } else {
            // Try to read from request body
            bpmnContent = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        }

        if (bpmnContent == null || bpmnContent.trim().isEmpty()) {
            sendError(response, SlingHttpServletResponse.SC_BAD_REQUEST, "No BPMN content provided");
            return;
        }

        // Extract process key from BPMN
        processKey = extractProcessKey(bpmnContent);
        if (processKey == null) {
            sendError(response, SlingHttpServletResponse.SC_BAD_REQUEST, "Could not extract process key from BPMN");
            return;
        }

        try {
            // Ensure definitions path exists
            ensureDefinitionsPath(resolver);

            // Create or update the definition node
            String definitionPath = DEFINITIONS_PATH + "/" + processKey;
            Resource definitionResource = resolver.getResource(definitionPath);

            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                throw new ServletException("Could not get JCR session");
            }

            Node definitionNode;
            if (definitionResource == null) {
                // Create new node
                Node parentNode = session.getNode(DEFINITIONS_PATH);
                definitionNode = parentNode.addNode(processKey, "nt:unstructured");
                log.info("Creating new workflow definition: {}", processKey);
            } else {
                // Update existing node
                definitionNode = definitionResource.adaptTo(Node.class);
                if (definitionNode == null) {
                    throw new ServletException("Could not adapt resource to node");
                }
                log.info("Updating existing workflow definition: {}", processKey);
            }

            // Set properties
            definitionNode.setProperty("processDefinitionKey", processKey);
            definitionNode.setProperty("bpmn", bpmnContent);
            definitionNode.setProperty("deploymentDate", Calendar.getInstance());

            // Extract name and version if available
            String name = extractProcessName(bpmnContent);
            if (name != null) {
                definitionNode.setProperty("name", name);
            }

            // Save changes
            session.save();

            log.info("Successfully deployed workflow definition: {}", processKey);

            // Send success response
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter()
                    .write(String.format(
                            "{\"success\":true,\"processKey\":\"%s\",\"path\":\"%s\",\"message\":\"Workflow definition deployed successfully\"}",
                            processKey, definitionPath));

        } catch (RepositoryException e) {
            log.error("Failed to deploy workflow definition: {}", processKey, e);
            sendError(
                    response,
                    SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to deploy workflow: " + e.getMessage());
        }
    }

    /**
     * Ensure the /etc/workflow/definitions path exists.
     */
    private void ensureDefinitionsPath(ResourceResolver resolver) throws RepositoryException {
        Session session = resolver.adaptTo(Session.class);
        if (session == null) {
            throw new RepositoryException("Could not get JCR session");
        }

        if (!session.nodeExists(DEFINITIONS_PATH)) {
            Node etcNode = ensureNodeExists(session, "/etc", "sling:Folder");
            Node workflowNode = ensureNodeExists(session, "/etc/workflow", "sling:Folder");
            ensureNodeExists(session, DEFINITIONS_PATH, "sling:Folder");
            session.save();
            log.info("Created workflow definitions path: {}", DEFINITIONS_PATH);
        }
    }

    /**
     * Ensure a node exists at the given path.
     */
    private Node ensureNodeExists(Session session, String path, String nodeType) throws RepositoryException {
        if (session.nodeExists(path)) {
            return session.getNode(path);
        }

        String parentPath = path.substring(0, path.lastIndexOf('/'));
        if (parentPath.isEmpty()) {
            parentPath = "/";
        }

        Node parent = session.nodeExists(parentPath) ? session.getNode(parentPath) : session.getRootNode();

        String nodeName = path.substring(path.lastIndexOf('/') + 1);
        return parent.addNode(nodeName, nodeType);
    }

    /**
     * Extract process key from BPMN XML.
     */
    private String extractProcessKey(String bpmnContent) {
        // Look for <bpmn2:process id="...">
        String pattern = "<bpmn2:process\\s+id=\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(bpmnContent);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * Extract process name from BPMN XML.
     */
    private String extractProcessName(String bpmnContent) {
        // Look for <bpmn2:process ... name="...">
        String pattern = "<bpmn2:process[^>]+name=\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(bpmnContent);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private void sendError(SlingHttpServletResponse response, int status, String message) throws IOException {
        response.setContentType("application/json");
        response.setStatus(status);
        response.getWriter().write("{\"error\":\"" + message.replace("\"", "\\\"") + "\"}");
    }
}
