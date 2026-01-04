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
 * Azure OpenAI implementation of AI text services.
 * Uses Azure's OpenAI Service with enterprise-grade security and compliance.
 * Uses centralized Azure OpenAI configuration for endpoint, API key, and settings.
 */
@Component(
        service = AiTextService.class,
        immediate = true,
        configurationPolicy = ConfigurationPolicy.IGNORE,
        property = {"service.ranking:Integer=100"})
public class AzureOpenAiTextService implements AiTextService {

    private static final Logger log = LoggerFactory.getLogger(AzureOpenAiTextService.class);

    @Reference
    private AzureOpenAiConfiguration azureConfig;

    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    @Activate
    protected void activate() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(azureConfig.getTimeout()))
                .build();

        if (azureConfig.isEnabled() && StringUtils.isBlank(azureConfig.getEndpoint())) {
            log.warn("Azure OpenAI service is enabled but endpoint is not configured");
        } else if (azureConfig.isEnabled() && StringUtils.isBlank(azureConfig.getApiKey())) {
            log.warn("Azure OpenAI service is enabled but API key is not configured");
        } else if (azureConfig.isEnabled() && StringUtils.isBlank(azureConfig.getTextDeploymentName())) {
            log.warn("Azure OpenAI service is enabled but text deployment name is not configured");
        } else if (azureConfig.isEnabled()) {
            log.info("Azure OpenAI text service activated with deployment: {}", azureConfig.getTextDeploymentName());
        }
    }

    @Deactivate
    protected void deactivate() {
        log.info("Azure OpenAI text service deactivated");
    }

    @Override
    public String getId() {
        return "azure-openai";
    }

    @Override
    public String getTitle() {
        return "Azure OpenAI";
    }

    @Override
    public boolean isEnabled() {
        return (azureConfig.isEnabled()
                && StringUtils.isNotBlank(azureConfig.getEndpoint())
                && StringUtils.isNotBlank(azureConfig.getApiKey())
                && StringUtils.isNotBlank(azureConfig.getTextDeploymentName()));
    }

    @Override
    public boolean requiresExternalApi() {
        return true;
    }

    @Override
    public AiResponse summarize(AiRequest request) {
        if (!isEnabled()) {
            return AiResponse.skipped("Azure OpenAI service is not enabled or configured");
        }

        String prompt = buildPrompt(
                "Summarize the following content in 2-3 sentences. Focus on the main points.", request.getContent());

        return executeRequest(request, prompt, "summarize");
    }

    @Override
    public AiResponse summarize(AiRequest request, int maxLength) {
        if (!isEnabled()) {
            return AiResponse.skipped("Azure OpenAI service is not enabled or configured");
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
            return AiResponse.skipped("Azure OpenAI service is not enabled or configured");
        }

        String prompt =
                "Generate a concise, engaging title (max 60 characters) for the following content. Return only the title, no quotes or explanations.\n\n"
                        + request.getContent();

        return executeRequest(request, prompt, "suggest-title");
    }

    @Override
    public AiResponse suggestMetaDescription(AiRequest request) {
        if (!isEnabled()) {
            return AiResponse.skipped("Azure OpenAI service is not enabled or configured");
        }

        String prompt =
                "Generate a compelling meta description (max 155 characters) for the following content. Return only the description, no quotes.\n\n"
                        + request.getContent();

        return executeRequest(request, prompt, "suggest-meta-description");
    }

    @Override
    public AiResponse rewrite(AiRequest request, Tone tone) {
        if (!isEnabled()) {
            return AiResponse.skipped("Azure OpenAI service is not enabled or configured");
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
            return AiResponse.skipped("Azure OpenAI service is not enabled or configured");
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
            return AiResponse.skipped("Azure OpenAI service is not enabled or configured");
        }

        String prompt = "Explain what this content is about in simple terms. What is the main message or purpose?\n\n"
                + request.getContent();

        return executeRequest(request, prompt, "explain");
    }

    @Override
    public AiResponse summarizeChanges(String originalContent, String updatedContent) {
        if (!isEnabled()) {
            return AiResponse.skipped("Azure OpenAI service is not enabled or configured");
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
        int maxRetries = azureConfig.getMaxRetries();
        int retryDelayMs = azureConfig.getRetryDelayMs();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String responseText = callAzureOpenAiApi(prompt);
                long processingTime = System.currentTimeMillis() - startTime;

                return AiResponse.success(responseText.trim(), getId());
            } catch (IOException e) {
                log.warn("Azure OpenAI API call failed (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());

                if (attempt == maxRetries) {
                    return AiResponse.failure(
                            "Azure OpenAI API error after " + maxRetries + " attempts: " + e.getMessage(), getId());
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

        return AiResponse.failure("Unexpected error in Azure OpenAI request", getId());
    }

    /**
     * Call the Azure OpenAI API with the given prompt
     */
    private String callAzureOpenAiApi(String prompt) throws IOException {
        // Build Azure OpenAI endpoint URL
        String url = String.format(
                "%s/openai/deployments/%s/chat/completions?api-version=%s",
                azureConfig.getEndpoint().replaceAll("/$", ""),
                azureConfig.getTextDeploymentName(),
                azureConfig.getApiVersion());

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("temperature", azureConfig.getTemperature());
        requestBody.put("max_tokens", azureConfig.getTextMaxTokens());

        ArrayNode messages = requestBody.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        message.put("content", prompt);

        String requestJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("api-key", azureConfig.getApiKey())
                .timeout(Duration.ofSeconds(azureConfig.getTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new IOException(
                        "Azure OpenAI API returned status " + response.statusCode() + ": " + response.body());
            }

            return parseAzureOpenAiResponse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /**
     * Parse the Azure OpenAI API response and extract the generated text
     */
    private String parseAzureOpenAiResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.get("choices");

        if (choices == null || !choices.isArray() || choices.size() == 0) {
            throw new IOException("Invalid Azure OpenAI response: no choices found");
        }

        JsonNode firstChoice = choices.get(0);
        JsonNode message = firstChoice.get("message");

        if (message == null) {
            throw new IOException("Invalid Azure OpenAI response: no message found");
        }

        JsonNode content = message.get("content");
        if (content == null) {
            throw new IOException("Invalid Azure OpenAI response: no content found");
        }

        return content.asText();
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

    /**
     * Build prompt with content
     */
    private String buildPrompt(String instruction, String content) {
        return instruction + "\n\n" + content;
    }
}
