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
import org.apache.sling.cms.PublishableResource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the Publication Status component.
 * Displays publication metadata for a resource.
 */
@Model(adaptables = {SlingHttpServletRequest.class, Resource.class})
public class PublicationStatus {

    @SlingObject
    private SlingHttpServletRequest request;

    private PublishableResource publishableResource;

    @PostConstruct
    protected void init() {
        Resource suffixResource = request.getRequestPathInfo().getSuffixResource();
        if (suffixResource != null) {
            publishableResource = suffixResource.adaptTo(PublishableResource.class);
        }
    }

    /**
     * Gets the publishable resource.
     *
     * @return the publishable resource, or null if not available
     */
    public PublishableResource getPublishableResource() {
        return publishableResource;
    }

    /**
     * Checks if the resource is published.
     *
     * @return true if published, false otherwise
     */
    public boolean isPublished() {
        return publishableResource != null && publishableResource.isPublished();
    }

    /**
     * Checks if publication metadata exists.
     *
     * @return true if the resource can be adapted to PublishableResource
     */
    public boolean hasPublicationMetadata() {
        return publishableResource != null;
    }
}
