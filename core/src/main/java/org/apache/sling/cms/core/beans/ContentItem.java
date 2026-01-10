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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.cms.PublishableResource;
import org.apache.sling.cms.publication.PublicationManager;

/**
 * Represents a content item in the content grid.
 * Contains metadata and properties for displaying content (pages, sites, folders, files).
 */
public class ContentItem {

    private final Resource resource;
    private final Resource configResource;
    private final ResourceResolver resolver;
    private final ValueMap properties;

    /**
     * Constructor for ContentItem
     *
     * @param resource The resource representing the content item
     * @param configResource Configuration resource for the content grid
     * @param resolver The resource resolver
     */
    public ContentItem(Resource resource, Resource configResource, ResourceResolver resolver) {
        this.resource = resource;
        this.configResource = configResource;
        this.resolver = resolver;
        this.properties = resource.getValueMap();
    }

    public String getPath() {
        return resource.getPath();
    }

    public String getName() {
        return resource.getName();
    }

    public String getResourceType() {
        return resource.getResourceType();
    }

    public String getPrimaryType() {
        return properties.get("jcr:primaryType", String.class);
    }

    /**
     * Get the display title for the content item.
     * Falls back to jcr:content/jcr:title, then jcr:title, then resource name.
     *
     * @return Display title
     */
    public String getTitle() {
        Resource contentResource = resource.getChild("jcr:content");
        if (contentResource != null) {
            String title = contentResource.getValueMap().get("jcr:title", String.class);
            if (StringUtils.isNotBlank(title)) {
                return title;
            }
        }
        String title = properties.get("jcr:title", String.class);
        return StringUtils.isNotBlank(title) ? title : resource.getName();
    }

    /**
     * Get MIME type for file resources
     *
     * @return MIME type or empty string
     */
    public String getMimeType() {
        Resource contentResource = resource.getChild("jcr:content");
        return contentResource != null ? contentResource.getValueMap().get("jcr:mimeType", "") : "";
    }

    /**
     * Check if this item is a folder
     *
     * @return true if folder type
     */
    public boolean isFolder() {
        String rt = getResourceType();
        return "sling:OrderedFolder".equals(rt) || "sling:Folder".equals(rt) || "nt:folder".equals(rt);
    }

    /**
     * Get taxonomy tags as comma-separated string for filtering
     *
     * @return Taxonomy string
     */
    public String getTaxonomyString() {
        Resource contentResource = resource.getChild("jcr:content");
        if (contentResource == null) {
            return "";
        }

        Object taxonomy = contentResource.getValueMap().get("sling:taxonomy");
        if (taxonomy == null) {
            return "";
        }

        if (taxonomy instanceof String[]) {
            return String.join(",", (String[]) taxonomy);
        } else if (taxonomy instanceof String) {
            return (String) taxonomy;
        }
        return "";
    }

    /**
     * Get the configuration path for this item's type
     *
     * @return Type configuration path or empty string
     */
    public String getTypePath() {
        String primaryType = getPrimaryType();
        if (StringUtils.isNotBlank(primaryType)) {
            Resource typeConfig = configResource.getChild("types/" + primaryType);
            if (typeConfig != null) {
                return typeConfig.getPath();
            }
        }
        return "";
    }

    /**
     * Get page status for filtering (published, draft, scheduled).
     *
     * @return page status or empty string
     */
    public String getPageStatus() {
        Resource contentResource = resource.getChild("jcr:content");
        if (contentResource == null) {
            return "";
        }
        // Check if published property exists
        boolean published = contentResource.getValueMap().get("published", false);
        return published ? "published" : "draft";
    }

    /**
     * Get template name for filtering.
     *
     * @return template name or empty string
     */
    public String getTemplate() {
        Resource contentResource = resource.getChild("jcr:content");
        if (contentResource == null) {
            return "";
        }
        String resourceType = contentResource.getValueMap().get("sling:resourceType", "");
        // Extract template name from resource type path
        if (resourceType.contains("/")) {
            String[] parts = resourceType.split("/");
            return parts[parts.length - 1];
        }
        return resourceType;
    }

