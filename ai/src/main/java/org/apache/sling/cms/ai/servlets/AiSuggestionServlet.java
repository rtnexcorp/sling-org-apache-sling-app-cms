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

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
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
 * Servlet for generating AI-powered content suggestions for various field types.
 * <p>
 * Endpoint: POST /bin/cms/ai/suggest
 * </p>
 * <p>
 * Parameters:
 * </p>
 * <ul>
 * <li>type - Type of suggestion: "title", "summary", "metaDescription", "excerpt", "description", "content", "category", "tags", "classification"</li>
 * <li>content - The page content to analyze</li>
 * <li>maxLength - (optional) Maximum length for summaries</li>
 * </ul>
 */
@Component(
        service = Servlet.class,
        property = {
            "sling.servlet.methods=POST",
            "sling.servlet.paths=/bin/cms/ai/suggest",
            "sling.servlet.extensions=json"
        })
public class AiSuggestionServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AiSuggestionServlet.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private volatile List<AiTextService> aiTextServices;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String type = request.getParameter("type");
        String content = request.getParameter("content");
        String maxLengthParam = request.getParameter("maxLength");

        if (type == null || content == null || content.trim().isEmpty()) {
            sendErrorResponse(response, 400, "Missing required parameters: type and content");
            return;
        }

        // Get the best AI service (prioritize external APIs over rules-based)
        AiTextService aiService = getBestAiService();
        if (aiService == null) {
            sendErrorResponse(response, 503, "No AI service available");
            return;
        }

        // Build AI request
        Resource resource = request.getResource();
        AiRequest aiRequest = AiRequest.builder()
                .content(content)
                .contentPath(resource.getPath())
                .userId(request.getResourceResolver().getUserID())
                .build();

        // Generate suggestion based on type
        AiResponse aiResponse;
        try {
            switch (type.toLowerCase()) {
                case "title":
                    aiResponse = aiService.suggestTitle(aiRequest);
                    break;
                case "summary":
                case "excerpt":
                    if (maxLengthParam != null && !maxLengthParam.isEmpty()) {
                        int maxLength = Integer.parseInt(maxLengthParam);
                        aiResponse = aiService.summarize(aiRequest, maxLength);
                    } else {
                        aiResponse = aiService.summarize(aiRequest);
                    }
                    break;
                case "description":
                case "content":
                    aiResponse = aiService.summarize(aiRequest);
                    break;
                case "metadescription":
                    aiResponse = aiService.suggestMetaDescription(aiRequest);
                    break;
                case "category":
                case "tags":
                case "classification":
                    aiResponse = aiService.suggestTitle(aiRequest);
                    break;
                default:
                    sendErrorResponse(
                            response,
                            400,
                            "Invalid type parameter. Must be one of: title, summary, metaDescription, excerpt, description, content, category, tags, classification");
                    return;
            }
        } catch (Exception e) {
            log.error("Error generating AI suggestion", e);
            sendErrorResponse(response, 500, "Error generating suggestion: " + e.getMessage());
            return;
        }

        // Send response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ObjectNode jsonResponse = objectMapper.createObjectNode();
        jsonResponse.put("success", aiResponse.isSuccess());

        if (aiResponse.isSuccess()) {
            jsonResponse.put("suggestion", aiResponse.getContent());
            jsonResponse.put("provider", aiResponse.getProviderId());
        } else {
            jsonResponse.put("error", aiResponse.getErrorMessage());
        }

        response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
    }

    /**
     * Get the best available AI service.
     * Prioritizes external API services over rules-based fallback.
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

        // Fall back to any enabled service (including rules-based)
        for (AiTextService service : aiTextServices) {
            if (service.isEnabled()) {
                return service;
            }
        }

        return null;
    }

    private void sendErrorResponse(SlingHttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ObjectNode errorResponse = objectMapper.createObjectNode();
        errorResponse.put("success", false);
        errorResponse.put("error", message);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
