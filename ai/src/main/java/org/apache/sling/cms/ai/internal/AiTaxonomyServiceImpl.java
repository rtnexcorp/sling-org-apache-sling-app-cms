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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.TaxonomyService;
import org.apache.sling.cms.ai.AiRequest;
import org.apache.sling.cms.ai.AiResponse;
import org.apache.sling.cms.ai.AiTaxonomyService;
import org.apache.sling.cms.ai.AiTextService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi service implementation for AI-powered taxonomy and classification.
 */
@Component(service = AiTaxonomyService.class)
public class AiTaxonomyServiceImpl implements AiTaxonomyService {

    private static final Logger log = LoggerFactory.getLogger(AiTaxonomyServiceImpl.class);
    private static final String PROVIDER_ID = "ai-taxonomy";
    private static final String PROVIDER_TITLE = "AI Taxonomy Service";

    @Reference
    private TaxonomyService taxonomyService;

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private volatile List<AiTextService> textServices;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getTitle() {
        return PROVIDER_TITLE;
    }

    @Override
    public boolean isEnabled() {
        return !getAvailableTextServices().isEmpty();
    }

    @Override
    public boolean requiresExternalApi() {
        return true;
    }

    @Override
    public TaxonomyAnalysisResult suggestTags(
            String content, ResourceResolver resolver, String taxonomyPath, int maxSuggestions, double minConfidence) {
        if (content == null || content.trim().isEmpty()) {
            return createEmptyResult();
        }

        try {
            List<String> availableTags = getAvailableTaxonomyTags(resolver, taxonomyPath);
            if (availableTags.isEmpty()) {
                return createEmptyResult();
            }

            String prompt = buildTagSuggestionPrompt(content, availableTags, maxSuggestions);
            AiTextService textService = getPreferredTextService();
            if (textService == null) {
                return createEmptyResult();
            }

            // Create AI request
            AiRequest aiRequest = AiRequest.builder()
                    .content(prompt)
                    .contextHints(List.of("taxonomy-suggestion", "classification"))
                    .build();

            // Get response from AI using explain method (most appropriate for analysis tasks)
            AiResponse aiResponse = textService.explain(aiRequest);
            String response = aiResponse.getContent();

            List<TaxonomySuggestion> suggestions =
                    parseTagSuggestions(response, resolver, taxonomyPath, availableTags, minConfidence);

            List<TaxonomySuggestion> limitedSuggestions =
                    suggestions.stream().limit(maxSuggestions).collect(Collectors.toList());

            return TaxonomyAnalysisResultImpl.builder()
                    .suggestions(limitedSuggestions)
                    .maxSuggestions(maxSuggestions)
                    .minConfidence(minConfidence)
                    .build();
        } catch (Exception e) {
            log.error("Error analyzing tags", e);
            return createEmptyResult();
        }
    }

    @Override
    public TaxonomyAnalysisResult suggestCategories(
            String content, ResourceResolver resolver, String categoryPath, int maxSuggestions, double minConfidence) {
        // Categories are just taxonomy tags from a specific path, so we reuse the same logic
        return suggestTags(content, resolver, categoryPath, maxSuggestions, minConfidence);
    }

    private List<String> getAvailableTaxonomyTags(ResourceResolver resolver, String taxonomyPath) {
        List<String> tags = new ArrayList<>();
        Resource taxonomyResource = resolver.getResource(taxonomyPath);
        if (taxonomyResource != null) {
            collectTaxonomyTags(taxonomyResource, tags, "");
        }
        return tags;
    }

    private void collectTaxonomyTags(Resource resource, List<String> tags, String parentPath) {
        for (Resource child : resource.getChildren()) {
            String tagTitle = child.getValueMap().get("jcr:title", String.class);
            if (tagTitle != null) {
                String fullPath = parentPath.isEmpty() ? tagTitle : parentPath + "/" + tagTitle;
                tags.add(fullPath);
                collectTaxonomyTags(child, tags, fullPath);
            }
        }
    }

    private String buildTagSuggestionPrompt(String content, List<String> availableTags, int maxSuggestions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze content and suggest relevant tags.\n\nContent:\n")
                .append(content);
        prompt.append("\n\nAvailable Tags:\n");
        for (String tag : availableTags) {
            prompt.append("- ").append(tag).append("\n");
        }
        prompt.append("\nRespond with JSON array: [{\"taxonomyPath\":\"/path\",\"taxonomyTitle\":\"Title\",");
        prompt.append("\"confidence\":0.95,\"reason\":\"explanation\"}]\n");
        prompt.append("Maximum ").append(maxSuggestions).append(" tags.\n");
        return prompt.toString();
    }

    private List<TaxonomySuggestion> parseTagSuggestions(
            String response,
            ResourceResolver resolver,
            String taxonomyBasePath,
            List<String> availableTags,
            double minConfidence) {
        List<TaxonomySuggestion> suggestions = new ArrayList<>();
        try {
            String jsonStr = extractJsonArray(response);
            JsonNode jsonArray = objectMapper.readTree(jsonStr);
            if (!jsonArray.isArray()) {
                return suggestions;
            }

            for (JsonNode item : jsonArray) {
                String taxonomyTitle =
                        item.has("taxonomyTitle") ? item.get("taxonomyTitle").asText() : null;
                String taxonomyPath =
                        item.has("taxonomyPath") ? item.get("taxonomyPath").asText() : null;
                double confidence =
                        item.has("confidence") ? item.get("confidence").asDouble() : 0.0;
                String reason = item.has("reason") ? item.get("reason").asText() : "";

                if (confidence >= minConfidence
                        && taxonomyTitle != null
                        && !taxonomyTitle.trim().isEmpty()) {
                    if (taxonomyPath == null || taxonomyPath.trim().isEmpty()) {
                        taxonomyPath = taxonomyBasePath + "/"
                                + taxonomyTitle.toLowerCase().replaceAll("\\s+", "-");
                    }
                    suggestions.add(TaxonomySuggestionImpl.builder()
                            .taxonomyPath(taxonomyPath)
                            .taxonomyTitle(taxonomyTitle)
                            .confidence(confidence)
                            .reason(reason)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Error parsing tag suggestions", e);
        }
        suggestions.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
        return suggestions;
    }

    private String extractJsonArray(String response) {
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private AiTextService getPreferredTextService() {
        List<AiTextService> services = getAvailableTextServices();
        return services.isEmpty() ? null : services.get(0);
    }

    private List<AiTextService> getAvailableTextServices() {
        if (textServices == null) {
            return new ArrayList<>();
        }
        return textServices.stream().filter(AiTextService::isEnabled).collect(Collectors.toList());
    }

    private TaxonomyAnalysisResult createEmptyResult() {
        return TaxonomyAnalysisResultImpl.builder()
                .suggestions(new ArrayList<>())
                .maxSuggestions(0)
                .minConfidence(0.0)
                .build();
    }
}