    /**
     * Get modified date category for filtering (today, week, month, year).
     *
     * @return date category or empty string
     */
    public String getModifiedDate() {
        Resource contentResource = resource.getChild("jcr:content");
        if (contentResource == null) {
            return "";
        }

        Calendar modified = contentResource.getValueMap().get("jcr:lastModified", Calendar.class);
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
     * Get the name column configuration prefix
     *
     * @return URL prefix for name column
     */
    public String getNameConfigPrefix() {
        Resource nameConfig = configResource.getChild("types/" + getPrimaryType() + "/columns/name");
        if (nameConfig != null) {
            return nameConfig.getValueMap().get("prefix", "");
        }
        return "";
    }

    /**
     * Get the thumbnail path for this content item.
     * Returns different thumbnails based on resource type.
     *
     * @return Thumbnail image path
     */
    public String getThumbnailPath() {
        String rt = getResourceType();
        if ("sling:File".equals(rt) || "nt:file".equals(rt)) {
            return "/cms/file/preview.html" + getPath() + ".transform/sling-cms-thumbnail.png";
        } else if ("sling:Site".equals(rt)) {
            return "/cms/file/preview.html" + getBrandingGridIconsBase() + "/site.png";
        } else if (isFolder()) {
            return "/cms/file/preview.html" + getBrandingGridIconsBase() + "/folder.png";
        } else if ("sling:Page".equals(rt)) {
            Resource contentResource = resource.getChild("jcr:content");
            if (contentResource != null) {
                String template = contentResource.getValueMap().get("sling:template", String.class);
                if (StringUtils.isNotBlank(template)) {
                    Resource templateThumbnail = resolver.getResource(template + "/thumbnail");
                    if (templateThumbnail != null) {
                        return "/cms/file/preview.html" + template + "/thumbnail.transform/sling-cms-thumbnail.png";
                    }
                }
            }
            return "/cms/file/preview.html" + getBrandingGridIconsBase() + "/page.png";
        }
        return "/cms/file/preview.html" + getBrandingGridIconsBase() + "/file.png";
    }

    /**
     * Get the base path for branding grid icons
     *
     * @return Grid icons base path
     */
    private String getBrandingGridIconsBase() {
        Resource brandingResource = resolver.getResource("/conf/global/sling-cms/branding");
        if (brandingResource != null) {
            return brandingResource.getValueMap().get("gridIconsBase", "/static/sling-cms/thumbnails");
        }
        return "/static/sling-cms/thumbnails";
    }

    /**
     * Get list of available actions for this content item based on configuration
     *
     * @return List of ContentAction objects
     */
    public List<ContentAction> getActions() {
        Resource actionsConfig = configResource.getChild("types/" + getPrimaryType() + "/columns/actions");
        if (actionsConfig != null) {
            return StreamSupport.stream(actionsConfig.getChildren().spliterator(), false)
                    .map(r -> new ContentAction(r, getPath()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * Get formatted last modified date string
     *
     * @return Formatted date string
     */
    public String getLastModified() {
        Resource contentResource = resource.getChild("jcr:content");
        if (contentResource != null) {
            Calendar lastModified = contentResource.getValueMap().get("jcr:lastModified", Calendar.class);
            if (lastModified != null) {
                return String.format("%1$tB %1$te, %1$tY %1$tl:%1$tM:%1$tS %1$Tp", lastModified);
            }
        }
        return "";
    }

    /**
     * Check if this content item is published
     *
     * @return true if published
     */
    public boolean isPublished() {
        PublishableResource publishable = resource.adaptTo(PublishableResource.class);
        return publishable != null && publishable.isPublished();
    }

    /**
     * Check if this content item can be published
     *
     * @return true if publishable
     */
    public boolean isPublishable() {
        String rt = getResourceType();
        return "sling:Page".equals(rt) || "sling:File".equals(rt) || "nt:file".equals(rt);
    }

    /**
     * Check if this content item can be navigated into
     *
     * @return true if navigable
     */
    public boolean isNavigable() {
        String rt = getResourceType();
        return "sling:Site".equals(rt) || isFolder() || "sling:Page".equals(rt);
    }

    /**
     * Check if content distribution mode is enabled
     *
     * @return true if content distribution mode
     */
    public boolean isContentDistribution() {
        PublicationManager pm = resolver.adaptTo(PublicationManager.class);
        return pm != null && "CONTENT_DISTRIBUTION".equals(pm.getPublicationMode());
    }

    /**
     * Get request attributes for the actions column component.
     * This includes the colConfigPath needed by ActionsColumnModel.
     *
     * @return ValueMap with colConfigPath attribute for this item's type
     */
    public ValueMap getActionsRequestAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        String primaryType = getPrimaryType();
        Resource actionsConfig = configResource.getChild("types/" + primaryType + "/columns/actions");
        if (actionsConfig != null) {
            attrs.put("colConfigPath", actionsConfig.getPath());
        }
        return new ValueMapDecorator(attrs);
    }
}
