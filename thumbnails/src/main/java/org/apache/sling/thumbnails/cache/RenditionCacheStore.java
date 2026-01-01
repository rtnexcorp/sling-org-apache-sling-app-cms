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
package org.apache.sling.thumbnails.cache;

import java.io.InputStream;
import java.util.Optional;

/**
 * Interface for storing and retrieving cached renditions.
 *
 * <p>Implementations can store renditions in JCR, filesystem, or other storage backends.
 */
public interface RenditionCacheStore {

    /**
     * Retrieve a cached rendition.
     *
     * @param cacheKey the cache key
     * @return the rendition data if found
     */
    Optional<InputStream> get(String cacheKey);

    /**
     * Store a rendition in the cache.
     *
     * @param cacheKey the cache key
     * @param data the rendition data
     * @param assetPath the source asset path
     */
    void put(String cacheKey, InputStream data, String assetPath);

    /**
     * Invalidate (remove) all renditions for a specific asset.
     *
     * @param assetPath the source asset path
     */
    void invalidate(String assetPath);

    /**
     * Purge all renditions from the cache.
     */
    void purgeAll();

    /**
     * Remove expired renditions based on age.
     *
     * @param maxAgeHours maximum age in hours
     */
    void removeExpired(int maxAgeHours);

    /**
     * Enforce cache size limit by removing oldest entries.
     *
     * @param maxSizeMB maximum cache size in megabytes
     */
    void enforceSizeLimit(int maxSizeMB);

    /**
     * Get the number of cached entries.
     *
     * @return number of entries
     */
    long getEntryCount();

    /**
     * Get the total cache size in bytes.
     *
     * @return size in bytes
     */
    long getSizeBytes();
}
