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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model for retrieving taxonomy options for selection
 */
@ProviderType
@Model(adaptables = SlingHttpServletRequest.class)
public class TaxonomyOptions {

    private static final Logger log = LoggerFactory.getLogger(TaxonomyOptions.class);

    private final List<Resource> taxonomies;

    @OSGiService
    private ConfigurationResourceResolver configResolver;

    public TaxonomyOptions(SlingHttpServletRequest request) {
        this.taxonomies = new ArrayList<>();

        try {
            Resource componentResource = request.getResource();
            String basePath = componentResource.getValueMap().get("basePath", String.class);

            // Get base path from configuration if not set directly
            if (StringUtils.isBlank(basePath)) {
                Resource suffixResource = request.getRequestPathInfo().getSuffixResource();
                if (suffixResource != null) {
                    Resource siteSettings = configResolver.getResource(suffixResource, "site", "settings");
                    if (siteSettings != null) {
                        basePath = siteSettings.getValueMap().get("taxonomyroot", String.class);
                    }
                }
            }

            if (StringUtils.isNotBlank(basePath)) {
                String query = "SELECT * FROM [sling:Taxonomy] WHERE ISDESCENDANTNODE([" + basePath + "])";
                log.debug("Executing taxonomy query: {}", query);

                Iterator<Resource> results = request.getResourceResolver().findResources(query, "JCR-SQL2");
                while (results.hasNext()) {
                    taxonomies.add(results.next());
                }
                log.debug("Found {} taxonomies", taxonomies.size());
            } else {
                log.warn("No basePath configured for taxonomy options");
            }
        } catch (Exception e) {
            log.error("Error loading taxonomy options", e);
        }
    }

    public List<Resource> getTaxonomies() {
        return taxonomies;
    }
}
