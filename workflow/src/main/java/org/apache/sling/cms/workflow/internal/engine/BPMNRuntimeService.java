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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.cms.workflow.ProcessInstance;
import org.apache.sling.cms.workflow.RuntimeService;
import org.apache.sling.cms.workflow.WorkflowException;
import org.apache.sling.cms.workflow.internal.executor.ProcessExecutor;
import org.apache.sling.cms.workflow.internal.instance.ProcessInstanceImpl;
import org.apache.sling.cms.workflow.internal.instance.ProcessInstanceManager;
import org.apache.sling.cms.workflow.internal.model.ProcessDefinitionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BPMN implementation of RuntimeService.
 */
@Component(service = RuntimeService.class)
public class BPMNRuntimeService implements RuntimeService {

    private static final Logger log = LoggerFactory.getLogger(BPMNRuntimeService.class);
    private static final String SERVICE_USER = "sling-cms-workflow";

    @Reference
    private ProcessInstanceManager processInstanceManager;

    @Reference
    private ProcessExecutor processExecutor;

    @Reference
    private BPMNRepositoryService repositoryService;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    @NotNull
    public ProcessInstance startProcessInstanceByKey(
            @NotNull String processDefinitionKey, @Nullable Map<String, Object> variables) {
        return startProcessInstanceByKey(processDefinitionKey, null, variables);
    }

