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
package org.apache.sling.cms;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service for managing asset collections and saved searches.
 */
@ProviderType
public interface CollectionService {

    /**
     * Create a new collection.
     *
     * @param parent the parent resource where the collection should be created
     * @param name the name of the collection
     * @param title the display title of the collection
     * @param description the description of the collection
     * @return the created collection resource
     * @throws Exception if the collection cannot be created
     */
    @NotNull
    Resource createCollection(
            @NotNull Resource parent, @NotNull String name, @NotNull String title, @Nullable String description)
            throws Exception;

    /**
     * Get a collection by path.
     *
     * @param collectionPath the path to the collection
     * @return the collection resource, or null if not found
     */
    @Nullable
    Resource getCollection(@NotNull String collectionPath);

    /**
     * Add an asset to a collection.
     *
     * @param collection the collection resource
     * @param asset the asset resource to add
     * @throws Exception if the asset cannot be added
     */
    void addAssetToCollection(@NotNull Resource collection, @NotNull Resource asset) throws Exception;

    /**
     * Remove an asset from a collection.
     *
     * @param collection the collection resource
     * @param asset the asset resource to remove
     * @throws Exception if the asset cannot be removed
     */
    void removeAssetFromCollection(@NotNull Resource collection, @NotNull Resource asset) throws Exception;

    /**
     * Get all assets in a collection.
     *
     * @param collection the collection resource
     * @return list of asset resources in the collection
     */
    @NotNull
    List<Resource> getCollectionAssets(@NotNull Resource collection);

    /**
     * Check if an asset is in a collection.
     *
     * @param collection the collection resource
     * @param asset the asset resource
     * @return true if the asset is in the collection
     */
    boolean isAssetInCollection(@NotNull Resource collection, @NotNull Resource asset);

    /**
     * Get all collections that contain a specific asset.
     *
     * @param asset the asset resource
     * @return list of collection resources containing the asset
     */
    @NotNull
    List<Resource> getCollectionsForAsset(@NotNull Resource asset);

    /**
     * Delete a collection.
     *
     * @param collection the collection resource to delete
     * @throws Exception if the collection cannot be deleted
     */
    void deleteCollection(@NotNull Resource collection) throws Exception;

    /**
     * Create a saved search.
     *
     * @param parent the parent resource where the saved search should be created
     * @param name the name of the saved search
     * @param title the display title of the saved search
     * @param query the search query to save
     * @param description the description of the saved search
     * @return the created saved search resource
     * @throws Exception if the saved search cannot be created
     */
    @NotNull
    Resource createSavedSearch(
            @NotNull Resource parent,
            @NotNull String name,
            @NotNull String title,
            @NotNull String query,
            @Nullable String description)
            throws Exception;

    /**
     * Get a saved search by path.
     *
     * @param savedSearchPath the path to the saved search
     * @return the saved search resource, or null if not found
     */
    @Nullable
    Resource getSavedSearch(@NotNull String savedSearchPath);

    /**
     * Execute a saved search and return the results.
     *
     * @param savedSearch the saved search resource
     * @return list of asset resources matching the saved search
     */
    @NotNull
    List<Resource> executeSavedSearch(@NotNull Resource savedSearch);

    /**
     * Get all saved searches under a parent path.
     *
     * @param parentPath the parent path to search under
     * @return list of saved search resources
     */
    @NotNull
    List<Resource> getSavedSearches(@NotNull String parentPath);

    /**
     * Delete a saved search.
     *
     * @param savedSearch the saved search resource to delete
     * @throws Exception if the saved search cannot be deleted
     */
    void deleteSavedSearch(@NotNull Resource savedSearch) throws Exception;
}
