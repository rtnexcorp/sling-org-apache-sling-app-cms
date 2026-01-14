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
package org.apache.sling.cms.personalization.models;

import javax.annotation.PostConstruct;

import java.util.Collections;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.personalization.PersonalizationService;
import org.apache.sling.cms.personalization.Segment;
import org.apache.sling.cms.personalization.VariantResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sling Model for personalized component rendering.
 * <p>
 * This model provides easy access to personalization functionality in HTL templates.
 * It automatically resolves the best matching variant based on the current request
 * and provides access to matched segments for debugging and analytics.
 * </p>
 * <p>
 * Usage in HTL:
 * </p>
 * <pre>
 * &lt;sly data-sly-use.personalized="org.apache.sling.cms.personalization.models.PersonalizedComponent"&gt;
 *
 *   &lt;!-- Use resolved variant content --&gt;
 *   &lt;sly data-sly-test="${personalized.hasVariants}"&gt;
 *     &lt;sly data-sly-resource="${personalized.resolvedVariant @ resourceType='myapp/components/hero'}" /&gt;
 *   &lt;/sly&gt;
 *
 *   &lt;!-- Fallback to default content --&gt;
 *   &lt;sly data-sly-test="${!personalized.hasVariants}"&gt;
 *     &lt;sly data-sly-resource="${resource @ resourceType='myapp/components/hero'}" /&gt;
 *   &lt;/sly&gt;
 *
 *   &lt;!-- Debug info (author mode) --&gt;
 *   &lt;sly data-sly-test="${wcmmode.edit}"&gt;
 *     &lt;div class="personalization-debug"&gt;
 *       Matched segments: ${personalized.matchedSegmentIds}
 *       Selected variant: ${personalized.resolvedVariantName}
 *     &lt;/div&gt;
 *   &lt;/sly&gt;
 * &lt;/sly&gt;
 * </pre>
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class PersonalizedComponent {

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @OSGiService
    private PersonalizationService personalizationService;

    @OSGiService
    private VariantResolver variantResolver;

    private Resource resolvedVariant;
    private List<Segment> matchedSegments;
    private boolean initialized = false;

    @PostConstruct
    protected void init() {
        if (personalizationService != null && personalizationService.isEnabled()) {
            matchedSegments = personalizationService.resolveSegments(request, resource);
            if (variantResolver != null) {
                resolvedVariant = variantResolver.resolveVariant(resource, request);
            }
        } else {
            matchedSegments = Collections.emptyList();
        }
        initialized = true;
    }

    /**
     * Returns the resolved variant resource based on matched segments.
     * <p>
     * This is the main method for accessing personalized content. The variant
     * is resolved by evaluating the current request against configured segments
     * and selecting the highest-priority matching variant.
     * </p>
     *
     * @return the resolved variant resource, or null if no variant matches
     */
    @Nullable
    public Resource getResolvedVariant() {
        ensureInitialized();
        return resolvedVariant;
    }

    /**
     * Returns the content resource to use for rendering.
     * <p>
     * If a variant is resolved, returns the variant. Otherwise returns the
     * default variant or null. Use this method when you want automatic
     * fallback behavior.
     * </p>
     *
     * @return the content resource to render, or null
     */
    @Nullable
    public Resource getContent() {
        ensureInitialized();
        if (resolvedVariant != null) {
            return resolvedVariant;
        }
        return variantResolver != null ? variantResolver.getDefaultVariant(resource) : null;
    }

    /**
     * Returns the name of the resolved variant for debugging.
     *
     * @return the variant name, or "none" if no variant resolved
     */
    @NotNull
    public String getResolvedVariantName() {
        ensureInitialized();
        return resolvedVariant != null ? resolvedVariant.getName() : "none";
    }

    /**
     * Returns the list of matched segments for the current request.
     * <p>
     * Segments are sorted by priority (highest first).
     * </p>
     *
     * @return list of matched segments, never null
     */
    @NotNull
    public List<Segment> getMatchedSegments() {
        ensureInitialized();
        return matchedSegments != null ? matchedSegments : Collections.emptyList();
    }

    /**
     * Returns matched segment IDs as a comma-separated string.
     * <p>
     * Useful for debugging output in HTL templates.
     * </p>
     *
     * @return comma-separated segment IDs, or "none"
     */
    @NotNull
    public String getMatchedSegmentIds() {
        ensureInitialized();
        if (matchedSegments == null || matchedSegments.isEmpty()) {
            return "none";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matchedSegments.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(matchedSegments.get(i).getId());
        }
        return sb.toString();
    }

    /**
     * Checks if the component has personalization variants configured.
     *
     * @return true if variants exist
     */
    public boolean isHasVariants() {
        return variantResolver != null && variantResolver.hasVariants(resource);
    }

    /**
     * Checks if personalization is enabled globally.
     *
     * @return true if personalization is enabled
     */
    public boolean isPersonalizationEnabled() {
        return personalizationService != null && personalizationService.isEnabled();
    }

    /**
     * Returns all available variants for authoring preview.
     *
     * @return list of all variant resources
     */
    @NotNull
    public List<Resource> getAllVariants() {
        if (variantResolver == null) {
            return Collections.emptyList();
        }
        return variantResolver.getVariants(resource);
    }

    /**
     * Returns the default variant for the component.
     *
     * @return the default variant resource, or null
     */
    @Nullable
    public Resource getDefaultVariant() {
        if (variantResolver == null) {
            return null;
        }
        return variantResolver.getDefaultVariant(resource);
    }

    /**
     * Returns the current resource being personalized.
     *
     * @return the current resource
     */
    @NotNull
    public Resource getResource() {
        return resource;
    }

    private void ensureInitialized() {
        if (!initialized) {
            init();
        }
    }
}
