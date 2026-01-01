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

import javax.annotation.PostConstruct;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model for displaying cache statistics.
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class CacheStatisticsModel {

    private static final Logger log = LoggerFactory.getLogger(CacheStatisticsModel.class);

    @OSGiService
    private SmartRenditionService smartRenditionService;

    private CacheStatistics statistics;

    @PostConstruct
    protected void init() {
        if (smartRenditionService != null) {
            statistics = smartRenditionService.getStatistics();
        } else {
            log.warn("SmartRenditionService not available");
            statistics = new CacheStatistics(); // Empty statistics
        }
    }

    public long getHits() {
        return statistics != null ? statistics.getHits() : 0;
    }

    public long getMisses() {
        return statistics != null ? statistics.getMisses() : 0;
    }

    public long getEntries() {
        return statistics != null ? statistics.getEntries() : 0;
    }

    public long getSizeMB() {
        return statistics != null ? statistics.getSizeMB() : 0;
    }

    public double getHitRatio() {
        return statistics != null ? statistics.getHitRatio() : 0.0;
    }

    public String getHitRatioPercent() {
        return String.format("%.1f%%", getHitRatio() * 100);
    }

    public long getTotalRequests() {
        return getHits() + getMisses();
    }

    public boolean isAvailable() {
        return smartRenditionService != null;
    }
}
