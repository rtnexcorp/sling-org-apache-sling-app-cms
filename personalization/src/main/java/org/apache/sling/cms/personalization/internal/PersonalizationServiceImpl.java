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
package org.apache.sling.cms.personalization.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.cms.personalization.PersonalizationService;
import org.apache.sling.cms.personalization.Segment;
import org.apache.sling.cms.personalization.SegmentEvaluator;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of PersonalizationService using Context-Aware Configuration.
 */
@Component(service = PersonalizationService.class)
@Designate(ocd = PersonalizationServiceImpl.Config.class)
public class PersonalizationServiceImpl implements PersonalizationService {

    private static final Logger log = LoggerFactory.getLogger(PersonalizationServiceImpl.class);

    private static final String REQUEST_ATTRIBUTE_SEGMENTS = "sling.cms.personalization.segments";
    private static final String CONFIG_BUCKET = "personalization";
    private static final String CONFIG_SEGMENTS_PATH = "segments";

    @ObjectClassDefinition(name = "Apache Sling CMS Personalization Service Configuration")
    public @interface Config {
        @AttributeDefinition(name = "Enabled", description = "Enable personalization globally")
        boolean enabled() default true;

        @AttributeDefinition(name = "Debug Mode", description = "Enable debug logging for segment evaluation")
        boolean debugMode() default false;
    }

    @Reference
    private ConfigurationResourceResolver configurationResourceResolver;

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private volatile List<SegmentEvaluator> evaluators;

