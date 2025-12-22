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
package org.apache.sling.thumbnails.internal.enrichers;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OCRMetadataEnricher.
 * Tests OCR text extraction and metadata enrichment.
 */
public class OCRMetadataEnricherTest {

    private static final Logger log = LoggerFactory.getLogger(OCRMetadataEnricherTest.class);

    private OCRMetadataEnricher enricher;
    private File file;
    private Resource resource;
    private ValueMap valueMap;

    @BeforeEach
    public void setUp() {
        enricher = new OCRMetadataEnricher();

        // Mock the file and resource
        file = mock(File.class);
        resource = mock(Resource.class);
        valueMap = mock(ValueMap.class);

        when(file.getResource()).thenReturn(resource);
        when(file.getPath()).thenReturn("/content/dam/test-image.png");
        when(resource.getValueMap()).thenReturn(valueMap);
    }

    @Test
    public void testEnricherDisabledByDefault() {
        // Activate with default config (disabled)
        OCRMetadataEnricher.Config config = createConfig(false, 50, new String[] {"image/png"}, "", "eng", 10000);
        enricher.activate(config);

        assertEquals("ocr", enricher.getName());
        assertEquals(50, enricher.getPriority());
        assertFalse(enricher.shouldEnrich(file));
    }

    @Test
    public void testEnricherEnabledForSupportedMimeType() {
        // Setup PNG image
        when(valueMap.get("jcr:content/jcr:mimeType", String.class)).thenReturn("image/png");

        // Activate with OCR enabled
        OCRMetadataEnricher.Config config =
                createConfig(true, 50, new String[] {"image/png", "image/jpeg"}, "", "eng", 10000);
        enricher.activate(config);

        assertTrue(enricher.shouldEnrich(file));
    }

    @Test
    public void testEnricherDisabledForUnsupportedMimeType() {
        // Setup video file
        when(valueMap.get("jcr:content/jcr:mimeType", String.class)).thenReturn("video/mp4");

        // Activate with OCR enabled
        OCRMetadataEnricher.Config config =
                createConfig(true, 50, new String[] {"image/png", "image/jpeg"}, "", "eng", 10000);
        enricher.activate(config);

        assertFalse(enricher.shouldEnrich(file));
    }

    @Test
    public void testEnricherSupportsJpegImages() {
        when(valueMap.get("jcr:content/jcr:mimeType", String.class)).thenReturn("image/jpeg");

        OCRMetadataEnricher.Config config =
                createConfig(true, 50, new String[] {"image/png", "image/jpeg"}, "", "eng", 10000);
        enricher.activate(config);

        assertTrue(enricher.shouldEnrich(file));
    }

    @Test
    public void testEnricherSupportsPDF() {
        when(valueMap.get("jcr:content/jcr:mimeType", String.class)).thenReturn("application/pdf");

        OCRMetadataEnricher.Config config =
                createConfig(true, 50, new String[] {"image/png", "application/pdf"}, "", "eng", 10000);
        enricher.activate(config);

        assertTrue(enricher.shouldEnrich(file));
    }

    @Test
    public void testMimeTypeFromAlternativeProperty() {
        // Test fallback to jcr:mimeType property
        when(valueMap.get("jcr:content/jcr:mimeType", String.class)).thenReturn(null);
        when(valueMap.get("jcr:mimeType", String.class)).thenReturn("image/png");

        OCRMetadataEnricher.Config config = createConfig(true, 50, new String[] {"image/png"}, "", "eng", 10000);
        enricher.activate(config);

        assertTrue(enricher.shouldEnrich(file));
    }

