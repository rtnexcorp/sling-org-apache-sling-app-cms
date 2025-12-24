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
package org.apache.sling.thumbnails.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutoRenditionConfigImplTest {

    private AutoRenditionConfigImpl config;

    @BeforeEach
    void setUp() {
        config = new AutoRenditionConfigImpl();
    }

    @Test
    void testDefaultConfiguration() {
        AutoRenditionConfigImpl.Config mockConfig = mock(AutoRenditionConfigImpl.Config.class);
        when(mockConfig.enabled()).thenReturn(true);
        when(mockConfig.transformationNames()).thenReturn(new String[] {"thumbnail"});
        when(mockConfig.supportedMimeTypes()).thenReturn(new String[] {"image/*"});
        when(mockConfig.contentPaths()).thenReturn(new String[] {"/content", "/static"});

        config.activate(mockConfig);

        assertTrue(config.isEnabled());
        assertArrayEquals(new String[] {"thumbnail"}, config.getTransformationNames());
        assertArrayEquals(new String[] {"image/*"}, config.getSupportedMimeTypes());
        assertArrayEquals(new String[] {"/content", "/static"}, config.getContentPaths());
    }

    @Test
    void testDisabledConfiguration() {
        AutoRenditionConfigImpl.Config mockConfig = mock(AutoRenditionConfigImpl.Config.class);
        when(mockConfig.enabled()).thenReturn(false);
        when(mockConfig.transformationNames()).thenReturn(new String[0]);
        when(mockConfig.supportedMimeTypes()).thenReturn(new String[0]);
        when(mockConfig.contentPaths()).thenReturn(new String[0]);

        config.activate(mockConfig);

        assertFalse(config.isEnabled());
        assertEquals(0, config.getTransformationNames().length);
        assertEquals(0, config.getSupportedMimeTypes().length);
        assertEquals(0, config.getContentPaths().length);
    }

    @Test
    void testCustomConfiguration() {
        AutoRenditionConfigImpl.Config mockConfig = mock(AutoRenditionConfigImpl.Config.class);
        when(mockConfig.enabled()).thenReturn(true);
        when(mockConfig.transformationNames()).thenReturn(new String[] {"thumbnail", "medium", "large"});
        when(mockConfig.supportedMimeTypes()).thenReturn(new String[] {"image/*", "video/*"});
        when(mockConfig.contentPaths()).thenReturn(new String[] {"/content/dam"});

        config.activate(mockConfig);

        assertTrue(config.isEnabled());
        assertArrayEquals(new String[] {"thumbnail", "medium", "large"}, config.getTransformationNames());
        assertArrayEquals(new String[] {"image/*", "video/*"}, config.getSupportedMimeTypes());
        assertArrayEquals(new String[] {"/content/dam"}, config.getContentPaths());
    }

    @Test
    void testNullArraysReturnEmpty() {
        AutoRenditionConfigImpl.Config mockConfig = mock(AutoRenditionConfigImpl.Config.class);
        when(mockConfig.enabled()).thenReturn(true);
        when(mockConfig.transformationNames()).thenReturn(null);
        when(mockConfig.supportedMimeTypes()).thenReturn(null);
        when(mockConfig.contentPaths()).thenReturn(null);

        config.activate(mockConfig);

        assertNotNull(config.getTransformationNames());
        assertNotNull(config.getSupportedMimeTypes());
        assertNotNull(config.getContentPaths());
        assertEquals(0, config.getTransformationNames().length);
        assertEquals(0, config.getSupportedMimeTypes().length);
        assertEquals(0, config.getContentPaths().length);
    }

    @Test
    void testModifiedConfiguration() {
        // Initial config
        AutoRenditionConfigImpl.Config initialConfig = mock(AutoRenditionConfigImpl.Config.class);
        when(initialConfig.enabled()).thenReturn(true);
        when(initialConfig.transformationNames()).thenReturn(new String[] {"thumbnail"});
        when(initialConfig.supportedMimeTypes()).thenReturn(new String[] {"image/*"});
        when(initialConfig.contentPaths()).thenReturn(new String[] {"/content"});

        config.activate(initialConfig);
        assertTrue(config.isEnabled());

        // Modified config
        AutoRenditionConfigImpl.Config modifiedConfig = mock(AutoRenditionConfigImpl.Config.class);
        when(modifiedConfig.enabled()).thenReturn(false);
        when(modifiedConfig.transformationNames()).thenReturn(new String[] {"large"});
        when(modifiedConfig.supportedMimeTypes()).thenReturn(new String[] {"video/*"});
        when(modifiedConfig.contentPaths()).thenReturn(new String[] {"/static"});

        config.activate(modifiedConfig);

        assertFalse(config.isEnabled());
        assertArrayEquals(new String[] {"large"}, config.getTransformationNames());
        assertArrayEquals(new String[] {"video/*"}, config.getSupportedMimeTypes());
        assertArrayEquals(new String[] {"/static"}, config.getContentPaths());
    }
}
