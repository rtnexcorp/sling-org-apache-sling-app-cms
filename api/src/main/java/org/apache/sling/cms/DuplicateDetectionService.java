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
 * Service for detecting duplicate assets using hash-based comparison.
 */
@ProviderType
public interface DuplicateDetectionService {

    /**
     * Calculate and store the hash for an asset.
     *
     * @param asset the asset resource
     * @return the calculated hash, or null if calculation failed
     * @throws Exception if the hash cannot be calculated or stored
     */
    @Nullable
    String calculateAndStoreHash(@NotNull Resource asset) throws Exception;

    /**
     * Get the stored hash for an asset.
     *
     * @param asset the asset resource
     * @return the stored hash, or null if not calculated
     */
    @Nullable
    String getStoredHash(@NotNull Resource asset);

    /**
     * Find duplicate assets for the given asset based on hash comparison.
     *
     * @param asset the asset resource to find duplicates for
     * @return list of duplicate asset resources (excluding the original)
     */
    @NotNull
    List<Resource> findDuplicates(@NotNull Resource asset);

    /**
     * Find all duplicate groups in the system.
     *
     * @return list of duplicate groups, where each group is a list of duplicate assets
     */
    @NotNull
    List<List<Resource>> findAllDuplicateGroups();

    /**
     * Check if an asset has duplicates.
     *
     * @param asset the asset resource
     * @return true if the asset has duplicates
     */
    boolean hasDuplicates(@NotNull Resource asset);

    /**
     * Get the number of duplicates for an asset.
     *
     * @param asset the asset resource
     * @return the number of duplicate assets
     */
    int getDuplicateCount(@NotNull Resource asset);

    /**
     * Remove the stored hash for an asset (useful when asset content changes).
     *
     * @param asset the asset resource
     * @throws Exception if the hash cannot be removed
     */
    void removeHash(@NotNull Resource asset) throws Exception;

    /**
     * Recalculate hashes for all assets in a given path.
     *
     * @param rootPath the root path to scan for assets
     * @return the number of assets processed
     * @throws Exception if the batch operation fails
     */
    int recalculateHashes(@NotNull String rootPath) throws Exception;
}
