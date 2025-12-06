/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.cms.distribution;

import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service interface for content distribution/replication from Author to Publisher.
 */
@ProviderType
public interface ContentDistributionService {

    /**
     * Publish content to configured publisher instances.
     * 
     * @param path the content path to publish
     * @return the distribution result
     */
    DistributionResult publish(String path);

    /**
     * Publish content with options.
     * 
     * @param path the content path to publish
     * @param deep whether to include descendants
     * @return the distribution result
     */
    DistributionResult publish(String path, boolean deep);

    /**
     * Distribute (publish) content using the provided ResourceResolver.
     * 
     * @param resolver the resource resolver to use
     * @param path the content path to distribute
     * @return the distribution result
     */
    DistributionResult distribute(ResourceResolver resolver, String path);

    /**
     * Distribute (publish) content with options using the provided ResourceResolver.
     * 
     * @param resolver the resource resolver to use
     * @param path the content path to distribute
     * @param deep whether to include descendants
     * @return the distribution result
     */
    DistributionResult distribute(ResourceResolver resolver, String path, boolean deep);

    /**
     * Unpublish (remove) content from publisher instances.
     * 
     * @param path the content path to unpublish
     * @return the distribution result
     */
    DistributionResult unpublish(String path);

    /**
     * Delete content from publisher instances using the provided ResourceResolver.
     * 
     * @param resolver the resource resolver to use
     * @param path the content path to delete
     * @return the distribution result
     */
    DistributionResult delete(ResourceResolver resolver, String path);

    /**
     * Check if the distribution service is available and configured.
     * 
     * @return true if available
     */
    boolean isAvailable();

    /**
     * Get the configured publisher endpoints.
     * 
     * @return array of endpoint URLs
     */
    String[] getPublisherEndpoints();
}
