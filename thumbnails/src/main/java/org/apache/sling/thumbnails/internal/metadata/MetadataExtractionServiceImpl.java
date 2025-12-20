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
package org.apache.sling.thumbnails.internal.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.thumbnails.metadata.MetadataEnricher;
import org.apache.sling.thumbnails.metadata.MetadataExtractionService;
import org.apache.sling.thumbnails.metadata.MetadataExtractor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the metadata extraction service.
 *
 * <p>Orchestrates the metadata extraction pipeline by:
 * <ol>
 *   <li>Selecting appropriate extractors based on MIME type and priority</li>
 *   <li>Running extractors to gather base metadata</li>
 *   <li>Running enrichers to add computed metadata</li>
 *   <li>Persisting metadata to jcr:content/metadata node</li>
 * </ol>
 *
 * @since 1.2.0
 */
@Component(service = MetadataExtractionService.class)
public class MetadataExtractionServiceImpl implements MetadataExtractionService {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataExtractionServiceImpl.class);

    private static final String METADATA_NODE_NAME = "metadata";

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private volatile List<MetadataExtractor> extractors;

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private volatile List<MetadataEnricher> enrichers;

    @Override
    public void extractAndPersistMetadata(Resource assetResource) throws IOException {
        LOG.debug("Extracting and persisting metadata for: {}", assetResource.getPath());

        try {
            Map<String, Object> metadata = extractMetadata(assetResource);

            // Add SHA-256 hash
            String sha256 = generateSha256(assetResource);
            if (sha256 != null) {
                metadata.put("SHA256", sha256);
            }

            persistMetadata(assetResource, metadata);

            // Commit changes
            ResourceResolver resolver = assetResource.getResourceResolver();
            resolver.commit();

            LOG.info("Metadata extracted and persisted for: {}", assetResource.getPath());

        } catch (Exception e) {
            throw new IOException("Failed to extract and persist metadata for " + assetResource.getPath(), e);
        }
    }

    @Override
    public Map<String, Object> extractMetadata(Resource assetResource) throws IOException {
        Map<String, Object> metadata = extractMetadataOnly(assetResource);

        // Run enrichers
        runEnrichers(assetResource, metadata);

        return metadata;
    }

    @Override
    public Map<String, Object> extractMetadataOnly(Resource assetResource) throws IOException {
        if (assetResource == null) {
            throw new IllegalArgumentException("Asset resource cannot be null");
        }

        String mimeType = getMimeType(assetResource);
        String filename = getFilename(assetResource);

        LOG.debug("Extracting metadata from: {} (type: {})", assetResource.getPath(), mimeType);

        // Find suitable extractors
        List<MetadataExtractor> suitableExtractors = findExtractorsForMimeType(mimeType);

        if (suitableExtractors.isEmpty()) {
            LOG.warn("No metadata extractor found for MIME type: {}", mimeType);
            return new HashMap<>();
        }

        Map<String, Object> metadata = new HashMap<>();

        // Run extractors in priority order
        for (MetadataExtractor extractor : suitableExtractors) {
            try (InputStream is = getInputStream(assetResource)) {
                if (is == null) {
                    LOG.warn("Cannot get input stream for: {}", assetResource.getPath());
                    continue;
                }

                LOG.debug("Running extractor: {} for {}", extractor.getName(), assetResource.getPath());

                Map<String, Object> extractorMetadata = extractor.extractMetadata(is, mimeType, filename);
                if (extractorMetadata != null && !extractorMetadata.isEmpty()) {
                    metadata.putAll(extractorMetadata);
                    LOG.debug("Extractor {} added {} properties", extractor.getName(), extractorMetadata.size());
                }

            } catch (Exception e) {
                LOG.error("Error running extractor {}: {}", extractor.getName(), e.getMessage(), e);
            }
        }

        return metadata;
    }

    /**
     * Run enrichers on the metadata.
     */
    private void runEnrichers(Resource assetResource, Map<String, Object> metadata) {
        if (enrichers == null || enrichers.isEmpty()) {
            LOG.debug("No metadata enrichers available");
            return;
        }

        String mimeType = getMimeType(assetResource);

        // Sort enrichers by priority (highest first)
        List<MetadataEnricher> sortedEnrichers = enrichers.stream()
                .sorted(Comparator.comparingInt(MetadataEnricher::getPriority).reversed())
                .collect(Collectors.toList());

        for (MetadataEnricher enricher : sortedEnrichers) {
            try {
                if (!enricher.shouldEnrich(assetResource, mimeType, metadata)) {
                    LOG.debug("Enricher {} skipped for {}", enricher.getName(), assetResource.getPath());
                    continue;
                }

                LOG.debug("Running enricher: {} for {}", enricher.getName(), assetResource.getPath());

                try (InputStream is = getInputStream(assetResource)) {
                    enricher.enrich(assetResource, is, mimeType, metadata);
                    LOG.debug("Enricher {} completed", enricher.getName());
                } catch (Exception e) {
                    LOG.error("Error running enricher {}: {}", enricher.getName(), e.getMessage(), e);
                }

            } catch (Exception e) {
                LOG.error("Error in enricher {}: {}", enricher.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Find extractors that support the given MIME type, sorted by priority.
     */
    private List<MetadataExtractor> findExtractorsForMimeType(String mimeType) {
        if (extractors == null || extractors.isEmpty()) {
            LOG.warn("No metadata extractors available");
            return List.of();
        }

        return extractors.stream()
                .filter(e -> e.supports(mimeType))
                .sorted(Comparator.comparingInt(MetadataExtractor::getPriority).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Persist metadata to the jcr:content/metadata node.
     */
    private void persistMetadata(Resource assetResource, Map<String, Object> metadata) throws IOException {
        Resource contentResource = assetResource.getChild(JcrConstants.JCR_CONTENT);
        if (contentResource == null) {
            throw new IOException("No jcr:content node found for: " + assetResource.getPath());
        }

        ResourceResolver resolver = assetResource.getResourceResolver();

        Resource metadataResource = contentResource.getChild(METADATA_NODE_NAME);
        if (metadataResource != null) {
            // Update existing metadata
            ModifiableValueMap properties = metadataResource.adaptTo(ModifiableValueMap.class);
            if (properties != null) {
                properties.putAll(metadata);
                LOG.debug("Updated existing metadata node with {} properties", metadata.size());
            }
        } else {
            // Create new metadata node
            Map<String, Object> properties = new HashMap<>(metadata);
            properties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
            resolver.create(contentResource, METADATA_NODE_NAME, properties);
            LOG.debug("Created new metadata node with {} properties", metadata.size());
        }
    }

    /**
     * Get MIME type from resource.
     */
    private String getMimeType(Resource resource) {
        Resource contentResource = resource.getChild(JcrConstants.JCR_CONTENT);
        if (contentResource != null) {
            ValueMap properties = contentResource.getValueMap();
            String mimeType = properties.get(JcrConstants.JCR_MIMETYPE, String.class);
            if (mimeType != null) {
                return mimeType;
            }
        }
        return "application/octet-stream";
    }

    /**
     * Get filename from resource.
     */
    private String getFilename(Resource resource) {
        String path = resource.getPath();
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    /**
     * Get input stream from resource.
     */
    private InputStream getInputStream(Resource resource) {
        Resource contentResource = resource.getChild(JcrConstants.JCR_CONTENT);
        if (contentResource != null) {
            return contentResource.adaptTo(InputStream.class);
        }
        return resource.adaptTo(InputStream.class);
    }

    /**
     * Generate SHA-256 hash for the asset.
     */
    private String generateSha256(Resource resource) {
        try (InputStream is = getInputStream(resource)) {
            if (is != null) {
                String sha256 = DigestUtils.sha256Hex(is);
                LOG.debug("Generated SHA-256: {} for {}", sha256, resource.getPath());
                return sha256;
            }
        } catch (Exception e) {
            LOG.error("Error generating SHA-256 for {}: {}", resource.getPath(), e.getMessage());
        }
        return null;
    }
}
