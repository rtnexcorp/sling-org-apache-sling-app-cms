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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.personalization.SegmentEvaluator;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates segments based on request path patterns.
 * <p>
 * Configuration properties:
 * </p>
 * <ul>
 *   <li><b>pathPattern</b> (String) - Regular expression pattern to match against request path</li>
 * </ul>
 */
@Component(service = SegmentEvaluator.class)
public class PathSegmentEvaluator implements SegmentEvaluator {

    private static final Logger log = LoggerFactory.getLogger(PathSegmentEvaluator.class);

    public static final String TYPE = "path";
    public static final String PROP_PATH_PATTERN = "pathPattern";

    @Override
    @NotNull
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean evaluate(@NotNull SlingHttpServletRequest request, @NotNull ValueMap rules) {
        String pathPattern = rules.get(PROP_PATH_PATTERN, String.class);

        if (StringUtils.isBlank(pathPattern)) {
            log.warn("PathSegmentEvaluator: pathPattern is not configured");
            return false;
        }

        String requestPath = request.getPathInfo();
        if (requestPath == null) {
            requestPath = request.getRequestURI();
        }

        if (requestPath == null) {
            log.debug("PathSegmentEvaluator: request path is null");
            return false;
        }

        try {
            boolean matches = requestPath.matches(pathPattern);
            log.debug("PathSegmentEvaluator: path '{}' matches pattern '{}': {}", requestPath, pathPattern, matches);
            return matches;
        } catch (Exception e) {
            log.error("PathSegmentEvaluator: error evaluating pattern '{}'", pathPattern, e);
            return false;
        }
    }
}
