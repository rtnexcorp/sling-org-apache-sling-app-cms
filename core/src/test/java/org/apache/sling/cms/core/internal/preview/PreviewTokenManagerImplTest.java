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
package org.apache.sling.cms.core.internal.preview;

import java.util.Calendar;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.CMSConstants;
import org.apache.sling.cms.preview.PreviewToken;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SlingContextExtension.class)
class PreviewTokenManagerImplTest {

    private final SlingContext context = new SlingContext();
    private PreviewTokenManagerImpl tokenManager;
    private Resource pageResource;

    @BeforeEach
    void setUp() {
        tokenManager = new PreviewTokenManagerImpl();

        // Create a test page structure
        pageResource =
                context.create().resource("/content/test/page", JcrConstants.JCR_PRIMARYTYPE, CMSConstants.NT_PAGE);
        context.create()
                .resource(
                        "/content/test/page/" + JcrConstants.JCR_CONTENT,
                        JcrConstants.JCR_PRIMARYTYPE,
                        JcrConstants.NT_UNSTRUCTURED);
    }

    @Test
    void testGenerateToken() throws Exception {
        PreviewToken token = tokenManager.generateToken(pageResource, 3600);

        assertNotNull(token);
        assertNotNull(token.getToken());
        assertFalse(token.getToken().isEmpty());
        assertNotNull(token.getExpiry());
        assertFalse(token.isExpired());
        assertTrue(token.isValid());
    }

    @Test
    void testGenerateTokenWithNoExpiry() throws Exception {
        PreviewToken token = tokenManager.generateToken(pageResource, 0);

        assertNotNull(token);
        assertNotNull(token.getToken());
        assertNull(token.getExpiry());
        assertFalse(token.isExpired());
        assertTrue(token.isValid());
    }

    @Test
    void testGetToken() throws Exception {
        // Generate a token first
        PreviewToken generated = tokenManager.generateToken(pageResource, 3600);

        // Retrieve it
        PreviewToken retrieved = tokenManager.getToken(pageResource);

        assertNotNull(retrieved);
        assertEquals(generated.getToken(), retrieved.getToken());
    }

    @Test
    void testGetTokenWhenNoneExists() {
        PreviewToken token = tokenManager.getToken(pageResource);
        assertNull(token);
    }

    @Test
    void testValidateToken() throws Exception {
        PreviewToken token = tokenManager.generateToken(pageResource, 3600);

        boolean isValid = tokenManager.validateToken(pageResource, token.getToken());
        assertTrue(isValid);
    }

    @Test
    void testValidateInvalidToken() throws Exception {
        tokenManager.generateToken(pageResource, 3600);

        boolean isValid = tokenManager.validateToken(pageResource, "invalid-token");
        assertFalse(isValid);
    }

    @Test
    void testValidateExpiredToken() throws Exception {
        // Generate token with negative timeout (expired)
        PreviewToken token = tokenManager.generateToken(pageResource, -1);

        boolean isValid = tokenManager.validateToken(pageResource, token.getToken());
        assertFalse(isValid);
    }

    @Test
    void testRevokeToken() throws Exception {
        tokenManager.generateToken(pageResource, 3600);

        // Verify token exists
        assertNotNull(tokenManager.getToken(pageResource));

        // Revoke it
        tokenManager.revokeToken(pageResource);

        // Verify it's gone
        assertNull(tokenManager.getToken(pageResource));
    }

    @Test
    void testGeneratePreviewUrl() throws Exception {
        String previewUrl = tokenManager.generatePreviewUrl(pageResource, 3600);

        assertNotNull(previewUrl);
        assertTrue(previewUrl.contains(CMSConstants.PARAM_PREVIEW_TOKEN));
        assertTrue(previewUrl.contains("="));
        assertTrue(previewUrl.startsWith("/content/test/page.html?"));
    }

    @Test
    void testTokenPersistence() throws Exception {
        PreviewToken token = tokenManager.generateToken(pageResource, 3600);

        // Verify token is stored in jcr:content
        Resource contentResource = pageResource.getChild(JcrConstants.JCR_CONTENT);
        assertNotNull(contentResource);

        String storedToken = contentResource.getValueMap().get(CMSConstants.PN_PREVIEW_TOKEN, String.class);
        assertEquals(token.getToken(), storedToken);

        Calendar storedExpiry = contentResource.getValueMap().get(CMSConstants.PN_PREVIEW_EXPIRY, Calendar.class);
        assertNotNull(storedExpiry);
    }
}
