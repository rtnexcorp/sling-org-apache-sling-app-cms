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
package org.apache.sling.cms.ai;

import java.io.InputStream;

import org.osgi.annotation.versioning.ProviderType;

/**
 * AI service for image analysis and description generation.
 * <p>
 * This service provides capabilities for:
 * </p>
 * <ul>
 *   <li>Generating alt-text for images (accessibility)</li>
 *   <li>Generating captions for images</li>
 *   <li>Describing image content</li>
 * </ul>
 * <p>
 * Implementations can be backed by vision AI APIs (OpenAI GPT-4V,
 * Azure Computer Vision, Google Cloud Vision, etc.) or rule-based
 * fallback logic using image metadata.
 * </p>
 */
@ProviderType
public interface AiImageService extends AiService {

    /**
     * Generates alt-text for an image.
     * <p>
     * Alt-text should be concise and describe the essential content
     * of the image for accessibility purposes.
     * </p>
     *
     * @param imageStream the image input stream
     * @param mimeType    the MIME type of the image (e.g., "image/jpeg")
     * @param context     optional context about where the image is used
     * @return the AI response with the generated alt-text
     */
    AiResponse generateAltText(InputStream imageStream, String mimeType, String context);

    /**
     * Generates alt-text for an image at the given path.
     *
     * @param imagePath the JCR path to the image asset
     * @param context   optional context about where the image is used
     * @return the AI response with the generated alt-text
     */
    AiResponse generateAltText(String imagePath, String context);

    /**
     * Generates a caption for an image.
     * <p>
     * Captions are typically longer than alt-text and may include
     * more descriptive or contextual information.
     * </p>
     *
     * @param imageStream the image input stream
     * @param mimeType    the MIME type of the image
     * @param context     optional context about where the image is used
     * @return the AI response with the generated caption
     */
    AiResponse generateCaption(InputStream imageStream, String mimeType, String context);

    /**
     * Generates a caption for an image at the given path.
     *
     * @param imagePath the JCR path to the image asset
     * @param context   optional context about where the image is used
     * @return the AI response with the generated caption
     */
    AiResponse generateCaption(String imagePath, String context);

    /**
     * Generates a detailed description of an image.
     *
     * @param imageStream the image input stream
     * @param mimeType    the MIME type of the image
     * @return the AI response with the detailed description
     */
    AiResponse describeImage(InputStream imageStream, String mimeType);

    /**
     * Generates a detailed description of an image at the given path.
     *
     * @param imagePath the JCR path to the image asset
     * @return the AI response with the detailed description
     */
    AiResponse describeImage(String imagePath);

    /**
     * Checks if the given MIME type is supported for image analysis.
     *
     * @param mimeType the MIME type to check
     * @return true if the MIME type is supported
     */
    boolean supportsMimeType(String mimeType);
}
