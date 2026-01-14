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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

/**
 * Wrapper for action configuration that provides data for HTL rendering.
 */
public class ActionItem {
    private final Resource config;
    private final SlingHttpServletRequest request;

    public ActionItem(Resource config, SlingHttpServletRequest request) {
        this.config = config;
        this.request = request;
    }

    public String getResourceType() {
        return config.getResourceType();
    }

    public Resource getConfig() {
        return config;
    }

    /**
     * Sets the actionConfig request attribute and returns the suffix path.
     * This is used by HTL to set the request attribute before including the action component.
     *
     * @return suffix path for the resource include
     */
    public String getSuffixPathWithConfig() {
        request.setAttribute("actionConfig", config);
        if (request.getRequestPathInfo() == null) {
            return "";
        }
        String suffix = request.getRequestPathInfo().getSuffix();
        return suffix != null ? suffix : "";
    }
}
