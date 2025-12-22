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
package org.apache.sling.thumbnails.internal.governance;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.cms.AssetGovernanceService;
import org.apache.sling.cms.PublishableResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of AssetGovernanceService for managing asset licensing, expiry, and usage policies.
 */
@Component(service = AssetGovernanceService.class)
public class AssetGovernanceServiceImpl implements AssetGovernanceService {

    private static final Logger log = LoggerFactory.getLogger(AssetGovernanceServiceImpl.class);

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public boolean isExpired(@NotNull Resource asset) {
        Date expiryDate = getExpiryDate(asset);
        if (expiryDate == null) {
            return false;
        }
        return expiryDate.before(new Date());
    }

    @Override
    @Nullable
    public Date getExpiryDate(@NotNull Resource asset) {
        Resource content = asset.getChild("jcr:content");
        if (content != null) {
            return content.getValueMap().get("expiryDate", Date.class);
        }
        return null;
    }

    @Override
    public void setExpiryDate(@NotNull Resource asset, @Nullable Date expiryDate) throws Exception {
        Resource content = asset.getChild("jcr:content");
        if (content == null) {
            throw new IllegalArgumentException("Asset does not have jcr:content node: " + asset.getPath());
        }

        ModifiableValueMap properties = content.adaptTo(ModifiableValueMap.class);
        if (properties == null) {
            throw new IllegalStateException("Cannot modify asset properties: " + asset.getPath());
        }

        if (expiryDate != null) {
            properties.put("expiryDate", expiryDate);
        } else {
            properties.remove("expiryDate");
        }

        // Commit the changes to persist them
        asset.getResourceResolver().commit();
        log.info("Set expiry date for asset {} to {}", asset.getPath(), expiryDate);
    }

    @Override
    public boolean hasValidLicense(@NotNull Resource asset) {
        String license = getLicense(asset);
        // For now, any non-null license is considered valid
        // This could be extended to validate against a license registry
        return license != null && !license.trim().isEmpty();
    }

    @Override
    @Nullable
    public String getLicense(@NotNull Resource asset) {
        Resource content = asset.getChild("jcr:content");
        if (content != null) {
            return content.getValueMap().get("license", String.class);
        }
        return null;
    }

    @Override
    public void setLicense(@NotNull Resource asset, @Nullable String license) throws Exception {
        Resource content = asset.getChild("jcr:content");
        if (content == null) {
            throw new IllegalArgumentException("Asset does not have jcr:content node: " + asset.getPath());
        }

        ModifiableValueMap properties = content.adaptTo(ModifiableValueMap.class);
        if (properties == null) {
            throw new IllegalStateException("Cannot modify asset properties: " + asset.getPath());
        }

        if (license != null && !license.trim().isEmpty()) {
            properties.put("license", license.trim());
        } else {
            properties.remove("license");
        }

        // Commit the changes to persist them
        asset.getResourceResolver().commit();
        log.info("Set license for asset {} to {}", asset.getPath(), license);
    }

    @Override
    @NotNull
    public List<String> getExpiredAssets() {
        List<String> expiredAssets = new ArrayList<>();

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-governance");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            // Query for assets with expiryDate before current timestamp
            String query = "SELECT asset.[jcr:path] FROM [nt:base] AS asset "
                    + "INNER JOIN [nt:unstructured] AS content ON ISCHILDNODE(content, asset) "
                    + "WHERE content.[expiryDate] IS NOT NULL "
                    + "AND CAST(content.[expiryDate] AS DATE) < CAST('"
                    + new java.sql.Timestamp(System.currentTimeMillis())
                    + "' AS DATE) " + "AND asset.[jcr:primaryType] = 'sling:Asset'";

            Iterator<Resource> results = resolver.findResources(query, "JCR-SQL2");
            while (results.hasNext()) {
                Resource assetResource = results.next();
                expiredAssets.add(assetResource.getPath());
            }

            log.debug("Found {} expired assets", expiredAssets.size());

        } catch (Exception e) {
            log.error("Error querying for expired assets", e);
        }

