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
package org.apache.sling.cms.workflow;

import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service for managing user tasks.
 *
 * <p>Modeled after Flowable TaskService API for compatibility.</p>
 */
@ProviderType
public interface TaskService {

    /**
     * Get a task by ID.
     *
     * @param taskId Task ID
     * @return Task instance or null if not found
     */
    @Nullable
    Task getTask(@NotNull String taskId);

    /**
     * Query tasks assigned to a user.
     *
     * @param userId User ID
     * @return List of tasks
     */
    @NotNull
    List<Task> getTasksAssignedTo(@NotNull String userId);

    /**
     * Query tasks by candidate group.
     *
     * @param groupId Group ID (e.g., "content-reviewers")
     * @return List of tasks
     */
    @NotNull
    List<Task> getTasksByCandidateGroup(@NotNull String groupId);

    /**
     * Query tasks by candidate user.
     *
     * @param userId User ID
     * @return List of tasks
     */
    @NotNull
    List<Task> getTasksByCandidateUser(@NotNull String userId);

    /**
     * Get all active tasks for a process instance.
     *
     * @param processInstanceId Process instance ID
     * @return List of tasks
     */
    @NotNull
    List<Task> getTasksByProcessInstanceId(@NotNull String processInstanceId);

    /**
     * Complete a task.
     *
     * @param taskId Task ID
     * @throws WorkflowException if task cannot be completed
     */
    void complete(@NotNull String taskId);

    /**
     * Complete a task with variables.
     *
     * @param taskId Task ID
     * @param variables Variables to set when completing task
     * @throws WorkflowException if task cannot be completed
     */
    void complete(@NotNull String taskId, @Nullable Map<String, Object> variables);

    /**
     * Claim a task for a user.
     *
     * @param taskId Task ID
     * @param userId User ID
     * @throws WorkflowException if task cannot be claimed
     */
    void claim(@NotNull String taskId, @NotNull String userId);

    /**
     * Unclaim a task (release back to group).
     *
     * @param taskId Task ID
     * @throws WorkflowException if task cannot be unclaimed
     */
    void unclaim(@NotNull String taskId);

    /**
     * Set task assignee.
     *
     * @param taskId Task ID
     * @param userId User ID or null to unassign
     */
    void setAssignee(@NotNull String taskId, @Nullable String userId);

    /**
     * Set task priority.
     *
     * @param taskId Task ID
     * @param priority Priority value
     */
    void setPriority(@NotNull String taskId, int priority);

    /**
     * Create a task query builder.
     *
     * @return Task query
     */
    @NotNull
    TaskQuery createTaskQuery();

    /**
     * Sets the ResourceResolver context for API calls.
     * MVP helper method - in production, use different approach.
     *
     * @param resolver Resource resolver
     */
    void setResolverContext(@NotNull ResourceResolver resolver);

    /**
     * Clears the ResourceResolver context.
     */
    void clearResolverContext();
}
