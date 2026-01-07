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
package org.apache.sling.cms.workflow.internal.engine;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.workflow.Task;
import org.apache.sling.cms.workflow.TaskQuery;
import org.apache.sling.cms.workflow.TaskService;
import org.apache.sling.cms.workflow.WorkflowException;
import org.apache.sling.cms.workflow.internal.executor.ProcessExecutor;
import org.apache.sling.cms.workflow.internal.instance.ProcessInstanceImpl;
import org.apache.sling.cms.workflow.internal.instance.ProcessInstanceManager;
import org.apache.sling.cms.workflow.internal.model.ProcessDefinitionImpl;
import org.apache.sling.cms.workflow.internal.task.TaskImpl;
import org.apache.sling.cms.workflow.internal.task.TaskManager;
import org.apache.sling.cms.workflow.internal.task.TaskQueryImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BPMN implementation of TaskService.
 */
@Component(service = TaskService.class)
public class BPMNTaskService implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(BPMNTaskService.class);

    @Reference
    private TaskManager taskManager;

    @Reference
    private ProcessInstanceManager processInstanceManager;

    @Reference
    private BPMNRepositoryService repositoryService;

    @Reference
    private ProcessExecutor processExecutor;

    // Internal helper to get ResourceResolver - for MVP, services need resolver passed in
    private ThreadLocal<ResourceResolver> resolverContext = new ThreadLocal<>();

    @Override
    @Nullable
    public Task getTask(@NotNull String taskId) {
        try {
            // MVP: Requires resolver to be set in context
            ResourceResolver resolver = resolverContext.get();
            if (resolver == null) {
                throw new IllegalStateException("ResourceResolver not set in context");
            }
            return taskManager.getTask(taskId, resolver);
        } catch (WorkflowException e) {
            return null;
        }
    }

    @Override
    @NotNull
    public List<Task> getTasksAssignedTo(@NotNull String userId) {
        try {
            ResourceResolver resolver = resolverContext.get();
            if (resolver == null) {
                return List.of();
            }
            return taskManager.findTasksByAssignee(userId, resolver).stream()
                    .map(task -> (Task) task)
                    .collect(Collectors.toList());
        } catch (WorkflowException e) {
            return List.of();
        }
    }

    @Override
    @NotNull
    public List<Task> getTasksByCandidateGroup(@NotNull String groupId) {
        try {
            ResourceResolver resolver = resolverContext.get();
            if (resolver == null) {
                return List.of();
            }
            return taskManager.findTasksByCandidateGroup(groupId, resolver).stream()
                    .map(task -> (Task) task)
                    .collect(Collectors.toList());
        } catch (WorkflowException e) {
            return List.of();
        }
    }

    @Override
    @NotNull
    public List<Task> getTasksByCandidateUser(@NotNull String userId) {
        // MVP: Same as assigned to
        return getTasksAssignedTo(userId);
    }

    @Override
    @NotNull
    public List<Task> getTasksByProcessInstanceId(@NotNull String processInstanceId) {
        try {
            ResourceResolver resolver = resolverContext.get();
            if (resolver == null) {
                log.warn("Cannot get tasks for process instance {}: resolver is null", processInstanceId);
                return List.of();
            }
            List<TaskImpl> tasks = taskManager.findTasksByProcessInstance(processInstanceId, resolver);
            log.debug("Found {} tasks for process instance {}", tasks.size(), processInstanceId);
            return tasks.stream().map(task -> (Task) task).collect(Collectors.toList());
        } catch (WorkflowException e) {
            log.error("Failed to get tasks for process instance: {}", processInstanceId, e);
            return List.of();
        }
    }

    @Override
    public void complete(@NotNull String taskId) {
        complete(taskId, null);
    }

    @Override
    public void complete(@NotNull String taskId, @Nullable Map<String, Object> variables) {
        try {
            ResourceResolver resolver = resolverContext.get();
            if (resolver == null) {
                throw new WorkflowException("ResourceResolver not set in context");
            }

            TaskImpl task = taskManager.getTask(taskId, resolver);
            if (task == null) {
                throw new WorkflowException("Task not found: " + taskId);
            }

            // Get process instance
            ProcessInstanceImpl processInstance =
                    processInstanceManager.getProcessInstance(task.getProcessInstanceId(), resolver);
            if (processInstance == null) {
                throw new WorkflowException("Process instance not found: " + task.getProcessInstanceId());
            }

            // Complete task
            Map<String, Object> completionVars = taskManager.completeTask(taskId, variables, resolver);

            // Get process definition
            ProcessDefinitionImpl processDefinition =
                    repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId(), resolver);
            if (processDefinition == null) {
                throw new WorkflowException(
                        "Process definition not found: " + processInstance.getProcessDefinitionId());
            }

            // Continue process execution
            processExecutor.continueExecution(
                    processDefinition, processInstance, task.getActivityId(), completionVars, resolver);
        } catch (WorkflowException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void claim(@NotNull String taskId, @NotNull String userId) {
        try {
            ResourceResolver resolver = resolverContext.get();
            if (resolver == null) {
                throw new WorkflowException("ResourceResolver not set in context");
            }
            taskManager.claimTask(taskId, userId, resolver);
        } catch (WorkflowException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unclaim(@NotNull String taskId) {
        try {
            ResourceResolver resolver = resolverContext.get();
            if (resolver == null) {
                throw new WorkflowException("ResourceResolver not set in context");
            }
            TaskImpl task = taskManager.getTask(taskId, resolver);
            if (task != null) {
                task.setAssignee(null);
                // MVP: Would need to update in JCR
            }
        } catch (WorkflowException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setAssignee(@NotNull String taskId, @Nullable String userId) {
        try {
            ResourceResolver resolver = resolverContext.get();
            if (resolver == null) {
                throw new WorkflowException("ResourceResolver not set in context");
            }
            if (userId != null) {
                taskManager.claimTask(taskId, userId, resolver);
            } else {
                unclaim(taskId);
            }
        } catch (WorkflowException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setPriority(@NotNull String taskId, int priority) {
        // MVP: Not implemented yet
    }

    @Override
    @NotNull
    public TaskQuery createTaskQuery() {
        ResourceResolver resolver = resolverContext.get();
        if (resolver == null) {
            throw new IllegalStateException("ResourceResolver not set in context");
        }
        return new TaskQueryImpl(taskManager, resolver);
    }

    /**
     * Sets the ResourceResolver context for API calls.
     * MVP helper method - in production, use different approach.
     *
     * @param resolver Resource resolver
     */
    public void setResolverContext(@NotNull ResourceResolver resolver) {
        resolverContext.set(resolver);
    }

    /**
     * Clears the ResourceResolver context.
     */
    public void clearResolverContext() {
        resolverContext.remove();
    }
}
