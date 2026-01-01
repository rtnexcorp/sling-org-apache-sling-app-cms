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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduled job to cleanup expired renditions from the cache.
 *
 * <p>Runs daily at 2 AM to remove expired renditions and enforce size limits.
 */
@Component(
        service = Runnable.class,
        property = {
            "scheduler.expression=0 0 2 * * ?", // Daily at 2 AM
            "scheduler.concurrent=false"
        })
@Designate(ocd = RenditionCacheConfig.class)
public class RenditionCacheCleanup implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(RenditionCacheCleanup.class);

    @Reference
    private RenditionCacheStore cacheStore;

    private RenditionCacheConfig config;

    @Activate
    protected void activate(RenditionCacheConfig config) {
        this.config = config;
        log.info(
                "RenditionCacheCleanup activated with maxAge={}h, maxSize={}MB",
                config.maxAgeHours(),
                config.maxCacheSizeMB());
    }

    @Override
    public void run() {
        log.info("Starting rendition cache cleanup...");

        try {
            // Remove expired entries
            if (config.maxAgeHours() > 0) {
                log.debug("Removing expired renditions (older than {} hours)", config.maxAgeHours());
                cacheStore.removeExpired(config.maxAgeHours());
            }

            // Enforce size limit
            if (config.maxCacheSizeMB() > 0) {
                log.debug("Enforcing cache size limit ({} MB)", config.maxCacheSizeMB());
                cacheStore.enforceSizeLimit(config.maxCacheSizeMB());
            }

            log.info(
                    "Rendition cache cleanup completed. Entries: {}, Size: {} MB",
                    cacheStore.getEntryCount(),
                    cacheStore.getSizeBytes() / (1024 * 1024));
        } catch (Exception e) {
            log.error("Error during cache cleanup", e);
        }
    }
}
