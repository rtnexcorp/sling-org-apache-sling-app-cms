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
package org.apache.sling.cms.workflow.internal.task;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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
 * Manages user tasks in JCR storage.
 */
@Component(service = TaskManager.class)
public class TaskManager {

    private static final Logger log = LoggerFactory.getLogger(TaskManager.class);
    private static final String TASKS_PATH = "/var/workflow/tasks";

    /**
     * Creates a new task.
     *
     * @param name Task name
     * @param processInstanceId Process instance ID
     * @param activityId Activity ID
     * @param candidateGroup Candidate group
     * @param resolver Resource resolver
     * @return TaskImpl
     * @throws WorkflowException if creation fails
     */
    @NotNull
    public TaskImpl createTask(
            @NotNull String name,
            @NotNull String processInstanceId,
            @NotNull String activityId,
            @Nullable String candidateGroup,
            @NotNull ResourceResolver resolver)
            throws WorkflowException {
        try {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                throw new WorkflowException("Cannot adapt ResourceResolver to JCR Session");
            }

            // Ensure tasks path exists
            Node tasksNode = ensureNodePath(session, TASKS_PATH);

            // Create task node
            String taskId = UUID.randomUUID().toString();
            Node taskNode = tasksNode.addNode(taskId, "nt:unstructured");
            taskNode.setProperty("name", name);
            taskNode.setProperty("processInstanceId", processInstanceId);
            taskNode.setProperty("activityId", activityId);
            if (candidateGroup != null) {
                taskNode.setProperty("candidateGroup", candidateGroup);
            }
            taskNode.setProperty("createTime", Calendar.getInstance());
            taskNode.setProperty("completed", false);

            session.save();
            log.info("Created task: {} for process instance: {}", taskId, processInstanceId);

            return new TaskImpl(taskNode);

        } catch (RepositoryException e) {
            throw new WorkflowException("Failed to create task", e);
        }
    }

    /**
     * Retrieves a task by ID.
     *
     * @param taskId Task ID
     * @param resolver Resource resolver
     * @return TaskImpl or null if not found
     * @throws WorkflowException if retrieval fails
     */
    @Nullable
    public TaskImpl getTask(@NotNull String taskId, @NotNull ResourceResolver resolver) throws WorkflowException {
        try {
            String path = TASKS_PATH + "/" + taskId;
            Resource resource = resolver.getResource(path);
            if (resource == null) {
                return null;
            }

            Node node = resource.adaptTo(Node.class);
            if (node == null) {
                return null;
            }

            return new TaskImpl(node);

        } catch (RepositoryException e) {
            throw new WorkflowException("Failed to retrieve task: " + taskId, e);
        }
    }

    /**
     * Claims a task for a user.
     *
     * @param taskId Task ID
     * @param userId User ID
     * @param resolver Resource resolver
     * @throws WorkflowException if claim fails
     */
    public void claimTask(@NotNull String taskId, @NotNull String userId, @NotNull ResourceResolver resolver)
            throws WorkflowException {
        TaskImpl task = getTask(taskId, resolver);
        if (task == null) {
            throw new WorkflowException("Task not found: " + taskId);
        }

        if (task.isCompleted()) {
            throw new WorkflowException("Cannot claim completed task: " + taskId);
        }

        try {
            task.setAssignee(userId);
            task.getNode().setProperty("assignee", userId);

            Session session = resolver.adaptTo(Session.class);
            if (session != null) {
                session.save();
                log.info("Task {} claimed by user: {}", taskId, userId);
            }

        } catch (RepositoryException e) {
            throw new WorkflowException("Failed to claim task: " + taskId, e);
        }
    }

    /**
     * Completes a task.
     *
     * @param taskId Task ID
     * @param variables Task completion variables
     * @param resolver Resource resolver
     * @return Completion variables
     * @throws WorkflowException if completion fails
     */
    @NotNull
    public Map<String, Object> completeTask(
            @NotNull String taskId, @Nullable Map<String, Object> variables, @NotNull ResourceResolver resolver)
            throws WorkflowException {
        TaskImpl task = getTask(taskId, resolver);
        if (task == null) {
            throw new WorkflowException("Task not found: " + taskId);
        }

        if (task.isCompleted()) {
            throw new WorkflowException("Task already completed: " + taskId);
        }

        try {
            task.setCompleted(true);
            Node taskNode = task.getNode();
            taskNode.setProperty("completed", true);
            taskNode.setProperty("completionTime", Calendar.getInstance());

            // Store completion variables
            if (variables != null && !variables.isEmpty()) {
                Node varsNode = taskNode.addNode("completionVariables", "nt:unstructured");
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    if (entry.getValue() != null) {
                        varsNode.setProperty(entry.getKey(), entry.getValue().toString());
                    }
                }
            }

            Session session = resolver.adaptTo(Session.class);
            if (session != null) {
                session.save();
                log.info("Task {} completed", taskId);
            }

            return variables != null ? variables : Map.of();

        } catch (RepositoryException e) {
            throw new WorkflowException("Failed to complete task: " + taskId, e);
        }
    }

    /**
     * Queries tasks by candidate group.
     *
     * @param candidateGroup Candidate group
     * @param resolver Resource resolver
     * @return List of tasks
     * @throws WorkflowException if query fails
     */
    @NotNull
    public List<TaskImpl> findTasksByCandidateGroup(@NotNull String candidateGroup, @NotNull ResourceResolver resolver)
            throws WorkflowException {
        List<TaskImpl> tasks = new ArrayList<>();

        try {
            Resource tasksResource = resolver.getResource(TASKS_PATH);
            if (tasksResource != null) {
                Node tasksNode = tasksResource.adaptTo(Node.class);
                if (tasksNode != null) {
                    NodeIterator iterator = tasksNode.getNodes();
                    while (iterator.hasNext()) {
                        Node taskNode = iterator.nextNode();
                        if (taskNode.hasProperty("candidateGroup")
                                && candidateGroup.equals(
                                        taskNode.getProperty("candidateGroup").getString())
                                && (!taskNode.hasProperty("completed")
                                        || !taskNode.getProperty("completed").getBoolean())) {
                            tasks.add(new TaskImpl(taskNode));
                        }
                    }
                }
            }

            return tasks;

        } catch (RepositoryException e) {
            throw new WorkflowException("Failed to query tasks by candidate group: " + candidateGroup, e);
        }
    }

    /**
     * Queries tasks by assignee.
     *
     * @param assignee Assignee user ID
     * @param resolver Resource resolver
     * @return List of tasks
     * @throws WorkflowException if query fails
     */
    @NotNull
    public List<TaskImpl> findTasksByAssignee(@NotNull String assignee, @NotNull ResourceResolver resolver)
            throws WorkflowException {
        List<TaskImpl> tasks = new ArrayList<>();

        try {
            Resource tasksResource = resolver.getResource(TASKS_PATH);
            if (tasksResource != null) {
                Node tasksNode = tasksResource.adaptTo(Node.class);
                if (tasksNode != null) {
                    NodeIterator iterator = tasksNode.getNodes();
                    while (iterator.hasNext()) {
                        Node taskNode = iterator.nextNode();
                        if (taskNode.hasProperty("assignee")
                                && assignee.equals(
                                        taskNode.getProperty("assignee").getString())
                                && (!taskNode.hasProperty("completed")
                                        || !taskNode.getProperty("completed").getBoolean())) {
                            tasks.add(new TaskImpl(taskNode));
                        }
                    }
                }
            }

            return tasks;

        } catch (RepositoryException e) {
            throw new WorkflowException("Failed to query tasks by assignee: " + assignee, e);
        }
    }

    /**
     * Queries tasks by process instance ID.
     *
     * @param processInstanceId Process instance ID
     * @param resolver Resource resolver
     * @return List of tasks
     * @throws WorkflowException if query fails
     */
    @NotNull
    public List<TaskImpl> findTasksByProcessInstance(
            @NotNull String processInstanceId, @NotNull ResourceResolver resolver) throws WorkflowException {
        List<TaskImpl> tasks = new ArrayList<>();

        try {
            Resource tasksResource = resolver.getResource(TASKS_PATH);
            if (tasksResource == null) {
                log.debug("Tasks path {} does not exist", TASKS_PATH);
                return tasks;
            }

            Node tasksNode = tasksResource.adaptTo(Node.class);
            if (tasksNode != null) {
                NodeIterator iterator = tasksNode.getNodes();
                log.debug("Searching for tasks in {} for process instance {}", TASKS_PATH, processInstanceId);
                while (iterator.hasNext()) {
                    Node taskNode = iterator.nextNode();
                    if (taskNode.hasProperty("processInstanceId")) {
                        String taskProcId =
                                taskNode.getProperty("processInstanceId").getString();
                        log.trace("Checking task {} with processInstanceId {}", taskNode.getName(), taskProcId);
                        if (processInstanceId.equals(taskProcId)) {
                            tasks.add(new TaskImpl(taskNode));
                            log.debug("Found matching task: {}", taskNode.getName());
                        }
                    }
                }
            }

            log.debug("Found {} tasks for process instance {}", tasks.size(), processInstanceId);
            return tasks;

        } catch (RepositoryException e) {
            throw new WorkflowException("Failed to query tasks by process instance: " + processInstanceId, e);
        }
    }

    /**
     * Queries all active (non-completed) tasks.
     *
     * @param resolver Resource resolver
     * @return List of all active tasks
     * @throws WorkflowException if query fails
     */
    @NotNull
    public List<TaskImpl> findAllTasks(@NotNull ResourceResolver resolver) throws WorkflowException {
        List<TaskImpl> tasks = new ArrayList<>();

        try {
            Resource tasksResource = resolver.getResource(TASKS_PATH);
            if (tasksResource == null) {
                log.debug("Tasks path {} does not exist", TASKS_PATH);
                return tasks;
            }

            log.debug("Found tasks resource at {}, resourceType: {}", TASKS_PATH, tasksResource.getResourceType());

            Node tasksNode = tasksResource.adaptTo(Node.class);
            if (tasksNode != null) {
                log.debug(
                        "Tasks node primary type: {}",
                        tasksNode.getPrimaryNodeType().getName());
                NodeIterator iterator = tasksNode.getNodes();
                log.debug("Searching for all active tasks in {}, hasNext: {}", TASKS_PATH, iterator.hasNext());
                while (iterator.hasNext()) {
                    Node taskNode = iterator.nextNode();
                    log.debug(
                            "Checking task node: {}, primaryType: {}",
                            taskNode.getName(),
                            taskNode.getPrimaryNodeType().getName());
                    // Skip special nodes like rep:policy
                    if (taskNode.getName().startsWith("rep:")
                            || taskNode.getName().startsWith("jcr:")) {
                        log.debug("Skipping special node: {}", taskNode.getName());
                        continue;
                    }
                    // Only return non-completed tasks
                    if (!taskNode.hasProperty("completed")
                            || !taskNode.getProperty("completed").getBoolean()) {
                        tasks.add(new TaskImpl(taskNode));
                        log.debug("Found active task: {}", taskNode.getName());
                    }
                }
            } else {
                log.warn("Could not adapt tasks resource to Node");
            }

            log.debug("Found {} active tasks", tasks.size());
            return tasks;

        } catch (RepositoryException e) {
            throw new WorkflowException("Failed to query all tasks", e);
        }
    }

    /**
     * Queries all unassigned active (non-completed) tasks.
     * These are tasks that don't have an assignee set yet.
     *
     * @param resolver Resource resolver
     * @return List of unassigned active tasks
     * @throws WorkflowException if query fails
     */
    @NotNull
    public List<TaskImpl> findUnassignedTasks(@NotNull ResourceResolver resolver) throws WorkflowException {
        List<TaskImpl> tasks = new ArrayList<>();

        try {
            Resource tasksResource = resolver.getResource(TASKS_PATH);
            if (tasksResource == null) {
                log.debug("Tasks path {} does not exist", TASKS_PATH);
                return tasks;
            }

            Node tasksNode = tasksResource.adaptTo(Node.class);
            if (tasksNode != null) {
                NodeIterator iterator = tasksNode.getNodes();
                while (iterator.hasNext()) {
                    Node taskNode = iterator.nextNode();
                    // Skip special nodes like rep:policy
                    if (taskNode.getName().startsWith("rep:")
                            || taskNode.getName().startsWith("jcr:")) {
                        continue;
                    }
                    // Only return non-completed tasks that have no assignee
                    boolean isCompleted = taskNode.hasProperty("completed")
                            && taskNode.getProperty("completed").getBoolean();
                    boolean hasAssignee = taskNode.hasProperty("assignee")
                            && taskNode.getProperty("assignee").getString() != null
                            && !taskNode.getProperty("assignee").getString().isEmpty();

                    if (!isCompleted && !hasAssignee) {
                        tasks.add(new TaskImpl(taskNode));
                        log.debug("Found unassigned task: {}", taskNode.getName());
                    }
                }
            }

            log.debug("Found {} unassigned tasks", tasks.size());
            return tasks;

        } catch (RepositoryException e) {
            throw new WorkflowException("Failed to query unassigned tasks", e);
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
