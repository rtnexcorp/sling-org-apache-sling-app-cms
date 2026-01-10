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

import java.util.Collections;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.cms.TaxonomyItem;
import org.apache.sling.cms.TaxonomyService;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

/**
 * Simple Sling Model for providing taxonomy options to content filters.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class TaxonomyModel {

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private TaxonomyService taxonomyService;

    private List<TaxonomyItem> taxonomyOptions;

    @PostConstruct
    protected void init() {
        if (taxonomyService != null && request != null) {
            taxonomyOptions = taxonomyService.getAllTaxonomyItems(request.getResourceResolver());
        } else {
            taxonomyOptions = Collections.emptyList();
        }
    }

    /**
     * Gets the list of available taxonomy items for filtering.
     *
     * @return unmodifiable list of taxonomy options
     */
    public List<TaxonomyItem> getTaxonomyOptions() {
        return taxonomyOptions != null ? Collections.unmodifiableList(taxonomyOptions) : Collections.emptyList();
    }

    /**
     * @return true if taxonomy options are available
     */
    public boolean hasTaxonomyOptions() {
        return taxonomyOptions != null && !taxonomyOptions.isEmpty();
    }
}
