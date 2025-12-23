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
package org.apache.sling.cms.core.models;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import java.util.Collections;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.schema.ContentSchema;
import org.apache.sling.cms.schema.SchemaField;
import org.apache.sling.cms.schema.SchemaManager;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for rendering fragment editor forms dynamically based on schema.
 * Generates appropriate form fields for each field defined in the fragment's schema.
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class FragmentEditor {

    @SlingObject
    private Resource resource;

    @Inject
    @Optional
    private String schemaId;

    @OSGiService
    @Optional
    private SchemaManager schemaManager;

    private ContentSchema schema;
    private ValueMap properties;

    @PostConstruct
    protected void init() {
        if (schemaId != null && schemaManager != null) {
            schema = schemaManager.getSchema(resource, schemaId);
        }
        properties = resource.getValueMap();
    }

    /**
     * Get the schema for this fragment.
     *
     * @return the content schema, or null if not found
     */
    public ContentSchema getSchema() {
        return schema;
    }

    /**
     * Get the list of fields from the schema.
     *
     * @return list of schema fields, or empty list if no schema
     */
    public List<SchemaField> getFields() {
        if (schema != null) {
            return schema.getFields();
        }
        return Collections.emptyList();
    }

    /**
     * Get the current value for a field from the resource.
     * This is called for each field during rendering to populate form values.
     *
     * @param fieldName the name of the field to get the value for
     * @return the field value as a String, or empty string if not set
     */
    public String getFieldValue(String fieldName) {
        if (properties != null && fieldName != null) {
            Object value = properties.get(fieldName);
            return value != null ? value.toString() : "";
        }
        return "";
    }

    /**
     * Get the properties ValueMap for direct access in HTL templates.
     * HTL can access this via ${fragmentEditor.properties['fieldName']}.
     *
     * @return the resource's ValueMap
     */
    public ValueMap getProperties() {
        return properties;
    }
}
