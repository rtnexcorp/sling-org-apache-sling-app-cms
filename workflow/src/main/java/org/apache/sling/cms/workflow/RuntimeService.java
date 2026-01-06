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
 * Service for managing runtime process instances.
 *
 * <p>Modeled after Flowable RuntimeService API for compatibility.</p>
 */
@ProviderType
public interface RuntimeService {

    /**
     * Start a new process instance by process definition key.
     *
     * @param processDefinitionKey Process key from BPMN XML (e.g., "contentApproval")
     * @param variables Process variables (initial context)
     * @return Process instance
     * @throws WorkflowException if process cannot be started
     */
    @NotNull
    ProcessInstance startProcessInstanceByKey(
            @NotNull String processDefinitionKey, @Nullable Map<String, Object> variables);

    /**
     * Start a new process instance with business key.
     *
     * @param processDefinitionKey Process definition key
     * @param businessKey Unique business identifier (e.g., content path)
     * @param variables Process variables
     * @return Process instance
     * @throws WorkflowException if process cannot be started
     */
    @NotNull
    ProcessInstance startProcessInstanceByKey(
            @NotNull String processDefinitionKey, @NotNull String businessKey, @Nullable Map<String, Object> variables);

    /**
     * Delete a running process instance.
     *
     * @param processInstanceId Process instance ID
     * @param deleteReason Reason for deletion
     * @throws WorkflowException if process cannot be deleted
     */
    void deleteProcessInstance(@NotNull String processInstanceId, @NotNull String deleteReason);

    /**
     * Get a process instance by ID.
     *
     * @param processInstanceId Process instance ID
     * @return Process instance or null if not found
     */
    @Nullable
    ProcessInstance getProcessInstance(@NotNull String processInstanceId);

    /**
     * Get a process instance by business key.
     *
     * @param businessKey Business key
     * @return Process instance or null if not found
     */
    @Nullable
    ProcessInstance getProcessInstanceByBusinessKey(@NotNull String businessKey);

    /**
     * Get all active process instances.
     *
     * @return List of active process instances
     */
    @NotNull
    List<ProcessInstance> getActiveProcessInstances();

    /**
     * Get process variables.
     *
     * @param processInstanceId Process instance ID
     * @return Process variables
     * @throws WorkflowException if process not found
     */
    @NotNull
    Map<String, Object> getVariables(@NotNull String processInstanceId);

    /**
     * Get a single process variable.
     *
     * @param processInstanceId Process instance ID
     * @param variableName Variable name
     * @return Variable value or null if not set
     */
    @Nullable
    Object getVariable(@NotNull String processInstanceId, @NotNull String variableName);

    /**
     * Set process variables.
     *
     * @param processInstanceId Process instance ID
     * @param variables Variables to set
     * @throws WorkflowException if process not found
     */
    void setVariables(@NotNull String processInstanceId, @NotNull Map<String, Object> variables);

    /**
     * Set a single process variable.
     *
     * @param processInstanceId Process instance ID
     * @param variableName Variable name
     * @param value Variable value
     * @throws WorkflowException if process not found
     */
    void setVariable(@NotNull String processInstanceId, @NotNull String variableName, @Nullable Object value);

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
