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
package org.apache.sling.cms.ai.internal;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Shared OpenAI configuration service that provides centralized settings
 * for all OpenAI-based AI services (text, image, classification).
 * This ensures a single configuration point for API key and common settings.
 */
@Component(service = OpenAiConfiguration.class, immediate = true)
@Designate(ocd = OpenAiConfiguration.Config.class)
public class OpenAiConfiguration {

    @ObjectClassDefinition(
            name = "Apache Sling CMS - OpenAI Configuration",
            description = "Centralized OpenAI API configuration for all AI services")
    public @interface Config {

        @AttributeDefinition(name = "Enabled", description = "Enable OpenAI services globally")
        boolean enabled() default false;

        @AttributeDefinition(
                name = "API Base URL",
                description = "OpenAI API base URL (useful for proxies or Azure OpenAI Service)")
        String apiBaseUrl() default "https://api.openai.com/v1";

        @AttributeDefinition(name = "API Key", description = "OpenAI API key (sk-...)")
        String apiKey() default "";

        @AttributeDefinition(name = "Organization ID", description = "Optional OpenAI organization ID")
        String organizationId() default "";

        @AttributeDefinition(
                name = "Text Model",
                description = "OpenAI model for text operations (e.g., gpt-4-turbo, gpt-4, gpt-3.5-turbo)")
        String textModel() default "gpt-4-turbo";

        @AttributeDefinition(
                name = "Vision Model",
                description = "OpenAI vision model for image operations (e.g., gpt-4o, gpt-4-vision-preview)")
        String visionModel() default "gpt-4o";

        @AttributeDefinition(
                name = "Temperature",
                description = "Sampling temperature (0.0 = deterministic, 2.0 = very creative)")
        double temperature() default 0.7;

        @AttributeDefinition(name = "Max Tokens (Text)", description = "Maximum tokens in text response")
        int textMaxTokens() default 1000;

        @AttributeDefinition(name = "Max Tokens (Vision)", description = "Maximum tokens in vision response")
        int visionMaxTokens() default 500;

        @AttributeDefinition(name = "Timeout (seconds)", description = "Request timeout in seconds")
        int timeout() default 30;

        @AttributeDefinition(
                name = "Vision Detail Level",
                description = "Image detail level for vision API: low (faster, cheaper), high (more detailed), or auto")
        String visionDetail() default "auto";

        @AttributeDefinition(
                name = "Max Retries",
                description = "Maximum number of retry attempts for failed API calls")
        int maxRetries() default 3;

        @AttributeDefinition(
                name = "Retry Delay (ms)",
                description = "Base delay in milliseconds between retry attempts (uses exponential backoff)")
        int retryDelayMs() default 1000;
    }

    private Config config;

    @Activate
    protected void activate(Config config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return config.enabled();
    }

    public String getApiBaseUrl() {
        return config.apiBaseUrl();
    }

    public String getApiKey() {
        return config.apiKey();
    }

    public String getOrganizationId() {
        return config.organizationId();
    }

    public String getTextModel() {
        return config.textModel();
    }

    public String getVisionModel() {
        return config.visionModel();
    }

    public double getTemperature() {
        return config.temperature();
    }

    public int getTextMaxTokens() {
        return config.textMaxTokens();
    }

    public int getVisionMaxTokens() {
        return config.visionMaxTokens();
    }

    public int getTimeout() {
        return config.timeout();
    }

    public String getVisionDetail() {
        return config.visionDetail();
    }

    public int getMaxRetries() {
        return config.maxRetries();
    }

    public int getRetryDelayMs() {
        return config.retryDelayMs();
    }
}
