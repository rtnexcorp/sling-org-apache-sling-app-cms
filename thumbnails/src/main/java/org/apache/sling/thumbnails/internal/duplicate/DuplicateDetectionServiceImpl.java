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
package org.apache.sling.thumbnails.internal.duplicate;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.cms.DuplicateDetectionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of DuplicateDetectionService for hash-based duplicate detection.
 */
@Component(service = DuplicateDetectionService.class)
public class DuplicateDetectionServiceImpl implements DuplicateDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DuplicateDetectionServiceImpl.class);

    private static final String HASH_PROPERTY = "assetHash";
    private static final String ALGORITHM = "SHA-256";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    @Nullable
    public String calculateAndStoreHash(@NotNull Resource asset) throws Exception {
        String hash = calculateHash(asset);
        if (hash != null) {
            storeHash(asset, hash);
        }
        return hash;
    }

    @Override
    @Nullable
    public String getStoredHash(@NotNull Resource asset) {
        Resource content = asset.getChild("jcr:content");
        if (content != null) {
            return content.getValueMap().get(HASH_PROPERTY, String.class);
        }
        return null;
    }

    @Override
    @NotNull
    public List<Resource> findDuplicates(@NotNull Resource asset) {
        List<Resource> duplicates = new ArrayList<>();
        String hash = getStoredHash(asset);

        if (hash == null) {
            // Try to calculate hash if not stored
            try {
                hash = calculateAndStoreHash(asset);
            } catch (Exception e) {
                log.error("Failed to calculate hash for asset {}", asset.getPath(), e);
                return duplicates;
            }
        }

        if (hash == null) {
            return duplicates;
        }

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-duplicate-detection");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            // Query for assets with the same hash
            String query = "SELECT asset.[jcr:path] FROM [nt:base] AS asset "
                    + "INNER JOIN [nt:unstructured] AS content ON ISCHILDNODE(content, asset) "
                    + "WHERE content.[" + HASH_PROPERTY + "] = '" + hash + "' "
                    + "AND asset.[jcr:primaryType] = 'sling:Asset' "
                    + "AND asset.[jcr:path] <> '" + asset.getPath() + "'";

            Iterator<Resource> results = resolver.findResources(query, "JCR-SQL2");
            while (results.hasNext()) {
                Resource duplicateAsset = results.next();
                duplicates.add(duplicateAsset);
            }

            log.debug("Found {} duplicates for asset {}", duplicates.size(), asset.getPath());

        } catch (Exception e) {
            log.error("Error finding duplicates for asset {}", asset.getPath(), e);
        }

        return duplicates;
    }

    @Override
    @NotNull
    public List<List<Resource>> findAllDuplicateGroups() {
        List<List<Resource>> duplicateGroups = new ArrayList<>();
        Map<String, List<Resource>> hashGroups = new HashMap<>();

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-duplicate-detection");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            // Query for all assets that have hashes
            String query = "SELECT asset.[jcr:path] FROM [nt:base] AS asset "
                    + "INNER JOIN [nt:unstructured] AS content ON ISCHILDNODE(content, asset) "
                    + "WHERE content.[" + HASH_PROPERTY + "] IS NOT NULL "
                    + "AND asset.[jcr:primaryType] = 'sling:Asset'";

            Iterator<Resource> results = resolver.findResources(query, "JCR-SQL2");
            while (results.hasNext()) {
                Resource asset = results.next();
                String hash = getStoredHash(asset);
                if (hash != null) {
                    hashGroups.computeIfAbsent(hash, k -> new ArrayList<>()).add(asset);
                }
            }

            // Filter to only groups with more than one asset
            for (List<Resource> group : hashGroups.values()) {
                if (group.size() > 1) {
                    duplicateGroups.add(group);
                }
            }

            log.debug("Found {} duplicate groups", duplicateGroups.size());

        } catch (Exception e) {
            log.error("Error finding all duplicate groups", e);
        }

        return duplicateGroups;
    }

    @Override
    public boolean hasDuplicates(@NotNull Resource asset) {
        return !findDuplicates(asset).isEmpty();
    }

    @Override
    public int getDuplicateCount(@NotNull Resource asset) {
        return findDuplicates(asset).size();
    }

    @Override
    public void removeHash(@NotNull Resource asset) throws Exception {
        Resource content = asset.getChild("jcr:content");
        if (content != null) {
            ModifiableValueMap properties = content.adaptTo(ModifiableValueMap.class);
            if (properties != null) {
                properties.remove(HASH_PROPERTY);
                asset.getResourceResolver().commit();
                log.info("Removed hash for asset {}", asset.getPath());
            }
        }
    }

    @Override
    public int recalculateHashes(@NotNull String rootPath) throws Exception {
        int processed = 0;

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-duplicate-detection");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            // Query for all assets under the root path
            String query = "SELECT asset.[jcr:path] FROM [nt:base] AS asset "
                    + "WHERE ISDESCENDANTNODE(asset, '" + rootPath + "') "
                    + "AND asset.[jcr:primaryType] = 'sling:Asset'";

            Iterator<Resource> results = resolver.findResources(query, "JCR-SQL2");
            while (results.hasNext()) {
                Resource asset = results.next();
                try {
                    calculateAndStoreHash(asset);
                    processed++;
                } catch (Exception e) {
                    log.warn("Failed to recalculate hash for asset {}", asset.getPath(), e);
                }
            }

            log.info("Recalculated hashes for {} assets under {}", processed, rootPath);

        } catch (Exception e) {
            log.error("Error recalculating hashes under {}", rootPath, e);
            throw e;
        }

        return processed;
    }

    /**
     * Calculate SHA-256 hash of the asset's binary content.
     */
    private String calculateHash(Resource asset) {
        try {
            // Get the asset's binary content
            Resource content = asset.getChild("jcr:content");
            if (content == null) {
                log.warn("Asset {} has no jcr:content node", asset.getPath());
                return null;
            }

            // Try to get the binary data from various possible locations
            InputStream inputStream = null;

            // First try jcr:data property (standard for nt:file)
            if (content.getValueMap().get("jcr:data") != null) {
                inputStream = content.getValueMap().get("jcr:data", InputStream.class);
            }

            // If not found, try to find binary data in renditions or original
            if (inputStream == null) {
                Resource original = content.getChild("jcr:content");
                if (original != null && original.getValueMap().get("jcr:data") != null) {
                    inputStream = original.getValueMap().get("jcr:data", InputStream.class);
                }
            }

            if (inputStream == null) {
                log.warn("Could not find binary content for asset {}", asset.getPath());
                return null;
            }

            // Calculate SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            inputStream.close();
            String hash = Hex.encodeHexString(digest.digest());

            log.debug("Calculated hash {} for asset {}", hash, asset.getPath());
            return hash;

        } catch (Exception e) {
            log.error("Error calculating hash for asset {}", asset.getPath(), e);
            return null;
        }
    }

    /**
     * Store the calculated hash in the asset's metadata.
     */
    private void storeHash(Resource asset, String hash) throws Exception {
        Resource content = asset.getChild("jcr:content");
        if (content == null) {
            throw new IllegalArgumentException("Asset does not have jcr:content node: " + asset.getPath());
        }

        ModifiableValueMap properties = content.adaptTo(ModifiableValueMap.class);
        if (properties == null) {
            throw new IllegalStateException("Cannot modify asset properties: " + asset.getPath());
        }

        properties.put(HASH_PROPERTY, hash);
        asset.getResourceResolver().commit();

        log.debug("Stored hash {} for asset {}", hash, asset.getPath());
    }
}
