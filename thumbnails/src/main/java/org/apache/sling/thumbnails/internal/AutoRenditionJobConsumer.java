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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.thumbnails.OutputFileFormat;
import org.apache.sling.thumbnails.RenditionSupport;
import org.apache.sling.thumbnails.Transformation;
import org.apache.sling.thumbnails.Transformer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Job Consumer for generating renditions automatically when assets are uploaded.
 * This consumer processes jobs queued by the AutoRenditionListener and generates
 * the configured transformations for each asset.
 */
@Component(
        service = JobConsumer.class,
        property = {JobConsumer.PROPERTY_TOPICS + "=" + AutoRenditionJobConsumer.TOPIC})
public class AutoRenditionJobConsumer implements JobConsumer {

    private static final Logger log = LoggerFactory.getLogger(AutoRenditionJobConsumer.class);

    /**
     * Job topic for auto-rendition generation.
     */
    public static final String TOPIC = "org/apache/sling/thumbnails/AutoRendition";

    /**
     * Job property for the resource path.
     */
    public static final String PROPERTY_PATH = "path";

    /**
     * Job property for the transformation name.
     */
    public static final String PROPERTY_TRANSFORMATION = "transformation";

    /**
     * Default output format for renditions.
     */
    private static final OutputFileFormat DEFAULT_FORMAT = OutputFileFormat.PNG;

    @Reference
    private TransformationServiceUser transformationServiceUser;

    @Reference
    private TransformationCache transformationCache;

    @Reference
    private Transformer transformer;

    @Reference
    private RenditionSupport renditionSupport;

    @Override
    public JobResult process(Job job) {
        String path = job.getProperty(PROPERTY_PATH, String.class);
        String transformationName = job.getProperty(PROPERTY_TRANSFORMATION, String.class);

        if (path == null || transformationName == null) {
            log.error("Invalid job properties: path={}, transformation={}", path, transformationName);
            return JobResult.CANCEL;
        }

        log.debug("Processing auto-rendition for {} with transformation {}", path, transformationName);

        try (ResourceResolver serviceResolver = transformationServiceUser.getTransformationServiceUser()) {
            Resource resource = serviceResolver.getResource(path);
            if (resource == null) {
                log.warn("Resource not found: {}", path);
                return JobResult.CANCEL;
            }

            if (!renditionSupport.supportsRenditions(resource)) {
                log.debug("Resource does not support renditions: {}", path);
                return JobResult.OK;
            }

            // Get the transformation
            Optional<Transformation> transformationOpt =
                    transformationCache.getTransformation(serviceResolver, "/" + transformationName);
            if (!transformationOpt.isPresent()) {
                log.warn("Transformation not found: {}", transformationName);
                return JobResult.CANCEL;
            }

            Transformation transformation = transformationOpt.get();

            // Generate the rendition
            String renditionName =
                    "/" + transformationName + "." + DEFAULT_FORMAT.name().toLowerCase();

            // Skip if rendition already exists
            if (renditionSupport.renditionExists(resource, renditionName)) {
                log.debug("Rendition already exists: {} for {}", renditionName, path);
                return JobResult.OK;
            }

            // Transform and save
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            transformer.transform(resource, transformation, DEFAULT_FORMAT, baos);

            renditionSupport.setRendition(resource, renditionName, new ByteArrayInputStream(baos.toByteArray()));

            log.info("Successfully generated rendition {} for {}", renditionName, path);
            return JobResult.OK;

        } catch (LoginException e) {
            log.error("Failed to get service user", e);
            return JobResult.FAILED;
        } catch (PersistenceException e) {
            log.error("Failed to save rendition for {}: {}", path, e.getMessage(), e);
            return JobResult.FAILED;
        } catch (IOException e) {
            log.error("Failed to generate rendition for {}: {}", path, e.getMessage(), e);
            return JobResult.FAILED;
        }
    }
}
