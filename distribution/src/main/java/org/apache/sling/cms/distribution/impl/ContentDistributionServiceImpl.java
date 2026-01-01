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
package org.apache.sling.cms.distribution.impl;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.cms.distribution.ContentDistributionService;
import org.apache.sling.cms.distribution.DistributionResult;
import org.apache.sling.cms.distribution.DistributionResult.Status;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ContentDistributionService that uses HTTP to push content
 * to publisher instances.
 */
@Component(service = ContentDistributionService.class, immediate = true)
@Designate(ocd = ContentDistributionConfig.class)
public class ContentDistributionServiceImpl implements ContentDistributionService {

    private static final Logger LOG = LoggerFactory.getLogger(ContentDistributionServiceImpl.class);
    private static final String IMPORT_ENDPOINT = "/bin/cms/distribution/import";

    @Reference
    private ResourceResolverFactory resolverFactory;

    private ContentDistributionConfig config;
    private CloseableHttpClient httpClient;
    private ExecutorService executorService;
    private Gson gson = new Gson();

    @Activate
    @Modified
    protected void activate(ContentDistributionConfig config) {
        this.config = config;

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(config.connectionTimeout())
                .setSocketTimeout(config.socketTimeout())
                .build();

        this.httpClient =
                HttpClients.custom().setDefaultRequestConfig(requestConfig).build();

        if (config.asyncDistribution()) {
            this.executorService = Executors.newFixedThreadPool(3);
        }

        LOG.info("Content Distribution Service activated. Endpoints: {}", Arrays.toString(config.publisherEndpoints()));
    }

