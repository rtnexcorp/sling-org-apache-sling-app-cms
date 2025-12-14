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

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the columns/actions component.
 * Provides action buttons for content table rows.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class ColumnActionsModel {

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    private Resource colConfig;
    private ValueMap colConfigValueMap;

    private Resource getColConfig() {
        if (colConfig == null) {
            colConfig = (Resource) request.getAttribute("colConfig");
        }
        return colConfig;
    }

    private ValueMap getColConfigValueMap() {
        if (colConfigValueMap == null && getColConfig() != null) {
            colConfigValueMap = getColConfig().getValueMap();
        }
        return colConfigValueMap;
    }

    /**
     * @return true if actions should be visible
     */
    public boolean isShow() {
        ValueMap vm = getColConfigValueMap();
        return vm != null && vm.get("show", false);
    }

    /**
     * @return the CSS class for the td element
     */
    public String getTdClass() {
        return isShow() ? "" : "cell-actions is-vhidden";
    }

    /**
     * @return the resource path
     */
    public String getResourcePath() {
        return resource.getPath();
    }

    /**
     * @return the list of action configurations
     */
    public List<ActionItem> getActions() {
        List<ActionItem> actions = new ArrayList<>();
        Resource config = getColConfig();
        if (config != null) {
            for (Resource actionConfig : config.getChildren()) {
                actions.add(new ActionItem(actionConfig, resource, request));
            }
        }
        return actions;
    }

    /**
     * Inner class representing an action item.
     */
    public static class ActionItem {
        private final Resource actionConfig;
        private final Resource contextResource;
        private final SlingHttpServletRequest request;

        public ActionItem(Resource actionConfig, Resource contextResource, SlingHttpServletRequest request) {
            this.actionConfig = actionConfig;
            this.contextResource = contextResource;
            this.request = request;
        }

        /**
         * @return the resource type for this action
         */
        public String getResourceType() {
            return actionConfig.getResourceType();
        }

        /**
         * Sets actionConfig in request and returns the context resource path.
         *
         * @return the context resource path
         */
        public String getResourcePathWithConfig() {
            request.setAttribute("actionConfig", actionConfig);
            return contextResource.getPath();
        }
    }
}
