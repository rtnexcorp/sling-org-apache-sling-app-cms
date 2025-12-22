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

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.CollectionService;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.jetbrains.annotations.NotNull;

/**
 * Sling Model for collection management UI
 */
@Model(adaptables = Resource.class)
public class CollectionModel {

    @SlingObject
    private Resource resource;

    @SlingObject
    private ResourceResolver resolver;

    @OSGiService
    private CollectionService collectionService;

    /**
     * Get the collection resource
     */
    public Resource getCollection() {
        return resource;
    }

    /**
     * Get assets in this collection
     */
    @NotNull
    public List<Resource> getAssets() {
        return collectionService.getCollectionAssets(resource);
    }

    /**
     * Get saved searches under /content/dam
     */
    @NotNull
    public List<Resource> getSavedSearches() {
        return collectionService.getSavedSearches("/content/dam");
    }

    /**
     * Get collections for this asset (if resource is an asset)
     */
    @NotNull
    public List<Resource> getCollectionsForAsset() {
        return collectionService.getCollectionsForAsset(resource);
    }

    /**
     * Check if this asset is in any collections
     */
    public boolean isInCollections() {
        return !collectionService.getCollectionsForAsset(resource).isEmpty();
    }

    /**
     * Get saved search count
     */
    public int getSavedSearchCount() {
        return collectionService.getSavedSearches("/content/dam").size();
    }
}
