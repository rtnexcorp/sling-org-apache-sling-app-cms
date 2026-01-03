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
import javax.jcr.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model for AI-enhanced taxonomy field.
 * Provides properties and logic for rendering taxonomy fields with AI suggestion capabilities.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class AiTaxonomyField {

    private static final Logger log = LoggerFactory.getLogger(AiTaxonomyField.class);

    @SlingObject
    private Resource resource;

    @SlingObject
    private SlingHttpServletRequest request;

    @SlingObject
    private ResourceResolver resolver;

    @ValueMapValue
    @Nullable
    private String label;

    @ValueMapValue
    @Nullable
    private String name;

    @ValueMapValue
    @Nullable
    private String basePath;

    @ValueMapValue
    @Nullable
    private String aiSuggestionType;

    @ValueMapValue
    @Nullable
    private String contentSource;

    @ValueMapValue
    @Nullable
    private String placeholder;

    @ValueMapValue
    @Nullable
    private String help;

    @ValueMapValue
    private boolean required;

    private String fieldName;
    private String fieldId;
    private List<TaxonomyItem> selectedItems;
    private List<TaxonomyItem> availableOptions;

    @PostConstruct
    protected void init() {
        // Generate unique field ID
        this.fieldId = UUID.randomUUID().toString().replace("-", "");

        // Get field name - check for multifield context first
        Object multifieldBaseName = request.getAttribute("multifieldBaseName");
        Object multifieldItemName = request.getAttribute("multifieldItemName");

        if (multifieldBaseName != null && multifieldItemName != null) {
            this.fieldName = multifieldBaseName.toString() + "/" + multifieldItemName.toString() + "/" + name;
        } else {
            this.fieldName = name;
        }

        // Get edited resource path
        String editPath = (String) request.getAttribute("editResourcePath");
        if (StringUtils.isBlank(editPath)) {
            editPath = request.getRequestPathInfo().getSuffix();
        }

        // Load selected taxonomy items
        this.selectedItems = new ArrayList<>();
        if (StringUtils.isNotBlank(editPath) && StringUtils.isNotBlank(name)) {
            Resource editedResource = resolver.getResource(editPath);
            if (editedResource != null) {
                String[] values = editedResource.getValueMap().get(name, String[].class);
                if (values != null) {
                    for (String value : values) {
                        Resource taxonomyRes = resolver.getResource(value);
                        if (taxonomyRes != null) {
                            String title = taxonomyRes.getValueMap().get("jcr:title", value);
                            selectedItems.add(new TaxonomyItem(value, title));
                        }
                    }
                }
            }
        }

        // Load available taxonomy options
        this.availableOptions = loadTaxonomyOptions();
    }

    /**
     * Loads available taxonomy options from the repository.
     *
     * @return list of taxonomy items
     */
    private List<TaxonomyItem> loadTaxonomyOptions() {
        List<TaxonomyItem> options = new ArrayList<>();

        try {
            // Get base path from configuration or site settings
            String taxonomyBasePath = basePath;
            if (StringUtils.isBlank(taxonomyBasePath)) {
                taxonomyBasePath = getSiteTaxonomyRoot();
                log.debug("Using site taxonomy root: {}", taxonomyBasePath);
            } else {
                log.debug("Using configured basePath: {}", taxonomyBasePath);
            }

            if (StringUtils.isNotBlank(taxonomyBasePath)) {
                // Query for taxonomy items
                String query = "SELECT * FROM [sling:Taxonomy] WHERE ISDESCENDANTNODE([" + taxonomyBasePath + "])";
                log.debug("Executing taxonomy query: {}", query);

                java.util.Iterator<Resource> results = resolver.findResources(query, Query.JCR_SQL2);
                int count = 0;
                while (results.hasNext()) {
                    Resource res = results.next();
                    ValueMap vm = res.getValueMap();
                    String title = vm.get("jcr:title", res.getName());
                    options.add(new TaxonomyItem(res.getPath(), title));
                    count++;
                }
                log.debug("Found {} taxonomy items", count);
            } else {
                log.warn("No taxonomy base path configured and could not determine site taxonomy root");
            }
        } catch (Exception e) {
            log.error("Error loading taxonomy options", e);
        }

        return options;
    }

    /**
     * Gets the site taxonomy root from configuration.
     *
     * @return taxonomy root path, or empty string if not found
     */
    private String getSiteTaxonomyRoot() {
        try {
            // Get suffix resource (the content being edited)
            String suffixPath = request.getRequestPathInfo().getSuffix();
            if (StringUtils.isNotBlank(suffixPath)) {
                Resource suffixResource = resolver.getResource(suffixPath);
                if (suffixResource != null) {
                    // Get site settings via Context-Aware Configuration
                    Resource settingsRes = resolver.getResource(suffixResource, "site/settings");
                    if (settingsRes != null) {
                        return settingsRes.getValueMap().get("taxonomyroot", "");
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not load site taxonomy root", e);
        }
        return "";
    }

    /**
     * Gets the label text for the field.
     *
     * @return the label, or null if not set
     */
    @Nullable
    public String getLabel() {
        return label;
    }

    /**
     * Gets the field name for form submission.
     *
     * @return the field name
     */
    @NotNull
    public String getFieldName() {
        return fieldName != null ? fieldName : "";
    }

    /**
     * Gets the unique field ID for HTML elements.
     *
     * @return the field ID
     */
    @NotNull
    public String getFieldId() {
        return fieldId != null ? fieldId : "";
    }

    /**
     * Gets the taxonomy base path.
     *
     * @return the base path, or null if not set
     */
    @Nullable
    public String getBasePath() {
        return basePath;
    }

    /**
     * Gets the AI suggestion type.
     *
     * @return the AI suggestion type, or null if not enabled
     */
    @Nullable
    public String getAiSuggestionType() {
        return aiSuggestionType;
    }

    /**
     * Checks if AI suggestions are enabled.
     *
     * @return true if AI suggestions are enabled
     */
    public boolean isHasAiSuggestions() {
        return StringUtils.isNotBlank(aiSuggestionType);
    }

    /**
     * Gets the content source field for AI suggestions.
     *
     * @return the content source field name
     */
    @Nullable
    public String getContentSource() {
        return contentSource;
    }

    /**
     * Gets the placeholder text.
     *
     * @return the placeholder text
     */
    @Nullable
    public String getPlaceholder() {
        return placeholder != null ? placeholder : "Select taxonomy...";
    }

    /**
     * Gets the help text.
     *
     * @return the help text, or null if not set
     */
    @Nullable
    public String getHelp() {
        return help;
    }

    /**
     * Checks if the field is required.
     *
     * @return true if required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Gets the list of selected taxonomy items.
     *
     * @return list of selected items
     */
    @NotNull
    public List<TaxonomyItem> getSelectedItems() {
        return selectedItems != null ? selectedItems : Collections.emptyList();
    }

    /**
     * Gets the list of available taxonomy options.
     *
     * @return list of available options
     */
    @NotNull
    public List<TaxonomyItem> getAvailableOptions() {
        return availableOptions != null ? availableOptions : Collections.emptyList();
    }

    /**
     * Represents a taxonomy item with path and title.
     */
    public static class TaxonomyItem {
        private final String path;
        private final String title;

        public TaxonomyItem(String path, String title) {
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
