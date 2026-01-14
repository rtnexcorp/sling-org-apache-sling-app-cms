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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.schema.ContentSchema;
import org.apache.sling.cms.schema.SchemaField;
import org.apache.sling.cms.schema.SchemaManager;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for rendering fragment editor forms dynamically based on schema.
 * Generates appropriate form fields for each field defined in the fragment's schema.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class FragmentEditor {

    @SlingObject
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @OSGiService
    private SchemaManager schemaManager;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

    private ContentSchema schema;
    private ValueMap properties;
    private Resource fragmentResource;
    private Map<String, Object> formattedProperties;

    @PostConstruct
    protected void init() {
        // Get the fragment resource from the request suffix
        if (request != null && request.getRequestPathInfo() != null) {
            String suffixPath = request.getRequestPathInfo().getSuffix();
            if (suffixPath != null && resource != null) {
                fragmentResource = resource.getResourceResolver().getResource(suffixPath);
            }
        }

        // Fallback to current resource if no suffix
        if (fragmentResource == null) {
            fragmentResource = resource;
        }

        properties = fragmentResource.getValueMap();
        String schemaId = properties.get("schemaId", String.class);

        if (schemaId != null && schemaManager != null) {
            schema = schemaManager.getSchema(fragmentResource, schemaId);
        }

        // Build formatted properties map
        formattedProperties = new HashMap<>();
        for (String key : properties.keySet()) {
            Object value = properties.get(key);
            formattedProperties.put(key, formatValue(value));
        }
    }

    /**
     * Format a value for display in form fields.
     * Converts Calendar objects to ISO date strings.
     */
    private Object formatValue(Object value) {
        if (value instanceof Calendar) {
            Calendar cal = (Calendar) value;
            synchronized (DATETIME_FORMAT) {
                // Check if time component is set (non-midnight)
                if (cal.get(Calendar.HOUR_OF_DAY) == 0
                        && cal.get(Calendar.MINUTE) == 0
                        && cal.get(Calendar.SECOND) == 0) {
                    synchronized (DATE_FORMAT) {
                        return DATE_FORMAT.format(cal.getTime());
                    }
                }
                return DATETIME_FORMAT.format(cal.getTime());
            }
        }
        return value;
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
     * This reads from child field nodes created during fragment initialization.
     *
     * @param fieldName the name of the field to get the value for
     * @return the field value as a String, or empty string if not set
     */
    public String getFieldValue(String fieldName) {
        if (fieldName == null || fragmentResource == null) {
            return "";
        }

        // First, check if there's a child node for this field (new structure)
        Resource fieldResource = fragmentResource.getChild(fieldName);
        if (fieldResource != null) {
            ValueMap fieldProps = fieldResource.getValueMap();
            Object value = fieldProps.get("value");
            if (value != null) {
                return value.toString();
            }
        }

        // Fallback to direct properties (legacy structure)
        if (properties != null) {
            Object value = properties.get(fieldName);
            if (value != null) {
                return value.toString();
            }
        }

        return "";
    }

    /**
     * Get the properties map for direct access in HTL templates.
     * HTL can access this via ${fragmentEditor.properties['fieldName']}.
     * Date/Calendar values are automatically formatted as ISO strings.
     *
     * @return map of property names to formatted values
     */
    public Map<String, Object> getProperties() {
        return formattedProperties;
    }
}
