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
 * A content schema definition for pages or content fragments.
 *
 * <p>Content schemas define the structure, fields, and validation rules
 * for content types. They enable schema-driven authoring, consistent
 * validation, and predictable content structure.
 *
 * <p>Schemas are stored at {@code /conf/{site}/schemas/*} and can be
 * adapted from schema definition resources.
 */
@ProviderType
public interface ContentSchema {

    /**
     * Gets the unique identifier for this schema.
     *
     * @return the schema ID (e.g., "article", "product-page")
     */
    @NotNull
    String getId();

    /**
     * Gets the human-readable title for this schema.
     *
     * @return the schema title
     */
    @NotNull
    String getTitle();

    /**
     * Gets the description of this schema.
     *
     * @return the schema description, or empty string if not set
     */
    @NotNull
    String getDescription();

    /**
     * Gets the schema version for tracking changes.
     *
     * @return the schema version (e.g., "1.0", "2.1")
     */
    @NotNull
    String getVersion();

    /**
     * Gets the list of field definitions in this schema.
     *
     * @return the list of schema fields
     */
    @NotNull
    List<SchemaField> getFields();

    /**
     * Gets a field definition by name.
     *
     * @param fieldName the name of the field
     * @return the field definition, or null if not found
     */
    SchemaField getField(@NotNull String fieldName);

    /**
     * Gets the resource backing this schema.
     *
     * @return the schema resource
     */
    @NotNull
    Resource getResource();

    /**
     * Gets the path to the schema resource.
     *
     * @return the schema path
     */
    @NotNull
    String getPath();

    /**
     * Checks if this schema extends another schema.
     *
     * @return true if this schema has a parent schema
     */
    boolean hasParentSchema();

    /**
     * Gets the parent schema ID if this schema extends another.
     *
     * @return the parent schema ID, or null if no parent
     */
    String getParentSchemaId();

    /**
     * Checks if this schema is enabled.
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();
}
