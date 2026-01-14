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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PathSegmentEvaluatorTest {

    @Mock
    private SlingHttpServletRequest request;

    private PathSegmentEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new PathSegmentEvaluator();
    }

    @Test
    void testGetType() {
        assertEquals("path", evaluator.getType());
    }

    @Test
    void testEvaluate_MatchingPath() {
        when(request.getPathInfo()).thenReturn("/content/mysite/en/products/widget");

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("pathPattern", "/content/mysite/.*/products/.*");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_NonMatchingPath() {
        when(request.getPathInfo()).thenReturn("/content/mysite/en/about");

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("pathPattern", "/content/mysite/.*/products/.*");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_NoPathPattern() {
        when(request.getPathInfo()).thenReturn("/content/mysite/en");

        Map<String, Object> rulesMap = new HashMap<>();
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_NullPathInfo_FallbackToRequestURI() {
        when(request.getPathInfo()).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/content/mysite/en/products/widget");

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("pathPattern", "/content/mysite/.*/products/.*");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_InvalidRegexPattern() {
        when(request.getPathInfo()).thenReturn("/content/mysite/en");

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("pathPattern", "[invalid(regex");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }
}
