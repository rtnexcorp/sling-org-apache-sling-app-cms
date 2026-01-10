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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a user segment for personalization.
 * <p>
 * Segments are used to group users based on specific criteria such as device type,
 * location, behavior, or any other characteristic that can be evaluated from the request.
 * </p>
 * <p>
 * Segments are typically defined in Context-Aware Configuration under
 * {@code /conf/{site}/personalization/segments/}.
 * </p>
 */
@ProviderType
public interface Segment {

    /**
     * Returns the unique segment identifier.
     * <p>
     * This ID is used to match segments with component variants and is typically
     * derived from the segment's resource name in the repository.
     * </p>
     *
     * @return the segment identifier (e.g., "mobile_user", "vip_customer")
     */
    @NotNull
    String getId();

    /**
     * Returns the human-readable segment name.
     * <p>
     * This is displayed in the authoring UI and analytics dashboards.
     * </p>
     *
     * @return the segment title
     */
    @NotNull
    String getTitle();

    /**
     * Returns the segment description.
     * <p>
     * Provides additional context about what this segment represents and
     * when users are matched to it.
     * </p>
     *
     * @return the segment description, or null if not defined
     */
    @Nullable
    String getDescription();

    /**
     * Returns the segment priority for variant resolution.
     * <p>
     * When multiple segments match for a request, the segment with the highest
     * priority determines which variant is rendered. Higher values indicate
     * higher priority.
     * </p>
     * <p>
     * Default priority is 0. Common priority ranges:
     * </p>
     * <ul>
     *   <li>0-9: General segments (device, browser)</li>
     *   <li>10-19: User segments (logged in, user groups)</li>
     *   <li>20-29: Behavioral segments (returning visitor)</li>
     *   <li>30+: High-priority overrides (VIP, admin)</li>
     * </ul>
     *
     * @return the segment priority (higher = more specific)
     */
    int getPriority();

    /**
     * Returns the type of evaluator used for this segment.
     * <p>
     * This corresponds to the {@code evaluator} property in the segment
     * configuration and is used to find the appropriate {@link SegmentEvaluator}.
     * </p>
     *
     * @return the evaluator type (e.g., "device", "cookie", "userGroup", "path")
     */
    @NotNull
    String getEvaluatorType();
}
