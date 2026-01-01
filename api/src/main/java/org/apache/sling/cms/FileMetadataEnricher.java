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

import java.io.IOException;
import java.util.Map;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Service interface for enriching file metadata. Implementations of this
 * interface can extract or augment metadata from files in various ways
 * (e.g., EXIF extraction, OCR, face detection, etc.).
 *
 * Multiple enrichers can be registered and will be applied in sequence
 * based on their service ranking.
 *
 * @since 2.4.0
 */
@ConsumerType
public interface FileMetadataEnricher {

    /**
     * Returns the name of this enricher for logging and configuration purposes.
     *
     * @return the enricher name (e.g., "tika", "ocr", "face-detection")
     */
    String getName();

    /**
     * Determines if this enricher should process the given file based on
     * its MIME type, resource type, or other characteristics.
     *
     * @param file the file to check
     * @return true if this enricher can process the file, false otherwise
     */
    boolean shouldEnrich(File file);

    /**
     * Enriches the metadata for the specified file. This method should add
     * new metadata properties to the provided map or augment existing ones.
     *
     * @param file the file to enrich metadata for
     * @param metadata the map to add/update metadata properties in
     * @throws IOException if an error occurs during enrichment
     */
    void enrichMetadata(File file, Map<String, Object> metadata) throws IOException;

    /**
     * Returns the priority/ranking of this enricher. Higher values run first.
     * Default implementation returns 0.
     *
     * @return the enricher priority (higher runs first)
     */
    default int getPriority() {
        return 0;
    }
}
