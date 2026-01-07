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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.PublishableResource;
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

        LOG.info(
                "Publishing content to {}: {} for process instance: {}",
                environment,
                contentPath,
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

            // Adapt to PublishableResource
            PublishableResource publishable = resource.adaptTo(PublishableResource.class);
            if (publishable == null) {
                throw new Exception("Content is not publishable: " + contentPath);
            }

            // Get PublicationManager
            PublicationManager publicationManager = resolver.adaptTo(PublicationManager.class);
            if (publicationManager == null) {
                publicationManager = publicationManagerFactory.getPublicationManager();
            }

            if (publicationManager == null) {
                throw new Exception("PublicationManager not available");
            }

            // Publish the content
            publicationManager.publish(publishable);

            execution.setVariable("publishedTo", environment);
            execution.setVariable("publishedAt", System.currentTimeMillis());
            execution.setVariable("publishStatus", "SUCCESS");
            execution.setVariable("publishedPath", contentPath);
            execution.setVariable("publishedUrl", publishable.getPublishedUrl());

            LOG.info("Content published successfully to {}: {}", environment, contentPath);

        } catch (Exception e) {
            execution.setVariable("publishStatus", "FAILED");
            execution.setVariable("publishError", e.getMessage());
            LOG.error("Publishing failed for {}", contentPath, e);
            throw new Exception("Publishing failed: " + e.getMessage(), e);
        }
    }
}
