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
package org.apache.sling.cms.workflow.test;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.cms.workflow.HistoricProcessInstance;
import org.apache.sling.cms.workflow.HistoricTaskInstance;
import org.apache.sling.cms.workflow.HistoryService;
import org.apache.sling.cms.workflow.ProcessInstance;
import org.apache.sling.cms.workflow.RepositoryService;
import org.apache.sling.cms.workflow.RuntimeService;
import org.apache.sling.cms.workflow.Task;
import org.apache.sling.cms.workflow.TaskQuery;
import org.apache.sling.cms.workflow.TaskService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test servlet to exercise the workflow API.
 *
 * Access at: http://localhost:8082/bin/workflow/test
 *
 * Query parameters:
 * - action: deploy, start, list, complete, history, full (default: full)
 * - processKey: process definition key (default: testProcess)
 * - businessKey: business key for process instance
 * - taskId: task ID to complete
 */
@Component(
        service = Servlet.class,
        property = {"sling.servlet.paths=/bin/workflow/test", "sling.servlet.methods=GET,POST"})
public class WorkflowTestServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowTestServlet.class);

    @Reference
    private RepositoryService repositoryService;

    @Reference
    private RuntimeService runtimeService;

    @Reference
    private TaskService taskService;

    @Reference
    private HistoryService historyService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        // Set ResourceResolver context for TaskService operations
        taskService.setResolverContext(request.getResourceResolver());

        try {
            String action = request.getParameter("action");
            if (action == null) {
                action = "full";
            }

            out.println("<!DOCTYPE html>");
            out.println("<html><head>");
            out.println("<title>Workflow API Test</title>");
            out.println("<style>");
            out.println("body { font-family: Arial, sans-serif; margin: 20px; }");
            out.println("h1 { color: #333; }");
            out.println("h2 { color: #666; margin-top: 30px; }");
            out.println(".success { color: green; font-weight: bold; }");
            out.println(".error { color: red; font-weight: bold; }");
            out.println(".info { background: #f0f0f0; padding: 10px; margin: 10px 0; border-radius: 5px; }");
            out.println("table { border-collapse: collapse; width: 100%; margin: 10px 0; }");
            out.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
            out.println("th { background-color: #4CAF50; color: white; }");
            out.println("tr:nth-child(even) { background-color: #f2f2f2; }");
            out.println(".nav { margin: 20px 0; }");
            out.println(
                    ".nav a { padding: 10px 15px; margin: 5px; background: #4CAF50; color: white; text-decoration: none; border-radius: 3px; }");
            out.println(".nav a:hover { background: #45a049; }");
            out.println("</style>");
            out.println("</head><body>");

            out.println("<h1>🔄 Workflow API Test Suite</h1>");

            // Navigation
            out.println("<div class='nav'>");
            out.println("<a href='?action=full'>Full Test</a>");
            out.println("<a href='?action=deploy'>Deploy Only</a>");
            out.println("<a href='?action=start'>Start Instance</a>");
            out.println("<a href='?action=list'>List Active</a>");
            out.println("<a href='?action=history'>View History</a>");
            out.println("</div>");

            switch (action) {
                case "deploy":
                    testDeploy(out);
                    break;
                case "start":
                    testStartInstance(request, out);
                    break;
                case "list":
                    testListActive(out);
                    break;
                case "complete":
                    testCompleteTask(request, out);
                    break;
                case "history":
                    testHistory(out);
                    break;
                case "full":
                default:
                    runFullTest(out);
                    break;
            }
        } catch (Exception e) {
            LOG.error("Test failed", e);
            out.println("<p class='error'>❌ Error: " + e.getMessage() + "</p>");
            out.println("<pre>");
            e.printStackTrace(out);
            out.println("</pre>");
        } finally {
            // Clear ResourceResolver context to prevent memory leaks
            taskService.clearResolverContext();
        }

        out.println("</body></html>");
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    private void runFullTest(PrintWriter out) throws Exception {
        out.println("<h2>Running Full Workflow Test Suite</h2>");

        // Test 1: Deploy workflow
        out.println("<h3>Test 1: Deploy Workflow</h3>");
        String deploymentId = testDeploy(out);

        // Test 2: Start process instance
        out.println("<h3>Test 2: Start Process Instance</h3>");
        String processInstanceId = testStartProcessInstance(out);

        // Test 3: Query active instances
        out.println("<h3>Test 3: Query Active Instances</h3>");
        testListActive(out);

        // Test 4: Get and complete tasks
        out.println("<h3>Test 4: Task Management</h3>");
        testTaskManagement(out, processInstanceId);

        // Test 5: Query history
        out.println("<h3>Test 5: Historical Data</h3>");
        testHistory(out);

        out.println("<p class='success'>✅ Full test suite completed!</p>");
    }

    private String testDeploy(PrintWriter out) throws Exception {
        out.println("<div class='info'>");
        out.println("<p><strong>Deploying test workflow...</strong></p>");

        // Create simple test BPMN
        String bpmnXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             targetNamespace="http://sling.apache.org/workflow">
                  <process id="testProcess" name="Test Process">
                    <startEvent id="start" name="Start"/>
                    <sequenceFlow sourceRef="start" targetRef="task1"/>
                    <userTask id="task1" name="Review Task">
                      <potentialOwner>
                        <resourceAssignmentExpression>
                          <formalExpression>reviewers</formalExpression>
                        </resourceAssignmentExpression>
                      </potentialOwner>
                    </userTask>
                    <sequenceFlow sourceRef="task1" targetRef="gateway1"/>
                    <exclusiveGateway id="gateway1" name="Approved?"/>
                    <sequenceFlow sourceRef="gateway1" targetRef="task2">
                      <conditionExpression>${approved == true}</conditionExpression>
                    </sequenceFlow>
                    <sequenceFlow sourceRef="gateway1" targetRef="end2">
                      <conditionExpression>${approved == false}</conditionExpression>
                    </sequenceFlow>
                    <serviceTask id="task2" name="Publish">
                      <extensionElements>
                        <delegateClass>org.apache.sling.cms.workflow.test.TestPublishDelegate</delegateClass>
                      </extensionElements>
                    </serviceTask>
                    <sequenceFlow sourceRef="task2" targetRef="end1"/>
                    <endEvent id="end1" name="Published"/>
                    <endEvent id="end2" name="Rejected"/>
                  </process>
                </definitions>
                """;

        InputStream bpmnStream = new java.io.ByteArrayInputStream(bpmnXml.getBytes("UTF-8"));
        repositoryService.deploy("testProcess", bpmnStream);

        out.println("<p class='success'>✅ Workflow deployed successfully</p>");
        out.println("<p>Process Definition Key: <code>testProcess</code></p>");
        out.println("</div>");

        return "testProcess";
    }

    private String testStartProcessInstance(PrintWriter out) throws Exception {
        out.println("<div class='info'>");
        out.println("<p><strong>Starting process instance...</strong></p>");

        Map<String, Object> variables = new HashMap<>();
        variables.put("author", "admin");
        variables.put("contentPath", "/content/test/page");
        variables.put("timestamp", System.currentTimeMillis());

        String businessKey = "TEST-" + System.currentTimeMillis();
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("testProcess", businessKey, variables);

        out.println("<p class='success'>✅ Process instance started</p>");
        out.println("<table>");
        out.println("<tr><th>Property</th><th>Value</th></tr>");
        out.println("<tr><td>Instance ID</td><td><code>" + instance.getId() + "</code></td></tr>");
        out.println("<tr><td>Business Key</td><td>" + instance.getBusinessKey() + "</td></tr>");
        out.println("<tr><td>Process Definition ID</td><td>" + instance.getProcessDefinitionId() + "</td></tr>");
        out.println("<tr><td>State</td><td>"
                + (instance.isEnded() ? "Ended" : instance.isSuspended() ? "Suspended" : "Active") + "</td></tr>");
        out.println("<tr><td>Variables</td><td>" + instance.getVariables().size() + "</td></tr>");
        out.println("</table>");
        out.println("</div>");

        return instance.getId();
    }

    private void testStartInstance(SlingHttpServletRequest request, PrintWriter out) throws Exception {
        String processKey = request.getParameter("processKey");
        if (processKey == null) {
            processKey = "testProcess";
        }

        String businessKey = request.getParameter("businessKey");
        if (businessKey == null) {
            businessKey = "TEST-" + System.currentTimeMillis();
        }

        out.println("<h2>Starting Process Instance</h2>");
        out.println("<div class='info'>");
        out.println("<p>Process Key: <code>" + processKey + "</code></p>");
        out.println("<p>Business Key: <code>" + businessKey + "</code></p>");

        Map<String, Object> variables = new HashMap<>();
        variables.put("author", request.getRemoteUser() != null ? request.getRemoteUser() : "admin");
        variables.put("startedFrom", "test-servlet");

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(processKey, businessKey, variables);

        out.println("<p class='success'>✅ Process instance created</p>");
        out.println("<p>Instance ID: <code>" + instance.getId() + "</code></p>");
        out.println("</div>");
    }

    private void testListActive(PrintWriter out) throws Exception {
        out.println("<h2>Active Process Instances</h2>");

        List<ProcessInstance> instances = runtimeService.getActiveProcessInstances();

        out.println("<div class='info'>");
        out.println("<p>Found <strong>" + instances.size() + "</strong> active process instances</p>");

        if (!instances.isEmpty()) {
            out.println("<table>");
            out.println(
                    "<tr><th>Instance ID</th><th>Process Def</th><th>Business Key</th><th>State</th><th>Variables</th></tr>");

            for (ProcessInstance instance : instances) {
                out.println("<tr>");
                out.println("<td><code>" + instance.getId() + "</code></td>");
                out.println("<td>" + instance.getProcessDefinitionId() + "</td>");
                out.println("<td>" + (instance.getBusinessKey() != null ? instance.getBusinessKey() : "-") + "</td>");
                out.println("<td>" + (instance.isEnded() ? "Ended" : instance.isSuspended() ? "Suspended" : "Active")
                        + "</td>");
                out.println("<td>" + instance.getVariables().size() + "</td>");
                out.println("</tr>");
            }

            out.println("</table>");
        }
        out.println("</div>");

        // Also list tasks
        out.println("<h3>Active Tasks</h3>");
        TaskQuery query = taskService.createTaskQuery();
        List<Task> tasks = query.list();

        out.println("<div class='info'>");
        out.println("<p>Found <strong>" + tasks.size() + "</strong> active tasks</p>");

        if (!tasks.isEmpty()) {
            out.println("<table>");
            out.println(
                    "<tr><th>Task ID</th><th>Name</th><th>Assignee</th><th>Process Instance</th><th>Actions</th></tr>");

            for (Task task : tasks) {
                out.println("<tr>");
                out.println("<td><code>" + task.getId() + "</code></td>");
                out.println("<td>" + task.getName() + "</td>");
                out.println("<td>" + (task.getAssignee() != null ? task.getAssignee() : "-") + "</td>");
                out.println("<td><code>" + task.getProcessInstanceId() + "</code></td>");
                out.println("<td><a href='?action=complete&taskId=" + task.getId() + "'>Complete</a></td>");
                out.println("</tr>");
            }

            out.println("</table>");
        }
        out.println("</div>");
    }

    private void testTaskManagement(PrintWriter out, String processInstanceId) throws Exception {
        out.println("<div class='info'>");
        out.println("<p><strong>Testing task management...</strong></p>");

        // Query tasks for this process
        List<Task> tasks = taskService.getTasksByProcessInstanceId(processInstanceId);

        out.println("<p>Found " + tasks.size() + " task(s) for process instance</p>");

        if (!tasks.isEmpty()) {
            Task task = tasks.get(0);
            out.println("<p>Task: " + task.getName() + " (ID: " + task.getId() + ")</p>");

            // Complete task with variables
            Map<String, Object> taskVariables = new HashMap<>();
            taskVariables.put("approved", true);
            taskVariables.put("reviewComment", "Approved via test servlet");

            taskService.complete(task.getId(), taskVariables);

            out.println("<p class='success'>✅ Task completed successfully</p>");
        }

        out.println("</div>");
    }

    private void testCompleteTask(SlingHttpServletRequest request, PrintWriter out) throws Exception {
        String taskId = request.getParameter("taskId");

        out.println("<h2>Complete Task</h2>");

        if (taskId == null || taskId.isEmpty()) {
            out.println("<p class='error'>❌ No taskId provided</p>");
            return;
        }

        out.println("<div class='info'>");
        out.println("<p>Completing task: <code>" + taskId + "</code></p>");

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("completedBy", request.getRemoteUser() != null ? request.getRemoteUser() : "admin");

        taskService.complete(taskId, variables);

        out.println("<p class='success'>✅ Task completed successfully</p>");
        out.println("</div>");
    }

    private void testHistory(PrintWriter out) throws Exception {
        out.println("<h2>Historical Data</h2>");

        // Get all completed processes
        List<HistoricProcessInstance> completedProcesses = historyService.getAllCompletedProcessInstances();

        out.println("<div class='info'>");
        out.println("<p>Found <strong>" + completedProcesses.size() + "</strong> completed process instances</p>");

        if (!completedProcesses.isEmpty()) {
            out.println("<table>");
            out.println(
                    "<tr><th>Process ID</th><th>Definition Key</th><th>Business Key</th><th>Start Time</th><th>End Time</th><th>Duration (ms)</th></tr>");

            for (HistoricProcessInstance hist : completedProcesses) {
                out.println("<tr>");
                out.println("<td><code>" + hist.getId() + "</code></td>");
                out.println("<td>" + hist.getProcessDefinitionKey() + "</td>");
                out.println("<td>" + (hist.getBusinessKey() != null ? hist.getBusinessKey() : "-") + "</td>");
                out.println("<td>" + (hist.getStartTime() != null ? hist.getStartTime() : "-") + "</td>");
                out.println("<td>" + (hist.getEndTime() != null ? hist.getEndTime() : "-") + "</td>");
                out.println("<td>" + hist.getDurationInMillis() + "</td>");
                out.println("</tr>");

                // Get tasks for this process
                List<HistoricTaskInstance> tasks = historyService.getCompletedTasks(hist.getId());
                if (!tasks.isEmpty()) {
                    out.println("<tr><td colspan='6'>");
                    out.println("<strong>Tasks:</strong><ul>");
                    for (HistoricTaskInstance task : tasks) {
                        out.println("<li>" + task.getName() + " - Assignee: " + task.getAssignee() + " - Duration: "
                                + task.getDurationInMillis() + "ms</li>");
                    }
                    out.println("</ul></td></tr>");
                }
            }

            out.println("</table>");
        }
        out.println("</div>");
    }
}
