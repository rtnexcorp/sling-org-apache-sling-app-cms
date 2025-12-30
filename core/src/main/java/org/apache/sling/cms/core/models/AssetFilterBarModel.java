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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the Asset Filter Bar component
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class AssetFilterBarModel {

    @SlingObject
    private ResourceResolver resourceResolver;

    private List<TaxonomyOption> taxonomyOptions;

    @PostConstruct
    protected void init() {
        taxonomyOptions = new ArrayList<>();

        // Query for taxonomies
        String query = "SELECT * FROM [sling:Taxonomy] WHERE ISDESCENDANTNODE([/etc/taxonomy])";
        try {
            Iterator<Resource> taxonomies = resourceResolver.findResources(query, "JCR-SQL2");
            while (taxonomies.hasNext()) {
                Resource taxonomy = taxonomies.next();
                ValueMap props = taxonomy.getValueMap();
                String title = props.get("jcr:title", taxonomy.getName());
                taxonomyOptions.add(new TaxonomyOption(taxonomy.getPath(), title));
            }
        } catch (Exception e) {
            // Log error but continue - taxonomy filter is optional
        }
    }

    public List<TaxonomyOption> getTaxonomyOptions() {
        return taxonomyOptions;
    }

    /**
     * Inner class to represent a taxonomy option
     */
    public static class TaxonomyOption {
        private final String path;
        private final String title;

        public TaxonomyOption(String path, String title) {
            this.path = path;
            this.title = title;
        }

        public String getPath() {
            return path;
        }

        public String getTitle() {
            return title;
        }
    }
}
