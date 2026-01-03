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
package org.apache.sling.cms.ai.servlets;

import javax.jcr.query.Query;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.cms.ai.AiRequest;
import org.apache.sling.cms.ai.AiResponse;
import org.apache.sling.cms.ai.AiTextService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for generating AI-powered taxonomy suggestions based on content analysis.
 * <p>
 * Endpoint: POST /bin/cms/ai/suggest-taxonomy
 * </p>
 * <p>
 * Parameters:
 * </p>
 * <ul>
 * <li>type - Type of taxonomy suggestion: "content-taxonomy", "semantic-taxonomy", "context-taxonomy"</li>
 * <li>content - The content to analyze for taxonomy suggestions</li>
 * <li>taxonomyBase - (optional) Base path for taxonomy items (e.g., /content/taxonomies)</li>
 * <li>maxSuggestions - (optional) Maximum number of suggestions to return (default: 5)</li>
 * </ul>
 * <p>
 * Response format:
 * </p>
 * <pre>
 * {
 *   "success": true,
 *   "suggestions": [
 *     {
 *       "path": "/content/taxonomies/topics/ai",
 *       "title": "Artificial Intelligence",
 *       "confidence": 0.95
 *     }
 *   ],
 *   "provider": "OpenAI GPT-4"
 * }
 * </pre>
 */
@Component(
        service = Servlet.class,
        property = {"sling.servlet.methods=POST", "sling.servlet.paths=/bin/cms/ai/suggest-taxonomy.json"})
public class AiTaxonomySuggestionServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AiTaxonomySuggestionServlet.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_MAX_SUGGESTIONS = 5;

    private static final String DEFAULT_TAXONOMY_BASE = "/etc/taxonomy";
    private static final String FALLBACK_TAXONOMY_BASE = "/content/taxonomies";

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private volatile List<AiTextService> aiTextServices;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String type = request.getParameter("type");
        String content = request.getParameter("content");
        String taxonomyBase = request.getParameter("taxonomyBase");
        String maxSuggestionsParam = request.getParameter("maxSuggestions");

        if (StringUtils.isBlank(type) || StringUtils.isBlank(content)) {
            sendErrorResponse(response, 400, "Missing required parameters: type and content");
            return;
        }

        int maxSuggestions = DEFAULT_MAX_SUGGESTIONS;
        if (StringUtils.isNotBlank(maxSuggestionsParam)) {
            try {
                maxSuggestions = Integer.parseInt(maxSuggestionsParam);
            } catch (NumberFormatException e) {
                log.warn("Invalid maxSuggestions parameter: {}", maxSuggestionsParam);
            }
        }

        // Get the best AI service
        AiTextService aiService = getBestAiService();
        if (aiService == null) {
            sendErrorResponse(response, 503, "No AI service available");
            return;
        }

        // Load available taxonomies from repository
        List<TaxonomyOption> availableTaxonomies = loadTaxonomies(request.getResourceResolver(), taxonomyBase);
        if (availableTaxonomies.isEmpty()) {
            // Fallback for older/newer repo layouts
            if (StringUtils.isBlank(taxonomyBase)) {
                availableTaxonomies = loadTaxonomies(request.getResourceResolver(), FALLBACK_TAXONOMY_BASE);
                taxonomyBase = FALLBACK_TAXONOMY_BASE;
            }
        }
        if (availableTaxonomies.isEmpty()) {
            sendErrorResponse(
                    response,
                    404,
                    "No taxonomies found in the repository under '"
                            + StringUtils.defaultIfBlank(taxonomyBase, DEFAULT_TAXONOMY_BASE)
                            + "'");
            return;
        }

        // Build prompt for AI based on type
        String prompt = buildTaxonomyPrompt(type, content, availableTaxonomies, maxSuggestions);

        // Build AI request
        Resource resource = request.getResource();
        String contentPath = resource != null
                ? resource.getPath()
                : StringUtils.defaultIfBlank(
                        request.getRequestPathInfo().getSuffix(),
                        request.getRequestPathInfo().getResourcePath());

        AiRequest aiRequest = AiRequest.builder()
                .content(prompt)
                .contentPath(contentPath)
                .userId(request.getResourceResolver().getUserID())
                .build();

        // Get AI suggestions
        AiResponse aiResponse;
        try {
            aiResponse = aiService.suggestTitle(aiRequest);
        } catch (Exception e) {
            log.error("Error generating AI taxonomy suggestions", e);
            sendErrorResponse(response, 500, "Error generating suggestions: " + e.getMessage());
            return;
        }

        // Parse AI response and match with actual taxonomy items
        List<TaxonomySuggestion> suggestions =
                parseTaxonomySuggestions(aiResponse.getContent(), availableTaxonomies, maxSuggestions);

        // Send response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ObjectNode jsonResponse = objectMapper.createObjectNode();
        jsonResponse.put("success", aiResponse.isSuccess());

        if (aiResponse.isSuccess() && !suggestions.isEmpty()) {
            ArrayNode suggestionsArray = objectMapper.createArrayNode();
            for (TaxonomySuggestion suggestion : suggestions) {
                ObjectNode suggestionNode = objectMapper.createObjectNode();
                suggestionNode.put("path", suggestion.path);
                suggestionNode.put("title", suggestion.title);
                suggestionNode.put("confidence", suggestion.confidence);
                suggestionsArray.add(suggestionNode);
            }
            jsonResponse.set("suggestions", suggestionsArray);
            jsonResponse.put("provider", aiResponse.getProviderId());
        } else {
            jsonResponse.put(
                    "error", aiResponse.isSuccess() ? "No matching taxonomies found" : aiResponse.getErrorMessage());
        }

        response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
    }

    /**
     * Load available taxonomies from the repository.
     */
    private List<TaxonomyOption> loadTaxonomies(ResourceResolver resolver, String taxonomyBase) {
        List<TaxonomyOption> taxonomies = new ArrayList<>();

        try {
            if (StringUtils.isBlank(taxonomyBase)) {
                taxonomyBase = DEFAULT_TAXONOMY_BASE;
            }

            String query = "SELECT * FROM [sling:Taxonomy] WHERE ISDESCENDANTNODE([" + taxonomyBase + "])";
            java.util.Iterator<Resource> results = resolver.findResources(query, Query.JCR_SQL2);

            while (results.hasNext()) {
                Resource res = results.next();
                ValueMap vm = res.getValueMap();
                String title = vm.get("jcr:title", res.getName());
                String description = vm.get("jcr:description", "");
                taxonomies.add(new TaxonomyOption(res.getPath(), title, description));
            }
        } catch (Exception e) {
            log.error("Error loading taxonomies (taxonomyBase={})", taxonomyBase, e);
        }

        return taxonomies;
    }

    /**
     * Build AI prompt based on taxonomy suggestion type.
     */
    private String buildTaxonomyPrompt(
            String type, String content, List<TaxonomyOption> taxonomies, int maxSuggestions) {
        StringBuilder prompt = new StringBuilder();

        switch (type.toLowerCase()) {
            case "content-taxonomy":
                prompt.append("Analyze the following content and suggest up to ")
                        .append(maxSuggestions)
                        .append(" relevant taxonomy tags based on keywords and topics:\n\n");
                break;
            case "semantic-taxonomy":
                prompt.append("Perform semantic analysis on the following content and suggest up to ")
                        .append(maxSuggestions)
                        .append(" taxonomy tags based on concepts and themes:\n\n");
                break;
            case "context-taxonomy":
                prompt.append("Analyze the context and intent of the following content and suggest up to ")
                        .append(maxSuggestions)
                        .append(" contextually appropriate taxonomy tags:\n\n");
                break;
            default:
                prompt.append("Suggest up to ")
                        .append(maxSuggestions)
                        .append(" relevant taxonomy tags for the following content:\n\n");
        }

        prompt.append("Content:\n").append(content).append("\n\n");
        prompt.append("Available taxonomies (comma-separated titles):\n");

        List<String> titles = new ArrayList<>();
        for (TaxonomyOption taxonomy : taxonomies) {
            titles.add(taxonomy.title);
        }
        prompt.append(String.join(", ", titles));
        prompt.append("\n\nRespond with only the taxonomy titles, comma-separated, ordered by relevance.");

        return prompt.toString();
    }

    /**
     * Parse AI response and match with actual taxonomy items.
     */
    private List<TaxonomySuggestion> parseTaxonomySuggestions(
            String aiResponse, List<TaxonomyOption> availableTaxonomies, int maxSuggestions) {

        List<TaxonomySuggestion> suggestions = new ArrayList<>();

        if (StringUtils.isBlank(aiResponse)) {
            return suggestions;
        }

        // Parse comma-separated taxonomy titles from AI response
        String[] suggestedTitles = aiResponse.split(",");

        for (int i = 0; i < suggestedTitles.length && suggestions.size() < maxSuggestions; i++) {
            String title = suggestedTitles[i].trim();

            // Find matching taxonomy
            for (TaxonomyOption taxonomy : availableTaxonomies) {
                if (taxonomy.title.equalsIgnoreCase(title)
                        || taxonomy.title.toLowerCase().contains(title.toLowerCase())) {

                    // Calculate confidence (decreases with position)
                    double confidence = 1.0 - (i * 0.1);
                    suggestions.add(new TaxonomySuggestion(taxonomy.path, taxonomy.title, confidence));
                    break;
                }
            }
        }

        return suggestions;
    }

    /**
     * Get the best available AI service.
     */
    private AiTextService getBestAiService() {
        if (aiTextServices == null || aiTextServices.isEmpty()) {
            return null;
        }

        // First, try to find an enabled external API service
        for (AiTextService service : aiTextServices) {
            if (service.isEnabled() && service.requiresExternalApi()) {
                return service;
            }
        }

        // Fall back to any enabled service
        for (AiTextService service : aiTextServices) {
            if (service.isEnabled()) {
                return service;
            }
        }

        return null;
    }

    /**
     * Send error response.
     */
    private void sendErrorResponse(SlingHttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ObjectNode jsonResponse = objectMapper.createObjectNode();
        jsonResponse.put("success", false);
        jsonResponse.put("error", message);

        response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
    }

    /**
     * Represents a taxonomy option from the repository.
     */
    private static class TaxonomyOption {
        final String path;
        final String title;

        TaxonomyOption(String path, String title, String description) {
            this.path = path;
            this.title = title;
            // description available for future use in enhanced prompts
        }
    }

    /**
     * Represents a taxonomy suggestion with confidence score.
     */
    private static class TaxonomySuggestion {
        final String path;
        final String title;
        final double confidence;

        TaxonomySuggestion(String path, String title, double confidence) {
            this.path = path;
            this.title = title;
            this.confidence = confidence;
        }
    }
}
