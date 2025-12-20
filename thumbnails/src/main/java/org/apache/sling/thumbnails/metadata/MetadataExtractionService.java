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
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service for extracting and enriching metadata from asset files.
 *
 * <p>This service orchestrates the metadata extraction pipeline:
 * <ol>
 *   <li>Determines asset MIME type</li>
 *   <li>Selects appropriate {@link MetadataExtractor}(s) based on MIME type</li>
 *   <li>Runs extractors to gather base metadata</li>
 *   <li>Runs {@link MetadataEnricher}(s) to add computed/derived metadata</li>
 *   <li>Persists metadata to jcr:content/metadata/* location</li>
 * </ol>
 *
 * @since 1.2.0
 */
@ProviderType
public interface MetadataExtractionService {

    /**
     * Extract and persist metadata for an asset.
     *
     * <p>This method:
     * <ul>
     *   <li>Extracts metadata using appropriate extractors</li>
     *   <li>Enriches metadata using registered enrichers</li>
     *   <li>Persists to jcr:content/metadata node</li>
     *   <li>Commits changes to repository</li>
     * </ul>
     *
     * @param assetResource the asset resource (sling:File or nt:file)
     * @throws IOException if extraction or persistence fails
     */
    void extractAndPersistMetadata(Resource assetResource) throws IOException;

    /**
     * Extract metadata for an asset without persisting.
     *
     * <p>This method runs extractors and enrichers but does not save to repository.
     * Useful for preview or validation scenarios.
     *
     * @param assetResource the asset resource
     * @return extracted and enriched metadata map
     * @throws IOException if extraction fails
     */
    Map<String, Object> extractMetadata(Resource assetResource) throws IOException;

    /**
     * Extract metadata without enrichment.
     *
     * <p>This method only runs extractors, skipping enrichers.
     *
     * @param assetResource the asset resource
     * @return extracted metadata map (not enriched)
     * @throws IOException if extraction fails
     */
    Map<String, Object> extractMetadataOnly(Resource assetResource) throws IOException;
}
