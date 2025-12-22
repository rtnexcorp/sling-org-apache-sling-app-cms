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

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A field definition within a content schema.
 *
 * <p>Schema fields define the structure, type, and validation rules
 * for individual content properties.
 */
@ProviderType
public interface SchemaField {

    /**
     * Gets the field name (property name in JCR).
     *
     * @return the field name
     */
    @NotNull
    String getName();

    /**
     * Gets the human-readable label for this field.
     *
     * @return the field label
     */
    @NotNull
    String getLabel();

    /**
     * Gets the description or help text for this field.
     *
     * @return the field description, or empty string if not set
     */
    @NotNull
    String getDescription();

    /**
     * Gets the field type.
     *
     * @return the field type
     */
    @NotNull
    FieldType getType();

    /**
     * Checks if this field is required.
     *
     * @return true if required, false otherwise
     */
    boolean isRequired();

    /**
     * Checks if this field supports multiple values.
     *
     * @return true if multi-valued, false otherwise
     */
    boolean isMultiple();

    /**
     * Gets the default value for this field.
     *
     * @return the default value, or null if not set
     */
    Object getDefaultValue();

    /**
     * Gets the validation rules for this field.
     *
     * @return map of validation rule names to parameters
     */
    @NotNull
    Map<String, Object> getValidation();

    /**
     * Gets the component resource type for rendering this field.
     *
     * @return the component resource type, or null to use default
     */
    String getComponentType();

    /**
     * Gets additional properties for this field.
     *
     * @return map of additional properties
     */
    @NotNull
    Map<String, Object> getProperties();

    /**
     * Gets the order/position of this field in the schema.
     *
     * @return the field order (lower numbers come first)
     */
    int getOrder();
}
