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
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.EditableResource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PagePropertiesEditorModel {

    @Self
    private SlingHttpServletRequest request;

    public boolean isEditable() {
        return getEditable() != null && getEditPath() != null && !getEditPath().isBlank();
    }

    public String getEditPath() {
        EditableResource editable = getEditable();
        return editable != null ? editable.getEditPath() : null;
    }

    public String getReplaceSuffix() {
        EditableResource editable = getEditable();
        if (editable != null && editable.getResource() != null) {
            return editable.getResource().getPath();
        }
        return null;
    }

    public String getMessage() {
        if (request == null
                || request.getRequestPathInfo() == null
                || request.getRequestPathInfo().getSuffixResource() == null) {
            return "Unable to open properties editor: no resource selected.";
        }
        Resource contentResource = getContentResource();
        if (contentResource == null) {
            return "Unable to open properties editor for this resource.";
        }
        return "Unable to open properties editor for this resource.";
    }

    private Resource getContentResource() {
        if (request == null || request.getRequestPathInfo() == null) {
            return null;
        }
        Resource suffixResource = request.getRequestPathInfo().getSuffixResource();
        if (suffixResource == null) {
            return null;
        }
        ResourceResolver rr = request.getResourceResolver();
        if (rr == null) {
            return null;
        }
        return rr.getResource(suffixResource.getPath() + "/jcr:content");
    }

    private EditableResource getEditable() {
        Resource contentResource = getContentResource();
        if (contentResource == null) {
            return null;
        }
        return contentResource.adaptTo(EditableResource.class);
    }
}
