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

import java.util.Collections;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.workflow.Task;
import org.apache.sling.cms.workflow.TaskService;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for displaying user task inbox.
 */
@Model(adaptables = {SlingHttpServletRequest.class, Resource.class})
public class TaskInboxModel {

    @OSGiService
    private TaskService taskService;

    @SlingObject
    private SlingHttpServletRequest request;

    /**
     * Get tasks assigned to current user.
     *
     * @return List of assigned tasks
     */
    public List<Task> getAssignedTasks() {
        try {
            taskService.setResolverContext(request.getResourceResolver());
            String userId = request.getRemoteUser();
            if (userId != null) {
                return taskService.getTasksAssignedTo(userId);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        } finally {
            taskService.clearResolverContext();
        }
    }

    /**
     * Get candidate tasks for current user's groups.
     *
     * @return List of candidate tasks
     */
    public List<Task> getCandidateTasks() {
        try {
            taskService.setResolverContext(request.getResourceResolver());
            String userId = request.getRemoteUser();
            if (userId != null) {
                return taskService.getTasksByCandidateUser(userId);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        } finally {
            taskService.clearResolverContext();
        }
    }

    /**
     * Check if user has assigned tasks.
     *
     * @return true if assigned tasks exist
     */
    public boolean getHasAssignedTasks() {
        return !getAssignedTasks().isEmpty();
    }

    /**
     * Check if user has candidate tasks.
     *
     * @return true if candidate tasks exist
     */
    public boolean getHasCandidateTasks() {
        return !getCandidateTasks().isEmpty();
    }

    /**
     * Get total task count.
     *
     * @return Total number of tasks
     */
    public int getTaskCount() {
        return getAssignedTasks().size() + getCandidateTasks().size();
    }

    /**
     * Get assigned task count.
     *
     * @return Number of assigned tasks
     */
    public int getAssignedCount() {
        return getAssignedTasks().size();
    }

    /**
     * Get candidate task count.
     *
     * @return Number of candidate tasks
     */
    public int getCandidateCount() {
        return getCandidateTasks().size();
    }
}
