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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.core.beans.KeyRow;
import org.apache.sling.cms.core.beans.LanguageFolder;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for i18n container component
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class I18nContainerModel {

    @SlingObject
    private SlingHttpServletRequest request;

    private List<LanguageFolder> languages;
    private List<String> keys;
    private Resource firstLanguage;
    private List<KeyRow> keyRows;

    /**
     * Get the request suffix (path after .html extension)
     * @return the suffix or empty string if none
     */
    public String getRequestSuffix() {
        String suffix = request.getRequestPathInfo().getSuffix();
        return suffix != null ? suffix : "";
    }

    public List<LanguageFolder> getLanguages() {
        if (languages == null) {
            languages = new ArrayList<>();
            Resource suffixResource = request.getRequestPathInfo().getSuffixResource();
            if (suffixResource != null) {
                languages = StreamSupport.stream(suffixResource.getChildren().spliterator(), false)
                        .filter(lang -> lang.getValueMap().get("jcr:language", String.class) != null)
                        .map(LanguageFolder::new)
                        .collect(Collectors.toList());

                // Set first language for "Add Entry" link
                if (!languages.isEmpty()) {
                    firstLanguage = languages.get(0).getResource();
                }
            }
        }
        return languages;
    }

    public List<String> getKeys() {
        if (keys == null) {
            Resource suffixResource = request.getRequestPathInfo().getSuffixResource();
            if (suffixResource != null) {
                I18nHelper helper = suffixResource.adaptTo(I18nHelper.class);
                if (helper != null) {
                    keys = new ArrayList<>(helper.getKeys());
                } else {
                    keys = new ArrayList<>();
                }
            } else {
                keys = new ArrayList<>();
            }
        }
        return keys;
    }

    /**
     * Get rows of translation keys with their entries for each language.
     * This is HTL-friendly as it avoids method calls with parameters.
     */
    public List<KeyRow> getKeyRows() {
        if (keyRows == null) {
            keyRows = new ArrayList<>();
            List<String> allKeys = getKeys();
            List<LanguageFolder> allLanguages = getLanguages();

            for (String key : allKeys) {
                keyRows.add(new KeyRow(key, allLanguages));
            }
        }
        return keyRows;
    }

    public String getFirstLanguagePath() {
        getLanguages(); // ensure languages are loaded
        return firstLanguage != null ? firstLanguage.getPath() : "";
    }

    public String getSuffixPath() {
        Resource suffixResource = request.getRequestPathInfo().getSuffixResource();
        return suffixResource != null ? suffixResource.getPath() : "";
    }
}
