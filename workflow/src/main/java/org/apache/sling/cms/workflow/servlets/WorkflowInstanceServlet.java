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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.cms.workflow.ProcessInstance;
import org.apache.sling.cms.workflow.RuntimeService;
import org.apache.sling.cms.workflow.Task;
import org.apache.sling.cms.workflow.TaskService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Servlet for viewing workflow instance details.
 * Endpoint: /bin/workflow/instance?instanceId={id}
 */
@Component(
        service = Servlet.class,
        property = {"sling.servlet.paths=/bin/workflow/instance", "sling.servlet.methods=GET"})
public class WorkflowInstanceServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Reference
    private transient RuntimeService runtimeService;

    @Reference
    private transient TaskService taskService;

    private final Gson gson = new Gson();

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String instanceId = request.getParameter("instanceId");

        if (instanceId == null || instanceId.isEmpty()) {
            response.sendError(400, "Missing required parameter: instanceId");
            return;
        }

        // Set resolver context for RuntimeService
        runtimeService.setResolverContext(request.getResourceResolver());

        // Set resolver context for TaskService
        taskService.setResolverContext(request.getResourceResolver());

        ProcessInstance instance = runtimeService.getProcessInstance(instanceId);

        if (instance == null) {
            response.sendError(404, "Workflow instance not found: " + instanceId);
            return;
        }

        // Get associated tasks
        List<Task> tasks = taskService.getTasksByProcessInstanceId(instanceId);

        // Get process variables
        Map<String, Object> variables = runtimeService.getVariables(instanceId);

        // Build response
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("instance", buildInstanceData(instance));
        responseData.put("tasks", buildTasksData(tasks));
        responseData.put("variables", variables);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(gson.toJson(responseData));
    }

    private List<Map<String, Object>> buildTasksData(List<Task> tasks) {
        List<Map<String, Object>> taskList = new ArrayList<>();
        for (Task task : tasks) {
            Map<String, Object> taskData = new HashMap<>();
            taskData.put("id", task.getId());
            taskData.put("name", task.getName());
            taskData.put("processInstanceId", task.getProcessInstanceId());
            taskData.put("assignee", task.getAssignee());
            taskData.put("candidateGroup", task.getCandidateGroup());
            taskData.put("createTime", task.getCreateTime());
            taskData.put("taskDefinitionKey", task.getTaskDefinitionKey());
            taskList.add(taskData);
        }
        return taskList;
    }

    private Map<String, Object> buildInstanceData(ProcessInstance instance) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", instance.getId());
        data.put("processDefinitionKey", instance.getProcessDefinitionKey());
        data.put("businessKey", instance.getBusinessKey());
        data.put("startTime", instance.getStartTime());
        data.put("suspended", instance.isSuspended());
        data.put("currentActivityId", instance.getCurrentActivityId());
        return data;
    }
}
