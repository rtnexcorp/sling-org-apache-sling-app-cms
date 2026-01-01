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
package org.apache.sling.thumbnails.internal.models;

import javax.inject.Named;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.thumbnails.delivery.DeliveryPreset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sling Model implementation of DeliveryPreset.
 * Reads configuration from resource properties.
 */
@Model(adaptables = Resource.class, adapters = DeliveryPreset.class)
public class DeliveryPresetImpl implements DeliveryPreset {

    /**
     * Resource type for delivery preset configuration nodes.
     */
    public static final String RESOURCE_TYPE = "sling/thumbnails/deliverypreset";

    /**
     * Supported image MIME types that can be processed.
     */
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/avif", "image/tiff", "image/bmp");

    @Self
    private Resource resource;

    @ValueMapValue
    @Named("jcr:title")
    @Default(values = "")
    private String title;

    @ValueMapValue
    @Named("jcr:description")
    @Optional
    private String description;

    @ValueMapValue
    @Default(intValues = 0)
    private int width;

    @ValueMapValue
    @Default(intValues = 0)
    private int height;

    @ValueMapValue
    @Default(intValues = 80)
    private int quality;

    @ValueMapValue
    @Default(values = "webp")
    private String format;

    @ValueMapValue
    @Optional
    private String[] fallbackFormats;

    @ValueMapValue
    @Default(values = "center")
    private String cropMode;

    @ValueMapValue
    @Default(booleanValues = true)
    private boolean keepAspectRatio;

    @ValueMapValue
    @Default(booleanValues = true)
    private boolean enabled;

    @ValueMapValue
    @Optional
    private String transformationName;

    @ValueMapValue
    @Optional
    private String[] categories;

    @Override
    @NotNull
    public String getName() {
        return resource.getName();
    }

    @Override
    @NotNull
    public String getTitle() {
        return title != null && !title.isEmpty() ? title : getName();
    }

    @Override
    @Nullable
    public String getDescription() {
        return description;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getQuality() {
        return Math.max(1, Math.min(100, quality));
    }

    @Override
    @NotNull
    public String getFormat() {
        return format != null ? format.toLowerCase() : "webp";
    }

    @Override
    @NotNull
    public List<String> getFallbackFormats() {
        if (fallbackFormats == null || fallbackFormats.length == 0) {
            // Default fallback chain
            return Arrays.asList("jpg", "png");
        }
        return Arrays.asList(fallbackFormats);
    }

    @Override
    @NotNull
    public String getCropMode() {
        return cropMode != null ? cropMode : "center";
    }

    @Override
    public boolean isKeepAspectRatio() {
        return keepAspectRatio;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    @Nullable
    public String getTransformationName() {
        return transformationName;
    }

    @Override
    @NotNull
    public String getPath() {
        return resource.getPath();
    }

    @Override
    @NotNull
    public Resource getResource() {
        return resource;
    }

    @Override
    @NotNull
    public List<String> getCategories() {
        if (categories == null || categories.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(categories);
    }

    @Override
    public boolean supportsMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return SUPPORTED_MIME_TYPES.contains(mimeType.toLowerCase());
    }

    @Override
    public String toString() {
        return "DeliveryPreset[name=" + getName() + ", format=" + getFormat() + ", size=" + getWidth() + "x"
                + getHeight() + ", quality=" + getQuality() + "]";
    }
}
