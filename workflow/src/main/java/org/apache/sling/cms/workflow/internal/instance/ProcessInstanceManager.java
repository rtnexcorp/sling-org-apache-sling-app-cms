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
package org.apache.sling.cms.workflow.internal.instance;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.workflow.WorkflowException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages process instances in JCR storage.
 */
@Component(service = ProcessInstanceManager.class)
public class ProcessInstanceManager {

    private static final Logger log = LoggerFactory.getLogger(ProcessInstanceManager.class);
    private static final String INSTANCES_PATH = "/var/workflow/instances";

    /**
     * Creates a new process instance in JCR.
     *
     * @param processDefinitionId Process definition ID
     * @param businessKey Optional business key
     * @param variables Initial process variables
     * @param resolver Resource resolver
     * @return ProcessInstanceImpl
     * @throws WorkflowException if creation fails
     */
    @NotNull
    public ProcessInstanceImpl createProcessInstance(
            @NotNull String processDefinitionId,
            @Nullable String businessKey,
            @Nullable Map<String, Object> variables,
            @NotNull ResourceResolver resolver)
            throws WorkflowException {
        try {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                throw new WorkflowException("Cannot adapt ResourceResolver to JCR Session");
            }

            // Ensure instances path exists
            Node instancesNode = ensureNodePath(session, INSTANCES_PATH);

            // Create instance node
            String instanceId = UUID.randomUUID().toString();
            Node instanceNode = instancesNode.addNode(instanceId, "nt:unstructured");
            instanceNode.setProperty("processDefinitionId", processDefinitionId);
            if (businessKey != null) {
                instanceNode.setProperty("businessKey", businessKey);
            }
            instanceNode.setProperty("startTime", Calendar.getInstance());
            instanceNode.setProperty("ended", false);

            // Create variables node if variables provided
            if (variables != null && !variables.isEmpty()) {
                Node varsNode = instanceNode.addNode("variables", "nt:unstructured");
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    if (entry.getValue() != null) {
                        varsNode.setProperty(entry.getKey(), entry.getValue().toString());
                    }
                }
            }

            session.save();
            log.info("Created process instance: {}", instanceId);

            return new ProcessInstanceImpl(instanceNode);

        } catch (RepositoryException e) {
            throw new WorkflowException("Failed to create process instance", e);
        }
    }

    /**
     * Retrieves a process instance by ID.
     *
     * @param instanceId Process instance ID
     * @param resolver Resource resolver
     * @return ProcessInstanceImpl or null if not found
     * @throws WorkflowException if retrieval fails
     */
    @Nullable
    public ProcessInstanceImpl getProcessInstance(@NotNull String instanceId, @NotNull ResourceResolver resolver)
            throws WorkflowException {
        try {
            String path = INSTANCES_PATH + "/" + instanceId;
            Resource resource = resolver.getResource(path);
            if (resource == null) {
                return null;
            }

            Node node = resource.adaptTo(Node.class);
            if (node == null) {
                return null;
            }

            return new ProcessInstanceImpl(node);

        } catch (RepositoryException e) {
            throw new WorkflowException("Failed to retrieve process instance: " + instanceId, e);
        }
    }

    /**
     * Updates a process instance in JCR.
     *
     * @param instance Process instance to update
     * @param resolver Resource resolver
     * @throws WorkflowException if update fails
     */
    public void updateProcessInstance(@NotNull ProcessInstanceImpl instance, @NotNull ResourceResolver resolver)
            throws WorkflowException {
        try {
            Node node = instance.getNode();
            if (instance.getCurrentActivityId() != null) {
                node.setProperty("currentActivityId", instance.getCurrentActivityId());
            }
            node.setProperty("ended", instance.isEnded());

            // Update variables
            Node varsNode;
            if (node.hasNode("variables")) {
                varsNode = node.getNode("variables");
            } else {
                varsNode = node.addNode("variables", "nt:unstructured");
            }

            Map<String, Object> variables = instance.getVariables();
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                if (entry.getValue() != null) {
                    varsNode.setProperty(entry.getKey(), entry.getValue().toString());
                }
            }

            Session session = resolver.adaptTo(Session.class);
            if (session != null) {
                session.save();
                log.debug("Updated process instance: {}", instance.getId());
            }

        } catch (RepositoryException e) {
            throw new WorkflowException("Failed to update process instance: " + instance.getId(), e);
        }
    }

    /**
     * Deletes a process instance from JCR.
     *
     * @param instanceId Process instance ID
     * @param resolver Resource resolver
     * @throws WorkflowException if deletion fails
     */
    public void deleteProcessInstance(@NotNull String instanceId, @NotNull ResourceResolver resolver)
            throws WorkflowException {
        try {
            String path = INSTANCES_PATH + "/" + instanceId;
            Resource resource = resolver.getResource(path);
            if (resource != null) {
                Node node = resource.adaptTo(Node.class);
                if (node != null) {
                    node.remove();
                    Session session = resolver.adaptTo(Session.class);
                    if (session != null) {
                        session.save();
                        log.info("Deleted process instance: {}", instanceId);
                    }
                }
            }

        } catch (RepositoryException e) {
            throw new WorkflowException("Failed to delete process instance: " + instanceId, e);
        }
    }

    private Node ensureNodePath(Session session, String path) throws RepositoryException {
        String[] parts = path.substring(1).split("/");
        Node current = session.getRootNode();

        for (String part : parts) {
            if (!current.hasNode(part)) {
                current = current.addNode(part, "nt:unstructured");
            } else {
                current = current.getNode(part);
            }
        }

        return current;
    }
}
