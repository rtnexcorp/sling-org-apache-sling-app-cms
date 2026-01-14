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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.graphql.internal.dto.ContentFragmentDTO;
import org.apache.sling.cms.graphql.internal.security.ForbiddenException;
import org.apache.sling.cms.graphql.internal.security.GraphQLSecurityService;
import org.apache.sling.cms.graphql.internal.security.UnauthorizedException;
import org.apache.sling.cms.graphql.internal.services.ContentFragmentQueryService;
import org.apache.sling.graphql.api.SlingDataFetcher;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data fetcher for retrieving a single content fragment.
 *
 * <p>GraphQL schema wiring should reference this fetcher via:
 * {@code @fetcher(name: "slingcms/fragment")}
 */
@Component(
        service = SlingDataFetcher.class,
        property = {"name=slingcms/fragment"})
public class ContentFragmentDataFetcher implements SlingDataFetcher<ContentFragmentDTO> {

    private static final Logger log = LoggerFactory.getLogger(ContentFragmentDataFetcher.class);

    @Reference
    private GraphQLSecurityService securityService;

    @Reference
    private ContentFragmentQueryService fragmentQueryService;

    @Override
    public ContentFragmentDTO get(SlingDataFetcherEnvironment environment) throws Exception {
        try {
            if (environment == null) {
                throw new IllegalArgumentException("Missing GraphQL execution context");
            }

            Resource currentResource = environment.getCurrentResource();
            if (currentResource == null) {
                throw new IllegalArgumentException("Missing GraphQL resource context");
            }

            ResourceResolver resolver = currentResource.getResourceResolver();
            if (resolver == null) {
                throw new IllegalArgumentException("Missing resource resolver");
            }

            // GraphQL field argument name convention: "id" (path)
            String fragmentId = environment.getArgument("id");
            if (fragmentId == null || fragmentId.isBlank()) {
                log.debug("Fragment query called without 'id' argument");
                return null;
            }

            // Enforce API query access and read access to the referenced resource.
            securityService.validateAccess(resolver, "fragment");
            securityService.validateReadAccess(resolver, fragmentId);

            return fragmentQueryService.getFragmentById(resolver, fragmentId);

        } catch (UnauthorizedException | ForbiddenException e) {
            // Expected/controlled rejection (authz/authn)
            log.info("Access denied for fragment query: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid execution context for fragment query: {}", e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Unexpected error while handling GraphQL query 'fragment'", e);
            throw new Exception("Failed to process fragment query");
        }
    }
}
