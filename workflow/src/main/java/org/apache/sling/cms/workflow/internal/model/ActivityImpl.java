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
package org.apache.sling.cms.workflow.internal.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal representation of a BPMN activity (task, event, gateway).
 */
public class ActivityImpl {

    private String id;
    private String name;
    private ActivityType type;
    private String candidateGroup;
    private String delegateClass;
    private Map<String, String> fieldValues = new HashMap<>();
    private List<SequenceFlowImpl> outgoingFlows = new ArrayList<>();
    private List<SequenceFlowImpl> incomingFlows = new ArrayList<>();
    private String documentation;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public String getCandidateGroup() {
        return candidateGroup;
    }

    public void setCandidateGroup(String candidateGroup) {
        this.candidateGroup = candidateGroup;
    }

    public String getDelegateClass() {
        return delegateClass;
    }

    public void setDelegateClass(String delegateClass) {
        this.delegateClass = delegateClass;
    }

    public Map<String, String> getFieldValues() {
        return fieldValues;
    }

    public void setFieldValues(Map<String, String> fieldValues) {
        this.fieldValues = fieldValues;
    }

    public List<SequenceFlowImpl> getOutgoingFlows() {
        return outgoingFlows;
    }

    public void addOutgoingFlow(SequenceFlowImpl flow) {
        this.outgoingFlows.add(flow);
    }

    public List<SequenceFlowImpl> getIncomingFlows() {
        return incomingFlows;
    }

    public void addIncomingFlow(SequenceFlowImpl flow) {
        this.incomingFlows.add(flow);
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    @Override
    public String toString() {
        return "ActivityImpl{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", type=" + type + '}';
    }
}
