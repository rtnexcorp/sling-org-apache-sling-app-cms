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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.graphql.internal.security.GraphQLSecurityService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet filter that enforces authentication and authorization for GraphQL endpoints.
 * This filter intercepts requests to GraphQL resources and validates user access
 * before allowing the request to proceed.
 */
@Component(
        service = Filter.class,
        property = {"sling.filter.scope=request", "sling.filter.pattern=/graphql.*", "service.ranking:Integer=1000"})
@Designate(ocd = GraphQLSecurityFilter.Config.class)
public class GraphQLSecurityFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(GraphQLSecurityFilter.class);

    @ObjectClassDefinition(name = "Sling CMS GraphQL Security Filter Configuration")
    public @interface Config {

        @AttributeDefinition(name = "Enable Filter", description = "Enable the GraphQL security filter")
        boolean enabled() default true;

        @AttributeDefinition(
                name = "GraphQL Paths",
                description = "URL patterns that should be protected by this filter")
        String[] graphqlPaths() default {"/graphql"};
    }

    @Reference
    private GraphQLSecurityService securityService;

    private volatile Config config;

    @org.osgi.service.component.annotations.Activate
    protected void activate(Config config) {
        this.config = config;
        log.info("GraphQL Security Filter activated - Enabled: {}", config.enabled());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Nothing required
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            if (!config.enabled()) {
                chain.doFilter(request, response);
                return;
            }

            if (!(request instanceof SlingHttpServletRequest)) {
                chain.doFilter(request, response);
                return;
            }

            SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
            String requestUri = slingRequest.getRequestURI();

            // Check if this request is for a GraphQL endpoint
            if (!isGraphQLRequest(requestUri)) {
                chain.doFilter(request, response);
                return;
            }

            if (!(response instanceof HttpServletResponse)) {
                log.warn("GraphQL security filter invoked with non-HTTP response for request {}", requestUri);
                chain.doFilter(request, response);
                return;
            }

            ResourceResolver resolver = slingRequest.getResourceResolver();

            log.debug("Processing GraphQL security for request: {} (userId={})", requestUri, resolver.getUserID());

            // Perform basic authentication check at the filter level
            // Individual data fetchers will perform query-specific authorization
            if (!securityService.isAuthenticated(resolver)) {
                // Check if authentication is required (may be disabled in config)
                if (securityService.requiresAuthentication("graphql")) {
                    log.warn("Unauthenticated access attempt to GraphQL endpoint: {}", requestUri);
                    sendUnauthorizedResponse((HttpServletResponse) response);
                    return;
                }
            }

            chain.doFilter(request, response);

        } catch (RuntimeException e) {
            String uri = request instanceof HttpServletRequest
                    ? ((HttpServletRequest) request).getRequestURI()
                    : "<unknown>";
            log.error("Unexpected error in GraphQL security filter for request {}", uri, e);
            if (response instanceof HttpServletResponse) {
                sendServerErrorResponse((HttpServletResponse) response);
                return;
            }
            throw e;
        }
    }

    private boolean isGraphQLRequest(String requestUri) {
        if (requestUri == null) {
            return false;
        }

        for (String path : config.graphqlPaths()) {
            if (requestUri.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"errors\":[{\"message\":\"Authentication required\"}]}");
    }

    private void sendServerErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        response.getWriter().write("{\"errors\":[{\"message\":\"Internal server error\"}]}");
    }

    @Override
    public void destroy() {
        // Nothing required
    }
}
