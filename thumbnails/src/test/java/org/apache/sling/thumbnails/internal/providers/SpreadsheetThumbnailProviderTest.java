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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
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

@ExtendWith(SlingContextExtension.class)
public class SpreadsheetThumbnailProviderTest {

    private static final Logger log = LoggerFactory.getLogger(SpreadsheetThumbnailProviderTest.class);

    public SlingContext context = new SlingContext();

    private Resource xlsxFile;
    private Resource docxFile;
    private Resource pngFile;

    private ThumbnailProvider provider;

    @BeforeEach
    public void init() {
        ContextHelper.initContext(context);
        xlsxFile = context.resourceResolver().getResource("/content/apache/sling-apache-org/index/Sling.xlsx");
        docxFile = context.resourceResolver().getResource("/content/apache/sling-apache-org/index/Sling.docx");
        pngFile = context.resourceResolver().getResource("/content/apache/sling-apache-org/index/apache.png");

        provider = new SpreadsheetThumbnailProvider();
    }

    @Test
    public void testAppliesXlsx() throws IOException {
        log.info("testAppliesXlsx");
        assertTrue(provider.applies(xlsxFile, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    public void testAppliesXls() throws IOException {
        log.info("testAppliesXls");
        assertTrue(provider.applies(xlsxFile, "application/vnd.ms-excel"));
    }

    @Test
    public void testDoesNotApplyToDocx() throws IOException {
        log.info("testDoesNotApplyToDocx");
        assertFalse(
                provider.applies(docxFile, "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        assertFalse(provider.applies(docxFile, "application/msword"));
    }

    @Test
    public void testDoesNotApplyToImage() throws IOException {
        log.info("testDoesNotApplyToImage");
        assertFalse(provider.applies(pngFile, "image/png"));
    }

    @Test
    public void testGetThumbnailXlsx() throws IOException {
        log.info("testGetThumbnailXlsx");
        assertNotNull(provider.getThumbnail(xlsxFile));
    }
}
