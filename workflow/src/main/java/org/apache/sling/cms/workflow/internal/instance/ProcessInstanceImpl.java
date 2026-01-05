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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.cms.workflow.ProcessInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of ProcessInstance backed by JCR storage.
 */
public class ProcessInstanceImpl implements ProcessInstance {

    private final String id;
    private final String processDefinitionId;
    private final String businessKey;
    private final Date startTime;
    private String currentActivityId;
    private boolean ended;
    private final Map<String, Object> variables;
    private final Node node;

    public ProcessInstanceImpl(@NotNull Node node) throws RepositoryException {
        this.node = node;
        this.id = node.getIdentifier();
        this.processDefinitionId = node.getProperty("processDefinitionId").getString();
        this.businessKey = node.hasProperty("businessKey")
                ? node.getProperty("businessKey").getString()
                : null;
        this.startTime = node.getProperty("startTime").getDate().getTime();
        this.currentActivityId = node.hasProperty("currentActivityId")
                ? node.getProperty("currentActivityId").getString()
                : null;
        this.ended = node.hasProperty("ended") && node.getProperty("ended").getBoolean();
        this.variables = new HashMap<>();
        loadVariables();
    }

    public ProcessInstanceImpl(
            @NotNull String id, @NotNull String processDefinitionId, @Nullable String businessKey, @NotNull Node node) {
        this.id = id;
        this.processDefinitionId = processDefinitionId;
        this.businessKey = businessKey;
        this.startTime = new Date();
        this.currentActivityId = null;
        this.ended = false;
        this.variables = new HashMap<>();
        this.node = node;
    }

    private void loadVariables() throws RepositoryException {
        if (node.hasNode("variables")) {
            Node varsNode = node.getNode("variables");
            var propertyIterator = varsNode.getProperties();
            while (propertyIterator.hasNext()) {
                var property = propertyIterator.nextProperty();
                if (!property.getName().startsWith("jcr:")) {
                    variables.put(property.getName(), property.getValue().getString());
                }
            }
        }
    }

    @Override
    @NotNull
    public String getId() {
        return id;
    }

    @Override
    @NotNull
    public String getProcessDefinitionId() {
        return processDefinitionId;
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
    public String getCurrentActivityId() {
        return currentActivityId;
    }

    public void setCurrentActivityId(@Nullable String activityId) {
        this.currentActivityId = activityId;
    }

    @Override
    public boolean isEnded() {
        return ended;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }

    @Override
    @NotNull
    public Map<String, Object> getVariables() {
        return new HashMap<>(variables);
    }

    @Override
    @Nullable
    public Object getVariable(@NotNull String name) {
        return variables.get(name);
    }

    public void setVariable(@NotNull String name, @Nullable Object value) {
        if (value == null) {
            variables.remove(name);
        } else {
            variables.put(name, value);
        }
    }

    @Override
    @NotNull
    public String getProcessDefinitionKey() {
        // Extract key from processDefinitionId (format: "key:version:id")
        if (processDefinitionId.contains(":")) {
            return processDefinitionId.split(":")[0];
        }
        return processDefinitionId;
    }

    @Override
    @Nullable
    public Date getEndTime() {
        // MVP: Return approximate end time if ended
        return ended ? new Date() : null;
    }

    @Override
    public boolean isSuspended() {
        // MVP: No suspend support yet
        return false;
    }

    @NotNull
    public Node getNode() {
        return node;
    }
}
