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
package org.apache.sling.cms.core.internal.models;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.cms.CMSConstants;
import org.apache.sling.cms.PageContext;
import org.apache.sling.cms.publication.INSTANCE_TYPE;
import org.apache.sling.cms.publication.PublicationManagerFactory;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of PageContext that provides instance type and page mode information.
 * This model can be adapted from SlingHttpServletRequest.
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = PageContext.class)
public class PageContextImpl implements PageContext {

    private static final Logger log = LoggerFactory.getLogger(PageContextImpl.class);

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private PublicationManagerFactory publicationManagerFactory;

    private boolean editMode;
    private INSTANCE_TYPE instanceType;

    @PostConstruct
    protected void init() {
        // Check edit mode from request attribute
        Object editEnabled = request.getAttribute(CMSConstants.ATTR_EDIT_ENABLED);
        this.editMode = "true".equals(editEnabled) || Boolean.TRUE.equals(editEnabled);

        // Get instance type from PublicationManagerFactory
        if (publicationManagerFactory != null) {
            this.instanceType = publicationManagerFactory.getInstanceType();
        } else {
            log.warn("PublicationManagerFactory not available, defaulting to STANDALONE");
            this.instanceType = INSTANCE_TYPE.STANDALONE;
        }

        log.debug("PageContext initialized - editMode: {}, instanceType: {}", editMode, instanceType);
    }

    @Override
    public PageMode getPageMode() {
        return editMode ? PageMode.EDIT : PageMode.PREVIEW;
    }

    @Override
    public boolean isEditMode() {
        return editMode;
    }

    @Override
    public boolean isPreviewMode() {
        return !editMode;
    }

    @Override
    public INSTANCE_TYPE getInstanceType() {
        return instanceType;
    }

    @Override
    public boolean isAuthor() {
        return instanceType == INSTANCE_TYPE.AUTHOR;
    }

    @Override
    public boolean isRenderer() {
        return instanceType == INSTANCE_TYPE.RENDERER;
    }

    @Override
    public boolean isStandalone() {
        return instanceType == INSTANCE_TYPE.STANDALONE;
    }

    @Override
    public boolean isAuthoringEnabled() {
        // Authoring is enabled when in edit mode on Author or Standalone
        return editMode && (isAuthor() || isStandalone());
    }

    @Override
    public boolean isPublishMode() {
        // Publish mode when on Renderer, or when in preview mode
        return isRenderer() || isPreviewMode();
    }
}