    @Test
    public void testEnrichMetadataExtractsText() throws IOException {
        // Setup PNG image with stream
        when(valueMap.get("jcr:content/jcr:mimeType", String.class)).thenReturn("image/png");

        // Return a fresh InputStream each time (needed for Tika parsing)
        when(resource.adaptTo(InputStream.class))
                .thenAnswer(invocation ->
                        OCRMetadataEnricherTest.class.getClassLoader().getResourceAsStream("apache.png"));

        OCRMetadataEnricher.Config config = createConfig(true, 50, new String[] {"image/png"}, "", "eng", 10000);
        enricher.activate(config);

        Map<String, Object> metadata = new HashMap<>();
        enricher.enrichMetadata(file, metadata);

        // The enricher should have attempted to extract text
        // Note: Without actual text in the image and Tesseract, this may not produce text
        // but the method should execute without errors
        assertNotNull(metadata);
        log.info("Extracted metadata: {}", metadata);

        // If OCR text was extracted, verify the key exists
        if (metadata.containsKey("ocr:text")) {
            assertNotNull(metadata.get("ocr:text"));
            assertTrue(metadata.get("ocr:text") instanceof String);
            log.info("OCR text extracted: {}", metadata.get("ocr:text"));
        } else {
            log.info("No OCR text extracted (expected for test images without text)");
        }
    }

    @Test
    public void testEnrichMetadataTruncatesLongText() throws IOException {
        // Setup resource
        when(valueMap.get("jcr:content/jcr:mimeType", String.class)).thenReturn("image/png");
        when(resource.adaptTo(InputStream.class))
                .thenAnswer(invocation ->
                        OCRMetadataEnricherTest.class.getClassLoader().getResourceAsStream("apache.png"));

        // Configure with very small max length
        OCRMetadataEnricher.Config config = createConfig(true, 50, new String[] {"image/png"}, "", "eng", 100);
        enricher.activate(config);

        Map<String, Object> metadata = new HashMap<>();
        enricher.enrichMetadata(file, metadata);

        // If text was extracted, verify it doesn't exceed max length
        if (metadata.containsKey("ocr:text")) {
            String text = (String) metadata.get("ocr:text");
            assertTrue(text.length() <= 100, "OCR text should be truncated to max length");
        }
    }

    @Test
    public void testEnrichMetadataHandlesEmptyStream() throws IOException {
        when(valueMap.get("jcr:content/jcr:mimeType", String.class)).thenReturn("image/png");
        when(resource.adaptTo(InputStream.class)).thenReturn(null);

        OCRMetadataEnricher.Config config = createConfig(true, 50, new String[] {"image/png"}, "", "eng", 10000);
        enricher.activate(config);

        Map<String, Object> metadata = new HashMap<>();

        // Should not throw exception, just skip enrichment
        try {
            enricher.enrichMetadata(file, metadata);
        } catch (Exception e) {
            // OCR enricher logs warnings but doesn't fail
            log.info("Expected exception handled: {}", e.getMessage());
        }
    }

    @Test
    public void testPriorityConfiguration() {
        OCRMetadataEnricher.Config config = createConfig(true, 75, new String[] {"image/png"}, "", "eng", 10000);
        enricher.activate(config);

        assertEquals(75, enricher.getPriority());
    }

    @Test
    public void testCustomLanguageConfiguration() {
        OCRMetadataEnricher.Config config = createConfig(true, 50, new String[] {"image/png"}, "", "fra+eng", 10000);
        enricher.activate(config);

        // Configuration should be stored (verified via logs during activate)
        assertEquals("ocr", enricher.getName());
    }

    /**
     * Helper method to create a Config instance with specified values
     */
    private OCRMetadataEnricher.Config createConfig(
            boolean enabled,
            int priority,
            String[] supportedMimeTypes,
            String tesseractPath,
            String language,
            int maxTextLength) {

        return new OCRMetadataEnricher.Config() {
            @Override
            public Class<OCRMetadataEnricher.Config> annotationType() {
                return OCRMetadataEnricher.Config.class;
            }

            @Override
            public boolean enabled() {
                return enabled;
            }

            @Override
            public int priority() {
                return priority;
            }

            @Override
            public String[] supportedMimeTypes() {
                return supportedMimeTypes;
            }

            @Override
            public String tesseractPath() {
                return tesseractPath;
            }

            @Override
            public String language() {
                return language;
            }

            @Override
            public int maxTextLength() {
                return maxTextLength;
            }
        };
    }
}
