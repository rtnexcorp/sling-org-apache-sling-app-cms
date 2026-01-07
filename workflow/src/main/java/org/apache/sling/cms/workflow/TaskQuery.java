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

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Builder for task queries.
 */
@ProviderType
public interface TaskQuery {

    /**
     * Filter by task assignee.
     *
     * @param assignee User ID
     * @return This query
     */
    @NotNull
    TaskQuery taskAssignee(@NotNull String assignee);

    /**
     * Filter by candidate group.
     *
     * @param candidateGroup Group ID
     * @return This query
     */
    @NotNull
    TaskQuery taskCandidateGroup(@NotNull String candidateGroup);

    /**
     * Filter by candidate user.
     *
     * @param candidateUser User ID
     * @return This query
     */
    @NotNull
    TaskQuery taskCandidateUser(@NotNull String candidateUser);

    /**
     * Filter by process instance ID.
     *
     * @param processInstanceId Process instance ID
     * @return This query
     */
    @NotNull
    TaskQuery processInstanceId(@NotNull String processInstanceId);

    /**
     * Filter by process definition key.
     *
     * @param processDefinitionKey Process definition key
     * @return This query
     */
    @NotNull
    TaskQuery processDefinitionKey(@NotNull String processDefinitionKey);

    /**
     * Order by creation time ascending.
     *
     * @return This query
     */
    @NotNull
    TaskQuery orderByTaskCreateTime();

    /**
     * Order by priority descending.
     *
     * @return This query
     */
    @NotNull
    TaskQuery orderByTaskPriority();

    /**
     * Execute query and return results.
     *
     * @return List of tasks matching query
     */
    @NotNull
    List<Task> list();

    /**
     * Count tasks matching query.
     *
     * @return Count of tasks
     */
    long count();
}
