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

import java.util.Date;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a running process instance.
 */
@ProviderType
public interface ProcessInstance {

    /**
     * Unique process instance ID.
     *
     * @return Process instance ID
     */
    @NotNull
    String getId();

    /**
     * Process definition key from BPMN XML.
     *
     * @return Process definition key
     */
    @NotNull
    String getProcessDefinitionKey();

    /**
     * Process definition ID.
     *
     * @return Process definition ID
     */
    @NotNull
    String getProcessDefinitionId();

    /**
     * Business key (e.g., content path).
     *
     * @return Business key or null if not set
     */
    @Nullable
    String getBusinessKey();

    /**
     * Start time.
     *
     * @return Start time
     */
    @NotNull
    Date getStartTime();

    /**
     * End time (null if not ended).
     *
     * @return End time or null
     */
    @Nullable
    Date getEndTime();

    /**
     * Whether process is ended.
     *
     * @return true if ended
     */
    boolean isEnded();

    /**
     * Whether process is suspended.
     *
     * @return true if suspended
     */
    boolean isSuspended();

    /**
     * Current activity ID.
     *
     * @return Current activity ID or null if process ended
     */
    @Nullable
    String getCurrentActivityId();

    /**
     * Process variables.
     *
     * @return Process variables map
     */
    @NotNull
    Map<String, Object> getVariables();

    /**
     * Get a single variable value.
     *
     * @param variableName Variable name
     * @return Variable value or null if not set
     */
    @Nullable
    Object getVariable(@NotNull String variableName);
}
