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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.thumbnails.OutputFileFormat;
import org.apache.sling.thumbnails.Transformation;
import org.apache.sling.thumbnails.Transformer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of SmartRenditionService.
 *
 * <p>This service generates renditions on-demand and caches them using a configurable cache store.
 */
@Component(service = SmartRenditionService.class)
@Designate(ocd = RenditionCacheConfig.class)
public class SmartRenditionServiceImpl implements SmartRenditionService {

    private static final Logger log = LoggerFactory.getLogger(SmartRenditionServiceImpl.class);

    @Reference
    private RenditionCacheKeyGenerator cacheKeyGenerator;

    @Reference
    private RenditionCacheStore cacheStore;

    @Reference
    private Transformer transformer;

    private final CacheStatistics statistics = new CacheStatistics();

    private RenditionCacheConfig config;

    @Activate
    protected void activate(RenditionCacheConfig config) {
        this.config = config;
        log.info("SmartRenditionService activated with caching: {}", config.enabled());
    }

    @Override
    public InputStream getRendition(Resource asset, Transformation transformation, OutputFileFormat format)
            throws RenditionException {
        if (asset == null || transformation == null || format == null) {
            throw new RenditionException("asset, transformation, and format must not be null");
        }

        // If caching is disabled, just generate and return
        if (!config.enabled()) {
            return generateRendition(asset, transformation, format);
        }

        // Generate cache key
        String cacheKey = cacheKeyGenerator.generate(asset, transformation, format);
        log.debug("Cache key generated: {}", cacheKey);

        // Check cache
        Optional<InputStream> cached = cacheStore.get(cacheKey);
        if (cached.isPresent()) {
            if (config.enableStatistics()) {
                statistics.recordHit();
            }
            log.debug("Cache hit for: {}", cacheKey);
            return cached.get();
        }

        // Cache miss - generate rendition
        if (config.enableStatistics()) {
            statistics.recordMiss();
        }
        log.debug("Cache miss for: {}", cacheKey);

        InputStream rendition = generateRendition(asset, transformation, format);

        // Store in cache
        try {
            // We need to read the stream to get its size and store it
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = rendition.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] data = baos.toByteArray();

            cacheStore.put(cacheKey, new ByteArrayInputStream(data), asset.getPath());

            if (config.enableStatistics()) {
                statistics.recordEntry(data.length);
            }

            log.debug("Stored rendition in cache: {} ({} bytes)", cacheKey, data.length);

            // Return a new stream with the data
            return new ByteArrayInputStream(data);
        } catch (IOException e) {
            log.error("Error caching rendition: {}", cacheKey, e);
            throw new RenditionException("Error caching rendition", e);
        }
    }

    @Override
    public void invalidateRenditions(Resource asset) {
        if (asset == null) {
            log.warn("Cannot invalidate renditions for null asset");
            return;
        }

        cacheStore.invalidate(asset.getPath());
        log.info("Invalidated renditions for asset: {}", asset.getPath());
    }

    @Override
    public void purgeAllRenditions() {
        cacheStore.purgeAll();
        if (config.enableStatistics()) {
            statistics.reset();
        }
        log.info("Purged all renditions from cache");
    }

    @Override
    public CacheStatistics getStatistics() {
        if (!config.enableStatistics()) {
            log.warn("Statistics are disabled in configuration");
            return new CacheStatistics(); // Return empty statistics
        }
        return statistics;
    }

    /**
     * Generate a rendition without caching.
     *
     * @param asset the source asset
     * @param transformation the transformation to apply
     * @param format the output format
     * @return the rendition data
     * @throws RenditionException if generation fails
     */
    private InputStream generateRendition(Resource asset, Transformation transformation, OutputFileFormat format)
            throws RenditionException {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            transformer.transform(asset, transformation, format, output);
            log.debug("Generated rendition for: {} with transformation: {}", asset.getPath(), transformation.getName());
            return new ByteArrayInputStream(output.toByteArray());
        } catch (IOException e) {
            log.error("Error generating rendition for: {}", asset.getPath(), e);
            throw new RenditionException("Error generating rendition", e);
        }
    }
}
