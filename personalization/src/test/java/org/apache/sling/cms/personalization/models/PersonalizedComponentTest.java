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
package org.apache.sling.cms.personalization.models;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.personalization.PersonalizationService;
import org.apache.sling.cms.personalization.Segment;
import org.apache.sling.cms.personalization.VariantResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PersonalizedComponentTest {

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private Resource resource;

    @Mock
    private PersonalizationService personalizationService;

    @Mock
    private VariantResolver variantResolver;

    private PersonalizedComponent component;

    @BeforeEach
    void setUp() throws Exception {
        component = new PersonalizedComponent();

        // Inject dependencies using reflection
        setField("request", request);
        setField("resource", resource);
        setField("personalizationService", personalizationService);
        setField("variantResolver", variantResolver);
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = PersonalizedComponent.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(component, value);
    }

    @Test
    void testGetResolvedVariant_WithMatch() {
        List<Segment> segments = createMockSegments("mobile-users");
        when(personalizationService.isEnabled()).thenReturn(true);
        when(personalizationService.resolveSegments(request, resource)).thenReturn(segments);

        Resource variant = mock(Resource.class);
        when(variantResolver.resolveVariant(resource, request)).thenReturn(variant);

        component.init();

        assertNotNull(component.getResolvedVariant());
        assertEquals(variant, component.getResolvedVariant());
    }

    @Test
    void testGetResolvedVariant_NoMatch() {
        when(personalizationService.isEnabled()).thenReturn(true);
        when(personalizationService.resolveSegments(request, resource)).thenReturn(Collections.emptyList());
        when(variantResolver.resolveVariant(resource, request)).thenReturn(null);

        component.init();

        assertNull(component.getResolvedVariant());
    }

    @Test
    void testGetResolvedVariant_PersonalizationDisabled() {
        when(personalizationService.isEnabled()).thenReturn(false);

        component.init();

        assertNull(component.getResolvedVariant());
    }

    @Test
    void testGetContent_WithVariant() {
        List<Segment> segments = createMockSegments("mobile-users");
        when(personalizationService.isEnabled()).thenReturn(true);
        when(personalizationService.resolveSegments(request, resource)).thenReturn(segments);

        Resource variant = mock(Resource.class);
        when(variantResolver.resolveVariant(resource, request)).thenReturn(variant);

        component.init();

        assertEquals(variant, component.getContent());
    }

    @Test
    void testGetContent_FallbackToDefault() {
        when(personalizationService.isEnabled()).thenReturn(true);
        when(personalizationService.resolveSegments(request, resource)).thenReturn(Collections.emptyList());
        when(variantResolver.resolveVariant(resource, request)).thenReturn(null);

        Resource defaultVariant = mock(Resource.class);
        when(variantResolver.getDefaultVariant(resource)).thenReturn(defaultVariant);

        component.init();

        assertEquals(defaultVariant, component.getContent());
    }

    @Test
    void testGetResolvedVariantName_WithVariant() {
        List<Segment> segments = createMockSegments("mobile-users");
        when(personalizationService.isEnabled()).thenReturn(true);
        when(personalizationService.resolveSegments(request, resource)).thenReturn(segments);

        Resource variant = mock(Resource.class);
        when(variant.getName()).thenReturn("mobile-variant");
        when(variantResolver.resolveVariant(resource, request)).thenReturn(variant);

        component.init();

        assertEquals("mobile-variant", component.getResolvedVariantName());
    }

    @Test
    void testGetResolvedVariantName_NoVariant() {
        when(personalizationService.isEnabled()).thenReturn(true);
        when(personalizationService.resolveSegments(request, resource)).thenReturn(Collections.emptyList());
        when(variantResolver.resolveVariant(resource, request)).thenReturn(null);

        component.init();

        assertEquals("none", component.getResolvedVariantName());
    }

    @Test
    void testGetMatchedSegments() {
        when(personalizationService.isEnabled()).thenReturn(true);

        List<Segment> segments = createMockSegments("seg1", "seg2");
        when(personalizationService.resolveSegments(request, resource)).thenReturn(segments);

        component.init();

        List<Segment> result = component.getMatchedSegments();
        assertEquals(2, result.size());
    }

    @Test
    void testGetMatchedSegments_Empty() {
        when(personalizationService.isEnabled()).thenReturn(true);
        when(personalizationService.resolveSegments(request, resource)).thenReturn(Collections.emptyList());

        component.init();

        assertTrue(component.getMatchedSegments().isEmpty());
    }

    @Test
    void testGetMatchedSegmentIds_WithSegments() {
        when(personalizationService.isEnabled()).thenReturn(true);

        List<Segment> segments = createMockSegments("mobile-users", "premium-users");
        when(personalizationService.resolveSegments(request, resource)).thenReturn(segments);

        component.init();

        String result = component.getMatchedSegmentIds();
        assertTrue(result.contains("mobile-users"));
        assertTrue(result.contains("premium-users"));
        assertTrue(result.contains(", "));
    }

    @Test
    void testGetMatchedSegmentIds_NoSegments() {
        when(personalizationService.isEnabled()).thenReturn(true);
        when(personalizationService.resolveSegments(request, resource)).thenReturn(Collections.emptyList());

        component.init();

        assertEquals("none", component.getMatchedSegmentIds());
    }

    @Test
    void testIsHasVariants_True() {
        when(variantResolver.hasVariants(resource)).thenReturn(true);

        assertTrue(component.isHasVariants());
    }

    @Test
    void testIsHasVariants_False() {
        when(variantResolver.hasVariants(resource)).thenReturn(false);

        assertFalse(component.isHasVariants());
    }

    @Test
    void testIsPersonalizationEnabled_True() {
        when(personalizationService.isEnabled()).thenReturn(true);

        assertTrue(component.isPersonalizationEnabled());
    }

    @Test
    void testIsPersonalizationEnabled_False() {
        when(personalizationService.isEnabled()).thenReturn(false);

        assertFalse(component.isPersonalizationEnabled());
    }

    @Test
    void testGetAllVariants() {
        List<Resource> variants = Arrays.asList(mock(Resource.class), mock(Resource.class));
        when(variantResolver.getVariants(resource)).thenReturn(variants);

        List<Resource> result = component.getAllVariants();

        assertEquals(2, result.size());
    }

    @Test
    void testGetDefaultVariant() {
        Resource defaultVariant = mock(Resource.class);
        when(variantResolver.getDefaultVariant(resource)).thenReturn(defaultVariant);

        assertEquals(defaultVariant, component.getDefaultVariant());
    }

    @Test
    void testGetResource() {
        assertEquals(resource, component.getResource());
    }

    @Test
    void testNullPersonalizationService() throws Exception {
        setField("personalizationService", null);

        component.init();

        assertNull(component.getResolvedVariant());
        assertTrue(component.getMatchedSegments().isEmpty());
        assertFalse(component.isPersonalizationEnabled());
    }

    @Test
    void testNullVariantResolver() throws Exception {
        setField("variantResolver", null);

        when(personalizationService.isEnabled()).thenReturn(true);
        when(personalizationService.resolveSegments(request, resource)).thenReturn(Collections.emptyList());

        component.init();

        assertNull(component.getResolvedVariant());
        assertFalse(component.isHasVariants());
        assertTrue(component.getAllVariants().isEmpty());
        assertNull(component.getDefaultVariant());
    }

    private List<Segment> createMockSegments(String... ids) {
        List<Segment> segments = new ArrayList<>();
        for (String id : ids) {
            Segment segment = mock(Segment.class);
            when(segment.getId()).thenReturn(id);
            segments.add(segment);
        }
        return segments;
    }
}
