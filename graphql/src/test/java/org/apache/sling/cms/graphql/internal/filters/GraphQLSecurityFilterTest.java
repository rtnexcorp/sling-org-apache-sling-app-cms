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
package org.apache.sling.cms.graphql.internal.filters;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.graphql.internal.security.GraphQLSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GraphQLSecurityFilterTest {

    private GraphQLSecurityFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new GraphQLSecurityFilter();

        GraphQLSecurityFilter.Config config = mock(GraphQLSecurityFilter.Config.class);
        when(config.enabled()).thenReturn(true);
        when(config.graphqlPaths()).thenReturn(new String[] {"/graphql"});

        inject(filter, "config", config);
    }

    @Test
    void testDisabledFilterPassesThrough() throws Exception {
        GraphQLSecurityFilter.Config config = mock(GraphQLSecurityFilter.Config.class);
        when(config.enabled()).thenReturn(false);
        when(config.graphqlPaths()).thenReturn(new String[] {"/graphql"});
        inject(filter, "config", config);

        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void testNonSlingRequestPassesThrough() throws Exception {
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void testNonGraphqlPathPassesThrough() throws Exception {
        GraphQLSecurityService securityService = mock(GraphQLSecurityService.class);
        inject(filter, "securityService", securityService);

        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/content/site/page.html");

        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(securityService);
    }

    @Test
    void testUnauthenticatedGraphqlRequestReturns401WhenAuthRequired() throws Exception {
        GraphQLSecurityService securityService = mock(GraphQLSecurityService.class);
        when(securityService.isAuthenticated(any())).thenReturn(false);
        when(securityService.requiresAuthentication("graphql")).thenReturn(true);
        inject(filter, "securityService", securityService);

        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/graphql.json");

        ResourceResolver resolver = mock(ResourceResolver.class);
        when(resolver.getUserID()).thenReturn("anonymous");
        when(request.getResourceResolver()).thenReturn(resolver);

        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
        assertTrue(sw.toString().contains("Authentication required"));
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
