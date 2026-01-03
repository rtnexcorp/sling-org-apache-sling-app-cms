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
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a response from an AI service.
 * <p>
 * This class encapsulates the AI-generated content along with metadata
 * about the response, including confidence level, warnings, and provider
 * information for audit purposes.
 * </p>
 */
public class AiResponse {

    /**
     * Status of the AI response.
     */
    public enum Status {
        /** AI operation completed successfully */
        SUCCESS,
        /** AI operation failed */
        FAILURE,
        /** AI operation was skipped (e.g., service disabled) */
        SKIPPED,
        /** AI operation timed out */
        TIMEOUT
    }

    private final Status status;
    private final String content;
    private final String providerId;
    private final String promptTemplateId;
    private final Double confidence;
    private final List<String> warnings;
    private final String errorMessage;
    private final long processingTimeMs;

    private AiResponse(Builder builder) {
        this.status = builder.status;
        this.content = builder.content;
        this.providerId = builder.providerId;
        this.promptTemplateId = builder.promptTemplateId;
        this.confidence = builder.confidence;
        this.warnings =
                builder.warnings != null ? Collections.unmodifiableList(builder.warnings) : Collections.emptyList();
        this.errorMessage = builder.errorMessage;
        this.processingTimeMs = builder.processingTimeMs;
    }

    /**
     * Gets the status of this response.
     *
     * @return the response status
     */
    @NotNull
    public Status getStatus() {
        return status != null ? status : Status.FAILURE;
    }

    /**
     * Returns whether the AI operation was successful.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Gets the AI-generated content.
     *
     * @return the generated content, or empty string if not available
     */
    @NotNull
    public String getContent() {
        return content != null ? content : "";
    }

    /**
     * Gets the ID of the provider that generated this response.
     *
     * @return the provider ID
     */
    @Nullable
    public String getProviderId() {
        return providerId;
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
     * Gets the confidence level of the AI response.
     *
     * @return an Optional containing the confidence (0.0-1.0) if available
     */
    public Optional<Double> getConfidence() {
        return Optional.ofNullable(confidence);
    }

    /**
     * Gets any warnings associated with this response.
     *
     * @return an unmodifiable list of warnings
     */
    @NotNull
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Returns whether this response has warnings.
     *
     * @return true if there are warnings
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Gets the error message if the operation failed.
     *
     * @return the error message, or null if successful
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
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
     * Creates a successful response with the given content.
     *
     * @param content    the generated content
     * @param providerId the provider ID
     * @return a success response
     */
    public static AiResponse success(String content, String providerId) {
        return builder()
                .status(Status.SUCCESS)
                .content(content)
                .providerId(providerId)
                .build();
    }

    /**
     * Creates a failure response with the given error message.
     *
     * @param errorMessage the error message
     * @param providerId   the provider ID
     * @return a failure response
     */
    public static AiResponse failure(String errorMessage, String providerId) {
        return builder()
                .status(Status.FAILURE)
                .errorMessage(errorMessage)
                .providerId(providerId)
                .build();
    }

    /**
     * Creates a skipped response (e.g., when AI is disabled).
     *
     * @param reason the reason for skipping
     * @return a skipped response
     */
    public static AiResponse skipped(String reason) {
        return builder().status(Status.SKIPPED).errorMessage(reason).build();
    }

    /**
     * Creates a new builder for constructing AiResponse instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link AiResponse} instances.
     */
    public static class Builder {
        private Status status;
        private String content;
        private String providerId;
        private String promptTemplateId;
        private Double confidence;
        private List<String> warnings;
        private String errorMessage;
        private long processingTimeMs;

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder providerId(String providerId) {
            this.providerId = providerId;
            return this;
        }

        public Builder promptTemplateId(String promptTemplateId) {
            this.promptTemplateId = promptTemplateId;
            return this;
        }

        public Builder confidence(Double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder warnings(List<String> warnings) {
            this.warnings = warnings;
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

        public AiResponse build() {
            return new AiResponse(this);
        }
    }
}
