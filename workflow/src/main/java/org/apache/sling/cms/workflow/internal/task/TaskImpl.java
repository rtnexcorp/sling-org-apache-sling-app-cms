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
import javax.jcr.RepositoryException;

import java.util.Date;

import org.apache.sling.cms.workflow.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of Task backed by JCR storage.
 */
public class TaskImpl implements Task {

    private final String id;
    private final String name;
    private final String processInstanceId;
    private final String activityId;
    private String assignee;
    private String candidateGroup;
    private final Date createTime;
    private boolean completed;
    private final Node node;

    public TaskImpl(@NotNull Node node) throws RepositoryException {
        this.node = node;
        this.id = node.getName(); // Use node name (task UUID), not JCR identifier
        this.name = node.getProperty("name").getString();
        this.processInstanceId = node.getProperty("processInstanceId").getString();
        this.activityId = node.getProperty("activityId").getString();
        this.assignee =
                node.hasProperty("assignee") ? node.getProperty("assignee").getString() : null;
        this.candidateGroup = node.hasProperty("candidateGroup")
                ? node.getProperty("candidateGroup").getString()
                : null;
        this.createTime = node.getProperty("createTime").getDate().getTime();
        this.completed =
                node.hasProperty("completed") && node.getProperty("completed").getBoolean();
    }

    public TaskImpl(
            @NotNull String id,
            @NotNull String name,
            @NotNull String processInstanceId,
            @NotNull String activityId,
            @Nullable String candidateGroup,
            @NotNull Node node) {
        this.id = id;
        this.name = name;
        this.processInstanceId = processInstanceId;
        this.activityId = activityId;
        this.candidateGroup = candidateGroup;
        this.createTime = new Date();
        this.completed = false;
        this.node = node;
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
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @NotNull
    public String getActivityId() {
        return activityId;
    }

    @Override
    @Nullable
    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(@Nullable String assignee) {
        this.assignee = assignee;
    }

    @Override
    @Nullable
    public String getCandidateGroup() {
        return candidateGroup;
    }

    @Override
    @NotNull
    public Date getCreateTime() {
        return createTime;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    @Nullable
    public String getDescription() {
        // MVP: Not stored in JCR yet
        return null;
    }

    @Override
    @Nullable
    public String getOwner() {
        // MVP: Same as assignee
        return assignee;
    }

    @Override
    @NotNull
    public String getProcessDefinitionKey() {
        // MVP: Would need to load from process instance
        return "contentApproval";
    }

    @Override
    @Nullable
    public Date getDueDate() {
        // MVP: Not implemented yet
        return null;
    }

    @Override
    public int getPriority() {
        // MVP: Default priority
        return 50;
    }

    @Override
    @NotNull
    public String getTaskDefinitionKey() {
        // Use activity ID as task definition key
        return activityId;
    }

    @NotNull
    public Node getNode() {
        return node;
    }
}
