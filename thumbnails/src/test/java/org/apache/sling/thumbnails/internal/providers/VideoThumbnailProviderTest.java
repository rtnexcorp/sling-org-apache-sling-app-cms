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

import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.apache.sling.thumbnails.extension.VideoExtractionConfig;
import org.apache.sling.thumbnails.extension.VideoFrameExtractor;
import org.apache.sling.thumbnails.internal.ContextHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(SlingContextExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VideoThumbnailProviderTest {

    private static final Logger log = LoggerFactory.getLogger(VideoThumbnailProviderTest.class);

    public SlingContext context = new SlingContext();

    private Resource imageFile;
    private Resource pdfFile;

    private VideoThumbnailProvider provider;

    @BeforeEach
    public void init() {
        ContextHelper.initContext(context);

        imageFile = context.resourceResolver().getResource("/content/apache/sling-apache-org/index/apache.png");
        pdfFile = context.resourceResolver().getResource("/content/apache/sling-apache-org/index/sling.pdf");

        provider = new VideoThumbnailProvider();

        // Create and configure a mock config
        VideoThumbnailProvider.Config config = mock(VideoThumbnailProvider.Config.class);
        lenient().when(config.samplePositions()).thenReturn(new int[] {10, 25, 50});
        lenient().when(config.useSharpnessDetection()).thenReturn(true);
        lenient().when(config.useFaceDetection()).thenReturn(false);
        lenient().when(config.timeoutMs()).thenReturn(30000L);

        provider.activate(config);
    }

    @Test
    public void testApplies_VideoMp4() {
        log.info("testApplies_VideoMp4");
        assertTrue(provider.applies(imageFile, "video/mp4"));
    }

    @Test
    public void testApplies_VideoQuicktime() {
        log.info("testApplies_VideoQuicktime");
        assertTrue(provider.applies(imageFile, "video/quicktime"));
    }

    @Test
    public void testApplies_VideoWebm() {
        log.info("testApplies_VideoWebm");
        assertTrue(provider.applies(imageFile, "video/webm"));
    }

    @Test
    public void testApplies_VideoMkv() {
        log.info("testApplies_VideoMkv");
        assertTrue(provider.applies(imageFile, "video/x-matroska"));
    }

    @Test
    public void testNotApplies_Image() {
        log.info("testNotApplies_Image");
        assertFalse(provider.applies(imageFile, "image/png"));
    }

    @Test
    public void testNotApplies_Pdf() {
        log.info("testNotApplies_Pdf");
        assertFalse(provider.applies(pdfFile, "application/pdf"));
    }

    @Test
    public void testExtractorBinding() {
        log.info("testExtractorBinding");

        // Create a mock extractor
        VideoFrameExtractor mockExtractor = mock(VideoFrameExtractor.class);
        lenient().when(mockExtractor.isAvailable()).thenReturn(true);
        lenient().when(mockExtractor.getPriority()).thenReturn(100);
        lenient().when(mockExtractor.getName()).thenReturn("Mock Extractor");
        lenient().when(mockExtractor.getSupportedTypes()).thenReturn(Set.of("video/mp4"));

        // Bind the extractor
        provider.bindExtractor(mockExtractor);

        // Unbind the extractor
        provider.unbindExtractor(mockExtractor);
    }

    @Test
    public void testVideoExtractionConfigDefaults() {
        log.info("testVideoExtractionConfigDefaults");

        VideoExtractionConfig defaultConfig = VideoExtractionConfig.defaultConfig();

        assertNotNull(defaultConfig);
        assertNotNull(defaultConfig.getSamplePositions());
        assertTrue(defaultConfig.getSamplePositions().length > 0);
        assertTrue(defaultConfig.getTimeoutMs() > 0);
    }

    @Test
    public void testVideoExtractionConfigOf() {
        log.info("testVideoExtractionConfigOf");

        int[] positions = {5, 15, 30};
        VideoExtractionConfig customConfig = VideoExtractionConfig.of(positions, true, false, 5000L);

        assertNotNull(customConfig);
        assertTrue(customConfig.useFaceDetection());
        assertFalse(customConfig.useSharpnessDetection());
        assertTrue(customConfig.getTimeoutMs() == 5000L);
    }
}
