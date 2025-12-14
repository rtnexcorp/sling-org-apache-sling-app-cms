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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the page edit bar actions component.
 * Provides the list of action configurations and the suffix path.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PageEditBarActionsModel {

    @Inject
    private Resource resource;

    @SlingObject
    private SlingHttpServletRequest request;

    /**
     * Gets the list of action items wrapping each action configuration resource.
     * Each item provides the resource type and the action config for request attributes.
     *
     * @return list of action items
     */
    public List<ActionItem> getActions() {
        if (resource == null) {
            return Collections.emptyList();
        }

        Iterator<Resource> it = resource.listChildren();
        if (it == null) {
            return Collections.emptyList();
        }

        List<ActionItem> actions = new ArrayList<>();
        it.forEachRemaining(r -> actions.add(new ActionItem(r)));
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

        public ActionItem(Resource config) {
            this.config = config;
        }

        public String getResourceType() {
            return config.getResourceType();
        }

        public Resource getConfig() {
            return config;
        }
    }
}
