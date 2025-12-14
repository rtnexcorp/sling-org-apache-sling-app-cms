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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PageEditBarModel {

    private static final Logger LOG = LoggerFactory.getLogger(PageEditBarModel.class);
    private static final String BRANDING_RESOURCE_PATH = "/mnt/overlay/sling-cms/content/branding";

    @SlingObject
    private SlingHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    public String getAppName() {
        String value = StringUtils.defaultString(getBranding().get("appName", String.class));
        LOG.debug("PageEditBarModel.getAppName() returning: {}", value);
        return value;
    }

    public String getLogo() {
        String value = StringUtils.defaultString(getBranding().get("logo", String.class));
        LOG.debug("PageEditBarModel.getLogo() returning: {}", value);
        return value;
    }

    private ValueMap getBranding() {
        if (resourceResolver == null) {
            LOG.warn("PageEditBarModel: resourceResolver is null");
            return ValueMap.EMPTY;
        }

        Resource brandingResource = resourceResolver.getResource(BRANDING_RESOURCE_PATH);
        if (brandingResource == null) {
            LOG.warn("PageEditBarModel: branding resource not found at {}", BRANDING_RESOURCE_PATH);
            return ValueMap.EMPTY;
        }

        LOG.debug("PageEditBarModel: using branding from resource {}", BRANDING_RESOURCE_PATH);
        return brandingResource.getValueMap();
    }
}
