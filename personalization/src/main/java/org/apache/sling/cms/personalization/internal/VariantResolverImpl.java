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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.personalization.PersonalizationService;
import org.apache.sling.cms.personalization.VariantResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link VariantResolver} for resolving personalized content variants.
 */
@Component(service = VariantResolver.class)
public class VariantResolverImpl implements VariantResolver {

    private static final Logger log = LoggerFactory.getLogger(VariantResolverImpl.class);

    @Reference
    private PersonalizationService personalizationService;

    @Override
    @Nullable
    public Resource resolveVariant(@NotNull Resource contentResource, @NotNull SlingHttpServletRequest request) {
        if (!hasVariants(contentResource)) {
            log.debug("No variants found for resource: {}", contentResource.getPath());
            return null;
        }

        // Get matched segments from request
        List<String> matchedSegments = personalizationService.evaluateSegments(request);
        log.debug("Matched segments for request: {}", matchedSegments);

        return resolveVariantForSegments(contentResource, matchedSegments);
    }

    @Override
    @Nullable
    public Resource resolveVariantForSegments(@NotNull Resource contentResource, @NotNull List<String> segmentIds) {
        List<Resource> variants = getVariants(contentResource);

        if (variants.isEmpty()) {
            return null;
        }

        // Find first variant that matches any segment (variants already sorted by priority)
        for (Resource variant : variants) {
            List<String> variantSegments = getVariantSegments(variant);

            for (String segmentId : segmentIds) {
                if (variantSegments.contains(segmentId)) {
                    log.debug("Variant '{}' matched segment '{}'", variant.getName(), segmentId);
                    return variant;
                }
            }
        }

        // No segment match, return default variant
        Resource defaultVariant = getDefaultVariant(contentResource);
        if (defaultVariant != null) {
            log.debug("Returning default variant: {}", defaultVariant.getName());
        }
        return defaultVariant;
    }

    @Override
    @NotNull
    public List<Resource> getVariants(@NotNull Resource contentResource) {
        Resource variantsNode = contentResource.getChild(VARIANTS_NODE_NAME);
        if (variantsNode == null) {
            return Collections.emptyList();
        }

        List<Resource> variants = new ArrayList<>();
        variantsNode.listChildren().forEachRemaining(variants::add);

        // Sort by priority (highest first)
        variants.sort(Comparator.comparingInt(this::getVariantPriority).reversed());

        return variants;
    }

    @Override
    @Nullable
    public Resource getDefaultVariant(@NotNull Resource contentResource) {
        Resource variantsNode = contentResource.getChild(VARIANTS_NODE_NAME);
        if (variantsNode == null) {
            return null;
        }

        for (Resource variant : variantsNode.getChildren()) {
            if (variant.getValueMap().get(PROP_IS_DEFAULT, false)) {
                return variant;
            }
        }

        return null;
    }

    @Override
    public boolean hasVariants(@NotNull Resource contentResource) {
        Resource variantsNode = contentResource.getChild(VARIANTS_NODE_NAME);
        return variantsNode != null && variantsNode.hasChildren();
    }

    @Override
    @NotNull
    public List<String> getVariantSegments(@NotNull Resource variantResource) {
        ValueMap props = variantResource.getValueMap();
        String[] segments = props.get(PROP_SEGMENTS, String[].class);

        if (segments == null || segments.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.asList(segments);
    }

    @Override
    public int getVariantPriority(@NotNull Resource variantResource) {
        return variantResource.getValueMap().get(PROP_PRIORITY, 0);
    }
}
