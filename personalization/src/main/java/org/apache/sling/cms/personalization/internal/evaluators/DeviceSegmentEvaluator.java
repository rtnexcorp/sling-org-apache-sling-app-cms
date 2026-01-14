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
 * Evaluates segments based on device type detection from User-Agent header.
 * <p>
 * This evaluator uses simple User-Agent string matching to detect device types.
 * For production use with complex detection requirements, consider using a dedicated
 * library like UADetector or WURFL.
 * </p>
 * <p>
 * Configuration properties:
 * </p>
 * <ul>
 *   <li><b>deviceType</b> (String, required) - Device type to match: "mobile", "tablet", or "desktop"</li>
 * </ul>
 * <p>
 * Detection logic:
 * </p>
 * <ul>
 *   <li><b>Tablet</b>: User-Agent contains "tablet" or "ipad"</li>
 *   <li><b>Mobile</b>: User-Agent contains "mobile" but not "tablet"</li>
 *   <li><b>Desktop</b>: Neither mobile nor tablet indicators present</li>
 * </ul>
 * <p>
 * Example segment configurations:
 * </p>
 */
@Component(service = SegmentEvaluator.class)
public class DeviceSegmentEvaluator implements SegmentEvaluator {

    private static final Logger log = LoggerFactory.getLogger(DeviceSegmentEvaluator.class);

    public static final String TYPE = "device";
    public static final String PROP_DEVICE_TYPE = "deviceType";

    public static final String DEVICE_TYPE_MOBILE = "mobile";
    public static final String DEVICE_TYPE_TABLET = "tablet";
    public static final String DEVICE_TYPE_DESKTOP = "desktop";

    @Override
    @NotNull
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean evaluate(@NotNull SlingHttpServletRequest request, @NotNull ValueMap rules) {
        String deviceType = rules.get(PROP_DEVICE_TYPE, String.class);

        if (StringUtils.isBlank(deviceType)) {
            log.warn("DeviceSegmentEvaluator: deviceType is not configured");
            return false;
        }

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            log.debug("DeviceSegmentEvaluator: User-Agent header is null");
            return false;
        }

        String lowerUserAgent = userAgent.toLowerCase();
        DeviceType detected = detectDeviceType(lowerUserAgent);

        boolean matches = matchesDeviceType(deviceType, detected);
        log.debug(
                "DeviceSegmentEvaluator: detected device type '{}', expected '{}', matches: {}",
                detected,
                deviceType,
                matches);

        return matches;
    }

    /**
     * Detects the device type from the User-Agent string.
     */
    private DeviceType detectDeviceType(String lowerUserAgent) {
        // Check for tablet first (more specific than mobile)
        if (containsTabletIndicators(lowerUserAgent)) {
            return DeviceType.TABLET;
        }

        // Check for mobile
        if (containsMobileIndicators(lowerUserAgent)) {
            return DeviceType.MOBILE;
        }

        // Default to desktop
        return DeviceType.DESKTOP;
    }

    /**
     * Checks if User-Agent contains tablet indicators.
     */
    private boolean containsTabletIndicators(String lowerUserAgent) {
        return lowerUserAgent.contains("tablet")
                || lowerUserAgent.contains("ipad")
                || (lowerUserAgent.contains("android") && !lowerUserAgent.contains("mobile"));
    }

    /**
     * Checks if User-Agent contains mobile indicators.
     */
    private boolean containsMobileIndicators(String lowerUserAgent) {
        return lowerUserAgent.contains("mobile")
                || lowerUserAgent.contains("iphone")
                || lowerUserAgent.contains("ipod")
                || lowerUserAgent.contains("blackberry")
                || lowerUserAgent.contains("windows phone");
    }

    /**
     * Matches the detected device type against the configured type.
     */
    private boolean matchesDeviceType(String configuredType, DeviceType detected) {
        switch (configuredType.toLowerCase()) {
            case DEVICE_TYPE_MOBILE:
                return detected == DeviceType.MOBILE;
            case DEVICE_TYPE_TABLET:
                return detected == DeviceType.TABLET;
            case DEVICE_TYPE_DESKTOP:
                return detected == DeviceType.DESKTOP;
            default:
                log.warn("DeviceSegmentEvaluator: unknown device type '{}'", configuredType);
                return false;
        }
    }

    /**
     * Internal enum for device types.
     */
    private enum DeviceType {
        MOBILE,
        TABLET,
        DESKTOP
    }
}
