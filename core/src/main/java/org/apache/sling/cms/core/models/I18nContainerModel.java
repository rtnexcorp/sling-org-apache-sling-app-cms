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
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
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

    /**
     * Represents a row in the translation table with a key and its translations
     */
    public static class KeyRow {
        private final String key;
        private final List<LanguageEntry> languageEntries;

        public KeyRow(String key, List<LanguageFolder> languages) {
            this.key = key;
            this.languageEntries = new ArrayList<>();
            for (LanguageFolder language : languages) {
                languageEntries.add(new LanguageEntry(language, key));
            }
        }

        public String getKey() {
            return key;
        }

        public List<LanguageEntry> getLanguageEntries() {
            return languageEntries;
        }
    }

    /**
     * Represents a translation entry for a specific language and key
     */
    public static class LanguageEntry {
        private final LanguageFolder language;
        private final String key;
        private final TranslationEntry entry;

        public LanguageEntry(LanguageFolder language, String key) {
            this.language = language;
            this.key = key;
            this.entry = language.getEntryForKey(key);
        }

        public String getLanguageCode() {
            return language.getLanguageCode();
        }

        public String getLanguageName() {
            return language.getName();
        }

        public boolean hasEntry() {
            return entry != null;
        }

        public String getEntryName() {
            return entry != null ? entry.getName() : key;
        }

        public String getMessage() {
            return entry != null ? entry.getMessage() : "";
        }

        public String getKey() {
            return key;
        }
    }

    public static class LanguageFolder {
        private final Resource resource;
        private final ValueMap properties;
        private final String languageCode;

        public LanguageFolder(Resource resource) {
            this.resource = resource;
            this.properties = resource.getValueMap();
            this.languageCode = properties.get("jcr:language", String.class);
        }

        public Resource getResource() {
            return resource;
        }

        public String getName() {
            return resource.getName();
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

        public List<TranslationEntry> getEntries() {
            return StreamSupport.stream(resource.getChildren().spliterator(), false)
                    .map(TranslationEntry::new)
                    .collect(Collectors.toList());
        }

        public TranslationEntry getEntryForKey(String key) {
            for (Resource entry : resource.getChildren()) {
                String entryKey = entry.getValueMap().get("sling:key", String.class);
                if (key.equals(entryKey)) {
                    return new TranslationEntry(entry);
                }
            }
            return null;
        }

        public boolean hasEntryForKey(String key) {
            return getEntryForKey(key) != null;
        }
    }

    public static class TranslationEntry {
        private final Resource resource;
        private final ValueMap properties;

        public TranslationEntry(Resource resource) {
            this.resource = resource;
            this.properties = resource.getValueMap();
        }

        public String getName() {
            return resource.getName();
        }

        public String getKey() {
            return properties.get("sling:key", "");
        }

        public String getMessage() {
            return properties.get("sling:message", "");
        }
    }
}
