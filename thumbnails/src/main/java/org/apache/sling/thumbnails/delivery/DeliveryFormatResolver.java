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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service for resolving the best format for asset delivery based on
 * browser capabilities (Accept header) and preset configuration.
 *
 * <p>Resolution priority:</p>
 * <ol>
 *   <li>Check Accept header for modern formats (avif, webp)</li>
 *   <li>Use preset's preferred format if supported</li>
 *   <li>Fall back through preset's fallback list</li>
 *   <li>Use original format as last resort</li>
 * </ol>
 */
@ProviderType
public interface DeliveryFormatResolver {

    /**
     * Resolve the best format for delivery based on request headers and preset.
     *
     * @param request the HTTP request (for Accept header inspection)
     * @param preset the delivery preset configuration
     * @param asset the asset resource
     * @return the best format to use
     */
    @NotNull
    String resolveFormat(
            @NotNull SlingHttpServletRequest request, @NotNull DeliveryPreset preset, @NotNull Resource asset);

    /**
     * Resolve the best format without request context (use preset defaults).
     *
     * @param preset the delivery preset configuration
     * @param asset the asset resource
     * @return the best format to use
     */
    @NotNull
    String resolveFormat(@NotNull DeliveryPreset preset, @NotNull Resource asset);

    /**
     * Get all formats supported by the client from Accept header.
     *
     * @param request the HTTP request
     * @return list of supported image formats
     */
    @NotNull
    List<String> getClientSupportedFormats(@NotNull SlingHttpServletRequest request);

    /**
     * Check if a specific format is supported by the client.
     *
     * @param request the HTTP request
     * @param format the format to check (e.g., "webp", "avif")
     * @return true if supported
     */
    boolean isFormatSupported(@NotNull SlingHttpServletRequest request, @NotNull String format);

    /**
     * Get the MIME type for a format.
     *
     * @param format the format name (e.g., "webp", "jpg", "png")
     * @return the MIME type (e.g., "image/webp")
     */
    @NotNull
    String getMimeType(@NotNull String format);

    /**
     * Get the file extension for a format.
     *
     * @param format the format name
     * @return the file extension without dot (e.g., "jpg")
     */
    @NotNull
    String getExtension(@NotNull String format);

    /**
     * Resolve full delivery result with URL and metadata.
     *
     * @param request the HTTP request
     * @param asset the asset resource
     * @param preset the delivery preset
     * @return the delivery resolution result
     */
    @NotNull
    DeliveryResolution resolve(
            @NotNull SlingHttpServletRequest request, @NotNull Resource asset, @NotNull DeliveryPreset preset);

    /**
     * Resolve delivery with explicit format override.
     *
     * @param asset the asset resource
     * @param preset the delivery preset
     * @param format the format to use
     * @return the delivery resolution result
     */
    @NotNull
    DeliveryResolution resolve(@NotNull Resource asset, @NotNull DeliveryPreset preset, @Nullable String format);
}
