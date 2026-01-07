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
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ollama local LLM implementation of AI text services.
 * Runs models locally without external API calls, ideal for privacy-sensitive deployments.
 * Requires Ollama to be installed and running locally (https://ollama.ai).
 * Uses centralized Ollama configuration for server URL and settings.
 */
@Component(
        service = AiTextService.class,
        immediate = true,
        configurationPolicy = ConfigurationPolicy.IGNORE,
        property = {"service.ranking:Integer=100"})
public class OllamaTextService implements AiTextService {

    private static final Logger log = LoggerFactory.getLogger(OllamaTextService.class);

    private static final String GENERATE_ENDPOINT = "/api/generate";

    @Reference
    private OllamaConfiguration ollamaConfig;

    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    @Activate
    protected void activate() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(ollamaConfig.getTimeout()))
                .build();

        if (ollamaConfig.isEnabled()) {
            log.info(
                    "Ollama text service activated with model: {} at {}",
                    ollamaConfig.getTextModel(),
                    ollamaConfig.getOllamaUrl());

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
        return ollamaConfig.isEnabled() && StringUtils.isNotBlank(ollamaConfig.getOllamaUrl());
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

        String languageName =
                java.util.Locale.forLanguageTag(targetLocale).getDisplayLanguage(java.util.Locale.ENGLISH);
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
        int maxRetries = ollamaConfig.getMaxRetries();
        int retryDelayMs = ollamaConfig.getRetryDelayMs();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String responseText = callOllamaApi(prompt);
                long processingTime = System.currentTimeMillis() - startTime;

                return AiResponse.success(responseText.trim(), getId());
            } catch (IOException e) {
                log.warn("Ollama API call failed (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());

                if (attempt == maxRetries) {
                    return AiResponse.failure(
                            "Ollama API error after " + maxRetries
                                    + " attempts: "
                                    + e.getMessage()
                                    + ". Is Ollama running?",
                            getId());
                }

                // Short retry delay for local service
                try {
                    Thread.sleep(retryDelayMs);
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
        String url = ollamaConfig.getOllamaUrl().replaceAll("/$", "") + GENERATE_ENDPOINT;

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", ollamaConfig.getTextModel());
        requestBody.put("prompt", prompt);
        requestBody.put("stream", ollamaConfig.isStream());

        ObjectNode options = requestBody.putObject("options");
        options.put("temperature", ollamaConfig.getTemperature());

        String requestJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(ollamaConfig.getTimeout()))
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
            String url = ollamaConfig.getOllamaUrl().replaceAll("/$", "") + "/api/tags";
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Successfully connected to Ollama at {}", ollamaConfig.getOllamaUrl());
            } else {
                log.warn("Ollama connectivity test returned status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn(
                    "Failed to connect to Ollama at {}: {}. Make sure Ollama is running.",
                    ollamaConfig.getOllamaUrl(),
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
