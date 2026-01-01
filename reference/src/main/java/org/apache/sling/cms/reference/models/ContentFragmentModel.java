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
package org.apache.sling.cms.reference.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model for rendering content fragments in the reference site.
 *
 * <p>This model loads a content fragment by path and provides access to its properties.
 * Content fragments are stored at /content/fragments/* and contain structured data
 * defined by a content schema.
 *
 * <p>Usage in HTL:
 * <pre>
 * &lt;sly data-sly-use.fragment="org.apache.sling.cms.reference.models.ContentFragmentModel"&gt;
 *   &lt;h1&gt;${fragment.title}&lt;/h1&gt;
 *   &lt;p&gt;${fragment.properties.description}&lt;/p&gt;
 * &lt;/sly&gt;
 * </pre>
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ContentFragmentModel {

    private static final Logger log = LoggerFactory.getLogger(ContentFragmentModel.class);

    @SlingObject
    private Resource resource;

    @SlingObject
    private ResourceResolver resourceResolver;

    @ValueMapValue
    private String fragmentPath;

    private Resource fragmentResource;
    private Map<String, Object> properties = new HashMap<>();
    private String schemaId;
    private String title;
    private boolean initialized;

    /**
     * Initialize the model by loading fragment properties.
     * Uses lazy initialization - called by getter methods.
     */
    private void init() {
        if (initialized) {
            return; // Already initialized
        }
        initialized = true;

        // If fragmentPath is provided, load that fragment
        if (fragmentPath != null && !fragmentPath.isEmpty()) {
            fragmentResource = resourceResolver.getResource(fragmentPath);
            log.info("Loading fragment from path: {}", fragmentPath);
        } else {
            // Otherwise, use the current resource as the fragment
            fragmentResource = resource;
            log.info("Loading fragment from current resource: {}", resource != null ? resource.getPath() : "null");
        }

        if (fragmentResource != null) {
            // Try to get jcr:content child node if it exists
            Resource contentResource = fragmentResource.getChild("jcr:content");
            Resource dataResource = contentResource != null ? contentResource : fragmentResource;

            ValueMap vm = dataResource.getValueMap();
            log.info("Fragment resource: {}, has {} properties", dataResource.getPath(), vm.size());

            // Get schema ID
            schemaId = vm.get("schemaId", String.class);

            // Get title (try both locations)
            title = vm.get("jcr:title", String.class);
            if (title == null && contentResource == null) {
                // Try on the fragment resource itself
                title = fragmentResource.getValueMap().get("jcr:title", String.class);
            }

            // Load all properties into a map for easy access
            vm.forEach((key, value) -> {
                properties.put(key, value);
                log.debug("Property: {} = {}", key, value);
            });

            log.info(
                    "Loaded content fragment: {} with {} properties, schema: {}, title: {}",
                    dataResource.getPath(),
                    properties.size(),
                    schemaId,
                    title);
        } else {
            log.warn(
                    "Content fragment not found at path: {}", fragmentPath != null ? fragmentPath : "current resource");
        }
    }

    /**
     * Gets the path to the content fragment resource.
     *
     * @return the fragment path
     */
    public String getFragmentPath() {
        init();
        return fragmentResource != null ? fragmentResource.getPath() : null;
    }

    /**
     * Gets the title of the content fragment.
     *
     * @return the fragment title
     */
    public String getTitle() {
        init();
        return title;
    }

    /**
     * Gets the schema ID of this content fragment.
     *
     * @return the schema ID
     */
    public String getSchemaId() {
        init();
        return schemaId;
    }

    /**
     * Gets all properties of the content fragment as a map.
     *
     * @return map of all fragment properties
     */
    public Map<String, Object> getProperties() {
        init();
        return properties;
    }

    /**
     * Gets display properties (filtered to exclude system properties).
     * Excludes properties starting with jcr:, sling:, and schemaId.
     *
     * @return map of display properties only
     */
    public Map<String, Object> getDisplayProperties() {
        init();
        Map<String, Object> displayProps = new HashMap<>();
        properties.forEach((key, value) -> {
            if (!key.startsWith("jcr:") && !key.startsWith("sling:") && !"schemaId".equals(key)) {
                displayProps.put(key, value);
            }
        });
        return displayProps;
    }

    /**
     * Gets a specific property value by name.
     *
     * @param propertyName the name of the property
     * @return the property value, or null if not found
     */
    public Object getProperty(String propertyName) {
        init();
        return properties.get(propertyName);
    }

    /**
     * Gets a string property value.
     *
     * @param propertyName the name of the property
     * @return the property value as string, or null if not found
     */
    public String getString(String propertyName) {
        init();
        Object value = properties.get(propertyName);
        return value != null ? value.toString() : null;
    }

    /**
     * Checks if the fragment was successfully loaded.
     *
     * @return true if fragment exists and was loaded
     */
    public boolean isLoaded() {
        init();
        return fragmentResource != null;
    }

    /**
     * Gets the underlying resource.
     *
     * @return the fragment resource
     */
    public Resource getResource() {
        init();
        return fragmentResource;
    }

    /**
     * Gets a list of property entries for iteration in HTL.
     * Each entry has 'key' and 'value' properties that can be accessed in HTL.
     * Filters out system properties (jcr:, sling:, schemaId).
     *
     * @return list of property entries
     */
    public List<PropertyEntry> getPropertyEntries() {
        init();
        List<PropertyEntry> entries = new ArrayList<>();
        properties.forEach((key, value) -> {
            if (!key.startsWith("jcr:") && !key.startsWith("sling:") && !"schemaId".equals(key)) {
                entries.add(new PropertyEntry(key, value));
            }
        });
        return entries;
    }

    /**
     * Simple bean class to hold property key-value pairs for HTL iteration.
     */
    public static class PropertyEntry {
        private final String key;
        private final Object value;

        public PropertyEntry(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }
    }
}
