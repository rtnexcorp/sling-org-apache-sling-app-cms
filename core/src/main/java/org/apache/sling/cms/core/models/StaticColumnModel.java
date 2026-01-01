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
package org.apache.sling.cms.core.models;

import javax.annotation.PostConstruct;

import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model for the Static Column component - displays a static i18n value
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class StaticColumnModel {

    private static final Logger log = LoggerFactory.getLogger(StaticColumnModel.class);

    @SlingObject
    private SlingHttpServletRequest request;

    @OSGiService(filter = "(component.name=org.apache.sling.i18n.impl.JcrResourceBundleProvider)")
    private ResourceBundleProvider resourceBundleProvider;

    private String value;
    private String messageKey;

    @PostConstruct
    protected void init() {
        try {
            // Get the column configuration from request attribute
            Object colConfig = request.getAttribute("colConfig");
            if (colConfig instanceof Resource) {
                Resource configResource = (Resource) colConfig;
                messageKey = configResource.getValueMap().get("value", String.class);

                if (messageKey != null && resourceBundleProvider != null) {
                    Locale locale = request.getLocale();
                    ResourceBundle bundle = resourceBundleProvider.getResourceBundle(locale);
                    if (bundle != null && bundle.containsKey(messageKey)) {
                        value = bundle.getString(messageKey);
                    } else {
                        value = messageKey;
                    }
                } else {
                    value = messageKey != null ? messageKey : "";
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get static column value", e);
            value = "";
        }
    }

    public String getValue() {
        return value != null ? value : "";
    }

    public String getMessageKey() {
        return messageKey != null ? messageKey : "";
    }
}
