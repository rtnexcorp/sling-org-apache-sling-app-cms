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
package org.apache.sling.cms.workflow;

import java.util.Date;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a user task in a process.
 */
@ProviderType
public interface Task {

    /**
     * Unique task ID.
     *
     * @return Task ID
     */
    @NotNull
    String getId();

    /**
     * Task name from BPMN XML.
     *
     * @return Task name
     */
    @NotNull
    String getName();

    /**
     * Task description.
     *
     * @return Task description or null
     */
    @Nullable
    String getDescription();

    /**
     * Assigned user (if claimed).
     *
     * @return Assignee user ID or null
     */
    @Nullable
    String getAssignee();

    /**
     * Owner user ID.
     *
     * @return Owner user ID or null
     */
    @Nullable
    String getOwner();

    /**
     * Candidate group.
     *
     * @return Candidate group or null
     */
    @Nullable
    String getCandidateGroup();

    /**
     * Process instance ID.
     *
     * @return Process instance ID
     */
    @NotNull
    String getProcessInstanceId();

    /**
     * Process definition key.
     *
     * @return Process definition key
     */
    @NotNull
    String getProcessDefinitionKey();

    /**
     * Creation time.
     *
     * @return Creation time
     */
    @NotNull
    Date getCreateTime();

    /**
     * Due date (if set).
     *
     * @return Due date or null
     */
    @Nullable
    Date getDueDate();

    /**
     * Priority.
     *
     * @return Priority value (default: 50)
     */
    int getPriority();

    /**
     * Task definition key from BPMN.
     *
     * @return Task definition key
     */
    @NotNull
    String getTaskDefinitionKey();
}
