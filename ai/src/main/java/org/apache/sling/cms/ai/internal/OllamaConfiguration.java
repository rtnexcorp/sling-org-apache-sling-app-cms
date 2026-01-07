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
 * Shared Ollama configuration service that provides centralized settings
 * for all Ollama-based AI services (text, image analysis, embeddings).
 * This ensures a single configuration point for local Ollama instance and common settings.
 * Ollama runs locally without external API calls, ideal for privacy-sensitive deployments.
 */
@Component(service = OllamaConfiguration.class, immediate = true)
@Designate(ocd = OllamaConfiguration.Config.class)
public class OllamaConfiguration {

    @ObjectClassDefinition(
            name = "Apache Sling CMS - Ollama Configuration",
            description = "Centralized Ollama local LLM configuration for all AI services")
    public @interface Config {

        @AttributeDefinition(name = "Enabled", description = "Enable Ollama services globally")
        boolean enabled() default false;

        @AttributeDefinition(name = "Ollama URL", description = "Ollama server URL (default: http://localhost:11434)")
        String ollamaUrl() default "http://localhost:11434";

        @AttributeDefinition(
                name = "Text Model",
                description = "Ollama model for text operations (e.g., llama3, llama2, mistral, codellama, phi, gemma)")
        String textModel() default "llama3";

        @AttributeDefinition(
                name = "Vision Model",
                description = "Ollama model for vision operations (e.g., llava, bakllava)")
        String visionModel() default "llava";

        @AttributeDefinition(
                name = "Temperature",
                description = "Sampling temperature (0.0 = deterministic, 1.0 = very creative)")
        double temperature() default 0.7;

        @AttributeDefinition(
                name = "Timeout (seconds)",
                description = "Request timeout in seconds (local models may need longer)")
        int timeout() default 60;

        @AttributeDefinition(
                name = "Stream",
                description = "Use streaming responses (false = wait for complete response)")
        boolean stream() default false;

        @AttributeDefinition(
                name = "Context Window",
                description = "Number of tokens to use for context (higher = more memory, better context)")
        int contextWindow() default 2048;

        @AttributeDefinition(
                name = "Max Retries",
                description = "Maximum number of retry attempts for failed API calls")
        int maxRetries() default 2;

        @AttributeDefinition(
                name = "Retry Delay (ms)",
                description = "Base delay in milliseconds between retry attempts")
        int retryDelayMs() default 500;
    }

    private Config config;

    @Activate
    protected void activate(Config config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return config.enabled();
    }

    public String getOllamaUrl() {
        return config.ollamaUrl();
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

    public int getTimeout() {
        return config.timeout();
    }

    public boolean isStream() {
        return config.stream();
    }

    public int getContextWindow() {
        return config.contextWindow();
    }

    public int getMaxRetries() {
        return config.maxRetries();
    }

    public int getRetryDelayMs() {
        return config.retryDelayMs();
    }
}
