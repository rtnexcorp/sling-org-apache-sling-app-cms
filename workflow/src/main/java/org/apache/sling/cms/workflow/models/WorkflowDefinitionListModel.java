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
import org.apache.sling.cms.workflow.ProcessDefinition;
import org.apache.sling.cms.workflow.RepositoryService;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;

/**
 * Sling Model for displaying workflow definition list.
 */
@Model(adaptables = {SlingHttpServletRequest.class, Resource.class})
public class WorkflowDefinitionListModel {

    @OSGiService
    private RepositoryService repositoryService;

    /**
     * Get all workflow definitions.
     *
     * @return List of process definitions
     */
    public List<ProcessDefinition> getDefinitions() {
        try {
            return repositoryService.getProcessDefinitions();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Check if there are any definitions.
     *
     * @return true if definitions exist
     */
    public boolean getHasDefinitions() {
        return !getDefinitions().isEmpty();
    }
}
