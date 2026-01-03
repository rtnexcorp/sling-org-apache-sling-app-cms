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

import java.util.Collections;
import java.util.List;

import org.apache.sling.cms.ai.AiResponse;
import org.apache.sling.cms.ai.AiTaxonomyService.TaxonomyAnalysisResult;
import org.apache.sling.cms.ai.AiTaxonomyService.TaxonomySuggestion;

/**
 * Implementation of TaxonomyAnalysisResult containing taxonomy suggestions.
 */
public class TaxonomyAnalysisResultImpl implements TaxonomyAnalysisResult {

    private final List<TaxonomySuggestion> suggestions;
    private final AiResponse aiResponse;
    private final int maxSuggestions;
    private final double minConfidence;

    private TaxonomyAnalysisResultImpl(Builder builder) {
        this.suggestions = Collections.unmodifiableList(builder.suggestions);
        this.aiResponse = builder.aiResponse;
        this.maxSuggestions = builder.maxSuggestions;
        this.minConfidence = builder.minConfidence;
    }

    @Override
    public List<TaxonomySuggestion> getSuggestions() {
        return suggestions;
    }

    @Override
    public AiResponse getAiResponse() {
        return aiResponse;
    }

    @Override
    public int getMaxSuggestions() {
        return maxSuggestions;
    }

    @Override
    public double getMinConfidence() {
        return minConfidence;
    }

    @Override
    public String toString() {
        return String.format(
                "TaxonomyAnalysisResult[suggestions=%d, maxSuggestions=%d, minConfidence=%.2f]",
                suggestions.size(), maxSuggestions, minConfidence);
    }

    /**
     * Builder for TaxonomyAnalysisResultImpl.
     */
    public static class Builder {
        private List<TaxonomySuggestion> suggestions;
        private AiResponse aiResponse;
        private int maxSuggestions;
        private double minConfidence;

        public Builder suggestions(List<TaxonomySuggestion> suggestions) {
            this.suggestions = suggestions;
            return this;
        }

        public Builder aiResponse(AiResponse aiResponse) {
            this.aiResponse = aiResponse;
            return this;
        }

        public Builder maxSuggestions(int maxSuggestions) {
            this.maxSuggestions = maxSuggestions;
            return this;
        }

        public Builder minConfidence(double minConfidence) {
            this.minConfidence = minConfidence;
            return this;
        }

        public TaxonomyAnalysisResultImpl build() {
            if (suggestions == null) {
                throw new IllegalArgumentException("suggestions list is required");
            }
            if (aiResponse == null) {
                throw new IllegalArgumentException("aiResponse is required");
            }
            return new TaxonomyAnalysisResultImpl(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
