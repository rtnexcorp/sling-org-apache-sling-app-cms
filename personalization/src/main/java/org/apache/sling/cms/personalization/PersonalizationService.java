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
import org.osgi.annotation.versioning.ProviderType;

/**
 * Main personalization service for segment resolution and management.
 * <p>
 * This service is responsible for:
 * </p>
 * <ul>
 *   <li>Resolving which segments match the current request</li>
 *   <li>Managing segment configurations from Context-Aware Configuration</li>
 *   <li>Coordinating with registered {@link SegmentEvaluator} implementations</li>
 * </ul>
 * <p>
 * Example usage in a Sling Model:
 * </p>
 */
@ProviderType
public interface PersonalizationService {

    /**
     * Resolves all matching segments for the current request.
     * <p>
     * This method evaluates all segment configurations accessible from the context
     * resource (following Context-Aware Configuration resolution rules) and returns
     * those that match the current request.
     * </p>
     * <p>
     * The returned list is sorted by segment priority in descending order (highest
     * priority first). This ordering is important for variant resolution, where the
     * first matching segment determines which variant is rendered.
     * </p>
     * <p>
     * The method uses request-scoped caching to avoid redundant evaluations when
     * called multiple times during the same request.
     * </p>
     * <p>
     * Configuration resolution follows Apache Sling Context-Aware Configuration patterns:
     * </p>
     * <ol>
     *   <li>Site-specific segments: {@code /conf/{site}/personalization/segments/}</li>
     *   <li>Global segments: {@code /conf/global/personalization/segments/}</li>
     * </ol>
     *
     * @param request the current HTTP request to evaluate
     * @param contextResource the resource for Context-Aware Configuration resolution
     *                        (typically the current page or component resource)
     * @return list of matching segments sorted by priority (highest first), never null
     */
    @NotNull
    List<Segment> resolveSegments(@NotNull SlingHttpServletRequest request, @NotNull Resource contextResource);

    /**
     * Returns all available segments for a site.
     * <p>
     * This method retrieves all segment configurations accessible from the site
     * resource, including both site-specific and global segments. Unlike
     * {@link #resolveSegments(SlingHttpServletRequest, Resource)}, this method does
     * not evaluate which segments match the request.
     * </p>
     * <p>
     * This is useful for:
     * </p>
     * <ul>
     *   <li>Authoring UI (displaying available segments in dropdowns)</li>
     *   <li>Analytics dashboards (showing all configured segments)</li>
     *   <li>Preview mode (allowing authors to test specific segments)</li>
     * </ul>
     * <p>
     * The returned list includes segments from:
     * </p>
     * <ol>
     *   <li>Site-specific configurations: {@code /conf/{site}/personalization/segments/}</li>
     *   <li>Global configurations: {@code /conf/global/personalization/segments/}</li>
     * </ol>
     *
     * @param siteResource the site root resource or any resource within the site
     * @return list of all configured segments, never null (may be empty)
     */
    @NotNull
    List<Segment> getAvailableSegments(@NotNull Resource siteResource);

    /**
     * Checks if personalization is enabled globally.
     * <p>
     * This provides a master switch to disable personalization without removing
     * segment configurations. When disabled, {@link #resolveSegments} will return
     * an empty list.
     * </p>
     * <p>
     * This can be controlled via OSGi configuration for emergency rollback or
     * maintenance scenarios.
     * </p>
     *
     * @return true if personalization is enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Evaluates which segments match the current request and returns their IDs.
     * <p>
     * This is a convenience method that returns just the segment IDs rather than
     * full Segment objects. Useful for REST APIs and lightweight checks.
     * </p>
     *
     * @param request the current HTTP request to evaluate
     * @return list of matching segment IDs, never null (may be empty)
     */
    @NotNull
    List<String> evaluateSegments(@NotNull SlingHttpServletRequest request);

    /**
     * Selects the best matching variant for a content resource based on request segments.
     * <p>
     * This method evaluates the request to determine which segments match, then
     * selects the highest-priority variant that matches one of those segments.
     * If no variant matches, returns the default variant or null if no default exists.
     * </p>
     *
     * @param contentResource the content resource to get variants for
     * @param request the current HTTP request
     * @return the selected variant resource, or null if no variant matches
     */
    Resource selectVariant(@NotNull Resource contentResource, @NotNull SlingHttpServletRequest request);

    /**
     * Gets all available variants for a content resource.
     * <p>
     * Returns all configured variants regardless of whether they match the current request.
     * Useful for authoring UI and preview modes.
     * </p>
     *
     * @param contentResource the content resource to get variants for
     * @return list of variant resources, never null (may be empty)
     */
    @NotNull
    List<Resource> getVariants(@NotNull Resource contentResource);
}
