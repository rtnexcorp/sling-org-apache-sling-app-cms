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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.cms.workflow.ProcessDefinition;

/**
 * Internal implementation of ProcessDefinition.
 */
public class ProcessDefinitionImpl implements ProcessDefinition {

    private String id;
    private String key;
    private String name;
    private int version;
    private String deploymentId;
    private String resourceName;
    private String description;
    private Date deploymentTime;
    private ActivityImpl startActivity;
    private Map<String, ActivityImpl> activities = new HashMap<>();
    private Map<String, SequenceFlowImpl> sequenceFlows = new HashMap<>();
    private String bpmnXml;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    @Override
    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Date getDeploymentTime() {
        return deploymentTime;
    }

    public void setDeploymentTime(Date deploymentTime) {
        this.deploymentTime = deploymentTime;
    }

    public ActivityImpl getStartActivity() {
        return startActivity;
    }

    public void setStartActivity(ActivityImpl startActivity) {
        this.startActivity = startActivity;
    }

    public Map<String, ActivityImpl> getActivities() {
        return activities;
    }

    public void setActivities(Map<String, ActivityImpl> activities) {
        this.activities = activities;
    }

    public ActivityImpl getActivity(String activityId) {
        return activities.get(activityId);
    }

    public Map<String, SequenceFlowImpl> getSequenceFlows() {
        return sequenceFlows;
    }

    public void setSequenceFlows(Map<String, SequenceFlowImpl> sequenceFlows) {
        this.sequenceFlows = sequenceFlows;
    }

    public String getBpmnXml() {
        return bpmnXml;
    }

    public void setBpmnXml(String bpmnXml) {
        this.bpmnXml = bpmnXml;
    }

    @Override
    public String toString() {
        return "ProcessDefinitionImpl{"
                + "key='"
                + key
                + '\''
                + ", name='"
                + name
                + '\''
                + ", version="
                + version
                + ", activities="
                + activities.size()
                + '}';
    }
}
