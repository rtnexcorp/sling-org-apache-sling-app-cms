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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.publication.PUBLICATION_MODE;
import org.apache.sling.cms.publication.PublicationManager;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the Publication Home component.
 * Provides publication mode information for display.
 */
@Model(adaptables = {SlingHttpServletRequest.class, Resource.class})
public class PublicationHome {

    @SlingObject
    private ResourceResolver resourceResolver;

    private PublicationManager publicationManager;

    @PostConstruct
    protected void init() {
        publicationManager = resourceResolver.adaptTo(PublicationManager.class);
    }

    /**
     * Gets the publication manager instance.
     *
     * @return the publication manager, or null if not available
     */
    public PublicationManager getPublicationManager() {
        return publicationManager;
    }

    /**
     * Gets a user-friendly label for the current publication mode.
     *
     * @return the publication mode label, or "Unknown" if not available
     */
    public String getPublicationModeLabel() {
        if (publicationManager == null) {
            return "Unknown";
        }

        PUBLICATION_MODE mode = publicationManager.getPublicationMode();
        if (mode == null) {
            return "Unknown";
        }

        switch (mode) {
            case STANDALONE:
                return "Standalone";
            case CONTENT_DISTRIBUTION:
                return "Content Distribution";
            default:
                return mode.name();
        }
    }
}
