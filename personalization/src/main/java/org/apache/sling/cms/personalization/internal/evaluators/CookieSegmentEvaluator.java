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
package org.apache.sling.cms.personalization.internal.evaluators;

import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.personalization.SegmentEvaluator;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates segments based on cookie presence and values.
 * <p>
 * Configuration properties:
 * </p>
 * <ul>
 *   <li><b>cookieName</b> (String, required) - Name of the cookie to check</li>
 *   <li><b>cookieValue</b> (String, optional) - Expected cookie value. If not specified,
 *       only checks for cookie presence</li>
 *   <li><b>matchPattern</b> (String, optional) - Regular expression pattern to match
 *       against cookie value (alternative to exact cookieValue match)</li>
 * </ul>
 * <p>
 * Example segment configurations:
 * </p>
 */
@Component(service = SegmentEvaluator.class)
public class CookieSegmentEvaluator implements SegmentEvaluator {

    private static final Logger log = LoggerFactory.getLogger(CookieSegmentEvaluator.class);

    public static final String TYPE = "cookie";
    public static final String PROP_COOKIE_NAME = "cookieName";
    public static final String PROP_COOKIE_VALUE = "cookieValue";
    public static final String PROP_MATCH_PATTERN = "matchPattern";

    @Override
    @NotNull
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean evaluate(@NotNull SlingHttpServletRequest request, @NotNull ValueMap rules) {
        String cookieName = rules.get(PROP_COOKIE_NAME, String.class);

        if (StringUtils.isBlank(cookieName)) {
            log.warn("CookieSegmentEvaluator: cookieName is not configured");
            return false;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            log.debug("CookieSegmentEvaluator: no cookies found in request");
            return false;
        }

        // Find the cookie
        Cookie targetCookie = null;
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                targetCookie = cookie;
                break;
            }
        }

        if (targetCookie == null) {
            log.debug("CookieSegmentEvaluator: cookie '{}' not found", cookieName);
            return false;
        }

        String cookieValue = targetCookie.getValue();
        String expectedValue = rules.get(PROP_COOKIE_VALUE, String.class);
        String matchPattern = rules.get(PROP_MATCH_PATTERN, String.class);

        // If no value or pattern specified, just check presence
        if (expectedValue == null && matchPattern == null) {
            log.debug("CookieSegmentEvaluator: cookie '{}' found (presence check)", cookieName);
            return true;
        }

        // Check exact value match
        if (expectedValue != null) {
            boolean matches = expectedValue.equals(cookieValue);
            log.debug(
                    "CookieSegmentEvaluator: cookie '{}' value '{}' matches expected '{}': {}",
                    cookieName,
                    cookieValue,
                    expectedValue,
                    matches);
            return matches;
        }

        // Check pattern match
        if (matchPattern != null) {
            try {
                boolean matches = cookieValue != null && cookieValue.matches(matchPattern);
                log.debug(
                        "CookieSegmentEvaluator: cookie '{}' value '{}' matches pattern '{}': {}",
                        cookieName,
                        cookieValue,
                        matchPattern,
                        matches);
                return matches;
            } catch (Exception e) {
                log.error("CookieSegmentEvaluator: error evaluating pattern '{}'", matchPattern, e);
                return false;
            }
        }

        return false;
    }
}
