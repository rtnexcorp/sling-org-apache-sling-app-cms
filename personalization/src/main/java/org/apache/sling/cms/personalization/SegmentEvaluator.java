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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Pluggable segment evaluator interface for determining if a request matches a segment.
 * <p>
 * Implementations of this interface are registered as OSGi services and are automatically
 * discovered by the {@link PersonalizationService}. Each evaluator is identified by a
 * unique type string.
 * </p>
 * <p>
 * Example implementation:
 * </p>
 * <p>
 * Built-in evaluators:
 * </p>
 * <ul>
 *   <li><b>path</b> - Matches request path patterns</li>
 *   <li><b>cookie</b> - Evaluates cookie presence/values</li>
 *   <li><b>userGroup</b> - Checks logged-in user groups</li>
 *   <li><b>device</b> - Detects mobile/tablet/desktop via User-Agent</li>
 * </ul>
 */
@ConsumerType
public interface SegmentEvaluator {

    /**
     * Returns the evaluator type identifier.
     * <p>
     * This type is used to match the evaluator with segment configurations.
     * The type should be unique across all registered evaluators.
     * </p>
     * <p>
     * Example types:
     * </p>
     * <ul>
     *   <li>"device" - Device type detection</li>
     *   <li>"cookie" - Cookie-based evaluation</li>
     *   <li>"userGroup" - User group membership</li>
     *   <li>"path" - Request path matching</li>
     *   <li>"geo" - Geographic location (custom)</li>
     * </ul>
     *
     * @return the evaluator type identifier
     */
    @NotNull
    String getType();

    /**
     * Evaluates if the request matches the segment rules.
     * <p>
     * This method is called by the PersonalizationService for each segment
     * configuration that uses this evaluator type. The implementation should
     * be lightweight and fast, as it may be called multiple times per request.
     * </p>
     * <p>
     * The rules parameter contains the segment-specific configuration from
     * the {@code /conf/{site}/personalization/segments/{segmentId}/rules/}
     * node as a ValueMap.
     * </p>
     * <p>
     * Example rules for a cookie evaluator:
     * </p>
     * <p>
     * Performance considerations:
     * </p>
     * <ul>
     *   <li>Keep evaluation logic simple and fast (target: &lt;5ms)</li>
     *   <li>Avoid expensive operations (database queries, external API calls)</li>
     *   <li>Cache results in request attributes if called multiple times</li>
     *   <li>Fail fast and return false on errors</li>
     * </ul>
     *
     * @param request the current HTTP request
     * @param rules the segment-specific evaluation rules from configuration
     * @return true if the request matches the segment, false otherwise
     */
    boolean evaluate(@NotNull SlingHttpServletRequest request, @NotNull ValueMap rules);
}
