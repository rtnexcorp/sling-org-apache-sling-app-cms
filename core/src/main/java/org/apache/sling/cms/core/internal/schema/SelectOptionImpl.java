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
package org.apache.sling.cms.core.internal.schema;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.schema.SelectOption;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of SelectOption for SELECT field types.
 */
@Model(adaptables = Resource.class, adapters = SelectOption.class)
public class SelectOptionImpl implements SelectOption {

    @Inject
    @Named("label")
    @Optional
    private String label;

    @Inject
    @Named("value")
    @Optional
    private String value;

    private final Resource resource;

    public SelectOptionImpl(Resource resource) {
        this.resource = resource;
    }

    @Override
    @NotNull
    public String getLabel() {
        // Fall back to value if label is not set
        if (label != null && !label.isEmpty()) {
            return label;
        }
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return resource.getName();
    }

    @Override
    @NotNull
    public String getValue() {
        // Fall back to resource name if value is not set
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return resource.getName();
    }
}
