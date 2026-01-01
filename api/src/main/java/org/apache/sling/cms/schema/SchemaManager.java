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
package org.apache.sling.cms.schema;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service for managing and retrieving content schemas.
 *
 * <p>This service provides access to schema definitions stored in the
 * repository and handles schema inheritance and resolution.
 */
@ProviderType
public interface SchemaManager {

    /**
     * Gets a schema by its ID, searching in the context of the given resource.
     *
     * @param contextResource the context resource for CAConfig resolution
     * @param schemaId the schema identifier
     * @return the schema, or null if not found
     */
    ContentSchema getSchema(@NotNull Resource contextResource, @NotNull String schemaId);

    /**
     * Gets all available schemas in the context of the given resource.
     *
     * @param contextResource the context resource for CAConfig resolution
     * @return list of all available schemas
     */
    @NotNull
    List<ContentSchema> getAllSchemas(@NotNull Resource contextResource);

    /**
     * Validates content against a schema.
     *
     * @param content the content resource to validate
     * @param schema the schema to validate against
     * @return validation result containing any errors
     */
    @NotNull
    ValidationResult validate(@NotNull Resource content, @NotNull ContentSchema schema);

    /**
     * Gets the schema associated with a content resource.
     *
     * <p>The schema is determined by the resource's schema property or
     * resource type mapping.
     *
     * @param content the content resource
     * @return the associated schema, or null if none
     */
    ContentSchema getSchemaForResource(@NotNull Resource content);
}
