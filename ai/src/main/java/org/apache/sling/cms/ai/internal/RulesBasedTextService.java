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

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.cms.ai.AiRequest;
import org.apache.sling.cms.ai.AiResponse;
import org.apache.sling.cms.ai.AiTextService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rules-based fallback implementation of {@link AiTextService}.
 * <p>
 * This implementation provides basic text processing without requiring
 * an external AI API. It uses simple rules like extracting the first
 * N sentences for summaries.
 * </p>
 * <p>
 * This service is useful as a fallback when AI is disabled or unavailable.
 * </p>
 */
@Component(
        service = AiTextService.class,
        configurationPolicy = ConfigurationPolicy.OPTIONAL,
        property = {"service.ranking:Integer=0"})
@Designate(ocd = RulesBasedTextService.Config.class)
public class RulesBasedTextService implements AiTextService {

    private static final Logger LOG = LoggerFactory.getLogger(RulesBasedTextService.class);
    private static final String PROVIDER_ID = "rules-based";
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("(?<=[.!?])\\s+");

    @ObjectClassDefinition(
            name = "Apache Sling CMS - Rules-Based Text Service",
            description = "Fallback text service using simple rules instead of AI")
    public @interface Config {

        @AttributeDefinition(name = "Enabled", description = "Enable this fallback service")
        boolean enabled() default true;

        @AttributeDefinition(
                name = "Summary Sentence Count",
                description = "Number of sentences to include in summaries")
        int summarySentenceCount() default 3;

        @AttributeDefinition(name = "Title Max Length", description = "Maximum length for generated titles")
        int titleMaxLength() default 60;

        @AttributeDefinition(name = "Meta Description Max Length", description = "Maximum length for meta descriptions")
        int metaDescriptionMaxLength() default 160;
    }

    private boolean enabled;
    private int summarySentenceCount;
    private int titleMaxLength;
    private int metaDescriptionMaxLength;

    @Activate
    protected void activate(Config config) {
        this.enabled = config.enabled();
        this.summarySentenceCount = config.summarySentenceCount();
        this.titleMaxLength = config.titleMaxLength();
        this.metaDescriptionMaxLength = config.metaDescriptionMaxLength();
        LOG.info("Rules-based text service activated, enabled: {}", enabled);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getTitle() {
        return "Rules-Based Text Service (Fallback)";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean requiresExternalApi() {
        return false;
    }

    @Override
    public AiResponse summarize(AiRequest request) {
        return summarize(request, -1);
    }

    @Override
    public AiResponse summarize(AiRequest request, int maxLength) {
        if (!enabled) {
            return AiResponse.skipped("Rules-based text service is disabled");
        }

        long startTime = System.currentTimeMillis();
        String content = request.getContent();

        if (StringUtils.isBlank(content)) {
            return AiResponse.failure("No content provided for summarization", PROVIDER_ID);
        }

        try {
            // Extract first N sentences
            String[] sentences = SENTENCE_PATTERN.split(content);
            int count = Math.min(summarySentenceCount, sentences.length);
            String summary = Arrays.stream(sentences).limit(count).collect(Collectors.joining(" "));

            // Apply max length if specified
            if (maxLength > 0 && summary.length() > maxLength) {
                summary = StringUtils.abbreviate(summary, maxLength);
            }

            long processingTime = System.currentTimeMillis() - startTime;

            return AiResponse.builder()
                    .status(AiResponse.Status.SUCCESS)
                    .content(summary.trim())
                    .providerId(PROVIDER_ID)
                    .processingTimeMs(processingTime)
                    .build();

        } catch (Exception e) {
            LOG.error("Error generating summary", e);
            return AiResponse.failure("Error generating summary: " + e.getMessage(), PROVIDER_ID);
        }
    }

    @Override
    public AiResponse suggestTitle(AiRequest request) {
        if (!enabled) {
            return AiResponse.skipped("Rules-based text service is disabled");
        }

        long startTime = System.currentTimeMillis();
        String content = request.getContent();

        if (StringUtils.isBlank(content)) {
            return AiResponse.failure("No content provided for title suggestion", PROVIDER_ID);
        }

        try {
            // Use first sentence or first N characters as title
            String[] sentences = SENTENCE_PATTERN.split(content);
            String title = sentences.length > 0 ? sentences[0] : content;
            title = StringUtils.abbreviate(title.trim(), titleMaxLength);

            long processingTime = System.currentTimeMillis() - startTime;

            return AiResponse.builder()
                    .status(AiResponse.Status.SUCCESS)
                    .content(title)
                    .providerId(PROVIDER_ID)
                    .processingTimeMs(processingTime)
                    .build();

        } catch (Exception e) {
            LOG.error("Error suggesting title", e);
            return AiResponse.failure("Error suggesting title: " + e.getMessage(), PROVIDER_ID);
        }
    }

    @Override
    public AiResponse suggestMetaDescription(AiRequest request) {
        if (!enabled) {
            return AiResponse.skipped("Rules-based text service is disabled");
        }

        // Use summarize with meta description max length
        AiResponse summaryResponse = summarize(request, metaDescriptionMaxLength);

        if (summaryResponse.isSuccess()) {
            return AiResponse.builder()
                    .status(AiResponse.Status.SUCCESS)
                    .content(summaryResponse.getContent())
                    .providerId(PROVIDER_ID)
                    .processingTimeMs(summaryResponse.getProcessingTimeMs())
                    .build();
        }

        return summaryResponse;
    }

    @Override
    public AiResponse rewrite(AiRequest request, Tone tone) {
        // Rules-based implementation cannot truly rewrite in different tones
        return AiResponse.builder()
                .status(AiResponse.Status.SKIPPED)
                .errorMessage(
                        "Tone rewriting requires an AI provider. Rules-based fallback cannot perform this operation.")
                .providerId(PROVIDER_ID)
                .build();
    }

    @Override
    public AiResponse translate(AiRequest request, String targetLocale) {
        // Rules-based implementation cannot translate
        return AiResponse.builder()
                .status(AiResponse.Status.SKIPPED)
                .errorMessage(
                        "Translation requires an AI provider. Rules-based fallback cannot perform this operation.")
                .providerId(PROVIDER_ID)
                .build();
    }

    @Override
    public AiResponse explain(AiRequest request) {
        // Use summarize as a basic explanation
        return summarize(request);
    }

    @Override
    public AiResponse summarizeChanges(String originalContent, String updatedContent) {
        if (!enabled) {
            return AiResponse.skipped("Rules-based text service is disabled");
        }

        // Simple change detection - just report if content changed
        if (java.util.Objects.equals(originalContent, updatedContent)) {
            return AiResponse.builder()
                    .status(AiResponse.Status.SUCCESS)
                    .content("No changes detected.")
                    .providerId(PROVIDER_ID)
                    .build();
        }

        int originalLength = StringUtils.length(originalContent);
        int updatedLength = StringUtils.length(updatedContent);
        int diff = updatedLength - originalLength;

        String summary = String.format(
                "Content was modified. Length changed from %d to %d characters (%s%d).",
                originalLength, updatedLength, diff >= 0 ? "+" : "", diff);

        return AiResponse.builder()
                .status(AiResponse.Status.SUCCESS)
                .content(summary)
                .providerId(PROVIDER_ID)
                .build();
    }
}
