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
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.cms.workflow.RuntimeService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for deleting workflow instances.
 * Endpoint: /bin/workflow/instance/delete
 */
@Component(
        service = Servlet.class,
        property = {"sling.servlet.paths=/bin/workflow/instance/delete", "sling.servlet.methods=POST"})
public class WorkflowInstanceDeleteServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(WorkflowInstanceDeleteServlet.class);

    @Reference
    private transient RuntimeService runtimeService;

    private final Gson gson = new Gson();

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String instanceId = request.getParameter("instanceId");
        String reason = request.getParameter("reason");

        if (instanceId == null || instanceId.isEmpty()) {
            sendError(response, 400, "Missing required parameter: instanceId");
            return;
        }

        if (reason == null || reason.isEmpty()) {
            reason = "Deleted by user";
        }

        try {
            // Set resolver context for RuntimeService
            runtimeService.setResolverContext(request.getResourceResolver());

            runtimeService.deleteProcessInstance(instanceId, reason);
            log.info("Deleted workflow instance: {} (reason: {})", instanceId, reason);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Workflow instance deleted successfully");
            responseData.put("instanceId", instanceId);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(responseData));

        } catch (Exception e) {
            log.error("Failed to delete workflow instance: {}", instanceId, e);
            sendError(response, 500, "Failed to delete workflow instance: " + e.getMessage());
        }
    }

    private void sendError(SlingHttpServletResponse response, int status, String message) throws IOException {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("success", false);
        errorData.put("message", message);

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(gson.toJson(errorData));
    }
}
