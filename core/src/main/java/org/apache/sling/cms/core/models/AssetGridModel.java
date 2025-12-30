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
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.PublishableResource;
import org.apache.sling.cms.publication.PublicationManager;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the Asset Grid component
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class AssetGridModel {

    private static final int PAGE_SIZE = 60;

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @SlingObject
    private ResourceResolver resourceResolver;

    private int paginationPage;
    private Resource suffixResource;
    private List<AssetItem> items;
    private int totalItems;
    private String dataPath;
    private String gridIconsBase;

    @PostConstruct
    protected void init() {
        // Get pagination page from request parameter
        String pageParam = request.getParameter("page");
        paginationPage = pageParam != null && !pageParam.isEmpty() ? Integer.parseInt(pageParam) : 0;

        // Get suffix resource
        suffixResource = request.getRequestPathInfo().getSuffixResource();

        // Get branding
        Resource brandingResource = resourceResolver.getResource("/mnt/overlay/sling-cms/content/branding");
        if (brandingResource != null) {
            ValueMap brandingProps = brandingResource.getValueMap();
            gridIconsBase = brandingProps.get("gridIconsBase", "/static/sling-cms/content/icons");
        } else {
            gridIconsBase = "/static/sling-cms/content/icons";
        }

        // Build data path
        dataPath = resource.getPath() + ".assetgrid.html"
                + (request.getRequestPathInfo().getSuffix() != null
                        ? request.getRequestPathInfo().getSuffix()
                        : "");

        // Load items
        items = new ArrayList<>();
        if (suffixResource != null) {
            // Get allowed types
            List<String> allowedTypes = getAllowedTypes();

            // Get all children
            List<Resource> allChildren = StreamSupport.stream(
                            suffixResource.getChildren().spliterator(), false)
                    .collect(Collectors.toList());

            totalItems = allChildren.size();

            // Calculate pagination range
            int start = paginationPage * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, totalItems);

            // Filter and paginate
            for (int i = start; i < end && i < allChildren.size(); i++) {
                Resource child = allChildren.get(i);
                String primaryType = child.getValueMap().get("jcr:primaryType", String.class);

                if (primaryType != null && allowedTypes.contains(primaryType)) {
                    items.add(new AssetItem(child, resourceResolver, gridIconsBase, request.getLocale(), resource));
                }
            }
        }
    }

    private List<String> getAllowedTypes() {
        List<String> types = new ArrayList<>();
        Resource typesResource = resource.getChild("types");
        if (typesResource != null) {
            StreamSupport.stream(typesResource.getChildren().spliterator(), false)
                    .forEach(type -> types.add(type.getName()));
        }
        return types;
    }

    public int getPaginationPage() {
        return paginationPage;
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    public List<AssetItem> getItems() {
        return items;
    }

    public int getItemCount() {
        return items.size();
    }

    public int getTotalItems() {
        return totalItems;
    }

    public boolean isHasPreviousPage() {
        return paginationPage > 0;
    }

    public boolean isHasNextPage() {
        return (paginationPage + 1) * PAGE_SIZE < totalItems;
    }

    public int getPreviousPage() {
        return paginationPage - 1;
    }

    public int getNextPage() {
        return paginationPage + 1;
    }

    public String getDataPath() {
        return dataPath;
    }

    /**
     * Inner class to represent an asset item in the grid
     */
    public static class AssetItem {
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

        public List<Action> getActions() {
            Resource actionsConfig = configResource.getChild("types/" + primaryType + "/columns/actions");
            if (actionsConfig != null) {
                return StreamSupport.stream(actionsConfig.getChildren().spliterator(), false)
                        .map(r -> new Action(r, path))
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        }
    }

    /**
     * Represents an action button configuration
     */
    public static class Action {
        private final String resourceType;
        private final String itemPath;
        private final String title;
        private final String icon;
        private final String prefix;
        private final String suffix;
        private final String ajaxPath;
        private final boolean openInNew;

        public Action(Resource actionResource, String itemPath) {
            this.itemPath = itemPath;
            ValueMap props = actionResource.getValueMap();
            this.resourceType = props.get("sling:resourceType", String.class);
            this.title = props.get("title", "");
            this.icon = props.get("icon", "");
            this.prefix = props.get("prefix", "");
            this.suffix = props.get("suffix", "");
            this.ajaxPath = props.get("ajaxPath", String.class);
            this.openInNew = props.get("openInNew", false);
        }

        public String getResourceType() {
            return resourceType;
        }

        public String getItemPath() {
            return itemPath;
        }

        public String getTitle() {
            return title;
        }

        public String getIcon() {
            return icon;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        public String getAjaxPath() {
            return ajaxPath;
        }

        public boolean isOpenInNew() {
            return openInNew;
        }
    }
}
