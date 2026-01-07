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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model for workflow designer component.
 */
@Model(adaptables = {SlingHttpServletRequest.class, Resource.class})
public class WorkflowDesignerModel {

    private static final Logger log = LoggerFactory.getLogger(WorkflowDesignerModel.class);
    private static final String DESIGNS_PATH = "/etc/workflow/designs";

    @SlingObject
    private ResourceResolver resourceResolver;

    private static final String BASE_URL = "/bin/workflow/designer";

    /**
     * Get the base URL for workflow designer operations.
     *
     * @return Base URL
     */
    public String getBaseUrl() {
        return BASE_URL;
    }

    /**
     * Get the save URL for workflow designer.
     *
     * @return Save URL
     */
    public String getSaveUrl() {
        return BASE_URL + "?operation=save";
    }

    /**
     * Get the load URL for workflow designer.
     *
     * @return Load URL
     */
    public String getLoadUrl() {
        return BASE_URL + "?operation=load";
    }

    /**
     * Get the deploy URL for workflow designer.
     *
     * @return Deploy URL
     */
    public String getDeployUrl() {
        return BASE_URL + "?operation=deploy";
    }

    /**
     * Get the delete URL for workflow designer.
     *
     * @return Delete URL
     */
    public String getDeleteUrl() {
        return BASE_URL + "?operation=delete";
    }

    /**
     * Get the list URL for workflow designer.
     *
     * @return List URL
     */
    public String getListUrl() {
        return BASE_URL + "?operation=list";
    }

    /**
     * Get all saved workflow designs.
     *
     * @return List of workflow design info
     */
    public List<WorkflowDesignInfo> getSavedWorkflows() {
        List<WorkflowDesignInfo> workflows = new ArrayList<>();
        Session session = resourceResolver.adaptTo(Session.class);

        if (session == null) {
            log.error("Unable to get JCR session");
            return Collections.emptyList();
        }

        try {
            if (!session.nodeExists(DESIGNS_PATH)) {
                return Collections.emptyList();
            }

            Node designsNode = session.getNode(DESIGNS_PATH);
            NodeIterator iterator = designsNode.getNodes();

            while (iterator.hasNext()) {
                Node workflowNode = iterator.nextNode();

                try {
                    String name = workflowNode.hasProperty("name")
                            ? workflowNode.getProperty("name").getString()
                            : workflowNode.getName();
                    String key = workflowNode.hasProperty("key")
                            ? workflowNode.getProperty("key").getString()
                            : workflowNode.getName();
                    Calendar lastModified = workflowNode.hasProperty("lastModified")
                            ? workflowNode.getProperty("lastModified").getDate()
                            : null;

                    workflows.add(new WorkflowDesignInfo(name, key, lastModified));
                } catch (Exception e) {
                    log.warn("Error reading workflow design node: {}", workflowNode.getPath(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error getting saved workflows", e);
        }

        return workflows;
    }

    /**
     * Check if there are any saved workflows.
     *
     * @return true if saved workflows exist
     */
    public boolean getHasSavedWorkflows() {
        return !getSavedWorkflows().isEmpty();
    }

    /**
     * Inner class to hold workflow design information.
     */
    public static class WorkflowDesignInfo {
        private final String name;
        private final String key;
        private final Calendar lastModified;

        public WorkflowDesignInfo(String name, String key, Calendar lastModified) {
            this.name = name;
            this.key = key;
            this.lastModified = lastModified;
        }

        public String getName() {
            return name;
        }

        public String getKey() {
            return key;
        }

        public Calendar getLastModified() {
            return lastModified;
        }

        public Date getLastModifiedDate() {
            return lastModified != null ? lastModified.getTime() : null;
        }
    }
}
