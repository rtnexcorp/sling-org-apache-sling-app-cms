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
import java.util.stream.Collectors;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.workflow.ProcessInstance;
import org.apache.sling.cms.workflow.RuntimeService;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for monitoring active process instances.
 */
@Model(adaptables = {SlingHttpServletRequest.class, Resource.class})
public class ProcessInstanceMonitorModel {

    @OSGiService
    private RuntimeService runtimeService;

    @SlingObject
    private SlingHttpServletRequest request;

    /**
     * Get all active process instances.
     *
     * @return List of active process instances
     */
    public List<ProcessInstance> getInstances() {
        try {
            // Set resolver context for RuntimeService
            runtimeService.setResolverContext(request.getResourceResolver());
            return runtimeService.getActiveProcessInstances();
        } catch (Exception e) {
            return Collections.emptyList();
        } finally {
            runtimeService.clearResolverContext();
        }
    }

    /**
     * Check if there are any instances.
     *
     * @return true if instances exist
     */
    public boolean getHasInstances() {
        return !getInstances().isEmpty();
    }

    /**
     * Get count of instances.
     *
     * @return Number of active instances
     */
    public int getInstanceCount() {
        return getInstances().size();
    }

    /**
     * Get list of unique process types for filtering.
     *
     * @return List of process definition keys
     */
    public List<String> getProcessTypes() {
        return getInstances().stream()
                .map(ProcessInstance::getProcessDefinitionKey)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
