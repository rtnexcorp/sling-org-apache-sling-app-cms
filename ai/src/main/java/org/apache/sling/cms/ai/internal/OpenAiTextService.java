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
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenAI implementation of AI text services using the OpenAI API.
 * Supports GPT-4, GPT-3.5-turbo and other OpenAI models.
 */
@Component(service = AiTextService.class, immediate = true)
@Designate(ocd = OpenAiTextService.Config.class)
public class OpenAiTextService implements AiTextService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiTextService.class);

    private static final String API_BASE_URL = "https://api.openai.com/v1";
    private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;

    @ObjectClassDefinition(
            name = "Apache Sling CMS - OpenAI Text Service",
            description = "OpenAI API integration for AI text operations")
    public @interface Config {

        @AttributeDefinition(name = "Enabled", description = "Enable the OpenAI text service")
        boolean enabled() default false;

        @AttributeDefinition(name = "API Key", description = "OpenAI API key (sk-...)")
        String apiKey() default "";

        @AttributeDefinition(
                name = "Model",
                description = "OpenAI model to use (e.g., gpt-4, gpt-3.5-turbo, gpt-4-turbo)")
        String model() default "gpt-4-turbo";

        @AttributeDefinition(
                name = "Temperature",
                description = "Sampling temperature (0.0 = deterministic, 2.0 = very creative)")
        double temperature() default 0.7;

        @AttributeDefinition(name = "Max Tokens", description = "Maximum tokens in response")
        int maxTokens() default 1000;

        @AttributeDefinition(name = "Timeout (seconds)", description = "Request timeout in seconds")
        int timeout() default 30;

        @AttributeDefinition(name = "Organization ID", description = "Optional OpenAI organization ID")
        String organizationId() default "";
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

        if (config.enabled() && StringUtils.isBlank(config.apiKey())) {
            log.warn("OpenAI service is enabled but API key is not configured");
        } else if (config.enabled()) {
            log.info("OpenAI text service activated with model: {}", config.model());
        }
    }

    @Deactivate
    protected void deactivate() {
        log.info("OpenAI text service deactivated");
    }

    @Override
    public String getId() {
        return "openai";
    }

    @Override
    public String getTitle() {
        return "OpenAI";
    }

    @Override
    public boolean isEnabled() {
        return config.enabled() && StringUtils.isNotBlank(config.apiKey());
    }

    @Override
    public boolean requiresExternalApi() {
        return true;
    }

    @Override
    public AiResponse summarize(AiRequest request) {
        if (!isEnabled()) {
            return AiResponse.skipped("OpenAI service is not enabled or configured");
        }

        String prompt = buildPrompt(
                "Summarize the following content in 2-3 sentences. Focus on the main points.", request.getContent());

        return executeRequest(request, prompt, "summarize");
    }

    @Override
    public AiResponse summarize(AiRequest request, int maxLength) {
        if (!isEnabled()) {
            return AiResponse.skipped("OpenAI service is not enabled or configured");
        }

        String prompt = buildPrompt(
                "Summarize the following content in no more than " + maxLength
                        + " characters. Keep it concise and focused on the main points.",
                request.getContent());

        return executeRequest(request, prompt, "summarize");
    }

    @Override
    public AiResponse suggestTitle(AiRequest request) {
        if (!isEnabled()) {
            return AiResponse.skipped("OpenAI service is not enabled or configured");
        }

        String prompt = buildPrompt(
                "Generate a concise, engaging title (max 60 characters) for the following content. Return only the title, no quotes or explanations.",
                request.getContent());

        return executeRequest(request, prompt, "suggest-title");
    }

    @Override
    public AiResponse suggestMetaDescription(AiRequest request) {
        if (!isEnabled()) {
            return AiResponse.skipped("OpenAI service is not enabled or configured");
        }

        String prompt = buildPrompt(
                "Generate a compelling meta description (max 155 characters) for the following content. Return only the description, no quotes.",
                request.getContent());

        return executeRequest(request, prompt, "suggest-meta-description");
    }

    @Override
    public AiResponse rewrite(AiRequest request, Tone tone) {
        if (!isEnabled()) {
            return AiResponse.skipped("OpenAI service is not enabled or configured");
        }

        String toneInstruction = getToneInstruction(tone);
        String prompt = buildPrompt(
                "Rewrite the following content in a " + toneInstruction
                        + " tone. Preserve the key information but adjust the style.",
                request.getContent());

        return executeRequest(request, prompt, "rewrite-" + tone.name().toLowerCase());
    }

    @Override
    public AiResponse translate(AiRequest request, String targetLocale) {
        if (!isEnabled()) {
            return AiResponse.skipped("OpenAI service is not enabled or configured");
        }

        String languageName = new java.util.Locale(targetLocale).getDisplayLanguage(java.util.Locale.ENGLISH);
        String prompt = buildPrompt(
                "Translate the following content to " + languageName + ". Maintain the tone and structure.",
                request.getContent());

        return executeRequest(request, prompt, "translate-" + targetLocale);
    }

    @Override
    public AiResponse explain(AiRequest request) {
        if (!isEnabled()) {
            return AiResponse.skipped("OpenAI service is not enabled or configured");
        }

        String prompt = buildPrompt(
                "Explain what this content is about in simple terms. What is the main message or purpose?",
                request.getContent());

        return executeRequest(request, prompt, "explain");
    }

    @Override
    public AiResponse summarizeChanges(String originalContent, String updatedContent) {
        if (!isEnabled()) {
            return AiResponse.skipped("OpenAI service is not enabled or configured");
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
                String responseText = callOpenAiApi(prompt);
                long processingTime = System.currentTimeMillis() - startTime;

                return AiResponse.success(responseText.trim(), getId());
            } catch (IOException e) {
                log.warn("OpenAI API call failed (attempt {}/{}): {}", attempt, MAX_RETRIES, e.getMessage());

                if (attempt == MAX_RETRIES) {
                    return AiResponse.failure(
                            "OpenAI API error after " + MAX_RETRIES + " attempts: " + e.getMessage(), getId());
                }

                // Exponential backoff
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return AiResponse.failure("Request interrupted", getId());
                }
            }
        }

        return AiResponse.failure("Unexpected error in OpenAI request", getId());
    }

    /**
     * Call the OpenAI API with the given prompt
     */
    private String callOpenAiApi(String prompt) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", config.model());
        requestBody.put("temperature", config.temperature());
        requestBody.put("max_tokens", config.maxTokens());

        ArrayNode messages = requestBody.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        message.put("content", prompt);

        String requestJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + CHAT_COMPLETIONS_ENDPOINT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.apiKey())
                .timeout(Duration.ofSeconds(config.timeout()))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8));

        // Add organization header if configured
        if (StringUtils.isNotBlank(config.organizationId())) {
            requestBuilder.header("OpenAI-Organization", config.organizationId());
        }

        try {
            HttpResponse<String> response =
                    httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new IOException("OpenAI API returned status " + response.statusCode() + ": " + response.body());
            }

            return parseOpenAiResponse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /**
     * Parse the OpenAI API response and extract the generated text
     */
    private String parseOpenAiResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.get("choices");

        if (choices == null || !choices.isArray() || choices.size() == 0) {
            throw new IOException("Invalid OpenAI response: no choices found");
        }

        JsonNode firstChoice = choices.get(0);
        JsonNode message = firstChoice.get("message");

        if (message == null) {
            throw new IOException("Invalid OpenAI response: no message found");
        }

        JsonNode content = message.get("content");
        if (content == null) {
            throw new IOException("Invalid OpenAI response: no content found");
        }

        return content.asText();
    }

    /**
     * Build a prompt with context
     */
    private String buildPrompt(String instruction, String content) {
        return instruction + "\n\n" + content;
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
