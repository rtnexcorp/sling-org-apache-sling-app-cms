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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

/**
 * Sling Model for the pagewrapper component.
 * Handles CMS edit mode wrapping of pages.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class PageWrapperModel {

    @Self
    private SlingHttpServletRequest request;

    private Resource suffixResource;
    private String forceResourceType;

    @PostConstruct
    protected void init() {
        // Set cmsEditEnabled in request scope
        request.setAttribute("cmsEditEnabled", Boolean.TRUE);

        String suffix = request.getRequestPathInfo().getSuffix();
        if (suffix != null) {
            suffixResource = request.getResourceResolver().getResource(suffix);
        }
        forceResourceType = request.getParameter("forceResourceType");
    }

    /**
     * @return the suffix resource
     */
    public Resource getSuffixResource() {
        return suffixResource;
    }

    /**
     * @return true if forceResourceType parameter is set
     */
    public boolean hasForceResourceType() {
        return forceResourceType != null && !forceResourceType.isEmpty();
    }

    /**
     * @return the forceResourceType parameter value
     */
    public String getForceResourceType() {
        return forceResourceType;
    }

    /**
     * Disable edit mode after rendering.
     */
    public void disableEditMode() {
        request.setAttribute("cmsEditEnabled", Boolean.FALSE);
    }
}
