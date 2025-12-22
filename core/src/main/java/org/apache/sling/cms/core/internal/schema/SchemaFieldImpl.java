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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.schema.FieldType;
import org.apache.sling.cms.schema.SchemaField;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of SchemaField.
 */
@Model(adaptables = Resource.class, adapters = SchemaField.class)
public class SchemaFieldImpl implements SchemaField {

    @Inject
    @Named("label")
    private String label;

    @Inject
    @Named("description")
    @Optional
    private String description;

    @Inject
    @Named("type")
    private String typeString;

    @Inject
    @Named("required")
    @Optional
    private Boolean required;

    @Inject
    @Named("multiple")
    @Optional
    private Boolean multiple;

    @Inject
    @Named("defaultValue")
    @Optional
    private Object defaultValue;

    @Inject
    @Named("componentType")
    @Optional
    private String componentType;

    @Inject
    @Named("order")
    @Optional
    private Integer order;

    private final Resource resource;
    private Map<String, Object> validation;
    private Map<String, Object> properties;

    public SchemaFieldImpl(Resource resource) {
        this.resource = resource;
    }

    @Override
    @NotNull
    public String getName() {
        return resource.getName();
    }

    @Override
    @NotNull
    public String getLabel() {
        return label != null ? label : getName();
    }

    @Override
    @NotNull
    public String getDescription() {
        return description != null ? description : "";
    }

    @Override
    @NotNull
    public FieldType getType() {
        if (typeString == null) {
            return FieldType.STRING;
        }
        try {
            return FieldType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FieldType.STRING;
        }
    }

    @Override
    public boolean isRequired() {
        return required != null && required;
    }

    @Override
    public boolean isMultiple() {
        return multiple != null && multiple;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    @NotNull
    public Map<String, Object> getValidation() {
        if (validation == null) {
            validation = loadValidation();
        }
        return validation;
    }

    @Override
    public String getComponentType() {
        return componentType;
    }

    @Override
    @NotNull
    public Map<String, Object> getProperties() {
        if (properties == null) {
            properties = loadProperties();
        }
        return properties;
    }

    @Override
    public int getOrder() {
        return order != null ? order : 0;
    }

    private Map<String, Object> loadValidation() {
        Resource validationResource = resource.getChild("validation");
        if (validationResource == null) {
            return Collections.emptyMap();
        }
        ValueMap vm = validationResource.getValueMap();
        return new HashMap<>(vm);
    }

    private Map<String, Object> loadProperties() {
        Resource propsResource = resource.getChild("properties");
        if (propsResource == null) {
            return Collections.emptyMap();
        }
        ValueMap vm = propsResource.getValueMap();
        return new HashMap<>(vm);
    }
}
