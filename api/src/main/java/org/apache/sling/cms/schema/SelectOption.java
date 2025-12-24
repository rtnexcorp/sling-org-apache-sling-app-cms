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

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a single option in a SELECT field.
 *
 * <p>Each option has a human-readable label and a stored value.
 */
@ProviderType
public interface SelectOption {

    /**
     * Gets the human-readable label for this option.
     *
     * @return the option label
     */
    @NotNull
    String getLabel();

    /**
     * Gets the value that will be stored when this option is selected.
     *
     * @return the option value
     */
    @NotNull
    String getValue();
}
