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
package org.apache.sling.thumbnails;

import java.util.List;

import org.apache.sling.api.resource.Resource;

/**
 * Interface for asset metadata information used in the DAM metadata editor.
 * Provides access to asset properties, file information, and renditions.
 */
public interface AssetMetadata {

    /**
     * @return the asset resource
     */
    Resource getAsset();

    /**
     * @return the asset content resource (jcr:content)
     */
    Resource getAssetContent();

    /**
     * @return the asset file name
     */
    String getFileName();

    /**
     * @return the asset path
     */
    String getAssetPath();

    /**
     * @return the asset content path for form submission
     */
    String getAssetContentPath();

    /**
     * @return the MIME type of the asset
     */
    String getMimeType();

    /**
     * @return true if the asset is an image
     */
    boolean isImage();

    /**
     * @return true if the asset is a video
     */
    boolean isVideo();

    /**
     * @return true if the asset is a PDF
     */
    boolean isPdf();

    /**
     * @return the file size in bytes
     */
    long getFileSize();

    /**
     * @return formatted file size string (e.g., "1.5 MB")
     */
    String getFormattedFileSize();

    /**
     * @return the last modified date formatted string
     */
    String getLastModified();

    /**
     * @return the created date formatted string
     */
    String getCreated();

    /**
     * @return image width in pixels (for images only)
     */
    Integer getImageWidth();

    /**
     * @return image height in pixels (for images only)
     */
    Integer getImageHeight();

    /**
     * @return true if dimensions are available
     */
    boolean hasDimensions();

    /**
     * @return the thumbnail URL for preview
     */
    String getThumbnailUrl();

    /**
     * @return list of rendition resources
     */
    List<Resource> getRenditions();

    /**
     * @return true if asset has renditions
     */
    boolean hasRenditions();

    // Metadata fields
    String getTitle();

    String getDescription();

    String getAltText();

    String getCopyright();

    String getCreator();

    String getSource();

    String getKeywords();
}
