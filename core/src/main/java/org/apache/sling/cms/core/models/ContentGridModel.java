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
import java.util.Iterator;
import java.util.List;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.core.beans.ContentItem;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the Content Grid component.
 * Provides pagination, filtering, and content item management for the content grid view.
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

    private List<ContentItem> items;
    private int currentPage;
    private boolean hasNextPage;
    private boolean hasPreviousPage;

    /**
     * Get the request suffix (path after .html extension)
     * @return the suffix or empty string if none
     */
    public String getRequestSuffix() {
        String suffix = request.getRequestPathInfo().getSuffix();
        return suffix != null ? suffix : "";
    }

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

    public List<ContentItem> getItems() {
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
                    items.add(new ContentItem(child, resource, resourceResolver));
                }

                hasNextPage = end < filteredChildren.size();
            }
        }
        return items;
    }
}
