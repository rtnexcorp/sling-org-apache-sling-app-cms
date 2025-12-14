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

import java.util.ResourceBundle;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

/**
 * Sling Model for the static column component.
 * Displays an i18n translated static value from the column configuration.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class StaticColumnModel {

    @Self
    private SlingHttpServletRequest request;

    /**
     * @return the translated column value
     */
    public String getColumnValue() {
        Resource colConfig = (Resource) request.getAttribute("colConfig");
        if (colConfig != null) {
            ValueMap valueMap = colConfig.getValueMap();
            String key = valueMap.get("value", String.class);
            if (key != null) {
                ResourceBundle bundle = request.getResourceBundle(request.getLocale());
                if (bundle != null && bundle.containsKey(key)) {
                    return bundle.getString(key);
                }
                return key;
            }
        }
        return "";
    }
}
