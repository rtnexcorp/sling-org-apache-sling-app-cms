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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.graphql.internal.security.ForbiddenException;
import org.apache.sling.cms.graphql.internal.security.GraphQLSecurityService;
import org.apache.sling.cms.graphql.internal.security.UnauthorizedException;
import org.apache.sling.graphql.api.SlingDataFetcher;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data fetcher that returns server information.
 * Demonstrates a more complex GraphQL query with nested fields.
 * This query requires authentication.
 *
 * <p>Example GraphQL query:
 * <pre>
 * {
 *   serverInfo {
 *     version
 *     timestamp
 *     environment
 *     graphqlVersion
 *   }
 * }
 * </pre>
 *
 * <p>Example curl commands:
 * <pre>
 * # Authenticated request with basic auth
 * curl -u admin:admin -X POST http://localhost:8082/graphql.json \
 *   -H "Content-Type: application/json" \
 *   -d '{"query": "{ serverInfo { version timestamp environment graphqlVersion } }"}'
 *
 * # Unauthenticated request (will return 401 Unauthorized)
 * curl -X POST http://localhost:8082/graphql.json \
 *   -H "Content-Type: application/json" \
 *   -d '{"query": "{ serverInfo { version } }"}'
 * </pre>
 */
@Component(
        service = SlingDataFetcher.class,
        property = {"name=slingcms/serverInfo"})
public class ServerInfoDataFetcher implements SlingDataFetcher<Map<String, Object>> {

    private static final Logger log = LoggerFactory.getLogger(ServerInfoDataFetcher.class);

    @Reference
    private GraphQLSecurityService securityService;

    @Override
    public Map<String, Object> get(SlingDataFetcherEnvironment environment) throws Exception {
        try {
            if (environment == null) {
                log.warn("GraphQL serverInfo called with null environment");
                throw new IllegalArgumentException("Missing GraphQL execution context");
            }

            Resource currentResource = environment.getCurrentResource();
            if (currentResource == null) {
                log.warn("GraphQL serverInfo called without a current resource");
                throw new IllegalArgumentException("Missing GraphQL resource context");
            }

            ResourceResolver resolver = currentResource.getResourceResolver();
            if (resolver == null) {
                log.warn("GraphQL serverInfo called without a resource resolver");
                throw new IllegalArgumentException("Missing resource resolver");
            }

            // Validate access - this query requires authentication
            securityService.validateAccess(resolver, "serverInfo");

            Map<String, Object> serverInfo = new HashMap<>();

            serverInfo.put("version", "1.1.9-SNAPSHOT");
            serverInfo.put("timestamp", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            serverInfo.put("environment", "development");
            serverInfo.put("graphqlVersion", "0.0.24");

            log.debug("Returning serverInfo for user '{}'", resolver.getUserID());
            return serverInfo;

        } catch (UnauthorizedException | ForbiddenException e) {
            // Expected/controlled rejection (authz/authn)
            log.info("Access denied for serverInfo query: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            // Bad/insufficient execution context
            log.warn("Invalid execution context for serverInfo query: {}", e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Unexpected error while handling GraphQL query 'serverInfo'", e);
            throw new Exception("Failed to process serverInfo query");
        }
    }
}
