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
package org.apache.sling.cms.workflow.internal.engine;

import org.apache.sling.cms.workflow.HistoryService;
import org.apache.sling.cms.workflow.ProcessEngine;
import org.apache.sling.cms.workflow.RepositoryService;
import org.apache.sling.cms.workflow.RuntimeService;
import org.apache.sling.cms.workflow.TaskService;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SimpleBPMN Process Engine implementation.
 * Main entry point for the BPMN 2.0 workflow engine.
 */
@Component(service = ProcessEngine.class, immediate = true)
public class SimpleBPMNProcessEngine implements ProcessEngine {

    private static final Logger log = LoggerFactory.getLogger(SimpleBPMNProcessEngine.class);
    private static final String ENGINE_NAME = "SimpleBPMN";

    @Reference
    private RuntimeService runtimeService;

    @Reference
    private TaskService taskService;

    @Reference
    private RepositoryService repositoryService;

    @Reference
    private HistoryService historyService;

    @Activate
    protected void activate() {
        log.info("SimpleBPMN Process Engine activated");
    }

    @Override
    @NotNull
    public String getName() {
        return ENGINE_NAME;
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0.0-SNAPSHOT";
    }

    @Override
    @NotNull
    public RuntimeService getRuntimeService() {
        return runtimeService;
    }

    @Override
    @NotNull
    public TaskService getTaskService() {
        return taskService;
    }

    @Override
    @NotNull
    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    @Override
    @NotNull
    public HistoryService getHistoryService() {
        return historyService;
    }
}
