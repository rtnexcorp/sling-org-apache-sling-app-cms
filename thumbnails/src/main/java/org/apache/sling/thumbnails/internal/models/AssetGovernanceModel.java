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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.AssetGovernanceService;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.jetbrains.annotations.NotNull;

/**
 * Sling Model for asset governance management UI
 */
@Model(adaptables = Resource.class)
public class AssetGovernanceModel {

    @SlingObject
    private Resource resource;

    @SlingObject
    private ResourceResolver resolver;

    @OSGiService
    private AssetGovernanceService governanceService;

    /**
     * Get the asset resource
     */
    public Resource getAsset() {
        return resource;
    }

    /**
     * Check if the asset has expired
     */
    public boolean isExpired() {
        return governanceService.isExpired(resource);
    }

    /**
     * Check if the asset has valid licensing
     */
    public boolean hasValidLicense() {
        return governanceService.hasValidLicense(resource);
    }

    /**
     * Get the expiry date
     */
    public Date getExpiryDate() {
        return governanceService.getExpiryDate(resource);
    }

    /**
     * Get the license information
     */
    public String getLicense() {
        return governanceService.getLicense(resource);
    }

    /**
     * Get assets that reference this asset
     */
    @NotNull
    public List<Resource> getReferences() {
        return governanceService.findReferences(resource);
    }

    /**
     * Get expired assets count (for admin overview)
     */
    public int getExpiredAssetsCount() {
        return governanceService.getExpiredAssets().size();
    }

    /**
     * Get assets expiring soon (within 30 days)
     */
    @NotNull
    public List<Resource> getAssetsExpiringSoon() {
        List<Resource> expiringSoon = new ArrayList<>();
        try {
            List<String> expiringPaths = governanceService.getAssetsExpiringWithin(30);
            for (String path : expiringPaths) {
                Resource asset = resolver.getResource(path);
                if (asset != null) {
                    expiringSoon.add(asset);
                }
            }
        } catch (Exception e) {
            // Ignore errors in model
        }
        return expiringSoon;
    }
}
