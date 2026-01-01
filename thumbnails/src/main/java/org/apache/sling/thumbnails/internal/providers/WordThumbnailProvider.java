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
package org.apache.sling.thumbnails.internal.providers;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.thumbnails.OutputFileFormat;
import org.apache.sling.thumbnails.extension.ThumbnailProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides thumbnails for Microsoft Word documents (DOC and DOCX).
 * Generates a preview image of the first page content.
 */
@Component(service = ThumbnailProvider.class, immediate = true)
public class WordThumbnailProvider implements ThumbnailProvider {

    private static final Logger log = LoggerFactory.getLogger(WordThumbnailProvider.class);

    private static final int THUMBNAIL_WIDTH = 595; // A4 width at 72 DPI
    private static final int THUMBNAIL_HEIGHT = 842; // A4 height at 72 DPI
    private static final int MARGIN = 40;
    private static final int MAX_CHARS = 2000;

    @Override
    public boolean applies(Resource resource, String metaType) {
        try {
            MimeType mt = new MimeType(metaType);
            return mt.match("application/msword")
                    || mt.match("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } catch (MimeTypeParseException e) {
            log.warn("Failed to parse mime type: {}", metaType, e);
            return false;
        }
    }

    @Override
    public InputStream getThumbnail(Resource resource) throws IOException {
        String mimeType = getMimeType(resource);
        String text = extractText(resource, mimeType);

        BufferedImage image = createDocumentPreview(text);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, OutputFileFormat.PNG.toString(), os);
        return new ByteArrayInputStream(os.toByteArray());
    }

    private String getMimeType(Resource resource) {
        return resource.getValueMap()
                .get("jcr:content/jcr:mimeType", resource.getValueMap().get("jcr:mimeType", "application/msword"));
    }

    private String extractText(Resource resource, String mimeType) throws IOException {
        try (InputStream is = resource.adaptTo(InputStream.class)) {
            if (is == null) {
                throw new IOException("Cannot read resource: " + resource.getPath());
            }

            if (mimeType.contains("openxmlformats")) {
                // DOCX format
                try (XWPFDocument document = new XWPFDocument(is);
                        XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    return truncateText(extractor.getText());
                }
            } else {
                // DOC format
                try (HWPFDocument document = new HWPFDocument(is);
                        WordExtractor extractor = new WordExtractor(document)) {
                    return truncateText(extractor.getText());
                }
            }
        }
    }

    private String truncateText(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() > MAX_CHARS) {
            return text.substring(0, MAX_CHARS) + "...";
        }
        return text;
    }

    private BufferedImage createDocumentPreview(String text) {
        BufferedImage image = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // White background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);

        // Draw a subtle border
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawRect(0, 0, THUMBNAIL_WIDTH - 1, THUMBNAIL_HEIGHT - 1);

        // Draw text content
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));

        drawWrappedText(g2d, text, MARGIN, MARGIN, THUMBNAIL_WIDTH - 2 * MARGIN, THUMBNAIL_HEIGHT - 2 * MARGIN);

        g2d.dispose();
        return image;
    }

    private void drawWrappedText(Graphics2D g2d, String text, int x, int y, int maxWidth, int maxHeight) {
        if (text == null || text.isEmpty()) {
            return;
        }

        int lineHeight = g2d.getFontMetrics().getHeight();
        int currentY = y + lineHeight;

        String[] paragraphs = text.split("\n");
        for (String paragraph : paragraphs) {
            if (currentY > y + maxHeight) {
                break;
            }

            String[] words = paragraph.split("\\s+");
            StringBuilder line = new StringBuilder();

            for (String word : words) {
                String testLine = line.length() == 0 ? word : line + " " + word;
                int lineWidth = g2d.getFontMetrics().stringWidth(testLine);

                if (lineWidth > maxWidth && line.length() > 0) {
                    g2d.drawString(line.toString(), x, currentY);
                    currentY += lineHeight;
                    line = new StringBuilder(word);

                    if (currentY > y + maxHeight) {
                        break;
                    }
                } else {
                    line = new StringBuilder(testLine);
                }
            }

            if (line.length() > 0 && currentY <= y + maxHeight) {
                g2d.drawString(line.toString(), x, currentY);
                currentY += lineHeight;
            }

            // Add extra spacing between paragraphs
            currentY += lineHeight / 2;
        }
    }
}
