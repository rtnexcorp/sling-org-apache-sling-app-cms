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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.thumbnails.OutputFileFormat;
import org.apache.sling.thumbnails.Transformation;

/**
 * Service for smart rendition generation with caching.
 *
 * <p>This service generates renditions on-demand and caches them for subsequent requests. It
 * provides cache invalidation and purging capabilities.
 */
public interface SmartRenditionService {

    /**
     * Get or generate a rendition with caching.
     *
     * <p>This method will first check the cache for an existing rendition. If found, it returns the
     * cached version. Otherwise, it generates the rendition, caches it, and returns it.
     *
     * @param asset the source asset resource
     * @param transformation the transformation to apply
     * @param format the output format
     * @return the rendition data
     * @throws RenditionException if rendition generation fails
     */
    InputStream getRendition(Resource asset, Transformation transformation, OutputFileFormat format)
            throws RenditionException;

    /**
     * Invalidate all renditions for a specific asset.
     *
     * <p>Removes all cached renditions for the given asset. Next request will regenerate them.
     *
     * @param asset the source asset resource
     */
    void invalidateRenditions(Resource asset);

    /**
     * Purge all renditions from the cache.
     *
     * <p>Removes all cached renditions for all assets. Use with caution.
     */
    void purgeAllRenditions();

    /**
     * Get cache statistics.
     *
     * @return cache statistics (hits, misses, size, etc.)
     */
    CacheStatistics getStatistics();

    /**
     * Exception thrown when rendition generation fails.
     */
    class RenditionException extends Exception {
        private static final long serialVersionUID = 1L;

        public RenditionException(String message) {
            super(message);
        }

        public RenditionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
