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
package org.apache.sling.cms.ai.audit;

import java.time.Instant;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an audit entry for an AI operation.
 * <p>
 * Each AI operation should be recorded with an audit entry to ensure
 * traceability and compliance with governance requirements.
 * </p>
 */
public class AiAuditEntry {

    /**
     * The type of AI operation performed.
     */
    public enum OperationType {
        SUMMARIZE,
        SUGGEST_TITLE,
        SUGGEST_META_DESCRIPTION,
        REWRITE,
        TRANSLATE,
        EXPLAIN,
        SUMMARIZE_CHANGES,
        SUGGEST_TAGS,
        SUGGEST_CATEGORIES,
        CLASSIFY,
        EXTRACT_KEYWORDS,
        GENERATE_ALT_TEXT,
        GENERATE_CAPTION,
        DESCRIBE_IMAGE
    }

    /**
     * The outcome of the AI operation.
     */
    public enum Outcome {
        /** Operation completed successfully */
        SUCCESS,
        /** Operation failed with an error */
        FAILURE,
        /** Operation was skipped (e.g., AI disabled) */
        SKIPPED,
        /** User applied the AI suggestion */
        APPLIED,
        /** User rejected/discarded the AI suggestion */
        REJECTED
    }

    private final String id;
    private final Instant timestamp;
    private final String userId;
    private final OperationType operationType;
    private final String providerId;
    private final String contentPath;
    private final String sitePath;
    private final String promptTemplateId;
    private final Outcome outcome;
    private final String inputHash;
    private final String outputPreview;
    private final String errorMessage;
    private final long processingTimeMs;

    private AiAuditEntry(Builder builder) {
        this.id = builder.id;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.userId = builder.userId;
        this.operationType = builder.operationType;
        this.providerId = builder.providerId;
        this.contentPath = builder.contentPath;
        this.sitePath = builder.sitePath;
        this.promptTemplateId = builder.promptTemplateId;
        this.outcome = builder.outcome;
        this.inputHash = builder.inputHash;
        this.outputPreview = builder.outputPreview;
        this.errorMessage = builder.errorMessage;
        this.processingTimeMs = builder.processingTimeMs;
    }

    /**
     * Gets the unique ID of this audit entry.
     *
     * @return the audit entry ID
     */
    @NotNull
    public String getId() {
        return id;
    }

    /**
     * Gets the timestamp when the operation occurred.
     *
     * @return the timestamp
     */
    @NotNull
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the ID of the user who triggered the operation.
     *
     * @return the user ID
     */
    @Nullable
    public String getUserId() {
        return userId;
    }

    /**
     * Gets the type of AI operation performed.
     *
     * @return the operation type
     */
    @NotNull
    public OperationType getOperationType() {
        return operationType;
    }

    /**
     * Gets the ID of the AI provider used.
     *
     * @return the provider ID
     */
    @Nullable
    public String getProviderId() {
        return providerId;
    }

    /**
     * Gets the JCR path of the content being processed.
     *
     * @return the content path
     */
    @Nullable
    public String getContentPath() {
        return contentPath;
    }

    /**
     * Gets the site path for context.
     *
     * @return the site path
     */
    @Nullable
    public String getSitePath() {
        return sitePath;
    }

    /**
     * Gets the ID of the prompt template used.
     *
     * @return the prompt template ID, or null if none was used
     */
    @Nullable
    public String getPromptTemplateId() {
        return promptTemplateId;
    }

    /**
     * Gets the outcome of the operation.
     *
     * @return the outcome
     */
    @NotNull
    public Outcome getOutcome() {
        return outcome != null ? outcome : Outcome.SUCCESS;
    }

    /**
     * Gets a hash of the input content (for privacy, not the full content).
     *
     * @return the input hash
     */
    @Nullable
    public String getInputHash() {
        return inputHash;
    }

    /**
     * Gets a preview of the output (first N characters).
     *
     * @return the output preview
     */
    @Nullable
    public String getOutputPreview() {
        return outputPreview;
    }

    /**
     * Gets the error message if the operation failed.
     *
     * @return the error message, or null if successful
     */
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    /**
     * Gets the processing time in milliseconds.
     *
     * @return the processing time
     */
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    /**
     * Creates a new builder for constructing AiAuditEntry instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link AiAuditEntry} instances.
     */
    public static class Builder {
        private String id;
        private Instant timestamp;
        private String userId;
        private OperationType operationType;
        private String providerId;
        private String contentPath;
        private String sitePath;
        private String promptTemplateId;
        private Outcome outcome;
        private String inputHash;
        private String outputPreview;
        private String errorMessage;
        private long processingTimeMs;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder operationType(OperationType operationType) {
            this.operationType = operationType;
            return this;
        }

        public Builder providerId(String providerId) {
            this.providerId = providerId;
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

        public Builder promptTemplateId(String promptTemplateId) {
            this.promptTemplateId = promptTemplateId;
            return this;
        }

        public Builder outcome(Outcome outcome) {
            this.outcome = outcome;
            return this;
        }

        public Builder inputHash(String inputHash) {
            this.inputHash = inputHash;
            return this;
        }

        public Builder outputPreview(String outputPreview) {
            this.outputPreview = outputPreview;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder processingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }

        public AiAuditEntry build() {
            if (id == null) {
                id = java.util.UUID.randomUUID().toString();
            }
            return new AiAuditEntry(this);
        }
    }
}
