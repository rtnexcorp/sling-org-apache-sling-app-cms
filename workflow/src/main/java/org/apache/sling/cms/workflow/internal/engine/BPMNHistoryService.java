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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.cms.workflow.HistoricProcessInstance;
import org.apache.sling.cms.workflow.HistoricTaskInstance;
import org.apache.sling.cms.workflow.HistoryService;
import org.apache.sling.cms.workflow.WorkflowException;
import org.apache.sling.cms.workflow.internal.history.HistoricProcessInstanceImpl;
import org.apache.sling.cms.workflow.internal.history.HistoricTaskInstanceImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of HistoryService for SimpleBPMN engine.
 * Provides JCR-based persistence for completed process instances and tasks.
 */
@Component(service = HistoryService.class)
public class SimpleBPMNHistoryService implements HistoryService {

    private static final Logger log = LoggerFactory.getLogger(SimpleBPMNHistoryService.class);
    private static final String SERVICE_USER = "sling-cms-workflow";
    private static final String HISTORY_BASE_PATH = "/var/workflow/history";
    private static final String PROCESSES_PATH = HISTORY_BASE_PATH + "/processes";
    private static final String TASKS_PATH = HISTORY_BASE_PATH + "/tasks";

    @Reference
    private ResourceResolverFactory resolverFactory;

    /**
     * Store a completed process instance to history.
     *
     * @param processInstanceId Process instance ID
     * @param processDefinitionKey Process definition key
     * @param businessKey Business key (optional)
     * @param startTime Start time
     * @param endTime End time
     */
    public void storeCompletedProcessInstance(
            @NotNull String processInstanceId,
            @NotNull String processDefinitionKey,
            @Nullable String businessKey,
            @NotNull Date startTime,
            @NotNull Date endTime) {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                throw new WorkflowException("Could not get JCR session");
            }

            // Ensure processes path exists
            ensurePathExists(session, PROCESSES_PATH);

            // Create historic process instance node
            Node processesNode = session.getNode(PROCESSES_PATH);
            Node historyNode = processesNode.addNode(processInstanceId, "nt:unstructured");
            historyNode.setProperty("processDefinitionKey", processDefinitionKey);
            if (businessKey != null) {
                historyNode.setProperty("businessKey", businessKey);
            }

            Calendar startCal = Calendar.getInstance();
            startCal.setTime(startTime);
            historyNode.setProperty("startTime", startCal);

            Calendar endCal = Calendar.getInstance();
            endCal.setTime(endTime);
            historyNode.setProperty("endTime", endCal);

