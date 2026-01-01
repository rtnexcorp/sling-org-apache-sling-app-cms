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
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the Page Properties Include component.
 * Provides access to the fields resource from the suffix resource (page template).
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class PagePropertiesIncludeModel {

    @SlingObject
    private SlingHttpServletRequest request;

    /**
     * Gets the 'fields' child resource from the request suffix resource.
     * The suffix points to a page template configuration.
     *
     * @return the fields resource or null if not found
     */
    public Resource getFieldsResource() {
        if (request == null || request.getRequestPathInfo() == null) {
            return null;
        }

        Resource suffixResource = request.getRequestPathInfo().getSuffixResource();
        if (suffixResource == null) {
            return null;
        }

        return suffixResource.getChild("fields");
    }
}
