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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.cms.personalization.Segment;
import org.apache.sling.cms.personalization.SegmentEvaluator;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PersonalizationServiceImplTest {

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private Resource contextResource;

    @Mock
    private ConfigurationResourceResolver configurationResourceResolver;

    @Mock
    private SegmentEvaluator deviceEvaluator;

    @Mock
    private SegmentEvaluator pathEvaluator;

    private PersonalizationServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new PersonalizationServiceImpl();

        // Inject ConfigurationResourceResolver using reflection
        Field configResolverField = PersonalizationServiceImpl.class.getDeclaredField("configurationResourceResolver");
        configResolverField.setAccessible(true);
        configResolverField.set(service, configurationResourceResolver);

        // Setup evaluators
        lenient().when(deviceEvaluator.getType()).thenReturn("device");
        lenient().when(pathEvaluator.getType()).thenReturn("path");

        List<SegmentEvaluator> evaluators = new ArrayList<>();
        evaluators.add(deviceEvaluator);
        evaluators.add(pathEvaluator);

        Field evaluatorsField = PersonalizationServiceImpl.class.getDeclaredField("evaluators");
        evaluatorsField.setAccessible(true);
        evaluatorsField.set(service, evaluators);
    }

    private void activateService(boolean enabled, boolean debugMode) {
        PersonalizationServiceImpl.Config config = mock(PersonalizationServiceImpl.Config.class);
        when(config.enabled()).thenReturn(enabled);
        when(config.debugMode()).thenReturn(debugMode);
        service.activate(config);
    }

    @Test
    void testIsEnabled_WhenEnabled() {
        activateService(true, false);
        assertTrue(service.isEnabled());
    }

    @Test
    void testIsEnabled_WhenDisabled() {
        activateService(false, false);
        assertFalse(service.isEnabled());
    }

    @Test
    void testResolveSegments_WhenDisabled_ReturnsEmptyList() {
        activateService(false, false);

        List<Segment> result = service.resolveSegments(request, contextResource);

        assertTrue(result.isEmpty());
        verify(configurationResourceResolver, never()).getResource(any(), any(), any());
    }

    @Test
    void testResolveSegments_WhenCached_ReturnsCachedSegments() {
        activateService(true, false);

        List<Segment> cachedSegments = new ArrayList<>();
        cachedSegments.add(mockSegment("cached-segment", 10));
        when(request.getAttribute("sling.cms.personalization.segments")).thenReturn(cachedSegments);

        List<Segment> result = service.resolveSegments(request, contextResource);

        assertEquals(cachedSegments, result);
        verify(configurationResourceResolver, never()).getResource(any(), any(), any());
    }

    @Test
    void testResolveSegments_NoConfigFound_ReturnsEmptyList() {
        activateService(true, false);
        when(request.getAttribute("sling.cms.personalization.segments")).thenReturn(null);
        when(configurationResourceResolver.getResource(contextResource, "personalization", "segments"))
                .thenReturn(null);

        List<Segment> result = service.resolveSegments(request, contextResource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testResolveSegments_WithMatchingSegment() {
        activateService(true, false);
        when(request.getAttribute("sling.cms.personalization.segments")).thenReturn(null);

        // Setup segment config
        Resource segmentsConfig = mock(Resource.class);
        Resource segmentResource = createSegmentResource("mobile-users", "device", 50);
        Resource rulesResource = mock(Resource.class);
        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("deviceType", "mobile");
        when(rulesResource.getValueMap()).thenReturn(new ValueMapDecorator(rulesMap));
        when(segmentResource.getChild("rules")).thenReturn(rulesResource);

        List<Resource> segmentChildren = new ArrayList<>();
        segmentChildren.add(segmentResource);
        when(segmentsConfig.getChildren()).thenReturn(segmentChildren);

        when(configurationResourceResolver.getResource(contextResource, "personalization", "segments"))
                .thenReturn(segmentsConfig);
        when(deviceEvaluator.evaluate(eq(request), any(ValueMap.class))).thenReturn(true);

        List<Segment> result = service.resolveSegments(request, contextResource);

        assertEquals(1, result.size());
        assertEquals("mobile-users", result.get(0).getId());
        verify(request).setAttribute(eq("sling.cms.personalization.segments"), any());
    }

    @Test
    void testResolveSegments_WithNonMatchingSegment() {
        activateService(true, false);
        when(request.getAttribute("sling.cms.personalization.segments")).thenReturn(null);

        Resource segmentsConfig = mock(Resource.class);
        Resource segmentResource = createSegmentResource("mobile-users", "device", 50);
        Resource rulesResource = mock(Resource.class);
        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("deviceType", "mobile");
        when(rulesResource.getValueMap()).thenReturn(new ValueMapDecorator(rulesMap));
        when(segmentResource.getChild("rules")).thenReturn(rulesResource);

        List<Resource> segmentChildren = new ArrayList<>();
        segmentChildren.add(segmentResource);
        when(segmentsConfig.getChildren()).thenReturn(segmentChildren);

        when(configurationResourceResolver.getResource(contextResource, "personalization", "segments"))
                .thenReturn(segmentsConfig);
        when(deviceEvaluator.evaluate(eq(request), any(ValueMap.class))).thenReturn(false);

        List<Segment> result = service.resolveSegments(request, contextResource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testResolveSegments_SortsByPriorityDescending() {
        activateService(true, false);
        when(request.getAttribute("sling.cms.personalization.segments")).thenReturn(null);

        Resource segmentsConfig = mock(Resource.class);

        // Low priority segment
        Resource lowPrioritySegment = createSegmentResource("low-priority", "device", 10);
        Resource lowRules = mock(Resource.class);
        when(lowRules.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<>()));
        when(lowPrioritySegment.getChild("rules")).thenReturn(lowRules);

        // High priority segment
        Resource highPrioritySegment = createSegmentResource("high-priority", "device", 100);
        Resource highRules = mock(Resource.class);
        when(highRules.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<>()));
        when(highPrioritySegment.getChild("rules")).thenReturn(highRules);

        // Medium priority segment
        Resource mediumPrioritySegment = createSegmentResource("medium-priority", "device", 50);
        Resource mediumRules = mock(Resource.class);
        when(mediumRules.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<>()));
        when(mediumPrioritySegment.getChild("rules")).thenReturn(mediumRules);

        List<Resource> segmentChildren = new ArrayList<>();
        segmentChildren.add(lowPrioritySegment);
        segmentChildren.add(highPrioritySegment);
        segmentChildren.add(mediumPrioritySegment);
        when(segmentsConfig.getChildren()).thenReturn(segmentChildren);

        when(configurationResourceResolver.getResource(contextResource, "personalization", "segments"))
                .thenReturn(segmentsConfig);
        when(deviceEvaluator.evaluate(eq(request), any(ValueMap.class))).thenReturn(true);

        List<Segment> result = service.resolveSegments(request, contextResource);

        assertEquals(3, result.size());
        assertEquals("high-priority", result.get(0).getId());
        assertEquals("medium-priority", result.get(1).getId());
        assertEquals("low-priority", result.get(2).getId());
    }

    @Test
    void testResolveSegments_NoEvaluatorTypeConfigured() {
        activateService(true, false);
        when(request.getAttribute("sling.cms.personalization.segments")).thenReturn(null);

        Resource segmentsConfig = mock(Resource.class);
        Resource segmentResource = createSegmentResource("no-evaluator", "", 50);

        List<Resource> segmentChildren = new ArrayList<>();
        segmentChildren.add(segmentResource);
        when(segmentsConfig.getChildren()).thenReturn(segmentChildren);

        when(configurationResourceResolver.getResource(contextResource, "personalization", "segments"))
                .thenReturn(segmentsConfig);

        List<Segment> result = service.resolveSegments(request, contextResource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testResolveSegments_UnknownEvaluatorType() {
        activateService(true, false);
        when(request.getAttribute("sling.cms.personalization.segments")).thenReturn(null);

        Resource segmentsConfig = mock(Resource.class);
        Resource segmentResource = createSegmentResource("unknown-type", "unknownEvaluator", 50);

        List<Resource> segmentChildren = new ArrayList<>();
        segmentChildren.add(segmentResource);
        when(segmentsConfig.getChildren()).thenReturn(segmentChildren);

        when(configurationResourceResolver.getResource(contextResource, "personalization", "segments"))
                .thenReturn(segmentsConfig);

        List<Segment> result = service.resolveSegments(request, contextResource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testResolveSegments_NoRulesConfigured() {
        activateService(true, false);
        when(request.getAttribute("sling.cms.personalization.segments")).thenReturn(null);

        Resource segmentsConfig = mock(Resource.class);
        Resource segmentResource = createSegmentResource("no-rules", "device", 50);
        when(segmentResource.getChild("rules")).thenReturn(null);

        List<Resource> segmentChildren = new ArrayList<>();
        segmentChildren.add(segmentResource);
        when(segmentsConfig.getChildren()).thenReturn(segmentChildren);

        when(configurationResourceResolver.getResource(contextResource, "personalization", "segments"))
                .thenReturn(segmentsConfig);

        List<Segment> result = service.resolveSegments(request, contextResource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAvailableSegments_ReturnsAllSegments() {
        Resource segmentsConfig = mock(Resource.class);
        Resource segment1 = createSegmentResource("segment1", "device", 10);
        Resource segment2 = createSegmentResource("segment2", "path", 20);

        List<Resource> segmentChildren = new ArrayList<>();
        segmentChildren.add(segment1);
        segmentChildren.add(segment2);
        when(segmentsConfig.getChildren()).thenReturn(segmentChildren);

        when(configurationResourceResolver.getResource(contextResource, "personalization", "segments"))
                .thenReturn(segmentsConfig);

        List<Segment> result = service.getAvailableSegments(contextResource);

        assertEquals(2, result.size());
    }

    @Test
    void testGetAvailableSegments_NoConfig_ReturnsEmptyList() {
        when(configurationResourceResolver.getResource(contextResource, "personalization", "segments"))
                .thenReturn(null);

        List<Segment> result = service.getAvailableSegments(contextResource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testEvaluateSegments_ReturnsSegmentIds() {
        activateService(true, false);
        when(request.getAttribute("sling.cms.personalization.segments")).thenReturn(null);
        when(request.getResource()).thenReturn(contextResource);

        Resource segmentsConfig = mock(Resource.class);
        Resource segmentResource = createSegmentResource("test-segment", "device", 50);
        Resource rulesResource = mock(Resource.class);
        when(rulesResource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<>()));
        when(segmentResource.getChild("rules")).thenReturn(rulesResource);

        List<Resource> segmentChildren = new ArrayList<>();
        segmentChildren.add(segmentResource);
        when(segmentsConfig.getChildren()).thenReturn(segmentChildren);

        when(configurationResourceResolver.getResource(contextResource, "personalization", "segments"))
                .thenReturn(segmentsConfig);
        when(deviceEvaluator.evaluate(eq(request), any(ValueMap.class))).thenReturn(true);

        List<String> result = service.evaluateSegments(request);

        assertEquals(1, result.size());
        assertEquals("test-segment", result.get(0));
    }

    @Test
    void testSelectVariant_NoVariantsResource_ReturnsNull() {
        activateService(true, false);
        when(request.getAttribute("sling.cms.personalization.segments")).thenReturn(null);
        when(request.getResource()).thenReturn(contextResource);
        when(configurationResourceResolver.getResource(contextResource, "personalization", "segments"))
                .thenReturn(null);

        Resource contentResource = mock(Resource.class);
        when(contentResource.getChild("variants")).thenReturn(null);

        Resource result = service.selectVariant(contentResource, request);

        assertNull(result);
    }

    @Test
    void testSelectVariant_WithMatchingVariant() {
        activateService(true, false);

        // Setup matched segments
        List<Segment> cachedSegments = new ArrayList<>();
        cachedSegments.add(mockSegment("mobile-users", 10));
        when(request.getAttribute("sling.cms.personalization.segments")).thenReturn(cachedSegments);
        when(request.getResource()).thenReturn(contextResource);

        // Setup variants
        Resource contentResource = mock(Resource.class);
        Resource variantsResource = mock(Resource.class);

        Resource mobileVariant = mock(Resource.class);
        Map<String, Object> mobileProps = new HashMap<>();
        mobileProps.put("segments", new String[] {"mobile-users"});
        mobileProps.put("priority", 10);
        when(mobileVariant.getValueMap()).thenReturn(new ValueMapDecorator(mobileProps));

        List<Resource> variantsList = new ArrayList<>();
        variantsList.add(mobileVariant);
        when(variantsResource.listChildren()).thenReturn(variantsList.iterator());

        when(contentResource.getChild("variants")).thenReturn(variantsResource);

        Resource result = service.selectVariant(contentResource, request);

        assertNotNull(result);
        assertEquals(mobileVariant, result);
    }

    @Test
    void testSelectVariant_NoMatchingVariant_ReturnsDefault() {
        activateService(true, false);

        // Setup matched segments with no match
        List<Segment> cachedSegments = new ArrayList<>();
        cachedSegments.add(mockSegment("tablet-users", 10));
        when(request.getAttribute("sling.cms.personalization.segments")).thenReturn(cachedSegments);
        when(request.getResource()).thenReturn(contextResource);

        // Setup variants
        Resource contentResource = mock(Resource.class);
        Resource variantsResource = mock(Resource.class);

        Resource mobileVariant = mock(Resource.class);
        Map<String, Object> mobileProps = new HashMap<>();
        mobileProps.put("segments", new String[] {"mobile-users"});
        mobileProps.put("priority", 10);
        when(mobileVariant.getValueMap()).thenReturn(new ValueMapDecorator(mobileProps));

        Resource defaultVariant = mock(Resource.class);
        Map<String, Object> defaultProps = new HashMap<>();
        defaultProps.put("isDefault", true);
        defaultProps.put("priority", 1);
        when(defaultVariant.getValueMap()).thenReturn(new ValueMapDecorator(defaultProps));

        List<Resource> variantsList = new ArrayList<>();
        variantsList.add(mobileVariant);
        variantsList.add(defaultVariant);
        when(variantsResource.listChildren()).thenReturn(variantsList.iterator());

        when(contentResource.getChild("variants")).thenReturn(variantsResource);

        Resource result = service.selectVariant(contentResource, request);

        assertNotNull(result);
        assertEquals(defaultVariant, result);
    }

    @Test
    void testSelectVariant_PrioritySorting() {
        activateService(true, false);

        // Setup matched segments
        List<Segment> cachedSegments = new ArrayList<>();
        cachedSegments.add(mockSegment("mobile-users", 10));
        when(request.getAttribute("sling.cms.personalization.segments")).thenReturn(cachedSegments);
        when(request.getResource()).thenReturn(contextResource);

        // Setup variants - both match, but different priorities
        Resource contentResource = mock(Resource.class);
        Resource variantsResource = mock(Resource.class);

        Resource lowPriorityVariant = mock(Resource.class);
        Map<String, Object> lowProps = new HashMap<>();
        lowProps.put("segments", new String[] {"mobile-users"});
        lowProps.put("priority", 10);
        when(lowPriorityVariant.getValueMap()).thenReturn(new ValueMapDecorator(lowProps));

        Resource highPriorityVariant = mock(Resource.class);
        Map<String, Object> highProps = new HashMap<>();
        highProps.put("segments", new String[] {"mobile-users"});
        highProps.put("priority", 100);
        when(highPriorityVariant.getValueMap()).thenReturn(new ValueMapDecorator(highProps));

        List<Resource> variantsList = new ArrayList<>();
        variantsList.add(lowPriorityVariant);
        variantsList.add(highPriorityVariant);
        when(variantsResource.listChildren()).thenReturn(variantsList.iterator());

        when(contentResource.getChild("variants")).thenReturn(variantsResource);

        Resource result = service.selectVariant(contentResource, request);

        assertNotNull(result);
        assertEquals(highPriorityVariant, result);
    }

    @Test
    void testGetVariants_ReturnsAllVariants() {
        Resource contentResource = mock(Resource.class);
        Resource variantsResource = mock(Resource.class);

        Resource variant1 = mock(Resource.class);
        Resource variant2 = mock(Resource.class);

        List<Resource> variantsList = new ArrayList<>();
        variantsList.add(variant1);
        variantsList.add(variant2);
        when(variantsResource.listChildren()).thenReturn(variantsList.iterator());

        when(contentResource.getChild("variants")).thenReturn(variantsResource);

        List<Resource> result = service.getVariants(contentResource);

        assertEquals(2, result.size());
    }

    @Test
    void testGetVariants_NoVariantsResource_ReturnsEmptyList() {
        Resource contentResource = mock(Resource.class);
        when(contentResource.getChild("variants")).thenReturn(null);

        List<Resource> result = service.getVariants(contentResource);

        assertTrue(result.isEmpty());
    }

    @Test
    void testResolveSegments_EvaluatorThrowsException_ContinuesWithOthers() {
        activateService(true, false);
        when(request.getAttribute("sling.cms.personalization.segments")).thenReturn(null);

        Resource segmentsConfig = mock(Resource.class);

        // First segment with device evaluator that throws
        Resource errorSegment = createSegmentResource("error-segment", "device", 50);
        Resource errorRules = mock(Resource.class);
        when(errorRules.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<>()));
        when(errorSegment.getChild("rules")).thenReturn(errorRules);

        // Second segment with path evaluator that works
        Resource workingSegment = createSegmentResource("working-segment", "path", 40);
        Resource workingRules = mock(Resource.class);
        when(workingRules.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<>()));
        when(workingSegment.getChild("rules")).thenReturn(workingRules);

        List<Resource> segmentChildren = new ArrayList<>();
        segmentChildren.add(errorSegment);
        segmentChildren.add(workingSegment);
        when(segmentsConfig.getChildren()).thenReturn(segmentChildren);

        when(configurationResourceResolver.getResource(contextResource, "personalization", "segments"))
                .thenReturn(segmentsConfig);
        when(deviceEvaluator.evaluate(eq(request), any(ValueMap.class)))
                .thenThrow(new RuntimeException("Test exception"));
        when(pathEvaluator.evaluate(eq(request), any(ValueMap.class))).thenReturn(true);

        List<Segment> result = service.resolveSegments(request, contextResource);

        assertEquals(1, result.size());
        assertEquals("working-segment", result.get(0).getId());
    }

    private Resource createSegmentResource(String name, String evaluatorType, int priority) {
        Resource resource = mock(Resource.class);
        when(resource.getName()).thenReturn(name);

        Map<String, Object> props = new HashMap<>();
        props.put("title", name);
        props.put("evaluator", evaluatorType);
        props.put("priority", priority);
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(props));

        return resource;
    }

    private Segment mockSegment(String id, int priority) {
        Segment segment = mock(Segment.class);
        when(segment.getId()).thenReturn(id);
        when(segment.getPriority()).thenReturn(priority);
        return segment;
    }
}
