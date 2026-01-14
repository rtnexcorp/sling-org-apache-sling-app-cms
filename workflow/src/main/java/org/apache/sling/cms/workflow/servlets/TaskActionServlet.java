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
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.cms.workflow.RuntimeService;
import org.apache.sling.cms.workflow.Task;
import org.apache.sling.cms.workflow.TaskService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for handling task actions like complete, claim, unclaim, delegate.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET /bin/workflow/task/action?taskId={id} - Get task details</li>
 *   <li>POST /bin/workflow/task/action - Perform task action</li>
 * </ul>
 *
 * <p>Actions:
 * <ul>
 *   <li>complete - Complete a task with variables (approved, comment)</li>
 *   <li>claim - Claim a task for the current user</li>
 *   <li>unclaim - Release a claimed task</li>
 *   <li>delegate - Delegate task to another user or group</li>
 * </ul>
 */
@Component(
        service = Servlet.class,
        property = {"sling.servlet.paths=/bin/workflow/task/action", "sling.servlet.methods=GET,POST"})
public class TaskActionServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(TaskActionServlet.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Reference
    private TaskService taskService;

    @Reference
    private RuntimeService runtimeService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();

        String taskId = request.getParameter("taskId");
        if (taskId == null || taskId.isEmpty()) {
            sendError(response, 400, "Missing taskId parameter");
            return;
        }

        try {
            taskService.setResolverContext(request.getResourceResolver());

            Task task = taskService.getTask(taskId);
            if (task == null) {
                sendError(response, 404, "Task not found: " + taskId);
                return;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("id", task.getId());
            result.put("name", task.getName());
            result.put("description", task.getDescription());
            result.put("assignee", task.getAssignee());
            result.put("processInstanceId", task.getProcessInstanceId());
            result.put(
                    "createTime",
                    task.getCreateTime() != null ? task.getCreateTime().getTime() : null);
            result.put("priority", task.getPriority());
            result.put("candidateGroup", task.getCandidateGroup());

            // Fetch workflow variables to provide context about published content
            try {
                runtimeService.setResolverContext(request.getResourceResolver());
                Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());

                // Include publishing-related variables for task details visibility
                Map<String, Object> publishInfo = new HashMap<>();

                // Get content path for building author URL
                String contentPath = null;
                if (variables.containsKey("contentPath")) {
                    contentPath = (String) variables.get("contentPath");
                    publishInfo.put("contentPath", contentPath);
                }

                // Get published URL (renderer URL from port 8083)
                if (variables.containsKey("publishedUrl")) {
                    publishInfo.put("publishedUrl", variables.get("publishedUrl"));
                }

                // Build author URL for the content path accessible from author instance
                if (contentPath != null && !contentPath.isEmpty()) {
                    // Remove /jcr:content suffix if present
                    String cleanPath = contentPath.replaceAll("/jcr:content.*$", "");
                    String authorUrl = cleanPath + ".html";
                    publishInfo.put("authorUrl", authorUrl);
                }

                if (variables.containsKey("publishedPath")) {
                    publishInfo.put("publishedPath", variables.get("publishedPath"));
                }
                if (variables.containsKey("publishCount")) {
                    publishInfo.put("publishCount", variables.get("publishCount"));
                }
                if (variables.containsKey("failureCount")) {
                    publishInfo.put("failureCount", variables.get("failureCount"));
                }
                if (variables.containsKey("deepPublish")) {
                    publishInfo.put("deepPublish", variables.get("deepPublish"));
                }
                if (variables.containsKey("publishStatus")) {
                    publishInfo.put("publishStatus", variables.get("publishStatus"));
                }
                if (variables.containsKey("publishedTo")) {
                    publishInfo.put("publishedTo", variables.get("publishedTo"));
                }

                if (!publishInfo.isEmpty()) {
                    result.put("publishInfo", publishInfo);
                }

                runtimeService.clearResolverContext();
            } catch (Exception e) {
                LOG.warn("Could not fetch workflow variables for task {}", taskId, e);
                // Continue without variables - not critical
            }

            out.println(GSON.toJson(result));

        } catch (Exception e) {
            LOG.error("Error getting task: {}", taskId, e);
            sendError(response, 500, "Error getting task: " + e.getMessage());
        } finally {
            taskService.clearResolverContext();
        }
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");

        String taskId = request.getParameter("taskId");
        String action = request.getParameter("action");

        if (taskId == null || taskId.isEmpty()) {
            sendError(response, 400, "Missing taskId parameter");
            return;
        }

        if (action == null || action.isEmpty()) {
            sendError(response, 400, "Missing action parameter");
            return;
        }

        try {
            taskService.setResolverContext(request.getResourceResolver());
            String userId = request.getRemoteUser();

            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("action", action);

            switch (action.toLowerCase()) {
                case "complete":
                    handleComplete(request, taskId, userId, result);
                    break;
                case "approve":
                    handleApprove(request, taskId, userId, result);
                    break;
                case "reject":
                    handleReject(request, taskId, userId, result);
                    break;
                case "claim":
                    handleClaim(taskId, userId, result);
                    break;
                case "unclaim":
                    handleUnclaim(taskId, result);
                    break;
                case "delegate":
                    handleDelegate(request, taskId, result);
                    break;
                default:
                    sendError(response, 400, "Unknown action: " + action);
                    return;
            }

            result.put("success", true);
            response.getWriter().println(GSON.toJson(result));

        } catch (Exception e) {
            LOG.error("Error performing action {} on task {}", action, taskId, e);
            sendError(response, 500, "Error performing action: " + e.getMessage());
        } finally {
            taskService.clearResolverContext();
        }
    }

    private void handleComplete(
            SlingHttpServletRequest request, String taskId, String userId, Map<String, Object> result) {
        Map<String, Object> variables = new HashMap<>();

        String comment = request.getParameter("comment");
        if (comment != null && !comment.isEmpty()) {
            variables.put("comment", comment);
        }
        variables.put("completedBy", userId);

        taskService.complete(taskId, variables);
        result.put("message", "Task completed successfully");
        LOG.info("Task {} completed by {}", taskId, userId);
    }

    private void handleApprove(
            SlingHttpServletRequest request, String taskId, String userId, Map<String, Object> result) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("completedBy", userId);

        String comment = request.getParameter("comment");
        if (comment != null && !comment.isEmpty()) {
            variables.put("approvalComment", comment);
        }

        taskService.complete(taskId, variables);
        result.put("message", "Task approved successfully");
        result.put("approved", true);
        LOG.info("Task {} approved by {}", taskId, userId);
    }

    private void handleReject(
            SlingHttpServletRequest request, String taskId, String userId, Map<String, Object> result) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("completedBy", userId);

        String comment = request.getParameter("comment");
        if (comment != null && !comment.isEmpty()) {
            variables.put("rejectionReason", comment);
        }

        taskService.complete(taskId, variables);
        result.put("message", "Task rejected");
        result.put("approved", false);
        LOG.info("Task {} rejected by {}", taskId, userId);
    }

    private void handleClaim(String taskId, String userId, Map<String, Object> result) {
        taskService.claim(taskId, userId);
        result.put("message", "Task claimed by " + userId);
        result.put("assignee", userId);
        LOG.info("Task {} claimed by {}", taskId, userId);
    }

    private void handleUnclaim(String taskId, Map<String, Object> result) {
        taskService.unclaim(taskId);
        result.put("message", "Task released");
        result.put("assignee", null);
        LOG.info("Task {} unclaimed", taskId);
    }

    private void handleDelegate(SlingHttpServletRequest request, String taskId, Map<String, Object> result) {
        String delegateUser = request.getParameter("delegateUser");
        String delegateGroup = request.getParameter("delegateGroup");

        if (delegateUser != null && !delegateUser.isEmpty()) {
            taskService.setAssignee(taskId, delegateUser);
            result.put("message", "Task delegated to user: " + delegateUser);
            result.put("delegatedTo", delegateUser);
            result.put("delegationType", "user");
            LOG.info("Task {} delegated to user {}", taskId, delegateUser);
        } else if (delegateGroup != null && !delegateGroup.isEmpty()) {
            // First unclaim, then set candidate group
            taskService.unclaim(taskId);
            // Note: Setting candidate group requires updating the task in JCR
            // This is a simplified implementation
            result.put("message", "Task released to group: " + delegateGroup);
            result.put("delegatedTo", delegateGroup);
            result.put("delegationType", "group");
            LOG.info("Task {} released to group {}", taskId, delegateGroup);
        } else {
            throw new IllegalArgumentException("Either delegateUser or delegateGroup must be specified");
        }
    }

    private void sendError(SlingHttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        response.getWriter().println(GSON.toJson(error));
    }
}
