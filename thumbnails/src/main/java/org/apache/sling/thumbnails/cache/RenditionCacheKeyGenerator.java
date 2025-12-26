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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.transformation.OutputFileFormat;
import org.apache.sling.cms.transformation.Transformation;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for generating deterministic cache keys for renditions.
 *
 * <p>Cache key format:
 * {resourcePath}_{transformationName}_{format}_{sourceChecksum}_{transformationVersion}
 *
 * <p>Example: /content/dam/image.jpg_thumbnail_webp_abc123_v1
 */
@Component(service = RenditionCacheKeyGenerator.class)
public class RenditionCacheKeyGenerator {

    private static final Logger log = LoggerFactory.getLogger(RenditionCacheKeyGenerator.class);

    /**
     * Generate a deterministic cache key for a rendition.
     *
     * @param asset the source asset resource
     * @param transformation the transformation to apply
     * @param format the output format
     * @return the cache key string
     */
    public String generate(Resource asset, Transformation transformation, OutputFileFormat format) {
        if (asset == null || transformation == null || format == null) {
            throw new IllegalArgumentException("asset, transformation, and format must not be null");
        }

        String resourcePath = sanitizePath(asset.getPath());
        String transformationName = transformation.getName();
        String formatName = format.name().toLowerCase();
        String sourceChecksum = getSourceChecksum(asset);
        String transformationVersion = getTransformationVersion(transformation);

        return String.format(
                "%s_%s_%s_%s_%s", resourcePath, transformationName, formatName, sourceChecksum, transformationVersion);
    }

    /**
     * Get the source checksum from asset metadata.
     *
     * @param asset the source asset
     * @return SHA256 checksum or "unknown" if not available
     */
    private String getSourceChecksum(Resource asset) {
        Resource metadata = asset.getChild("jcr:content/metadata");
        if (metadata != null) {
            ValueMap props = metadata.getValueMap();
            String sha256 = props.get("sha256", String.class);
            if (sha256 != null && !sha256.isEmpty()) {
                return sha256.substring(0, Math.min(8, sha256.length())); // First 8 chars
            }
        }
        log.warn("No SHA256 metadata found for asset: {}", asset.getPath());
        return "unknown";
    }

    /**
     * Generate a version hash for the transformation configuration.
     *
     * @param transformation the transformation
     * @return hash of transformation properties
     */
    private String getTransformationVersion(Transformation transformation) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Include transformation name
            digest.update(transformation.getName().getBytes(StandardCharsets.UTF_8));

            // Include transformation path as proxy for configuration
            String path = transformation.getPath();
            if (path != null) {
                digest.update(path.getBytes(StandardCharsets.UTF_8));
            }

            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes).substring(0, 8); // First 8 chars
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            return "v1"; // Fallback version
        }
    }

    /**
     * Sanitize resource path for use in cache key.
     *
     * @param path the resource path
     * @return sanitized path (replace / with _)
     */
    private String sanitizePath(String path) {
        if (path == null) {
            return "unknown";
        }
        return path.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Convert byte array to hex string.
     *
     * @param bytes the bytes
     * @return hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
