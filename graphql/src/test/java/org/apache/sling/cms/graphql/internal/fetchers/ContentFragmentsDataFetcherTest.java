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
import java.util.Collections;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.graphql.internal.dto.FragmentConnectionDTO;
import org.apache.sling.cms.graphql.internal.security.GraphQLSecurityService;
import org.apache.sling.cms.graphql.internal.services.ContentFragmentQueryService;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentFragmentsDataFetcherTest {

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

    private ContentFragmentsDataFetcher fetcher;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(environment.getCurrentResource()).thenReturn(currentResource);
        when(currentResource.getResourceResolver()).thenReturn(resolver);

        fetcher = new ContentFragmentsDataFetcher();
        inject(fetcher, "securityService", securityService);
        inject(fetcher, "fragmentQueryService", queryService);
    }

    @Test
    void testGetDelegatesToServiceAndValidatesAccess() throws Exception {
        when(environment.getArgument("schemaType")).thenReturn("article");
        when(environment.getArgument("path")).thenReturn("/content/fragments");
        when(environment.getArgument("filters")).thenReturn(Collections.emptyList());
        when(environment.getArgument("sortField")).thenReturn("jcr:title");
        when(environment.getArgument("sortDirection")).thenReturn("ASC");
        when(environment.getArgument("limit")).thenReturn(10);
        when(environment.getArgument("offset")).thenReturn(0);
        when(environment.getArgument("cursor")).thenReturn(null);

        FragmentConnectionDTO expected = new FragmentConnectionDTO();
        when(queryService.listFragments(
                        resolver,
                        "article",
                        "/content/fragments",
                        Collections.emptyList(),
                        "jcr:title",
                        "ASC",
                        10,
                        0,
                        null))
                .thenReturn(expected);

        FragmentConnectionDTO result = fetcher.get(environment);

        assertSame(expected, result);

        verify(securityService).validateAccess(resolver, "fragments");
        verify(securityService).validateReadAccess(resolver, "/content/fragments");
    }

    @Test
    void testGetHandlesNullFilters() throws Exception {
        when(environment.getArgument("schemaType")).thenReturn(null);
        when(environment.getArgument("path")).thenReturn(null);
        when(environment.getArgument("filters")).thenReturn(null);

        FragmentConnectionDTO expected = new FragmentConnectionDTO();
        when(queryService.listFragments(
                        eq(resolver),
                        isNull(),
                        isNull(),
                        eq(Collections.<Map<String, String>>emptyList()),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()))
                .thenReturn(expected);

        FragmentConnectionDTO result = fetcher.get(environment);

        assertSame(expected, result);
        verify(securityService).validateAccess(resolver, "fragments");
        verify(securityService, never()).validateReadAccess(any(), any());
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
