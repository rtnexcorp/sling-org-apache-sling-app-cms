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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for Asset Card component
 * Provides asset metadata and thumbnail information for DAM asset cards
 */
@Model(adaptables = { Resource.class, SlingHttpServletRequest.class }, 
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class AssetCard {

    private static final long KB = 1024;
    private static final long MB = 1048576;
    private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("#.#");
    private static final DecimalFormat NO_DECIMAL = new DecimalFormat("#");

    @Self
    private Resource asset;

    @SlingObject
    private ResourceResolver resourceResolver;

    @Inject
    private ValueMap properties;

    @ChildResource(name = "jcr:content")
    private Resource content;

    @Inject
    @ChildResource(name = "actionConfigs")
    private List<Resource> actionConfigs;

    /**
     * Get the asset title (from jcr:content/jcr:title or fallback to filename)
     */
    public String getTitle() {
        if (content != null) {
            ValueMap contentProps = content.getValueMap();
            String title = contentProps.get("jcr:title", String.class);
            if (title != null && !title.isEmpty()) {
                return title;
            }
        }
        return asset.getName();
    }

    /**
     * Get the asset's MIME type
     */
    public String getMimeType() {
        if (content != null) {
            return content.getValueMap().get("jcr:mimeType", String.class);
        }
        return null;
    }

    /**
     * Get the file type (extension) from MIME type
     */
    public String getFileType() {
        String mimeType = getMimeType();
        if (mimeType != null && mimeType.contains("/")) {
            return mimeType.substring(mimeType.indexOf("/") + 1);
        }
        return "";
    }

    /**
     * Get the asset path
     */
    public String getPath() {
        return asset.getPath();
    }

    /**
     * Get file size in bytes
     */
    public long getFileSize() {
        if (content != null) {
            ValueMap contentProps = content.getValueMap();
            Object data = contentProps.get("jcr:data");
            if (data instanceof javax.jcr.Binary) {
                try {
                    return ((javax.jcr.Binary) data).getSize();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
        return 0;
    }

    /**
     * Get formatted file size (e.g., "1.5 MB", "256 KB", "512 B")
     */
    public String getFormattedFileSize() {
        long size = getFileSize();
        if (size >= MB) {
            return ONE_DECIMAL.format(size / (double) MB) + " MB";
        } else if (size >= KB) {
            return NO_DECIMAL.format(size / (double) KB) + " KB";
        } else if (size > 0) {
            return size + " B";
        }
        return "";
    }

    /**
     * Get formatted last modified date
     */
    public String getLastModified() {
        if (content != null) {
            Calendar cal = content.getValueMap().get("jcr:lastModified", Calendar.class);
            if (cal != null) {
                Date date = cal.getTime();
                DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
                return df.format(date);
            }
        }
        return null;
    }

    /**
     * Check if the asset is an image
     */
    public boolean isImage() {
        String mimeType = getMimeType();
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * Check if the asset is a video
     */
    public boolean isVideo() {
        String mimeType = getMimeType();
        return mimeType != null && mimeType.startsWith("video/");
    }

    /**
     * Check if the asset is a PDF
     */
    public boolean isPdf() {
        return "application/pdf".equals(getMimeType());
    }

    /**
     * Get the thumbnail URL using delivery presets
     * Uses asset-grid preset (200x200) for consistent grid display
     */
    public String getThumbnailUrl() {
        return getPath() + ".deliver/asset-grid.webp";
    }

    /**
     * Get the fallback thumbnail URL (JPG format for broader compatibility)
     */
    public String getFallbackThumbnailUrl() {
        return getPath() + ".deliver/asset-grid.jpg";
    }

    /**
     * Get the default icon URL for unsupported file types
     */
    public String getDefaultIconUrl() {
        Resource branding = resourceResolver.getResource("/conf/global/sling-cms/branding");
        if (branding != null) {
            String gridIconsBase = branding.getValueMap().get("gridIconsBase", String.class);
            if (gridIconsBase != null) {
                return gridIconsBase + "/file.png";
            }
        }
        return "/static/sling-cms/thumbnails/file.png";
    }

    /**
     * Get action configs for the asset
     */
    public List<Resource> getActionConfigs() {
        return actionConfigs;
    }

    /**
     * Check if asset has action configs
     */
    public boolean hasActionConfigs() {
        return actionConfigs != null && !actionConfigs.isEmpty();
    }
}
