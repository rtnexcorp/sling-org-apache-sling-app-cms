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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.cms.personalization.PersonalizationService;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VariantResolverImplTest {

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private Resource contentResource;

    @Mock
    private PersonalizationService personalizationService;

    private VariantResolverImpl resolver;

    @BeforeEach
    void setUp() throws Exception {
        resolver = new VariantResolverImpl();

        // Inject PersonalizationService
        Field serviceField = VariantResolverImpl.class.getDeclaredField("personalizationService");
        serviceField.setAccessible(true);
        serviceField.set(resolver, personalizationService);
    }

    @Test
    void testResolveVariant_NoVariants() {
        when(contentResource.getChild("variants")).thenReturn(null);

        Resource result = resolver.resolveVariant(contentResource, request);

        assertNull(result);
    }

    @Test
    void testResolveVariant_WithMatchingSegment() {
        // Setup matched segments
        when(personalizationService.evaluateSegments(request)).thenReturn(Arrays.asList("mobile-users"));

        // Setup variants
        Resource variantsNode = mock(Resource.class);
        Resource mobileVariant = createMockVariant("mobile", new String[] {"mobile-users"}, 50, false);
        Resource defaultVariant = createMockVariant("default", null, 0, true);

        List<Resource> variants = Arrays.asList(mobileVariant, defaultVariant);
        when(variantsNode.getChildren()).thenReturn(variants);
        lenient().when(variantsNode.listChildren()).thenReturn(variants.iterator());
        lenient().when(variantsNode.hasChildren()).thenReturn(true);
        when(contentResource.getChild("variants")).thenReturn(variantsNode);

        Resource result = resolver.resolveVariant(contentResource, request);

        assertNotNull(result);
        assertEquals("mobile", result.getName());
    }

    @Test
    void testResolveVariant_NoMatchingSegment_ReturnsDefault() {
        // Setup matched segments that don't match any variant
        when(personalizationService.evaluateSegments(request)).thenReturn(Arrays.asList("tablet-users"));

        // Setup variants
        Resource variantsNode = mock(Resource.class);
        Resource mobileVariant = createMockVariant("mobile", new String[] {"mobile-users"}, 50, false);
        Resource defaultVariant = createMockVariant("default", null, 0, true);

        List<Resource> variants = Arrays.asList(mobileVariant, defaultVariant);
        when(variantsNode.getChildren()).thenReturn(variants);
        lenient().when(variantsNode.listChildren()).thenReturn(variants.iterator());
        lenient().when(variantsNode.hasChildren()).thenReturn(true);
        when(contentResource.getChild("variants")).thenReturn(variantsNode);

        Resource result = resolver.resolveVariant(contentResource, request);

        assertNotNull(result);
        assertEquals("default", result.getName());
    }

    @Test
    void testResolveVariant_HigherPriorityWins() {
        // Setup matched segments
        when(personalizationService.evaluateSegments(request))
                .thenReturn(Arrays.asList("mobile-users", "premium-users"));

        // Setup variants - premium has higher priority
        Resource variantsNode = mock(Resource.class);
        Resource mobileVariant = createMockVariant("mobile", new String[] {"mobile-users"}, 50, false);
        Resource premiumVariant = createMockVariant("premium", new String[] {"premium-users"}, 100, false);

        List<Resource> variants = Arrays.asList(mobileVariant, premiumVariant);
        when(variantsNode.getChildren()).thenReturn(variants);
        lenient().when(variantsNode.listChildren()).thenReturn(variants.iterator());
        lenient().when(variantsNode.hasChildren()).thenReturn(true);
        when(contentResource.getChild("variants")).thenReturn(variantsNode);

        Resource result = resolver.resolveVariant(contentResource, request);

        assertNotNull(result);
        assertEquals("premium", result.getName());
    }

    @Test
    void testResolveVariantForSegments_DirectCall() {
        // Setup variants
        Resource variantsNode = mock(Resource.class);
        Resource mobileVariant = createMockVariant("mobile", new String[] {"mobile-users"}, 50, false);

        List<Resource> variants = Collections.singletonList(mobileVariant);
        when(variantsNode.getChildren()).thenReturn(variants);
        lenient().when(variantsNode.listChildren()).thenReturn(variants.iterator());
        lenient().when(variantsNode.hasChildren()).thenReturn(true);
        when(contentResource.getChild("variants")).thenReturn(variantsNode);

        Resource result = resolver.resolveVariantForSegments(contentResource, Arrays.asList("mobile-users"));

        assertNotNull(result);
        assertEquals("mobile", result.getName());
    }

    @Test
    void testGetVariants_SortedByPriority() {
        Resource variantsNode = mock(Resource.class);
        Resource lowPriority = createMockVariant("low", null, 10, false);
        Resource highPriority = createMockVariant("high", null, 100, false);
        Resource mediumPriority = createMockVariant("medium", null, 50, false);

        List<Resource> variants = Arrays.asList(lowPriority, highPriority, mediumPriority);
        when(variantsNode.listChildren()).thenReturn(variants.iterator());
        when(contentResource.getChild("variants")).thenReturn(variantsNode);

        List<Resource> result = resolver.getVariants(contentResource);

        assertEquals(3, result.size());
        assertEquals("high", result.get(0).getName());
        assertEquals("medium", result.get(1).getName());
        assertEquals("low", result.get(2).getName());
    }

    @Test
    void testGetVariants_NoVariantsNode() {
        when(contentResource.getChild("variants")).thenReturn(null);

        List<Resource> result = resolver.getVariants(contentResource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetDefaultVariant_Found() {
        Resource variantsNode = mock(Resource.class);
        Resource regularVariant = createMockVariant("regular", null, 50, false);
        Resource defaultVariant = createMockVariant("default", null, 0, true);

        List<Resource> variants = Arrays.asList(regularVariant, defaultVariant);
        when(variantsNode.getChildren()).thenReturn(variants);
        when(contentResource.getChild("variants")).thenReturn(variantsNode);

        Resource result = resolver.getDefaultVariant(contentResource);

        assertNotNull(result);
        assertEquals("default", result.getName());
    }

    @Test
    void testGetDefaultVariant_NotFound() {
        Resource variantsNode = mock(Resource.class);
        Resource regularVariant = createMockVariant("regular", null, 50, false);

        List<Resource> variants = Collections.singletonList(regularVariant);
        when(variantsNode.getChildren()).thenReturn(variants);
        when(contentResource.getChild("variants")).thenReturn(variantsNode);

        Resource result = resolver.getDefaultVariant(contentResource);

        assertNull(result);
    }

    @Test
    void testHasVariants_True() {
        Resource variantsNode = mock(Resource.class);
        when(variantsNode.hasChildren()).thenReturn(true);
        when(contentResource.getChild("variants")).thenReturn(variantsNode);

        assertTrue(resolver.hasVariants(contentResource));
    }

    @Test
    void testHasVariants_False_NoNode() {
        when(contentResource.getChild("variants")).thenReturn(null);

        assertFalse(resolver.hasVariants(contentResource));
    }

    @Test
    void testHasVariants_False_EmptyNode() {
        Resource variantsNode = mock(Resource.class);
        when(variantsNode.hasChildren()).thenReturn(false);
        when(contentResource.getChild("variants")).thenReturn(variantsNode);

        assertFalse(resolver.hasVariants(contentResource));
    }

    @Test
    void testGetVariantSegments_WithSegments() {
        Resource variant = createMockVariant("test", new String[] {"seg1", "seg2"}, 50, false);

        List<String> result = resolver.getVariantSegments(variant);

        assertEquals(2, result.size());
        assertTrue(result.contains("seg1"));
        assertTrue(result.contains("seg2"));
    }

    @Test
    void testGetVariantSegments_NoSegments() {
        Resource variant = createMockVariant("test", null, 50, false);

        List<String> result = resolver.getVariantSegments(variant);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetVariantPriority() {
        Resource variant = createMockVariant("test", null, 75, false);

        assertEquals(75, resolver.getVariantPriority(variant));
    }

    @Test
    void testGetVariantPriority_Default() {
        Resource variant = mock(Resource.class);
        when(variant.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<>()));

        assertEquals(0, resolver.getVariantPriority(variant));
    }

    private Resource createMockVariant(String name, String[] segments, int priority, boolean isDefault) {
        Resource variant = mock(Resource.class);
        when(variant.getName()).thenReturn(name);

        Map<String, Object> props = new HashMap<>();
        props.put(VariantResolver.PROP_PRIORITY, priority);
        props.put(VariantResolver.PROP_IS_DEFAULT, isDefault);
        if (segments != null) {
            props.put(VariantResolver.PROP_SEGMENTS, segments);
        }
        when(variant.getValueMap()).thenReturn(new ValueMapDecorator(props));

        return variant;
    }
}
