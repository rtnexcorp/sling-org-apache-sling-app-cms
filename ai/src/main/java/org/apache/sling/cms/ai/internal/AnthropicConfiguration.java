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
 * Shared Anthropic configuration service that provides centralized settings
 * for all Anthropic Claude-based AI services (text, image analysis, classification).
 * This ensures a single configuration point for API key and common settings.
 */
@Component(service = AnthropicConfiguration.class, immediate = true)
@Designate(ocd = AnthropicConfiguration.Config.class)
public class AnthropicConfiguration {

    @ObjectClassDefinition(
            name = "Apache Sling CMS - Anthropic Configuration",
            description = "Centralized Anthropic Claude API configuration for all AI services")
    public @interface Config {

        @AttributeDefinition(name = "Enabled", description = "Enable Anthropic services globally")
        boolean enabled() default false;

        @AttributeDefinition(
                name = "API Base URL",
                description = "Anthropic API base URL (useful for proxies or testing)")
        String apiBaseUrl() default "https://api.anthropic.com/v1";

        @AttributeDefinition(name = "API Key", description = "Anthropic API key")
        String apiKey() default "";

        @AttributeDefinition(
                name = "Text Model",
                description =
                        "Claude model for text operations (e.g., claude-3-opus-20240229, claude-3-sonnet-20240229, claude-3-haiku-20240307, claude-3-5-sonnet-20241022)")
        String textModel() default "claude-3-5-sonnet-20241022";

        @AttributeDefinition(
                name = "Vision Model",
                description =
                        "Claude model for vision operations (e.g., claude-3-opus-20240229, claude-3-sonnet-20240229)")
        String visionModel() default "claude-3-opus-20240229";

        @AttributeDefinition(name = "API Version", description = "Anthropic API version")
        String apiVersion() default "2023-06-01";

        @AttributeDefinition(
                name = "Temperature",
                description = "Sampling temperature (0.0 = deterministic, 1.0 = very creative)")
        double temperature() default 0.7;

        @AttributeDefinition(name = "Max Tokens (Text)", description = "Maximum tokens in text response")
        int textMaxTokens() default 1000;

        @AttributeDefinition(name = "Max Tokens (Vision)", description = "Maximum tokens in vision response")
        int visionMaxTokens() default 500;

        @AttributeDefinition(name = "Timeout (seconds)", description = "Request timeout in seconds")
        int timeout() default 30;

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

    public String getTextModel() {
        return config.textModel();
    }

    public String getVisionModel() {
        return config.visionModel();
    }

    public String getApiVersion() {
        return config.apiVersion();
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

    public int getMaxRetries() {
        return config.maxRetries();
    }

    public int getRetryDelayMs() {
        return config.retryDelayMs();
    }
}
