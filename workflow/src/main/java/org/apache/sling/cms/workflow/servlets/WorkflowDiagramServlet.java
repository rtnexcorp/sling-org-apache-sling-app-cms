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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.cms.workflow.RepositoryService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to display workflow diagram (BPMN XML).
 */
@Component(
        service = Servlet.class,
        property = {"sling.servlet.paths=/bin/workflow/diagram", "sling.servlet.methods=GET"})
public class WorkflowDiagramServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(WorkflowDiagramServlet.class);

    @Reference
    private transient RepositoryService repositoryService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String processKey = request.getParameter("processKey");

        if (processKey == null || processKey.isEmpty()) {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Missing processKey parameter");
            return;
        }

        try {
            String bpmnXml = repositoryService.getProcessModel(processKey);

            if (bpmnXml == null || bpmnXml.isEmpty()) {
                response.sendError(
                        SlingHttpServletResponse.SC_NOT_FOUND, "Workflow definition not found: " + processKey);
                return;
            }

            // Return the BPMN XML as content
            response.setContentType("application/xml");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(bpmnXml);

        } catch (Exception e) {
            log.error("Error retrieving workflow diagram for: {}", processKey, e);
            response.sendError(
                    SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving workflow: " + e.getMessage());
        }
    }
}
