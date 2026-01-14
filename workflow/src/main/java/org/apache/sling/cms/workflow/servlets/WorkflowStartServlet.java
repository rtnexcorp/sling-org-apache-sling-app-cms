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

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.cms.workflow.ProcessInstance;
import org.apache.sling.cms.workflow.RuntimeService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to start a workflow instance.
 *
 * GET: Redirects to the workflow start form page
 * POST: Starts the workflow and returns JSON response or redirects
 */
@Component(
        service = Servlet.class,
        property = {"sling.servlet.paths=/bin/workflow/start", "sling.servlet.methods=GET", "sling.servlet.methods=POST"
        })
public class WorkflowStartServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(WorkflowStartServlet.class);

    private static final String PARAM_PROCESS_KEY = "processKey";
    private static final String PARAM_BUSINESS_KEY = "businessKey";
    private static final String PARAM_CONTENT_PATH = "contentPath";
    private static final String PARAM_VAR_PREFIX = "var_";

    @Reference
    private transient RuntimeService runtimeService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String processKey = request.getParameter(PARAM_PROCESS_KEY);

        // Redirect to the workflow start form page with processKey pre-selected
        String redirectUrl = "/cms/workflow/start.html";
        if (processKey != null && !processKey.isEmpty()) {
            redirectUrl += "?processKey=" + processKey;
        }

        response.sendRedirect(redirectUrl);
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String processKey = request.getParameter(PARAM_PROCESS_KEY);
        String businessKey = request.getParameter(PARAM_BUSINESS_KEY);
        String contentPath = request.getParameter(PARAM_CONTENT_PATH);

        // Use contentPath as businessKey if businessKey is not provided
        if ((businessKey == null || businessKey.isEmpty()) && contentPath != null && !contentPath.isEmpty()) {
            businessKey = contentPath;
        }

        if (processKey == null || processKey.isEmpty()) {
            sendError(response, SlingHttpServletResponse.SC_BAD_REQUEST, "Missing processKey parameter");
            return;
        }

        try {
            // Set resolver context for RuntimeService
            runtimeService.setResolverContext(request.getResourceResolver());

            // Collect process variables from request parameters
            Map<String, Object> variables = collectVariables(request);

            // Add initiator information
            String userId = request.getResourceResolver().getUserID();
            variables.put("initiator", userId);

            ProcessInstance instance;
            if (businessKey != null && !businessKey.isEmpty()) {
                instance = runtimeService.startProcessInstanceByKey(processKey, businessKey, variables);
            } else {
                instance = runtimeService.startProcessInstanceByKey(processKey, variables);
            }

            log.info(
                    "Workflow started: processKey={}, instanceId={}, businessKey={}, initiator={}",
                    processKey,
                    instance.getId(),
                    businessKey,
                    userId);

            // Check if JSON response is requested
            String accept = request.getHeader("Accept");
            if (accept != null && accept.contains("application/json")) {
                sendJsonResponse(response, instance);
            } else {
                // Redirect to workflow instance monitor or back to definitions
                String redirect = request.getParameter("redirect");
                if (redirect != null && !redirect.isEmpty()) {
                    response.sendRedirect(redirect);
                } else {
                    response.sendRedirect("/cms/workflow/instances.html?started=" + instance.getId());
                }
            }

        } catch (Exception e) {
            log.error("Error starting workflow: processKey={}", processKey, e);
            sendError(
                    response,
                    SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error starting workflow: " + e.getMessage());
        } finally {
            runtimeService.clearResolverContext();
        }
    }

    /**
     * Collect process variables from request parameters.
     * Parameters prefixed with "var_" are treated as process variables.
     * Additionally, "contentPath" and "deep" are always included as variables if provided.
     */
    private Map<String, Object> collectVariables(SlingHttpServletRequest request) {
        Map<String, Object> variables = new HashMap<>();

        // Always include contentPath if provided
        String contentPath = request.getParameter(PARAM_CONTENT_PATH);
        if (contentPath != null && !contentPath.isEmpty()) {
            variables.put("contentPath", contentPath);
        }

        // Include deep flag for recursive publishing
        String deep = request.getParameter("deep");
        if (deep != null && !deep.isEmpty()) {
            variables.put("deep", "true".equalsIgnoreCase(deep));
        }

        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            if (paramName.startsWith(PARAM_VAR_PREFIX)) {
                String varName = paramName.substring(PARAM_VAR_PREFIX.length());
                String value = request.getParameter(paramName);
                if (value != null && !value.isEmpty()) {
                    variables.put(varName, value);
                }
            }
        }

        return variables;
    }

    private void sendError(SlingHttpServletResponse response, int status, String message) throws IOException {
        response.setContentType("application/json");
        response.setStatus(status);
        response.getWriter().write("{\"error\":\"" + message.replace("\"", "\\\"") + "\"}");
    }

    private void sendJsonResponse(SlingHttpServletResponse response, ProcessInstance instance) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter()
                .write(String.format(
                        "{\"success\":true,\"instanceId\":\"%s\",\"processDefinitionId\":\"%s\",\"businessKey\":\"%s\"}",
                        instance.getId(),
                        instance.getProcessDefinitionId(),
                        instance.getBusinessKey() != null ? instance.getBusinessKey() : ""));
    }
}
