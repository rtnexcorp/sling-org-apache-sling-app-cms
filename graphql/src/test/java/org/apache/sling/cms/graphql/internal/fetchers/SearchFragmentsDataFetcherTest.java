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

class SearchFragmentsDataFetcherTest {

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

    private SearchFragmentsDataFetcher fetcher;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(environment.getCurrentResource()).thenReturn(currentResource);
        when(currentResource.getResourceResolver()).thenReturn(resolver);

        fetcher = new SearchFragmentsDataFetcher();
        inject(fetcher, "securityService", securityService);
        inject(fetcher, "fragmentQueryService", queryService);
    }

    @Test
    void testGetReturnsEmptyConnectionWhenSearchTermBlank() throws Exception {
        when(environment.getArgument("searchTerm")).thenReturn("  ");

        FragmentConnectionDTO result = fetcher.get(environment);

        assertNotNull(result);
        assertNotNull(result.getNodes());
        assertTrue(result.getNodes().isEmpty());

        verifyNoInteractions(securityService);
        verifyNoInteractions(queryService);
    }

    @Test
    void testGetDelegatesToServiceAndValidatesAccess() throws Exception {
        when(environment.getArgument("searchTerm")).thenReturn("hello");
        when(environment.getArgument("schemaType")).thenReturn("article");
        when(environment.getArgument("limit")).thenReturn(5);
        when(environment.getArgument("offset")).thenReturn(10);

        FragmentConnectionDTO expected = new FragmentConnectionDTO();
        when(queryService.searchFragments(resolver, "hello", "article", 5, 10)).thenReturn(expected);

        FragmentConnectionDTO result = fetcher.get(environment);

        assertSame(expected, result);

        verify(securityService).validateAccess(resolver, "fragmentSearch");
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