            session.save();
            log.info("Stored completed process instance to history: {}", processInstanceId);

        } catch (LoginException e) {
            log.error("Failed to get service resource resolver", e);
        } catch (RepositoryException e) {
            log.error("Failed to store process instance history: {}", processInstanceId, e);
        }
    }

    /**
     * Store a completed task instance to history.
     *
     * @param taskId Task ID
     * @param name Task name
     * @param assignee Assignee
     * @param processInstanceId Process instance ID
     * @param startTime Start time
     * @param endTime End time
     */
    public void storeCompletedTask(
            @NotNull String taskId,
            @NotNull String name,
            @Nullable String assignee,
            @NotNull String processInstanceId,
            @NotNull Date startTime,
            @NotNull Date endTime) {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                throw new WorkflowException("Could not get JCR session");
            }

            // Ensure tasks path exists
            ensurePathExists(session, TASKS_PATH);

            // Create historic task instance node
            Node tasksNode = session.getNode(TASKS_PATH);
            Node historyNode = tasksNode.addNode(taskId, "nt:unstructured");
            historyNode.setProperty("name", name);
            if (assignee != null) {
                historyNode.setProperty("assignee", assignee);
            }
            historyNode.setProperty("processInstanceId", processInstanceId);

            Calendar startCal = Calendar.getInstance();
            startCal.setTime(startTime);
            historyNode.setProperty("startTime", startCal);

            Calendar endCal = Calendar.getInstance();
            endCal.setTime(endTime);
            historyNode.setProperty("endTime", endCal);

            session.save();
            log.debug("Stored completed task to history: {}", taskId);

        } catch (LoginException e) {
            log.error("Failed to get service resource resolver", e);
        } catch (RepositoryException e) {
            log.error("Failed to store task history: {}", taskId, e);
        }
    }

    private void ensurePathExists(Session session, String path) throws RepositoryException {
        if (!session.nodeExists(path)) {
            String[] parts = path.substring(1).split("/");
            Node current = session.getRootNode();
            for (String part : parts) {
                if (!current.hasNode(part)) {
                    current = current.addNode(part, "sling:Folder");
                } else {
                    current = current.getNode(part);
                }
            }
            session.save();
        }
    }

    @Override
    @NotNull
    public List<HistoricProcessInstance> getCompletedProcessInstances(@NotNull String processDefinitionKey) {
        List<HistoricProcessInstance> instances = new ArrayList<>();
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            Session session = resolver.adaptTo(Session.class);
            if (session == null || !session.nodeExists(PROCESSES_PATH)) {
                return instances;
            }

            Node processesNode = session.getNode(PROCESSES_PATH);
            NodeIterator iterator = processesNode.getNodes();

            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                if (node.hasProperty("processDefinitionKey")) {
                    String key = node.getProperty("processDefinitionKey").getString();
                    if (processDefinitionKey.equals(key)) {
                        instances.add(new HistoricProcessInstanceImpl(node));
                    }
                }
            }

            log.debug(
                    "Found {} completed process instances for definition: {}", instances.size(), processDefinitionKey);

        } catch (LoginException e) {
            log.error("Failed to get service resource resolver", e);
        } catch (RepositoryException e) {
            log.error("Failed to query completed process instances", e);
        }

        return instances;
    }

    @Override
    @NotNull
    public List<HistoricTaskInstance> getCompletedTasks(@NotNull String processInstanceId) {
        List<HistoricTaskInstance> tasks = new ArrayList<>();
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            Session session = resolver.adaptTo(Session.class);
            if (session == null || !session.nodeExists(TASKS_PATH)) {
                return tasks;
            }

            Node tasksNode = session.getNode(TASKS_PATH);
            NodeIterator iterator = tasksNode.getNodes();

            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                if (node.hasProperty("processInstanceId")) {
                    String pid = node.getProperty("processInstanceId").getString();
                    if (processInstanceId.equals(pid)) {
                        tasks.add(new HistoricTaskInstanceImpl(node));
                    }
                }
            }

            log.debug("Found {} completed tasks for process instance: {}", tasks.size(), processInstanceId);

        } catch (LoginException e) {
            log.error("Failed to get service resource resolver", e);
        } catch (RepositoryException e) {
            log.error("Failed to query completed tasks", e);
        }

        return tasks;
    }

    @Override
    @Nullable
    public HistoricProcessInstance getHistoricProcessInstance(@NotNull String processInstanceId) {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                return null;
            }

            String nodePath = PROCESSES_PATH + "/" + processInstanceId;
            if (session.nodeExists(nodePath)) {
                Node node = session.getNode(nodePath);
                return new HistoricProcessInstanceImpl(node);
            }

        } catch (LoginException e) {
            log.error("Failed to get service resource resolver", e);
        } catch (RepositoryException e) {
            log.error("Failed to get historic process instance: {}", processInstanceId, e);
        }

        return null;
    }

    @Override
    @Nullable
    public HistoricTaskInstance getHistoricTaskInstance(@NotNull String taskId) {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                return null;
            }

            String nodePath = TASKS_PATH + "/" + taskId;
            if (session.nodeExists(nodePath)) {
                Node node = session.getNode(nodePath);
                return new HistoricTaskInstanceImpl(node);
            }

        } catch (LoginException e) {
            log.error("Failed to get service resource resolver", e);
        } catch (RepositoryException e) {
            log.error("Failed to get historic task instance: {}", taskId, e);
        }

        return null;
    }

    @Override
    @NotNull
    public List<HistoricProcessInstance> getAllCompletedProcessInstances() {
        List<HistoricProcessInstance> instances = new ArrayList<>();
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            Session session = resolver.adaptTo(Session.class);
            if (session == null || !session.nodeExists(PROCESSES_PATH)) {
                return instances;
            }

            Node processesNode = session.getNode(PROCESSES_PATH);
            NodeIterator iterator = processesNode.getNodes();

            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                instances.add(new HistoricProcessInstanceImpl(node));
            }

            log.debug("Found {} total completed process instances", instances.size());

        } catch (LoginException e) {
            log.error("Failed to get service resource resolver", e);
        } catch (RepositoryException e) {
            log.error("Failed to query all completed process instances", e);
        }

        return instances;
    }

    @Override
    public void deleteHistoricProcessInstance(@NotNull String processInstanceId) {
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(serviceParams)) {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                throw new WorkflowException("Could not get JCR session");
            }

            String nodePath = PROCESSES_PATH + "/" + processInstanceId;
            if (session.nodeExists(nodePath)) {
                Node node = session.getNode(nodePath);
                node.remove();
                session.save();
                log.info("Deleted historic process instance: {}", processInstanceId);
            }

        } catch (LoginException e) {
            log.error("Failed to get service resource resolver", e);
        } catch (RepositoryException e) {
            log.error("Failed to delete historic process instance: {}", processInstanceId, e);
        }
    }
}
