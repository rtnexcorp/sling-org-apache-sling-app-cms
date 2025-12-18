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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for rendition caching.
 */
@ObjectClassDefinition(
        name = "Rendition Cache Configuration",
        description = "Configuration for smart rendition caching")
public @interface RenditionCacheConfig {

    /**
     * Enable caching.
     *
     * @return true if caching is enabled
     */
    @AttributeDefinition(name = "Enabled", description = "Enable rendition caching")
    boolean enabled() default true;

    /**
     * Maximum cache size in megabytes.
     *
     * @return max size in MB
     */
    @AttributeDefinition(name = "Max Cache Size (MB)", description = "Maximum cache size in megabytes (0 = unlimited)")
    int maxCacheSizeMB() default 10240; // 10 GB

    /**
     * Maximum rendition age in hours.
     *
     * @return max age in hours
     */
    @AttributeDefinition(
            name = "Max Rendition Age (hours)",
            description = "Maximum age of cached renditions in hours (0 = unlimited)")
    int maxAgeHours() default 720; // 30 days

    /**
     * Enable statistics tracking.
     *
     * @return true if statistics are enabled
     */
    @AttributeDefinition(name = "Enable Statistics", description = "Track cache hit/miss statistics")
    boolean enableStatistics() default true;
}
