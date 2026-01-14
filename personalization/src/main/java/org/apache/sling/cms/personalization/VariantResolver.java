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
package org.apache.sling.cms.personalization;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service for resolving content variants based on personalization segments.
 * <p>
 * This service provides advanced variant resolution capabilities, allowing
 * components to select the best matching content variant based on the
 * current request's matched segments.
 * </p>
 * <p>
 * Variant structure convention:
 * </p>
 * <pre>
 * content-resource/
 *   variants/
 *     mobile/
 *       @segments=["mobile-users"]
 *       @priority=50
 *       ... content properties ...
 *     premium/
 *       @segments=["premium-users"]
 *       @priority=80
 *       ... content properties ...
 *     default/
 *       @isDefault=true
 *       @priority=0
 *       ... content properties ...
 * </pre>
 * <p>
 * Usage in Sling Models:
 * </p>
 * <pre>
 * &#64;Model(adaptables = SlingHttpServletRequest.class)
 * public class HeroComponent {
 *     &#64;OSGiService
 *     private VariantResolver variantResolver;
 *
 *     &#64;SlingObject
 *     private Resource resource;
 *
 *     &#64;SlingObject
 *     private SlingHttpServletRequest request;
 *
 *     public Resource getContent() {
 *         return variantResolver.resolveVariant(resource, request);
 *     }
 * }
 * </pre>
 */
@ProviderType
public interface VariantResolver {

    /**
     * Name of the child resource containing variants.
     */
    String VARIANTS_NODE_NAME = "variants";

    /**
     * Property name for segment associations on variant resources.
     */
    String PROP_SEGMENTS = "segments";

    /**
     * Property name for variant priority.
     */
    String PROP_PRIORITY = "priority";

    /**
     * Property name for default variant flag.
     */
    String PROP_IS_DEFAULT = "isDefault";

    /**
     * Resolves the best matching variant for a content resource.
     * <p>
     * The resolution algorithm:
     * </p>
     * <ol>
     *   <li>Get matched segments from the current request</li>
     *   <li>Sort variants by priority (highest first)</li>
     *   <li>Return first variant that matches any matched segment</li>
     *   <li>If no match, return the default variant (isDefault=true)</li>
     *   <li>If no default, return null</li>
     * </ol>
     *
     * @param contentResource the content resource containing variants
     * @param request the current request for segment evaluation
     * @return the resolved variant resource, or null if no variant matches
     */
    @Nullable
    Resource resolveVariant(@NotNull Resource contentResource, @NotNull SlingHttpServletRequest request);

    /**
     * Resolves a variant for specific segment IDs.
     * <p>
     * This method allows direct variant resolution without evaluating the request,
     * useful for preview modes where authors want to test specific segments.
     * </p>
     *
     * @param contentResource the content resource containing variants
     * @param segmentIds the segment IDs to match against
     * @return the resolved variant resource, or null if no variant matches
     */
    @Nullable
    Resource resolveVariantForSegments(@NotNull Resource contentResource, @NotNull List<String> segmentIds);

    /**
     * Gets all variants for a content resource.
     * <p>
     * Returns variants sorted by priority (highest first).
     * </p>
     *
     * @param contentResource the content resource containing variants
     * @return list of variant resources, sorted by priority
     */
    @NotNull
    List<Resource> getVariants(@NotNull Resource contentResource);

    /**
     * Gets the default variant for a content resource.
     *
     * @param contentResource the content resource containing variants
     * @return the default variant resource, or null if none defined
     */
    @Nullable
    Resource getDefaultVariant(@NotNull Resource contentResource);

    /**
     * Checks if a content resource has personalization variants.
     *
     * @param contentResource the content resource to check
     * @return true if variants exist, false otherwise
     */
    boolean hasVariants(@NotNull Resource contentResource);

    /**
     * Gets the segments associated with a variant.
     *
     * @param variantResource the variant resource
     * @return list of segment IDs, never null (may be empty)
     */
    @NotNull
    List<String> getVariantSegments(@NotNull Resource variantResource);

    /**
     * Gets the priority of a variant.
     *
     * @param variantResource the variant resource
     * @return the priority value (default 0)
     */
    int getVariantPriority(@NotNull Resource variantResource);
}
