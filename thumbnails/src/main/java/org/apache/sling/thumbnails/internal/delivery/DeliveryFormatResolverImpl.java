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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.thumbnails.delivery.DeliveryFormatResolver;
import org.apache.sling.thumbnails.delivery.DeliveryPreset;
import org.apache.sling.thumbnails.delivery.DeliveryResolution;
import org.apache.sling.thumbnails.internal.models.DeliveryResolutionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of DeliveryFormatResolver.
 * Resolves the best format for asset delivery based on browser capabilities.
 */
@Component(service = DeliveryFormatResolver.class)
@Designate(ocd = DeliveryFormatResolverImpl.Config.class)
public class DeliveryFormatResolverImpl implements DeliveryFormatResolver {

    private static final Logger log = LoggerFactory.getLogger(DeliveryFormatResolverImpl.class);

    private static final String ACCEPT_HEADER = "Accept";
    private static final String DELIVER_EXTENSION = ".deliver";

    /**
     * MIME type mapping for common image formats.
     */
    private static final Map<String, String> FORMAT_TO_MIME = new HashMap<>();

    private static final Map<String, String> MIME_TO_FORMAT = new HashMap<>();

    static {
        FORMAT_TO_MIME.put("webp", "image/webp");
        FORMAT_TO_MIME.put("avif", "image/avif");
        FORMAT_TO_MIME.put("jpg", "image/jpeg");
        FORMAT_TO_MIME.put("jpeg", "image/jpeg");
        FORMAT_TO_MIME.put("png", "image/png");
        FORMAT_TO_MIME.put("gif", "image/gif");
        FORMAT_TO_MIME.put("tiff", "image/tiff");
        FORMAT_TO_MIME.put("bmp", "image/bmp");
        FORMAT_TO_MIME.put("heic", "image/heic");
        FORMAT_TO_MIME.put("heif", "image/heif");

        // Reverse mapping
        FORMAT_TO_MIME.forEach((format, mime) -> MIME_TO_FORMAT.put(mime, format));
    }

    @ObjectClassDefinition(
            name = "Apache Sling CMS - Delivery Format Resolver",
            description = "Configuration for delivery format resolution")
    public @interface Config {
        @AttributeDefinition(
                name = "Enable AVIF",
                description = "Enable AVIF format support (requires server-side encoding support)")
        boolean enableAvif() default true;

        @AttributeDefinition(name = "Enable WebP", description = "Enable WebP format support")
        boolean enableWebp() default true;

        @AttributeDefinition(name = "Default Format", description = "Default format when no preference is detected")
        String defaultFormat() default "jpg";

        @AttributeDefinition(
                name = "Format Priority",
                description = "Priority order for modern formats (comma-separated)")
        String[] formatPriority() default {"avif", "webp", "jpg", "png"};
    }

    private boolean enableAvif;
    private boolean enableWebp;
    private String defaultFormat;
    private List<String> formatPriority;

    @Activate
    protected void activate(Config config) {
        this.enableAvif = config.enableAvif();
        this.enableWebp = config.enableWebp();
        this.defaultFormat = config.defaultFormat();
        this.formatPriority = Arrays.asList(config.formatPriority());

        log.info(
                "Delivery Format Resolver activated: avif={}, webp={}, default={}",
                enableAvif,
                enableWebp,
                defaultFormat);
    }

    @Override
    @NotNull
    public String resolveFormat(
            @NotNull SlingHttpServletRequest request, @NotNull DeliveryPreset preset, @NotNull Resource asset) {
        Objects.requireNonNull(request, "Request must not be null");
        Objects.requireNonNull(preset, "Preset must not be null");
        Objects.requireNonNull(asset, "Asset must not be null");

        List<String> clientFormats = getClientSupportedFormats(request);
        String presetFormat = preset.getFormat();

        // Check if preset's preferred format is supported
        if (isFormatEnabledAndSupported(presetFormat, clientFormats)) {
            return presetFormat;
        }

        // Try fallback formats from preset
        for (String fallback : preset.getFallbackFormats()) {
            if (isFormatEnabledAndSupported(fallback, clientFormats)) {
                return fallback;
            }
        }

        // Fall back to universal format
        return defaultFormat;
    }

    @Override
    @NotNull
    public String resolveFormat(@NotNull DeliveryPreset preset, @NotNull Resource asset) {
        // Without request context, use preset's preferred format if available
        String presetFormat = preset.getFormat();

        if (isFormatEnabled(presetFormat)) {
            return presetFormat;
        }

        // Try fallback formats
        for (String fallback : preset.getFallbackFormats()) {
            if (isFormatEnabled(fallback)) {
                return fallback;
            }
        }

        return defaultFormat;
    }

