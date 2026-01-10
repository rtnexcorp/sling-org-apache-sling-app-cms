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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.core.beans.AssetItem;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the Asset Grid component.
 * Provides pagination, filtering, and asset item management for the asset grid view.
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
}
