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
package org.apache.sling.cms.ai;

import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ProviderType;

/**
 * AI service for content classification and taxonomy suggestions.
 * <p>
 * This service provides AI-powered capabilities for:
 * </p>
 * <ul>
 *   <li>Suggesting relevant taxonomy tags for content based on existing taxonomy structure</li>
 *   <li>Categorizing content into predefined categories</li>
 *   <li>Analyzing content to determine appropriate classification</li>
 *   <li>Providing confidence scores for taxonomy suggestions</li>
 * </ul>
 * <p>
 * The service is taxonomy-aware and maps suggestions to existing taxonomy items
 * in the CMS (/etc/taxonomy), ensuring consistency with the site's information architecture.
 * </p>
 */
@ProviderType
public interface AiTaxonomyService extends AiService {

    /**
     * Represents a taxonomy suggestion for content.
     */
    interface TaxonomySuggestion {
        /**
         * @return the path of the suggested taxonomy item (e.g., "/etc/taxonomy/topics/java")
         */
        String getTaxonomyPath();

        /**
         * @return the title of the suggested taxonomy item
         */
        String getTaxonomyTitle();

        /**
         * @return confidence score for this suggestion (0.0 to 1.0)
         */
        double getConfidence();

        /**
         * @return brief explanation of why this taxonomy was suggested
         */
        String getReason();
    }

    /**
     * Represents the result of taxonomy analysis.
     */
    interface TaxonomyAnalysisResult {
        /**
         * @return list of suggested taxonomy tags, ordered by confidence (highest first)
         */
        List<TaxonomySuggestion> getSuggestions();

        /**
         * @return the original AI response containing raw analysis
         */
        AiResponse getAiResponse();

        /**
         * @return maximum number of suggestions requested
         */
        int getMaxSuggestions();

        /**
         * @return minimum confidence threshold used for filtering
         */
        double getMinConfidence();
    }

    /**
     * Suggests relevant taxonomy tags for the provided content.
     * <p>
     * Analyzes the content and matches it against the existing taxonomy structure
     * to suggest appropriate tags. Only suggests tags that exist in the CMS.
     * </p>
     *
     * @param content        the content text to analyze
     * @param resolver       the resource resolver for accessing taxonomy structure
     * @param taxonomyPath   the root path of the taxonomy to use (e.g., "/etc/taxonomy")
     * @param maxSuggestions maximum number of suggestions to return
     * @param minConfidence  minimum confidence threshold (0.0 to 1.0) for filtering suggestions
     * @return taxonomy analysis result with ordered suggestions
     */
    TaxonomyAnalysisResult suggestTags(
            String content, ResourceResolver resolver, String taxonomyPath, int maxSuggestions, double minConfidence);

    /**
     * Suggests taxonomy tags with default parameters (max 5 suggestions, 0.5 confidence threshold).
     *
     * @param content      the content text to analyze
     * @param resolver     the resource resolver for accessing taxonomy structure
     * @param taxonomyPath the root path of the taxonomy to use
     * @return taxonomy analysis result with suggestions
     */
    default TaxonomyAnalysisResult suggestTags(String content, ResourceResolver resolver, String taxonomyPath) {
        return suggestTags(content, resolver, taxonomyPath, 5, 0.5);
    }

    /**
     * Suggests categories for content based on a specific taxonomy branch.
     * <p>
     * Similar to suggestTags, but allows targeting a specific taxonomy subtree
     * for more focused categorization (e.g., only product categories).
     * </p>
     *
     * @param content        the content text to categorize
     * @param resolver       the resource resolver for accessing taxonomy structure
     * @param categoryPath   the path to the category taxonomy branch
     * @param maxSuggestions maximum number of category suggestions to return
     * @param minConfidence  minimum confidence threshold for filtering
     * @return taxonomy analysis result with category suggestions
     */
    TaxonomyAnalysisResult suggestCategories(
            String content, ResourceResolver resolver, String categoryPath, int maxSuggestions, double minConfidence);

    /**
     * Suggests categories with default parameters (max 3 categories, 0.6 confidence threshold).
     *
     * @param content      the content text to categorize
     * @param resolver     the resource resolver for accessing taxonomy structure
     * @param categoryPath the path to the category taxonomy branch
     * @return taxonomy analysis result with category suggestions
     */
    default TaxonomyAnalysisResult suggestCategories(String content, ResourceResolver resolver, String categoryPath) {
        return suggestCategories(content, resolver, categoryPath, 3, 0.6);
    }
}
