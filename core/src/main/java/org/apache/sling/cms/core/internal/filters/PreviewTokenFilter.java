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
package org.apache.sling.cms.core.internal.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.CMSConstants;
import org.apache.sling.cms.CMSUtils;
import org.apache.sling.cms.preview.PreviewTokenManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter that detects and validates preview tokens in requests.
 * When a valid preview token is detected, sets a request attribute
 * to enable preview mode, allowing access to unpublished content.
 */
@Component(
        service = {Filter.class},
        property = {"sling.filter.scope=request", "service.ranking:Integer=10000"},
        immediate = true)
public class PreviewTokenFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(PreviewTokenFilter.class);

    @Reference
    private PreviewTokenManager previewTokenManager;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Nothing required
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof SlingHttpServletRequest) {
            SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
            String previewToken = slingRequest.getParameter(CMSConstants.PARAM_PREVIEW_TOKEN);

            if (StringUtils.isNotBlank(previewToken)) {
                log.debug("Preview token detected in request to {}", slingRequest.getRequestURI());

                Resource publishableResource = CMSUtils.findPublishableParent(slingRequest.getResource());

                if (publishableResource != null) {
                    boolean isValid = previewTokenManager.validateToken(publishableResource, previewToken);

                    if (isValid) {
                        log.debug("Valid preview token for resource: {}", publishableResource.getPath());
                        request.setAttribute(CMSConstants.ATTR_PREVIEW_ENABLED, Boolean.TRUE);
                    } else {
                        log.warn("Invalid or expired preview token for resource: {}", publishableResource.getPath());
                    }
                } else {
                    log.debug(
                            "No publishable parent found for resource: {}",
                            slingRequest.getResource().getPath());
                }
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Nothing required
    }
}