    @Override
    @NotNull
    public List<String> getClientSupportedFormats(@NotNull SlingHttpServletRequest request) {
        String acceptHeader = request.getHeader(ACCEPT_HEADER);
        if (acceptHeader == null || acceptHeader.isEmpty()) {
            return Collections.singletonList(defaultFormat);
        }

        List<String> formats = new ArrayList<>();

        // Check for modern formats
        if (enableAvif && acceptHeader.contains("image/avif")) {
            formats.add("avif");
        }
        if (enableWebp && acceptHeader.contains("image/webp")) {
            formats.add("webp");
        }

        // Add standard formats based on Accept header
        if (acceptHeader.contains("image/jpeg") || acceptHeader.contains("image/*") || acceptHeader.contains("*/*")) {
            formats.add("jpg");
        }
        if (acceptHeader.contains("image/png") || acceptHeader.contains("image/*") || acceptHeader.contains("*/*")) {
            formats.add("png");
        }
        if (acceptHeader.contains("image/gif") || acceptHeader.contains("image/*") || acceptHeader.contains("*/*")) {
            formats.add("gif");
        }

        // Ensure we always have at least the default format
        if (formats.isEmpty()) {
            formats.add(defaultFormat);
        }

        return formats;
    }

    @Override
    public boolean isFormatSupported(@NotNull SlingHttpServletRequest request, @NotNull String format) {
        return getClientSupportedFormats(request).contains(format.toLowerCase());
    }

    @Override
    @NotNull
    public String getMimeType(@NotNull String format) {
        String normalized = format.toLowerCase();
        return FORMAT_TO_MIME.getOrDefault(normalized, "application/octet-stream");
    }

    @Override
    @NotNull
    public String getExtension(@NotNull String format) {
        String normalized = format.toLowerCase();
        // Handle jpeg -> jpg normalization
        if ("jpeg".equals(normalized)) {
            return "jpg";
        }
        return normalized;
    }

    @Override
    @NotNull
    public DeliveryResolution resolve(
            @NotNull SlingHttpServletRequest request, @NotNull Resource asset, @NotNull DeliveryPreset preset) {
        String requestedFormat = preset.getFormat();
        String resolvedFormat = resolveFormat(request, preset, asset);
        boolean isFallback = !requestedFormat.equalsIgnoreCase(resolvedFormat);

        String url = buildDeliveryUrl(asset, preset, resolvedFormat);

        return DeliveryResolutionImpl.builder()
                .resolvedFormat(resolvedFormat)
                .requestedFormat(requestedFormat)
                .url(url)
                .fallback(isFallback)
                .mimeType(getMimeType(resolvedFormat))
                .width(preset.getWidth())
                .height(preset.getHeight())
                .quality(preset.getQuality())
                .presetName(preset.getName())
                .success(true)
                .build();
    }

    @Override
    @NotNull
    public DeliveryResolution resolve(
            @NotNull Resource asset, @NotNull DeliveryPreset preset, @Nullable String format) {
        String resolvedFormat = format != null ? format : resolveFormat(preset, asset);
        String requestedFormat = preset.getFormat();
        boolean isFallback = format != null && !requestedFormat.equalsIgnoreCase(format);

        String url = buildDeliveryUrl(asset, preset, resolvedFormat);

        return DeliveryResolutionImpl.builder()
                .resolvedFormat(resolvedFormat)
                .requestedFormat(requestedFormat)
                .url(url)
                .fallback(isFallback)
                .mimeType(getMimeType(resolvedFormat))
                .width(preset.getWidth())
                .height(preset.getHeight())
                .quality(preset.getQuality())
                .presetName(preset.getName())
                .success(true)
                .build();
    }

    /**
     * Build the delivery URL for an asset with preset and format.
     */
    private String buildDeliveryUrl(Resource asset, DeliveryPreset preset, String format) {
        return asset.getPath() + DELIVER_EXTENSION + "/" + preset.getName() + "." + getExtension(format);
    }

    /**
     * Check if a format is enabled in configuration.
     */
    private boolean isFormatEnabled(String format) {
        String normalized = format.toLowerCase();
        if ("avif".equals(normalized) && !enableAvif) {
            return false;
        }
        if ("webp".equals(normalized) && !enableWebp) {
            return false;
        }
        return true;
    }

    /**
     * Check if a format is both enabled and supported by the client.
     */
    private boolean isFormatEnabledAndSupported(String format, List<String> clientFormats) {
        return isFormatEnabled(format) && clientFormats.contains(format.toLowerCase());
    }
}
