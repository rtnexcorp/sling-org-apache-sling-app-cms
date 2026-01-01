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
package org.apache.sling.cms.core.models;

import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Base Sling Model for Distribution Agent/Exporter/Importer components.
 * Provides common functionality for displaying distribution configuration.
 */
@Model(adaptables = {SlingHttpServletRequest.class, Resource.class})
public class DistributionConfig {

    @SlingObject
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    private Resource configResource;
    private ValueMap configProperties;

    @PostConstruct
    protected void init() {
        Resource suffixResource = request.getRequestPathInfo().getSuffixResource();
        if (suffixResource != null) {
            this.configResource = suffixResource;
            this.configProperties = suffixResource.getValueMap();
        }
    }

    /**
     * Gets the configuration resource.
     *
     * @return the configuration resource
     */
    public Resource getConfigResource() {
        return configResource;
    }

    /**
     * Gets the configuration properties.
     *
     * @return the configuration value map
     */
    public ValueMap getConfigProperties() {
        return configProperties != null ? configProperties : ValueMap.EMPTY;
    }

    /**
     * Gets the name from configuration.
     *
     * @return the name property
     */
    public String getName() {
        return getConfigProperties().get("name", String.class);
    }

    /**
     * Gets the title from configuration.
     *
     * @return the title property
     */
    public String getTitle() {
        String title = getConfigProperties().get("title", String.class);
        return StringUtils.isNotBlank(title) ? title : getName();
    }

    /**
     * Gets the service PID for OSGi config link.
     *
     * @return the service.pid property
     */
    public String getServicePid() {
        return getConfigProperties().get("service.pid", String.class);
    }

    /**
     * Gets the details/description from configuration.
     *
     * @return the details property
     */
    public String getDetails() {
        return getConfigProperties().get("details", String.class);
    }

    /**
     * Checks if details exist.
     *
     * @return true if details property is not empty
     */
    public boolean hasDetails() {
        return StringUtils.isNotBlank(getDetails());
    }

    /**
     * Gets child configuration items for display.
     *
     * @return list of configuration child resources
     */
    public List<Resource> getConfigurationItems() {
        if (resource == null) {
            return Collections.emptyList();
        }

        Resource configurationResource = resource.getChild("configuration");
        if (configurationResource == null) {
            return Collections.emptyList();
        }

        List<Resource> items = new ArrayList<>();
        configurationResource.getChildren().forEach(items::add);
        return items;
    }

    /**
     * Gets package importer endpoints (for agents).
     *
     * @return array of endpoint URLs
     */
    public String[] getEndpoints() {
        return getConfigProperties().get("packageImporter.endpoints", String[].class);
    }

    /**
     * Checks if endpoints exist.
     *
     * @return true if endpoints array is not empty
     */
    public boolean hasEndpoints() {
        String[] endpoints = getEndpoints();
        return endpoints != null && endpoints.length > 0;
    }

    /**
     * Gets the queue resource path for a specific endpoint index.
     *
     * @param endpointIndex the endpoint index (0-based)
     * @return the queue resource path
     */
    public String getQueuePath(int endpointIndex) {
        return "/libs/sling/distribution/services/agents/" + getName() + "/queues/endpoint" + endpointIndex;
    }

    /**
     * Gets queue information for all endpoints.
     *
     * @return list of queue info objects
     */
    public List<QueueInfo> getQueues() {
        if (!hasEndpoints()) {
            return Collections.emptyList();
        }

        List<QueueInfo> queues = new ArrayList<>();
        String[] endpoints = getEndpoints();
        for (int i = 0; i < endpoints.length; i++) {
            queues.add(new QueueInfo(endpoints[i], i, this));
        }
        return queues;
    }

    /**
     * Inner class to hold queue information for HTL.
     */
    public class QueueInfo {
        private final String endpoint;
        private final int index;
        private Resource queueResource;

        public QueueInfo(String endpoint, int index, DistributionConfig parent) {
            this.endpoint = endpoint;
            this.index = index;

            // Try to get queue resource
            if (parent.resource != null && parent.resource.getResourceResolver() != null) {
                String queuePath = parent.getQueuePath(index);
                this.queueResource = parent.resource.getResourceResolver().getResource(queuePath);
            }
        }

        public String getEndpoint() {
            return endpoint;
        }

        public int getIndex() {
            return index;
        }

        public int getCount() {
            return index + 1; // 1-based for display
        }

        public String getItemsCount() {
            if (queueResource != null) {
                return queueResource.getValueMap().get("itemsCount", "N/A");
            }
            return "N/A";
        }

        public String getState() {
            if (queueResource != null) {
                return queueResource.getValueMap().get("state", "N/A");
            }
            return "N/A";
        }
    }
}
