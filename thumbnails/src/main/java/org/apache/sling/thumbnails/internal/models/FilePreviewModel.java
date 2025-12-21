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

import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.thumbnails.DeliveryPresetView;
import org.apache.sling.thumbnails.FilePreview;
import org.apache.sling.thumbnails.RenderedResource;
import org.apache.sling.thumbnails.delivery.DeliveryPreset;
import org.apache.sling.thumbnails.delivery.DeliveryPresetManager;

/**
 * Sling Model implementation of {@link FilePreview} for the DAM.
 * Provides file type detection and preview capabilities for various file types
 * including images, videos, PDFs, and Office documents.
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = FilePreview.class)
public class FilePreviewModel implements FilePreview {

    /** PDF MIME types */
    private static final Set<String> PDF_TYPES = Set.of("application/pdf");

    /** Microsoft Word MIME types */
    private static final Set<String> WORD_TYPES =
            Set.of("application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    /** Microsoft Excel MIME types */
    private static final Set<String> EXCEL_TYPES =
            Set.of("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    /** Microsoft PowerPoint MIME types */
    private static final Set<String> POWERPOINT_TYPES = Set.of(
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    /** Text-like application MIME types */
    private static final Set<String> TEXT_APP_TYPES =
            Set.of("application/json", "application/xml", "application/javascript");

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private DeliveryPresetManager deliveryPresetManager;

    private String filePath;
    private String fileName;
    private String mimeType;
    private List<String> supportedRenditions;
    private List<DeliveryPresetView> deliveryPresets;

    @PostConstruct
    protected void init() {
        filePath = request.getRequestPathInfo().getSuffix();
        Resource fileResource = request.getRequestPathInfo().getSuffixResource();

        if (fileResource != null) {
            fileName = fileResource.getName();
            ValueMap contentProps = fileResource.getValueMap();
            mimeType = contentProps.get("jcr:content/jcr:mimeType", String.class);

            // Get renditions from RenderedResource
            RenderedResource rendered = request.adaptTo(RenderedResource.class);
            if (rendered != null && rendered.getSupportedRenditions() != null) {
                supportedRenditions = new ArrayList<>(rendered.getSupportedRenditions());
            } else {
                supportedRenditions = Collections.emptyList();
            }

            // Get delivery presets and wrap them with URLs
            if (deliveryPresetManager != null) {
                List<DeliveryPreset> presets = deliveryPresetManager.getEnabledPresets(fileResource);
                deliveryPresets = presets.stream()
                        .map(preset -> {
                            // Use a supported format (jpg/png) instead of webp
                            String format = getSupportedFormat(preset);
                            String url = deliveryPresetManager.getDeliveryUrl(fileResource, preset, format);
                            return new DeliveryPresetView(preset, url);
                        })
                        .collect(Collectors.toList());
            } else {
                deliveryPresets = Collections.emptyList();
            }
        } else {
            fileName = "File";
            mimeType = "";
            supportedRenditions = Collections.emptyList();
            deliveryPresets = Collections.emptyList();
        }
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String getMimeType() {
        return mimeType != null ? mimeType : "";
    }

    @Override
    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    @Override
    public boolean isVideo() {
        return mimeType != null && mimeType.startsWith("video/");
    }

    @Override
    public boolean isPdf() {
        return mimeType != null && PDF_TYPES.contains(mimeType);
    }

    @Override
    public boolean isWord() {
        return mimeType != null && WORD_TYPES.contains(mimeType);
    }

    @Override
    public boolean isExcel() {
        return mimeType != null && EXCEL_TYPES.contains(mimeType);
    }

    @Override
    public boolean isPowerPoint() {
        return mimeType != null && POWERPOINT_TYPES.contains(mimeType);
    }

    @Override
    public boolean isText() {
        return mimeType != null && (mimeType.startsWith("text/") || TEXT_APP_TYPES.contains(mimeType));
    }

    @Override
    public boolean isDocument() {
        return isPdf() || isWord() || isExcel() || isPowerPoint();
    }

    @Override
    public boolean isPreviewable() {
        return isImage() || isVideo() || isPdf();
    }

    @Override
    public boolean hasRenditions() {
        return !supportedRenditions.isEmpty();
    }

    @Override
    public List<String> getSupportedRenditions() {
        return supportedRenditions;
    }

    @Override
    public String getPreviewType() {
        if (isImage()) {
            return "image";
        }
        if (isVideo()) {
            return "video";
        }
        if (isPdf()) {
            return "pdf";
        }
        if (isWord()) {
            return "word";
        }
        if (isExcel()) {
            return "excel";
        }
        if (isPowerPoint()) {
            return "powerpoint";
        }
        if (isText()) {
            return "text";
        }
        return "unknown";
    }

    @Override
    public String getFileIcon() {
        if (isImage()) {
            return "jam-picture";
        }
        if (isVideo()) {
            return "jam-video-camera";
        }
        if (isPdf()) {
            return "jam-document";
        }
        if (isWord()) {
            return "jam-document";
        }
        if (isExcel()) {
            return "jam-grid-f";
        }
        if (isPowerPoint()) {
            return "jam-presentation";
        }
        if (isText()) {
            return "jam-code";
        }
        return "jam-files";
    }

    @Override
    public String getFileTypeColor() {
        if (isImage()) {
            return "is-info";
        }
        if (isVideo()) {
            return "is-danger";
        }
        if (isPdf()) {
            return "is-danger";
        }
        if (isWord()) {
            return "is-link";
        }
        if (isExcel()) {
            return "is-success";
        }
        if (isPowerPoint()) {
            return "is-warning";
        }
        if (isText()) {
            return "is-dark";
        }
        return "is-grey";
    }

    @Override
    public String getFileTypeLabel() {
        if (isImage()) {
            return "Image";
        }
        if (isVideo()) {
            return "Video";
        }
        if (isPdf()) {
            return "PDF Document";
        }
        if (isWord()) {
            return "Word Document";
        }
        if (isExcel()) {
            return "Excel Spreadsheet";
        }
        if (isPowerPoint()) {
            return "PowerPoint Presentation";
        }
        if (isText()) {
            return "Text File";
        }
        return "File";
    }

    @Override
    public boolean hasDeliveryPresets() {
        return !deliveryPresets.isEmpty();
    }

    @Override
    public List<DeliveryPresetView> getDeliveryPresets() {
        return deliveryPresets;
    }

    /**
     * Get a supported format for the transformation system.
     * The transformation system currently only supports: gif, jpg/jpeg, png.
     * If the preset uses an unsupported format (like webp), use the first fallback format.
     */
    private String getSupportedFormat(DeliveryPreset preset) {
        String format = preset.getFormat().toLowerCase();

        // Check if format is supported by OutputFileFormat enum
        if ("gif".equals(format) || "jpg".equals(format) || "jpeg".equals(format) || "png".equals(format)) {
            return format;
        }

        // Use fallback formats
        List<String> fallbacks = preset.getFallbackFormats();
        if (fallbacks != null && !fallbacks.isEmpty()) {
            for (String fallback : fallbacks) {
                String fb = fallback.toLowerCase();
                if ("gif".equals(fb) || "jpg".equals(fb) || "jpeg".equals(fb) || "png".equals(fb)) {
                    return fb;
                }
            }
        }

        // Default to jpg if no supported format found
        return "jpg";
    }
}
