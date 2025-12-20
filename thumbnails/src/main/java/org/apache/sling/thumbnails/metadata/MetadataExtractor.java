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
import java.util.Set;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * SPI for extracting metadata from asset files.
 *
 * <p>Multiple implementations can coexist, each handling specific file types.
 * Implementations are automatically discovered and invoked based on MIME type support.
 *
 * <p>Implementations should:
 * <ul>
 *   <li>Return specific MIME types they can handle via {@link #getSupportedMimeTypes()}</li>
 *   <li>Extract metadata as key-value pairs (String keys, Object values)</li>
 *   <li>Support JCR property types: String, Long, Double, Calendar, Boolean, String[]</li>
 *   <li>Handle errors gracefully (return empty map rather than throwing)</li>
 *   <li>Clean up resources properly</li>
 * </ul>
 *
 * <p>Metadata keys should follow these conventions:
 * <ul>
 *   <li>Use standard namespaces (exif:, iptc:, xmp:, dc:, etc.)</li>
 *   <li>Colon-separated namespace:property format (e.g., "exif:DateTimeOriginal")</li>
 *   <li>No spaces or special characters in keys</li>
 * </ul>
 *
 * @since 1.2.0
 */
@ConsumerType
public interface MetadataExtractor {

    /**
     * Get supported MIME types for this extractor.
     *
     * @return set of supported MIME types (e.g., "image/jpeg", "video/mp4")
     */
    Set<String> getSupportedMimeTypes();

    /**
     * Check if this extractor supports the given MIME type.
     *
     * @param mimeType the MIME type to check
     * @return true if this extractor can handle the MIME type
     */
    default boolean supports(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        Set<String> supported = getSupportedMimeTypes();
        if (supported.contains(mimeType)) {
            return true;
        }
        // Check for wildcard match (e.g., "image/*")
        for (String type : supported) {
            if (type.endsWith("/*")) {
                String prefix = type.substring(0, type.length() - 1);
                if (mimeType.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the priority of this extractor. Higher priority extractors
     * are preferred when multiple extractors support the same MIME type.
     *
     * <p>Recommended priority ranges:
     * <ul>
     *   <li>0-50: Fallback/generic implementations (e.g., Tika)</li>
     *   <li>51-100: Standard format-specific implementations</li>
     *   <li>101-200: Specialized/optimized implementations</li>
     *   <li>201+: Custom implementations</li>
     * </ul>
     *
     * @return priority value (higher = preferred)
     */
    default int getPriority() {
        return 100;
    }

    /**
     * Get the display name of this extractor for logging and diagnostics.
     *
     * @return human-readable name of this extractor
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Extract metadata from an asset file.
     *
     * @param inputStream the asset file input stream
     * @param mimeType    the MIME type of the asset
     * @param filename    the original filename (may be null)
     * @return map of metadata key-value pairs, never null (empty map on failure)
     * @throws IOException if an I/O error occurs during extraction
     */
    Map<String, Object> extractMetadata(InputStream inputStream, String mimeType, String filename) throws IOException;
}
