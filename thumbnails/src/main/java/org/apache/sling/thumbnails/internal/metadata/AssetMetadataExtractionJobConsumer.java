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

import java.io.IOException;
import java.util.Collections;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.thumbnails.metadata.MetadataExtractionService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling job consumer for asynchronous asset metadata extraction.
 *
 * <p>Processes jobs queued by {@link AssetMetadataExtractionListener} to extract
 * and persist metadata for uploaded or updated assets.
 *
 * @since 1.2.0
 */
@Component(
        service = {JobConsumer.class},
        property = {JobConsumer.PROPERTY_TOPICS + "=" + AssetMetadataExtractionJobConsumer.TOPIC})
public class AssetMetadataExtractionJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AssetMetadataExtractionJobConsumer.class);

    public static final String TOPIC = "org/apache/sling/thumbnails/metadata/ExtractMetadata";

    @Reference
    private MetadataExtractionService metadataExtractionService;

    @Reference
    private ResourceResolverFactory factory;

    @Override
    public JobResult process(Job job) {
        String path = job.getProperty(SlingConstants.PROPERTY_PATH, String.class);

        if (path == null) {
            LOG.error("Job does not contain path property");
            return JobResult.FAILED;
        }

        try (ResourceResolver serviceResolver = factory.getServiceResourceResolver(
                Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, "sling-thumbnails"))) {

            LOG.debug("Processing metadata extraction for: {}", path);

            Resource resource = serviceResolver.getResource(path);
            if (resource == null) {
                LOG.warn("Resource not found: {}", path);
                return JobResult.FAILED;
            }

            // Extract and persist metadata
            metadataExtractionService.extractAndPersistMetadata(resource);

            LOG.info("Metadata extraction completed successfully for: {}", path);
            return JobResult.OK;

        } catch (LoginException e) {
            LOG.error("Exception getting service user for: {}", path, e);
            return JobResult.FAILED;
        } catch (IOException e) {
            LOG.error("Failed to extract metadata from: {}", path, e);
            return JobResult.FAILED;
        } catch (Exception e) {
            LOG.error("Unexpected error extracting metadata from: {}", path, e);
            return JobResult.FAILED;
        }
    }
}
