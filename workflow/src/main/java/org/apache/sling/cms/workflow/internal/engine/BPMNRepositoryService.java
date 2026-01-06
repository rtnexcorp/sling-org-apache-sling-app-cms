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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.cms.workflow.Deployment;
import org.apache.sling.cms.workflow.ProcessDefinition;
import org.apache.sling.cms.workflow.RepositoryService;
import org.apache.sling.cms.workflow.WorkflowException;
import org.apache.sling.cms.workflow.internal.model.ProcessDefinitionImpl;
import org.apache.sling.cms.workflow.internal.parser.BPMNParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BPMN implementation of RepositoryService.
 */
@Component(service = {RepositoryService.class, BPMNRepositoryService.class})
public class BPMNRepositoryService implements RepositoryService {

    private static final Logger log = LoggerFactory.getLogger(BPMNRepositoryService.class);
    // Store workflow definitions in /etc (persistent configuration, backed up)
    private static final String DEFINITIONS_PATH = "/etc/workflow/definitions";
    private static final String SERVICE_USER = "sling-cms-workflow";

    @Reference
    private BPMNParser bpmnParser;

    @Reference
    private ResourceResolverFactory resolverFactory;

    private final Map<String, ProcessDefinitionImpl> definitionCache = new ConcurrentHashMap<>();

    @Activate
    protected void activate() {
        // Load all process definitions from JCR on startup
        loadDefinitionsFromJCR();
    }

