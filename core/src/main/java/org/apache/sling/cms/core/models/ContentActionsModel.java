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
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.AuthorizableWrapper;
import org.apache.sling.cms.PageManager;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model for the Content Actions component
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ContentActionsModel {

    private static final Logger LOG = LoggerFactory.getLogger(ContentActionsModel.class);

    @SlingObject
    private SlingHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    @Inject
    private Resource resource;

    private List<Action> actions;

    public List<Action> getActions() {
        if (actions == null) {
            actions = new ArrayList<>();
            Resource actionsResource = resource.getChild("actions");
            if (actionsResource != null) {
                actions = StreamSupport.stream(actionsResource.getChildren().spliterator(), false)
                        .map(Action::new)
                        .collect(Collectors.toList());
            }
        }
        return actions;
    }

    public boolean isTableView() {
        RequestPathInfo pathInfo = request.getRequestPathInfo();
        String selectorString = pathInfo.getSelectorString();

        // Check if table selector is present
        if ("table".equals(selectorString)) {
            return true;
        }

        // Check user's default layout preference
        AuthorizableWrapper auth = resourceResolver.adaptTo(AuthorizableWrapper.class);
        if (auth != null && !"grid".equals(selectorString)) {
            try {
                Resource profile =
                        resourceResolver.getResource(auth.getAuthorizable().getPath() + "/profile");
                if (profile != null) {
                    String defaultLayout = profile.getValueMap().get("defaultLayout", "");
                    return "table".equals(defaultLayout);
                }
            } catch (Exception e) {
                LOG.debug("Failed to get user profile", e);
            }
        }

        return false;
    }

    public String getGridViewUrl() {
        PageManager pm = resource.adaptTo(PageManager.class);
        if (pm != null && pm.getPage() != null) {
            String pagePath = pm.getPage().getPath();
            String cmsPath = pagePath.substring(30); // Remove /libs/sling-cms/content prefix
            return "/cms" + cmsPath + ".grid.html" + getSuffix();
        }
        return "";
    }

    public String getTableViewUrl() {
        PageManager pm = resource.adaptTo(PageManager.class);
        if (pm != null && pm.getPage() != null) {
            String pagePath = pm.getPage().getPath();
            String cmsPath = pagePath.substring(30); // Remove /libs/sling-cms/content prefix
            return "/cms" + cmsPath + ".table.html" + getSuffix();
        }
        return "";
    }

    private String getSuffix() {
        String suffix = request.getRequestPathInfo().getSuffix();
        return suffix != null ? suffix : "";
    }

    public static class Action {
        private final ValueMap properties;

        public Action(Resource resource) {
            this.properties = resource.getValueMap();
        }

        public String getLabel() {
            return properties.get("label", String.class);
        }

        public String getPrefix() {
            return properties.get("prefix", "");
        }

        public String getSuffix() {
            return properties.get("suffix", "");
        }
    }
}
