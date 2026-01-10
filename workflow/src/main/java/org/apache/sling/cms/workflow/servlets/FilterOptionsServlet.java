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
package org.apache.sling.cms.workflow.servlets;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.cms.workflow.RuntimeService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet that provides dynamic filter options for ContentFilter component.
 * Supports workflow process types and site templates.
 */
@Component(
        service = {Servlet.class},
        property = {
            "sling.servlet.paths=/bin/cms/filter-options",
            "sling.servlet.methods=" + HttpConstants.METHOD_GET,
            "sling.servlet.extensions=json"
        })
public class FilterOptionsServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(FilterOptionsServlet.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private RuntimeService runtimeService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String type = request.getParameter("type");

        if (StringUtils.isEmpty(type)) {
            response.sendError(400, "Missing required parameter: type");
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            PrintWriter writer = response.getWriter();

            switch (type) {
                case "process-type":
                    writeProcessTypeOptions(request, writer);
                    break;
                case "template":
                    writeTemplateOptions(request, writer);
                    break;
                default:
                    response.sendError(400, "Unknown filter type: " + type);
                    return;
            }

        } catch (Exception e) {
            log.error("Error loading filter options for type: {}", type, e);
            response.sendError(500, "Error loading filter options");
        }
    }

    /**
     * Write workflow process type options to response.
     *
     * @param request the current request
     * @param writer the response writer
     */
    private void writeProcessTypeOptions(SlingHttpServletRequest request, PrintWriter writer) {
        writer.write("[");

        if (runtimeService != null) {
            try {
                runtimeService.setResolverContext(request.getResourceResolver());

                List<String> processTypes = runtimeService.getActiveProcessInstances().stream()
                        .map(instance -> instance.getProcessDefinitionKey())
                        .distinct()
                        .sorted()
                        .toList();

                boolean first = true;
                for (String processType : processTypes) {
                    if (!first) {
                        writer.write(",");
                    }
                    writer.write("{\"value\":\"");
                    writer.write(escapeJson(processType));
                    writer.write("\",\"label\":\"");
                    writer.write(escapeJson(processType));
                    writer.write("\"}");
                    first = false;
                }

            } catch (Exception e) {
                log.error("Error loading process types", e);
            } finally {
                runtimeService.clearResolverContext();
            }
        } else {
            log.warn("RuntimeService not available for process type options");
        }

        writer.write("]");
    }

    /**
     * Write site template options to response.
     *
     * @param request the current request
     * @param writer the response writer
     */
    private void writeTemplateOptions(SlingHttpServletRequest request, PrintWriter writer) {
        writer.write("[");

        try {
            // Get templates from /libs/sling-cms/components/pages
            Resource templatesRoot = request.getResourceResolver().getResource("/libs/sling-cms/components/pages");

            if (templatesRoot != null) {
                boolean first = true;

                for (Resource template : templatesRoot.getChildren()) {
                    String resourceType = template.getResourceType();
                    String primaryType = template.getValueMap().get("jcr:primaryType", String.class);

                    if ("sling:Component".equals(resourceType) || primaryType != null) {
                        if (!first) {
                            writer.write(",");
                        }

                        String name = template.getName();
                        String title = template.getValueMap().get("jcr:title", name);

                        writer.write("{\"value\":\"");
                        writer.write(escapeJson(name));
                        writer.write("\",\"label\":\"");
                        writer.write(escapeJson(title));
                        writer.write("\"}");

                        first = false;
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error loading templates", e);
        }

        writer.write("]");
    }

    /**
     * Escape special characters for JSON string values.
     *
     * @param value the string to escape
     * @return escaped string
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
