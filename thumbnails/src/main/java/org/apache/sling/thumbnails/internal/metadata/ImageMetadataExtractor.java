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

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
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
 * Metadata extractor for image files using Apache Tika.
 *
 * <p>Extracts EXIF, IPTC, and XMP metadata from image files including:
 * <ul>
 *   <li>Image dimensions (width, height)</li>
 *   <li>EXIF data (camera settings, GPS, timestamps)</li>
 *   <li>IPTC data (keywords, copyright, creator)</li>
 *   <li>XMP metadata</li>
 *   <li>Color space and bit depth</li>
 * </ul>
 *
 * @since 1.2.0
 */
@Component(
        service = MetadataExtractor.class,
        property = {"service.ranking:Integer=100"})
public class ImageMetadataExtractor implements MetadataExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(ImageMetadataExtractor.class);

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/tiff",
            "image/bmp",
            "image/webp",
            "image/heic",
            "image/heif");

    @Override
    public Set<String> getSupportedMimeTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public String getName() {
        return "Image Metadata Extractor (EXIF/IPTC/XMP)";
    }

    @Override
    @SuppressWarnings(value = {"java:S1874"})
    public Map<String, Object> extractMetadata(InputStream inputStream, String mimeType, String filename)
            throws IOException {
        Map<String, Object> metadata = new HashMap<>();

        try {
            // Use Tika to extract comprehensive metadata
            Parser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata tikaMetadata = new Metadata();
            ParseContext context = new ParseContext();

            // Mark the stream to allow re-reading
            inputStream.mark(Integer.MAX_VALUE);

            try {
                parser.parse(inputStream, handler, tikaMetadata, context);
            } catch (SAXException | TikaException e) {
                LOG.debug("Tika parsing completed with exception (may be normal): {}", e.getMessage());
            }

            // Extract all Tika metadata
            for (String name : tikaMetadata.names()) {
                extractTikaProperty(metadata, name, tikaMetadata);
            }

            // Reset stream for ImageIO extraction
            try {
                inputStream.reset();
            } catch (IOException e) {
                LOG.debug("Could not reset stream for ImageIO extraction: {}", e.getMessage());
                return metadata;
            }

            // Extract image dimensions using ImageIO
            extractImageDimensions(inputStream, metadata);

        } catch (Exception e) {
            LOG.error("Error extracting image metadata", e);
        }

        return metadata;
    }

    /**
     * Extract a property from Tika metadata and add to the metadata map.
     */
    private void extractTikaProperty(Map<String, Object> metadata, String name, Metadata tikaMetadata) {
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

        // Ensure namespace format (prefix:name)
        if (!formatted.contains(":") && formatted.length() > 0) {
            // Add default namespace for non-namespaced keys
            if (formatted.startsWith("EXIF") || formatted.startsWith("exif")) {
                formatted = "exif:" + formatted.substring(4);
            } else if (formatted.startsWith("IPTC") || formatted.startsWith("iptc")) {
                formatted = "iptc:" + formatted.substring(4);
            } else if (formatted.startsWith("XMP") || formatted.startsWith("xmp")) {
                formatted = "xmp:" + formatted.substring(3);
            }
        }

        return formatted;
    }

    /**
     * Extract image dimensions using ImageIO.
     */
    private void extractImageDimensions(InputStream inputStream, Map<String, Object> metadata) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(inputStream)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(iis);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);

                    metadata.put("image:width", width);
                    metadata.put("image:height", height);

                    LOG.debug("Extracted image dimensions: {}x{}", width, height);
                } finally {
                    reader.dispose();
                }
            }
        } catch (Exception e) {
            LOG.debug("Could not extract image dimensions via ImageIO: {}", e.getMessage());
        }
    }
}
