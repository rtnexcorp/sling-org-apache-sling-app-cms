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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceSegmentEvaluatorTest {

    @Mock
    private SlingHttpServletRequest request;

    private DeviceSegmentEvaluator evaluator;

    private static final String UA_MOBILE = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) Mobile Safari";
    private static final String UA_TABLET = "Mozilla/5.0 (iPad; CPU OS 14_0 like Mac OS X) Safari";
    private static final String UA_ANDROID_TABLET = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Tablet";
    private static final String UA_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/91.0";

    @BeforeEach
    void setUp() {
        evaluator = new DeviceSegmentEvaluator();
    }

    @Test
    void testGetType() {
        assertEquals("device", evaluator.getType());
    }

    @Test
    void testEvaluate_MobileDevice() {
        when(request.getHeader("User-Agent")).thenReturn(UA_MOBILE);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("deviceType", "mobile");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_TabletDevice() {
        when(request.getHeader("User-Agent")).thenReturn(UA_TABLET);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("deviceType", "tablet");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_AndroidTablet() {
        when(request.getHeader("User-Agent")).thenReturn(UA_ANDROID_TABLET);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("deviceType", "tablet");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_DesktopDevice() {
        when(request.getHeader("User-Agent")).thenReturn(UA_DESKTOP);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("deviceType", "desktop");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_MobileNotMatchingTablet() {
        when(request.getHeader("User-Agent")).thenReturn(UA_MOBILE);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("deviceType", "tablet");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_NullUserAgent() {
        when(request.getHeader("User-Agent")).thenReturn(null);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("deviceType", "mobile");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_NoDeviceType() {
        when(request.getHeader("User-Agent")).thenReturn(UA_MOBILE);

        Map<String, Object> rulesMap = new HashMap<>();
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_InvalidDeviceType() {
        when(request.getHeader("User-Agent")).thenReturn(UA_MOBILE);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("deviceType", "wearable");
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }
}
