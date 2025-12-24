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

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a delivery preset configuration for front-end asset delivery.
 * Delivery presets provide a higher-level abstraction over transformations,
 * combining format, sizes, quality, and cropping into productized profiles
 * that are safe for authors and developers to use.
 *
 * <p>Configuration is stored under: /conf/{site}/dam/delivery-presets/*</p>
 *
 * <p>Example preset configurations:</p>
 * <ul>
 *   <li>hero-banner: 1920x600, webp/jpg, quality 85, crop center</li>
 *   <li>card-thumbnail: 400x300, webp/jpg, quality 80, crop smart</li>
 *   <li>avatar: 128x128, webp/jpg, quality 90, crop center-face</li>
 * </ul>
 */
@ProviderType
public interface DeliveryPreset {

    /**
     * The unique name/identifier of this preset.
     * Used in URLs and API calls.
     *
     * @return the preset name (e.g., "hero-banner", "card-thumbnail")
     */
    @NotNull
    String getName();

    /**
     * Human-readable title for UI display.
     *
     * @return the display title
     */
    @NotNull
    String getTitle();

    /**
     * Optional description of the preset's intended use.
     *
     * @return description or null
     */
    @Nullable
    String getDescription();

    /**
     * The target width in pixels.
     *
     * @return width or 0 if not constrained
     */
    int getWidth();

    /**
     * The target height in pixels.
     *
     * @return height or 0 if not constrained
     */
    int getHeight();

    /**
     * The output quality (1-100).
     * Higher values mean better quality but larger file sizes.
     *
     * @return quality percentage (default 80)
     */
    int getQuality();

    /**
     * The preferred output format.
     *
     * @return format like "webp", "avif", "jpg", "png"
     */
    @NotNull
    String getFormat();

    /**
     * Fallback formats if the preferred format is not supported.
     * Ordered by preference (first is most preferred fallback).
     *
     * @return list of fallback formats (e.g., ["jpg", "png"])
     */
    @NotNull
    List<String> getFallbackFormats();

    /**
     * The crop mode to use.
     *
     * @return crop mode (e.g., "center", "smart", "face", "none")
     */
    @NotNull
    String getCropMode();

    /**
     * Whether to maintain aspect ratio when resizing.
     *
     * @return true to maintain aspect ratio
     */
    boolean isKeepAspectRatio();

    /**
     * Whether this preset is enabled.
     * Disabled presets are not available for use.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * The underlying transformation name to use for image processing.
     * If set, uses an existing transformation; if null, creates dynamic transformation.
     *
     * @return transformation name or null for dynamic
     */
    @Nullable
    String getTransformationName();

    /**
     * Get the configuration resource path.
     *
     * @return the JCR path to this preset's configuration
     */
    @NotNull
    String getPath();

    /**
     * Get the underlying resource.
     *
     * @return the configuration resource
     */
    @NotNull
    Resource getResource();

    /**
     * Categories/tags for organizing presets in UI.
     *
     * @return list of category names (e.g., ["marketing", "social"])
     */
    @NotNull
    List<String> getCategories();

    /**
     * Check if this preset supports a specific mime type.
     *
     * @param mimeType the mime type to check (e.g., "image/jpeg")
     * @return true if this preset can process the mime type
     */
    boolean supportsMimeType(String mimeType);
}
