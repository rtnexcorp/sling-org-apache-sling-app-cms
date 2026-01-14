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
import java.util.Arrays;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.graphql.internal.security.GraphQLSecurityService;
import org.apache.sling.cms.schema.ContentSchema;
import org.apache.sling.cms.schema.SchemaManager;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemasDataFetcherTest {

    @Mock
    private SlingDataFetcherEnvironment environment;

    @Mock
    private Resource currentResource;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private GraphQLSecurityService securityService;

    @Mock
    private SchemaManager schemaManager;

    private SchemasDataFetcher fetcher;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(environment.getCurrentResource()).thenReturn(currentResource);
        when(currentResource.getResourceResolver()).thenReturn(resolver);

        fetcher = new SchemasDataFetcher();
        inject(fetcher, "securityService", securityService);
        inject(fetcher, "schemaManager", schemaManager);
    }

    @Test
    void testGetReturnsSchemasAndValidatesAccess() throws Exception {
        ContentSchema s1 = mock(ContentSchema.class);
        ContentSchema s2 = mock(ContentSchema.class);
        List<ContentSchema> expected = Arrays.asList(s1, s2);

        when(schemaManager.getAllSchemas(currentResource)).thenReturn(expected);

        List<ContentSchema> result = fetcher.get(environment);

        assertSame(expected, result);
        verify(securityService).validateAccess(resolver, "schemas");
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
