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

import org.osgi.annotation.versioning.ProviderType;

/**
 * AI service for content classification and taxonomy suggestions.
 * <p>
 * This service provides capabilities for:
 * </p>
 * <ul>
 *   <li>Suggesting tags from existing taxonomy</li>
 *   <li>Suggesting categories for content</li>
 *   <li>Content classification and categorization</li>
 * </ul>
 * <p>
 * Implementations should integrate with the CMS's existing
 * {@link org.apache.sling.cms.TaxonomyService} to ensure suggestions
 * are mapped to valid taxonomy items.
 * </p>
 */
@ProviderType
public interface AiClassificationService extends AiService {

    /**
     * Represents a suggested taxonomy item with confidence score.
     */
    interface TaxonomySuggestion {
        /**
         * Gets the path to the taxonomy item.
         *
         * @return the taxonomy path (e.g., "/etc/taxonomy/topics/technology")
         */
        String getTaxonomyPath();

        /**
         * Gets the title of the taxonomy item.
         *
         * @return the taxonomy title
         */
        String getTitle();

        /**
         * Gets the confidence score for this suggestion.
         *
         * @return confidence score between 0.0 and 1.0
         */
        double getConfidence();
    }

    /**
     * Represents a category suggestion with confidence score.
     */
    interface CategorySuggestion {
        /**
         * Gets the category identifier or path.
         *
         * @return the category ID or path
         */
        String getCategoryId();

        /**
         * Gets the display name of the category.
         *
         * @return the category name
         */
        String getName();

        /**
         * Gets the confidence score for this suggestion.
         *
         * @return confidence score between 0.0 and 1.0
         */
        double getConfidence();
    }

    /**
     * Suggests taxonomy tags for the provided content.
     * <p>
     * The suggestions are mapped to existing taxonomy items in the CMS.
     * </p>
     *
     * @param request      the AI request containing the content to analyze
     * @param taxonomyRoot the root path of the taxonomy tree to search
     *                     (e.g., "/etc/taxonomy")
     * @param maxResults   the maximum number of suggestions to return
     * @return a list of taxonomy suggestions sorted by confidence
     */
    List<TaxonomySuggestion> suggestTags(AiRequest request, String taxonomyRoot, int maxResults);

    /**
     * Suggests taxonomy tags using the default taxonomy root.
     *
     * @param request    the AI request containing the content to analyze
     * @param maxResults the maximum number of suggestions to return
     * @return a list of taxonomy suggestions sorted by confidence
     */
    List<TaxonomySuggestion> suggestTags(AiRequest request, int maxResults);

    /**
     * Suggests categories for the provided content.
     *
     * @param request    the AI request containing the content to analyze
     * @param maxResults the maximum number of suggestions to return
     * @return a list of category suggestions sorted by confidence
     */
    List<CategorySuggestion> suggestCategories(AiRequest request, int maxResults);

    /**
     * Classifies content into predefined categories.
     *
     * @param request    the AI request containing the content to classify
     * @param categories the list of available category IDs to choose from
     * @return a list of matching categories with confidence scores
     */
    List<CategorySuggestion> classify(AiRequest request, List<String> categories);

    /**
     * Extracts keywords from the content.
     *
     * @param request    the AI request containing the content to analyze
     * @param maxResults the maximum number of keywords to extract
     * @return a list of extracted keywords
     */
    List<String> extractKeywords(AiRequest request, int maxResults);
}
