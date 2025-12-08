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
package org.apache.sling.cms.distribution.impl;

import org.apache.sling.cms.PublishableResource;
import org.apache.sling.cms.distribution.ContentDistributionService;
import org.apache.sling.cms.distribution.DistributionResult;
import org.apache.sling.cms.publication.PUBLICATION_MODE;
import org.apache.sling.cms.publication.PublicationException;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the PublicationManager interface using HTTP-based content distribution.
 */
public class HttpDistributionPublicationManager extends StandalonePublicationManager {

    private static final Logger log = LoggerFactory.getLogger(HttpDistributionPublicationManager.class);

    private final ContentDistributionService distributionService;

    public HttpDistributionPublicationManager(ContentDistributionService distributionService, EventAdmin eventAdmin) {
        super(eventAdmin);
        this.distributionService = distributionService;
    }

    @Override
    public void publish(PublishableResource resource) throws PublicationException {
        log.info("Publishing via HTTP distribution: {}", resource.getPath());

        try {
            DistributionResult result =
                    distributionService.distribute(resource.getResource().getResourceResolver(), resource.getPath());

            if (!result.isSuccess()) {
                throw new PublicationException("Failed to distribute content: " + result.getMessage());
            }

            log.debug("HTTP distribution successful, updating publication information");
            super.publish(resource);
        } catch (PublicationException e) {
            throw e;
        } catch (Exception e) {
            throw new PublicationException("Failed to publish content via HTTP distribution", e);
        }
    }

    @Override
    public void unpublish(PublishableResource resource) throws PublicationException {
        log.info("Unpublishing via HTTP distribution: {}", resource.getPath());

        try {
            DistributionResult result =
                    distributionService.delete(resource.getResource().getResourceResolver(), resource.getPath());

            if (!result.isSuccess()) {
                throw new PublicationException("Failed to remove content from publisher: " + result.getMessage());
            }

            log.debug("HTTP distribution delete successful, updating publication information");
            super.unpublish(resource);
        } catch (PublicationException e) {
            throw e;
        } catch (Exception e) {
            throw new PublicationException("Failed to unpublish content via HTTP distribution", e);
        }
    }

    @Override
    public PUBLICATION_MODE getPublicationMode() {
        return PUBLICATION_MODE.CONTENT_DISTRIBUTION;
    }
}
