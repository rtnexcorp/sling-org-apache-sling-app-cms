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
package org.apache.sling.cms.core.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.cms.CMSConstants;
import org.apache.sling.cms.File;
import org.apache.sling.cms.FileMetadataEnricher;
import org.apache.sling.cms.FileMetadataExtractor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = FileMetadataExtractor.class)
public class FileMetadataExtractorImpl implements FileMetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(FileMetadataExtractorImpl.class);

    private ResourceResolverFactory resolverFactory;

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            bind = "bindEnricher",
            unbind = "unbindEnricher")
    private volatile List<FileMetadataEnricher> enrichers = new ArrayList<>();

    @Activate
    public FileMetadataExtractorImpl(@Reference ResourceResolverFactory resolverFactory) {
        this.resolverFactory = resolverFactory;
    }

    protected void bindEnricher(FileMetadataEnricher enricher) {
        enrichers.add(enricher);
        sortEnrichers();
        log.info("Registered metadata enricher: {}", enricher.getName());
    }

    protected void unbindEnricher(FileMetadataEnricher enricher) {
        enrichers.remove(enricher);
        log.info("Unregistered metadata enricher: {}", enricher.getName());
    }

    private void sortEnrichers() {
        Collections.sort(
                enrichers,
                Comparator.comparingInt(FileMetadataEnricher::getPriority).reversed());
    }

    @Override
    public Map<String, Object> extractMetadata(File file) throws IOException {
        log.debug("Extracting metadata from {} using {} enrichers", file.getPath(), enrichers.size());
        Map<String, Object> metadata = new HashMap<>();

        // Apply all registered enrichers that support this file
        for (FileMetadataEnricher enricher : enrichers) {
            try {
                if (enricher.shouldEnrich(file)) {
                    log.debug("Applying enricher: {}", enricher.getName());
                    enricher.enrichMetadata(file, metadata);
                } else {
                    log.trace("Skipping enricher {} for file {}", enricher.getName(), file.getPath());
                }
            } catch (Exception e) {
                log.error("Error applying enricher {} to file {}", enricher.getName(), file.getPath(), e);
                // Continue with other enrichers even if one fails
            }
        }

        // Add SHA256 checksum (always computed)
        try {
            metadata.put("SHA256", generateSha(file.getResource()));
        } catch (IOException e) {
            log.warn("Failed to generate SHA256 for {}", file.getPath(), e);
        }

        return metadata;
    }

    @Override
    public void updateMetadata(File file) throws IOException {
        updateMetadata(file, true);
    }

    @Override
    public void updateMetadata(File file, boolean persist) throws IOException {
        log.trace("Updating metadata for {}, persist {}", file, persist);
        Resource resource = file.getResource();
        Resource content = resource.getChild(JcrConstants.JCR_CONTENT);
        if (content == null) {
            log.warn("Content resource is null for {}", resource.getPath());
            return;
        }

        Map<String, Object> properties = null;
        Resource metadata = content.getChild(CMSConstants.NN_METADATA);
        if (metadata != null) {
            properties = metadata.adaptTo(ModifiableValueMap.class);
        } else {
            properties = new HashMap<>();
            properties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        }

        if (properties != null) {
            // Extract metadata using all enrichers
            properties.putAll(extractMetadata(file));
            resource.getResourceResolver().refresh();

            if (metadata == null) {
                resource.getResourceResolver().create(content, CMSConstants.NN_METADATA, properties);
            }

            if (persist) {
                resource.getResourceResolver().commit();
            }
            log.info("Metadata extracted from {}", resource.getPath());
        } else {
            throw new IOException("Unable to update metadata for " + resource.getPath());
        }
    }

    protected String generateSha(Resource resource) throws IOException {
        try (InputStream is = resource.adaptTo(InputStream.class)) {
            String sha256 = DigestUtils.sha256Hex(is);
            log.debug("Generated SHA {} for {}", sha256, resource.getPath());
            return sha256;
        }
    }
}
