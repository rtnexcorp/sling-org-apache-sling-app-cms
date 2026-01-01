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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.apache.sling.cms.core.insights.impl.BaseInsightProvider;
import org.apache.sling.cms.core.insights.impl.providers.HTMLValidatorInsightProvider.Config;
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
public class HTMLValidatorInsightProvider extends BaseInsightProvider {

    public static final String I18N_KEY_HTMLVALIDATOR_DANGER = "There were {0} validation errors and {1} warnings";
    public static final String I18N_KEY_HTMLVALIDATOR_WARN = "There were {0} validation warnings";
    public static final String I18N_KEY_HTMLVALIDATOR_SUCCESS = "HTML Validation successful!";

    @ObjectClassDefinition(
            name = "%htmlvalidator.config.name",
            description = "%htmlvalidator.config.description",
            localization = "OSGI-INF/l10n/bundle")
    public @interface Config {
        @AttributeDefinition(
                name = "%htmlvalidator.param.enabled.name",
                description = "%htmlvalidator.param.enabled.description")
        boolean enabled() default true;
    }

    @Reference
    private I18NProvider i18nProvider;

    private static final Logger log = LoggerFactory.getLogger(HTMLValidatorInsightProvider.class);

    private Config config;

    @Activate
    public void activate(Config config) {
        this.config = config;
    }

    @Override
    protected Insight doEvaluateRequest(InsightRequest request) throws Exception {
        Insight insight = new Insight(this, request);
        insight.setScored(true);
        PageInsightRequest pageRequest = (PageInsightRequest) request;

        String html = pageRequest.getPageHtml();

        log.debug("Starting HTML validation for page: {}", pageRequest.getPage().getPath());

        // Create HTTP client with timeouts
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // Build HTTP request with proper headers
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://validator.w3.org/nu/?out=json&showsource=no&level=all"))
                .header("Content-Type", "text/html; charset=utf-8")
                .header("User-Agent", "Apache-Sling-CMS/1.1 (HTML Validator)")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(html, StandardCharsets.UTF_8))
                .build();

        I18NDictionary dictionary =
                i18nProvider.getDictionary(request.getResource().getResourceResolver());

        try {
            // Send request and process response
            log.debug("Sending HTML validation request to W3C validator");
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            // Check response status
            if (response.statusCode() != 200) {
                log.error(
                        "W3C validator returned status code: {} with body: {}", response.statusCode(), response.body());
                throw new Exception("W3C validator returned status code: " + response.statusCode());
            }

            try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                JsonObject json = reader.readObject();
                log.debug("Loaded response: {}", json);
                JsonArray messages = json.getJsonArray("messages");
                int errors = 0;
                int warnings = 0;
                Set<String> msgSet = new HashSet<>();
                for (int i = 0; i < messages.size(); i++) {
                    JsonObject message = messages.getJsonObject(i);
                    if ("error".equals(message.getString("type"))) {
                        errors++;
                        String messageStr = message.getString("message");
                        if (!msgSet.contains(messageStr)) {
                            insight.addMessage(Message.danger(messageStr));
                            msgSet.add(messageStr);
                        }
                    } else if ("info".equals(message.getString("type"))
                            && message.containsKey("subtype")
                            && "warning".equals(message.getString("subtype"))) {
                        warnings++;
                        String messageStr = message.getString("message");
                        if (!msgSet.contains(messageStr)) {
                            insight.addMessage(Message.warn(messageStr));
                            msgSet.add(messageStr);
                        }
                    }
                }
                updateInsight(insight, pageRequest, dictionary, errors, warnings);
            } catch (Exception e) {
                log.error("Failed to parse W3C validator response: {}", response.body(), e);
                throw new Exception("Failed to parse W3C validator response: " + e.getMessage(), e);
            }
        } catch (java.net.http.HttpTimeoutException e) {
            log.error("HTML validation timed out after 30 seconds", e);
            throw new Exception("HTML validation timed out - W3C validator did not respond in time", e);
        } catch (java.net.ConnectException | java.net.UnknownHostException e) {
            log.error("Cannot connect to W3C validator at validator.w3.org", e);
            throw new Exception("Cannot connect to W3C validator - check network connectivity", e);
        } catch (Exception e) {
            log.error("Unexpected error during HTML validation", e);
            throw e;
        }

        return insight;
    }

    private void updateInsight(
            Insight insight, PageInsightRequest pageRequest, I18NDictionary dictionary, int errors, int warnings)
            throws UnsupportedEncodingException {
        double score;
        if (errors > 5) {
            insight.setPrimaryMessage(
                    Message.danger(dictionary.get(I18N_KEY_HTMLVALIDATOR_DANGER, new Object[] {errors, warnings})));
            score = 0.2;
        } else if (errors > 0) {
            insight.setPrimaryMessage(
                    Message.danger(dictionary.get(I18N_KEY_HTMLVALIDATOR_DANGER, new Object[] {errors, warnings})));
            score = 0.4;
        } else if (warnings > 5) {
            insight.setPrimaryMessage(
                    Message.warn(dictionary.get(I18N_KEY_HTMLVALIDATOR_WARN, new Object[] {warnings})));
            score = 0.6;
        } else if (warnings > 0) {
            insight.setPrimaryMessage(
                    Message.warn(dictionary.get(I18N_KEY_HTMLVALIDATOR_WARN, new Object[] {warnings})));
            score = 0.8;
        } else {
            insight.setPrimaryMessage(Message.success(dictionary.get(I18N_KEY_HTMLVALIDATOR_SUCCESS)));
            score = 1.0;
        }
        insight.setScore(score);
        insight.setMoreDetailsLink("https://validator.w3.org/nu/?doc="
                + URLEncoder.encode(pageRequest.getPage().getPublishedUrl(), StandardCharsets.UTF_8.toString()));
    }

    @Override
    public String getId() {
        return "htmlvalidator";
    }

    @Override
    public String getTitle() {
        return "HTML Validator";
    }

    @Override
    public boolean isEnabled(InsightRequest request) {
        if (!config.enabled()) {
            log.debug("HTML Validator is not enabled");
            return false;
        }
        if (request.getType() != InsightRequest.TYPE.PAGE) {
            log.debug("Request {} is not a page", request);
            return false;
        }
        return true;
    }
}
