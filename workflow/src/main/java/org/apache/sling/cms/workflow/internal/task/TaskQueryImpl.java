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
package org.apache.sling.cms.workflow.internal.task;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.workflow.Task;
import org.apache.sling.cms.workflow.TaskQuery;
import org.apache.sling.cms.workflow.WorkflowException;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of TaskQuery.
 */
public class TaskQueryImpl implements TaskQuery {

    private final TaskManager taskManager;
    private final ResourceResolver resolver;
    private String processInstanceId;
    private String assignee;
    private String candidateGroup;

    public TaskQueryImpl(@NotNull TaskManager taskManager, @NotNull ResourceResolver resolver) {
        this.taskManager = taskManager;
        this.resolver = resolver;
    }

    @Override
    @NotNull
    public TaskQuery processInstanceId(@NotNull String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Override
    @NotNull
    public TaskQuery taskAssignee(@NotNull String assignee) {
        this.assignee = assignee;
        return this;
    }

    @Override
    @NotNull
    public TaskQuery taskCandidateGroup(@NotNull String candidateGroup) {
        this.candidateGroup = candidateGroup;
        return this;
    }

    @Override
    @NotNull
    public TaskQuery taskCandidateUser(@NotNull String candidateUser) {
        // MVP: Treat same as assignee
        this.assignee = candidateUser;
        return this;
    }

    @Override
    @NotNull
    public TaskQuery processDefinitionKey(@NotNull String processDefinitionKey) {
        // MVP: Not filtering by process definition key yet
        return this;
    }

    @Override
    @NotNull
    public TaskQuery orderByTaskCreateTime() {
        // MVP: No ordering implemented yet
        return this;
    }

    @Override
    @NotNull
    public TaskQuery orderByTaskPriority() {
        // MVP: No ordering implemented yet
        return this;
    }

    @Override
    @NotNull
    public List<Task> list() {
        try {
            List<TaskImpl> tasks;

            if (processInstanceId != null) {
                tasks = taskManager.findTasksByProcessInstance(processInstanceId, resolver);
            } else if (assignee != null) {
                tasks = taskManager.findTasksByAssignee(assignee, resolver);
            } else if (candidateGroup != null) {
                tasks = taskManager.findTasksByCandidateGroup(candidateGroup, resolver);
            } else {
                throw new WorkflowException("At least one query parameter must be specified");
            }

            return tasks.stream().map(task -> (Task) task).collect(Collectors.toList());
        } catch (WorkflowException e) {
            // MVP: Return empty list on error
            return List.of();
        }
    }

    @Override
    public long count() {
        return list().size();
    }
}
