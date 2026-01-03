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
import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.cms.ai.AiImageService;
import org.apache.sling.cms.ai.AiResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for generating AI-powered image suggestions (alt-text, captions, descriptions).
 * <p>
 * Endpoint: POST /bin/cms/ai/suggest-image
 * </p>
 * <p>
 * Parameters:
 * </p>
 * <ul>
 * <li>type - Type of suggestion: "altText", "caption", "description"</li>
 * <li>imagePath - (optional) JCR path to the image asset</li>
 * <li>image - (optional) Uploaded image file</li>
 * <li>context - (optional) Context about where the image is used</li>
 * </ul>
 */
@Component(
        service = Servlet.class,
        property = {
            "sling.servlet.methods=POST",
            "sling.servlet.paths=/bin/cms/ai/suggest-image",
            "sling.servlet.extensions=json"
        })
public class AiImageSuggestionServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AiImageSuggestionServlet.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private volatile List<AiImageService> aiImageServices;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String type = request.getParameter("type");
        String imagePath = request.getParameter("imagePath");
        String context = request.getParameter("context");
        RequestParameter imageParam = request.getRequestParameter("image");

        if (type == null) {
            sendErrorResponse(response, 400, "Missing required parameter: type");
            return;
        }

        if (imagePath == null && imageParam == null) {
            sendErrorResponse(response, 400, "Either imagePath or image file must be provided");
            return;
        }

        // Get the best AI image service
        AiImageService aiService = getBestAiImageService();
        if (aiService == null) {
            sendErrorResponse(response, 503, "No AI image service available");
            return;
        }

        // Generate suggestion based on type
        AiResponse aiResponse;
        try {
            switch (type.toLowerCase()) {
                case "alttext":
                case "alt":
                    if (imagePath != null) {
                        aiResponse = aiService.generateAltText(imagePath, context);
                    } else {
                        InputStream imageStream = imageParam.getInputStream();
                        String mimeType = imageParam.getContentType();
                        aiResponse = aiService.generateAltText(imageStream, mimeType, context);
                    }
                    break;

                case "caption":
                    if (imagePath != null) {
                        aiResponse = aiService.generateCaption(imagePath, context);
                    } else {
                        InputStream imageStream = imageParam.getInputStream();
                        String mimeType = imageParam.getContentType();
                        aiResponse = aiService.generateCaption(imageStream, mimeType, context);
                    }
                    break;

                case "description":
                case "describe":
                    if (imagePath != null) {
                        aiResponse = aiService.describeImage(imagePath);
                    } else {
                        InputStream imageStream = imageParam.getInputStream();
                        String mimeType = imageParam.getContentType();
                        aiResponse = aiService.describeImage(imageStream, mimeType);
                    }
                    break;

                default:
                    sendErrorResponse(
                            response, 400, "Invalid type parameter. Must be one of: altText, caption, description");
                    return;
            }
        } catch (Exception e) {
            log.error("Error generating AI image suggestion", e);
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
            if (aiResponse.getStatus() == AiResponse.Status.SKIPPED) {
                jsonResponse.put("skipped", true);
            }
        }

        response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
    }

    /**
     * Get the best available AI image service.
     * Prioritizes external API services over rules-based fallback.
     */
    private AiImageService getBestAiImageService() {
        if (aiImageServices == null || aiImageServices.isEmpty()) {
            return null;
        }

        // First, try to find an enabled external API service
        for (AiImageService service : aiImageServices) {
            if (service.isEnabled() && service.requiresExternalApi()) {
                return service;
            }
        }

        // Fall back to any enabled service (including rules-based)
        for (AiImageService service : aiImageServices) {
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
