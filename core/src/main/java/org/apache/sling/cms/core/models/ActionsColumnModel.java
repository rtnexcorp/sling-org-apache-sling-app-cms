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

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the Actions Column component
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ActionsColumnModel {

    @SlingObject
    private SlingHttpServletRequest request;

    @Inject
    private Resource resource;

    /**
     * Check if the column should be shown (not hidden)
     * @return true if the column should be shown
     */
    public boolean isShow() {
        Object show = request.getAttribute("show");
        return show != null && (show instanceof Boolean ? (Boolean) show : Boolean.parseBoolean(show.toString()));
    }

    /**
     * Get the CSS class for the cell
     * @return "cell-actions is-vhidden" if not shown, empty string otherwise
     */
    public String getCellClass() {
        return isShow() ? "" : "cell-actions is-vhidden";
    }

    /**
     * Get the resource path
     * @return the path of the current resource
     */
    public String getPath() {
        return resource.getPath();
    }

    /**
     * Get the action configurations from the colConfig resource
     * This needs to read the child resources of the column config
     * @return list of action configurations
     */
    public List<ActionConfig> getActions() {
        // First try to get the colConfigPath from request attributes
        String colConfigPath = (String) request.getAttribute("colConfigPath");
        if (colConfigPath != null) {
            Resource colConfig = request.getResourceResolver().getResource(colConfigPath);
            if (colConfig != null) {
                return StreamSupport.stream(colConfig.getChildren().spliterator(), false)
                        .map(ActionConfig::new)
                        .collect(Collectors.toList());
            }
        }

        // Fallback to old approach for backwards compatibility
        Object colConfigObj = request.getAttribute("colConfig");
        if (colConfigObj instanceof Resource) {
            Resource colConfig = (Resource) colConfigObj;
            return StreamSupport.stream(colConfig.getChildren().spliterator(), false)
                    .map(ActionConfig::new)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * Inner class to represent an action configuration
     */
    public static class ActionConfig {
        private final Resource resource;
        private final ValueMap properties;

        public ActionConfig(Resource resource) {
            this.resource = resource;
            this.properties = resource.getValueMap();
        }

        public String getName() {
            return resource.getName();
        }

        public String getResourceType() {
            return properties.get("sling:resourceType", String.class);
        }

        public String getTitle() {
            return properties.get("title", "");
        }

        public String getIcon() {
            return properties.get("icon", "");
        }

        public String getPrefix() {
            return properties.get("prefix", "");
        }

        public ValueMap getProperties() {
            return properties;
        }

        public Resource getResource() {
            return resource;
        }
    }
}
