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

import org.apache.sling.thumbnails.delivery.DeliveryResolution;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of DeliveryResolution.
 * Immutable result object for delivery resolution.
 */
public class DeliveryResolutionImpl implements DeliveryResolution {

    private final String resolvedFormat;
    private final String url;
    private final String requestedFormat;
    private final boolean fallback;
    private final String mimeType;
    private final int width;
    private final int height;
    private final int quality;
    private final String presetName;
    private final boolean success;
    private final String errorMessage;

    private DeliveryResolutionImpl(Builder builder) {
        this.resolvedFormat = builder.resolvedFormat;
        this.url = builder.url;
        this.requestedFormat = builder.requestedFormat;
        this.fallback = builder.fallback;
        this.mimeType = builder.mimeType;
        this.width = builder.width;
        this.height = builder.height;
        this.quality = builder.quality;
        this.presetName = builder.presetName;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
    }

    @Override
    @NotNull
    public String getResolvedFormat() {
        return resolvedFormat;
    }

    @Override
    @NotNull
    public String getUrl() {
        return url;
    }

    @Override
    @NotNull
    public String getRequestedFormat() {
        return requestedFormat;
    }

    @Override
    public boolean isFallback() {
        return fallback;
    }

    @Override
    @NotNull
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getQuality() {
        return quality;
    }

    @Override
    @NotNull
    public String getPresetName() {
        return presetName;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Create a new builder.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create an error resolution.
     *
     * @param presetName the preset name
     * @param errorMessage the error message
     * @return an error resolution
     */
    public static DeliveryResolution error(String presetName, String errorMessage) {
        return builder()
                .presetName(presetName)
                .success(false)
                .errorMessage(errorMessage)
                .url("")
                .resolvedFormat("")
                .requestedFormat("")
                .mimeType("")
                .build();
    }

    /**
     * Builder for DeliveryResolutionImpl.
     */
    public static class Builder {
        private String resolvedFormat = "";
        private String url = "";
        private String requestedFormat = "";
        private boolean fallback = false;
        private String mimeType = "";
        private int width = 0;
        private int height = 0;
        private int quality = 80;
        private String presetName = "";
        private boolean success = true;
        private String errorMessage = null;

        public Builder resolvedFormat(String resolvedFormat) {
            this.resolvedFormat = resolvedFormat;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder requestedFormat(String requestedFormat) {
            this.requestedFormat = requestedFormat;
            return this;
        }

        public Builder fallback(boolean fallback) {
            this.fallback = fallback;
            return this;
        }

        public Builder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder quality(int quality) {
            this.quality = quality;
            return this;
        }

        public Builder presetName(String presetName) {
            this.presetName = presetName;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public DeliveryResolution build() {
            return new DeliveryResolutionImpl(this);
        }
    }
}
