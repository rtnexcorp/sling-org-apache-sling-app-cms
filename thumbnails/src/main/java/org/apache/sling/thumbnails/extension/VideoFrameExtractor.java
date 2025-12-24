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
package org.apache.sling.thumbnails.extension;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * SPI for extracting frames from video files.
 * Multiple implementations can coexist with different priorities.
 * The system automatically selects the best available implementation at runtime.
 *
 * <p>Implementations should:
 * <ul>
 *   <li>Check library availability in {@link #isAvailable()}</li>
 *   <li>Return appropriate priority via {@link #getPriority()}</li>
 *   <li>Handle errors gracefully (return null rather than throwing)</li>
 *   <li>Clean up resources properly</li>
 * </ul>
 */
@ConsumerType
public interface VideoFrameExtractor {

    /**
     * Check if this extractor is available on the current platform.
     * This method should verify that all required libraries and native
     * dependencies are present and functional.
     *
     * @return true if the required libraries are available and functional
     */
    boolean isAvailable();

    /**
     * Get the priority of this extractor. Higher priority extractors
     * are preferred when multiple are available.
     *
     * <p>Recommended priority ranges:
     * <ul>
     *   <li>0-50: Fallback/placeholder implementations</li>
     *   <li>51-100: Pure Java implementations</li>
     *   <li>101-200: Native accelerated implementations</li>
     *   <li>201+: Custom/specialized implementations</li>
     * </ul>
     *
     * @return priority value (higher = preferred)
     */
    int getPriority();

    /**
     * Get the display name of this extractor for logging and diagnostics.
     *
     * @return human-readable name of this extractor
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Get supported video MIME types.
     *
     * @return set of supported MIME types (e.g., "video/mp4", "video/webm")
     */
    Set<String> getSupportedTypes();

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
        Set<String> supported = getSupportedTypes();
        if (supported.contains(mimeType)) {
            return true;
        }
        // Check for wildcard match (e.g., "video/*")
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
     * Extract the best frame from a video file.
     *
     * @param videoFile the video file to extract from
     * @param config    extraction configuration options
     * @return the extracted frame as BufferedImage, or null if extraction fails
     * @throws IOException if an I/O error occurs during extraction
     */
    BufferedImage extractFrame(File videoFile, VideoExtractionConfig config) throws IOException;
}
