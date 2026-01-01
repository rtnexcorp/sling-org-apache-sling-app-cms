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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model for the page edit bar actions component.
 * Reads action configurations from /conf/global/actions/pageeditbar
 * and provides them to the HTL template.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PageEditBarActionsModel {

    private static final Logger LOG = LoggerFactory.getLogger(PageEditBarActionsModel.class);
    private static final String ACTIONS_CONFIG_PATH = "/conf/global/actions/pageeditbar";

    @SlingObject
    private ResourceResolver resourceResolver;

    @SlingObject
    private SlingHttpServletRequest request;

    /**
     * Gets the list of action items from /conf/global/actions/pageeditbar.
     * Each item wraps an action configuration resource with its properties
     * (title, icon, prefix, suffix, etc.)
     *
     * @return list of action items
     */
    public List<ActionItem> getActions() {
        if (resourceResolver == null) {
            LOG.warn("PageEditBarActionsModel: resourceResolver is null");
            return Collections.emptyList();
        }

        Resource actionsConfigResource = resourceResolver.getResource(ACTIONS_CONFIG_PATH);
        if (actionsConfigResource == null) {
            LOG.warn("PageEditBarActionsModel: actions config not found at {}", ACTIONS_CONFIG_PATH);
            return Collections.emptyList();
        }

        Iterator<Resource> it = actionsConfigResource.listChildren();
        if (it == null) {
            return Collections.emptyList();
        }

        List<ActionItem> actions = new ArrayList<>();
        while (it.hasNext()) {
            Resource actionResource = it.next();
            // Skip jcr:content and other non-action nodes
            if (!actionResource.getName().startsWith("jcr:")) {
                actions.add(new ActionItem(actionResource, request));
                LOG.debug("PageEditBarActionsModel: added action {}", actionResource.getPath());
            }
        }

        LOG.info("PageEditBarActionsModel: loaded {} actions from {}", actions.size(), ACTIONS_CONFIG_PATH);
        return actions;
    }

    /**
     * Gets the suffix path from the request (the path of the page being edited).
     *
     * @return suffix path or empty string
     */
    public String getSuffixPath() {
        if (request == null || request.getRequestPathInfo() == null) {
            return "";
        }
        String suffix = request.getRequestPathInfo().getSuffix();
        return suffix != null ? suffix : "";
    }

    /**
     * Wrapper for action configuration that provides data for HTL rendering.
     */
    public static class ActionItem {
        private final Resource config;
        private final SlingHttpServletRequest request;

        public ActionItem(Resource config, SlingHttpServletRequest request) {
            this.config = config;
            this.request = request;
        }

        public String getResourceType() {
            return config.getResourceType();
        }

        public Resource getConfig() {
            return config;
        }

        /**
         * Sets the actionConfig request attribute and returns the suffix path.
         * This is used by HTL to set the request attribute before including the action component.
         *
         * @return suffix path for the resource include
         */
        public String getSuffixPathWithConfig() {
            request.setAttribute("actionConfig", config);
            if (request.getRequestPathInfo() == null) {
                return "";
            }
            String suffix = request.getRequestPathInfo().getSuffix();
            return suffix != null ? suffix : "";
        }
    }
}
