/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.cms.distribution.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Servlet that receives content from the Author instance and imports it.
 * This runs on the Publisher instance.
 */
@Component(service = Servlet.class)
@SlingServletPaths("/bin/cms/distribution/import")
public class ContentImportServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(ContentImportServlet.class);
    
    @Reference
    private ResourceResolverFactory resolverFactory;

    private final Gson gson = new Gson();

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        JsonObject result = new JsonObject();

        try {
            // Read request body
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            String requestBody = sb.toString();
            LOG.debug("Received distribution request: {}", requestBody);

            JsonObject requestJson = gson.fromJson(requestBody, JsonObject.class);
            
            String path = requestJson.get("path").getAsString();
            String action = requestJson.get("action").getAsString();
            boolean deep = requestJson.has("deep") && requestJson.get("deep").getAsBoolean();

            LOG.info("Processing distribution: action={}, path={}, deep={}", action, path, deep);

            // Get service resolver for import
            Map<String, Object> authInfo = new HashMap<>();
            authInfo.put(ResourceResolverFactory.SUBSERVICE, "distribution");
            
            try (ResourceResolver serviceResolver = resolverFactory.getServiceResourceResolver(authInfo)) {
                if ("ADD".equals(action)) {
                    if (requestJson.has("content")) {
                        String contentJson = requestJson.get("content").getAsString();
                        JsonObject content = gson.fromJson(contentJson, JsonObject.class);
                        importContent(serviceResolver, path, content, deep);
                    }
                    result.addProperty("status", "success");
                    result.addProperty("message", "Content imported: " + path);
                } else if ("DELETE".equals(action)) {
                    deleteContent(serviceResolver, path);
                    result.addProperty("status", "success");
                    result.addProperty("message", "Content deleted: " + path);
                } else {
                    result.addProperty("status", "error");
                    result.addProperty("message", "Unknown action: " + action);
                    response.setStatus(400);
                }

                serviceResolver.commit();
                
            } catch (Exception e) {
                LOG.error("Error processing distribution request", e);
                result.addProperty("status", "error");
                result.addProperty("message", e.getMessage());
                response.setStatus(500);
            }

        } catch (Exception e) {
            LOG.error("Error parsing distribution request", e);
            result.addProperty("status", "error");
            result.addProperty("message", "Error parsing request: " + e.getMessage());
            response.setStatus(400);
        }

        response.getWriter().write(gson.toJson(result));
    }

    private void importContent(ResourceResolver resolver, String path, JsonObject content, boolean deep) 
            throws PersistenceException, RepositoryException {
        
        LOG.debug("Importing content to: {}", path);

        // Ensure parent exists
        String parentPath = path.substring(0, path.lastIndexOf('/'));
        if (parentPath.isEmpty()) {
            parentPath = "/";
        }
        
        Resource parent = resolver.getResource(parentPath);
        if (parent == null) {
            LOG.debug("Creating parent path: {}", parentPath);
            createPath(resolver, parentPath);
            parent = resolver.getResource(parentPath);
        }

        // Get or create the resource
        Resource resource = resolver.getResource(path);
        String resourceName = path.substring(path.lastIndexOf('/') + 1);

        if (resource == null) {
            // Create new resource
            Map<String, Object> properties = extractProperties(content);
            String primaryType = (String) properties.getOrDefault("jcr:primaryType", "nt:unstructured");
            properties.put("jcr:primaryType", primaryType);
            
            resource = resolver.create(parent, resourceName, properties);
            LOG.debug("Created resource: {}", path);
        } else {
            // Update existing resource
            ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);
            if (mvm != null) {
                Map<String, Object> properties = extractProperties(content);
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    if (!entry.getKey().startsWith("jcr:") || entry.getKey().equals("jcr:title")) {
                        mvm.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            LOG.debug("Updated resource: {}", path);
        }

        // Process children if deep
        if (deep && content.has(":children")) {
            JsonObject children = content.getAsJsonObject(":children");
            for (String childName : children.keySet()) {
                JsonObject childContent = children.getAsJsonObject(childName);
                String childPath = path + "/" + childName;
                importContent(resolver, childPath, childContent, true);
            }
        }
    }

    private Map<String, Object> extractProperties(JsonObject json) {
        Map<String, Object> props = new HashMap<>();
        
        for (String key : json.keySet()) {
            if (key.equals("jcr:path") || key.equals(":children")) {
                continue;
            }
            
            JsonElement element = json.get(key);
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                String[] values = new String[array.size()];
                for (int i = 0; i < array.size(); i++) {
                    values[i] = array.get(i).getAsString();
                }
                props.put(key, values);
            } else if (element.isJsonPrimitive()) {
                props.put(key, element.getAsString());
            }
        }
        
        return props;
    }

    private void createPath(ResourceResolver resolver, String path) throws PersistenceException {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return;
        }
        
        Resource resource = resolver.getResource(path);
        if (resource != null) {
            return;
        }
        
        String parentPath = path.substring(0, path.lastIndexOf('/'));
        if (parentPath.isEmpty()) {
            parentPath = "/";
        }
        
        createPath(resolver, parentPath);
        
        Resource parent = resolver.getResource(parentPath);
        if (parent != null) {
            String name = path.substring(path.lastIndexOf('/') + 1);
            Map<String, Object> props = new HashMap<>();
            props.put("jcr:primaryType", "sling:Folder");
            resolver.create(parent, name, props);
        }
    }

    private void deleteContent(ResourceResolver resolver, String path) throws PersistenceException {
        Resource resource = resolver.getResource(path);
        if (resource != null) {
            LOG.info("Deleting content: {}", path);
            resolver.delete(resource);
        } else {
            LOG.debug("Resource not found for deletion: {}", path);
        }
    }
}
