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
package org.apache.sling.cms.workflow.internal;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.workflow.ProcessDefinition;
import org.apache.sling.cms.workflow.ProcessInstance;
import org.apache.sling.cms.workflow.RuntimeService;
import org.apache.sling.cms.workflow.WorkflowException;
import org.apache.sling.cms.workflow.internal.engine.BPMNRepositoryService;
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
 * Implementation of RuntimeService for managing process instances.
 *
 * <p>Provides operations for starting, querying, and managing workflow process instances.</p>
 */
@Component(service = RuntimeService.class)
public class RuntimeServiceImpl implements RuntimeService {

    private static final Logger log = LoggerFactory.getLogger(RuntimeServiceImpl.class);
    private static final String INSTANCES_PATH = "/var/workflow/instances";

    @Reference
    private ProcessInstanceManager processInstanceManager;

    @Reference
    private BPMNRepositoryService repositoryService;

    @Reference
    private ProcessExecutor processExecutor;

    // ThreadLocal context for ResourceResolver (MVP approach)
    private ThreadLocal<ResourceResolver> resolverContext = new ThreadLocal<>();

    @Override
    public @NotNull ProcessInstance startProcessInstanceByKey(
            @NotNull String processDefinitionKey, @Nullable Map<String, Object> variables) {
        return startProcessInstanceByKey(processDefinitionKey, null, variables);
    }

    @Override
    public @NotNull ProcessInstance startProcessInstanceByKey(
            @NotNull String processDefinitionKey,
            @Nullable String businessKey,
            @Nullable Map<String, Object> variables) {
        try {
            ResourceResolver resolver = getResolver();

            // Get process definition by key
            ProcessDefinition processDefinition = repositoryService.getProcessDefinitionByKey(processDefinitionKey);
            if (processDefinition == null) {
                throw new WorkflowException("Process definition not found: " + processDefinitionKey);
            }

            // Create process instance
            ProcessInstanceImpl processInstance = processInstanceManager.createProcessInstance(
                    processDefinition.getId(), businessKey, variables, resolver);

            log.info("Started process instance {} for definition {}", processInstance.getId(), processDefinitionKey);

            // Start execution
            ProcessDefinitionImpl processDefImpl =
                    repositoryService.getProcessDefinition(processDefinition.getId(), resolver);
            if (processDefImpl != null) {
                processExecutor.executeProcess(processDefImpl, processInstance, resolver);
            }

            return processInstance;

        } catch (WorkflowException e) {
            log.error("Failed to start process instance for key: {}", processDefinitionKey, e);
            throw new RuntimeException("Failed to start process instance", e);
        }
    }

    @Override
    public void deleteProcessInstance(@NotNull String processInstanceId, @NotNull String deleteReason) {
        try {
            ResourceResolver resolver = getResolver();

            ProcessInstanceImpl instance = processInstanceManager.getProcessInstance(processInstanceId, resolver);
            if (instance == null) {
                throw new WorkflowException("Process instance not found: " + processInstanceId);
            }

            // Mark as ended before deletion
            instance.setEnded(true);
            processInstanceManager.updateProcessInstance(instance, resolver);

            // Delete from JCR
            processInstanceManager.deleteProcessInstance(processInstanceId, resolver);

            log.info("Deleted process instance {} - Reason: {}", processInstanceId, deleteReason);

        } catch (WorkflowException e) {
            log.error("Failed to delete process instance: {}", processInstanceId, e);
            throw new RuntimeException("Failed to delete process instance", e);
        }
    }

    @Override
    public @Nullable ProcessInstance getProcessInstance(@NotNull String processInstanceId) {
        try {
            ResourceResolver resolver = getResolver();
            return processInstanceManager.getProcessInstance(processInstanceId, resolver);

        } catch (WorkflowException e) {
            log.error("Failed to retrieve process instance: {}", processInstanceId, e);
            return null;
        }
    }

