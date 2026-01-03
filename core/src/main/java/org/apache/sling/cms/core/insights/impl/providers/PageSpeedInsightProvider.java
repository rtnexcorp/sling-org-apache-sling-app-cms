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
package org.apache.sling.cms.core.insights.impl.providers;

import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.apache.sling.cms.core.insights.impl.BaseInsightProvider;
import org.apache.sling.cms.core.insights.impl.providers.PageSpeedInsightProvider.Config;
import org.apache.sling.cms.i18n.I18NDictionary;
import org.apache.sling.cms.i18n.I18NProvider;
import org.apache.sling.cms.insights.Insight;
import org.apache.sling.cms.insights.InsightProvider;
import org.apache.sling.cms.insights.InsightRequest;
import org.apache.sling.cms.insights.Message;
import org.apache.sling.cms.insights.PageInsightRequest;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = InsightProvider.class, immediate = true)
@Designate(ocd = Config.class)
public class PageSpeedInsightProvider extends BaseInsightProvider {

    @ObjectClassDefinition(
            name = "%pagespeed.config.name",
            description = "%pagespeed.config.description",
            localization = "OSGI-INF/l10n/bundle")
    public @interface Config {
        @AttributeDefinition(
                name = "%pagespeed.param.enabled.name",
                description = "%pagespeed.param.enabled.description")
        boolean enabled();

        @AttributeDefinition(name = "%pagespeed.param.apikey.name", description = "%pagespeed.param.apikey.description")
        String apikey();
    }

    @Reference
    private I18NProvider i18nProvider;

    public static final String MESSAGE_RESULT_DANGER =
            "Poor performance - Core Web Vitals need improvement (Score: {0})";
    public static final String MESSAGE_RESULT_WARN = "Fair performance - Some optimization recommended (Score: {0})";
    public static final String MESSAGE_RESULT_SUCCESS = "Good performance - Core Web Vitals passed (Score: {0})";
    private static final String REQUEST_FORMAT =
            "https://www.googleapis.com/pagespeedonline/v5/runPagespeed?url=%s&category=PERFORMANCE&strategy=%s&key=%s";
    private static final String PAGESPEED_FORMAT = "https://pagespeed.web.dev/analysis?url=%s";
    private static final Logger log = LoggerFactory.getLogger(PageSpeedInsightProvider.class);

    private Config config;

    @Activate
    public void activate(Config config) {
        this.config = config;
    }

    @Override
    protected Insight doEvaluateRequest(InsightRequest request) throws Exception {
        Insight insight = new Insight(this, request);
        PageInsightRequest pageRequest = (PageInsightRequest) request;
        String publishedUrl = pageRequest.getPage().getPublishedUrl();

        // Use MOBILE strategy by default (can be made configurable)
        String strategy = "MOBILE";
        String checkUrl = String.format(
                REQUEST_FORMAT, URLEncoder.encode(publishedUrl, StandardCharsets.UTF_8), strategy, config.apikey());

        log.debug("Starting PageSpeed v5 analysis for URL: {} with strategy: {}", publishedUrl, strategy);

        // Create HTTP client with timeouts
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // Build HTTP request
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(checkUrl))
                .header("User-Agent", "Apache-Sling-CMS/1.1 (PageSpeed Insights v5)")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        I18NDictionary dictionary =
                i18nProvider.getDictionary(request.getResource().getResourceResolver());

        log.debug("Requesting page speed via: {}", checkUrl);

