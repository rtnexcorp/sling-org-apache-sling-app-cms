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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.sling.thumbnails.metadata.MetadataExtractor;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Metadata extractor for PDF files using Apache PDFBox.
 *
 * <p>Extracts PDF metadata including:
 * <ul>
 *   <li>Title, Author, Subject, Keywords</li>
 *   <li>Creator, Producer</li>
 *   <li>Creation date, Modification date</li>
 *   <li>Page count</li>
 *   <li>PDF version</li>
 *   <li>Encryption status</li>
 * </ul>
 *
 * @since 1.2.0
 */
@Component(
        service = MetadataExtractor.class,
        property = {"service.ranking:Integer=100"})
public class PdfMetadataExtractor implements MetadataExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(PdfMetadataExtractor.class);

    private static final Set<String> SUPPORTED_TYPES = Set.of("application/pdf");

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
        return "PDF Metadata Extractor (PDFBox)";
    }

    @Override
    public Map<String, Object> extractMetadata(InputStream inputStream, String mimeType, String filename)
            throws IOException {
        Map<String, Object> metadata = new HashMap<>();

        try (PDDocument document = PDDocument.load(inputStream.readAllBytes())) {

            // Check if document is encrypted
            if (document.isEncrypted()) {
                metadata.put("pdf:encrypted", true);
                try {
                    document.setAllSecurityToBeRemoved(true);
                } catch (Exception e) {
                    LOG.warn("Cannot decrypt PDF document: {}", e.getMessage());
                    metadata.put("pdf:encryptionError", e.getMessage());
                    return metadata;
                }
            } else {
                metadata.put("pdf:encrypted", false);
            }

            // Extract document information
            PDDocumentInformation info = document.getDocumentInformation();
            if (info != null) {
                extractDocumentInfo(info, metadata);
            }

            // Extract page count
            int pageCount = document.getNumberOfPages();
            metadata.put("pdf:pageCount", pageCount);

            // Extract PDF version
            float pdfVersion = document.getVersion();
            metadata.put("pdf:version", String.valueOf(pdfVersion));

            LOG.debug("Extracted PDF metadata: {} pages, version {}", pageCount, pdfVersion);

        } catch (InvalidPasswordException e) {
            LOG.warn("PDF is password protected: {}", filename);
            metadata.put("pdf:passwordProtected", true);
        } catch (Exception e) {
            LOG.error("Error extracting PDF metadata", e);
        }

        return metadata;
    }

    /**
     * Extract PDF document information fields.
     */
    private void extractDocumentInfo(PDDocumentInformation info, Map<String, Object> metadata) {
        // Title
        String title = info.getTitle();
        if (title != null && !title.isEmpty()) {
            metadata.put("pdf:title", title);
            metadata.put("dc:title", title); // Dublin Core
        }

        // Author
        String author = info.getAuthor();
        if (author != null && !author.isEmpty()) {
            metadata.put("pdf:author", author);
            metadata.put("dc:creator", author); // Dublin Core
        }

        // Subject
        String subject = info.getSubject();
        if (subject != null && !subject.isEmpty()) {
            metadata.put("pdf:subject", subject);
            metadata.put("dc:description", subject); // Dublin Core
        }

        // Keywords
        String keywords = info.getKeywords();
        if (keywords != null && !keywords.isEmpty()) {
            metadata.put("pdf:keywords", keywords);
        }

        // Creator (application that created the original document)
        String creator = info.getCreator();
        if (creator != null && !creator.isEmpty()) {
            metadata.put("pdf:creator", creator);
        }

        // Producer (application that converted to PDF)
        String producer = info.getProducer();
        if (producer != null && !producer.isEmpty()) {
            metadata.put("pdf:producer", producer);
        }

        // Creation date
        Calendar creationDate = info.getCreationDate();
        if (creationDate != null) {
            metadata.put("pdf:creationDate", creationDate);
            metadata.put("dc:created", creationDate); // Dublin Core
        }

        // Modification date
        Calendar modificationDate = info.getModificationDate();
        if (modificationDate != null) {
            metadata.put("pdf:modificationDate", modificationDate);
            metadata.put("dc:modified", modificationDate); // Dublin Core
        }

        // Trapped
        String trapped = info.getTrapped();
        if (trapped != null && !trapped.isEmpty()) {
            metadata.put("pdf:trapped", trapped);
        }
    }
}
