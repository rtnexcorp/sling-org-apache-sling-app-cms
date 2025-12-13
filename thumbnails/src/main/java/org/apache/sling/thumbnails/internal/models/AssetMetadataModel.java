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

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.thumbnails.AssetMetadata;

/**
 * Sling Model implementation of {@link AssetMetadata} for the DAM metadata editor.
 * Provides asset properties, file information, and renditions for editing.
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = AssetMetadata.class)
public class AssetMetadataModel implements AssetMetadata {

    private static final String JCR_CONTENT = "jcr:content";
    private static final String RENDITIONS = "renditions";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy h:mm a");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    @Self
    private SlingHttpServletRequest request;

    private Resource asset;
    private Resource assetContent;
    private ValueMap contentProps;
    private String mimeType;
    private long fileSize;

    @PostConstruct
    protected void init() {
        asset = request.getRequestPathInfo().getSuffixResource();
        if (asset != null) {
            assetContent = asset.getChild(JCR_CONTENT);
            if (assetContent != null) {
                contentProps = assetContent.getValueMap();
                mimeType = contentProps.get("jcr:mimeType", String.class);

                // Calculate file size from jcr:data
                InputStream dataStream = contentProps.get("jcr:data", InputStream.class);
                if (dataStream != null) {
                    try {
                        fileSize = dataStream.available();
                        dataStream.close();
                    } catch (Exception e) {
                        fileSize = 0;
                    }
                }
            }
        }
    }

    @Override
    public Resource getAsset() {
        return asset;
    }

    @Override
    public Resource getAssetContent() {
        return assetContent;
    }

    @Override
    public String getFileName() {
        return asset != null ? asset.getName() : "";
    }

    @Override
    public String getAssetPath() {
        return asset != null ? asset.getPath() : "";
    }

    @Override
    public String getAssetContentPath() {
        return assetContent != null ? assetContent.getPath() : "";
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
        return "application/pdf".equals(mimeType);
    }

    @Override
    public long getFileSize() {
        return fileSize;
    }

    @Override
    public String getFormattedFileSize() {
        if (fileSize >= 1048576) {
            return DECIMAL_FORMAT.format(fileSize / 1048576.0) + " MB";
        } else if (fileSize >= 1024) {
            return DECIMAL_FORMAT.format(fileSize / 1024.0) + " KB";
        } else {
            return fileSize + " bytes";
        }
    }

    @Override
    public String getLastModified() {
        if (contentProps == null) {
            return "";
        }
        Calendar lastMod = contentProps.get("jcr:lastModified", Calendar.class);
        if (lastMod != null) {
            return DATE_FORMAT.format(lastMod.getTime());
        }
        return "";
    }

    @Override
    public String getCreated() {
        if (contentProps == null) {
            return "";
        }
        Calendar created = contentProps.get("jcr:created", Calendar.class);
        if (created != null) {
            return DATE_FORMAT.format(created.getTime());
        }
        return "";
    }

    @Override
    public Integer getImageWidth() {
        if (contentProps == null) {
            return null;
        }
        return contentProps.get("tiff:ImageWidth", Integer.class);
    }

    @Override
    public Integer getImageHeight() {
        if (contentProps == null) {
            return null;
        }
        return contentProps.get("tiff:ImageLength", Integer.class);
    }

    @Override
    public boolean hasDimensions() {
        return isImage() && getImageWidth() != null && getImageHeight() != null;
    }

    @Override
    public String getThumbnailUrl() {
        if (asset == null) {
            return "";
        }
        if (isImage() || isVideo() || isPdf()) {
            return asset.getPath() + ".transform/sling-cms-thumbnail.png";
        }
        return "/static/sling-cms/thumbnails/file.png";
    }

    @Override
    public List<Resource> getRenditions() {
        if (assetContent == null) {
            return Collections.emptyList();
        }
        Resource renditionsRes = assetContent.getChild(RENDITIONS);
        if (renditionsRes == null) {
            return Collections.emptyList();
        }
        List<Resource> result = new ArrayList<>();
        renditionsRes.getChildren().forEach(result::add);
        return result;
    }

    @Override
    public boolean hasRenditions() {
        return !getRenditions().isEmpty();
    }

    // Metadata field getters
    @Override
    public String getTitle() {
        return contentProps != null ? contentProps.get("jcr:title", "") : "";
    }

    @Override
    public String getDescription() {
        return contentProps != null ? contentProps.get("jcr:description", "") : "";
    }

    @Override
    public String getAltText() {
        return contentProps != null ? contentProps.get("alt", "") : "";
    }

    @Override
    public String getCopyright() {
        return contentProps != null ? contentProps.get("dc:rights", "") : "";
    }

    @Override
    public String getCreator() {
        return contentProps != null ? contentProps.get("dc:creator", "") : "";
    }

    @Override
    public String getSource() {
        return contentProps != null ? contentProps.get("dc:source", "") : "";
    }

    @Override
    public String getKeywords() {
        return contentProps != null ? contentProps.get("keywords", "") : "";
    }
}