        try {
            // Send request and process response
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            // Check response status
            if (response.statusCode() != 200) {
                log.error(
                        "PageSpeed API returned status code: {} with body: {}", response.statusCode(), response.body());
                throw new Exception("PageSpeed API returned status code: " + response.statusCode());
            }

            try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                JsonObject resp = reader.readObject();

                log.debug("Retrieved response from PageSpeed v5 API");

                insight.setScored(true);

                // PageSpeed v5 API returns score in lighthouseResult.categories.performance.score (0-1 scale)
                JsonObject lighthouseResult = resp.getJsonObject("lighthouseResult");
                JsonObject categories = lighthouseResult.getJsonObject("categories");
                JsonObject performance = categories.getJsonObject("performance");
                double score = performance.getJsonNumber("score").doubleValue();

                insight.setScore(score);
                log.debug("Parsed PageSpeed v5 performance score: {}", score);

                // Format score as percentage for display
                int scorePercent = (int) (score * 100);
                Object[] scoreArgs = new Object[] {scorePercent};

                // Score ranges for v5: 0-49 (poor), 50-89 (needs improvement), 90-100 (good)
                if (score < 0.5) {
                    insight.setPrimaryMessage(Message.danger(dictionary.get(MESSAGE_RESULT_DANGER, scoreArgs)));
                } else if (score < 0.9) {
                    insight.setPrimaryMessage(Message.warn(dictionary.get(MESSAGE_RESULT_WARN, scoreArgs)));
                } else {
                    insight.setPrimaryMessage(Message.success(dictionary.get(MESSAGE_RESULT_SUCCESS, scoreArgs)));
                }

                // Add Core Web Vitals details if available
                if (lighthouseResult.containsKey("audits")) {
                    JsonObject audits = lighthouseResult.getJsonObject("audits");
                    addCoreWebVitals(insight, audits);
                }

                insight.setMoreDetailsLink(
                        String.format(PAGESPEED_FORMAT, URLEncoder.encode(publishedUrl, StandardCharsets.UTF_8)));

                log.debug("PageSpeed v5 response parsed successfully");
            } catch (Exception e) {
                log.error("Failed to parse PageSpeed v5 API response: {}", response.body(), e);
                throw new Exception("Failed to parse PageSpeed v5 API response: " + e.getMessage(), e);
            }
        } catch (java.net.http.HttpTimeoutException e) {
            log.error("PageSpeed analysis timed out after 30 seconds", e);
            throw new Exception("PageSpeed analysis timed out - API did not respond in time", e);
        } catch (java.net.ConnectException | java.net.UnknownHostException e) {
            log.error("Cannot connect to PageSpeed API at googleapis.com", e);
            throw new Exception("Cannot connect to PageSpeed API - check network connectivity and API key", e);
        } catch (Exception e) {
            log.error("Unexpected error during PageSpeed analysis", e);
            throw e;
        }

        return insight;
    }

    /**
     * Add Core Web Vitals metrics to insight details
     */
    private void addCoreWebVitals(Insight insight, JsonObject audits) {
        try {
            // Largest Contentful Paint (LCP)
            if (audits.containsKey("largest-contentful-paint")) {
                JsonObject lcp = audits.getJsonObject("largest-contentful-paint");
                double lcpValue = lcp.getJsonNumber("numericValue").doubleValue() / 1000; // Convert to seconds
                String lcpScore = lcp.getString("score", "0");
                insight.addMessage(
                        Message.defaultMsg(String.format("LCP: %.2fs %s", lcpValue, getScoreEmoji(lcpScore))));
            }

            // First Input Delay (FID) - now Interaction to Next Paint (INP) in newer versions
            if (audits.containsKey("interaction-to-next-paint")) {
                JsonObject inp = audits.getJsonObject("interaction-to-next-paint");
                double inpValue = inp.getJsonNumber("numericValue").doubleValue();
                String inpScore = inp.getString("score", "0");
                insight.addMessage(
                        Message.defaultMsg(String.format("INP: %.0fms %s", inpValue, getScoreEmoji(inpScore))));
            }

            // Cumulative Layout Shift (CLS)
            if (audits.containsKey("cumulative-layout-shift")) {
                JsonObject cls = audits.getJsonObject("cumulative-layout-shift");
                double clsValue = cls.getJsonNumber("numericValue").doubleValue();
                String clsScore = cls.getString("score", "0");
                insight.addMessage(
                        Message.defaultMsg(String.format("CLS: %.3f %s", clsValue, getScoreEmoji(clsScore))));
            }

            // First Contentful Paint (FCP)
            if (audits.containsKey("first-contentful-paint")) {
                JsonObject fcp = audits.getJsonObject("first-contentful-paint");
                double fcpValue = fcp.getJsonNumber("numericValue").doubleValue() / 1000;
                String fcpScore = fcp.getString("score", "0");
                insight.addMessage(
                        Message.defaultMsg(String.format("FCP: %.2fs %s", fcpValue, getScoreEmoji(fcpScore))));
            }

            // Total Blocking Time (TBT)
            if (audits.containsKey("total-blocking-time")) {
                JsonObject tbt = audits.getJsonObject("total-blocking-time");
                double tbtValue = tbt.getJsonNumber("numericValue").doubleValue();
                String tbtScore = tbt.getString("score", "0");
                insight.addMessage(
                        Message.defaultMsg(String.format("TBT: %.0fms %s", tbtValue, getScoreEmoji(tbtScore))));
            }
        } catch (Exception e) {
            log.warn("Failed to extract Core Web Vitals details", e);
        }
    }

    /**
     * Convert numeric score to emoji indicator
     */
    private String getScoreEmoji(String score) {
        try {
            double scoreValue = Double.parseDouble(score);
            if (scoreValue >= 0.9) {
                return "✓"; // Good
            } else if (scoreValue >= 0.5) {
                return "!"; // Needs improvement
            } else {
                return "✗"; // Poor
            }
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String getId() {
        return "pagespeed";
    }

    @Override
    public String getTitle() {
        return "Page Speed";
    }

    @Override
    public boolean isEnabled(InsightRequest request) {
        if (!config.enabled()) {
            log.debug("Page Speed is not enabled");
            return false;
        }
        if (request.getType() != InsightRequest.TYPE.PAGE) {
            log.debug("Request {} is not a page", request);
            return false;
        }
        if (!((PageInsightRequest) request).getPage().isPublished()) {
            log.debug("The page for {} is not published", request);
            return false;
        }
        return true;
    }
}
