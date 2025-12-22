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
package org.apache.sling.thumbnails.internal.collections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.cms.CollectionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of CollectionService for managing asset collections and saved searches.
 */
@Component(service = CollectionService.class)
public class CollectionServiceImpl implements CollectionService {

    private static final Logger log = LoggerFactory.getLogger(CollectionServiceImpl.class);

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    @NotNull
    public Resource createCollection(
            @NotNull Resource parent, @NotNull String name, @NotNull String title, @Nullable String description)
            throws Exception {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-collections");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            // Create the collection node
            Map<String, Object> properties = new HashMap<>();
            properties.put("jcr:primaryType", "sling:Collection");
            properties.put("jcr:title", title);
            if (description != null) {
                properties.put("jcr:description", description);
            }
            properties.put("collectionType", "asset");

            String collectionPath = parent.getPath() + "/" + name;
            Resource collectionResource = resolver.create(parent, name, properties);

            // Create assets subnode to hold collection members
            Map<String, Object> assetsProperties = new HashMap<>();
            assetsProperties.put("jcr:primaryType", "sling:Folder");
            resolver.create(collectionResource, "assets", assetsProperties);

            resolver.commit();
            log.info("Created collection: {}", collectionPath);
            return collectionResource;
        }
    }

    @Override
    @Nullable
    public Resource getCollection(@NotNull String collectionPath) {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-collections");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            return resolver.getResource(collectionPath);
        } catch (Exception e) {
            log.error("Error getting collection: {}", collectionPath, e);
            return null;
        }
    }

    @Override
    public void addAssetToCollection(@NotNull Resource collection, @NotNull Resource asset) throws Exception {
        Resource assetsFolder = collection.getChild("assets");
        if (assetsFolder == null) {
            throw new IllegalStateException("Collection does not have assets folder: " + collection.getPath());
        }

        // Create a reference to the asset in the collection
        Map<String, Object> properties = new HashMap<>();
        properties.put("jcr:primaryType", "sling:AssetReference");
        properties.put("assetPath", asset.getPath());

        String assetName = asset.getName();
        // Ensure unique name in case of duplicates
        String referenceName = assetName;
        int counter = 1;
        while (assetsFolder.getChild(referenceName) != null) {
            referenceName = assetName + "_" + counter;
            counter++;
        }

        collection.getResourceResolver().create(assetsFolder, referenceName, properties);
        collection.getResourceResolver().commit();

        log.info("Added asset {} to collection {}", asset.getPath(), collection.getPath());
    }

    @Override
    public void removeAssetFromCollection(@NotNull Resource collection, @NotNull Resource asset) throws Exception {
        Resource assetsFolder = collection.getChild("assets");
        if (assetsFolder == null) {
            return; // Nothing to remove
        }

        // Find and remove the asset reference
        Iterator<Resource> children = assetsFolder.listChildren();
        while (children.hasNext()) {
            Resource child = children.next();
            String assetPath = child.getValueMap().get("assetPath", String.class);
            if (asset.getPath().equals(assetPath)) {
                collection.getResourceResolver().delete(child);
                collection.getResourceResolver().commit();
                log.info("Removed asset {} from collection {}", asset.getPath(), collection.getPath());
                return;
            }
        }
    }

    @Override
    @NotNull
    public List<Resource> getCollectionAssets(@NotNull Resource collection) {
        List<Resource> assets = new ArrayList<>();
        Resource assetsFolder = collection.getChild("assets");
        if (assetsFolder == null) {
            return assets;
        }

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-collections");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            Iterator<Resource> children = assetsFolder.listChildren();
            while (children.hasNext()) {
                Resource child = children.next();
                String assetPath = child.getValueMap().get("assetPath", String.class);
                if (assetPath != null) {
                    Resource assetResource = resolver.getResource(assetPath);
                    if (assetResource != null) {
                        assets.add(assetResource);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting collection assets for {}", collection.getPath(), e);
        }

        return assets;
    }

    @Override
    public boolean isAssetInCollection(@NotNull Resource collection, @NotNull Resource asset) {
        Resource assetsFolder = collection.getChild("assets");
        if (assetsFolder == null) {
            return false;
        }

        Iterator<Resource> children = assetsFolder.listChildren();
        while (children.hasNext()) {
            Resource child = children.next();
            String assetPath = child.getValueMap().get("assetPath", String.class);
            if (asset.getPath().equals(assetPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @NotNull
    public List<Resource> getCollectionsForAsset(@NotNull Resource asset) {
        List<Resource> collections = new ArrayList<>();

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-collections");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            // Query for collections that contain this asset
            String query = "SELECT collection.[jcr:path] FROM [sling:Collection] AS collection "
                    + "INNER JOIN [sling:AssetReference] AS ref ON ISDESCENDANTNODE(ref, collection) "
                    + "WHERE ref.[assetPath] = '"
                    + asset.getPath() + "'";

            Iterator<Resource> results = resolver.findResources(query, "JCR-SQL2");
            while (results.hasNext()) {
                Resource collectionResource = results.next();
                collections.add(collectionResource);
            }
        } catch (Exception e) {
            log.error("Error finding collections for asset {}", asset.getPath(), e);
        }

        return collections;
    }

    @Override
    public void deleteCollection(@NotNull Resource collection) throws Exception {
        collection.getResourceResolver().delete(collection);
        collection.getResourceResolver().commit();
        log.info("Deleted collection: {}", collection.getPath());
    }

    @Override
    @NotNull
    public Resource createSavedSearch(
            @NotNull Resource parent,
            @NotNull String name,
            @NotNull String title,
            @NotNull String query,
            @Nullable String description)
            throws Exception {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-collections");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            // Create the saved search node
            Map<String, Object> properties = new HashMap<>();
            properties.put("jcr:primaryType", "sling:SavedSearch");
            properties.put("jcr:title", title);
            properties.put("searchQuery", query);
            if (description != null) {
                properties.put("jcr:description", description);
            }

            String savedSearchPath = parent.getPath() + "/" + name;
            Resource savedSearchResource = resolver.create(parent, name, properties);

            resolver.commit();
            log.info("Created saved search: {}", savedSearchPath);
            return savedSearchResource;
        }
    }

    @Override
    @Nullable
    public Resource getSavedSearch(@NotNull String savedSearchPath) {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-collections");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            return resolver.getResource(savedSearchPath);
        } catch (Exception e) {
            log.error("Error getting saved search: {}", savedSearchPath, e);
            return null;
        }
    }

    @Override
    @NotNull
    public List<Resource> executeSavedSearch(@NotNull Resource savedSearch) {
        List<Resource> results = new ArrayList<>();
        String query = savedSearch.getValueMap().get("searchQuery", String.class);

        if (query == null || query.trim().isEmpty()) {
            log.warn("Saved search {} has no query", savedSearch.getPath());
            return results;
        }

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-collections");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            Iterator<Resource> searchResults = resolver.findResources(query, "JCR-SQL2");
            while (searchResults.hasNext()) {
                results.add(searchResults.next());
            }
        } catch (Exception e) {
            log.error("Error executing saved search {}", savedSearch.getPath(), e);
        }

        return results;
    }

    @Override
    @NotNull
    public List<Resource> getSavedSearches(@NotNull String parentPath) {
        List<Resource> savedSearches = new ArrayList<>();

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-collections");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            Resource parent = resolver.getResource(parentPath);
            if (parent != null) {
                Iterator<Resource> children = parent.listChildren();
                while (children.hasNext()) {
                    Resource child = children.next();
                    if ("sling:SavedSearch".equals(child.getValueMap().get("jcr:primaryType", String.class))) {
                        savedSearches.add(child);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting saved searches under {}", parentPath, e);
        }

        return savedSearches;
    }

    @Override
    public void deleteSavedSearch(@NotNull Resource savedSearch) throws Exception {
        savedSearch.getResourceResolver().delete(savedSearch);
        savedSearch.getResourceResolver().commit();
        log.info("Deleted saved search: {}", savedSearch.getPath());
    }
}
