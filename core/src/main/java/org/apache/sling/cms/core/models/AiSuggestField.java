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
 * Sling Model for AI-enhanced text input field.
 * Provides properties and logic for rendering text fields with AI suggestion
 * capabilities.
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
}
