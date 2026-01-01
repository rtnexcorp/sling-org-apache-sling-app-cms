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
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for accessing branding configuration.
 * Provides access to branding properties like gridIconsBase, logos, etc.
 */
@Model(adaptables = {SlingHttpServletRequest.class, Resource.class})
public class BrandingModel {

    private static final String BRANDING_PATH = "/mnt/overlay/sling-cms/content/branding";

    @SlingObject
    private ResourceResolver resourceResolver;

    private ValueMap brandingProperties;

    @PostConstruct
    protected void init() {
        Resource brandingResource = resourceResolver.getResource(BRANDING_PATH);
        if (brandingResource != null) {
            brandingProperties = brandingResource.getValueMap();
        }
    }

    /**
     * Gets the base path for grid icons.
     *
     * @return the grid icons base path, or empty string if not available
     */
    public String getGridIconsBase() {
        return brandingProperties != null ? brandingProperties.get("gridIconsBase", "") : "";
    }

    /**
     * Gets the application name.
     *
     * @return the application name, or empty string if not available
     */
    public String getAppName() {
        return brandingProperties != null ? brandingProperties.get("appName", "") : "";
    }

    /**
     * Gets the logo path.
     *
     * @return the logo path, or empty string if not available
     */
    public String getLogo() {
        return brandingProperties != null ? brandingProperties.get("logo", "") : "";
    }

    /**
     * Gets all branding properties.
     *
     * @return the branding ValueMap, or null if not available
     */
    public ValueMap getProperties() {
        return brandingProperties;
    }
}
