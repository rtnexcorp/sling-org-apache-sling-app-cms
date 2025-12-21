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
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model for the Publish Column component
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PublishColumnModel {

    private static final Logger log = LoggerFactory.getLogger(PublishColumnModel.class);

    @SlingObject
    private Resource resource;

    private PublishableResource publishableResource;
    private boolean published;

    @PostConstruct
    protected void init() {
        try {
            publishableResource = resource.adaptTo(PublishableResource.class);
            if (publishableResource != null) {
                published = publishableResource.isPublished();
            }
        } catch (Exception e) {
            log.debug("Failed to adapt resource to PublishableResource", e);
        }
    }

    public boolean isPublished() {
        return published;
    }

    public int getDataValue() {
        return published ? 0 : 1;
    }

    public String getResourcePath() {
        return resource != null ? resource.getPath() : "";
    }

    public String getActionUrl() {
        if (resource == null) {
            return "";
        }
        return published
                ? "/cms/shared/unpublish.html" + resource.getPath()
                : "/cms/shared/publish.html" + resource.getPath();
    }

    public String getActionTitle() {
        return published ? "Unpublish" : "Publish";
    }

    public String getButtonClass() {
        return published ? "button is-success is-outlined Fetch-Modal" : "button is-warning is-outlined Fetch-Modal";
    }

    public String getIconClass() {
        return published ? "jam jam-check" : "jam jam-close";
    }

    public String getMessageKey() {
        return published ? "Content Published" : "Content Not Published";
    }
}
