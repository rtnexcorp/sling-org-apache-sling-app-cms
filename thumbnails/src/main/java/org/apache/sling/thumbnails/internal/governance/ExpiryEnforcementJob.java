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
package org.apache.sling.thumbnails.internal.governance;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.cms.AssetGovernanceService;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduled job to enforce asset expiry policies.
 * Automatically unpublishes expired assets.
 */
@Component(
        service = JobConsumer.class,
        property = {JobConsumer.PROPERTY_TOPICS + "=org/apache/sling/thumbnails/expiryEnforcement"})
public class ExpiryEnforcementJob implements JobConsumer {

    private static final Logger log = LoggerFactory.getLogger(ExpiryEnforcementJob.class);

    @Reference
    private AssetGovernanceService governanceService;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public JobResult process(@NotNull Job job) {
        log.info("Starting asset expiry enforcement job");

        try {
            List<String> expiredAssets = governanceService.getExpiredAssets();

            if (expiredAssets.isEmpty()) {
                log.info("No expired assets found");
                return JobResult.OK;
            }

            int processed = 0;
            int failed = 0;

            // Create service resource resolver to resolve asset paths
            java.util.Map<String, Object> authInfo = new java.util.HashMap<>();
            authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-governance");

            try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
                for (String assetPath : expiredAssets) {
                    try {
                        Resource assetResource = resolver.getResource(assetPath);
                        if (assetResource != null) {
                            governanceService.unpublishExpiredAsset(assetResource);
                            processed++;
                            log.info("Unpublished expired asset: {}", assetPath);
                        } else {
                            log.warn("Could not resolve asset resource: {}", assetPath);
                            failed++;
                        }
                    } catch (Exception e) {
                        log.error("Failed to unpublish expired asset: {}", assetPath, e);
                        failed++;
                    }
                }
            }

            log.info("Asset expiry enforcement completed. Processed: {}, Failed: {}", processed, failed);

            if (failed > 0) {
                return JobResult.CANCEL;
            }

            return JobResult.OK;

        } catch (Exception e) {
            log.error("Error during asset expiry enforcement", e);
            return JobResult.CANCEL;
        }
    }
}
