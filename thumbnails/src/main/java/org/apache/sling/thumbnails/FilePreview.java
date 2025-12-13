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
package org.apache.sling.thumbnails;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface for file preview functionality in the DAM.
 * Provides file type detection and preview capabilities for various file types
 * including images, videos, PDFs, and Office documents.
 */
@ProviderType
public interface FilePreview {

    /**
     * @return the path to the file being previewed
     */
    String getFilePath();

    /**
     * @return the name of the file
     */
    String getFileName();

    /**
     * @return the MIME type of the file
     */
    String getMimeType();

    /**
     * @return true if the file is an image
     */
    boolean isImage();

    /**
     * @return true if the file is a video
     */
    boolean isVideo();

    /**
     * @return true if the file is a PDF
     */
    boolean isPdf();

    /**
     * @return true if the file is a Word document
     */
    boolean isWord();

    /**
     * @return true if the file is an Excel spreadsheet
     */
    boolean isExcel();

    /**
     * @return true if the file is a PowerPoint presentation
     */
    boolean isPowerPoint();

    /**
     * @return true if the file is a text file
     */
    boolean isText();

    /**
     * @return true if the file is any document type (PDF, Word, Excel, PowerPoint)
     */
    boolean isDocument();

    /**
     * @return true if the file can be previewed in browser (image, video, PDF)
     */
    boolean isPreviewable();

    /**
     * @return true if renditions are available for this file
     */
    boolean hasRenditions();

    /**
     * @return list of supported rendition names
     */
    List<String> getSupportedRenditions();

    /**
     * @return the preview type identifier (image, video, pdf, word, excel, powerpoint, text, unknown)
     */
    String getPreviewType();

    /**
     * @return the Jam icon class for the file type
     */
    String getFileIcon();

    /**
     * @return the Bulma color class for the file type badge
     */
    String getFileTypeColor();

    /**
     * @return a human-readable label for the file type
     */
    String getFileTypeLabel();
}
