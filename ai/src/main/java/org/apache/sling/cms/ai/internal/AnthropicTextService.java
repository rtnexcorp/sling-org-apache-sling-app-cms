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
import com.fasterxml.jackson.databind.node.ArrayNode;
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
 * Anthropic Claude implementation of AI text services.
 * Supports Claude 3 models (Opus, Sonnet, Haiku).
 * Uses centralized Anthropic configuration for API key and settings.
 */
@Component(
        service = AiTextService.class,
        immediate = true,
        configurationPolicy = ConfigurationPolicy.IGNORE,
        property = {"service.ranking:Integer=100"})
public class AnthropicTextService implements AiTextService {

    private static final Logger log = LoggerFactory.getLogger(AnthropicTextService.class);

    private static final String MESSAGES_ENDPOINT = "/messages";

    @Reference
    private AnthropicConfiguration anthropicConfig;

    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    @Activate
    protected void activate() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(anthropicConfig.getTimeout()))
                .build();

        if (anthropicConfig.isEnabled() && StringUtils.isBlank(anthropicConfig.getApiKey())) {
            log.warn("Anthropic service is enabled but API key is not configured");
        } else if (anthropicConfig.isEnabled()) {
            log.info("Anthropic text service activated with model: {}", anthropicConfig.getTextModel());
        }
    }

    @Deactivate
    protected void deactivate() {
        log.info("Anthropic text service deactivated");
    }

    @Override
    public String getId() {
        return "anthropic";
    }

    @Override
    public String getTitle() {
        return "Anthropic Claude";
    }

    @Override
    public boolean isEnabled() {
        return anthropicConfig.isEnabled() && StringUtils.isNotBlank(anthropicConfig.getApiKey());
    }

    @Override
    public boolean requiresExternalApi() {
        return true;
    }

    @Override
    public AiResponse summarize(AiRequest request) {
        if (!isEnabled()) {
            return AiResponse.skipped("Anthropic service is not enabled or configured");
        }

        String prompt = "Summarize the following content in 2-3 sentences. Focus on the main points.\n\n"
                + request.getContent();

        return executeRequest(request, prompt, "summarize");
    }

    @Override
    public AiResponse summarize(AiRequest request, int maxLength) {
        if (!isEnabled()) {
            return AiResponse.skipped("Anthropic service is not enabled or configured");
        }

        String prompt = "Summarize the following content in no more than " + maxLength
                + " characters. Keep it concise and focused on the main points.\n\n"
                + request.getContent();

        return executeRequest(request, prompt, "summarize");
    }

    @Override
    public AiResponse suggestTitle(AiRequest request) {
        if (!isEnabled()) {
            return AiResponse.skipped("Anthropic service is not enabled or configured");
        }

        String prompt =
                "Generate a concise, engaging title (max 60 characters) for the following content. Return only the title, no quotes or explanations.\n\n"
                        + request.getContent();

        return executeRequest(request, prompt, "suggest-title");
    }

    @Override
    public AiResponse suggestMetaDescription(AiRequest request) {
        if (!isEnabled()) {
            return AiResponse.skipped("Anthropic service is not enabled or configured");
        }

        String prompt =
                "Generate a compelling meta description (max 155 characters) for the following content. Return only the description, no quotes.\n\n"
                        + request.getContent();

        return executeRequest(request, prompt, "suggest-meta-description");
    }

    @Override
    public AiResponse rewrite(AiRequest request, Tone tone) {
        if (!isEnabled()) {
            return AiResponse.skipped("Anthropic service is not enabled or configured");
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
            return AiResponse.skipped("Anthropic service is not enabled or configured");
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
            return AiResponse.skipped("Anthropic service is not enabled or configured");
        }

        String prompt = "Explain what this content is about in simple terms. What is the main message or purpose?\n\n"
                + request.getContent();

        return executeRequest(request, prompt, "explain");
    }

    @Override
    public AiResponse summarizeChanges(String originalContent, String updatedContent) {
        if (!isEnabled()) {
            return AiResponse.skipped("Anthropic service is not enabled or configured");
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
        int maxRetries = anthropicConfig.getMaxRetries();
        int retryDelayMs = anthropicConfig.getRetryDelayMs();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String responseText = callAnthropicApi(prompt);
                long processingTime = System.currentTimeMillis() - startTime;

                return AiResponse.success(responseText.trim(), getId());
            } catch (IOException e) {
                log.warn("Anthropic API call failed (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());

                if (attempt == maxRetries) {
                    return AiResponse.failure(
                            "Anthropic API error after " + maxRetries + " attempts: " + e.getMessage(), getId());
                }

                // Exponential backoff
                try {
                    Thread.sleep(retryDelayMs * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return AiResponse.failure("Request interrupted", getId());
                }
            }
        }

        return AiResponse.failure("Unexpected error in Anthropic request", getId());
    }

    /**
     * Call the Anthropic API with the given prompt
     */
    private String callAnthropicApi(String prompt) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", anthropicConfig.getTextModel());
        requestBody.put("max_tokens", anthropicConfig.getTextMaxTokens());
        requestBody.put("temperature", anthropicConfig.getTemperature());

        ArrayNode messages = requestBody.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        message.put("content", prompt);

        String requestJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(anthropicConfig.getApiBaseUrl() + MESSAGES_ENDPOINT))
                .header("Content-Type", "application/json")
                .header("x-api-key", anthropicConfig.getApiKey())
                .header("anthropic-version", anthropicConfig.getApiVersion())
                .timeout(Duration.ofSeconds(anthropicConfig.getTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new IOException(
                        "Anthropic API returned status " + response.statusCode() + ": " + response.body());
            }

            return parseAnthropicResponse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /**
     * Parse the Anthropic API response and extract the generated text
     */
    private String parseAnthropicResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode content = root.get("content");

        if (content == null || !content.isArray() || content.size() == 0) {
            throw new IOException("Invalid Anthropic response: no content found");
        }

        JsonNode firstContent = content.get(0);
        JsonNode text = firstContent.get("text");

        if (text == null) {
            throw new IOException("Invalid Anthropic response: no text found");
        }

        return text.asText();
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
