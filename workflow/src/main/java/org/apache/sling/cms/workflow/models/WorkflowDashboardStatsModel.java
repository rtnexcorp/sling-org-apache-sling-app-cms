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
package org.apache.sling.cms.workflow.models;

import javax.annotation.PostConstruct;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.cms.workflow.ProcessDefinition;
import org.apache.sling.cms.workflow.ProcessInstance;
import org.apache.sling.cms.workflow.RepositoryService;
import org.apache.sling.cms.workflow.RuntimeService;
import org.apache.sling.cms.workflow.Task;
import org.apache.sling.cms.workflow.TaskService;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model for the workflow dashboard statistics component.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class WorkflowDashboardStatsModel {

    private static final Logger log = LoggerFactory.getLogger(WorkflowDashboardStatsModel.class);

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private RepositoryService repositoryService;

    @OSGiService
    private RuntimeService runtimeService;

    @OSGiService
    private TaskService taskService;

    private int definitionCount;
    private int activeInstanceCount;
    private int pendingTaskCount;
    private int myTaskCount;

    @PostConstruct
    protected void init() {
        try {
            // Get definition count from repository service
            if (repositoryService != null) {
                List<ProcessDefinition> definitions = repositoryService.getProcessDefinitions();
                definitionCount = definitions != null ? definitions.size() : 0;
            }

            // Set resolver context and get active instances
            if (runtimeService != null) {
                runtimeService.setResolverContext(request.getResourceResolver());
                List<ProcessInstance> instances = runtimeService.getActiveProcessInstances();
                activeInstanceCount = instances != null ? instances.size() : 0;
            }

            if (taskService != null) {
                taskService.setResolverContext(request.getResourceResolver());

                // Get all pending tasks (tasks available for candidate groups)
                List<Task> allTasks = taskService.getTasksByCandidateUser(request.getRemoteUser());
                pendingTaskCount = allTasks != null ? allTasks.size() : 0;

                // Get tasks assigned to current user
                List<Task> myTasks = taskService.getTasksAssignedTo(request.getRemoteUser());
                myTaskCount = myTasks != null ? myTasks.size() : 0;
            }
        } catch (Exception e) {
            log.error("Error loading workflow dashboard stats", e);
        }
    }

    /**
     * @return Number of deployed workflow definitions
     */
    public int getDefinitionCount() {
        return definitionCount;
    }

    /**
     * @return Number of active process instances
     */
    public int getActiveInstanceCount() {
        return activeInstanceCount;
    }

    /**
     * @return Number of pending tasks available to claim
     */
    public int getPendingTaskCount() {
        return pendingTaskCount;
    }

    /**
     * @return Number of tasks assigned to the current user
     */
    public int getMyTaskCount() {
        return myTaskCount;
    }
}
