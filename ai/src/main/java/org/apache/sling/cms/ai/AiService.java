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
package org.apache.sling.cms.ai;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Base marker interface for all AI services in Sling CMS.
 * <p>
 * All AI service implementations should implement this interface along with
 * their specific capability interface (e.g., {@link AiTextService},
 * {@link AiClassificationService}, {@link AiImageService}).
 * </p>
 * <p>
 * AI services are designed to be:
 * </p>
 * <ul>
 *   <li><strong>Vendor-neutral</strong>: Switch providers without changing templates</li>
 *   <li><strong>Author-first</strong>: AI suggests; humans approve</li>
 *   <li><strong>Auditable</strong>: Every AI-assisted change is traceable</li>
 *   <li><strong>Safe by default</strong>: Protect PII, enforce permissions</li>
 * </ul>
 */
@ProviderType
public interface AiService {

    /**
     * Gets the unique identifier for this AI service provider.
     * This should be human-readable and URL-safe.
     *
     * @return the provider ID (e.g., "openai", "azure-openai", "rules-based")
     */
    String getId();

    /**
     * Gets the display title for this AI service provider.
     *
     * @return the user-displayed title
     */
    String getTitle();

    /**
     * Returns whether this AI service is currently enabled and available.
     *
     * @return true if the service is enabled and ready, false otherwise
     */
    boolean isEnabled();

    /**
     * Returns whether this service requires an external API connection.
     * Rule-based fallback implementations would return false.
     *
     * @return true if external API is required, false for local implementations
     */
    boolean requiresExternalApi();
}
