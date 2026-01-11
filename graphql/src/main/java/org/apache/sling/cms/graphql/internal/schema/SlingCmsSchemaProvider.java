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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

    /**
     * Load the schema from a bundled resource to keep SDL readable while still returning a String.
     */
    private static final String SCHEMA_RESOURCE_PATH = "/apps/sling-cms/servlet/GQLschema.gql";

    /**
     * Fallback location for unit tests (schema file is in this module's resources, not as /apps/... on the test classpath).
     */
    private static final String SCHEMA_TEST_RESOURCE_PATH = "/jcr_root/apps/sling-cms/servlet/GQLschema.gql";

    @Override
    public String getSchema(@NotNull Resource schemaResource, String[] selectors) throws IOException {
        try {
            // selectors can be null depending on invocation
            String selectorLog = selectors == null ? "[]" : Arrays.toString(selectors);
            log.debug("Providing schema for resource: {} with selectors: {}", schemaResource.getPath(), selectorLog);

            InputStream in = SlingCmsSchemaProvider.class.getResourceAsStream(SCHEMA_RESOURCE_PATH);
            if (in == null) {
                in = SlingCmsSchemaProvider.class.getResourceAsStream(SCHEMA_TEST_RESOURCE_PATH);
            }

            try (InputStream schemaStream = in) {
                if (schemaStream == null) {
                    throw new IOException("Unable to locate schema resource on classpath: " + SCHEMA_RESOURCE_PATH
                            + " (also tried " + SCHEMA_TEST_RESOURCE_PATH + ")");
                }
                // Keep it a plain string so Sling GraphQL Core parses it normally (no script execution)
                return new String(schemaStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (RuntimeException e) {
            log.error("Failed to provide GraphQL schema for resource: {}", schemaResource.getPath(), e);
            throw new IOException("Unable to load GraphQL schema");
        }
    }
}
