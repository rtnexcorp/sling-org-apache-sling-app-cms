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
 * Shared Azure OpenAI configuration service that provides centralized settings
 * for all Azure OpenAI-based AI services (text, image, classification).
 * This ensures a single configuration point for Azure OpenAI endpoint, API key, and common settings.
 */
@Component(service = AzureOpenAiConfiguration.class, immediate = true)
@Designate(ocd = AzureOpenAiConfiguration.Config.class)
public class AzureOpenAiConfiguration {

    @ObjectClassDefinition(
            name = "Apache Sling CMS - Azure OpenAI Configuration",
            description = "Centralized Azure OpenAI configuration for all AI services")
    public @interface Config {

        @AttributeDefinition(name = "Enabled", description = "Enable Azure OpenAI services globally")
        boolean enabled() default false;

        @AttributeDefinition(
                name = "Endpoint",
                description = "Azure OpenAI endpoint (e.g., https://your-resource.openai.azure.com/)")
        String endpoint() default "";

        @AttributeDefinition(name = "API Key", description = "Azure OpenAI API key")
        String apiKey() default "";

        @AttributeDefinition(
                name = "Text Deployment Name",
                description = "Azure OpenAI deployment name for text operations (e.g., gpt-4, gpt-35-turbo)")
        String textDeploymentName() default "gpt-4";

        @AttributeDefinition(
                name = "Vision Deployment Name",
                description = "Azure OpenAI deployment name for vision operations (e.g., gpt-4o, gpt-4-vision)")
        String visionDeploymentName() default "gpt-4o";

        @AttributeDefinition(name = "API Version", description = "Azure OpenAI API version")
        String apiVersion() default "2024-02-15-preview";

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

    public String getEndpoint() {
        return config.endpoint();
    }

    public String getApiKey() {
        return config.apiKey();
    }

    public String getTextDeploymentName() {
        return config.textDeploymentName();
    }

    public String getVisionDeploymentName() {
        return config.visionDeploymentName();
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
