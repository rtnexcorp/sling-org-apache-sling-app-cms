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

import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A model retrieving all of the keys for a i18n dictionary
 */
@ProviderType
@Model(adaptables = Resource.class)
public class I18nHelper {

    private static final Logger log = LoggerFactory.getLogger(I18nHelper.class);

    @SlingObject
    private Resource resource;

    private Set<String> keys = new TreeSet<>();

    private Random rand = new Random();

    public Set<String> getKeys() {
        if (keys.isEmpty()) {
            log.debug("Collecting keys from dictionary at: {}", resource.getPath());
            // Iterate through language folders
            for (Resource langFolder : resource.getChildren()) {
                String primaryType = langFolder.getValueMap().get("jcr:primaryType", String.class);
                log.debug("Checking language folder: {} with type: {}", langFolder.getPath(), primaryType);

                // Check if this is a language folder
                if ("sling:Folder".equals(primaryType)) {
                    // Iterate through message entries in this language folder
                    for (Resource entry : langFolder.getChildren()) {
                        String entryType = entry.getValueMap().get("jcr:primaryType", String.class);
                        if ("sling:MessageEntry".equals(entryType)) {
                            String key = entry.getValueMap().get("sling:key", String.class);
                            if (key != null && !key.isEmpty()) {
                                keys.add(key);
                                log.debug("Added key: {} from entry: {}", key, entry.getPath());
                            }
                        }
                    }
                }
            }
            log.info("Found {} unique keys for dictionary at: {}", keys.size(), resource.getPath());
        }
        return keys;
    }

    public String getRandom() {
        return String.valueOf(rand.nextInt());
    }
}
