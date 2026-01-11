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

import org.apache.sling.graphql.api.SlingDataFetcher;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.osgi.service.component.annotations.Component;

/**
 * Data fetcher for the hello GraphQL query.
 * Returns a welcome message for the Sling CMS GraphQL API.
 * This query allows anonymous access (no authentication required).
 *
 * <p>Example GraphQL query:
 * <pre>
 * {
 *   hello
 * }
 * </pre>
 *
 * <p>Example curl command (no authentication needed):
 * <pre>
 * curl -X POST http://localhost:8082/graphql.json \
 *   -H "Content-Type: application/json" \
 *   -d '{"query": "{ hello }"}'
 * </pre>
 */
@Component(
        service = SlingDataFetcher.class,
        property = {"name=slingcms/hello"})
public class HelloDataFetcher implements SlingDataFetcher<String> {

    @Override
    public String get(SlingDataFetcherEnvironment environment) throws Exception {
        return "Apache Sling CMS GraphQL API - Version 1.1.9 | Ready to serve content queries | "
                + "Use this endpoint to access CMS content, pages, assets, and metadata through GraphQL queries.";
    }
}