        return expiredAssets;
    }

    @Override
    @NotNull
    public List<String> getAssetsExpiringWithin(int days) {
        List<String> expiringAssets = new ArrayList<>();

        if (days <= 0) {
            return expiringAssets;
        }

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-governance");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            // Calculate the cutoff date (current time + days)
            long cutoffTime = System.currentTimeMillis() + (days * 24L * 60L * 60L * 1000L);
            java.sql.Timestamp cutoffTimestamp = new java.sql.Timestamp(cutoffTime);

            // Query for assets with expiryDate within the specified days
            String query = "SELECT asset.[jcr:path] FROM [nt:base] AS asset "
                    + "INNER JOIN [nt:unstructured] AS content ON ISCHILDNODE(content, asset) "
                    + "WHERE content.[expiryDate] IS NOT NULL "
                    + "AND CAST(content.[expiryDate] AS DATE) >= CAST('"
                    + new java.sql.Timestamp(System.currentTimeMillis())
                    + "' AS DATE) " + "AND CAST(content.[expiryDate] AS DATE) <= CAST('"
                    + cutoffTimestamp + "' AS DATE) " + "AND asset.[jcr:primaryType] = 'sling:Asset'";

            Iterator<Resource> results = resolver.findResources(query, "JCR-SQL2");
            while (results.hasNext()) {
                Resource assetResource = results.next();
                expiringAssets.add(assetResource.getPath());
            }

            log.debug("Found {} assets expiring within {} days", expiringAssets.size(), days);

        } catch (Exception e) {
            log.error("Error querying for assets expiring within {} days", days, e);
        }

        return expiringAssets;
    }

    @Override
    @NotNull
    public List<Resource> findReferences(@NotNull Resource asset) {
        List<Resource> references = new ArrayList<>();
        String assetPath = asset.getPath();

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-governance");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            // Search for common property names that might reference assets
            String[] referencePropertyPatterns = {
                "imagePath", "assetPath", "fileReference", "imageReference",
                "backgroundImage", "logoPath", "iconPath", "thumbnailPath",
                "attachmentPath", "documentPath", "mediaPath"
            };

            for (String propertyName : referencePropertyPatterns) {
                try {
                    // Query for nodes that have this property containing the asset path
                    String query =
                            "SELECT [jcr:path] FROM [nt:base] WHERE [" + propertyName + "] = '" + assetPath + "'";

                    Iterator<Resource> results = resolver.findResources(query, "JCR-SQL2");
                    while (results.hasNext()) {
                        Resource referencingResource = results.next();
                        if (!references.contains(referencingResource)) {
                            references.add(referencingResource);
                        }
                    }
                } catch (Exception e) {
                    // Continue with next property pattern if this one fails
                    log.debug("Error searching for references with property {}: {}", propertyName, e.getMessage());
                }
            }

            // Also search for sling:resourceType = 'sling-cms/components' nodes that might reference assets
            try {
                String componentQuery =
                        "SELECT [jcr:path] FROM [nt:base] WHERE [sling:resourceType] LIKE 'sling-cms/components/%'";

                Iterator<Resource> componentResults = resolver.findResources(componentQuery, "JCR-SQL2");
                while (componentResults.hasNext()) {
                    Resource componentResource = componentResults.next();
                    // Check if any property value contains the asset path
                    for (String key : componentResource.getValueMap().keySet()) {
                        Object value = componentResource.getValueMap().get(key);
                        if (value instanceof String && ((String) value).contains(assetPath)) {
                            if (!references.contains(componentResource)) {
                                references.add(componentResource);
                            }
                            break; // Found a reference, no need to check other properties
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Error searching component references: {}", e.getMessage());
            }

            log.debug("Found {} references to asset {}", references.size(), assetPath);

        } catch (Exception e) {
            log.error("Error finding references for asset {}", assetPath, e);
        }

        return references;
    }

    @Override
    public void unpublishExpiredAsset(@NotNull Resource asset) throws Exception {
        if (!isExpired(asset)) {
            log.info("Asset {} is not expired, skipping unpublish", asset.getPath());
            return;
        }

        PublishableResource publishable = asset.adaptTo(PublishableResource.class);
        if (publishable != null && publishable.isPublished()) {
            // Set published to false
            Resource content = asset.getChild("jcr:content");
            if (content != null) {
                ModifiableValueMap properties = content.adaptTo(ModifiableValueMap.class);
                if (properties != null) {
                    properties.put("published", false);
                    // Commit the changes to persist them
                    asset.getResourceResolver().commit();
                    log.info("Unpublished expired asset: {}", asset.getPath());
                }
            }
        } else {
            log.debug("Asset {} is not published or not publishable", asset.getPath());
        }
    }
}
