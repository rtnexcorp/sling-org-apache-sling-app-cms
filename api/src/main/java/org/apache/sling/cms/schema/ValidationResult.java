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

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Result of schema validation.
 *
 * <p>Contains validation errors and warnings for content validated
 * against a schema.
 */
@ProviderType
public interface ValidationResult {

    /**
     * Checks if validation passed (no errors).
     *
     * @return true if valid, false if there are errors
     */
    boolean isValid();

    /**
     * Gets all validation errors.
     *
     * @return list of validation errors
     */
    @NotNull
    List<ValidationError> getErrors();

    /**
     * Gets all validation warnings.
     *
     * @return list of validation warnings
     */
    @NotNull
    List<ValidationError> getWarnings();

    /**
     * Gets the total number of errors.
     *
     * @return error count
     */
    int getErrorCount();

    /**
     * Gets the total number of warnings.
     *
     * @return warning count
     */
    int getWarningCount();

    /**
     * A single validation error or warning.
     */
    interface ValidationError {

        /**
         * Gets the field name that failed validation.
         *
         * @return the field name
         */
        @NotNull
        String getFieldName();

        /**
         * Gets the error message.
         *
         * @return the error message
         */
        @NotNull
        String getMessage();

        /**
         * Gets the validation rule that failed.
         *
         * @return the rule name (e.g., "required", "min", "max")
         */
        @NotNull
        String getRule();

        /**
         * Gets additional context about the error.
         *
         * @return error context, or null
         */
        String getContext();
    }
}
