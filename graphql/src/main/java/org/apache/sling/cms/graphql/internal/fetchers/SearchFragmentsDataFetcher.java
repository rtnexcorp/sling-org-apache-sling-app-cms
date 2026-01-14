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
 * Data fetcher for full-text search across content fragments.
 *
 * <p>GraphQL schema wiring should reference this fetcher via:
 * {@code @fetcher(name: "slingcms/fragmentSearch")}
 */
@Component(
        service = SlingDataFetcher.class,
        property = {"name=slingcms/fragmentSearch"})
public class SearchFragmentsDataFetcher implements SlingDataFetcher<FragmentConnectionDTO> {

    private static final Logger log = LoggerFactory.getLogger(SearchFragmentsDataFetcher.class);

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

            String searchTerm = environment.getArgument("searchTerm");
            if (searchTerm == null || searchTerm.isBlank()) {
                return new FragmentConnectionDTO(
                        java.util.Collections.emptyList(),
                        new org.apache.sling.cms.graphql.internal.dto.PageInfoDTO(),
                        0);
            }

            securityService.validateAccess(resolver, "fragmentSearch");

            String schemaType = environment.getArgument("schemaType");
            Integer limit = environment.getArgument("limit");
            Integer offset = environment.getArgument("offset");

            return fragmentQueryService.searchFragments(resolver, searchTerm, schemaType, limit, offset);

        } catch (UnauthorizedException | ForbiddenException e) {
            log.info("Access denied for fragmentSearch query: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid execution context for fragmentSearch query: {}", e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Unexpected error while handling GraphQL query 'fragmentSearch'", e);
            throw new Exception("Failed to process fragmentSearch query");
        }
    }
}
