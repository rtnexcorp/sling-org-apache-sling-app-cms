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
package org.apache.sling.thumbnails.internal.models;

import javax.annotation.PostConstruct;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.thumbnails.delivery.DeliveryPreset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model for the delivery presets configuration page.
 * Used by the caconfig/deliverypresets component to list all presets.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class DeliveryPresetsConfig {

    private static final Logger log = LoggerFactory.getLogger(DeliveryPresetsConfig.class);

    @SlingObject
    private SlingHttpServletRequest request;

    private List<DeliveryPreset> presets = Collections.emptyList();

    @PostConstruct
    protected void init() {
        Resource suffixResource = request.getRequestPathInfo().getSuffixResource();
        if (suffixResource == null) {
            log.warn("No suffix resource found for delivery presets config");
            return;
        }

        log.debug("Loading delivery presets from: {}", suffixResource.getPath());

        presets = StreamSupport.stream(suffixResource.getChildren().spliterator(), false)
                .map(r -> r.adaptTo(DeliveryPreset.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.debug("Loaded {} delivery presets", presets.size());
    }

    /**
     * Get all delivery presets.
     *
     * @return list of delivery presets
     */
    public List<DeliveryPreset> getPresets() {
        return presets;
    }

    /**
     * Check if there are any presets configured.
     *
     * @return true if presets exist
     */
    public boolean hasPresets() {
        return !presets.isEmpty();
    }
}
