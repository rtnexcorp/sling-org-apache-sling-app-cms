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
package org.apache.sling.cms.graphql.internal.security;

import java.lang.reflect.Field;

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GraphQLSecurityServiceTest {

    private GraphQLSecurityService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new GraphQLSecurityService();

        // activate with default config proxy
        GraphQLSecurityService.Config config = mock(GraphQLSecurityService.Config.class);
        when(config.enableAuthentication()).thenReturn(true);
        when(config.allowedAnonymousQueries()).thenReturn(new String[] {"hello"});
        when(config.requiredGroups()).thenReturn(new String[] {});

        service.activate(config);
    }

    @Test
    void testIsAuthenticatedNullResolver() {
        assertFalse(service.isAuthenticated(null));
    }

    @Test
    void testIsAuthenticatedAnonymous() {
        ResourceResolver resolver = mock(ResourceResolver.class);
        when(resolver.getUserID()).thenReturn("anonymous");

        assertFalse(service.isAuthenticated(resolver));
    }

    @Test
    void testIsAuthenticatedNonAnonymous() {
        ResourceResolver resolver = mock(ResourceResolver.class);
        when(resolver.getUserID()).thenReturn("admin");

        assertTrue(service.isAuthenticated(resolver));
    }

    @Test
    void testRequiresAuthenticationDisabledGlobally() throws Exception {
        GraphQLSecurityService.Config config = mock(GraphQLSecurityService.Config.class);
        when(config.enableAuthentication()).thenReturn(false);
        when(config.allowedAnonymousQueries()).thenReturn(new String[] {});
        when(config.requiredGroups()).thenReturn(new String[] {});
        service.activate(config);

        assertFalse(service.requiresAuthentication("anyQuery"));
    }

    @Test
    void testRequiresAuthenticationAllowedAnonymousQuery() {
        assertFalse(service.requiresAuthentication("hello"));
    }

    @Test
    void testRequiresAuthenticationNotAllowedQuery() {
        assertTrue(service.requiresAuthentication("serverInfo"));
    }

    @Test
    void testValidateAccessAllowsAnonymousQueryWithoutAuth() {
        ResourceResolver resolver = mock(ResourceResolver.class);
        when(resolver.getUserID()).thenReturn("anonymous");

        assertDoesNotThrow(() -> service.validateAccess(resolver, "hello"));
    }

    @Test
    void testValidateAccessThrowsUnauthorizedWhenAuthRequired() {
        ResourceResolver resolver = mock(ResourceResolver.class);
        when(resolver.getUserID()).thenReturn("anonymous");

        assertThrows(UnauthorizedException.class, () -> service.validateAccess(resolver, "serverInfo"));
    }

    @Test
    void testValidateQueryComplexitySkipsWhenAnalyzerMissing() {
        assertDoesNotThrow(() -> service.validateQueryComplexity("query { hello }"));
        assertNull(service.analyzeQueryComplexity("query { hello }"));
    }

    @Test
    void testValidateRequestRunsComplexityThenAccess() throws Exception {
        QueryComplexityAnalyzer analyzer = mock(QueryComplexityAnalyzer.class);
        inject(service, "complexityAnalyzer", analyzer);

        ResourceResolver resolver = mock(ResourceResolver.class);
        when(resolver.getUserID()).thenReturn("anonymous");

        assertThrows(
                UnauthorizedException.class,
                () -> service.validateRequest(resolver, "serverInfo", "query { serverInfo { version } }"));

        verify(analyzer).validate(anyString());
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
