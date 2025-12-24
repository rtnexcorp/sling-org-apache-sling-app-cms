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
package org.apache.sling.thumbnails.delivery;

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Result of resolving a delivery request to a specific rendition.
 * Contains the resolved format, URL, and metadata about the resolution.
 */
@ProviderType
public interface DeliveryResolution {

    /**
     * Get the resolved format (e.g., "webp", "jpg", "png").
     * This may differ from the requested format based on browser support
     * and fallback configuration.
     *
     * @return the resolved format
     */
    @NotNull
    String getResolvedFormat();

    /**
     * Get the URL to the resolved rendition.
     *
     * @return the rendition URL
     */
    @NotNull
    String getUrl();

    /**
     * Get the original requested format.
     *
     * @return the requested format
     */
    @NotNull
    String getRequestedFormat();

    /**
     * Check if the resolved format differs from the requested format.
     *
     * @return true if a fallback format was used
     */
    boolean isFallback();

    /**
     * Get the MIME type for the resolved format.
     *
     * @return the MIME type (e.g., "image/webp")
     */
    @NotNull
    String getMimeType();

    /**
     * Get the width of the resolved rendition.
     *
     * @return width in pixels
     */
    int getWidth();

    /**
     * Get the height of the resolved rendition.
     *
     * @return height in pixels
     */
    int getHeight();

    /**
     * Get the quality setting used.
     *
     * @return quality percentage (1-100)
     */
    int getQuality();

    /**
     * Get the preset name used for this resolution.
     *
     * @return the preset name
     */
    @NotNull
    String getPresetName();

    /**
     * Check if the resolution was successful.
     *
     * @return true if resolved successfully
     */
    boolean isSuccess();

    /**
     * Get error message if resolution failed.
     *
     * @return error message or null if successful
     */
    String getErrorMessage();
}
