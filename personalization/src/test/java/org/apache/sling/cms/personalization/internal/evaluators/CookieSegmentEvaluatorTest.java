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
package org.apache.sling.cms.personalization.internal.evaluators;

import javax.servlet.http.Cookie;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CookieSegmentEvaluatorTest {

    @Mock
    private SlingHttpServletRequest request;

    private CookieSegmentEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new CookieSegmentEvaluator();
    }

    @Test
    void testGetType() {
        assertEquals("cookie", evaluator.getType());
    }

    @Test
    void testEvaluate_CookiePresence() {
        Cookie[] cookies = {new Cookie("visited", "true")};
        when(request.getCookies()).thenReturn(cookies);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("cookieName", "visited");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_CookieNotPresent() {
        Cookie[] cookies = {new Cookie("other", "value")};
        when(request.getCookies()).thenReturn(cookies);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("cookieName", "visited");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_CookieValueExactMatch() {
        Cookie[] cookies = {new Cookie("customer_tier", "vip")};
        when(request.getCookies()).thenReturn(cookies);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("cookieName", "customer_tier");
        rulesMap.put("cookieValue", "vip");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_CookieValueMismatch() {
        Cookie[] cookies = {new Cookie("customer_tier", "regular")};
        when(request.getCookies()).thenReturn(cookies);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("cookieName", "customer_tier");
        rulesMap.put("cookieValue", "vip");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_CookieValuePatternMatch() {
        Cookie[] cookies = {new Cookie("subscription", "premium")};
        when(request.getCookies()).thenReturn(cookies);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("cookieName", "subscription");
        rulesMap.put("matchPattern", "(premium|enterprise)");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_NoCookies() {
        when(request.getCookies()).thenReturn(null);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("cookieName", "visited");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_NoCookieName() {
        Map<String, Object> rulesMap = new HashMap<>();
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }
}
