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
package org.apache.sling.cms.core.beans;

import java.util.Locale;

import org.apache.sling.api.resource.Resource;

/**
 * Information about a language folder in i18n dictionary.
 */
public class LanguageInfo {
    private final Resource resource;
    private final String languageCode;

    public LanguageInfo(Resource resource) {
        this.resource = resource;
        this.languageCode = resource.getValueMap().get("jcr:language", String.class);
    }

    public String getName() {
        return resource.getName();
    }

    public String getPath() {
        return resource.getPath();
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getDisplayLanguage() {
        try {
            Locale locale = Locale.forLanguageTag(languageCode);
            return locale.getDisplayLanguage();
        } catch (Exception e) {
            return languageCode;
        }
    }

    public String getDisplayCountry() {
        try {
            Locale locale = Locale.forLanguageTag(languageCode);
            return locale.getDisplayCountry();
        } catch (Exception e) {
            return "";
        }
    }

    public String getDisplayName() {
        String country = getDisplayCountry();
        if (country != null && !country.isEmpty()) {
            return getDisplayLanguage() + " " + country;
        }
        return getDisplayLanguage();
    }
}
