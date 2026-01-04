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

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * BPMN 2.0 Process Engine interface.
 *
 * <p>This interface abstracts the underlying workflow engine implementation,
 * allowing seamless migration from native Sling to Flowable/Camunda.</p>
 *
 * <p>The API design follows the Flowable/Camunda pattern for compatibility.</p>
 */
@ProviderType
public interface ProcessEngine {

    /**
     * Get the runtime service for managing process instances.
     *
     * @return RuntimeService instance
     */
    @NotNull
    RuntimeService getRuntimeService();

    /**
     * Get the task service for managing user tasks.
     *
     * @return TaskService instance
     */
    @NotNull
    TaskService getTaskService();

    /**
     * Get the repository service for managing process definitions.
     *
     * @return RepositoryService instance
     */
    @NotNull
    RepositoryService getRepositoryService();

    /**
     * Get the history service for querying completed processes.
     *
     * @return HistoryService instance
     */
    @NotNull
    HistoryService getHistoryService();

    /**
     * Get the engine name.
     *
     * @return Engine name (e.g., "SimpleBPMN", "Flowable")
     */
    @NotNull
    String getName();

    /**
     * Get the engine version.
     *
     * @return Engine version
     */
    @NotNull
    String getVersion();
}
