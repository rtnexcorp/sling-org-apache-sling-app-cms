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

import org.apache.sling.cms.preview.PreviewToken;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviewTokenImplTest {

    @Test
    void testGetToken() {
        PreviewToken token = new PreviewTokenImpl("test-token-123", null, "testuser");
        assertEquals("test-token-123", token.getToken());
    }

    @Test
    void testGetExpiry() {
        Calendar expiry = Calendar.getInstance();
        expiry.add(Calendar.HOUR, 1);

        PreviewToken token = new PreviewTokenImpl("test-token", expiry, "testuser");
        assertEquals(expiry, token.getExpiry());
    }

    @Test
    void testGetCreatedBy() {
        PreviewToken token = new PreviewTokenImpl("test-token", null, "testuser");
        assertEquals("testuser", token.getCreatedBy());
    }

    @Test
    void testIsValidWithFutureExpiry() {
        Calendar expiry = Calendar.getInstance();
        expiry.add(Calendar.HOUR, 1);

        PreviewToken token = new PreviewTokenImpl("test-token", expiry, "testuser");
        assertTrue(token.isValid());
        assertFalse(token.isExpired());
    }

    @Test
    void testIsValidWithPastExpiry() {
        Calendar expiry = Calendar.getInstance();
        expiry.add(Calendar.HOUR, -1); // 1 hour ago

        PreviewToken token = new PreviewTokenImpl("test-token", expiry, "testuser");
        assertFalse(token.isValid());
        assertTrue(token.isExpired());
    }

    @Test
    void testIsValidWithNoExpiry() {
        PreviewToken token = new PreviewTokenImpl("test-token", null, "testuser");
        assertTrue(token.isValid());
        assertFalse(token.isExpired());
    }

    @Test
    void testNullCreatedBy() {
        PreviewToken token = new PreviewTokenImpl("test-token", null, null);
        assertNull(token.getCreatedBy());
    }
}