    /**
     * Load all process definitions from JCR into cache on startup.
     */
    private void loadDefinitionsFromJCR() {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            Resource definitionsResource = resolver.getResource(DEFINITIONS_PATH);
            if (definitionsResource == null) {
                log.info("No process definitions found in JCR at {}", DEFINITIONS_PATH);
                return;
            }

            Node definitionsNode = definitionsResource.adaptTo(Node.class);
            if (definitionsNode == null) {
                return;
            }

            NodeIterator nodes = definitionsNode.getNodes();
            int loadedCount = 0;

            while (nodes.hasNext()) {
                Node defNode = nodes.nextNode();
                try {
                    if (defNode.hasProperty("bpmn")) {
                        String bpmnContent = defNode.getProperty("bpmn").getString();
                        String key = defNode.getProperty("processDefinitionKey").getString();

                        ProcessDefinitionImpl processDefinition = bpmnParser.parse(
                                key, new ByteArrayInputStream(bpmnContent.getBytes(StandardCharsets.UTF_8)));

                        definitionCache.put(processDefinition.getKey(), processDefinition);
                        definitionCache.put(processDefinition.getId(), processDefinition);

                        loadedCount++;
                        log.debug(
                                "Loaded process definition: {} (version {})",
                                processDefinition.getKey(),
                                processDefinition.getVersion());
                    }
                } catch (Exception e) {
                    log.error("Failed to load process definition from node: {}", defNode.getPath(), e);
                }
            }

            log.info("Loaded {} process definitions from JCR", loadedCount);

        } catch (LoginException e) {
            log.error("Failed to get service resource resolver for loading process definitions", e);
        } catch (RepositoryException e) {
            log.error("Failed to load process definitions from JCR", e);
        }
    }

    @Override
    @NotNull
    public Deployment deploy(@NotNull String name, @NotNull InputStream bpmnXml) {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            // Parse BPMN
            String bpmnContent = IOUtils.toString(bpmnXml, StandardCharsets.UTF_8);
            ProcessDefinitionImpl processDefinition =
                    bpmnParser.parse(name, new ByteArrayInputStream(bpmnContent.getBytes(StandardCharsets.UTF_8)));

            // Store in JCR
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                throw new WorkflowException("Cannot adapt ResourceResolver to JCR Session");
            }

            Node definitionsNode = ensureNodePath(session, DEFINITIONS_PATH);

            // Use process definition key as node name
            String nodeName = processDefinition.getKey();
            Node defNode;

            if (definitionsNode.hasNode(nodeName)) {
                // Update existing definition
                defNode = definitionsNode.getNode(nodeName);
                log.info("Updating existing process definition: {}", nodeName);
            } else {
                // Create new definition
                defNode = definitionsNode.addNode(nodeName, "nt:unstructured");
                log.info("Creating new process definition: {}", nodeName);
            }

            defNode.setProperty("processDefinitionKey", processDefinition.getKey());
            defNode.setProperty("processDefinitionId", processDefinition.getId());
            defNode.setProperty("name", processDefinition.getName());
            defNode.setProperty("version", processDefinition.getVersion());
            defNode.setProperty("bpmn", bpmnContent);
            defNode.setProperty("deploymentDate", Calendar.getInstance());

            session.save();

            // Cache definition
            definitionCache.put(processDefinition.getKey(), processDefinition);
            definitionCache.put(processDefinition.getId(), processDefinition);

            log.info(
                    "Deployed process definition: {} (version {}) to JCR",
                    processDefinition.getKey(),
                    processDefinition.getVersion());

            return new DeploymentImpl(
                    processDefinition.getKey(),
                    processDefinition.getName(),
                    Calendar.getInstance().getTime());

        } catch (Exception e) {
            throw new WorkflowException("Failed to deploy process definition", e);
        }
    }

    @Override
    @NotNull
    public Deployment deployFromResource(@NotNull String name, @NotNull String resourcePath) {
        // MVP: Not implemented yet
        throw new WorkflowException("deployFromResource not implemented yet");
    }

    @Override
    @NotNull
    public List<ProcessDefinition> getProcessDefinitions() {
        // Return all cached definitions
        return new ArrayList<>(definitionCache.values());
    }

    @Override
    @Nullable
    public ProcessDefinition getProcessDefinitionByKey(@NotNull String processDefinitionKey) {
        return definitionCache.get(processDefinitionKey);
    }

    @Override
    @Nullable
    public ProcessDefinition getProcessDefinitionById(@NotNull String processDefinitionId) {
        return definitionCache.get(processDefinitionId);
    }

    @Override
    public void deleteDeployment(@NotNull String deploymentId, boolean cascade) {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            String path = DEFINITIONS_PATH + "/" + deploymentId;
            Resource resource = resolver.getResource(path);

            if (resource != null) {
                Session session = resolver.adaptTo(Session.class);
                if (session != null) {
                    Node node = resource.adaptTo(Node.class);
                    if (node != null) {
                        node.remove();
                        session.save();
                        log.info("Deleted process definition from JCR: {}", deploymentId);
                    }
                }
            }

            // Remove from cache
            definitionCache.remove(deploymentId);

            // Also try to remove by ID if it's cached differently
            ProcessDefinitionImpl def = definitionCache.values().stream()
                    .filter(d -> d.getKey().equals(deploymentId) || d.getId().equals(deploymentId))
                    .findFirst()
                    .orElse(null);
            if (def != null) {
                definitionCache.remove(def.getKey());
                definitionCache.remove(def.getId());
            }

        } catch (Exception e) {
            log.error("Failed to delete deployment: {}", deploymentId, e);
            throw new WorkflowException("Failed to delete deployment: " + deploymentId, e);
        }
    }

    @Override
    @Nullable
    public String getProcessModel(@NotNull String processDefinitionId) {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            // Try to find by ID or key
            String path = DEFINITIONS_PATH + "/" + processDefinitionId;
            Resource resource = resolver.getResource(path);

            if (resource == null) {
                // Maybe it's an ID, try to find by matching in cache first
                ProcessDefinitionImpl def = definitionCache.get(processDefinitionId);
                if (def != null) {
                    path = DEFINITIONS_PATH + "/" + def.getKey();
                    resource = resolver.getResource(path);
                }
            }

            if (resource != null) {
                Node node = resource.adaptTo(Node.class);
                if (node != null && node.hasProperty("bpmn")) {
                    return node.getProperty("bpmn").getString();
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Failed to get process model: {}", processDefinitionId, e);
            return null;
        }
    }

    /**
     * Retrieves a process definition by key or ID.
     * Helper method for internal use with ResourceResolver.
     *
     * @param keyOrId Process definition key or ID
     * @param resolver Resource resolver
     * @return ProcessDefinitionImpl or null
     * @throws WorkflowException if retrieval fails
     */
    @Nullable
    public ProcessDefinitionImpl getProcessDefinition(@NotNull String keyOrId, @NotNull ResourceResolver resolver)
            throws WorkflowException {
        // Check cache first
        ProcessDefinitionImpl cached = definitionCache.get(keyOrId);
        if (cached != null) {
            return cached;
        }

        // Load from JCR if not in cache
        try {
            String path = DEFINITIONS_PATH + "/" + keyOrId;
            Resource resource = resolver.getResource(path);

            if (resource == null) {
                return null;
            }

            Node node = resource.adaptTo(Node.class);
            if (node == null || !node.hasProperty("bpmn")) {
                return null;
            }

            String bpmnContent = node.getProperty("bpmn").getString();
            ProcessDefinitionImpl processDefinition =
                    bpmnParser.parse(keyOrId, new ByteArrayInputStream(bpmnContent.getBytes(StandardCharsets.UTF_8)));

            // Cache it
            definitionCache.put(processDefinition.getKey(), processDefinition);
            definitionCache.put(processDefinition.getId(), processDefinition);

            return processDefinition;

        } catch (Exception e) {
            throw new WorkflowException("Failed to retrieve process definition: " + keyOrId, e);
        }
    }

    /**
     * Ensures a node path exists in JCR, creating nodes as needed.
     *
     * @param session JCR session
     * @param path Path to ensure
     * @return The node at the path
     * @throws RepositoryException if node creation fails
     */
    private Node ensureNodePath(Session session, String path) throws RepositoryException {
        if (path == null || path.isEmpty() || !path.startsWith("/")) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }

        String[] parts = path.substring(1).split("/");
        Node current = session.getRootNode();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!current.hasNode(part)) {
                current = current.addNode(part, "nt:unstructured");
            } else {
                current = current.getNode(part);
            }
        }

        return current;
    }

    private static class DeploymentImpl implements Deployment {
        private final String id;
        private final String name;
        private final Date deploymentTime;

        DeploymentImpl(String id, String name, Date deploymentTime) {
            this.id = id;
            this.name = name;
            this.deploymentTime = deploymentTime;
        }

        @Override
        @NotNull
        public String getId() {
            return id;
        }

        @Override
        @NotNull
        public String getName() {
            return name;
        }

        @Override
        @NotNull
        public Date getDeploymentTime() {
            return deploymentTime;
        }
    }
}
