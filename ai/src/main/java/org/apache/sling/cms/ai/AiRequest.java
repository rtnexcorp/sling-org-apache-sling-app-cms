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
package org.apache.sling.cms.ai;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a request to an AI service.
 * <p>
 * This class encapsulates all the information needed for an AI operation,
 * including the content to process, context about the request, and any
 * additional parameters.
 * </p>
 */
public class AiRequest {

    private final String content;
    private final String contentPath;
    private final String sitePath;
    private final String locale;
    private final String userId;
    private final String promptTemplateId;
    private final Map<String, Object> parameters;
    private final List<String> contextHints;

    private AiRequest(Builder builder) {
        this.content = builder.content;
        this.contentPath = builder.contentPath;
        this.sitePath = builder.sitePath;
        this.locale = builder.locale;
        this.userId = builder.userId;
        this.promptTemplateId = builder.promptTemplateId;
        this.parameters =
                builder.parameters != null ? Collections.unmodifiableMap(builder.parameters) : Collections.emptyMap();
        this.contextHints = builder.contextHints != null
                ? Collections.unmodifiableList(builder.contextHints)
                : Collections.emptyList();
    }

    /**
     * Gets the main content to process.
     *
     * @return the content text
     */
    @NotNull
    public String getContent() {
        return content != null ? content : "";
    }

    /**
     * Gets the JCR path of the content being processed.
     *
     * @return the content path, or null if not applicable
     */
    @Nullable
    public String getContentPath() {
        return contentPath;
    }

    /**
     * Gets the site path for context-aware processing.
     *
     * @return the site path, or null if not applicable
     */
    @Nullable
    public String getSitePath() {
        return sitePath;
    }

    /**
     * Gets the locale for the request.
     *
     * @return the locale code (e.g., "en", "en_US"), or null for default
     */
    @Nullable
    public String getLocale() {
        return locale;
    }

    /**
     * Gets the ID of the user making the request.
     *
     * @return the user ID
     */
    @Nullable
    public String getUserId() {
        return userId;
    }

    /**
     * Gets the ID of the prompt template to use.
     *
     * @return the prompt template ID, or null for default
     */
    @Nullable
    public String getPromptTemplateId() {
        return promptTemplateId;
    }

    /**
     * Gets additional parameters for the AI operation.
     *
     * @return an unmodifiable map of parameters
     */
    @NotNull
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Gets a specific parameter value.
     *
     * @param key the parameter key
     * @param <T> the expected type
     * @return an Optional containing the parameter value if present
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getParameter(String key) {
        return Optional.ofNullable((T) parameters.get(key));
    }

    /**
     * Gets context hints that may help the AI understand the request.
     *
     * @return an unmodifiable list of context hints
     */
    @NotNull
    public List<String> getContextHints() {
        return contextHints;
    }

    /**
     * Creates a new builder for constructing AiRequest instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link AiRequest} instances.
     */
    public static class Builder {
        private String content;
        private String contentPath;
        private String sitePath;
        private String locale;
        private String userId;
        private String promptTemplateId;
        private Map<String, Object> parameters;
        private List<String> contextHints;

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder contentPath(String contentPath) {
            this.contentPath = contentPath;
            return this;
        }

        public Builder sitePath(String sitePath) {
            this.sitePath = sitePath;
            return this;
        }

        public Builder locale(String locale) {
            this.locale = locale;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder promptTemplateId(String promptTemplateId) {
            this.promptTemplateId = promptTemplateId;
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder contextHints(List<String> contextHints) {
            this.contextHints = contextHints;
            return this;
        }

        public AiRequest build() {
            return new AiRequest(this);
        }
    }
}
