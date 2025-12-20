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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.sling.thumbnails.metadata.MetadataExtractor;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Generic fallback metadata extractor using Apache Tika.
 *
 * <p>This extractor serves as a fallback for file types that don't have
 * specialized extractors. It uses Tika's AutoDetectParser to handle
 * a wide variety of file formats.
 *
 * <p>Runs with low priority (50) to allow specialized extractors to take precedence.
 *
 * @since 1.2.0
 */
@Component(
        service = MetadataExtractor.class,
        property = {"service.ranking:Integer=50"})
public class TikaMetadataExtractor implements MetadataExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(TikaMetadataExtractor.class);

    /**
     * Wildcard matcher - supports all MIME types as fallback
     */
    private static final Set<String> SUPPORTED_TYPES = Set.of("*/*");

    @Override
    public Set<String> getSupportedMimeTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public int getPriority() {
        return 50; // Low priority - fallback only
    }

    @Override
    public String getName() {
        return "Tika Fallback Metadata Extractor";
    }

    @Override
    public boolean supports(String mimeType) {
        // Support all MIME types as fallback
        return mimeType != null;
    }

    @Override
    @SuppressWarnings(value = {"java:S1874"})
    public Map<String, Object> extractMetadata(InputStream inputStream, String mimeType, String filename)
            throws IOException {
        Map<String, Object> metadata = new HashMap<>();

        try {
            Parser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata tikaMetadata = new Metadata();
            ParseContext context = new ParseContext();

            // Set the MIME type hint if available
            if (mimeType != null) {
                tikaMetadata.set(Metadata.CONTENT_TYPE, mimeType);
            }

            // Set filename hint if available
            if (filename != null) {
                tikaMetadata.set(Metadata.RESOURCE_NAME_KEY, filename);
            }

            try {
                parser.parse(inputStream, handler, tikaMetadata, context);
            } catch (SAXException se) {
                // WriteLimitReachedException is normal for large files
                if ("WriteLimitReachedException".equals(se.getClass().getSimpleName())) {
                    LOG.debug("Write limit reached during parsing (normal for large files)");
                } else {
                    throw se;
                }
            }

            // Extract all Tika metadata
            for (String name : tikaMetadata.names()) {
                extractProperty(metadata, name, tikaMetadata);
            }

            LOG.debug("Tika extracted {} metadata properties", metadata.size());

        } catch (SAXException | TikaException e) {
            LOG.debug("Tika parsing completed with exception: {}", e.getMessage());
        } catch (Exception e) {
            LOG.error("Error extracting metadata with Tika", e);
        }

        return metadata;
    }

    /**
     * Extract a property from Tika metadata and add to the metadata map.
     */
    private void extractProperty(Map<String, Object> metadata, String name, Metadata tikaMetadata) {
        try {
            String key = formatMetadataKey(name);

            Property property = Property.get(name);
            if (property != null) {
                if (tikaMetadata.isMultiValued(property)) {
                    String[] values = tikaMetadata.getValues(property);
                    if (values != null && values.length > 0) {
                        metadata.put(key, values);
                    }
                } else if (tikaMetadata.getDate(property) != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(tikaMetadata.getDate(property));
                    metadata.put(key, cal);
                } else if (tikaMetadata.getInt(property) != null) {
                    metadata.put(key, tikaMetadata.getInt(property));
                } else {
                    String value = tikaMetadata.get(property);
                    if (value != null && !value.isEmpty()) {
                        metadata.put(key, value);
                    }
                }
            } else {
                String value = tikaMetadata.get(name);
                if (value != null && !value.isEmpty()) {
                    metadata.put(key, value);
                }
            }
        } catch (Exception e) {
            LOG.debug("Error extracting property {}: {}", name, e.getMessage());
        }
    }

    /**
     * Format metadata key to be JCR-compatible.
     */
    private String formatMetadataKey(String key) {
        if (key == null) {
            return null;
        }

        // Replace spaces and special characters
        String formatted = key.replace(" ", "").replace("/", "-").replace("\\", "-");

        // Ensure namespace format for common prefixes
        if (!formatted.contains(":") && formatted.length() > 0) {
            // Add namespace for common prefixes
            if (formatted.toLowerCase().startsWith("content")) {
                // Keep content- prefix keys as-is
                return "tika:" + formatted;
            }
        }

        return formatted;
    }
}
