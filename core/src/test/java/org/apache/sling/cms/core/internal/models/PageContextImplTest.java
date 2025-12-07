/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.cms.core.internal.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.cms.CMSConstants;
import org.apache.sling.cms.PageContext;
import org.apache.sling.cms.publication.INSTANCE_TYPE;
import org.apache.sling.cms.publication.PublicationManagerFactory;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class PageContextImplTest {

    @Rule
    public final SlingContext context = new SlingContext();

    private PublicationManagerFactory publicationManagerFactory;

    @Before
    public void setup() {
        publicationManagerFactory = mock(PublicationManagerFactory.class);
        context.registerService(PublicationManagerFactory.class, publicationManagerFactory);
    }

    @Test
    public void testEditModeOnAuthor() {
        when(publicationManagerFactory.getInstanceType()).thenReturn(INSTANCE_TYPE.AUTHOR);
        context.request().setAttribute(CMSConstants.ATTR_EDIT_ENABLED, "true");

        PageContext pageContext = context.request().adaptTo(PageContext.class);

        assertTrue("Should be in edit mode", pageContext.isEditMode());
        assertFalse("Should not be in preview mode", pageContext.isPreviewMode());
        assertEquals(PageContext.PageMode.EDIT, pageContext.getPageMode());
        assertTrue("Should be on Author", pageContext.isAuthor());
        assertFalse("Should not be on Renderer", pageContext.isRenderer());
        assertFalse("Should not be on Standalone", pageContext.isStandalone());
        assertTrue("Should have authoring enabled", pageContext.isAuthoringEnabled());
        assertFalse("Should not be in publish mode", pageContext.isPublishMode());
    }

    @Test
    public void testPreviewModeOnAuthor() {
        when(publicationManagerFactory.getInstanceType()).thenReturn(INSTANCE_TYPE.AUTHOR);
        // No edit attribute set, so preview mode

        PageContext pageContext = context.request().adaptTo(PageContext.class);

        assertFalse("Should not be in edit mode", pageContext.isEditMode());
        assertTrue("Should be in preview mode", pageContext.isPreviewMode());
        assertEquals(PageContext.PageMode.PREVIEW, pageContext.getPageMode());
        assertTrue("Should be on Author", pageContext.isAuthor());
        assertFalse("Should not have authoring enabled in preview", pageContext.isAuthoringEnabled());
        assertTrue("Should be in publish mode", pageContext.isPublishMode());
    }

    @Test
    public void testRendererInstance() {
        when(publicationManagerFactory.getInstanceType()).thenReturn(INSTANCE_TYPE.RENDERER);

        PageContext pageContext = context.request().adaptTo(PageContext.class);

        assertFalse("Should be on Renderer, not Author", pageContext.isAuthor());
        assertTrue("Should be on Renderer", pageContext.isRenderer());
        assertFalse("Should not be on Standalone", pageContext.isStandalone());
        assertEquals(INSTANCE_TYPE.RENDERER, pageContext.getInstanceType());
        assertFalse("Should not have authoring enabled on Renderer", pageContext.isAuthoringEnabled());
        assertTrue("Should be in publish mode on Renderer", pageContext.isPublishMode());
    }

    @Test
    public void testStandaloneEditMode() {
        when(publicationManagerFactory.getInstanceType()).thenReturn(INSTANCE_TYPE.STANDALONE);
        context.request().setAttribute(CMSConstants.ATTR_EDIT_ENABLED, "true");

        PageContext pageContext = context.request().adaptTo(PageContext.class);

        assertTrue("Should be in edit mode", pageContext.isEditMode());
        assertTrue("Should be on Standalone", pageContext.isStandalone());
        assertTrue("Should have authoring enabled on Standalone in edit mode", pageContext.isAuthoringEnabled());
        assertFalse("Should not be in publish mode", pageContext.isPublishMode());
    }

    @Test
    public void testStandalonePreviewMode() {
        when(publicationManagerFactory.getInstanceType()).thenReturn(INSTANCE_TYPE.STANDALONE);
        // No edit attribute

        PageContext pageContext = context.request().adaptTo(PageContext.class);

        assertFalse("Should not be in edit mode", pageContext.isEditMode());
        assertTrue("Should be in preview mode", pageContext.isPreviewMode());
        assertTrue("Should be on Standalone", pageContext.isStandalone());
        assertFalse("Should not have authoring enabled in preview", pageContext.isAuthoringEnabled());
        assertTrue("Should be in publish mode", pageContext.isPublishMode());
    }

    @Test
    public void testEditEnabledWithBooleanTrue() {
        when(publicationManagerFactory.getInstanceType()).thenReturn(INSTANCE_TYPE.AUTHOR);
        context.request().setAttribute(CMSConstants.ATTR_EDIT_ENABLED, Boolean.TRUE);

        PageContext pageContext = context.request().adaptTo(PageContext.class);

        assertTrue("Should recognize Boolean.TRUE as edit mode", pageContext.isEditMode());
    }

    @Test
    public void testEditEnabledWithBooleanFalse() {
        when(publicationManagerFactory.getInstanceType()).thenReturn(INSTANCE_TYPE.AUTHOR);
        context.request().setAttribute(CMSConstants.ATTR_EDIT_ENABLED, Boolean.FALSE);

        PageContext pageContext = context.request().adaptTo(PageContext.class);

        assertFalse("Should recognize Boolean.FALSE as not edit mode", pageContext.isEditMode());
    }
}
