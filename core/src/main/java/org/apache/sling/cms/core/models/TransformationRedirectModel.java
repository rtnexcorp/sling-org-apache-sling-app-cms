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
import javax.jcr.RepositoryException;

import java.io.IOException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.cms.AuthorizableWrapper;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model for the transformationredirect component.
 * Redirects to the user's transformations page.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class TransformationRedirectModel {

    private static final Logger log = LoggerFactory.getLogger(TransformationRedirectModel.class);

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private SlingHttpServletResponse response;

    @PostConstruct
    protected void init() {
        try {
            AuthorizableWrapper auth = request.getResourceResolver().adaptTo(AuthorizableWrapper.class);
            if (auth != null && auth.getAuthorizable() != null) {
                String redirectUrl = "/cms/transformations/user.html"
                        + auth.getAuthorizable().getPath() + "/transformations";
                response.sendRedirect(redirectUrl);
            }
        } catch (IOException | RepositoryException e) {
            log.error("Failed to redirect to transformations page", e);
        }
    }
}
