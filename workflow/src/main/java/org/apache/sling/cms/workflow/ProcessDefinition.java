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
 * Represents a deployed process definition.
 */
@ProviderType
public interface ProcessDefinition {

    /**
     * Unique process definition ID.
     *
     * @return Process definition ID
     */
    @NotNull
    String getId();

    /**
     * Process definition key from BPMN XML.
     *
     * @return Process definition key
     */
    @NotNull
    String getKey();

    /**
     * Process name from BPMN XML.
     *
     * @return Process name
     */
    @NotNull
    String getName();

    /**
     * Process version.
     *
     * @return Version number
     */
    int getVersion();

    /**
     * Deployment ID.
     *
     * @return Deployment ID
     */
    @NotNull
    String getDeploymentId();

    /**
     * Resource name (BPMN file name).
     *
     * @return Resource name or null
     */
    @Nullable
    String getResourceName();

    /**
     * Process description from BPMN XML.
     *
     * @return Description or null
     */
    @Nullable
    String getDescription();

    /**
     * Deployment time of this process definition.
     *
     * @return Deployment time or null
     */
    @Nullable
    Date getDeploymentTime();
}
