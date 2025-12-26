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
package org.apache.sling.thumbnails.internal.transformers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.IOUtils;
import org.apache.sling.cms.transformation.TransformationHandlerConfig;
import org.apache.sling.thumbnails.extension.TransformationHandler;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Auto-rotation handler that uses EXIF orientation metadata to correct image
 * orientation automatically.
 * <p>
 * This handler reads the EXIF orientation tag from the source image metadata and
 * applies the appropriate rotation to ensure the image is displayed correctly.
 * Common use case: Photos taken with mobile devices in portrait orientation.
 * </p>
 * <p>
 * EXIF Orientation values:
 * <ul>
 * <li>1 = Normal (0°)</li>
 * <li>3 = Rotate 180°</li>
 * <li>6 = Rotate 90° CW</li>
 * <li>8 = Rotate 270° CW (90° CCW)</li>
 * </ul>
 * </p>
 *
 * @since 1.1.0
 */
@Component(service = TransformationHandler.class)
public class AutoRotateHandler implements TransformationHandler {

    private static final Logger log = LoggerFactory.getLogger(AutoRotateHandler.class);

    /**
     * Resource type for this handler's configuration nodes.
     */
    private static final String RESOURCE_TYPE = "sling/thumbnails/transformers/autorotate";

    /**
     * Standard EXIF orientation metadata keys to check (in order of preference).
     */
    private static final String[] ORIENTATION_KEYS =
            new String[] {"tiff:Orientation", "exif:orientation", "Orientation"};

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public boolean usesMetadata() {
        return true; // This handler requires metadata
    }

    @Override
    public void handle(InputStream inputStream, OutputStream outputStream, TransformationHandlerConfig config)
            throws IOException {
        // Backward compatibility: call metadata-aware version with empty metadata
        handle(inputStream, outputStream, config, Collections.emptyMap());
    }

    @Override
    public void handle(
            InputStream inputStream,
            OutputStream outputStream,
            TransformationHandlerConfig config,
            Map<String, Object> sourceMetadata)
            throws IOException {

        // Get EXIF orientation from metadata
        Object orientationObj = findOrientation(sourceMetadata);
        double rotation = 0;

        if (orientationObj != null) {
            int orientation = parseOrientation(orientationObj);
            rotation = calculateRotation(orientation);
            log.debug("Applying auto-rotation: {} degrees (EXIF orientation: {})", rotation, orientation);
        } else {
            log.debug("No EXIF orientation found, skipping auto-rotation");
        }

        // Apply rotation if needed
        if (rotation != 0) {
            Thumbnails.of(inputStream).rotate(rotation).scale(1.0).toOutputStream(outputStream);
        } else {
            // No rotation needed, pass through unchanged
            IOUtils.copy(inputStream, outputStream);
        }
    }

    /**
     * Find orientation value in metadata by checking multiple possible keys.
     *
     * @param metadata source file metadata
     * @return orientation value or null if not found
     */
    private Object findOrientation(Map<String, Object> metadata) {
        for (String key : ORIENTATION_KEYS) {
            Object value = metadata.get(key);
            if (value != null) {
                log.debug("Found orientation value {} under key {}", value, key);
                return value;
            }
        }
        return null;
    }

    /**
     * Calculate rotation degrees from EXIF orientation value.
     * <p>
     * EXIF Orientation values:
     * <ul>
     * <li>1 = Normal (0°)</li>
     * <li>3 = Rotate 180°</li>
     * <li>6 = Rotate 90° CW</li>
     * <li>8 = Rotate 270° CW (or 90° CCW)</li>
     * </ul>
     * </p>
     *
     * @param orientation EXIF orientation value (1-8)
     * @return rotation in degrees (0, 90, 180, or 270)
     */
    private double calculateRotation(int orientation) {
        switch (orientation) {
            case 3:
                return 180.0;
            case 6:
                return 90.0;
            case 8:
                return 270.0;
            case 1:
            default:
                return 0.0;
        }
    }

    /**
     * Parse orientation value from metadata (handles Integer or String).
     *
     * @param orientationObj orientation value from metadata
     * @return parsed integer orientation value (defaults to 1 if invalid)
     */
    private int parseOrientation(Object orientationObj) {
        if (orientationObj instanceof Integer) {
            return (Integer) orientationObj;
        } else if (orientationObj instanceof String) {
            try {
                return Integer.parseInt((String) orientationObj);
            } catch (NumberFormatException e) {
                log.warn("Invalid orientation value: {}, defaulting to 1 (normal)", orientationObj);
                return 1;
            }
        }
        log.warn("Unexpected orientation type: {}, defaulting to 1 (normal)", orientationObj.getClass());
        return 1;
    }
}
