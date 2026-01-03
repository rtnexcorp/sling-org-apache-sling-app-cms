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
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sling Model for AI-enhanced form fields.
 * Provides properties and logic for rendering various field types (text, textarea, richtext, select)
 * with AI suggestion capabilities.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class AiSuggestField {

    @SlingObject
    private Resource resource;

    @SlingObject
    private SlingHttpServletRequest request;

    @ValueMapValue
    @Nullable
    private String label;

    @ValueMapValue
    @Nullable
    private String name;

    @ValueMapValue
    @Nullable
    private String type;

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

    @ValueMapValue
    private boolean disabled;

    @ValueMapValue
    @Nullable
    private String defaultValue;

    // Textarea-specific
    @ValueMapValue
    @Nullable
    private Integer rows;

    // Richtext-specific
    @ValueMapValue
    @Nullable
    private String toolbar;

    // Select-specific
    @ValueMapValue
    private boolean multiple;

    private String value;
    private String fieldName;

    @PostConstruct
    protected void init() {
        // Get field name - check for multifield context first
        Object multifieldBaseName = request.getAttribute("multifieldBaseName");
        Object multifieldItemName = request.getAttribute("multifieldItemName");

        if (multifieldBaseName != null && multifieldItemName != null) {
            this.fieldName = multifieldBaseName.toString() + "/" + multifieldItemName.toString() + "/" + name;
        } else {
            this.fieldName = name;
        }

        // Get value from edited resource
        // Check for editResourcePath attribute first (for Edit Properties modal)
        String editPath = (String) request.getAttribute("editResourcePath");
        if (StringUtils.isBlank(editPath)) {
            editPath = request.getRequestPathInfo().getSuffix();
        }

        if (StringUtils.isNotBlank(editPath) && StringUtils.isNotBlank(name)) {
            Resource editedResource = request.getResourceResolver().getResource(editPath);
            if (editedResource != null) {
                this.value = editedResource.getValueMap().get(name, String.class);
            }
        }

        // Fall back to default value if no value found
        if (StringUtils.isBlank(this.value) && StringUtils.isNotBlank(defaultValue)) {
            this.value = defaultValue;
        }

        // Ensure value is never null
        if (this.value == null) {
            this.value = "";
        }
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
     * Gets the field name attribute.
     *
     * @return the name attribute
     */
    @NotNull
    public String getName() {
        return StringUtils.defaultString(name);
    }

    /**
     * Gets the actual field name to use (may be overridden).
     *
     * @return the field name
     */
    @NotNull
    public String getFieldName() {
        return StringUtils.isNotBlank(fieldName) ? fieldName : getName();
    }

    /**
     * Gets the input type (text, email, url, etc.).
     *
     * @return the input type, defaults to "text"
     */
    @NotNull
    public String getType() {
        return StringUtils.defaultIfBlank(type, "text");
    }

    /**
     * Gets the AI suggestion type (title, summary, metaDescription).
     *
     * @return the AI suggestion type, or null if AI not enabled
     */
    @Nullable
    public String getAiSuggestionType() {
        return aiSuggestionType;
    }

    /**
     * Gets the field name to use as content source for AI suggestions.
     *
     * @return the content source field name, or null
     */
    @Nullable
    public String getContentSource() {
        return contentSource;
    }

    /**
     * Gets the placeholder text.
     *
     * @return the placeholder text, or null
     */
    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * Gets the help text.
     *
     * @return the help text, or null
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
     * Gets the HTML required attribute.
     *
     * @return "required" if field is required, empty string otherwise
     */
    @NotNull
    public String getRequiredAttr() {
        return required ? "required" : "";
    }

    /**
     * Checks if the field is disabled.
     *
     * @return true if disabled
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Gets the HTML disabled attribute.
     *
     * @return "disabled" if field is disabled, empty string otherwise
     */
    @NotNull
    public String getDisabledAttr() {
        return disabled ? "disabled" : "";
    }

    /**
     * Gets the current field value.
     *
     * @return the field value
     */
    @NotNull
    public String getValue() {
        return StringUtils.defaultString(value);
    }

    /**
     * Checks if AI suggestions are enabled for this field.
     *
     * @return true if AI suggestion type is configured
     */
    public boolean hasAiSuggestions() {
        return StringUtils.isNotBlank(aiSuggestionType);
    }

    /**
     * Gets the number of rows for textarea field.
     *
     * @return the number of rows, or null if not specified
     */
    @Nullable
    public Integer getRows() {
        return rows;
    }

    /**
     * Gets the toolbar path for richtext field.
     *
     * @return the toolbar resource path, or null if not specified
     */
    @Nullable
    public String getToolbar() {
        return toolbar;
    }

    /**
     * Checks if multiple selection is enabled for select field.
     *
     * @return true if multiple selection is allowed
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * Gets the HTML multiple attribute for select field.
     *
     * @return "multiple" if multiple selection is enabled, empty string otherwise
     */
    @NotNull
    public String getMultipleAttr() {
        return multiple ? "multiple" : "";
    }

    /**
     * Gets the select options for select field type.
     * Reads child nodes under the "options" child resource.
     *
     * @return list of options, or empty list if none defined
     */
    @NotNull
    public List<Option> getOptions() {
        if (resource == null) {
            return Collections.emptyList();
        }

        Resource optionsResource = resource.getChild("options");
        if (optionsResource == null) {
            return Collections.emptyList();
        }

        List<Option> options = new ArrayList<>();
        for (Resource optionResource : optionsResource.getChildren()) {
            String optionLabel = optionResource.getValueMap().get("label", String.class);
            String optionValue = optionResource.getValueMap().get("value", String.class);

            if (StringUtils.isNotBlank(optionValue)) {
                boolean isSelected = optionValue.equals(this.value);
                options.add(new Option(StringUtils.defaultIfBlank(optionLabel, optionValue), optionValue, isSelected));
            }
        }

        return options;
    }

    /**
     * Inner class representing a select option.
     */
    public static class Option {
        private final String label;
        private final String value;
        private final boolean selected;

        public Option(String label, String value, boolean selected) {
            this.label = label;
            this.value = value;
            this.selected = selected;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public boolean isSelected() {
            return selected;
        }

        /**
         * Gets the HTML selected attribute.
         *
         * @return "selected" if this option is selected, empty string otherwise
         */
        public String getSelected() {
            return selected ? "selected" : "";
        }
    }
}
