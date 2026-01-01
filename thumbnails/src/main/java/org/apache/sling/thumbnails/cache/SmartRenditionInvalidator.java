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
package org.apache.sling.thumbnails.cache;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener that invalidates cached renditions when source assets change.
 *
 * <p>Listens for CHANGED and REMOVED events on file content and invalidates all renditions for the
 * affected asset.
 */
@Component(
        service = ResourceChangeListener.class,
        property = {
            ResourceChangeListener.PATHS + "=/content",
            ResourceChangeListener.CHANGES + "=CHANGED",
            ResourceChangeListener.CHANGES + "=REMOVED"
        })
public class SmartRenditionInvalidator implements ResourceChangeListener {

    private static final Logger log = LoggerFactory.getLogger(SmartRenditionInvalidator.class);

    @Reference
    private SmartRenditionService smartRenditionService;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public void onChange(java.util.List<ResourceChange> changes) {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-thumbnails");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            for (ResourceChange change : changes) {
                handleChange(resolver, change);
            }
        } catch (LoginException e) {
            log.error("Error getting service resource resolver", e);
        }
    }

    /**
     * Handle a resource change event.
     *
     * @param resolver the resource resolver
     * @param change the resource change
     */
    private void handleChange(ResourceResolver resolver, ResourceChange change) {
        String path = change.getPath();

        // Only process changes to jcr:content nodes (actual file content)
        if (!path.contains("/jcr:content")) {
            log.trace("Ignoring change to non-content node: {}", path);
            return;
        }

        // Extract asset path (remove /jcr:content and anything after)
        String assetPath = extractAssetPath(path);
        if (assetPath == null) {
            log.debug("Could not extract asset path from: {}", path);
            return;
        }

        Resource asset = resolver.getResource(assetPath);
        if (asset == null) {
            log.debug("Asset not found at path: {}", assetPath);
            return;
        }

        // Invalidate renditions for this asset
        log.info("Invalidating renditions for changed asset: {}", assetPath);
        smartRenditionService.invalidateRenditions(asset);
    }

    /**
     * Extract asset path from a jcr:content path.
     *
     * @param path the full path (e.g., /content/dam/image.jpg/jcr:content)
     * @return the asset path (e.g., /content/dam/image.jpg) or null if invalid
     */
    private String extractAssetPath(String path) {
        if (path == null || !path.contains("/jcr:content")) {
            return null;
        }

        int jcrContentIndex = path.indexOf("/jcr:content");
        return path.substring(0, jcrContentIndex);
    }
}
