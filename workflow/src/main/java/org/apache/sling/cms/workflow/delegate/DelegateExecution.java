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
package org.apache.sling.cms.workflow.delegate;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Execution context passed to JavaDelegate implementations.
 *
 * <p>Provides access to process variables and execution metadata.</p>
 */
@ProviderType
public interface DelegateExecution {

    /**
     * Get process instance ID.
     *
     * @return Process instance ID
     */
    @NotNull
    String getProcessInstanceId();

    /**
     * Get process definition key.
     *
     * @return Process definition key
     */
    @NotNull
    String getProcessDefinitionKey();

    /**
     * Get current activity ID.
     *
     * @return Current activity ID
     */
    @NotNull
    String getCurrentActivityId();

    /**
     * Get business key.
     *
     * @return Business key or null
     */
    @Nullable
    String getBusinessKey();

    /**
     * Get process variable.
     *
     * @param variableName Variable name
     * @return Variable value or null
     */
    @Nullable
    Object getVariable(@NotNull String variableName);

    /**
     * Get process variable with type.
     *
     * @param variableName Variable name
     * @param variableClass Expected variable type
     * @param <T> Variable type
     * @return Variable value or null
     */
    @Nullable
    <T> T getVariable(@NotNull String variableName, @NotNull Class<T> variableClass);

    /**
     * Set process variable.
     *
     * @param variableName Variable name
     * @param value Variable value
     */
    void setVariable(@NotNull String variableName, @Nullable Object value);

    /**
     * Get all process variables.
     *
     * @return Process variables map
     */
    @NotNull
    Map<String, Object> getVariables();

    /**
     * Set multiple process variables.
     *
     * @param variables Variables to set
     */
    void setVariables(@NotNull Map<String, Object> variables);

    /**
     * Get injected field value from BPMN XML.
     *
     * <p>Fields are defined in BPMN service task using flowable:field elements.</p>
     *
     * @param fieldName Field name
     * @return Field value or null
     */
    @Nullable
    String getFieldValue(@NotNull String fieldName);

    /**
     * Check if variable exists.
     *
     * @param variableName Variable name
     * @return true if variable exists
     */
    boolean hasVariable(@NotNull String variableName);

    /**
     * Remove a variable.
     *
     * @param variableName Variable name
     */
    void removeVariable(@NotNull String variableName);
}
