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

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

/**
 * Represents a language folder containing translation entries.
 */
public class LanguageFolder {
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
