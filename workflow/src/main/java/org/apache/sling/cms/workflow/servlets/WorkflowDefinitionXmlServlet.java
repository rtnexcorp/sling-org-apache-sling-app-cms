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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to return workflow definition BPMN XML content.
 *
 * <p>Responds to requests with .bpmn selector on workflow definition nodes.
 *
 * <p>Usage:
 * <pre>
 * GET /etc/workflow/definitions/publishingWorkflow.bpmn.xml
 * </pre>
 *
 * <p>Returns the BPMN XML content stored in the "bpmn" property.
 */
@Component(
        service = Servlet.class,
        property = {
            "sling.servlet.resourceTypes=nt:unstructured",
            "sling.servlet.selectors=bpmn",
            "sling.servlet.extensions=xml",
            "sling.servlet.methods=GET"
        })
public class WorkflowDefinitionXmlServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(WorkflowDefinitionXmlServlet.class);

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        Resource resource = request.getResource();
        ValueMap properties = resource.getValueMap();

        // Check if this is a workflow definition (has bpmn property)
        String bpmnContent = properties.get("bpmn", String.class);

        if (bpmnContent == null || bpmnContent.isEmpty()) {
            response.sendError(
                    SlingHttpServletResponse.SC_NOT_FOUND, "No BPMN content found for resource: " + resource.getPath());
            return;
        }

        log.debug("Serving BPMN XML for workflow definition: {}", resource.getPath());

        // Set content type and encoding
        response.setContentType("application/xml");
        response.setCharacterEncoding("UTF-8");

        // Write BPMN content
        response.getWriter().write(bpmnContent);
    }
}
