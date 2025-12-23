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
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for fragment modal pages.
 * Resolves the fragment resource from the request suffix.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class FragmentModalPage {

    @SlingObject
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    private Resource fragmentResource;
    private String fragmentPath;

    @PostConstruct
    protected void init() {
        // Get the fragment resource from the request suffix
        if (request != null && request.getRequestPathInfo() != null) {
            String suffixPath = request.getRequestPathInfo().getSuffix();
            if (suffixPath != null && resource != null) {
                fragmentResource = resource.getResourceResolver().getResource(suffixPath);
                fragmentPath = suffixPath;
            }
        }
    }

    /**
     * Get the fragment resource being edited.
     *
     * @return the fragment resource, or null if not found
     */
    public Resource getFragmentResource() {
        return fragmentResource;
    }

    /**
     * Get the path to the fragment being edited.
     *
     * @return the fragment path from the suffix
     */
    public String getFragmentPath() {
        return fragmentPath;
    }
}
