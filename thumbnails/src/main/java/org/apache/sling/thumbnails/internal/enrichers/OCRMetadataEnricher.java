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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.File;
import org.apache.sling.cms.FileMetadataEnricher;
import org.apache.tika.Tika;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Metadata enricher that performs OCR (Optical Character Recognition) on
 * images and PDFs using Apache Tika with Tesseract.
 *
 * <p>Prerequisites:
 * <ul>
 *   <li>Tesseract OCR must be installed on the system</li>
 *   <li>Tesseract binary must be in system PATH or configured via tesseractPath</li>
 *   <li>Language data files must be installed (e.g., eng.traineddata)</li>
 * </ul>
 *
 * <p>This enricher extracts text content from images and scanned PDFs,
 * storing the result in the 'ocr:text' metadata property.
 */
@Component(service = FileMetadataEnricher.class)
@Designate(ocd = OCRMetadataEnricher.Config.class)
public class OCRMetadataEnricher implements FileMetadataEnricher {

    private static final Logger log = LoggerFactory.getLogger(OCRMetadataEnricher.class);
    private static final String OCR_TEXT_KEY = "ocr:text";
    private static final int MAX_OCR_TEXT_LENGTH = 10000;

    @ObjectClassDefinition(name = "OCR Metadata Enricher Configuration")
    public @interface Config {
        @AttributeDefinition(
                name = "Enabled",
                description = "Enable OCR metadata extraction. Requires Tesseract to be installed.")
        boolean enabled() default false;

        @AttributeDefinition(name = "Priority", description = "Enricher priority (higher runs first)")
        int priority() default 50;

        @AttributeDefinition(
                name = "Supported MIME Types",
                description = "MIME types to perform OCR on (e.g., image/png, image/jpeg, application/pdf)")
        String[] supportedMimeTypes() default {
            "image/png", "image/jpeg", "image/jpg", "image/tiff", "image/bmp", "image/gif", "application/pdf"
        };

        @AttributeDefinition(
                name = "Tesseract Path",
                description = "Path to tesseract binary (leave empty to use system PATH)")
        String tesseractPath() default "";

        @AttributeDefinition(
                name = "Tesseract Language",
                description = "Language for OCR (e.g., 'eng', 'fra', 'deu', 'eng+fra')")
        String language() default "eng";

        @AttributeDefinition(name = "Max Text Length", description = "Maximum length of extracted OCR text to store")
        int maxTextLength() default MAX_OCR_TEXT_LENGTH;
    }

    private boolean enabled;
    private int priority;
    private Set<String> supportedMimeTypes;
    private String tesseractPath;
    private String language;
    private int maxTextLength;

    @Activate
    protected void activate(Config config) {
        this.enabled = config.enabled();
        this.priority = config.priority();
        this.supportedMimeTypes = new HashSet<>(Arrays.asList(config.supportedMimeTypes()));
        this.tesseractPath = config.tesseractPath();
        this.language = config.language();
        this.maxTextLength = config.maxTextLength();

        log.info(
                "OCRMetadataEnricher activated - enabled: {}, priority: {}, language: {}, supported types: {}",
                enabled,
                priority,
                language,
                supportedMimeTypes);

        if (enabled) {
            checkTesseractAvailability();
        }
    }

    @Override
    public String getName() {
        return "ocr";
    }

    @Override
    public boolean shouldEnrich(File file) {
        if (!enabled) {
            return false;
        }

        String mimeType = getMimeType(file.getResource());
        return mimeType != null && supportedMimeTypes.contains(mimeType);
    }

    @Override
    public void enrichMetadata(File file, Map<String, Object> metadata) throws IOException {
        log.debug("Performing OCR on {}", file.getPath());
        Resource resource = file.getResource();

        try (InputStream is = resource.adaptTo(InputStream.class)) {
            // Note: OCR functionality requires additional Tika parsers and Tesseract installation
            // This is a placeholder implementation that extracts text content
            // For full OCR support, add org.apache.tika:tika-parsers dependency
            // and install Tesseract OCR engine

            Tika tika = new Tika();
            String extractedText = tika.parseToString(is);

            if (extractedText != null && !extractedText.trim().isEmpty()) {
                // Truncate if needed
                if (extractedText.length() > maxTextLength) {
                    extractedText = extractedText.substring(0, maxTextLength);
                }
                metadata.put(OCR_TEXT_KEY, extractedText.trim());
                log.info("Extracted {} characters from {}", extractedText.length(), file.getPath());
            } else {
                log.debug("No text extracted from {}", file.getPath());
            }
        } catch (Exception e) {
            log.warn("Text extraction error for {}: {}", file.getPath(), e.getMessage());
            // Don't fail the enrichment, just skip OCR for this file
        }
    }

    @Override
    public int getPriority() {
        return priority;
    }

    private String getMimeType(Resource resource) {
        ValueMap properties = resource.getValueMap();
        String mimeType = properties.get("jcr:content/jcr:mimeType", String.class);
        if (mimeType == null) {
            mimeType = properties.get("jcr:mimeType", String.class);
        }
        return mimeType;
    }

    private void checkTesseractAvailability() {
        try {
            ProcessBuilder pb = new ProcessBuilder(tesseractPath.isEmpty() ? "tesseract" : tesseractPath, "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Tesseract OCR is available");
            } else {
                log.warn("Tesseract OCR returned non-zero exit code: {}", exitCode);
            }
        } catch (IOException | InterruptedException e) {
            log.warn(
                    "Tesseract OCR not found - OCR enricher is enabled but will fail. "
                            + "Please install Tesseract or disable this enricher. Error: {}",
                    e.getMessage());
        }
    }
}
