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
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service for querying historical process data.
 *
 * <p>Modeled after Flowable HistoryService API for compatibility.</p>
 */
@ProviderType
public interface HistoryService {

    /**
     * Query completed process instances.
     *
     * @param processDefinitionKey Process definition key
     * @return List of historic process instances
     */
    @NotNull
    List<HistoricProcessInstance> getCompletedProcessInstances(@NotNull String processDefinitionKey);

    /**
     * Query completed tasks for a process instance.
     *
     * @param processInstanceId Process instance ID
     * @return List of historic tasks
     */
    @NotNull
    List<HistoricTaskInstance> getCompletedTasks(@NotNull String processInstanceId);

    /**
     * Get process instance history.
     *
     * @param processInstanceId Process instance ID
     * @return Historic process instance or null if not found
     */
    @Nullable
    HistoricProcessInstance getHistoricProcessInstance(@NotNull String processInstanceId);

    /**
     * Get historic task instance.
     *
     * @param taskId Task ID
     * @return Historic task instance or null if not found
     */
    @Nullable
    HistoricTaskInstance getHistoricTaskInstance(@NotNull String taskId);

    /**
     * Query all completed process instances.
     *
     * @return List of all historic process instances
     */
    @NotNull
    List<HistoricProcessInstance> getAllCompletedProcessInstances();

    /**
     * Delete historic process instance.
     *
     * @param processInstanceId Process instance ID
     */
    void deleteHistoricProcessInstance(@NotNull String processInstanceId);
}
