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

import java.util.Date;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service for managing asset governance including licensing, expiry, and usage policies.
 */
@ProviderType
public interface AssetGovernanceService {

    /**
     * Check if an asset has expired and should be unpublished.
     *
     * @param asset the asset resource
     * @return true if the asset has expired
     */
    boolean isExpired(@NotNull Resource asset);

    /**
     * Get the expiry date of an asset.
     *
     * @param asset the asset resource
     * @return the expiry date, or null if not set
     */
    @Nullable
    Date getExpiryDate(@NotNull Resource asset);

    /**
     * Set the expiry date of an asset.
     *
     * @param asset the asset resource
     * @param expiryDate the expiry date
     * @throws Exception if the expiry date cannot be set
     */
    void setExpiryDate(@NotNull Resource asset, @Nullable Date expiryDate) throws Exception;

    /**
     * Check if an asset has valid licensing for use.
     *
     * @param asset the asset resource
     * @return true if the asset has valid licensing
     */
    boolean hasValidLicense(@NotNull Resource asset);

    /**
     * Get the license information for an asset.
     *
     * @param asset the asset resource
     * @return the license information, or null if not set
     */
    @Nullable
    String getLicense(@NotNull Resource asset);

    /**
     * Set the license information for an asset.
     *
     * @param asset the asset resource
     * @param license the license information
     * @throws Exception if the license cannot be set
     */
    void setLicense(@NotNull Resource asset, @Nullable String license) throws Exception;

    /**
     * Get all assets that are expired.
     *
     * @return list of expired asset paths
     */
    @NotNull
    List<String> getExpiredAssets();

    /**
     * Get all assets that expire within the specified number of days.
     *
     * @param days the number of days
     * @return list of asset paths expiring soon
     */
    @NotNull
    List<String> getAssetsExpiringWithin(int days);

    /**
     * Find all resources that reference the given asset.
     *
     * @param asset the asset resource
     * @return list of resources that reference the asset
     */
    @NotNull
    List<Resource> findReferences(@NotNull Resource asset);

    /**
     * Unpublish an expired asset.
     *
     * @param asset the asset resource
     * @throws Exception if the asset cannot be unpublished
     */
    void unpublishExpiredAsset(@NotNull Resource asset) throws Exception;
}
