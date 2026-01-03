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
package org.apache.sling.cms.ai.internal;

import org.apache.sling.cms.ai.AiTaxonomyService.TaxonomySuggestion;

/**
 * Implementation of TaxonomySuggestion for AI-generated taxonomy recommendations.
 */
public class TaxonomySuggestionImpl implements TaxonomySuggestion {

    private final String taxonomyPath;
    private final String taxonomyTitle;
    private final double confidence;
    private final String reason;

    private TaxonomySuggestionImpl(Builder builder) {
        this.taxonomyPath = builder.taxonomyPath;
        this.taxonomyTitle = builder.taxonomyTitle;
        this.confidence = builder.confidence;
        this.reason = builder.reason;
    }

    @Override
    public String getTaxonomyPath() {
        return taxonomyPath;
    }

    @Override
    public String getTaxonomyTitle() {
        return taxonomyTitle;
    }

    @Override
    public double getConfidence() {
        return confidence;
    }

    @Override
    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return String.format(
                "TaxonomySuggestion[path=%s, title=%s, confidence=%.2f, reason=%s]",
                taxonomyPath, taxonomyTitle, confidence, reason);
    }

    /**
     * Builder for TaxonomySuggestionImpl.
     */
    public static class Builder {
        private String taxonomyPath;
        private String taxonomyTitle;
        private double confidence;
        private String reason;

        public Builder taxonomyPath(String taxonomyPath) {
            this.taxonomyPath = taxonomyPath;
            return this;
        }

        public Builder taxonomyTitle(String taxonomyTitle) {
            this.taxonomyTitle = taxonomyTitle;
            return this;
        }

        public Builder confidence(double confidence) {
            this.confidence = Math.max(0.0, Math.min(1.0, confidence)); // Clamp to 0.0-1.0
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public TaxonomySuggestionImpl build() {
            if (taxonomyPath == null || taxonomyPath.isEmpty()) {
                throw new IllegalArgumentException("taxonomyPath is required");
            }
            if (taxonomyTitle == null || taxonomyTitle.isEmpty()) {
                throw new IllegalArgumentException("taxonomyTitle is required");
            }
            return new TaxonomySuggestionImpl(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
