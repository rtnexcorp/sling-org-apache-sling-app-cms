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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.cms.ai.AiImageService;
import org.apache.sling.cms.ai.AiResponse;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenAI implementation of AI image services using GPT-4 Vision API.
 * Supports image analysis, alt-text generation, and caption generation.
 * Uses centralized OpenAI configuration for API key and settings.
 */
@Component(service = AiImageService.class, immediate = true)
public class OpenAiImageService implements AiImageService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiImageService.class);

    private static final String API_BASE_URL = "https://api.openai.com/v1";
    private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;
    private static final int MAX_IMAGE_SIZE_BYTES = 20 * 1024 * 1024; // 20MB
    private static final String SERVICE_USER = "sling-cms-ai";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private OpenAiConfiguration openAiConfig;

    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    @Activate
    protected void activate() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(openAiConfig.getTimeout()))
                .build();

        if (openAiConfig.isEnabled() && StringUtils.isBlank(openAiConfig.getApiKey())) {
            log.warn("OpenAI image service is enabled but API key is not configured");
        } else if (openAiConfig.isEnabled()) {
            log.info("OpenAI image service activated with model: {}", openAiConfig.getVisionModel());
        }
    }

    @Deactivate
    protected void deactivate() {
        log.info("OpenAI image service deactivated");
    }

    @Override
    public String getId() {
        return "openai-vision";
    }

    @Override
    public String getTitle() {
        return "OpenAI Vision";
    }

    @Override
    public boolean isEnabled() {
        return openAiConfig.isEnabled() && StringUtils.isNotBlank(openAiConfig.getApiKey());
    }

    @Override
    public boolean requiresExternalApi() {
        return true;
    }

    @Override
    public AiResponse generateAltText(InputStream imageStream, String mimeType, String context) {
        if (!isEnabled()) {
            return AiResponse.skipped("OpenAI image service is not enabled or configured");
        }

        if (!supportsMimeType(mimeType)) {
            return AiResponse.failure("Unsupported image MIME type: " + mimeType, getId());
        }

        try {
            byte[] imageBytes = readImageBytes(imageStream);
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String prompt = buildAltTextPrompt(context);
            return executeVisionRequest(base64Image, mimeType, prompt, "generate-alt-text");

        } catch (IOException e) {
            log.error("Error reading image for alt-text generation", e);
            return AiResponse.failure("Error reading image: " + e.getMessage(), getId());
        }
    }

    @Override
    public AiResponse generateAltText(String imagePath, String context) {
        if (!isEnabled()) {
            return AiResponse.skipped("OpenAI image service is not enabled or configured");
        }

        ResourceResolver resolver = null;
        try {
            resolver = resolverFactory.getServiceResourceResolver(
                    Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_USER));
            Resource imageResource = resolver.getResource(imagePath);

            if (imageResource == null) {
                return AiResponse.failure("Image not found at path: " + imagePath, getId());
            }

            // Get the image content
            Resource contentResource = imageResource.getChild("jcr:content");
            if (contentResource == null) {
                return AiResponse.failure("Image content not found at path: " + imagePath, getId());
            }

            InputStream imageStream = contentResource.adaptTo(InputStream.class);
            if (imageStream == null) {
                return AiResponse.failure("Cannot read image content at path: " + imagePath, getId());
            }

            String mimeType = contentResource.getValueMap().get("jcr:mimeType", String.class);
            if (mimeType == null) {
                mimeType = "image/jpeg"; // Default fallback
            }

            return generateAltText(imageStream, mimeType, context);

        } catch (Exception e) {
            log.error("Error generating alt-text for image at path: " + imagePath, e);
            return AiResponse.failure("Error generating alt-text: " + e.getMessage(), getId());
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }

    @Override
    public AiResponse generateCaption(InputStream imageStream, String mimeType, String context) {
        if (!isEnabled()) {
            return AiResponse.skipped("OpenAI image service is not enabled or configured");
        }

        if (!supportsMimeType(mimeType)) {
            return AiResponse.failure("Unsupported image MIME type: " + mimeType, getId());
        }

        try {
            byte[] imageBytes = readImageBytes(imageStream);
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String prompt = buildCaptionPrompt(context);
            return executeVisionRequest(base64Image, mimeType, prompt, "generate-caption");

        } catch (IOException e) {
            log.error("Error reading image for caption generation", e);
            return AiResponse.failure("Error reading image: " + e.getMessage(), getId());
        }
    }

    @Override
    public AiResponse generateCaption(String imagePath, String context) {
        if (!isEnabled()) {
            return AiResponse.skipped("OpenAI image service is not enabled or configured");
        }

        ResourceResolver resolver = null;
        try {
            resolver = resolverFactory.getServiceResourceResolver(
                    Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_USER));
            Resource imageResource = resolver.getResource(imagePath);

            if (imageResource == null) {
                return AiResponse.failure("Image not found at path: " + imagePath, getId());
            }

            Resource contentResource = imageResource.getChild("jcr:content");
            if (contentResource == null) {
                return AiResponse.failure("Image content not found at path: " + imagePath, getId());
            }

            InputStream imageStream = contentResource.adaptTo(InputStream.class);
            if (imageStream == null) {
                return AiResponse.failure("Cannot read image content at path: " + imagePath, getId());
            }

            String mimeType = contentResource.getValueMap().get("jcr:mimeType", String.class);
            if (mimeType == null) {
                mimeType = "image/jpeg";
            }

            return generateCaption(imageStream, mimeType, context);

        } catch (Exception e) {
            log.error("Error generating caption for image at path: " + imagePath, e);
            return AiResponse.failure("Error generating caption: " + e.getMessage(), getId());
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }

    @Override
    public AiResponse describeImage(InputStream imageStream, String mimeType) {
        if (!isEnabled()) {
            return AiResponse.skipped("OpenAI image service is not enabled or configured");
        }

        if (!supportsMimeType(mimeType)) {
            return AiResponse.failure("Unsupported image MIME type: " + mimeType, getId());
        }

        try {
            byte[] imageBytes = readImageBytes(imageStream);
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String prompt =
                    "Describe this image in detail. Include what you see, the setting, any notable objects or people, colors, and the overall mood or atmosphere.";
            return executeVisionRequest(base64Image, mimeType, prompt, "describe-image");

        } catch (IOException e) {
            log.error("Error reading image for description", e);
            return AiResponse.failure("Error reading image: " + e.getMessage(), getId());
        }
    }

    @Override
    public AiResponse describeImage(String imagePath) {
        if (!isEnabled()) {
            return AiResponse.skipped("OpenAI image service is not enabled or configured");
        }

        ResourceResolver resolver = null;
        try {
            resolver = resolverFactory.getServiceResourceResolver(
                    Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_USER));
            Resource imageResource = resolver.getResource(imagePath);

            if (imageResource == null) {
                return AiResponse.failure("Image not found at path: " + imagePath, getId());
            }

            Resource contentResource = imageResource.getChild("jcr:content");
            if (contentResource == null) {
                return AiResponse.failure("Image content not found at path: " + imagePath, getId());
            }

            InputStream imageStream = contentResource.adaptTo(InputStream.class);
            if (imageStream == null) {
                return AiResponse.failure("Cannot read image content at path: " + imagePath, getId());
            }

            String mimeType = contentResource.getValueMap().get("jcr:mimeType", String.class);
            if (mimeType == null) {
                mimeType = "image/jpeg";
            }

            return describeImage(imageStream, mimeType);

        } catch (Exception e) {
            log.error("Error describing image at path: " + imagePath, e);
            return AiResponse.failure("Error describing image: " + e.getMessage(), getId());
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }

    @Override
    public boolean supportsMimeType(String mimeType) {
        if (StringUtils.isBlank(mimeType)) {
            return false;
        }

        // OpenAI Vision API supports: PNG, JPEG, WEBP, GIF (non-animated)
        return mimeType.equals("image/png")
                || mimeType.equals("image/jpeg")
                || mimeType.equals("image/jpg")
                || mimeType.equals("image/webp")
                || mimeType.equals("image/gif");
    }

    /**
     * Execute a vision API request with retry logic
     */
    private AiResponse executeVisionRequest(String base64Image, String mimeType, String prompt, String operation) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String responseText = callOpenAiVisionApi(base64Image, mimeType, prompt);

                return AiResponse.success(responseText.trim(), getId());

            } catch (IOException e) {
                log.warn("OpenAI Vision API call failed (attempt {}/{}): {}", attempt, MAX_RETRIES, e.getMessage());

                if (attempt == MAX_RETRIES) {
                    return AiResponse.failure(
                            "OpenAI Vision API error after " + MAX_RETRIES + " attempts: " + e.getMessage(), getId());
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

        return AiResponse.failure("Unexpected error in OpenAI Vision request", getId());
    }

    /**
     * Call the OpenAI Vision API
     */
    private String callOpenAiVisionApi(String base64Image, String mimeType, String prompt) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", openAiConfig.getVisionModel());
        requestBody.put("max_tokens", openAiConfig.getVisionMaxTokens());

        ArrayNode messages = requestBody.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");

        // Build content array with text and image
        ArrayNode content = message.putArray("content");

        // Text part
        ObjectNode textPart = content.addObject();
        textPart.put("type", "text");
        textPart.put("text", prompt);

        // Image part
        ObjectNode imagePart = content.addObject();
        imagePart.put("type", "image_url");
        ObjectNode imageUrl = imagePart.putObject("image_url");
        imageUrl.put("url", "data:" + mimeType + ";base64," + base64Image);
        imageUrl.put("detail", openAiConfig.getVisionDetail());

        String requestJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + CHAT_COMPLETIONS_ENDPOINT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openAiConfig.getApiKey())
                .timeout(Duration.ofSeconds(openAiConfig.getTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8));

        // Add organization header if configured
        if (StringUtils.isNotBlank(openAiConfig.getOrganizationId())) {
            requestBuilder.header("OpenAI-Organization", openAiConfig.getOrganizationId());
        }

        try {
            HttpResponse<String> response =
                    httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new IOException(
                        "OpenAI Vision API returned status " + response.statusCode() + ": " + response.body());
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
     * Read image bytes from input stream
     */
    private byte[] readImageBytes(InputStream imageStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(imageStream, baos);
        byte[] imageBytes = baos.toByteArray();

        if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
            throw new IOException(
                    "Image size exceeds maximum allowed size of " + (MAX_IMAGE_SIZE_BYTES / 1024 / 1024) + "MB");
        }

        return imageBytes;
    }

    /**
     * Build prompt for alt-text generation
     */
    private String buildAltTextPrompt(String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate concise, descriptive alt-text for this image suitable for accessibility purposes. ");
        prompt.append(
                "The alt-text should be clear and informative, describing the essential content of the image in one or two sentences. ");
        prompt.append("Do not include phrases like 'image of' or 'picture of'. ");
        prompt.append("Return only the alt-text, no quotes or additional explanation.");

        if (StringUtils.isNotBlank(context)) {
            prompt.append("\n\nContext: ").append(context);
        }

        return prompt.toString();
    }

    /**
     * Build prompt for caption generation
     */
    private String buildCaptionPrompt(String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate an engaging caption for this image. ");
        prompt.append(
                "The caption should be descriptive and may include contextual information beyond what's visible. ");
        prompt.append("Keep it concise but informative (2-3 sentences). ");
        prompt.append("Return only the caption, no quotes or additional explanation.");

        if (StringUtils.isNotBlank(context)) {
            prompt.append("\n\nContext: ").append(context);
        }

        return prompt.toString();
    }
}
