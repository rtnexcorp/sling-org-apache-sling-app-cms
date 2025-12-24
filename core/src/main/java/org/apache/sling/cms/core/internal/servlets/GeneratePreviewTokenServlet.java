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
package org.apache.sling.cms.core.internal.servlets;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.cms.preview.PreviewTokenManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for generating preview tokens via POST requests.
 * Returns a JSON response with the preview URL.
 */
@Component(
        service = Servlet.class,
        property = {
            "sling.servlet.resourceTypes=sling:Page",
            "sling.servlet.resourceTypes=sling:File",
            "sling.servlet.methods=POST",
            "sling.servlet.selectors=generatePreview",
            "sling.servlet.extensions=json"
        })
public class GeneratePreviewTokenServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(GeneratePreviewTokenServlet.class);

    private static final int DEFAULT_TIMEOUT_SECONDS = 86400; // 24 hours

    @Reference
    private PreviewTokenManager previewTokenManager;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        String timeoutParam = request.getParameter("timeout");
        if (timeoutParam != null) {
            try {
                timeoutSeconds = Integer.parseInt(timeoutParam);
            } catch (NumberFormatException e) {
                log.warn("Invalid timeout parameter: {}, using default", timeoutParam);
            }
        }

        try {
            String previewUrl = previewTokenManager.generatePreviewUrl(request.getResource(), timeoutSeconds);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(String.format("{\"success\":true,\"previewUrl\":\"%s\"}", previewUrl));

            log.info(
                    "Generated preview URL for {} with timeout {}s",
                    request.getResource().getPath(),
                    timeoutSeconds);

        } catch (PersistenceException e) {
            log.error(
                    "Failed to generate preview token for "
                            + request.getResource().getPath(),
                    e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter()
                    .write(String.format(
                            "{\"success\":false,\"error\":\"%s\"}",
                            e.getMessage().replace("\"", "\\\"")));
        }
    }
}
