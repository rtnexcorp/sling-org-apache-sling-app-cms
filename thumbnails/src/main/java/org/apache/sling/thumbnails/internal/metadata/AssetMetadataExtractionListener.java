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
package org.apache.sling.thumbnails.internal.metadata;

import java.util.Collections;
import java.util.List;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource change listener that triggers metadata extraction when assets are uploaded or updated.
 *
 * <p>Listens for:
 * <ul>
 *   <li>ADDED events - new assets uploaded</li>
 *   <li>CHANGED events - existing assets updated</li>
 * </ul>
 *
 * <p>Creates a Sling job for asynchronous metadata extraction to avoid blocking the upload.
 *
 * @since 1.2.0
 */
@Component(
        service = {
            AssetMetadataExtractionListener.class,
            ResourceChangeListener.class,
            ExternalResourceChangeListener.class
        },
        property = {
            ResourceChangeListener.CHANGES + "=ADDED",
            ResourceChangeListener.CHANGES + "=CHANGED",
            ResourceChangeListener.PATHS + "=/content",
            ResourceChangeListener.PATHS + "=/static"
        },
        immediate = true)
public class AssetMetadataExtractionListener implements ResourceChangeListener, ExternalResourceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(AssetMetadataExtractionListener.class);

    @Reference
    private JobManager jobManager;

    @Reference
    private ResourceResolverFactory factory;

    @Override
    public void onChange(List<ResourceChange> changes) {
        try (ResourceResolver serviceResolver = factory.getServiceResourceResolver(
                Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, "sling-thumbnails"))) {

            changes.stream()
                    .map(rc -> serviceResolver.getResource(rc.getPath()))
                    .filter(r -> r != null && isAssetFile(r))
                    .forEach(r -> {
                        LOG.debug("Queueing metadata extraction for: {}", r.getPath());
                        jobManager.addJob(
                                AssetMetadataExtractionJobConsumer.TOPIC,
                                Collections.singletonMap(SlingConstants.PROPERTY_PATH, r.getPath()));
                    });

        } catch (LoginException e) {
            LOG.error("Exception getting service user for metadata extraction", e);
        }
    }

    /**
     * Check if resource is an asset file (sling:File or nt:file).
     */
    private boolean isAssetFile(Resource resource) {
        String resourceType = resource.getResourceType();
        return "sling:File".equals(resourceType) || "nt:file".equals(resourceType);
    }
}