    @Deactivate
    protected void deactivate() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                LOG.warn("Error closing HTTP client", e);
            }
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public DistributionResult publish(String path) {
        return publish(path, true);
    }

    @Override
    public DistributionResult publish(String path, boolean deep) {
        if (!isAvailable()) {
            return new DistributionResult(
                    Status.NOT_AVAILABLE, "Distribution service not available or not configured", path);
        }

        if (!isPathAllowed(path)) {
            return new DistributionResult(Status.FAILURE, "Path not in allowed roots: " + path, path);
        }

        LOG.info("Publishing content: {} (deep={})", path, deep);

        if (config.asyncDistribution()) {
            executorService.submit(() -> doPublish(path, deep));
            return new DistributionResult(Status.SUCCESS, "Distribution queued for async processing", path);
        } else {
            return doPublish(path, deep);
        }
    }

    private DistributionResult doPublish(String path, boolean deep) {
        String[] endpoints = config.publisherEndpoints();
        DistributionResult result = new DistributionResult(Status.SUCCESS, "Published successfully", path);

        int successCount = 0;
        int failureCount = 0;

        for (String endpoint : endpoints) {
            try {
                boolean success = sendToPublisher(endpoint, path, deep, "ADD");
                if (success) {
                    result.addEndpointResult(endpoint, true, "Published successfully");
                    successCount++;
                } else {
                    result.addEndpointResult(endpoint, false, "Failed to publish");
                    failureCount++;
                }
            } catch (Exception e) {
                LOG.error("Error publishing to endpoint: " + endpoint, e);
                result.addEndpointResult(endpoint, false, e.getMessage());
                failureCount++;
            }
        }

        if (failureCount == 0) {
            return new DistributionResult(Status.SUCCESS, "Published to " + successCount + " endpoint(s)", path);
        } else if (successCount > 0) {
            return new DistributionResult(
                    Status.PARTIAL_SUCCESS,
                    "Published to " + successCount + " of " + (successCount + failureCount) + " endpoints",
                    path);
        } else {
            return new DistributionResult(Status.FAILURE, "Failed to publish to all endpoints", path);
        }
    }

    @Override
    public DistributionResult unpublish(String path) {
        if (!isAvailable()) {
            return new DistributionResult(
                    Status.NOT_AVAILABLE, "Distribution service not available or not configured", path);
        }

        LOG.info("Unpublishing content: {}", path);

        String[] endpoints = config.publisherEndpoints();
        int successCount = 0;

        for (String endpoint : endpoints) {
            try {
                boolean success = sendToPublisher(endpoint, path, false, "DELETE");
                if (success) {
                    successCount++;
                }
            } catch (Exception e) {
                LOG.error("Error unpublishing from endpoint: " + endpoint, e);
            }
        }

        if (successCount == endpoints.length) {
            return new DistributionResult(Status.SUCCESS, "Unpublished successfully", path);
        } else if (successCount > 0) {
            return new DistributionResult(
                    Status.PARTIAL_SUCCESS,
                    "Unpublished from " + successCount + " of " + endpoints.length + " endpoints",
                    path);
        } else {
            return new DistributionResult(Status.FAILURE, "Failed to unpublish", path);
        }
    }

    @Override
    public DistributionResult distribute(ResourceResolver resolver, String path) {
        return distribute(resolver, path, true);
    }

    @Override
    public DistributionResult distribute(ResourceResolver resolver, String path, boolean deep) {
        if (!isAvailable()) {
            return new DistributionResult(
                    Status.NOT_AVAILABLE, "Distribution service not available or not configured", path);
        }

        if (!isPathAllowed(path)) {
            return new DistributionResult(Status.FAILURE, "Path not in allowed roots: " + path, path);
        }

        LOG.info("Distributing content with resolver: {} (deep={})", path, deep);

        return doDistribute(resolver, path, deep);
    }

    private DistributionResult doDistribute(ResourceResolver resolver, String path, boolean deep) {
        String[] endpoints = config.publisherEndpoints();
        DistributionResult result = new DistributionResult(Status.SUCCESS, "Distributed successfully", path);

        int successCount = 0;
        int failureCount = 0;

        for (String endpoint : endpoints) {
            try {
                boolean success = sendToPublisherWithResolver(endpoint, resolver, path, deep, "ADD");
                if (success) {
                    result.addEndpointResult(endpoint, true, "Distributed successfully");
                    successCount++;
                } else {
                    result.addEndpointResult(endpoint, false, "Failed to distribute");
                    failureCount++;
                }
            } catch (Exception e) {
                LOG.error("Error distributing to endpoint: " + endpoint, e);
                result.addEndpointResult(endpoint, false, e.getMessage());
                failureCount++;
            }
        }

        if (failureCount == 0) {
            return new DistributionResult(Status.SUCCESS, "Distributed to " + successCount + " endpoint(s)", path);
        } else if (successCount > 0) {
            return new DistributionResult(
                    Status.PARTIAL_SUCCESS,
                    "Distributed to " + successCount + " of " + (successCount + failureCount) + " endpoints",
                    path);
        } else {
            return new DistributionResult(Status.FAILURE, "Failed to distribute to all endpoints", path);
        }
    }

    @Override
    public DistributionResult delete(ResourceResolver resolver, String path) {
        if (!isAvailable()) {
            return new DistributionResult(
                    Status.NOT_AVAILABLE, "Distribution service not available or not configured", path);
        }

        LOG.info("Deleting content from publishers: {}", path);

        String[] endpoints = config.publisherEndpoints();
        int successCount = 0;

        for (String endpoint : endpoints) {
            try {
                boolean success = sendToPublisher(endpoint, path, false, "DELETE");
                if (success) {
                    successCount++;
                }
            } catch (Exception e) {
                LOG.error("Error deleting from endpoint: " + endpoint, e);
            }
        }

        if (successCount == endpoints.length) {
            return new DistributionResult(Status.SUCCESS, "Deleted successfully", path);
        } else if (successCount > 0) {
            return new DistributionResult(
                    Status.PARTIAL_SUCCESS,
                    "Deleted from " + successCount + " of " + endpoints.length + " endpoints",
                    path);
        } else {
            return new DistributionResult(Status.FAILURE, "Failed to delete", path);
        }
    }

    private boolean sendToPublisherWithResolver(
            String endpoint, ResourceResolver resolver, String path, boolean deep, String action) throws Exception {
        String url = endpoint + IMPORT_ENDPOINT;

        HttpPost post = new HttpPost(url);

        // Add basic authentication
        String auth = config.publisherUsername() + ":" + config.publisherPassword();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        post.setHeader("Authorization", "Basic " + encodedAuth);

        // Build the request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("path", path);
        requestBody.addProperty("action", action);
        requestBody.addProperty("deep", deep);

        if ("ADD".equals(action)) {
            // Export content as JSON using the provided resolver
            String contentJson = exportContentAsJsonWithResolver(resolver, path, deep);
            requestBody.addProperty("content", contentJson);
        }

        post.setEntity(new StringEntity(gson.toJson(requestBody), ContentType.APPLICATION_JSON));

        LOG.debug("Sending {} request to {} for path {}", action, url, path);

        HttpResponse response = httpClient.execute(post);
        int statusCode = response.getStatusLine().getStatusCode();

        String responseBody = "";
        if (response.getEntity() != null) {
            responseBody = EntityUtils.toString(response.getEntity());
        }

        LOG.debug("Response from {}: {} - {}", endpoint, statusCode, responseBody);

        return statusCode >= 200 && statusCode < 300;
    }

    private String exportContentAsJsonWithResolver(ResourceResolver resolver, String path, boolean deep) {
        try {
            Resource resource = resolver.getResource(path);
            if (resource != null) {
                JsonObject json = resourceToJson(resource, deep ? 10 : 1);
                return gson.toJson(json);
            }
        } catch (Exception e) {
            LOG.error("Error exporting content as JSON: " + path, e);
        }
        return "{}";
    }

    private boolean sendToPublisher(String endpoint, String path, boolean deep, String action) throws Exception {
        String url = endpoint + IMPORT_ENDPOINT;

        HttpPost post = new HttpPost(url);

        // Add basic authentication
        String auth = config.publisherUsername() + ":" + config.publisherPassword();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        post.setHeader("Authorization", "Basic " + encodedAuth);

        // Build the request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("path", path);
        requestBody.addProperty("action", action);
        requestBody.addProperty("deep", deep);

        if ("ADD".equals(action)) {
            // Export content as JSON and send
            String contentJson = exportContentAsJson(path, deep);
            requestBody.addProperty("content", contentJson);
        }

        post.setEntity(new StringEntity(gson.toJson(requestBody), ContentType.APPLICATION_JSON));

        LOG.debug("Sending {} request to {} for path {}", action, url, path);

        HttpResponse response = httpClient.execute(post);
        int statusCode = response.getStatusLine().getStatusCode();

        String responseBody = "";
        if (response.getEntity() != null) {
            responseBody = EntityUtils.toString(response.getEntity());
        }

        LOG.debug("Response from {}: {} - {}", endpoint, statusCode, responseBody);

        return statusCode >= 200 && statusCode < 300;
    }

    private String exportContentAsJson(String path, boolean deep) {
        try {
            ResourceResolver resolver = resolverFactory.getServiceResourceResolver(
                    java.util.Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, "distribution"));

            try {
                Resource resource = resolver.getResource(path);
                if (resource != null) {
                    JsonObject json = resourceToJson(resource, deep ? 10 : 1);
                    return gson.toJson(json);
                }
            } finally {
                resolver.close();
            }
        } catch (Exception e) {
            LOG.error("Error exporting content as JSON: " + path, e);
        }
        return "{}";
    }

    private JsonObject resourceToJson(Resource resource, int depth) {
        JsonObject json = new JsonObject();
        json.addProperty("jcr:path", resource.getPath());

        // Add properties
        Node node = resource.adaptTo(Node.class);
        if (node != null) {
            try {
                PropertyIterator props = node.getProperties();
                while (props.hasNext()) {
                    Property prop = props.nextProperty();
                    String name = prop.getName();

                    if (prop.isMultiple()) {
                        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
                        for (Value val : prop.getValues()) {
                            array.add(valueToString(val));
                        }
                        json.add(name, array);
                    } else {
                        json.addProperty(name, valueToString(prop.getValue()));
                    }
                }
            } catch (RepositoryException e) {
                LOG.warn("Error reading properties from: " + resource.getPath(), e);
            }
        }

        // Add children if depth allows
        if (depth > 0) {
            JsonObject children = new JsonObject();
            for (Resource child : resource.getChildren()) {
                children.add(child.getName(), resourceToJson(child, depth - 1));
            }
            if (children.size() > 0) {
                json.add(":children", children);
            }
        }

        return json;
    }

    private String valueToString(Value value) throws RepositoryException {
        switch (value.getType()) {
            case PropertyType.BINARY:
                return "[binary]";
            case PropertyType.DATE:
                return value.getDate().toInstant().toString();
            default:
                return value.getString();
        }
    }

    @Override
    public boolean isAvailable() {
        return config != null
                && config.enabled()
                && config.publisherEndpoints() != null
                && config.publisherEndpoints().length > 0;
    }

    @Override
    public String[] getPublisherEndpoints() {
        return config != null ? config.publisherEndpoints() : new String[0];
    }

    private boolean isPathAllowed(String path) {
        if (config.allowedRoots() == null || config.allowedRoots().length == 0) {
            return true;
        }
        for (String root : config.allowedRoots()) {
            if (path.startsWith(root)) {
                return true;
            }
        }
        return false;
    }
}
