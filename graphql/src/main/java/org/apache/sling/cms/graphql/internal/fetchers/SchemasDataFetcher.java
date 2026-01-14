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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.graphql.internal.security.ForbiddenException;
import org.apache.sling.cms.graphql.internal.security.GraphQLSecurityService;
import org.apache.sling.cms.graphql.internal.security.UnauthorizedException;
import org.apache.sling.cms.schema.ContentSchema;
import org.apache.sling.cms.schema.SchemaManager;
import org.apache.sling.graphql.api.SlingDataFetcher;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data fetcher for schema introspection.
 *
 * <p>Returns all enabled {@link ContentSchema}s available in the context of the current resource.
 *
 * <p>GraphQL schema wiring should reference this fetcher via:
 * {@code @fetcher(name: "slingcms/schemas")}
 */
@Component(
        service = SlingDataFetcher.class,
        property = {"name=slingcms/schemas"})
public class SchemasDataFetcher implements SlingDataFetcher<List<ContentSchema>> {

    private static final Logger log = LoggerFactory.getLogger(SchemasDataFetcher.class);

    @Reference
    private GraphQLSecurityService securityService;

    @Reference
    private SchemaManager schemaManager;

    @Override
    public List<ContentSchema> get(SlingDataFetcherEnvironment environment) throws Exception {
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

            // Schema metadata should not be exposed anonymously by default.
            securityService.validateAccess(resolver, "schemas");

            List<ContentSchema> schemas = schemaManager.getAllSchemas(currentResource);
            if (schemas == null) {
                return Collections.emptyList();
            }

            return schemas;

        } catch (UnauthorizedException | ForbiddenException e) {
            log.info("Access denied for schemas query: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid execution context for schemas query: {}", e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Unexpected error while handling GraphQL query 'schemas'", e);
            throw new Exception("Failed to process schemas query");
        }
    }
}
