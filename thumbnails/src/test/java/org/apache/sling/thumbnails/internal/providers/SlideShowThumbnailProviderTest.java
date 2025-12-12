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

import java.io.IOException;
import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.apache.sling.thumbnails.ThumbnailSupport;
import org.apache.sling.thumbnails.extension.ThumbnailProvider;
import org.apache.sling.thumbnails.internal.ContextHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SlingContextExtension.class)
public class SlideShowThumbnailProviderTest {

    private static final Logger log = LoggerFactory.getLogger(SlideShowThumbnailProviderTest.class);

    public SlingContext context = new SlingContext();

    private Resource docxFile;
    private Resource pptFile;
    private Resource pptxFile;

    private ThumbnailProvider provider;

    @BeforeEach
    public void init() {
        ContextHelper.initContext(context);
        docxFile = context.resourceResolver().getResource("/content/apache/sling-apache-org/index/Sling.docx");
        pptxFile = context.resourceResolver().getResource("/content/apache/sling-apache-org/index/Sling.pptx");
        pptFile = context.resourceResolver().getResource("/content/apache/sling-apache-org/index/Sling.ppt");

        ThumbnailSupport thumbnailSupport = mock(ThumbnailSupport.class);
        when(thumbnailSupport.getPersistableTypes()).thenReturn(Collections.emptySet());
        when(thumbnailSupport.getSupportedTypes()).thenReturn(Collections.singleton("sling:File"));
        when(thumbnailSupport.getMetaTypePropertyPath("sling:File")).thenReturn("jcr:content/jcr:mimeType");

        provider = new SlideShowThumbnailProvider(null, thumbnailSupport);
    }

    @Test
    public void testApplies() throws IOException {
        log.info("testApplies");
        assertTrue(provider.applies(
                pptxFile, "application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        assertTrue(provider.applies(pptFile, "application/vnd.ms-powerpoint"));
        assertFalse(
                provider.applies(docxFile, "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @Test
    public void testGetThumbnail() throws IOException {
        log.info("testGetThumbnail");
        assertNotNull(provider.getThumbnail(pptxFile));
        assertNotNull(provider.getThumbnail(pptFile));
    }
}
