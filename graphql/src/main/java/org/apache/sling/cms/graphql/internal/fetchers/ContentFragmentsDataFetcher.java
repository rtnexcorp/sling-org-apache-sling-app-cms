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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.graphql.internal.dto.FragmentConnectionDTO;
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
 * Data fetcher for listing content fragments with filtering, sorting, and pagination.
 *
 * <p>GraphQL schema wiring should reference this fetcher via:
 * {@code @fetcher(name: "slingcms/fragments")}
 */
@Component(
        service = SlingDataFetcher.class,
        property = {"name=slingcms/fragments"})
public class ContentFragmentsDataFetcher implements SlingDataFetcher<FragmentConnectionDTO> {

    private static final Logger log = LoggerFactory.getLogger(ContentFragmentsDataFetcher.class);

    @Reference
    private GraphQLSecurityService securityService;

    @Reference
    private ContentFragmentQueryService fragmentQueryService;

    @Override
    public FragmentConnectionDTO get(SlingDataFetcherEnvironment environment) throws Exception {
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

            // Enforce query-level access. Path-specific read checks are done per fragment by Sling/JCR,
            // but we also validate the base path argument if provided.
            securityService.validateAccess(resolver, "fragments");

            String schemaType = environment.getArgument("schemaType");
            String path = environment.getArgument("path");
            List<Map<String, String>> filters = environment.getArgument("filters");

            String sortField = environment.getArgument("sortField");
            String sortDirection = environment.getArgument("sortDirection");

            Integer limit = environment.getArgument("limit");
            Integer offset = environment.getArgument("offset");
            String cursor = environment.getArgument("cursor");

            if (path != null && !path.isBlank()) {
                // Ensure caller can read the requested base path (prevents probing).
                securityService.validateReadAccess(resolver, path);
            }

            if (filters == null) {
                filters = Collections.emptyList();
            }

            return fragmentQueryService.listFragments(
                    resolver, schemaType, path, filters, sortField, sortDirection, limit, offset, cursor);

        } catch (UnauthorizedException | ForbiddenException e) {
            log.info("Access denied for fragments query: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid execution context for fragments query: {}", e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Unexpected error while handling GraphQL query 'fragments'", e);
            throw new Exception("Failed to process fragments query");
        }
    }
}
