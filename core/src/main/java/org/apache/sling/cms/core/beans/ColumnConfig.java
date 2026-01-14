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
package org.apache.sling.cms.core.beans;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

/**
 * Represents a column configuration within a table row.
 */
public class ColumnConfig {
    private final Resource resource;
    private final ValueMap properties;
    private final ValueMap enhancedProperties;

    public ColumnConfig(Resource resource) {
        this.resource = resource;
        this.properties = resource.getValueMap();
        // Create enhanced properties map with resource path for HTL components
        this.enhancedProperties =
                new org.apache.sling.api.wrappers.ValueMapDecorator(new java.util.HashMap<>(properties));
        ((java.util.Map<String, Object>) enhancedProperties).put("colConfigPath", resource.getPath());
    }

    public String getName() {
        return resource.getName();
    }

    public String getResourceType() {
        return properties.get("sling:resourceType", String.class);
    }

    public boolean hasResourceType() {
        return StringUtils.isNotBlank(getResourceType());
    }

    public Resource getResource() {
        return resource;
    }

    public boolean isLink() {
        return properties.get("link", false);
    }

    public String getPrefix() {
        return properties.get("prefix", "");
    }

    public ValueMap getProperties() {
        return enhancedProperties;
    }
}
