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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.schema.ContentSchema;
import org.apache.sling.cms.schema.SchemaManager;
import org.apache.sling.cms.schema.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of SchemaManager.
 */
@Component(service = SchemaManager.class)
public class SchemaManagerImpl implements SchemaManager {

    private static final Logger log = LoggerFactory.getLogger(SchemaManagerImpl.class);
    private static final String SCHEMAS_PATH = "/conf/global/site/schemas";

    @Override
    public ContentSchema getSchema(@NotNull Resource contextResource, @NotNull String schemaId) {
        ResourceResolver resolver = contextResource.getResourceResolver();
        Resource schemasResource = resolver.getResource(SCHEMAS_PATH);

        if (schemasResource == null) {
            log.debug("Schemas resource not found at: {}", SCHEMAS_PATH);
            return null;
        }

        Iterable<Resource> schemaResources = schemasResource.getChildren();
        for (Resource schemaResource : schemaResources) {
            if (schemaId.equals(schemaResource.getName())) {
                ContentSchema schema = schemaResource.adaptTo(ContentSchema.class);
                if (schema != null && schema.isEnabled()) {
                    return schema;
                }
            }
        }

        log.debug("Schema not found: {}", schemaId);
        return null;
    }

    @Override
    @NotNull
    public List<ContentSchema> getAllSchemas(@NotNull Resource contextResource) {
        ResourceResolver resolver = contextResource.getResourceResolver();
        Resource schemasResource = resolver.getResource(SCHEMAS_PATH);

        if (schemasResource == null) {
            log.debug("Schemas resource not found at: {}", SCHEMAS_PATH);
            return new ArrayList<>();
        }

        Iterable<Resource> schemaResources = schemasResource.getChildren();
        return StreamSupport.stream(schemaResources.spliterator(), false)
                .map(r -> r.adaptTo(ContentSchema.class))
                .filter(s -> s != null && s.isEnabled())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @NotNull
    public ValidationResult validate(@NotNull Resource content, @NotNull ContentSchema schema) {
        SchemaValidator validator = new SchemaValidator(schema);
        return validator.validate(content);
    }

    @Override
    public ContentSchema getSchemaForResource(@NotNull Resource content) {
        // Try to get schema from resource property
        String schemaId = content.getValueMap().get("schemaId", String.class);
        if (schemaId != null) {
            return getSchema(content, schemaId);
        }

        // Try to get schema from sling:resourceType mapping
        String resourceType = content.getResourceType();
        if (resourceType != null) {
            // Check if there's a schema with the same name as the resource type
            String schemaName = resourceType.substring(resourceType.lastIndexOf('/') + 1);
            ContentSchema schema = getSchema(content, schemaName);
            if (schema != null) {
                return schema;
            }
        }

        log.debug("No schema found for resource: {}", content.getPath());
        return null;
    }
}
