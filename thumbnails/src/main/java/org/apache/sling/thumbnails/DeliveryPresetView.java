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
package org.apache.sling.thumbnails;

import org.apache.sling.thumbnails.delivery.DeliveryPreset;

/**
 * View wrapper for DeliveryPreset that includes computed properties for HTL templates.
 * This class decorates a DeliveryPreset with additional properties like delivery URLs
 * that are needed for UI rendering but don't belong in the core API.
 */
public class DeliveryPresetView {

    private final DeliveryPreset preset;
    private final String deliveryUrl;

    public DeliveryPresetView(DeliveryPreset preset, String deliveryUrl) {
        this.preset = preset;
        this.deliveryUrl = deliveryUrl;
    }

    /**
     * Get the delivery URL for this preset.
     * @return the full URL to access the asset via this preset
     */
    public String getDeliveryUrl() {
        return deliveryUrl;
    }

    // Delegate all DeliveryPreset methods to the wrapped preset

    public String getName() {
        return preset.getName();
    }

    public String getTitle() {
        return preset.getTitle();
    }

    public String getDescription() {
        return preset.getDescription();
    }

    public int getWidth() {
        return preset.getWidth();
    }

    public int getHeight() {
        return preset.getHeight();
    }

    public int getQuality() {
        return preset.getQuality();
    }

    public String getFormat() {
        return preset.getFormat();
    }

    public String getCropMode() {
        return preset.getCropMode();
    }

    public boolean isEnabled() {
        return preset.isEnabled();
    }

    public DeliveryPreset getPreset() {
        return preset;
    }
}
