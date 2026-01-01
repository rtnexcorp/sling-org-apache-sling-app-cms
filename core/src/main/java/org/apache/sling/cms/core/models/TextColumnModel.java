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

import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the Text Column component
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class TextColumnModel {

    @SlingObject
    private SlingHttpServletRequest request;

    @Inject
    private Resource resource;

    /**
     * Get the property name to display
     * @return the property name from request attributes
     */
    public String getProperty() {
        Object property = request.getAttribute("property");
        return property != null ? property.toString() : "";
    }

    /**
     * Get the value of the configured property from the resource
     * @return the property value as a string, or empty string if not found
     */
    public String getValue() {
        String property = getProperty();
        if (property.isEmpty()) {
            return "";
        }
        Object value = resource.getValueMap().get(property);
        return value != null ? String.valueOf(value) : "";
    }

    /**
     * Check if this column should render as a link
     * @return true if the column is configured as a link
     */
    public boolean isLink() {
        Object link = request.getAttribute("link");
        return link != null && (link instanceof Boolean ? (Boolean) link : Boolean.parseBoolean(link.toString()));
    }

    /**
     * Get the link prefix
     * @return the prefix for the link URL
     */
    public String getPrefix() {
        Object prefix = request.getAttribute("prefix");
        return prefix != null ? prefix.toString() : "";
    }

    /**
     * Get the resource path
     * @return the path of the current resource
     */
    public String getPath() {
        return resource.getPath();
    }
}
