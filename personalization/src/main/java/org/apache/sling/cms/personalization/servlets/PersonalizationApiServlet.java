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
package org.apache.sling.cms.personalization.servlets;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.cms.personalization.PersonalizationService;
import org.apache.sling.cms.personalization.SegmentEvaluator;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST API servlet for personalization operations.
 * <p>
 * Provides JSON endpoints for querying segments, variants, and evaluating personalization.
 * </p>
 * <p>
 * Endpoints (using selectors on /bin/personalization path):
 * </p>
 * <ul>
 *   <li>GET /bin/personalization.segments.json - List all segments</li>
 *   <li>GET /bin/personalization.segment.{segmentId}.json - Get specific segment</li>
 *   <li>GET /bin/personalization.evaluate.json?path=/content/page - Evaluate personalization</li>
 *   <li>GET /bin/personalization.evaluators.json - List available evaluator types</li>
 * </ul>
 */
@Component(
        service = Servlet.class,
        property = {
            "sling.servlet.paths=/bin/personalization",
            "sling.servlet.methods=GET",
            "sling.servlet.extensions=json"
        })
public class PersonalizationApiServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(PersonalizationApiServlet.class);

    private static final String SEGMENTS_BASE_PATH = "/etc/personalization/segments";

    @Reference
    private PersonalizationService personalizationService;

    @Reference
    private List<SegmentEvaluator> segmentEvaluators;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            // Get selectors to determine which operation to perform
            String[] selectors = request.getRequestPathInfo().getSelectors();

            if (selectors.length == 0 || "segments".equals(selectors[0])) {
                handleSegmentsList(request, response);
            } else if ("segment".equals(selectors[0]) && selectors.length > 1) {
                handleSegmentDetail(request, response, selectors[1]);
            } else if ("evaluate".equals(selectors[0])) {
                handleEvaluate(request, response);
            } else if ("evaluators".equals(selectors[0])) {
                handleEvaluatorsList(request, response);
            } else {
                sendError(response, 404, "Unknown operation. Use segments, segment.{id}, evaluate, or evaluators");
            }
        } catch (Exception e) {
            log.error("Error processing personalization API request", e);
            sendError(response, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * GET /bin/personalization.segments.json
     * Query parameters:
     *   - type: Filter by evaluator type
     *   - active: Filter by active status (true/false)
     */
    private void handleSegmentsList(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {
        ResourceResolver resolver = request.getResourceResolver();
        String typeFilter = request.getParameter("type");
        String activeFilter = request.getParameter("active");

        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode segments = result.putArray("segments");

        Resource segmentsRoot = resolver.getResource(SEGMENTS_BASE_PATH);
        if (segmentsRoot != null) {
            Iterator<Resource> segmentResources = segmentsRoot.listChildren();
            while (segmentResources.hasNext()) {
                Resource segmentResource = segmentResources.next();
                ValueMap props = segmentResource.getValueMap();

                // Apply filters
                if (typeFilter != null && !typeFilter.equals(props.get("evaluatorType", String.class))) {
                    continue;
                }
                if (activeFilter != null && !activeFilter.equals(String.valueOf(props.get("active", true)))) {
                    continue;
                }

                segments.add(resourceToSegmentJson(segmentResource));
            }
        }

        result.put("total", segments.size());
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    /**
     * GET /bin/personalization.segment.{segmentId}.json
     */
    private void handleSegmentDetail(
            SlingHttpServletRequest request, SlingHttpServletResponse response, String segmentId) throws IOException {
        ResourceResolver resolver = request.getResourceResolver();

        Resource segmentResource = resolver.getResource(SEGMENTS_BASE_PATH + "/" + segmentId);
        if (segmentResource == null) {
            sendError(response, 404, "Segment not found: " + segmentId);
            return;
        }

        ObjectNode result = resourceToSegmentJson(segmentResource);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    /**
     * GET /bin/personalization.evaluate.json?path=/content/page
     * Query parameters:
     *   - path: Content path to evaluate (required)
     */
    private void handleEvaluate(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String contentPath = request.getParameter("path");
        if (contentPath == null) {
            sendError(response, 400, "Missing required parameter: path");
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();
        Resource contentResource = resolver.getResource(contentPath);
        if (contentResource == null) {
            sendError(response, 404, "Content not found: " + contentPath);
            return;
        }

        // Evaluate segments for current request
        List<String> matchedSegments = personalizationService.evaluateSegments(request);

        // Get selected variant
        Resource selectedVariant = personalizationService.selectVariant(contentResource, request);

        // Build response
        ObjectNode result = objectMapper.createObjectNode();
        result.put("path", contentPath);

        ArrayNode matchedArray = result.putArray("matchedSegments");
        for (String segmentId : matchedSegments) {
            matchedArray.add(segmentId);
        }

        if (selectedVariant != null) {
            result.set("selectedVariant", resourceToVariantJson(selectedVariant));
        }

        // Get all available variants
        List<Resource> variants = personalizationService.getVariants(contentResource);
        ArrayNode variantsArray = result.putArray("availableVariants");
        for (Resource variant : variants) {
            variantsArray.add(resourceToVariantJson(variant));
        }

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    /**
     * GET /bin/personalization.evaluators.json
     */
    private void handleEvaluatorsList(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode evaluators = result.putArray("evaluators");

        for (SegmentEvaluator evaluator : segmentEvaluators) {
            ObjectNode evalNode = objectMapper.createObjectNode();
            evalNode.put("type", evaluator.getType());
            evaluators.add(evalNode);
        }

        result.put("total", evaluators.size());
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private ObjectNode resourceToSegmentJson(Resource segmentResource) {
        ValueMap props = segmentResource.getValueMap();
        ObjectNode segment = objectMapper.createObjectNode();

        segment.put("id", segmentResource.getName());
        segment.put("name", props.get("name", segmentResource.getName()));
        segment.put("description", props.get("description", ""));
        segment.put("type", props.get("evaluatorType", ""));
        segment.put("priority", props.get("priority", 0));
        segment.put("active", props.get("active", true));
        segment.put("path", segmentResource.getPath());

        // Add rules
        Resource rulesResource = segmentResource.getChild("rules");
        if (rulesResource != null) {
            ObjectNode rules = objectMapper.createObjectNode();
            rulesResource.getValueMap().forEach((key, value) -> {
                if (!key.startsWith("jcr:") && !key.startsWith("sling:")) {
                    if (value instanceof String[]) {
                        ArrayNode arrayNode = rules.putArray(key);
                        for (String val : (String[]) value) {
                            arrayNode.add(val);
                        }
                    } else {
                        rules.put(key, String.valueOf(value));
                    }
                }
            });
            segment.set("rules", rules);
        }

        return segment;
    }

    private ObjectNode resourceToVariantJson(Resource variantResource) {
        ValueMap props = variantResource.getValueMap();
        ObjectNode variant = objectMapper.createObjectNode();

        variant.put("id", variantResource.getName());
        variant.put("name", props.get("name", variantResource.getName()));
        variant.put("contentPath", variantResource.getPath());
        variant.put("isDefault", props.get("isDefault", false));
        variant.put("priority", props.get("priority", 0));

        String[] segments = props.get("segments", String[].class);
        if (segments != null) {
            ArrayNode segmentsArray = variant.putArray("segments");
            for (String segment : segments) {
                segmentsArray.add(segment);
            }
        }

        return variant;
    }

    private void sendError(SlingHttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", message);
        error.put("status", status);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
