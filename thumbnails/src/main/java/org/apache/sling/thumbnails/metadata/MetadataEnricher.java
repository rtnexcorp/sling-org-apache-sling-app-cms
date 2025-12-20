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
package org.apache.sling.thumbnails.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * SPI for enriching extracted metadata with additional computed or derived values.
 *
 * <p>Enrichers run after basic metadata extraction and can:
 * <ul>
 *   <li>Add computed values (e.g., dominant colors, face detection)</li>
 *   <li>Perform OCR on images or PDFs</li>
 *   <li>Generate tags or classifications</li>
 *   <li>Validate or normalize extracted metadata</li>
 *   <li>Call external services for enrichment</li>
 * </ul>
 *
 * <p>Enrichers are executed in priority order (highest first).
 * Each enricher receives the already-extracted metadata and can add/modify values.
 *
 * @since 1.2.0
 */
@ConsumerType
public interface MetadataEnricher {

    /**
     * Get the priority of this enricher. Higher priority enrichers run first.
     *
     * <p>Recommended priority ranges:
     * <ul>
     *   <li>0-50: Low priority enrichers (cleanup, normalization)</li>
     *   <li>51-100: Standard enrichers</li>
     *   <li>101-200: High priority enrichers (expensive operations)</li>
     *   <li>201+: Critical enrichers that others may depend on</li>
     * </ul>
     *
     * @return priority value (higher = runs first)
     */
    default int getPriority() {
        return 100;
    }

    /**
     * Get the display name of this enricher for logging and diagnostics.
     *
     * @return human-readable name of this enricher
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Check if this enricher should run for the given asset.
     *
     * @param assetResource the asset resource
     * @param mimeType      the MIME type of the asset
     * @param metadata      the currently extracted metadata
     * @return true if this enricher should run
     */
    boolean shouldEnrich(Resource assetResource, String mimeType, Map<String, Object> metadata);

    /**
     * Enrich the metadata with additional values.
     *
     * <p>Implementations should:
     * <ul>
     *   <li>Add new key-value pairs to the metadata map</li>
     *   <li>Optionally modify existing values</li>
     *   <li>Handle errors gracefully (log and continue)</li>
     *   <li>Avoid expensive operations that block for too long</li>
     *   <li>Use appropriate namespace prefixes for keys</li>
     * </ul>
     *
     * @param assetResource the asset resource
     * @param inputStream   the asset file input stream (may be null if enricher doesn't need it)
     * @param mimeType      the MIME type of the asset
     * @param metadata      the metadata map to enrich (mutable)
     * @throws IOException if an I/O error occurs during enrichment
     */
    void enrich(Resource assetResource, InputStream inputStream, String mimeType, Map<String, Object> metadata)
            throws IOException;
}