    @Override
    @NotNull
    public ProcessInstance startProcessInstanceByKey(
            @NotNull String processDefinitionKey,
            @NotNull String businessKey,
            @Nullable Map<String, Object> variables) {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            // Get process definition
            ProcessDefinitionImpl processDefinition =
                    repositoryService.getProcessDefinition(processDefinitionKey, resolver);
            if (processDefinition == null) {
                throw new WorkflowException("Process definition not found: " + processDefinitionKey);
            }

            // Create process instance
            ProcessInstanceImpl processInstance = processInstanceManager.createProcessInstance(
                    processDefinition.getId(), businessKey, variables, resolver);

            log.info("Started process instance: {} for definition: {}", processInstance.getId(), processDefinitionKey);

            // Execute process (starts workflow execution)
            processExecutor.executeProcess(processDefinition, processInstance, resolver);

            return processInstance;

        } catch (LoginException e) {
            throw new WorkflowException("Failed to get service resource resolver", e);
        } catch (Exception e) {
            throw new WorkflowException("Failed to start process instance", e);
        }
    }

    @Override
    public void deleteProcessInstance(@NotNull String processInstanceId, @NotNull String deleteReason) {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            ProcessInstanceImpl instance = processInstanceManager.getProcessInstance(processInstanceId, resolver);
            if (instance == null) {
                throw new WorkflowException("Process instance not found: " + processInstanceId);
            }

            // Mark as ended before deletion
            instance.setEnded(true);
            processInstanceManager.updateProcessInstance(instance, resolver);

            // Delete from JCR
            processInstanceManager.deleteProcessInstance(processInstanceId, resolver);

            log.info("Deleted process instance: {} - Reason: {}", processInstanceId, deleteReason);

        } catch (LoginException e) {
            throw new WorkflowException("Failed to get service resource resolver", e);
        }
    }

    @Override
    @Nullable
    public ProcessInstance getProcessInstance(@NotNull String processInstanceId) {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            return processInstanceManager.getProcessInstance(processInstanceId, resolver);

        } catch (LoginException e) {
            log.error("Failed to get service resource resolver", e);
            return null;
        } catch (WorkflowException e) {
            log.error("Failed to get process instance: {}", processInstanceId, e);
            return null;
        }
    }

    @Override
    @Nullable
    public ProcessInstance getProcessInstanceByBusinessKey(@NotNull String businessKey) {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            // Query for process instance by business key
            String basePath = "/var/workflow/instances";
            Resource instancesResource = resolver.getResource(basePath);
            if (instancesResource == null) {
                return null;
            }

            // Iterate through instances to find matching business key
            for (Resource child : instancesResource.getChildren()) {
                ProcessInstanceImpl instance = processInstanceManager.getProcessInstance(child.getName(), resolver);
                if (instance != null && businessKey.equals(instance.getBusinessKey())) {
                    return instance;
                }
            }

            return null;

        } catch (LoginException e) {
            log.error("Failed to get service resource resolver", e);
            return null;
        } catch (WorkflowException e) {
            log.error("Failed to get process instance by business key: {}", businessKey, e);
            return null;
        }
    }

    @Override
    @NotNull
    public List<ProcessInstance> getActiveProcessInstances() {
        List<ProcessInstance> instances = new ArrayList<>();
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            String basePath = "/var/workflow/instances";
            Resource instancesResource = resolver.getResource(basePath);
            if (instancesResource == null) {
                return instances;
            }

            // Iterate through all instances and filter active ones
            for (Resource child : instancesResource.getChildren()) {
                // Skip system nodes like rep:policy
                String childName = child.getName();
                if (childName.startsWith("rep:") || childName.startsWith("jcr:")) {
                    continue;
                }

                ProcessInstanceImpl instance = processInstanceManager.getProcessInstance(childName, resolver);
                if (instance != null && !instance.isEnded()) {
                    instances.add(instance);
                }
            }

            log.debug("Found {} active process instances", instances.size());

        } catch (LoginException e) {
            log.error("Failed to get service resource resolver", e);
        } catch (WorkflowException e) {
            log.error("Failed to get active process instances", e);
        }

        return instances;
    }

    @Override
    @NotNull
    public Map<String, Object> getVariables(@NotNull String processInstanceId) {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            ProcessInstanceImpl instance = processInstanceManager.getProcessInstance(processInstanceId, resolver);
            if (instance == null) {
                throw new WorkflowException("Process instance not found: " + processInstanceId);
            }

            return instance.getVariables();

        } catch (LoginException e) {
            throw new WorkflowException("Failed to get service resource resolver", e);
        }
    }

    @Override
    @Nullable
    public Object getVariable(@NotNull String processInstanceId, @NotNull String variableName) {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            ProcessInstanceImpl instance = processInstanceManager.getProcessInstance(processInstanceId, resolver);
            if (instance == null) {
                throw new WorkflowException("Process instance not found: " + processInstanceId);
            }

            return instance.getVariable(variableName);

        } catch (LoginException e) {
            throw new WorkflowException("Failed to get service resource resolver", e);
        }
    }

    @Override
    public void setVariables(@NotNull String processInstanceId, @NotNull Map<String, Object> variables) {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            ProcessInstanceImpl instance = processInstanceManager.getProcessInstance(processInstanceId, resolver);
            if (instance == null) {
                throw new WorkflowException("Process instance not found: " + processInstanceId);
            }

            // Update all variables
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                instance.setVariable(entry.getKey(), entry.getValue());
            }

            // Save to JCR
            processInstanceManager.updateProcessInstance(instance, resolver);

            log.debug("Updated {} variables for process instance: {}", variables.size(), processInstanceId);

        } catch (LoginException e) {
            throw new WorkflowException("Failed to get service resource resolver", e);
        }
    }

    @Override
    public void setVariable(@NotNull String processInstanceId, @NotNull String variableName, @Nullable Object value) {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            ProcessInstanceImpl instance = processInstanceManager.getProcessInstance(processInstanceId, resolver);
            if (instance == null) {
                throw new WorkflowException("Process instance not found: " + processInstanceId);
            }

            // Update variable
            instance.setVariable(variableName, value);

            // Save to JCR
            processInstanceManager.updateProcessInstance(instance, resolver);

            log.debug("Updated variable '{}' for process instance: {}", variableName, processInstanceId);

        } catch (LoginException e) {
            throw new WorkflowException("Failed to get service resource resolver", e);
        }
    }
}
