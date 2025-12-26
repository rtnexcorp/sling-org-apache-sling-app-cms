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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.cms.transformation.BadRequestException;
import org.apache.sling.cms.transformation.TransformationHandlerConfig;
import org.apache.sling.thumbnails.internal.models.TransformationHandlerConfigImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ColorizeHandlerTest {

    private ByteArrayOutputStream outputStream;
    private InputStream inputStream;
    private ColorizeHandler colorizer;

    @BeforeEach
    public void init() {
        inputStream = getClass().getClassLoader().getResourceAsStream("apache.png");
        outputStream = new ByteArrayOutputStream();
        colorizer = new ColorizeHandler();
    }

    @Test
    public void testResourceType() throws IOException {
        assertEquals("sling/thumbnails/transformers/colorize", colorizer.getResourceType());
    }

    @Test
    public void testColorize() throws IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ColorizeHandler.PN_BLUE, 12);
        properties.put(ColorizeHandler.PN_RED, 12);
        properties.put(ColorizeHandler.PN_GREEN, 12);
        properties.put(ColorizeHandler.PN_ALPHA, .8);

        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        colorizer.handle(inputStream, outputStream, config);
        assertNotEquals(0, outputStream.toByteArray().length);
    }

    @Test
    public void testNoAlpha() throws IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ColorizeHandler.PN_BLUE, 12);
        properties.put(ColorizeHandler.PN_RED, 12);
        properties.put(ColorizeHandler.PN_GREEN, 12);

        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        colorizer.handle(inputStream, outputStream, config);
        assertNotEquals(0, outputStream.toByteArray().length);
    }

    @Test
    public void testMissingColor() throws IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ColorizeHandler.PN_BLUE, 12);
        properties.put(ColorizeHandler.PN_GREEN, 12);
        properties.put(ColorizeHandler.PN_ALPHA, .8);

        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        colorizer.handle(inputStream, outputStream, config);
        assertNotEquals(0, outputStream.toByteArray().length);
    }

    @Test
    public void testInvalidColor() throws IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ColorizeHandler.PN_BLUE, 12);
        properties.put(ColorizeHandler.PN_RED, 256);
        properties.put(ColorizeHandler.PN_GREEN, 12);
        properties.put(ColorizeHandler.PN_ALPHA, .8);
        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        assertThrows(BadRequestException.class, () -> colorizer.handle(inputStream, outputStream, config));
    }

    @Test
    public void testInvalidAlpha() throws IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ColorizeHandler.PN_BLUE, 12);
        properties.put(ColorizeHandler.PN_RED, 12);
        properties.put(ColorizeHandler.PN_GREEN, 12);
        properties.put(ColorizeHandler.PN_ALPHA, -.8);
        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        assertThrows(BadRequestException.class, () -> colorizer.handle(inputStream, outputStream, config));
    }
}
