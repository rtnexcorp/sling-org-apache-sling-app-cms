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
package org.apache.sling.thumbnails.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.thumbnails.AutoRenditionConfig;
import org.apache.sling.thumbnails.ThumbnailSupport;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource Change Listener that queues jobs to automatically generate renditions
 * when assets are uploaded. This listener detects new sling:File resources and
 * creates background jobs for rendition generation.
 */
@Component(
        service = {ResourceChangeListener.class, ExternalResourceChangeListener.class},
        property = {ResourceChangeListener.CHANGES + "=ADDED"},
        immediate = true)
public class AutoRenditionListener implements ResourceChangeListener, ExternalResourceChangeListener {

    private static final Logger log = LoggerFactory.getLogger(AutoRenditionListener.class);

    @Reference
    private JobManager jobManager;

    @Reference
    private TransformationServiceUser transformationServiceUser;

    @Reference
    private ThumbnailSupport thumbnailSupport;

    @Reference
    private AutoRenditionConfig autoRenditionConfig;

    private String[] contentPaths;

    @Activate
    @Modified
    protected void activate() {
        this.contentPaths = autoRenditionConfig.getContentPaths();
        log.info(
                "Auto-rendition listener activated. Enabled: {}, Paths: {}, Transformations: {}",
                autoRenditionConfig.isEnabled(),
                Arrays.toString(contentPaths),
                Arrays.toString(autoRenditionConfig.getTransformationNames()));
    }

    @Override
    public void onChange(List<ResourceChange> changes) {
        if (!autoRenditionConfig.isEnabled()) {
            log.trace("Auto-rendition is disabled, skipping");
            return;
        }

        try (ResourceResolver serviceResolver = transformationServiceUser.getTransformationServiceUser()) {
            changes.stream()
                    .filter(this::isUnderConfiguredPath)
                    .map(rc -> serviceResolver.getResource(rc.getPath()))
                    .filter(this::isSupported)
                    .filter(this::matchesMimeType)
                    .forEach(this::queueRenditionJob);
        } catch (LoginException e) {
            log.error("Failed to get service user for auto-rendition processing", e);
        }
    }

    private boolean isUnderConfiguredPath(ResourceChange change) {
        String path = change.getPath();
        for (String contentPath : contentPaths) {
            if (path.startsWith(contentPath + "/") || path.equals(contentPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSupported(Resource resource) {
        if (resource == null) {
            return false;
        }
        String resourceType = resource.getResourceType();
        return thumbnailSupport.getSupportedTypes().contains(resourceType)
                && thumbnailSupport.getPersistableTypes().contains(resourceType);
    }

    private boolean matchesMimeType(Resource resource) {
        String mimeType = getMimeType(resource);
        if (mimeType == null) {
            return false;
        }

        String[] supportedTypes = autoRenditionConfig.getSupportedMimeTypes();
        for (String pattern : supportedTypes) {
            if (matchesMimePattern(mimeType, pattern)) {
                return true;
            }
        }
        return false;
    }

    private String getMimeType(Resource resource) {
        try {
            String metaTypePath = thumbnailSupport.getMetaTypePropertyPath(resource.getResourceType());
            return resource.getValueMap().get(metaTypePath, String.class);
        } catch (IllegalArgumentException e) {
            log.debug("Could not get MIME type for resource: {}", resource.getPath());
            return null;
        }
    }

    private boolean matchesMimePattern(String mimeType, String pattern) {
        if (pattern.equals("*/*") || pattern.equals(mimeType)) {
            return true;
        }
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return mimeType.startsWith(prefix);
        }
        return false;
    }

    private void queueRenditionJob(Resource resource) {
        String[] transformations = autoRenditionConfig.getTransformationNames();
        for (String transformationName : transformations) {
            Map<String, Object> jobProperties = new HashMap<>();
            jobProperties.put(AutoRenditionJobConsumer.PROPERTY_PATH, resource.getPath());
            jobProperties.put(AutoRenditionJobConsumer.PROPERTY_TRANSFORMATION, transformationName);

            log.debug(
                    "Queueing auto-rendition job for {} with transformation {}",
                    resource.getPath(),
                    transformationName);

            jobManager.addJob(AutoRenditionJobConsumer.TOPIC, jobProperties);
        }
    }
}
