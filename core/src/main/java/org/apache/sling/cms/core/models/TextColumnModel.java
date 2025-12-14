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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the text column component.
 * Displays a property value from the resource with optional link.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class TextColumnModel {

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    private Resource colConfig;
    private ValueMap colConfigValueMap;

    private Resource getColConfig() {
        if (colConfig == null) {
            colConfig = (Resource) request.getAttribute("colConfig");
        }
        return colConfig;
    }

    private ValueMap getColConfigValueMap() {
        if (colConfigValueMap == null && getColConfig() != null) {
            colConfigValueMap = getColConfig().getValueMap();
        }
        return colConfigValueMap;
    }

    /**
     * @return the property name from colConfig
     */
    public String getProperty() {
        ValueMap vm = getColConfigValueMap();
        return vm != null ? vm.get("property", String.class) : null;
    }

    /**
     * @return the property value from the resource
     */
    public String getPropertyValue() {
        String property = getProperty();
        if (property != null) {
            return resource.getValueMap().get(property, String.class);
        }
        return null;
    }

    /**
     * @return the resource path
     */
    public String getResourcePath() {
        return resource.getPath();
    }

    /**
     * @return true if link should be displayed
     */
    public boolean isLink() {
        ValueMap vm = getColConfigValueMap();
        return vm != null && vm.get("link", false);
    }

    /**
     * @return the link prefix
     */
    public String getPrefix() {
        ValueMap vm = getColConfigValueMap();
        return vm != null ? vm.get("prefix", "") : "";
    }

    /**
     * @return the full link href
     */
    public String getLinkHref() {
        return getPrefix() + resource.getPath();
    }
}
