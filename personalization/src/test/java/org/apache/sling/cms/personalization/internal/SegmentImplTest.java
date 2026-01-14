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
package org.apache.sling.cms.personalization.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SegmentImplTest {

    @Mock
    private Resource resource;

    @Test
    void testGetId_ReturnsResourceName() {
        when(resource.getName()).thenReturn("test-segment");
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<>()));

        SegmentImpl segment = new SegmentImpl(resource);

        assertEquals("test-segment", segment.getId());
    }

    @Test
    void testGetTitle_ReturnsTitleProperty() {
        when(resource.getName()).thenReturn("test-segment");

        Map<String, Object> props = new HashMap<>();
        props.put("title", "Test Segment Title");
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(props));

        SegmentImpl segment = new SegmentImpl(resource);

        assertEquals("Test Segment Title", segment.getTitle());
    }

    @Test
    void testGetTitle_DefaultsToIdWhenNoTitle() {
        when(resource.getName()).thenReturn("test-segment");
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<>()));

        SegmentImpl segment = new SegmentImpl(resource);

        assertEquals("test-segment", segment.getTitle());
    }

    @Test
    void testGetDescription_ReturnsDescriptionProperty() {
        when(resource.getName()).thenReturn("test-segment");

        Map<String, Object> props = new HashMap<>();
        props.put("description", "This is a test segment");
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(props));

        SegmentImpl segment = new SegmentImpl(resource);

        assertEquals("This is a test segment", segment.getDescription());
    }

    @Test
    void testGetDescription_ReturnsNullWhenNoDescription() {
        when(resource.getName()).thenReturn("test-segment");
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<>()));

        SegmentImpl segment = new SegmentImpl(resource);

        assertNull(segment.getDescription());
    }

    @Test
    void testGetPriority_ReturnsPriorityProperty() {
        when(resource.getName()).thenReturn("test-segment");

        Map<String, Object> props = new HashMap<>();
        props.put("priority", 50);
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(props));

        SegmentImpl segment = new SegmentImpl(resource);

        assertEquals(50, segment.getPriority());
    }

    @Test
    void testGetPriority_DefaultsToZeroWhenNoPriority() {
        when(resource.getName()).thenReturn("test-segment");
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<>()));

        SegmentImpl segment = new SegmentImpl(resource);

        assertEquals(0, segment.getPriority());
    }

    @Test
    void testGetPriority_HandlesHighPriority() {
        when(resource.getName()).thenReturn("test-segment");

        Map<String, Object> props = new HashMap<>();
        props.put("priority", 100);
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(props));

        SegmentImpl segment = new SegmentImpl(resource);

        assertEquals(100, segment.getPriority());
    }

    @Test
    void testGetPriority_HandlesNegativePriority() {
        when(resource.getName()).thenReturn("test-segment");

        Map<String, Object> props = new HashMap<>();
        props.put("priority", -10);
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(props));

        SegmentImpl segment = new SegmentImpl(resource);

        assertEquals(-10, segment.getPriority());
    }

    @Test
    void testGetEvaluatorType_ReturnsEvaluatorProperty() {
        when(resource.getName()).thenReturn("test-segment");

        Map<String, Object> props = new HashMap<>();
        props.put("evaluator", "device");
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(props));

        SegmentImpl segment = new SegmentImpl(resource);

        assertEquals("device", segment.getEvaluatorType());
    }

    @Test
    void testGetEvaluatorType_DefaultsToEmptyStringWhenNoEvaluator() {
        when(resource.getName()).thenReturn("test-segment");
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<>()));

        SegmentImpl segment = new SegmentImpl(resource);

        assertEquals("", segment.getEvaluatorType());
    }

    @Test
    void testToString_ContainsAllFields() {
        when(resource.getName()).thenReturn("mobile-users");

        Map<String, Object> props = new HashMap<>();
        props.put("title", "Mobile Users");
        props.put("priority", 75);
        props.put("evaluator", "device");
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(props));

        SegmentImpl segment = new SegmentImpl(resource);
        String result = segment.toString();

        assertNotNull(result);
        assertTrue(result.contains("mobile-users"));
        assertTrue(result.contains("Mobile Users"));
        assertTrue(result.contains("75"));
        assertTrue(result.contains("device"));
    }

    @Test
    void testFullSegment_AllPropertiesSet() {
        when(resource.getName()).thenReturn("premium-users");

        Map<String, Object> props = new HashMap<>();
        props.put("title", "Premium Users");
        props.put("description", "Users with premium subscription");
        props.put("priority", 90);
        props.put("evaluator", "userGroup");
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(props));

        SegmentImpl segment = new SegmentImpl(resource);

        assertEquals("premium-users", segment.getId());
        assertEquals("Premium Users", segment.getTitle());
        assertEquals("Users with premium subscription", segment.getDescription());
        assertEquals(90, segment.getPriority());
        assertEquals("userGroup", segment.getEvaluatorType());
    }

    @Test
    void testMinimalSegment_OnlyRequiredProperties() {
        when(resource.getName()).thenReturn("basic-segment");
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<>()));

        SegmentImpl segment = new SegmentImpl(resource);

        assertEquals("basic-segment", segment.getId());
        assertEquals("basic-segment", segment.getTitle()); // defaults to id
        assertNull(segment.getDescription());
        assertEquals(0, segment.getPriority());
        assertEquals("", segment.getEvaluatorType());
    }
}