    private boolean enabled;
    private boolean debugMode;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.enabled = config.enabled();
        this.debugMode = config.debugMode();
        log.info("Personalization service {} (debug mode: {})", enabled ? "enabled" : "disabled", debugMode);
    }

    @Override
    @NotNull
    public List<Segment> resolveSegments(@NotNull SlingHttpServletRequest request, @NotNull Resource contextResource) {
        if (!enabled) {
            if (debugMode) {
                log.debug("Personalization is disabled globally");
            }
            return Collections.emptyList();
        }

        // Check request-scoped cache
        @SuppressWarnings("unchecked")
        List<Segment> cachedSegments = (List<Segment>) request.getAttribute(REQUEST_ATTRIBUTE_SEGMENTS);
        if (cachedSegments != null) {
            if (debugMode) {
                log.debug("Returning cached segments: {}", cachedSegments);
            }
            return cachedSegments;
        }

        List<Segment> matchedSegments = new ArrayList<>();

        // Get segment configuration resources
        Resource segmentsConfig = getSegmentsConfig(contextResource);
        if (segmentsConfig == null) {
            if (debugMode) {
                log.debug("No segment configuration found for resource: {}", contextResource.getPath());
            }
            return Collections.emptyList();
        }

        // Evaluate each segment
        for (Resource segmentResource : segmentsConfig.getChildren()) {
            try {
                Segment segment = new SegmentImpl(segmentResource);

                if (evaluateSegment(request, segmentResource, segment)) {
                    matchedSegments.add(segment);
                    if (debugMode) {
                        log.debug("Segment matched: {}", segment);
                    }
                }
            } catch (Exception e) {
                log.error("Error evaluating segment: {}", segmentResource.getPath(), e);
            }
        }

        // Sort by priority (descending)
        matchedSegments.sort(Comparator.comparingInt(Segment::getPriority).reversed());

        // Cache in request scope
        request.setAttribute(REQUEST_ATTRIBUTE_SEGMENTS, matchedSegments);

        if (debugMode) {
            log.debug("Resolved {} segments for request: {}", matchedSegments.size(), matchedSegments);
        }

        return matchedSegments;
    }

    @Override
    @NotNull
    public List<Segment> getAvailableSegments(@NotNull Resource siteResource) {
        Resource segmentsConfig = getSegmentsConfig(siteResource);
        if (segmentsConfig == null) {
            return Collections.emptyList();
        }

        List<Segment> segments = new ArrayList<>();
        for (Resource segmentResource : segmentsConfig.getChildren()) {
            try {
                segments.add(new SegmentImpl(segmentResource));
            } catch (Exception e) {
                log.error("Error loading segment: {}", segmentResource.getPath(), e);
            }
        }

        return segments;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the segments configuration resource using CA Config resolution.
     */
    private Resource getSegmentsConfig(Resource contextResource) {
        return configurationResourceResolver.getResource(contextResource, CONFIG_BUCKET, CONFIG_SEGMENTS_PATH);
    }

    /**
     * Evaluates a single segment against the request.
     */
    private boolean evaluateSegment(SlingHttpServletRequest request, Resource segmentResource, Segment segment) {
        String evaluatorType = segment.getEvaluatorType();
        if (evaluatorType == null || evaluatorType.isEmpty()) {
            log.warn("Segment {} has no evaluator type configured", segment.getId());
            return false;
        }

        // Find matching evaluator
        SegmentEvaluator evaluator = findEvaluator(evaluatorType);
        if (evaluator == null) {
            log.warn("No evaluator found for type: {}", evaluatorType);
            return false;
        }

        // Get rules resource
        Resource rulesResource = segmentResource.getChild("rules");
        if (rulesResource == null) {
            log.warn("Segment {} has no rules configured", segment.getId());
            return false;
        }

        // Evaluate
        ValueMap rules = rulesResource.getValueMap();
        try {
            long startTime = System.currentTimeMillis();
            boolean result = evaluator.evaluate(request, rules);
            long duration = System.currentTimeMillis() - startTime;

            if (debugMode) {
                log.debug("Segment {} evaluation result: {} (took {}ms)", segment.getId(), result, duration);
            }

            if (duration > 5) {
                log.warn(
                        "Segment evaluation took {}ms for segment {} (evaluator: {})",
                        duration,
                        segment.getId(),
                        evaluatorType);
            }

            return result;
        } catch (Exception e) {
            log.error("Error evaluating segment {}", segment.getId(), e);
            return false;
        }
    }

    /**
     * Finds an evaluator by type.
     */
    private SegmentEvaluator findEvaluator(String type) {
        if (evaluators == null) {
            return null;
        }
        return evaluators.stream()
                .filter(e -> type.equals(e.getType()))
                .findFirst()
                .orElse(null);
    }

    @Override
    @NotNull
    public List<String> evaluateSegments(@NotNull SlingHttpServletRequest request) {
        // Get the context resource from request
        Resource contextResource = request.getResource();
        List<Segment> segments = resolveSegments(request, contextResource);

        List<String> segmentIds = new ArrayList<>();
        for (Segment segment : segments) {
            segmentIds.add(segment.getId());
        }
        return segmentIds;
    }

    @Override
    public Resource selectVariant(@NotNull Resource contentResource, @NotNull SlingHttpServletRequest request) {
        List<String> matchedSegments = evaluateSegments(request);
        Resource variantsResource = contentResource.getChild("variants");

        if (variantsResource == null) {
            return null;
        }

        // Find variants and sort by priority
        List<Resource> variants = new ArrayList<>();
        variantsResource.listChildren().forEachRemaining(variants::add);

        // Sort by priority (highest first)
        variants.sort((v1, v2) -> {
            int p1 = v1.getValueMap().get("priority", 0);
            int p2 = v2.getValueMap().get("priority", 0);
            return Integer.compare(p2, p1);
        });

        // Find first variant that matches a segment
        for (Resource variant : variants) {
            String[] variantSegments = variant.getValueMap().get("segments", String[].class);
            if (variantSegments != null) {
                for (String segmentId : variantSegments) {
                    if (matchedSegments.contains(segmentId)) {
                        return variant;
                    }
                }
            }
        }

        // Return default variant if no match
        for (Resource variant : variants) {
            if (variant.getValueMap().get("isDefault", false)) {
                return variant;
            }
        }

        return null;
    }

    @Override
    @NotNull
    public List<Resource> getVariants(@NotNull Resource contentResource) {
        Resource variantsResource = contentResource.getChild("variants");
        if (variantsResource == null) {
            return Collections.emptyList();
        }

        List<Resource> variants = new ArrayList<>();
        variantsResource.listChildren().forEachRemaining(variants::add);
        return variants;
    }
}
