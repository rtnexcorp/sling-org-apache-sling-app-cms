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
package org.apache.sling.cms.core.beans;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.PublishableResource;
import org.apache.sling.cms.publication.PublicationManager;

/**
 * Represents an asset item in the asset grid.
 * Contains metadata and properties for displaying assets (files, folders, etc.)
 */
public class AssetItem {

    private final Resource resource;
    private final ResourceResolver resolver;
    private final Resource configResource;
    private final ValueMap properties;
    private final String name;
    private final String path;
    private final String title;
    private final String mimeType;
    private final boolean isFile;
    private final boolean isFolder;
    private final String thumbnailPath;
    private final String fileExtension;
    private final String fileSize;
    private final String lastModified;
    private final String taxonomyStr;
    private final boolean isPublished;
    private final String primaryType;

    /**
     * Constructor for AssetItem
     *
     * @param resource The resource representing the asset
     * @param resolver The resource resolver
     * @param gridIconsBase Base path for grid icons
     * @param locale Locale for date formatting
     * @param configResource Configuration resource for the asset grid
     */
    public AssetItem(
            Resource resource,
            ResourceResolver resolver,
            String gridIconsBase,
            Locale locale,
            Resource configResource) {
        this.resolver = resolver;
        this.configResource = configResource;
        this.resource = resource;
        this.properties = resource.getValueMap();
        this.name = resource.getName();
        this.path = resource.getPath();
        this.primaryType = properties.get("jcr:primaryType", String.class);

        // Determine resource type
        String resourceType = resource.getResourceType();
        this.isFile = "sling:File".equals(resourceType) || "nt:file".equals(resourceType);
        this.isFolder = "sling:OrderedFolder".equals(resourceType)
                || "sling:Folder".equals(resourceType)
                || "nt:folder".equals(resourceType);

        // Get title
        String jcrContentTitle = properties.get("jcr:content/jcr:title", String.class);
        String jcrTitle = properties.get("jcr:title", String.class);
        this.title = jcrContentTitle != null ? jcrContentTitle : (jcrTitle != null ? jcrTitle : name);

        // Get mime type
        this.mimeType = properties.get("jcr:content/jcr:mimeType", String.class);

        // Build thumbnail path
        if (isFile) {
            this.thumbnailPath = "/cms/file/preview.html" + path + ".transform/sling-cms-thumbnail.png";
        } else if (isFolder) {
            this.thumbnailPath = "/cms/file/preview.html" + gridIconsBase + "/folder.png";
        } else {
            this.thumbnailPath = "/cms/file/preview.html" + gridIconsBase + "/file.png";
        }

        // Get file extension from mime type
        if (mimeType != null && mimeType.contains("/")) {
            this.fileExtension = mimeType.substring(mimeType.indexOf('/') + 1);
        } else {
            this.fileExtension = "";
        }

        // Calculate file size
        Long fileSizeBytes = properties.get("jcr:content/jcr:data", Long.class);
        if (fileSizeBytes != null && fileSizeBytes > 0) {
            this.fileSize = formatFileSize(fileSizeBytes);
        } else {
            this.fileSize = null;
        }

        // Format last modified date
        Calendar lastMod = properties.get("jcr:content/jcr:lastModified", Calendar.class);
        if (lastMod != null) {
            DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
            this.lastModified = dateFormat.format(lastMod.getTime());
        } else {
            this.lastModified = null;
        }

        // Get taxonomy
        Object taxonomy = properties.get("jcr:content/sling:taxonomy");
        if (taxonomy != null) {
            if (taxonomy.getClass().isArray()) {
                String[] taxonomies = (String[]) taxonomy;
                this.taxonomyStr = String.join(",", taxonomies);
            } else {
                this.taxonomyStr = taxonomy.toString();
            }
        } else {
            this.taxonomyStr = "";
        }

        // Check publication status
        PublicationManager publicationManager = resolver.adaptTo(PublicationManager.class);
        if (publicationManager != null) {
            PublishableResource publishableResource = resource.adaptTo(PublishableResource.class);
            this.isPublished = publishableResource != null && publishableResource.isPublished();
        } else {
            this.isPublished = false;
        }
    }

    /**
     * Format file size in human-readable format
     *
     * @param bytes Size in bytes
     * @return Formatted size string (e.g., "1.5 MB", "256 KB")
     */
    private String formatFileSize(long bytes) {
        if (bytes >= 1048576) { // MB
            return String.format("%.1f MB", bytes / 1048576.0);
        } else if (bytes >= 1024) { // KB
            return String.format("%d KB", bytes / 1024);
        } else {
            return bytes + " B";
        }
    }

    public Resource getResource() {
        return resource;
    }

    public String getName() {
        return name;
    }

    public String getNameLowercase() {
        return name.toLowerCase();
    }

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isFile() {
        return isFile;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getFileSize() {
        return fileSize;
    }

    public String getLastModified() {
        return lastModified;
    }

    public String getTaxonomyStr() {
        return taxonomyStr;
    }

    public boolean isPublished() {
        return isPublished;
    }

    public boolean isVideo() {
        return mimeType != null && mimeType.startsWith("video/");
    }

    public boolean isPdf() {
        return "application/pdf".equals(mimeType);
    }

    public String getPrimaryType() {
        return primaryType;
    }

    /**
     * Get asset size category for filtering.
     *
     * @return size category string (e.g., "0-100", "100-1024")
     */
    public String getAssetSize() {
        Long fileSizeBytes = properties.get("jcr:content/jcr:data", Long.class);
        if (fileSizeBytes == null || fileSizeBytes == 0) {
            return "";
        }

        long sizeKB = fileSizeBytes / 1024;

        if (sizeKB < 100) {
            return "0-100";
        } else if (sizeKB < 1024) {
            return "100-1024";
        } else if (sizeKB < 10240) {
            return "1024-10240";
        } else {
            return "10240-";
        }
    }

    /**
     * Get modified date category for filtering (today, week, month, year).
     *
     * @return date category or empty string
     */
    public String getModifiedDate() {
        Calendar modified = properties.get("jcr:content/jcr:lastModified", Calendar.class);
        if (modified == null) {
            return "";
        }

        Calendar now = Calendar.getInstance();
        long diffMillis = now.getTimeInMillis() - modified.getTimeInMillis();
        long diffDays = diffMillis / (1000 * 60 * 60 * 24);

        if (diffDays < 1) {
            return "today";
        } else if (diffDays < 7) {
            return "week";
        } else if (diffDays < 30) {
            return "month";
        } else if (diffDays < 365) {
            return "year";
        }
        return "older";
    }

    /**
     * Get list of available actions for this asset based on configuration
     *
     * @return List of Action objects
     */
    public List<AssetAction> getActions() {
        Resource actionsConfig = configResource.getChild("types/" + primaryType + "/columns/actions");
        if (actionsConfig != null) {
            return StreamSupport.stream(actionsConfig.getChildren().spliterator(), false)
                    .map(r -> new AssetAction(r, path))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
