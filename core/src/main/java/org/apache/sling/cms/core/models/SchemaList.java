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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.schema.ContentSchema;
import org.apache.sling.cms.schema.SchemaManager;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for providing a list of available schemas/content types.
 * Used in dropdowns and selection lists.
 */
@Model(adaptables = Resource.class)
public class SchemaList {

    @SlingObject
    private Resource resource;

    @OSGiService
    private SchemaManager schemaManager;

    private List<ContentSchema> schemas;

    @PostConstruct
    protected void init() {
        if (schemaManager != null) {
            schemas = schemaManager.getAllSchemas(resource);
        } else {
            schemas = Collections.emptyList();
        }
    }

    /**
     * Get all available schemas.
     *
     * @return list of content schemas
     */
    public List<ContentSchema> getSchemas() {
        return schemas;
    }

    /**
     * Get schema options for select fields.
     * Returns a list of objects with 'value' and 'label' properties.
     *
     * @return list of schema options
     */
    public List<SchemaOption> getOptions() {
        List<SchemaOption> options = new ArrayList<>();
        if (schemas != null) {
            for (ContentSchema schema : schemas) {
                options.add(new SchemaOption(schema.getId(), schema.getTitle()));
            }
        }
        return options;
    }

    /**
     * Simple bean for schema select options.
     */
    public static class SchemaOption {
        private final String value;
        private final String label;

        public SchemaOption(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }
}
