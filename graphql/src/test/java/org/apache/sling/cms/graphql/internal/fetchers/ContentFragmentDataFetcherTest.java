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
package org.apache.sling.cms.graphql.internal.fetchers;

import java.lang.reflect.Field;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.graphql.internal.dto.ContentFragmentDTO;
import org.apache.sling.cms.graphql.internal.security.GraphQLSecurityService;
import org.apache.sling.cms.graphql.internal.services.ContentFragmentQueryService;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentFragmentDataFetcherTest {

    @Mock
    private SlingDataFetcherEnvironment environment;

    @Mock
    private Resource currentResource;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private GraphQLSecurityService securityService;

    @Mock
    private ContentFragmentQueryService queryService;

    private ContentFragmentDataFetcher fetcher;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(environment.getCurrentResource()).thenReturn(currentResource);
        when(currentResource.getResourceResolver()).thenReturn(resolver);

        fetcher = new ContentFragmentDataFetcher();
        inject(fetcher, "securityService", securityService);
        inject(fetcher, "fragmentQueryService", queryService);
    }

    @Test
    void testGetReturnsNullWhenIdMissing() throws Exception {
        when(environment.getArgument("id")).thenReturn(null);

        assertNull(fetcher.get(environment));

        verifyNoInteractions(securityService);
        verifyNoInteractions(queryService);
    }

    @Test
    void testGetFetchesFragmentWhenIdProvided() throws Exception {
        when(environment.getArgument("id")).thenReturn("/content/fragments/test");

        ContentFragmentDTO dto = new ContentFragmentDTO();
        dto.setId("/content/fragments/test");

        when(queryService.getFragmentById(resolver, "/content/fragments/test")).thenReturn(dto);

        ContentFragmentDTO result = fetcher.get(environment);

        assertNotNull(result);
        assertEquals("/content/fragments/test", result.getId());

        verify(securityService).validateAccess(resolver, "fragment");
        verify(securityService).validateReadAccess(resolver, "/content/fragments/test");
        verify(queryService).getFragmentById(resolver, "/content/fragments/test");
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
