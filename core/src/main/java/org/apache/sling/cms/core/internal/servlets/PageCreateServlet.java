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

import java.io.IOException;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for creating pages with proper sling:Page node type.
 * This servlet handles the page creation process by properly setting
 * the jcr:primaryType to sling:Page which the standard Sling POST
 * servlet import operation does not support.
 */
@Component(service = Servlet.class, property = {
        "sling.servlet.paths=/bin/cms/createpage",
        "sling.servlet.methods=POST"
})
public class PageCreateServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(PageCreateServlet.class);

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String parentPath = request.getParameter("parentPath");
        String pageName = request.getParameter(":name");
        String contentJson = request.getParameter(":content");

        if (parentPath == null || parentPath.isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("{\"error\": \"Missing parentPath parameter\"}");
            return;
        }

        if (pageName == null || pageName.isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("{\"error\": \"Missing :name parameter\"}");
            return;
        }

        if (contentJson == null || contentJson.isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("{\"error\": \"Missing :content parameter\"}");
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();
        Resource parentResource = resolver.getResource(parentPath);

        if (parentResource == null) {
            response.setStatus(404);
            response.getWriter().write("{\"error\": \"Parent path not found: " + parentPath + "\"}");
            return;
        }

        try {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                response.setStatus(500);
                response.getWriter().write("{\"error\": \"Could not get JCR session\"}");
                return;
            }

            Node parentNode = session.getNode(parentPath);
            String pagePath = parentPath + "/" + pageName;

            // Parse the JSON content
            JsonObject jsonContent;
            try (JsonReader jsonReader = Json.createReader(new StringReader(contentJson))) {
                jsonContent = jsonReader.readObject();
            }

            // Create the page node with sling:Page type
            String primaryType = jsonContent.getString("jcr:primaryType", "sling:Page");
            Node pageNode = parentNode.addNode(pageName, primaryType);

            // Set properties on page node (excluding jcr:content and jcr:primaryType)
            setNodeProperties(pageNode, jsonContent);

            // Create jcr:content if present
            if (jsonContent.containsKey("jcr:content")) {
                JsonObject jcrContent = jsonContent.getJsonObject("jcr:content");
                String contentType = jcrContent.getString("jcr:primaryType", "nt:unstructured");
                Node contentNode = pageNode.addNode("jcr:content", contentType);
                setNodeProperties(contentNode, jcrContent);
                
                // Set audit properties
                Calendar now = Calendar.getInstance();
                String userId = session.getUserID();
                contentNode.setProperty("jcr:lastModified", now);
                contentNode.setProperty("jcr:lastModifiedBy", userId);
            }

            session.save();

            log.info("Created page: {}", pagePath);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"path\": \"" + pagePath + "\", \"changes\": [{\"type\": \"created\", \"argument\": [\"" + pagePath + "\"]}]}");

        } catch (RepositoryException e) {
            log.error("Error creating page", e);
            response.setStatus(500);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void setNodeProperties(Node node, JsonObject json) throws RepositoryException {
        for (Map.Entry<String, JsonValue> entry : json.entrySet()) {
            String key = entry.getKey();
            JsonValue value = entry.getValue();

            // Skip jcr:primaryType (already set), jcr:content (handled separately), and system properties
            if ("jcr:primaryType".equals(key) || "jcr:content".equals(key) 
                    || "jcr:created".equals(key) || "jcr:createdBy".equals(key)) {
                continue;
            }

            switch (value.getValueType()) {
                case STRING:
                    node.setProperty(key, json.getString(key));
                    break;
                case TRUE:
                    node.setProperty(key, true);
                    break;
                case FALSE:
                    node.setProperty(key, false);
                    break;
                case NUMBER:
                    node.setProperty(key, json.getJsonNumber(key).longValue());
                    break;
                case OBJECT:
                    // Create child node for nested objects
                    JsonObject childJson = json.getJsonObject(key);
                    String childType = childJson.getString("jcr:primaryType", "nt:unstructured");
                    Node childNode = node.addNode(key, childType);
                    setNodeProperties(childNode, childJson);
                    break;
                default:
                    // Skip arrays and other types for now
                    break;
            }
        }
    }
}
