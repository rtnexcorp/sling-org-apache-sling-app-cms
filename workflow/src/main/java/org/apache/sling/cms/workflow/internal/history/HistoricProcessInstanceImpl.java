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

import org.apache.sling.cms.workflow.HistoricProcessInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of HistoricProcessInstance backed by JCR storage.
 * Stored at: /var/workflow/history/processes/{processInstanceId}
 */
public class HistoricProcessInstanceImpl implements HistoricProcessInstance {

    private final String id;
    private final String processDefinitionKey;
    private final String businessKey;
    private final Date startTime;
    private final Date endTime;
    private final String deleteReason;

    public HistoricProcessInstanceImpl(@NotNull Node node) throws RepositoryException {
        this.id = node.getName();
        this.processDefinitionKey = node.getProperty("processDefinitionKey").getString();
        this.businessKey = node.hasProperty("businessKey")
                ? node.getProperty("businessKey").getString()
                : null;
        this.startTime = node.getProperty("startTime").getDate().getTime();
        this.endTime = node.hasProperty("endTime")
                ? node.getProperty("endTime").getDate().getTime()
                : null;
        this.deleteReason = node.hasProperty("deleteReason")
                ? node.getProperty("deleteReason").getString()
                : null;
    }

    public HistoricProcessInstanceImpl(
            @NotNull String id,
            @NotNull String processDefinitionKey,
            @Nullable String businessKey,
            @NotNull Date startTime,
            @NotNull Date endTime) {
        this(id, processDefinitionKey, businessKey, startTime, endTime, null);
    }

    public HistoricProcessInstanceImpl(
            @NotNull String id,
            @NotNull String processDefinitionKey,
            @Nullable String businessKey,
            @NotNull Date startTime,
            @NotNull Date endTime,
            @Nullable String deleteReason) {
        this.id = id;
        this.processDefinitionKey = processDefinitionKey;
        this.businessKey = businessKey;
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
    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    @Override
    @Nullable
    public String getBusinessKey() {
        return businessKey;
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
