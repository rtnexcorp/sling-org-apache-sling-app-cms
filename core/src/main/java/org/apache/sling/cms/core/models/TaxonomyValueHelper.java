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
import org.apache.sling.cms.CMSConstants;
import org.apache.sling.models.annotations.Model;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Helper model for taxonomy values to safely retrieve titles
 */
@ProviderType
@Model(adaptables = SlingHttpServletRequest.class)
public class TaxonomyValueHelper {

    private ResourceResolver resourceResolver;

    @PostConstruct
    protected void init() {
        // Initialization if needed
    }

    public TaxonomyValueHelper(SlingHttpServletRequest request) {
        this.resourceResolver = request.getResourceResolver();
    }

    /**
     * Get taxonomy title from path, safely handling null resources
     *
     * @param path the taxonomy resource path
     * @return the title or the path if resource not found
     */
    public String get(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        Resource taxonomyResource = resourceResolver.getResource(path);
        if (taxonomyResource == null) {
            return path; // fallback to path if resource doesn't exist
        }

        String title = taxonomyResource.getValueMap().get(CMSConstants.PN_TITLE, String.class);
        return title != null ? title : path;
    }
}
