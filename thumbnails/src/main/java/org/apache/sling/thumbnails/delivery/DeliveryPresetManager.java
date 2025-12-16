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
 * Service for managing and resolving delivery presets.
 * Provides methods to find presets by name, list available presets,
 * and resolve the best rendition for an asset based on a preset.
 */
@ProviderType
public interface DeliveryPresetManager {

    /**
     * Configuration path pattern for delivery presets.
     */
    String CONFIG_PATH = "/dam/delivery-presets";

    /**
     * Get a delivery preset by name for a given context resource.
     * Uses CA Config pattern to resolve site-specific presets.
     *
     * @param contextResource the resource to use for configuration resolution
     * @param presetName the name of the preset to find
     * @return the preset or null if not found
     */
    @Nullable
    DeliveryPreset getPreset(@NotNull Resource contextResource, @NotNull String presetName);

    /**
     * Get all available delivery presets for a given context resource.
     *
     * @param contextResource the resource to use for configuration resolution
     * @return list of available presets (never null, may be empty)
     */
    @NotNull
    List<DeliveryPreset> getPresets(@NotNull Resource contextResource);

    /**
     * Get all enabled delivery presets for a given context resource.
     *
     * @param contextResource the resource to use for configuration resolution
     * @return list of enabled presets
     */
    @NotNull
    List<DeliveryPreset> getEnabledPresets(@NotNull Resource contextResource);

    /**
     * Get delivery presets by category.
     *
     * @param contextResource the resource to use for configuration resolution
     * @param category the category to filter by
     * @return list of presets in the given category
     */
    @NotNull
    List<DeliveryPreset> getPresetsByCategory(@NotNull Resource contextResource, @NotNull String category);

    /**
     * Resolve the delivery URL for an asset using a specific preset.
     * The URL will include format negotiation based on the preset configuration.
     *
     * @param asset the asset resource
     * @param preset the delivery preset to use
     * @return the delivery URL for the asset
     */
    @NotNull
    String getDeliveryUrl(@NotNull Resource asset, @NotNull DeliveryPreset preset);

    /**
     * Resolve the delivery URL with format override.
     *
     * @param asset the asset resource
     * @param preset the delivery preset to use
     * @param format the specific format to request (e.g., "webp", "jpg")
     * @return the delivery URL for the asset
     */
    @NotNull
    String getDeliveryUrl(@NotNull Resource asset, @NotNull DeliveryPreset preset, @NotNull String format);

    /**
     * Resolve the delivery URL for an asset using a preset name.
     *
     * @param asset the asset resource
     * @param presetName the name of the preset to use
     * @return the delivery URL or null if preset not found
     */
    @Nullable
    String getDeliveryUrl(@NotNull Resource asset, @NotNull String presetName);

    /**
     * Get all available categories from all presets.
     *
     * @param contextResource the resource to use for configuration resolution
     * @return list of unique category names
     */
    @NotNull
    List<String> getCategories(@NotNull Resource contextResource);

    /**
     * Check if a preset exists for the given context.
     *
     * @param contextResource the resource to use for configuration resolution
     * @param presetName the name of the preset
     * @return true if the preset exists
     */
    boolean presetExists(@NotNull Resource contextResource, @NotNull String presetName);
}
