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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.cms.workflow.ProcessDefinition;
import org.apache.sling.cms.workflow.RepositoryService;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model for displaying workflow diagram in read-only viewer.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class WorkflowDiagramViewerModel {

    private static final Logger log = LoggerFactory.getLogger(WorkflowDiagramViewerModel.class);

    @SlingObject
    private SlingHttpServletRequest request;

    @OSGiService
    private RepositoryService repositoryService;

    private ProcessDefinition processDefinition;
    private String bpmnXml;
    private boolean initialized = false;

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;

        String processKey = request.getParameter("processKey");

        if (StringUtils.isNotBlank(processKey)) {
            try {
                processDefinition = repositoryService.getProcessDefinitionByKey(processKey);
                if (processDefinition != null) {
                    bpmnXml = repositoryService.getProcessModel(processKey);
                    log.debug("Loaded BPMN diagram for process: {}", processKey);
                } else {
                    log.warn("Process definition not found: {}", processKey);
                }
            } catch (Exception e) {
                log.error("Error loading process definition: {}", processKey, e);
            }
        }
    }

    /**
     * Check if a process is loaded.
     */
    public boolean getHasProcess() {
        ensureInitialized();
        return processDefinition != null && StringUtils.isNotBlank(bpmnXml);
    }

    /**
     * Get the process name.
     */
    public String getProcessName() {
        ensureInitialized();
        return processDefinition != null ? processDefinition.getName() : "";
    }

    /**
     * Get the process key.
     */
    public String getProcessKey() {
        ensureInitialized();
        return processDefinition != null ? processDefinition.getKey() : "";
    }

    /**
     * Get the process version.
     */
    public int getProcessVersion() {
        ensureInitialized();
        return processDefinition != null ? processDefinition.getVersion() : 0;
    }

    /**
     * Get the BPMN XML content.
     */
    public String getBpmnXml() {
        ensureInitialized();
        return bpmnXml != null ? bpmnXml : "";
    }
}
