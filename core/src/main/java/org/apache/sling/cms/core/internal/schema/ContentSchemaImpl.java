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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.CMSConstants;
import org.apache.sling.cms.schema.ContentSchema;
import org.apache.sling.cms.schema.SchemaField;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of ContentSchema.
 */
@Model(adaptables = Resource.class, adapters = ContentSchema.class)
public class ContentSchemaImpl implements ContentSchema {

    @Inject
    @Named(CMSConstants.PN_TITLE)
    private String title;

    @Inject
    @Named(CMSConstants.PN_DESCRIPTION)
    @Optional
    private String description;

    @Inject
    @Named("version")
    @Optional
    private String version;

    @Inject
    @Named("enabled")
    @Optional
    private Boolean enabled;

    @Inject
    @Named("parentSchema")
    @Optional
    private String parentSchemaId;

    private final Resource resource;
    private List<SchemaField> fields;

    public ContentSchemaImpl(Resource resource) {
        this.resource = resource;
    }

    @Override
    @NotNull
    public String getId() {
        return resource.getName();
    }

    @Override
    @NotNull
    public String getTitle() {
        return title != null ? title : getId();
    }

    @Override
    @NotNull
    public String getDescription() {
        return description != null ? description : "";
    }

    @Override
    @NotNull
    public String getVersion() {
        return version != null ? version : "1.0";
    }

    @Override
    @NotNull
    public List<SchemaField> getFields() {
        if (fields == null) {
            fields = loadFields();
        }
        return fields;
    }

    @Override
    public SchemaField getField(@NotNull String fieldName) {
        return getFields().stream()
                .filter(f -> f.getName().equals(fieldName))
                .findFirst()
                .orElse(null);
    }

    @Override
    @NotNull
    public Resource getResource() {
        return resource;
    }

    @Override
    @NotNull
    public String getPath() {
        return resource.getPath();
    }

    @Override
    public boolean hasParentSchema() {
        return parentSchemaId != null && !parentSchemaId.isEmpty();
    }

    @Override
    public String getParentSchemaId() {
        return parentSchemaId;
    }

    @Override
    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    private List<SchemaField> loadFields() {
        Resource fieldsResource = resource.getChild("fields");
        if (fieldsResource == null) {
            return Collections.emptyList();
        }

        return StreamSupport.stream(fieldsResource.getChildren().spliterator(), false)
                .map(r -> r.adaptTo(SchemaField.class))
                .filter(f -> f != null)
                .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
