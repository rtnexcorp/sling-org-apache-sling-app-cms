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

import org.apache.sling.api.SlingConstants;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource Change Listener that queues jobs to automatically generate renditions
 * when assets are uploaded. This listener detects new sling:File resources and
 * creates background jobs for rendition generation.
 * <p>
 * This listener also implements EventHandler to listen for metadata extraction
 * completion events, ensuring renditions are generated only after metadata is
 * available. This prevents race conditions and enables metadata-aware transformations.
 * </p>
 *
 * @since 1.1.0 - Added EventHandler for metadata extraction coordination
 */
@Component(
        service = {ResourceChangeListener.class, ExternalResourceChangeListener.class, EventHandler.class},
        property = {
            ResourceChangeListener.CHANGES + "=ADDED",
            EventConstants.EVENT_TOPIC + "=org/apache/sling/cms/metadata/EXTRACTED"
        },
        immediate = true)
public class AutoRenditionListener implements ResourceChangeListener, ExternalResourceChangeListener, EventHandler {

    private static final Logger log = LoggerFactory.getLogger(AutoRenditionListener.class);

    /**
     * Event topic for metadata extraction completion.
     * Must match the topic fired by FileMetadataExtractorConsumer.
     */
    private static final String EVENT_METADATA_EXTRACTED = "org/apache/sling/cms/metadata/EXTRACTED";

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
                    .forEach(resource -> processResource(resource, serviceResolver));
        } catch (LoginException e) {
            log.error("Failed to get service user for auto-rendition processing", e);
        }
    }

    /**
     * Handle OSGi events, specifically metadata extraction completion events.
     * When metadata extraction completes, queue rendition generation jobs.
     *
     * @param event the OSGi event
     */
    @Override
    public void handleEvent(Event event) {
        if (!autoRenditionConfig.isEnabled()) {
            log.trace("Auto-rendition is disabled, skipping event");
            return;
        }

        String topic = event.getTopic();
        if (EVENT_METADATA_EXTRACTED.equals(topic)) {
            String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
            log.debug("Metadata extraction completed for {}, queueing rendition jobs", path);

            try (ResourceResolver serviceResolver = transformationServiceUser.getTransformationServiceUser()) {
                Resource resource = serviceResolver.getResource(path);
                if (resource != null && isSupported(resource) && matchesMimeType(resource)) {
                    queueRenditionJob(resource);
                }
            } catch (LoginException e) {
                log.error("Failed to get service user for metadata event processing", e);
            }
        }
    }

    /**
     * Process a resource for rendition generation.
     * Checks if metadata exists before queueing jobs.
     *
     * @param resource the resource to process
     * @param resolver the resource resolver
     */
    private void processResource(Resource resource, ResourceResolver resolver) {
        // Check if metadata already exists
        Resource metadataResource = resource.getChild("jcr:content/metadata");

        if (metadataResource != null) {
            // Metadata exists, queue rendition jobs immediately
            log.debug("Metadata exists for {}, queueing rendition jobs", resource.getPath());
            queueRenditionJob(resource);
        } else {
            // Metadata doesn't exist yet, renditions will be queued when
            // metadata extraction completes (via handleEvent)
            log.debug("Metadata not yet available for {}, waiting for extraction event", resource.getPath());
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
