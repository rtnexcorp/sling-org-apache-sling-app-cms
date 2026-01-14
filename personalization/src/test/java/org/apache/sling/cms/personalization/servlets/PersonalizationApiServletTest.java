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
package org.apache.sling.cms.personalization.servlets;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.cms.personalization.PersonalizationService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PersonalizationApiServletTest {

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private RequestPathInfo requestPathInfo;

    @Mock
    private PersonalizationService personalizationService;

    @Mock
    private SegmentEvaluator deviceEvaluator;

    @Mock
    private SegmentEvaluator pathEvaluator;

    private PersonalizationApiServlet servlet;
    private StringWriter responseWriter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new PersonalizationApiServlet();
        objectMapper = new ObjectMapper();

        // Inject PersonalizationService
        Field serviceField = PersonalizationApiServlet.class.getDeclaredField("personalizationService");
        serviceField.setAccessible(true);
        serviceField.set(servlet, personalizationService);

        // Inject evaluators
        lenient().when(deviceEvaluator.getType()).thenReturn("device");
        lenient().when(pathEvaluator.getType()).thenReturn("path");
        List<SegmentEvaluator> evaluators = new ArrayList<>();
        evaluators.add(deviceEvaluator);
        evaluators.add(pathEvaluator);

        Field evaluatorsField = PersonalizationApiServlet.class.getDeclaredField("segmentEvaluators");
        evaluatorsField.setAccessible(true);
        evaluatorsField.set(servlet, evaluators);

        // Setup response
        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // Setup request
        lenient().when(request.getResourceResolver()).thenReturn(resourceResolver);
        lenient().when(request.getRequestPathInfo()).thenReturn(requestPathInfo);
    }

    // ==================== Segments List Tests ====================

    @Test
    void testHandleSegmentsList_EmptySelectors() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {});
        when(resourceResolver.getResource("/etc/personalization/segments")).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).setContentType("application/json");
        JsonNode result = objectMapper.readTree(responseWriter.toString());
        assertEquals(0, result.get("total").asInt());
        assertTrue(result.get("segments").isArray());
    }

    @Test
    void testHandleSegmentsList_WithSegmentsSelector() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {"segments"});
        when(resourceResolver.getResource("/etc/personalization/segments")).thenReturn(null);

        servlet.doGet(request, response);

        JsonNode result = objectMapper.readTree(responseWriter.toString());
        assertEquals(0, result.get("total").asInt());
    }

    @Test
    void testHandleSegmentsList_WithSegments() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {"segments"});

        Resource segmentsRoot = mock(Resource.class);
        Resource segment1 = createMockSegmentResource("mobile-users", "device", 50, true);
        Resource segment2 = createMockSegmentResource("premium-users", "userGroup", 80, true);

        List<Resource> segmentList = Arrays.asList(segment1, segment2);
        when(segmentsRoot.listChildren()).thenReturn(segmentList.iterator());
        when(resourceResolver.getResource("/etc/personalization/segments")).thenReturn(segmentsRoot);

        servlet.doGet(request, response);

        JsonNode result = objectMapper.readTree(responseWriter.toString());
        assertEquals(2, result.get("total").asInt());
        assertEquals(2, result.get("segments").size());
    }

    @Test
    void testHandleSegmentsList_FilterByType() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {"segments"});
        when(request.getParameter("type")).thenReturn("device");

        Resource segmentsRoot = mock(Resource.class);
        Resource deviceSegment = createMockSegmentResource("mobile-users", "device", 50, true);
        Resource userGroupSegment = createMockSegmentResource("premium-users", "userGroup", 80, true);

        List<Resource> segmentList = Arrays.asList(deviceSegment, userGroupSegment);
        when(segmentsRoot.listChildren()).thenReturn(segmentList.iterator());
        when(resourceResolver.getResource("/etc/personalization/segments")).thenReturn(segmentsRoot);

        servlet.doGet(request, response);

        JsonNode result = objectMapper.readTree(responseWriter.toString());
        assertEquals(1, result.get("total").asInt());
        assertEquals("mobile-users", result.get("segments").get(0).get("id").asText());
    }

    @Test
    void testHandleSegmentsList_FilterByActive() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {"segments"});
        when(request.getParameter("active")).thenReturn("false");

        Resource segmentsRoot = mock(Resource.class);
        Resource activeSegment = createMockSegmentResource("active-segment", "device", 50, true);
        Resource inactiveSegment = createMockSegmentResource("inactive-segment", "device", 30, false);

        List<Resource> segmentList = Arrays.asList(activeSegment, inactiveSegment);
        when(segmentsRoot.listChildren()).thenReturn(segmentList.iterator());
        when(resourceResolver.getResource("/etc/personalization/segments")).thenReturn(segmentsRoot);

        servlet.doGet(request, response);

        JsonNode result = objectMapper.readTree(responseWriter.toString());
        assertEquals(1, result.get("total").asInt());
        assertEquals("inactive-segment", result.get("segments").get(0).get("id").asText());
    }

    // ==================== Segment Detail Tests ====================

    @Test
    void testHandleSegmentDetail_SegmentFound() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {"segment", "mobile-users"});

        Resource segmentResource = createMockSegmentResource("mobile-users", "device", 50, true);
        when(resourceResolver.getResource("/etc/personalization/segments/mobile-users"))
                .thenReturn(segmentResource);

        servlet.doGet(request, response);

        JsonNode result = objectMapper.readTree(responseWriter.toString());
        assertEquals("mobile-users", result.get("id").asText());
        assertEquals("device", result.get("type").asText());
        assertEquals(50, result.get("priority").asInt());
    }

    @Test
    void testHandleSegmentDetail_SegmentNotFound() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {"segment", "nonexistent"});
        when(resourceResolver.getResource("/etc/personalization/segments/nonexistent"))
                .thenReturn(null);

        servlet.doGet(request, response);

        verify(response).setStatus(404);
        JsonNode result = objectMapper.readTree(responseWriter.toString());
        assertEquals(404, result.get("status").asInt());
        assertTrue(result.get("error").asText().contains("Segment not found"));
    }

    @Test
    void testHandleSegmentDetail_WithRules() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {"segment", "mobile-users"});

        Resource segmentResource = createMockSegmentResource("mobile-users", "device", 50, true);
        Resource rulesResource = mock(Resource.class);
        Map<String, Object> rulesProps = new HashMap<>();
        rulesProps.put("deviceType", "mobile");
        rulesProps.put("jcr:primaryType", "nt:unstructured"); // Should be filtered out
        when(rulesResource.getValueMap()).thenReturn(new ValueMapDecorator(rulesProps));
        when(segmentResource.getChild("rules")).thenReturn(rulesResource);
        when(resourceResolver.getResource("/etc/personalization/segments/mobile-users"))
                .thenReturn(segmentResource);

        servlet.doGet(request, response);

        JsonNode result = objectMapper.readTree(responseWriter.toString());
        assertNotNull(result.get("rules"));
        assertEquals("mobile", result.get("rules").get("deviceType").asText());
        assertFalse(result.get("rules").has("jcr:primaryType"));
    }

    @Test
    void testHandleSegmentDetail_WithArrayRules() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {"segment", "premium-users"});

        Resource segmentResource = createMockSegmentResource("premium-users", "userGroup", 80, true);
        Resource rulesResource = mock(Resource.class);
        Map<String, Object> rulesProps = new HashMap<>();
        rulesProps.put("groups", new String[] {"premium", "vip"});
        when(rulesResource.getValueMap()).thenReturn(new ValueMapDecorator(rulesProps));
        when(segmentResource.getChild("rules")).thenReturn(rulesResource);
        when(resourceResolver.getResource("/etc/personalization/segments/premium-users"))
                .thenReturn(segmentResource);

        servlet.doGet(request, response);

        JsonNode result = objectMapper.readTree(responseWriter.toString());
        assertNotNull(result.get("rules"));
        assertTrue(result.get("rules").get("groups").isArray());
        assertEquals(2, result.get("rules").get("groups").size());
    }

    // ==================== Evaluate Tests ====================

    @Test
    void testHandleEvaluate_Success() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {"evaluate"});
        when(request.getParameter("path")).thenReturn("/content/test-page");

        Resource contentResource = mock(Resource.class);
        when(resourceResolver.getResource("/content/test-page")).thenReturn(contentResource);

        List<String> matchedSegments = Arrays.asList("mobile-users", "returning-visitors");
        when(personalizationService.evaluateSegments(request)).thenReturn(matchedSegments);

        Resource selectedVariant =
                createMockVariantResource("mobile-variant", false, 50, new String[] {"mobile-users"});
        when(personalizationService.selectVariant(contentResource, request)).thenReturn(selectedVariant);

        List<Resource> allVariants =
                Arrays.asList(selectedVariant, createMockVariantResource("default-variant", true, 10, null));
        when(personalizationService.getVariants(contentResource)).thenReturn(allVariants);

        servlet.doGet(request, response);

        JsonNode result = objectMapper.readTree(responseWriter.toString());
        assertEquals("/content/test-page", result.get("path").asText());
        assertEquals(2, result.get("matchedSegments").size());
        assertNotNull(result.get("selectedVariant"));
        assertEquals("mobile-variant", result.get("selectedVariant").get("id").asText());
        assertEquals(2, result.get("availableVariants").size());
    }

    @Test
    void testHandleEvaluate_MissingPathParameter() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {"evaluate"});
        when(request.getParameter("path")).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).setStatus(400);
        JsonNode result = objectMapper.readTree(responseWriter.toString());
        assertEquals(400, result.get("status").asInt());
        assertTrue(result.get("error").asText().contains("Missing required parameter"));
    }

    @Test
    void testHandleEvaluate_ContentNotFound() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {"evaluate"});
        when(request.getParameter("path")).thenReturn("/content/nonexistent");
        when(resourceResolver.getResource("/content/nonexistent")).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).setStatus(404);
        JsonNode result = objectMapper.readTree(responseWriter.toString());
        assertEquals(404, result.get("status").asInt());
        assertTrue(result.get("error").asText().contains("Content not found"));
    }

    @Test
    void testHandleEvaluate_NoMatchingVariant() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {"evaluate"});
        when(request.getParameter("path")).thenReturn("/content/test-page");

        Resource contentResource = mock(Resource.class);
        when(resourceResolver.getResource("/content/test-page")).thenReturn(contentResource);

        when(personalizationService.evaluateSegments(request)).thenReturn(new ArrayList<>());
        when(personalizationService.selectVariant(contentResource, request)).thenReturn(null);
        when(personalizationService.getVariants(contentResource)).thenReturn(new ArrayList<>());

        servlet.doGet(request, response);

        JsonNode result = objectMapper.readTree(responseWriter.toString());
        assertEquals("/content/test-page", result.get("path").asText());
        assertEquals(0, result.get("matchedSegments").size());
        // selectedVariant is not added to response when null
        assertFalse(result.has("selectedVariant"));
        assertEquals(0, result.get("availableVariants").size());
    }

    // ==================== Evaluators List Tests ====================

    @Test
    void testHandleEvaluatorsList() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {"evaluators"});

        servlet.doGet(request, response);

        JsonNode result = objectMapper.readTree(responseWriter.toString());
        assertEquals(2, result.get("total").asInt());
        assertEquals(2, result.get("evaluators").size());

        List<String> types = new ArrayList<>();
        result.get("evaluators").forEach(e -> types.add(e.get("type").asText()));
        assertTrue(types.contains("device"));
        assertTrue(types.contains("path"));
    }

    // ==================== Unknown Operation Tests ====================

    @Test
    void testUnknownSelector() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {"unknown"});

        servlet.doGet(request, response);

        verify(response).setStatus(404);
        JsonNode result = objectMapper.readTree(responseWriter.toString());
        assertEquals(404, result.get("status").asInt());
        assertTrue(result.get("error").asText().contains("Unknown operation"));
    }

    // ==================== Error Handling Tests ====================

    @Test
    void testInternalServerError() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {"segments"});
        when(resourceResolver.getResource("/etc/personalization/segments"))
                .thenThrow(new RuntimeException("Test exception"));

        servlet.doGet(request, response);

        verify(response).setStatus(500);
        JsonNode result = objectMapper.readTree(responseWriter.toString());
        assertEquals(500, result.get("status").asInt());
        assertTrue(result.get("error").asText().contains("Internal server error"));
    }

    // ==================== Response Format Tests ====================

    @Test
    void testResponseContentType() throws Exception {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] {"segments"});
        when(resourceResolver.getResource("/etc/personalization/segments")).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
    }

    // ==================== Helper Methods ====================

    private Resource createMockSegmentResource(String name, String evaluatorType, int priority, boolean active) {
        Resource resource = mock(Resource.class);
        when(resource.getName()).thenReturn(name);
        when(resource.getPath()).thenReturn("/etc/personalization/segments/" + name);

        Map<String, Object> props = new HashMap<>();
        props.put("name", name);
        props.put("evaluatorType", evaluatorType);
        props.put("priority", priority);
        props.put("active", active);
        props.put("description", "Test segment: " + name);
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(props));
        lenient().when(resource.getChild("rules")).thenReturn(null);

        return resource;
    }

    private Resource createMockVariantResource(String name, boolean isDefault, int priority, String[] segments) {
        Resource resource = mock(Resource.class);
        when(resource.getName()).thenReturn(name);
        when(resource.getPath()).thenReturn("/content/test-page/variants/" + name);

        Map<String, Object> props = new HashMap<>();
        props.put("name", name);
        props.put("isDefault", isDefault);
        props.put("priority", priority);
        if (segments != null) {
            props.put("segments", segments);
        }
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(props));

        return resource;
    }
}
