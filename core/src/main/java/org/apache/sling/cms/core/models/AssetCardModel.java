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
package org.apache.sling.cms.core.models;

import javax.annotation.PostConstruct;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the Asset Card component.
 * Displays an individual asset (file) as a card with thumbnail, metadata, and actions.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class AssetCardModel {

    @SlingObject
    private Resource resource;

    @SlingObject
    private SlingHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    private String assetTitle;
    private String assetPath;
    private String mimeType;
    private String thumbnailPath;
    private String fileExtension;
    private String formattedFileSize;
    private String formattedLastModified;
    private boolean isImage;
    private boolean isVideo;
    private boolean isPdf;
    private List<Resource> actionConfigs;
    private String gridIconsBase;

    @PostConstruct
    protected void init() {
        if (resource == null) {
            return;
        }

        ValueMap properties = resource.getValueMap();
        assetPath = resource.getPath();

        // Get title - check jcr:content/jcr:title first, then name
        String jcrContentTitle = properties.get("jcr:content/jcr:title", String.class);
        assetTitle = jcrContentTitle != null ? jcrContentTitle : resource.getName();

        // Get MIME type
        mimeType = properties.get("jcr:content/jcr:mimeType", String.class);

        // Determine asset type flags
        if (mimeType != null) {
            isImage = mimeType.startsWith("image/");
            isVideo = mimeType.startsWith("video/");
            isPdf = "application/pdf".equals(mimeType);

            // Extract file extension from mime type
            if (mimeType.contains("/")) {
                fileExtension = mimeType.substring(mimeType.indexOf('/') + 1).toUpperCase();
            }
        }

        // Get branding for icon paths
        Resource brandingResource = resourceResolver.getResource("/mnt/overlay/sling-cms/content/branding");
        if (brandingResource != null) {
            ValueMap brandingProps = brandingResource.getValueMap();
            gridIconsBase = brandingProps.get("gridIconsBase", "/static/sling-cms/content/icons");
        } else {
            gridIconsBase = "/static/sling-cms/content/icons";
        }

        // Build thumbnail path
        if (isImage || isVideo || isPdf) {
            thumbnailPath = "/cms/file/preview.html" + assetPath + ".transform/sling-cms-thumbnail.png";
        } else {
            thumbnailPath = "/cms/file/preview.html" + gridIconsBase + "/file.png";
        }

        // Format file size
        Long fileSizeBytes = properties.get("jcr:content/jcr:data", Long.class);
        if (fileSizeBytes != null && fileSizeBytes > 0) {
            formattedFileSize = formatFileSize(fileSizeBytes);
        }

        // Format last modified date
        Calendar lastModified = properties.get("jcr:content/jcr:lastModified", Calendar.class);
        if (lastModified != null) {
            DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, request.getLocale());
            formattedLastModified = dateFormat.format(lastModified.getTime());
        }

        // Get action configurations from parent component
        actionConfigs = new ArrayList<>();
        Resource parentResource = resource.getParent();
        if (parentResource != null) {
            Resource actionsResource = parentResource.getChild("actions");
            if (actionsResource != null) {
                actionsResource.getChildren().forEach(actionConfigs::add);
            }
        }
    }

    /**
     * Format file size into human-readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes >= 1048576) { // >= 1 MB
            return String.format("%.1f MB", bytes / 1048576.0);
        } else if (bytes >= 1024) { // >= 1 KB
            return String.format("%d KB", bytes / 1024);
        } else {
            return bytes + " B";
        }
    }

    public String getAssetTitle() {
        return assetTitle;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getFormattedFileSize() {
        return formattedFileSize;
    }

    public String getFormattedLastModified() {
        return formattedLastModified;
    }

    public boolean isImage() {
        return isImage;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public boolean isPdf() {
        return isPdf;
    }

    public List<Resource> getActionConfigs() {
        return actionConfigs;
    }
}
