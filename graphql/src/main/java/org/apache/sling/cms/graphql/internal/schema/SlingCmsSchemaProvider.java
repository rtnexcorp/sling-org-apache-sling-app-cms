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
package org.apache.sling.cms.graphql.internal.schema;

import java.io.IOException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.graphql.api.SchemaProvider;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom schema provider for Sling CMS GraphQL that returns the schema
 * as a string to avoid recursive execution of .gql files.
 */
@Component(
        service = SchemaProvider.class,
        property = {"name=slingcms/schema"})
public class SlingCmsSchemaProvider implements SchemaProvider {

    private static final Logger log = LoggerFactory.getLogger(SlingCmsSchemaProvider.class);

    private static final String SCHEMA = "type Query {\n"
            + "  hello: String @fetcher(name: \"slingcms/hello\")\n"
            + "  serverInfo: ServerInfo @fetcher(name: \"slingcms/serverInfo\")\n"
            + "}\n"
            + "\n"
            + "type ServerInfo {\n"
            + "  version: String\n"
            + "  timestamp: String\n"
            + "  environment: String\n"
            + "  graphqlVersion: String\n"
            + "}\n";

    @Override
    public String getSchema(@NotNull Resource schemaResource, String[] selectors) throws IOException {
        log.debug("Providing schema for resource: {} with selectors: {}", schemaResource.getPath(), selectors);
        return SCHEMA;
    }
}
