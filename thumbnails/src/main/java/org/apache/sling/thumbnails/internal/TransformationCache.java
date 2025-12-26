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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.transformation.Transformation;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = {Runnable.class, TransformationCache.class, EventHandler.class},
        property = {
            EventConstants.EVENT_TOPIC + "=org/apache/sling/api/resource/Resource/CHANGED",
            EventConstants.EVENT_FILTER + "=(&(resourceType=sling/thumbnails/transformation))",
            "scheduler.period=L3600"
        })
public class TransformationCache implements EventHandler, Runnable {

    private static final Logger log = LoggerFactory.getLogger(TransformationCache.class);
    private final TransformationServiceUser transformationServiceUser;
    private final Map<String, Optional<String>> cache = new ConcurrentHashMap<>();

    @Activate
    public TransformationCache(@Reference TransformationServiceUser transformationServiceUser) {
        this.transformationServiceUser = transformationServiceUser;
    }

    public Optional<Transformation> getTransformation(ResourceResolver resolver, String name) {
        Optional<String> cachedPath = cache.computeIfAbsent(name, this::findTransformation);
        if (!cachedPath.isPresent()) {
            return Optional.empty();
        }

        // Use service resolver to get the transformation resource since the request resolver
        // might not have access to /conf paths
        try (ResourceResolver serviceResolver = transformationServiceUser.getTransformationServiceUser()) {
            Resource transformationResource = serviceResolver.getResource(cachedPath.get());
            if (transformationResource != null) {
                return Optional.ofNullable(transformationResource.adaptTo(Transformation.class));
            }
        } catch (LoginException e) {
            log.error("Could not get service resolver for transformation lookup", e);
        }
        return Optional.empty();
    }

    @Override
    public void handleEvent(Event event) {
        cache.clear();
    }

    private Optional<String> findTransformation(String name) {
        try {
            try (ResourceResolver serviceResolver = transformationServiceUser.getTransformationServiceUser()) {
                // Handle both absolute paths and simple names
                // If name starts with /, remove it to get the transformation name
                String transformationName = name.startsWith("/") ? name.substring(1) : name;
                log.debug("Finding transformation with name: {}", transformationName);

                // Search in standard transformation paths
                String[] searchPaths = {
                    "/conf/global/dam/transformations/" + transformationName,
                    "/libs/conf/global/dam/transformations/" + transformationName,
                    "/apps/conf/global/dam/transformations/" + transformationName
                };

                for (String path : searchPaths) {
                    log.debug("Checking path: {}", path);
                    Resource transformation = serviceResolver.getResource(path);
                    if (transformation != null) {
                        String resourceType = transformation.getResourceType();
                        log.debug("Found resource at {} with type: {}", path, resourceType);
                        if ("sling/thumbnails/transformation".equals(resourceType)) {
                            log.info("Found transformation at: {}", path);
                            return Optional.of(path);
                        }
                    } else {
                        log.debug("No resource at path: {}", path);
                    }
                }

                log.warn("No transformation found with name: {} after checking all paths", transformationName);
                return Optional.empty();
            }
        } catch (LoginException le) {
            throw new RuntimeException("Could not get service resolver", le);
        }
    }

    public Set<Entry<String, Optional<String>>> getCacheEntries() {
        return cache.entrySet();
    }

    @Override
    public void run() {
        cache.clear();
    }
}
