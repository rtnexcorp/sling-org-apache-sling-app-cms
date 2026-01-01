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
package org.apache.sling.cms.core.internal.models;

import org.apache.sling.cms.CMSConstants;
import org.apache.sling.cms.PageContext;
import org.apache.sling.cms.publication.INSTANCE_TYPE;
import org.apache.sling.cms.publication.PublicationManagerFactory;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SlingContextExtension.class)
public class PageContextImplTest {

    public SlingContext context = new SlingContext();

    private PublicationManagerFactory publicationManagerFactory;

    @BeforeEach
    public void setup() {
        publicationManagerFactory = mock(PublicationManagerFactory.class);
        context.registerService(PublicationManagerFactory.class, publicationManagerFactory);
    }

    @Test
    public void testEditModeOnAuthor() {
        when(publicationManagerFactory.getInstanceType()).thenReturn(INSTANCE_TYPE.AUTHOR);
        context.request().setAttribute(CMSConstants.ATTR_EDIT_ENABLED, "true");

        PageContext pageContext = context.request().adaptTo(PageContext.class);

        assertTrue(pageContext.isEditMode(), "Should be in edit mode");
        assertFalse(pageContext.isPreviewMode(), "Should not be in preview mode");
        assertEquals(PageContext.PageMode.EDIT, pageContext.getPageMode());
        assertTrue(pageContext.isAuthor(), "Should be on Author");
        assertFalse(pageContext.isRenderer(), "Should not be on Renderer");
        assertFalse(pageContext.isStandalone(), "Should not be on Standalone");
        assertTrue(pageContext.isAuthoringEnabled(), "Should have authoring enabled");
        assertFalse(pageContext.isPublishMode(), "Should not be in publish mode");
    }

    @Test
    public void testPreviewModeOnAuthor() {
        when(publicationManagerFactory.getInstanceType()).thenReturn(INSTANCE_TYPE.AUTHOR);
        // No edit attribute set, so preview mode

        PageContext pageContext = context.request().adaptTo(PageContext.class);

        assertFalse(pageContext.isEditMode(), "Should not be in edit mode");
        assertTrue(pageContext.isPreviewMode(), "Should be in preview mode");
        assertEquals(PageContext.PageMode.PREVIEW, pageContext.getPageMode());
        assertTrue(pageContext.isAuthor(), "Should be on Author");
        assertFalse(pageContext.isAuthoringEnabled(), "Should not have authoring enabled in preview");
        assertTrue(pageContext.isPublishMode(), "Should be in publish mode");
    }

    @Test
    public void testRendererInstance() {
        when(publicationManagerFactory.getInstanceType()).thenReturn(INSTANCE_TYPE.RENDERER);

        PageContext pageContext = context.request().adaptTo(PageContext.class);

        assertFalse(pageContext.isAuthor(), "Should be on Renderer, not Author");
        assertTrue(pageContext.isRenderer(), "Should be on Renderer");
        assertFalse(pageContext.isStandalone(), "Should not be on Standalone");
        assertEquals(INSTANCE_TYPE.RENDERER, pageContext.getInstanceType());
        assertFalse(pageContext.isAuthoringEnabled(), "Should not have authoring enabled on Renderer");
        assertTrue(pageContext.isPublishMode(), "Should be in publish mode on Renderer");
    }

    @Test
    public void testStandaloneEditMode() {
        when(publicationManagerFactory.getInstanceType()).thenReturn(INSTANCE_TYPE.STANDALONE);
        context.request().setAttribute(CMSConstants.ATTR_EDIT_ENABLED, "true");

        PageContext pageContext = context.request().adaptTo(PageContext.class);

        assertTrue(pageContext.isEditMode(), "Should be in edit mode");
        assertTrue(pageContext.isStandalone(), "Should be on Standalone");
        assertTrue(pageContext.isAuthoringEnabled(), "Should have authoring enabled on Standalone in edit mode");
        assertFalse(pageContext.isPublishMode(), "Should not be in publish mode");
    }

    @Test
    public void testStandalonePreviewMode() {
        when(publicationManagerFactory.getInstanceType()).thenReturn(INSTANCE_TYPE.STANDALONE);
        // No edit attribute

        PageContext pageContext = context.request().adaptTo(PageContext.class);

        assertFalse(pageContext.isEditMode(), "Should not be in edit mode");
        assertTrue(pageContext.isPreviewMode(), "Should be in preview mode");
        assertTrue(pageContext.isStandalone(), "Should be on Standalone");
        assertFalse(pageContext.isAuthoringEnabled(), "Should not have authoring enabled in preview");
        assertTrue(pageContext.isPublishMode(), "Should be in publish mode");
    }

    @Test
    public void testInstanceTypeDefaultsToStandalone() {
        when(publicationManagerFactory.getInstanceType()).thenReturn(null);

        PageContext pageContext = context.request().adaptTo(PageContext.class);

        // When no instance type is configured, it should default to standalone
        assertTrue(pageContext.isStandalone(), "Should default to Standalone");
    }
}
