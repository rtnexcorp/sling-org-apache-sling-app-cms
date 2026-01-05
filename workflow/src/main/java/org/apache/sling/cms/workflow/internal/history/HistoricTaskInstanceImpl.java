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
package org.apache.sling.cms.workflow.internal.history;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import java.util.Date;

import org.apache.sling.cms.workflow.HistoricTaskInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of HistoricTaskInstance backed by JCR storage.
 * Stored at: /var/workflow/history/tasks/{taskId}
 */
public class HistoricTaskInstanceImpl implements HistoricTaskInstance {

    private final String id;
    private final String name;
    private final String assignee;
    private final String processInstanceId;
    private final Date startTime;
    private final Date endTime;
    private final String deleteReason;

    public HistoricTaskInstanceImpl(@NotNull Node node) throws RepositoryException {
        this.id = node.getName();
        this.name = node.getProperty("name").getString();
        this.assignee =
                node.hasProperty("assignee") ? node.getProperty("assignee").getString() : null;
        this.processInstanceId = node.getProperty("processInstanceId").getString();
        this.startTime = node.getProperty("startTime").getDate().getTime();
        this.endTime = node.hasProperty("endTime")
                ? node.getProperty("endTime").getDate().getTime()
                : null;
        this.deleteReason = node.hasProperty("deleteReason")
                ? node.getProperty("deleteReason").getString()
                : null;
    }

    public HistoricTaskInstanceImpl(
            @NotNull String id,
            @NotNull String name,
            @Nullable String assignee,
            @NotNull String processInstanceId,
            @NotNull Date startTime,
            @NotNull Date endTime) {
        this(id, name, assignee, processInstanceId, startTime, endTime, null);
    }

    public HistoricTaskInstanceImpl(
            @NotNull String id,
            @NotNull String name,
            @Nullable String assignee,
            @NotNull String processInstanceId,
            @NotNull Date startTime,
            @NotNull Date endTime,
            @Nullable String deleteReason) {
        this.id = id;
        this.name = name;
        this.assignee = assignee;
        this.processInstanceId = processInstanceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.deleteReason = deleteReason;
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
    @Nullable
    public String getAssignee() {
        return assignee;
    }

    @Override
    @NotNull
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    @NotNull
    public Date getStartTime() {
        return startTime;
    }

    @Override
    @Nullable
    public Date getEndTime() {
        return endTime;
    }

    @Override
    @Nullable
    public Long getDurationInMillis() {
        if (startTime != null && endTime != null) {
            return endTime.getTime() - startTime.getTime();
        }
        return null;
    }

    @Override
    @Nullable
    public String getDeleteReason() {
        return deleteReason;
    }
}
