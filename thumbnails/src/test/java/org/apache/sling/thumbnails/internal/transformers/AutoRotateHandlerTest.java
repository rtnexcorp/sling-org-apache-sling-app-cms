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

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.cms.transformation.TransformationHandlerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link AutoRotateHandler}.
 */
class AutoRotateHandlerTest {

    private AutoRotateHandler handler;
    private TransformationHandlerConfig config;

    @BeforeEach
    void setUp() {
        handler = new AutoRotateHandler();
        config = mock(TransformationHandlerConfig.class);
    }

    @Test
    void testGetResourceType() {
        assertEquals("sling/thumbnails/transformers/autorotate", handler.getResourceType());
    }

    @Test
    void testUsesMetadata() {
        assertTrue(handler.usesMetadata(), "AutoRotateHandler should use metadata");
    }

    @Test
    void testNoRotation_Orientation1() throws IOException {
        // Orientation 1 = Normal (no rotation)
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tiff:Orientation", 1);

        testRotation(metadata, 100, 50, 100, 50); // Width and height should remain same
    }

    @Test
    void testRotate90_Orientation6() throws IOException {
        // Orientation 6 = Rotate 90° CW
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tiff:Orientation", 6);

        // After 90° rotation: width/height swap
        testRotation(metadata, 100, 50, 50, 100);
    }

    @Test
    void testRotate180_Orientation3() throws IOException {
        // Orientation 3 = Rotate 180°
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tiff:Orientation", 3);

        // After 180° rotation: dimensions remain same
        testRotation(metadata, 100, 50, 100, 50);
    }

    @Test
    void testRotate270_Orientation8() throws IOException {
        // Orientation 8 = Rotate 270° CW (90° CCW)
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tiff:Orientation", 8);

        // After 270° rotation: width/height swap
        testRotation(metadata, 100, 50, 50, 100);
    }

    @Test
    void testNoMetadata_PassThrough() throws IOException {
        // Empty metadata - should pass through unchanged
        testRotation(Collections.emptyMap(), 100, 50, 100, 50);
    }

    @Test
    void testOrientationAsString() throws IOException {
        // Orientation as String (should be parsed)
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tiff:Orientation", "6");

        testRotation(metadata, 100, 50, 50, 100);
    }

    @Test
    void testAlternativeMetadataKey_ExifOrientation() throws IOException {
        // Use alternative key: exif:orientation
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("exif:orientation", 6);

        testRotation(metadata, 100, 50, 50, 100);
    }

    @Test
    void testAlternativeMetadataKey_Orientation() throws IOException {
        // Use alternative key: Orientation
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Orientation", 6);

        testRotation(metadata, 100, 50, 50, 100);
    }

    @Test
    void testInvalidOrientation_DefaultToNormal() throws IOException {
        // Invalid orientation value - should default to normal (no rotation)
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tiff:Orientation", "invalid");

        testRotation(metadata, 100, 50, 100, 50);
    }

    /**
     * Helper method to test rotation with a simple test image.
     *
     * @param metadata source metadata
     * @param inputWidth input image width
     * @param inputHeight input image height
     * @param expectedWidth expected output width
     * @param expectedHeight expected output height
     */
    private void testRotation(
            Map<String, Object> metadata, int inputWidth, int inputHeight, int expectedWidth, int expectedHeight)
            throws IOException {

        // Create a simple test image
        BufferedImage testImage = new BufferedImage(inputWidth, inputHeight, BufferedImage.TYPE_INT_RGB);

        // Write to ByteArrayOutputStream
        ByteArrayOutputStream inputBaos = new ByteArrayOutputStream();
        ImageIO.write(testImage, "PNG", inputBaos);
        InputStream inputStream = new ByteArrayInputStream(inputBaos.toByteArray());

        // Process with handler
        ByteArrayOutputStream outputBaos = new ByteArrayOutputStream();
        handler.handle(inputStream, outputBaos, config, metadata);

        // Read output image and verify dimensions
        InputStream outputStream = new ByteArrayInputStream(outputBaos.toByteArray());
        BufferedImage outputImage = ImageIO.read(outputStream);

        assertEquals(
                expectedWidth,
                outputImage.getWidth(),
                "Output image width should be " + expectedWidth + " after rotation");
        assertEquals(
                expectedHeight,
                outputImage.getHeight(),
                "Output image height should be " + expectedHeight + " after rotation");
    }
}
