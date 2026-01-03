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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.cms.ai.AiRequest;
import org.apache.sling.cms.ai.AiResponse;
import org.apache.sling.cms.ai.AiTextService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ollama local LLM implementation of AI text services.
 * Runs models locally without external API calls, ideal for privacy-sensitive deployments.
 * Requires Ollama to be installed and running locally (https://ollama.ai).
 */
@Component(service = AiTextService.class, immediate = true)
@Designate(ocd = OllamaTextService.Config.class)
public class OllamaTextService implements AiTextService {

    private static final Logger log = LoggerFactory.getLogger(OllamaTextService.class);

    private static final String GENERATE_ENDPOINT = "/api/generate";
    private static final int MAX_RETRIES = 2;
    private static final int RETRY_DELAY_MS = 500;

    @ObjectClassDefinition(
            name = "Apache Sling CMS - Ollama Text Service",
            description = "Ollama local LLM integration for AI text operations (privacy-friendly, no external API)")
    public @interface Config {

        @AttributeDefinition(name = "Enabled", description = "Enable the Ollama text service")
        boolean enabled() default false;

        @AttributeDefinition(name = "Ollama URL", description = "Ollama server URL (default: http://localhost:11434)")
        String ollamaUrl() default "http://localhost:11434";

        @AttributeDefinition(
                name = "Model",
                description = "Ollama model to use (e.g., llama2, mistral, codellama, llama3)")
        String model() default "llama2";

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
    }

    private Config config;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    @Activate
    protected void activate(Config config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.timeout()))
                .build();

        if (config.enabled()) {
            log.info("Ollama text service activated with model: {} at {}", config.model(), config.ollamaUrl());

            // Test connectivity
            testConnection();
        }
    }

    @Deactivate
    protected void deactivate() {
        log.info("Ollama text service deactivated");
    }

    @Override
    public String getId() {
        return "ollama";
    }

    @Override
    public String getTitle() {
        return "Ollama (Local)";
    }

    @Override
    public boolean isEnabled() {
        return config.enabled() && StringUtils.isNotBlank(config.ollamaUrl());
    }

    @Override
    public boolean requiresExternalApi() {
        return false; // Local service
    }

    @Override
    public AiResponse summarize(AiRequest request) {
        if (!isEnabled()) {
            return AiResponse.skipped("Ollama service is not enabled or configured");
        }

        String prompt = "Summarize the following content in 2-3 sentences. Focus on the main points.\n\n"
                + request.getContent();

        return executeRequest(request, prompt, "summarize");
    }

    @Override
    public AiResponse summarize(AiRequest request, int maxLength) {
        if (!isEnabled()) {
            return AiResponse.skipped("Ollama service is not enabled or configured");
        }

        String prompt = "Summarize the following content in no more than " + maxLength
                + " characters. Keep it concise and focused on the main points.\n\n"
                + request.getContent();

        return executeRequest(request, prompt, "summarize");
    }

    @Override
    public AiResponse suggestTitle(AiRequest request) {
        if (!isEnabled()) {
            return AiResponse.skipped("Ollama service is not enabled or configured");
        }

        String prompt =
                "Generate a concise, engaging title (max 60 characters) for the following content. Return only the title, no quotes or explanations.\n\n"
                        + request.getContent();

        return executeRequest(request, prompt, "suggest-title");
    }

    @Override
    public AiResponse suggestMetaDescription(AiRequest request) {
        if (!isEnabled()) {
            return AiResponse.skipped("Ollama service is not enabled or configured");
        }

        String prompt =
                "Generate a compelling meta description (max 155 characters) for the following content. Return only the description, no quotes.\n\n"
                        + request.getContent();

        return executeRequest(request, prompt, "suggest-meta-description");
    }

    @Override
    public AiResponse rewrite(AiRequest request, Tone tone) {
        if (!isEnabled()) {
            return AiResponse.skipped("Ollama service is not enabled or configured");
        }

        String toneInstruction = getToneInstruction(tone);
        String prompt = "Rewrite the following content in a " + toneInstruction
                + " tone. Preserve the key information but adjust the style.\n\n"
                + request.getContent();

        return executeRequest(request, prompt, "rewrite-" + tone.name().toLowerCase());
    }

    @Override
    public AiResponse translate(AiRequest request, String targetLocale) {
        if (!isEnabled()) {
            return AiResponse.skipped("Ollama service is not enabled or configured");
        }

        String languageName = new java.util.Locale(targetLocale).getDisplayLanguage(java.util.Locale.ENGLISH);
        String prompt = "Translate the following content to " + languageName
                + ". Maintain the tone and structure.\n\n"
                + request.getContent();

        return executeRequest(request, prompt, "translate-" + targetLocale);
    }

    @Override
    public AiResponse explain(AiRequest request) {
        if (!isEnabled()) {
            return AiResponse.skipped("Ollama service is not enabled or configured");
        }

        String prompt = "Explain what this content is about in simple terms. What is the main message or purpose?\n\n"
                + request.getContent();

        return executeRequest(request, prompt, "explain");
    }

    @Override
    public AiResponse summarizeChanges(String originalContent, String updatedContent) {
        if (!isEnabled()) {
            return AiResponse.skipped("Ollama service is not enabled or configured");
        }

        String prompt = String.format(
                "Compare these two versions of content and summarize what changed:\n\nBEFORE:\n%s\n\nAFTER:\n%s\n\nProvide a brief summary of the changes.",
                originalContent, updatedContent);

        return executeRequest(null, prompt, "summarize-changes");
    }

    /**
     * Execute an API request with retry logic
     */
    private AiResponse executeRequest(AiRequest request, String prompt, String operation) {
        long startTime = System.currentTimeMillis();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String responseText = callOllamaApi(prompt);
                long processingTime = System.currentTimeMillis() - startTime;

                return AiResponse.success(responseText.trim(), getId());
            } catch (IOException e) {
                log.warn("Ollama API call failed (attempt {}/{}): {}", attempt, MAX_RETRIES, e.getMessage());

                if (attempt == MAX_RETRIES) {
                    return AiResponse.failure(
                            "Ollama API error after " + MAX_RETRIES
                                    + " attempts: "
                                    + e.getMessage()
                                    + ". Is Ollama running?",
                            getId());
                }

                // Short retry delay for local service
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return AiResponse.failure("Request interrupted", getId());
                }
            }
        }

        return AiResponse.failure("Unexpected error in Ollama request", getId());
    }

    /**
     * Call the Ollama API with the given prompt
     */
    private String callOllamaApi(String prompt) throws IOException {
        String url = config.ollamaUrl().replaceAll("/$", "") + GENERATE_ENDPOINT;

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", config.model());
        requestBody.put("prompt", prompt);
        requestBody.put("stream", config.stream());

        ObjectNode options = requestBody.putObject("options");
        options.put("temperature", config.temperature());

        String requestJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(config.timeout()))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new IOException("Ollama API returned status " + response.statusCode() + ": " + response.body());
            }

            return parseOllamaResponse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /**
     * Parse the Ollama API response and extract the generated text
     */
    private String parseOllamaResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode response = root.get("response");

        if (response == null) {
            throw new IOException("Invalid Ollama response: no response field found");
        }

        return response.asText();
    }

    /**
     * Test connection to Ollama server
     */
    private void testConnection() {
        try {
            String url = config.ollamaUrl().replaceAll("/$", "") + "/api/tags";
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Successfully connected to Ollama at {}", config.ollamaUrl());
            } else {
                log.warn("Ollama connectivity test returned status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn(
                    "Failed to connect to Ollama at {}: {}. Make sure Ollama is running.",
                    config.ollamaUrl(),
                    e.getMessage());
        }
    }

    /**
     * Get tone instruction for rewriting
     */
    private String getToneInstruction(Tone tone) {
        switch (tone) {
            case FORMAL:
                return "formal and professional";
            case INFORMAL:
                return "informal and conversational";
            case CONCISE:
                return "concise and to-the-point";
            case DETAILED:
                return "detailed and comprehensive";
            case FRIENDLY:
                return "friendly and approachable";
            case TECHNICAL:
                return "technical and precise";
            default:
                return "clear";
        }
    }
}
