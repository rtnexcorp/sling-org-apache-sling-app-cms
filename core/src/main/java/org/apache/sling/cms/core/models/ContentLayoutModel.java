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

import javax.jcr.RepositoryException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.AuthorizableWrapper;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model for the contentlayout component.
 * Determines whether to show table or grid layout.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class ContentLayoutModel {

    private static final Logger log = LoggerFactory.getLogger(ContentLayoutModel.class);

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    /**
     * @return true if table layout should be used
     */
    public boolean isTableLayout() {
        String selector = request.getRequestPathInfo().getSelectorString();

        // Explicit table selector
        if ("table".equals(selector)) {
            return true;
        }

        // Explicit grid selector
        if ("grid".equals(selector)) {
            return false;
        }

        // Check user profile default
        try {
            AuthorizableWrapper auth = request.getResourceResolver().adaptTo(AuthorizableWrapper.class);
            if (auth != null && auth.getAuthorizable() != null) {
                String profilePath = auth.getAuthorizable().getPath() + "/profile";
                Resource profile = request.getResourceResolver().getResource(profilePath);
                if (profile != null) {
                    String defaultLayout = profile.getValueMap().get("defaultLayout", String.class);
                    return "table".equals(defaultLayout);
                }
            }
        } catch (RepositoryException e) {
            log.error("Failed to get user profile", e);
        }

        return false; // Default to grid
    }

    /**
     * @return the resource path
     */
    public String getResourcePath() {
        return resource.getPath();
    }
}
