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
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.TaxonomyItem;
import org.apache.sling.cms.TaxonomyService;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Sling Model for the flexible content filter component.
 * Supports multiple filter types: content, workflow, and custom filters.
 * Configuration is done via component properties.
 */
@Model(
        adaptables = {SlingHttpServletRequest.class, Resource.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ContentFilter {

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private TaxonomyService taxonomyService;

    @ValueMapValue
    private String filterMode;

    @ValueMapValue
    private String[] customFilters;

    private List<TaxonomyItem> taxonomyOptions;
    private List<FilterConfig> filterConfigs;

    @PostConstruct
    protected void init() {
        // Initialize taxonomy options for content mode
        if (taxonomyService != null && !"workflow".equals(filterMode)) {
            taxonomyOptions = taxonomyService.getAllTaxonomyItems(request.getResourceResolver());
        } else {
            taxonomyOptions = Collections.emptyList();
        }

        // Initialize filter configurations
        filterConfigs = new ArrayList<>();

        // Add filters based on mode
        if ("workflow".equals(filterMode)) {
            initWorkflowFilters();
        } else if ("custom".equals(filterMode) && customFilters != null) {
            initCustomFilters();
        } else {
            initContentFilters();
        }
    }

    private void initContentFilters() {
        filterConfigs.add(new FilterConfig("content-type-filter", "Filter by Type", "filter", new FilterOption[] {
            new FilterOption("", "All Types"),
            new FilterOption("image/", "Images"),
            new FilterOption("video/", "Videos"),
            new FilterOption("application/pdf,application/msword,application/vnd", "Documents"),
            new FilterOption("folder", "Folders")
        }));
    }

    private void initWorkflowFilters() {
        filterConfigs.add(new FilterConfig(
                "filter-process-type",
                "Filter by Process",
                "filter",
                "process-type",
                new FilterOption[] {new FilterOption("", "All Processes")},
                true // dynamic options loaded from backend
                ));

        filterConfigs.add(new FilterConfig(
                "filter-status",
                "Filter by Status",
                "check",
                "status",
                new FilterOption[] {
                    new FilterOption("", "All Status"),
                    new FilterOption("active", "Active"),
                    new FilterOption("suspended", "Suspended")
                },
                false));
    }

    private void initCustomFilters() {
        // Parse custom filter configurations
        // Format: "filterId:label:icon:dataAttr:option1=value1,option2=value2"
        for (String config : customFilters) {
            String[] parts = config.split(":");
            if (parts.length >= 4) {
                String filterId = parts[0];
                String label = parts[1];
                String icon = parts[2];
                String dataAttr = parts[3];

                List<FilterOption> options = new ArrayList<>();
                if (parts.length > 4) {
                    String[] optionPairs = parts[4].split(",");
                    for (String pair : optionPairs) {
                        String[] optionParts = pair.split("=");
                        if (optionParts.length == 2) {
                            options.add(new FilterOption(optionParts[0], optionParts[1]));
                        }
                    }
                }

                filterConfigs.add(
                        new FilterConfig(filterId, label, icon, dataAttr, options.toArray(new FilterOption[0]), false));
            }
        }
    }

    /**
     * @return filter mode (content, workflow, custom)
     */
    public String getFilterMode() {
        return StringUtils.defaultIfBlank(filterMode, "content");
    }

    /**
     * @return list of filter configurations
     */
    public List<FilterConfig> getFilterConfigs() {
        return filterConfigs;
    }

    /**
     * @return list of available taxonomy options
     */
    public List<TaxonomyItem> getTaxonomyOptions() {
        return taxonomyOptions;
    }

    /**
     * @return true if taxonomy options are available
     */
    public boolean hasTaxonomyOptions() {
        return taxonomyOptions != null && !taxonomyOptions.isEmpty();
    }

    /**
     * @return true if in workflow mode
     */
    public boolean isWorkflowMode() {
        return "workflow".equals(filterMode);
    }

    /**
     * @return true if in content mode
     */
    public boolean isContentMode() {
        return filterMode == null || "content".equals(filterMode);
    }

    /**
     * Filter configuration class
     */
    public static class FilterConfig {
        private final String id;
        private final String label;
        private final String icon;
        private final String dataAttribute;
        private final FilterOption[] options;
        private final boolean dynamicOptions;

        public FilterConfig(String id, String label, String icon, FilterOption[] options) {
            this(id, label, icon, null, options, false);
        }

        public FilterConfig(
                String id,
                String label,
                String icon,
                String dataAttribute,
                FilterOption[] options,
                boolean dynamicOptions) {
            this.id = id;
            this.label = label;
            this.icon = icon;
            this.dataAttribute = dataAttribute;
            this.options = options;
            this.dynamicOptions = dynamicOptions;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public String getIcon() {
            return icon;
        }

        public String getDataAttribute() {
            return dataAttribute;
        }

        public FilterOption[] getOptions() {
            return options;
        }

        public boolean isDynamicOptions() {
            return dynamicOptions;
        }
    }

    /**
     * Filter option class
     */
    public static class FilterOption {
        private final String value;
        private final String label;

        public FilterOption(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }
}
