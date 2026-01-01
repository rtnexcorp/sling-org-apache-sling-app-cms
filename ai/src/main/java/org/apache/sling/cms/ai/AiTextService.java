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

import org.osgi.annotation.versioning.ProviderType;

/**
 * AI service for text generation and manipulation.
 * <p>
 * This service provides capabilities for:
 * </p>
 * <ul>
 *   <li>Summarizing content (short/long summaries)</li>
 *   <li>Generating titles and meta descriptions</li>
 *   <li>Rewriting content in different tones</li>
 *   <li>Translating content to different locales</li>
 * </ul>
 * <p>
 * Implementations can be backed by external AI APIs (OpenAI, Azure OpenAI,
 * Anthropic, etc.) or rule-based fallback logic.
 * </p>
 */
@ProviderType
public interface AiTextService extends AiService {

    /**
     * Tone options for content rewriting.
     */
    enum Tone {
        /** Professional, business-appropriate tone */
        FORMAL,
        /** Casual, conversational tone */
        INFORMAL,
        /** Brief and to the point */
        CONCISE,
        /** More detailed and explanatory */
        DETAILED,
        /** Friendly and approachable */
        FRIENDLY,
        /** Technical and precise */
        TECHNICAL
    }

    /**
     * Generates a summary of the provided content.
     *
     * @param request the AI request containing the content to summarize
     * @return the AI response with the generated summary
     */
    AiResponse summarize(AiRequest request);

    /**
     * Generates a summary with a specified maximum length.
     *
     * @param request   the AI request containing the content to summarize
     * @param maxLength the maximum length of the summary in characters
     * @return the AI response with the generated summary
     */
    AiResponse summarize(AiRequest request, int maxLength);

    /**
     * Generates a title suggestion for the provided content.
     *
     * @param request the AI request containing the content
     * @return the AI response with the suggested title
     */
    AiResponse suggestTitle(AiRequest request);

    /**
     * Generates a meta description for the provided content.
     *
     * @param request the AI request containing the content
     * @return the AI response with the suggested meta description
     */
    AiResponse suggestMetaDescription(AiRequest request);

    /**
     * Rewrites the content in a specified tone.
     *
     * @param request the AI request containing the content to rewrite
     * @param tone    the target tone for the rewritten content
     * @return the AI response with the rewritten content
     */
    AiResponse rewrite(AiRequest request, Tone tone);

    /**
     * Translates the content to a target locale.
     *
     * @param request      the AI request containing the content to translate
     * @param targetLocale the target locale code (e.g., "fr", "de", "es")
     * @return the AI response with the translated content
     */
    AiResponse translate(AiRequest request, String targetLocale);

    /**
     * Explains a piece of content for reviewers or editors.
     * Useful for workflow assistance.
     *
     * @param request the AI request containing the content to explain
     * @return the AI response with an explanation of the content
     */
    AiResponse explain(AiRequest request);

    /**
     * Generates a summary of changes between two versions of content.
     *
     * @param originalContent the original content
     * @param updatedContent  the updated content
     * @return the AI response with a summary of changes
     */
    AiResponse summarizeChanges(String originalContent, String updatedContent);
}
