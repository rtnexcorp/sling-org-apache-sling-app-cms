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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.ComponentPolicyManager;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Sling Model for the container component.
 */
@Model(adaptables = {Resource.class, SlingHttpServletRequest.class})
public class Container {

    @ValueMapValue
    @Optional
    private String classes;

    @ScriptVariable
    private Resource resource;

    @SlingObject
    @Optional
    private SlingHttpServletRequest request;

    @Inject
    @Optional
    @Default(values = "")
    private String cmsEditEnabled;

    /**
     * Get the CSS classes to apply to the container.
     *
     * @return the CSS classes or empty string if none
     */
    public String getClasses() {
        return StringUtils.defaultString(classes);
    }

    /**
     * Check if container has CSS classes defined.
     *
     * @return true if classes are defined, false otherwise
     */
    public boolean getHasClasses() {
        return StringUtils.isNotBlank(classes);
    }

    /**
     * Get the child resources of the container.
     *
     * @return list of child resources
     */
    public List<Resource> getChildren() {
        List<Resource> children = new ArrayList<>();
        Iterator<Resource> iter = resource.listChildren();
        while (iter.hasNext()) {
            children.add(iter.next());
        }
        return children;
    }

    /**
     * Check if edit mode is enabled.
     *
     * @return true if edit mode is enabled
     */
    public boolean getEditEnabled() {
        return "true".equals(cmsEditEnabled);
    }

    /**
     * Check if the resource exists.
     *
     * @return true if resource exists
     */
    public boolean getResourceExists() {
        return resource.getResourceResolver().getResource(resource.getPath()) != null;
    }

    /**
     * Get available component types for this container.
     *
     * @return comma-separated list of available types or empty string
     */
    public String getAvailableTypes() {
        // First check request scope attributes (matches JSP behavior)
        if (request != null) {
            Object availableTypesAttr = request.getAttribute("availableTypes");
            if (availableTypesAttr != null) {
                return availableTypesAttr.toString();
            }
        }

        // Then check component policy
        ComponentPolicyManager policyMgr = resource.adaptTo(ComponentPolicyManager.class);
        if (policyMgr != null && policyMgr.getComponentPolicy() != null) {
            String[] types = policyMgr.getComponentPolicy().getAvailableComponentTypes();
            if (types != null && types.length > 0) {
                return String.join(",", types);
            }
        }

        return "";
    }
}
