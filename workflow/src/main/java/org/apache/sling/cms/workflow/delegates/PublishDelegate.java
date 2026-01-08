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
package org.apache.sling.cms.workflow.delegates;

import java.util.Collections;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.PublishableResource;
import org.apache.sling.cms.ResourceTree;
import org.apache.sling.cms.publication.IsPublishableResourceContainer;
import org.apache.sling.cms.publication.IsPublishableResourceType;
import org.apache.sling.cms.publication.PublicationException;
import org.apache.sling.cms.publication.PublicationManager;
import org.apache.sling.cms.publication.PublicationManagerFactory;
import org.apache.sling.cms.workflow.delegate.DelegateExecution;
import org.apache.sling.cms.workflow.delegate.JavaDelegate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes content to staging or production environment.
 *
 * <p>Handles content publishing operations using the PublicationManager.
 * The target environment can be specified via the "environment" field in BPMN or as a variable.</p>
 *
 * <p>Supports recursive (deep) publishing when the "deep" variable is set to "true".
 * When deep=true, all child pages and content under the specified path will be published.</p>
 */
@Component(
        service = JavaDelegate.class,
        property = "delegate.class=org.apache.sling.cms.workflow.delegates.PublishDelegate")
public class PublishDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(PublishDelegate.class);

    @Reference
    private PublicationManagerFactory publicationManagerFactory;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String contentPath = execution.getBusinessKey();
        if (contentPath == null || contentPath.isEmpty()) {
            contentPath = (String) execution.getVariable("contentPath");
        }

        // Get environment from field or variable (staging/production)
        String environment = execution.getFieldValue("environment");
        if (environment == null || environment.isEmpty()) {
            environment = (String) execution.getVariable("environment");
        }
        if (environment == null || environment.isEmpty()) {
            environment = "staging"; // Default to staging
        }

        // Get deep flag from variable (recursive publishing)
        // Handle both Boolean and String types
        boolean deep = false;
        Object deepVar = execution.getVariable("deep");
        if (deepVar instanceof Boolean) {
            deep = (Boolean) deepVar;
        } else if (deepVar instanceof String) {
            deep = "true".equalsIgnoreCase((String) deepVar);
        } else if (deepVar != null) {
            deep = "true".equalsIgnoreCase(String.valueOf(deepVar));
        }

        LOG.info(
                "Publishing content to {}: {} (deep={}) for process instance: {}",
                environment,
                contentPath,
                deep,
                execution.getProcessInstanceId());

        ResourceResolver resolver = execution.getResourceResolver();
        if (resolver == null) {
            throw new Exception("ResourceResolver not available in execution context");
        }

        try {
            // Get the resource to publish
            Resource resource = resolver.getResource(contentPath);
            if (resource == null) {
                throw new Exception("Content not found: " + contentPath);
            }

            // Get PublicationManager
            PublicationManager publicationManager = resolver.adaptTo(PublicationManager.class);
            if (publicationManager == null) {
                publicationManager = publicationManagerFactory.getPublicationManager();
            }

            if (publicationManager == null) {
                throw new Exception("PublicationManager not available");
            }

            // Collect resources to publish
            Stream<PublishableResource> toPublish;
            int publishCount = 0;

            if (deep) {
                // Deep publishing: traverse the tree and publish all children
                LOG.info("Deep publishing enabled - traversing children of {}", contentPath);
                toPublish = ResourceTree.stream(
                                resource, new IsPublishableResourceContainer(), new IsPublishableResourceType())
                        .map(rt -> rt.getResource().adaptTo(PublishableResource.class))
                        .filter(pr -> pr != null);
            } else {
                // Single resource publishing
                PublishableResource publishable = resource.adaptTo(PublishableResource.class);
                if (publishable == null) {
                    throw new Exception("Content is not publishable: " + contentPath);
                }
                toPublish = Collections.singletonList(publishable).stream();
            }

            // Publish all resources in the stream
            String lastPublishedUrl = null;
            int failureCount = 0;

            for (PublishableResource pr : (Iterable<PublishableResource>) toPublish::iterator) {
                try {
                    publicationManager.publish(pr);
                    publishCount++;
                    lastPublishedUrl = pr.getPublishedUrl();
                    LOG.debug("Published: {}", pr.getPath());
                } catch (PublicationException e) {
                    failureCount++;
                    LOG.warn("Failed to publish resource: {}", pr.getPath(), e);
                    // Continue publishing other resources even if one fails
                }
            }

            // Set workflow variables
            execution.setVariable("publishedTo", environment);
            execution.setVariable("publishedAt", System.currentTimeMillis());
            execution.setVariable("publishedPath", contentPath);
            execution.setVariable("publishedUrl", lastPublishedUrl);
            execution.setVariable("publishCount", publishCount);
            execution.setVariable("failureCount", failureCount);
            execution.setVariable("deepPublish", deep);

            // Determine status based on results
            if (publishCount == 0 && failureCount == 0) {
                if (deep) {
                    // When deep=true and no publishable resources found, treat as success with warning
                    // This is valid for folders that only contain non-publishable resources
                    LOG.warn("No publishable resources found under {}", contentPath);
                    execution.setVariable("publishStatus", "SUCCESS");
                    LOG.info(
                            "Publishing completed for {}: {} (0 publishable resources found)",
                            environment,
                            contentPath);
                } else {
                    // When deep=false and single resource is not publishable, this is an error
                    throw new Exception("Resource is not publishable: " + contentPath);
                }
            } else if (publishCount > 0 && failureCount == 0) {
                execution.setVariable("publishStatus", "SUCCESS");
                LOG.info(
                        "Content published successfully to {}: {} ({} resource(s))",
                        environment,
                        contentPath,
                        publishCount);
            } else if (publishCount > 0 && failureCount > 0) {
                // Partial success - some published, some failed
                execution.setVariable("publishStatus", "PARTIAL");
                LOG.warn(
                        "Content partially published to {}: {} ({} succeeded, {} failed)",
                        environment,
                        contentPath,
                        publishCount,
                        failureCount);
            } else {
                // publishCount == 0 && failureCount > 0 - all failed
                execution.setVariable("publishStatus", "FAILED");
                throw new Exception(
                        String.format("All %d resources failed to publish under: %s", failureCount, contentPath));
            }

        } catch (Exception e) {
            execution.setVariable("publishStatus", "FAILED");
            execution.setVariable("publishError", e.getMessage());
            LOG.error("Publishing failed for {}", contentPath, e);
            throw new Exception("Publishing failed: " + e.getMessage(), e);
        }
    }
}
