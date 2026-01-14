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

/**
 * Represents a translation entry for a specific language and key.
 */
public class LanguageEntry {
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
