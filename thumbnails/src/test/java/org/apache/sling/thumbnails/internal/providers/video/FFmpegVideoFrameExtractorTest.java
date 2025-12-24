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
package org.apache.sling.thumbnails.internal.providers.video;

import java.util.Set;

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

public class FFmpegVideoFrameExtractorTest {

    private static final Logger log = LoggerFactory.getLogger(FFmpegVideoFrameExtractorTest.class);

    private FFmpegVideoFrameExtractor extractor;

    @BeforeEach
    public void init() {
        extractor = new FFmpegVideoFrameExtractor();

        // Create mock config
        FFmpegVideoFrameExtractor.Config config = mock(FFmpegVideoFrameExtractor.Config.class);
        when(config.enabled()).thenReturn(true);
        when(config.ffmpeg_path()).thenReturn("");
        when(config.ffprobe_path()).thenReturn("");
        when(config.timeout_seconds()).thenReturn(30L);

        extractor.activate(config);
    }

    @Test
    public void testGetPriority() {
        log.info("testGetPriority");
        assertEquals(200, extractor.getPriority());
    }

    @Test
    public void testGetName() {
        log.info("testGetName");
        assertEquals("FFmpeg", extractor.getName());
    }

    @Test
    public void testGetSupportedTypes() {
        log.info("testGetSupportedTypes");
        Set<String> types = extractor.getSupportedTypes();

        assertNotNull(types);
        assertFalse(types.isEmpty());
        assertTrue(types.contains("video/mp4"));
        assertTrue(types.contains("video/quicktime"));
        assertTrue(types.contains("video/x-m4v"));
        assertTrue(types.contains("video/webm"));
        assertTrue(types.contains("video/x-msvideo"));
    }

    @Test
    public void testSupports_Mp4() {
        log.info("testSupports_Mp4");
        assertTrue(extractor.supports("video/mp4"));
    }

    @Test
    public void testSupports_Quicktime() {
        log.info("testSupports_Quicktime");
        assertTrue(extractor.supports("video/quicktime"));
    }

    @Test
    public void testSupports_M4v() {
        log.info("testSupports_M4v");
        assertTrue(extractor.supports("video/x-m4v"));
    }

    @Test
    public void testSupports_Webm() {
        log.info("testSupports_Webm");
        // FFmpeg supports WebM format
        assertTrue(extractor.supports("video/webm"));
    }

    @Test
    public void testSupports_Avi() {
        log.info("testSupports_Avi");
        assertTrue(extractor.supports("video/x-msvideo"));
    }

    @Test
    public void testNotSupports_Image() {
        log.info("testNotSupports_Image");
        assertFalse(extractor.supports("image/png"));
    }

    @Test
    public void testNotSupports_Audio() {
        log.info("testNotSupports_Audio");
        assertFalse(extractor.supports("audio/mp3"));
    }

    @Test
    public void testDisabledConfig() {
        log.info("testDisabledConfig");
        FFmpegVideoFrameExtractor disabledExtractor = new FFmpegVideoFrameExtractor();

        FFmpegVideoFrameExtractor.Config config = mock(FFmpegVideoFrameExtractor.Config.class);
        when(config.enabled()).thenReturn(false);
        when(config.ffmpeg_path()).thenReturn("");
        when(config.ffprobe_path()).thenReturn("");
        when(config.timeout_seconds()).thenReturn(30L);

        disabledExtractor.activate(config);

        assertFalse(disabledExtractor.isAvailable());
    }
}
