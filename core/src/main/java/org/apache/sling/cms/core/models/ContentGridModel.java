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

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.PublishableResource;
import org.apache.sling.cms.publication.PublicationManager;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the Content Grid component
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ContentGridModel {

    private static final int PAGE_SIZE = 60;

    @SlingObject
    private SlingHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    @Inject
    private Resource resource;

    private List<GridItem> items;
    private int currentPage;
    private boolean hasNextPage;
    private boolean hasPreviousPage;

    public int getCurrentPage() {
        if (currentPage == 0) {
            String pageParam = request.getParameter("page");
            currentPage = StringUtils.isNotBlank(pageParam) ? Integer.parseInt(pageParam) : 0;
        }
        return currentPage;
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    public boolean hasNextPage() {
        getItems(); // ensure items are loaded
        return hasNextPage;
    }

    public boolean hasPreviousPage() {
        return getCurrentPage() > 0;
    }

    public int getPreviousPage() {
        return Math.max(0, getCurrentPage() - 1);
    }

    public int getNextPage() {
        return getCurrentPage() + 1;
    }

    public List<GridItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
            Resource suffixResource = request.getRequestPathInfo().getSuffixResource();
            if (suffixResource != null) {
                Resource typesResource = resource.getChild("types");
                List<String> allowedTypes = new ArrayList<>();
                if (typesResource != null) {
                    StreamSupport.stream(typesResource.getChildren().spliterator(), false)
                            .forEach(type -> allowedTypes.add(type.getName()));
                }

                // First filter by allowed types, then paginate
                Iterator<Resource> children = suffixResource.listChildren();
                List<Resource> filteredChildren = new ArrayList<>();
                children.forEachRemaining(child -> {
                    String primaryType = child.getValueMap().get("jcr:primaryType", String.class);
                    if (allowedTypes.isEmpty() || allowedTypes.contains(primaryType)) {
                        filteredChildren.add(child);
                    }
                });

                int start = getCurrentPage() * PAGE_SIZE;
                int end = Math.min(start + PAGE_SIZE, filteredChildren.size());

                for (int i = start; i < end; i++) {
                    Resource child = filteredChildren.get(i);
                    items.add(new GridItem(child, resource, resourceResolver));
                }

                hasNextPage = end < filteredChildren.size();
            }
        }
        return items;
    }

    public static class GridItem {
        private final Resource resource;
        private final Resource configResource;
        private final ResourceResolver resolver;
        private final ValueMap properties;

        public GridItem(Resource resource, Resource configResource, ResourceResolver resolver) {
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

        public String getMimeType() {
            Resource contentResource = resource.getChild("jcr:content");
            return contentResource != null ? contentResource.getValueMap().get("jcr:mimeType", "") : "";
        }

        public boolean isFolder() {
            String rt = getResourceType();
            return "sling:OrderedFolder".equals(rt) || "sling:Folder".equals(rt) || "nt:folder".equals(rt);
        }

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

        public String getNameConfigPrefix() {
            Resource nameConfig = configResource.getChild("types/" + getPrimaryType() + "/columns/name");
            if (nameConfig != null) {
                return nameConfig.getValueMap().get("prefix", "");
            }
            return "";
        }

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

        private String getBrandingGridIconsBase() {
            Resource brandingResource = resolver.getResource("/conf/global/sling-cms/branding");
            if (brandingResource != null) {
                return brandingResource.getValueMap().get("gridIconsBase", "/static/sling-cms/thumbnails");
            }
            return "/static/sling-cms/thumbnails";
        }

        public List<Action> getActions() {
            Resource actionsConfig = configResource.getChild("types/" + getPrimaryType() + "/columns/actions");
            if (actionsConfig != null) {
                return StreamSupport.stream(actionsConfig.getChildren().spliterator(), false)
                        .map(r -> new Action(r, getPath()))
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        }

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

        public boolean isPublished() {
            PublishableResource publishable = resource.adaptTo(PublishableResource.class);
            return publishable != null && publishable.isPublished();
        }

        public boolean isPublishable() {
            String rt = getResourceType();
            return "sling:Page".equals(rt) || "sling:File".equals(rt) || "nt:file".equals(rt);
        }

        public boolean isNavigable() {
            String rt = getResourceType();
            return "sling:Site".equals(rt) || isFolder() || "sling:Page".equals(rt);
        }

        public boolean isContentDistribution() {
            PublicationManager pm = resolver.adaptTo(PublicationManager.class);
            return pm != null && "CONTENT_DISTRIBUTION".equals(pm.getPublicationMode());
        }
    }

    /**
     * Represents an action button configuration with its resource type and properties
     */
    public static class Action {
        private final String resourceType;
        private final String itemPath;
        private final Resource actionResource;
        private final String title;
        private final String icon;
        private final String prefix;
        private final String suffix;
        private final String ajaxPath;
        private final boolean openInNew;

        public Action(Resource actionResource, String itemPath) {
            this.actionResource = actionResource;
            this.itemPath = itemPath;
            ValueMap vm = actionResource.getValueMap();
            this.resourceType = vm.get("sling:resourceType", String.class);
            this.title = vm.get("title", String.class);
            this.icon = vm.get("icon", String.class);
            this.prefix = vm.get("prefix", "");
            this.suffix = vm.get("suffix", "");
            this.ajaxPath = vm.get("ajaxPath", String.class);
            this.openInNew = vm.get("new", false);
        }

        public String getResourceType() {
            return resourceType;
        }

        public String getItemPath() {
            return itemPath;
        }

        public Resource getActionResource() {
            return actionResource;
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
