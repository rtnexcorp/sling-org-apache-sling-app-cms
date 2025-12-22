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

import org.osgi.annotation.versioning.ProviderType;

/**
 * Enumeration of supported field types in content schemas.
 */
@ProviderType
public enum FieldType {
    /** String/text field */
    STRING,

    /** Multi-line text field */
    TEXT,

    /** Rich text field */
    RICHTEXT,

    /** Boolean field */
    BOOLEAN,

    /** Integer number field */
    INTEGER,

    /** Decimal number field */
    DECIMAL,

    /** Date field */
    DATE,

    /** Date and time field */
    DATETIME,

    /** Reference to another resource */
    REFERENCE,

    /** Tag/taxonomy reference */
    TAG,

    /** Asset/file reference */
    ASSET,

    /** Dropdown selection */
    SELECT,

    /** Radio button selection */
    RADIO,

    /** Checkbox selection (multi-valued) */
    CHECKBOX,

    /** Hidden field */
    HIDDEN,

    /** Nested object/component */
    OBJECT,

    /** Array of objects */
    ARRAY
}