    @Override
    public @Nullable ProcessInstance getProcessInstanceByBusinessKey(@NotNull String businessKey) {
        try {
            ResourceResolver resolver = getResolver();
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                return null;
            }

            // Query for process instance by business key
            String queryString = String.format(
                    "SELECT * FROM [nt:unstructured] AS instance "
                            + "WHERE ISDESCENDANTNODE(instance, '%s') "
                            + "AND instance.[businessKey] = '%s' "
                            + "AND instance.[ended] = false",
                    INSTANCES_PATH, businessKey.replace("'", "''"));

            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            QueryResult result = query.execute();

            NodeIterator nodes = result.getNodes();
            if (nodes.hasNext()) {
                Node instanceNode = nodes.nextNode();
                return new ProcessInstanceImpl(instanceNode);
            }

            return null;

        } catch (RepositoryException | WorkflowException e) {
            log.error("Failed to retrieve process instance by business key: {}", businessKey, e);
            return null;
        }
    }

    @Override
    public @NotNull List<ProcessInstance> getActiveProcessInstances() {
        try {
            ResourceResolver resolver = getResolver();
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                return Collections.emptyList();
            }

            // Query for all active (not ended) process instances
            String queryString = String.format(
                    "SELECT * FROM [nt:unstructured] AS instance "
                            + "WHERE ISDESCENDANTNODE(instance, '%s') "
                            + "AND instance.[ended] = false",
                    INSTANCES_PATH);

            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            QueryResult result = query.execute();

            List<ProcessInstance> instances = new ArrayList<>();
            NodeIterator nodes = result.getNodes();
            while (nodes.hasNext()) {
                Node instanceNode = nodes.nextNode();
                instances.add(new ProcessInstanceImpl(instanceNode));
            }

            log.debug("Found {} active process instances", instances.size());
            return instances;

        } catch (RepositoryException | WorkflowException e) {
            log.error("Failed to retrieve active process instances", e);
            return Collections.emptyList();
        }
    }

    @Override
    public @NotNull Map<String, Object> getVariables(@NotNull String processInstanceId) {
        try {
            ResourceResolver resolver = getResolver();
            ProcessInstanceImpl instance = processInstanceManager.getProcessInstance(processInstanceId, resolver);
            if (instance == null) {
                throw new WorkflowException("Process instance not found: " + processInstanceId);
            }

            return instance.getVariables();

        } catch (WorkflowException e) {
            log.error("Failed to retrieve variables for process instance: {}", processInstanceId, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public @Nullable Object getVariable(@NotNull String processInstanceId, @NotNull String variableName) {
        try {
            ResourceResolver resolver = getResolver();
            ProcessInstanceImpl instance = processInstanceManager.getProcessInstance(processInstanceId, resolver);
            if (instance == null) {
                return null;
            }

            return instance.getVariable(variableName);

        } catch (WorkflowException e) {
            log.error("Failed to retrieve variable {} for process instance: {}", variableName, processInstanceId, e);
            return null;
        }
    }

    @Override
    public void setVariables(@NotNull String processInstanceId, @NotNull Map<String, Object> variables) {
        try {
            ResourceResolver resolver = getResolver();
            ProcessInstanceImpl instance = processInstanceManager.getProcessInstance(processInstanceId, resolver);
            if (instance == null) {
                throw new WorkflowException("Process instance not found: " + processInstanceId);
            }

            // Set all variables
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                instance.setVariable(entry.getKey(), entry.getValue());
            }

            // Update in JCR
            processInstanceManager.updateProcessInstance(instance, resolver);

            log.debug("Set {} variables for process instance: {}", variables.size(), processInstanceId);

        } catch (WorkflowException e) {
            log.error("Failed to set variables for process instance: {}", processInstanceId, e);
            throw new RuntimeException("Failed to set process variables", e);
        }
    }

    @Override
    public void setVariable(@NotNull String processInstanceId, @NotNull String variableName, @Nullable Object value) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(variableName, value);
        setVariables(processInstanceId, variables);
    }

    /**
     * Sets the ResourceResolver context for API calls.
     * MVP helper method - in production, use different approach.
     *
     * @param resolver Resource resolver
     */
    public void setResolverContext(@NotNull ResourceResolver resolver) {
        resolverContext.set(resolver);
    }

    /**
     * Clears the ResourceResolver context.
     */
    public void clearResolverContext() {
        resolverContext.remove();
    }

    /**
     * Gets the current ResourceResolver from thread-local context.
     *
     * @return ResourceResolver
     * @throws IllegalStateException if resolver not set
     */
    private ResourceResolver getResolver() {
        ResourceResolver resolver = resolverContext.get();
        if (resolver == null) {
            throw new IllegalStateException("ResourceResolver not set in context");
        }
        return resolver;
    }
}
