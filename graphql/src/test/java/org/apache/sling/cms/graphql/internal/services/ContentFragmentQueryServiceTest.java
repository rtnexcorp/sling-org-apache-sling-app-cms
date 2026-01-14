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
package org.apache.sling.cms.graphql.internal.services;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentFragmentQueryServiceTest {

    @Test
    void testEscapeJcrContainsTermEscapesSingleQuotesAndWildcards() throws Exception {
        ContentFragmentQueryService service = new ContentFragmentQueryService();

        Method m = ContentFragmentQueryService.class.getDeclaredMethod("escapeJcrContainsTerm", String.class);
        m.setAccessible(true);

        String escaped = (String) m.invoke(service, "a'b*c?d");

        // single quotes doubled for JCR-SQL2 string literal
        assertTrue(escaped.contains("a''b"));
        // wildcard characters escaped for CONTAINS
        assertTrue(escaped.contains("\\*"));
        assertTrue(escaped.contains("\\?"));
    }

    @Test
    void testEscapeJcrContainsTermNormalizesWhitespace() throws Exception {
        ContentFragmentQueryService service = new ContentFragmentQueryService();

        Method m = ContentFragmentQueryService.class.getDeclaredMethod("escapeJcrContainsTerm", String.class);
        m.setAccessible(true);

        String escaped = (String) m.invoke(service, "  hello   world  ");

        // normalized/trimmed
        assertEquals("hello world", escaped);
    }
}
