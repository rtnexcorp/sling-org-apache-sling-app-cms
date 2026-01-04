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

import java.io.InputStream;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service for managing process definitions.
 *
 * <p>Modeled after Flowable RepositoryService API for compatibility.</p>
 */
@ProviderType
public interface RepositoryService {

    /**
     * Deploy a process definition from BPMN 2.0 XML.
     *
     * @param name Deployment name
     * @param bpmnXml BPMN XML input stream
     * @return Deployment
     * @throws WorkflowException if deployment fails
     */
    @NotNull
    Deployment deploy(@NotNull String name, @NotNull InputStream bpmnXml);

    /**
     * Deploy a process definition from resource path.
     *
     * @param name Deployment name
     * @param resourcePath Path to BPMN XML resource
     * @return Deployment
     * @throws WorkflowException if deployment fails
     */
    @NotNull
    Deployment deployFromResource(@NotNull String name, @NotNull String resourcePath);

    /**
     * Get all deployed process definitions.
     *
     * @return List of process definitions
     */
    @NotNull
    List<ProcessDefinition> getProcessDefinitions();

    /**
     * Get process definition by key.
     *
     * @param processDefinitionKey Process definition key
     * @return Process definition or null if not found
     */
    @Nullable
    ProcessDefinition getProcessDefinitionByKey(@NotNull String processDefinitionKey);

    /**
     * Get process definition by ID.
     *
     * @param processDefinitionId Process definition ID
     * @return Process definition or null if not found
     */
    @Nullable
    ProcessDefinition getProcessDefinitionById(@NotNull String processDefinitionId);

    /**
     * Delete a deployment.
     *
     * @param deploymentId Deployment ID
     * @param cascade Delete related process instances
     * @throws WorkflowException if deletion fails
     */
    void deleteDeployment(@NotNull String deploymentId, boolean cascade);

    /**
     * Get BPMN XML for a process definition.
     *
     * @param processDefinitionId Process definition ID
     * @return BPMN XML as string or null if not found
     */
    @Nullable
    String getProcessModel(@NotNull String processDefinitionId);
}
