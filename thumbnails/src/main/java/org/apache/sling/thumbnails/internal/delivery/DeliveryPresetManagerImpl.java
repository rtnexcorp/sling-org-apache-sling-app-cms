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
package org.apache.sling.thumbnails.internal.delivery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.cms.Site;
import org.apache.sling.cms.SiteManager;
import org.apache.sling.thumbnails.delivery.DeliveryPreset;
import org.apache.sling.thumbnails.delivery.DeliveryPresetManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of DeliveryPresetManager.
 * Uses CA Config pattern to resolve site-specific delivery presets.
 */
@Component(service = DeliveryPresetManager.class)
public class DeliveryPresetManagerImpl implements DeliveryPresetManager {

    private static final Logger log = LoggerFactory.getLogger(DeliveryPresetManagerImpl.class);

    private static final String DELIVERY_PRESETS_PATH = "dam/delivery-presets";
    private static final String DELIVER_EXTENSION = ".deliver";

    @Reference
    private ConfigurationResourceResolver configResourceResolver;

    @Override
    @Nullable
    public DeliveryPreset getPreset(@NotNull Resource contextResource, @NotNull String presetName) {
        Objects.requireNonNull(contextResource, "Context resource must not be null");
        Objects.requireNonNull(presetName, "Preset name must not be null");

        Resource configResource = getConfigResource(contextResource);
        if (configResource != null) {
            Resource presetResource = configResource.getChild(presetName);
            if (presetResource != null) {
                return presetResource.adaptTo(DeliveryPreset.class);
            }
        }

        // Try global config as fallback
        return getGlobalPreset(contextResource.getResourceResolver(), presetName);
    }

    @Override
    @NotNull
    public List<DeliveryPreset> getPresets(@NotNull Resource contextResource) {
        Objects.requireNonNull(contextResource, "Context resource must not be null");

        List<DeliveryPreset> presets = new ArrayList<>();

        Resource configResource = getConfigResource(contextResource);
        if (configResource != null) {
            presets.addAll(loadPresetsFromResource(configResource));
        }

        // Add global presets that don't conflict with site-specific ones
        List<DeliveryPreset> globalPresets = getGlobalPresets(contextResource.getResourceResolver());
        for (DeliveryPreset globalPreset : globalPresets) {
            boolean exists = presets.stream().anyMatch(p -> p.getName().equals(globalPreset.getName()));
            if (!exists) {
                presets.add(globalPreset);
            }
        }

        return presets;
    }

    @Override
    @NotNull
    public List<DeliveryPreset> getEnabledPresets(@NotNull Resource contextResource) {
        return getPresets(contextResource).stream()
                .filter(DeliveryPreset::isEnabled)
                .collect(Collectors.toList());
    }

    @Override
    @NotNull
    public List<DeliveryPreset> getPresetsByCategory(@NotNull Resource contextResource, @NotNull String category) {
        Objects.requireNonNull(category, "Category must not be null");

        return getEnabledPresets(contextResource).stream()
                .filter(p -> p.getCategories().contains(category))
                .collect(Collectors.toList());
    }

    @Override
    @NotNull
    public String getDeliveryUrl(@NotNull Resource asset, @NotNull DeliveryPreset preset) {
        return getDeliveryUrl(asset, preset, preset.getFormat());
    }

    @Override
    @NotNull
    public String getDeliveryUrl(@NotNull Resource asset, @NotNull DeliveryPreset preset, @NotNull String format) {
        Objects.requireNonNull(asset, "Asset must not be null");
        Objects.requireNonNull(preset, "Preset must not be null");
        Objects.requireNonNull(format, "Format must not be null");

        // URL format: /content/path/to/asset.jpg.deliver/preset-name.webp
        StringBuilder url = new StringBuilder();
        url.append(asset.getPath());
        url.append(DELIVER_EXTENSION);
        url.append("/");
        url.append(preset.getName());
        url.append(".");
        url.append(format.toLowerCase());

        return url.toString();
    }

    @Override
    @Nullable
    public String getDeliveryUrl(@NotNull Resource asset, @NotNull String presetName) {
        DeliveryPreset preset = getPreset(asset, presetName);
        if (preset == null) {
            log.warn("Delivery preset not found: {}", presetName);
            return null;
        }
        return getDeliveryUrl(asset, preset);
    }

    @Override
    @NotNull
    public List<String> getCategories(@NotNull Resource contextResource) {
        return getEnabledPresets(contextResource).stream()
                .flatMap(p -> p.getCategories().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public boolean presetExists(@NotNull Resource contextResource, @NotNull String presetName) {
        return getPreset(contextResource, presetName) != null;
    }

    /**
     * Get the configuration resource for delivery presets.
     */
    private Resource getConfigResource(Resource contextResource) {
        try {
            // First try site-specific config - adapt SiteManager from the resource
            SiteManager mgr = contextResource.adaptTo(SiteManager.class);
            if (mgr != null) {
                Site site = mgr.getSite();
                if (site != null) {
                    String configPath = site.getPath() + "/conf/" + DELIVERY_PRESETS_PATH;
                    Resource siteConfig = contextResource.getResourceResolver().getResource(configPath);
                    if (siteConfig != null) {
                        return siteConfig;
                    }
                }
            }

            // Try CA Config resolution
            if (configResourceResolver != null) {
                return configResourceResolver.getResource(contextResource, "sling:configs", DELIVERY_PRESETS_PATH);
            }
        } catch (Exception e) {
            log.debug("Error resolving config resource", e);
        }
        return null;
    }

    /**
     * Get a preset from global configuration.
     */
    private DeliveryPreset getGlobalPreset(ResourceResolver resolver, String presetName) {
        Resource globalConfig = resolver.getResource("/conf/global/dam/delivery-presets/" + presetName);
        if (globalConfig != null) {
            return globalConfig.adaptTo(DeliveryPreset.class);
        }
        return null;
    }

    /**
     * Get all global presets.
     */
    private List<DeliveryPreset> getGlobalPresets(ResourceResolver resolver) {
        Resource globalConfig = resolver.getResource("/conf/global/dam/delivery-presets");
        if (globalConfig != null) {
            return loadPresetsFromResource(globalConfig);
        }
        return Collections.emptyList();
    }

    /**
     * Load all presets from a parent resource.
     */
    private List<DeliveryPreset> loadPresetsFromResource(Resource parent) {
        return StreamSupport.stream(parent.getChildren().spliterator(), false)
                .map(r -> r.adaptTo(DeliveryPreset.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
