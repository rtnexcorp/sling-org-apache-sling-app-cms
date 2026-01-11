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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for authenticating and authorizing GraphQL requests.
 * Provides centralized security checks for all GraphQL data fetchers.
 */
@Component(service = GraphQLSecurityService.class)
@Designate(ocd = GraphQLSecurityService.Config.class)
public class GraphQLSecurityService {

    private static final Logger log = LoggerFactory.getLogger(GraphQLSecurityService.class);

    @ObjectClassDefinition(name = "Sling CMS GraphQL Security Configuration")
    public @interface Config {

        @AttributeDefinition(
                name = "Enable Authentication",
                description = "Require authentication for GraphQL queries. If false, allows anonymous access.")
        boolean enableAuthentication() default true;

        @AttributeDefinition(
                name = "Allowed Anonymous Queries",
                description = "GraphQL queries that don't require authentication (e.g., 'hello', 'serverInfo')")
        String[] allowedAnonymousQueries() default {"hello"};

        @AttributeDefinition(
                name = "Required Groups",
                description = "User groups that have access to GraphQL API (empty = all authenticated users)")
        String[] requiredGroups() default {};
    }

    private volatile Config config;

    @org.osgi.service.component.annotations.Activate
    protected void activate(Config config) {
        this.config = config;
        log.info("GraphQL Security Service activated - Authentication enabled: {}", config.enableAuthentication());
    }

    /**
     * Checks if the user is authenticated.
     *
     * @param resolver the ResourceResolver to check
     * @return true if user is authenticated (not anonymous)
     */
    public boolean isAuthenticated(ResourceResolver resolver) {
        if (resolver == null) {
            log.warn("ResourceResolver is null");
            return false;
        }

        String userId = resolver.getUserID();
        boolean authenticated = userId != null && !"anonymous".equals(userId);

        log.debug("User '{}' authentication check: {}", userId, authenticated);
        return authenticated;
    }

    /**
     * Checks if authentication is required for a specific query.
     *
     * @param queryName the name of the GraphQL query being executed
     * @return true if authentication is required
     */
    public boolean requiresAuthentication(String queryName) {
        if (!config.enableAuthentication()) {
            return false;
        }

        Set<String> allowedQueries = new HashSet<>(Arrays.asList(config.allowedAnonymousQueries()));
        boolean required = !allowedQueries.contains(queryName);

        log.debug("Query '{}' requires authentication: {}", queryName, required);
        return required;
    }

    /**
     * Validates that the user has permission to execute a GraphQL query.
     *
     * @param resolver the ResourceResolver for the current user
     * @param queryName the name of the query being executed
     * @throws UnauthorizedException if user is not authenticated
     * @throws ForbiddenException if user lacks required permissions
     */
    public void validateAccess(ResourceResolver resolver, String queryName)
            throws UnauthorizedException, ForbiddenException {

        // Check if authentication is required for this query
        if (!requiresAuthentication(queryName)) {
            log.debug("Query '{}' allows anonymous access", queryName);
            return;
        }

        // Check authentication
        if (!isAuthenticated(resolver)) {
            log.warn("Unauthorized access attempt to query: {}", queryName);
            throw new UnauthorizedException("Authentication required to access GraphQL API");
        }

        // Check group membership if required
        String[] requiredGroups = config.requiredGroups();
        if (requiredGroups.length > 0) {
            if (!hasRequiredGroup(resolver, requiredGroups)) {
                log.warn("User '{}' lacks required group membership for query: {}", resolver.getUserID(), queryName);
                throw new ForbiddenException("Insufficient permissions to access GraphQL API");
            }
        }

        log.debug("User '{}' authorized for query: {}", resolver.getUserID(), queryName);
    }

    /**
     * Checks if the user has permission to read a specific resource.
     *
     * @param resolver the ResourceResolver for the current user
     * @param resourcePath the path to check
     * @return true if user can read the resource
     */
    public boolean canRead(ResourceResolver resolver, String resourcePath) {
        if (resolver == null || resourcePath == null) {
            return false;
        }

        try {
            Resource resource = resolver.getResource(resourcePath);
            if (resource == null) {
                log.debug("Resource not found: {}", resourcePath);
                return false;
            }

            // Try to adapt to JCR Session to check permissions
            Session session = resolver.adaptTo(Session.class);
            if (session != null) {
                boolean canRead = session.hasPermission(resourcePath, Session.ACTION_READ);
                log.debug("User '{}' read permission for '{}': {}", resolver.getUserID(), resourcePath, canRead);
                return canRead;
            }

            // Fallback: if resource exists and we can get it, assume read permission
            return true;

        } catch (Exception e) {
            log.error("Error checking read permission for path: {}", resourcePath, e);
            return false;
        }
    }

    /**
     * Validates that the user can read a specific resource.
     *
     * @param resolver the ResourceResolver for the current user
     * @param resourcePath the path to validate
     * @throws ForbiddenException if user cannot read the resource
     */
    public void validateReadAccess(ResourceResolver resolver, String resourcePath) throws ForbiddenException {

        if (!canRead(resolver, resourcePath)) {
            log.warn("User '{}' denied read access to: {}", resolver.getUserID(), resourcePath);
            throw new ForbiddenException("Access denied to resource: " + resourcePath);
        }
    }

    /**
     * Checks if user belongs to any of the required groups.
     *
     * @param resolver the ResourceResolver for the current user
     * @param requiredGroups array of group names that grant access
     * @return true if user belongs to any of the required groups
     */
    private boolean hasRequiredGroup(ResourceResolver resolver, String[] requiredGroups) {
        try {
            Session session = resolver.adaptTo(Session.class);
            if (!(session instanceof JackrabbitSession)) {
                log.warn("Session is not a JackrabbitSession, cannot check group membership");
                return false;
            }

            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            if (userManager == null) {
                log.warn("Unable to retrieve UserManager");
                return false;
            }

            String userId = resolver.getUserID();
            Authorizable authorizable = userManager.getAuthorizable(userId);
            if (authorizable == null) {
                log.warn("Unable to retrieve Authorizable for user: {}", userId);
                return false;
            }

            if (!(authorizable instanceof User)) {
                log.warn("Authorizable is not a User: {}", userId);
                return false;
            }

            Set<String> requiredGroupSet = new HashSet<>(Arrays.asList(requiredGroups));
            Iterator<Group> groups = ((User) authorizable).memberOf();

            while (groups.hasNext()) {
                Group group = groups.next();
                String groupId = group.getID();
                log.debug("User '{}' is member of group '{}'", userId, groupId);

                if (requiredGroupSet.contains(groupId)) {
                    log.debug("User '{}' has required group membership: {}", userId, groupId);
                    return true;
                }
            }

            log.debug("User '{}' is not a member of any required groups: {}", userId, Arrays.toString(requiredGroups));
            return false;

        } catch (RepositoryException e) {
            log.error("Error checking group membership", e);
            return false;
        }
    }
}
