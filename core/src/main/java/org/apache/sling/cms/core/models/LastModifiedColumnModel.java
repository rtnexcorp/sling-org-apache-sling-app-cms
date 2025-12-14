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

import java.text.DateFormat;
import java.util.Calendar;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the lastmodified column component.
 * Displays the last modified date and user.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class LastModifiedColumnModel {

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    private Resource colConfig;
    private ValueMap colConfigValueMap;

    private Resource getColConfig() {
        if (colConfig == null) {
            colConfig = (Resource) request.getAttribute("colConfig");
        }
        return colConfig;
    }

    private ValueMap getColConfigValueMap() {
        if (colConfigValueMap == null && getColConfig() != null) {
            colConfigValueMap = getColConfig().getValueMap();
        }
        return colConfigValueMap;
    }

    private String getSubPath() {
        ValueMap vm = getColConfigValueMap();
        return vm != null ? vm.get("subPath", "") : "";
    }

    /**
     * @return the formatted last modified date
     */
    public String getLastModified() {
        try {
            String property = getSubPath() + "jcr:lastModified";
            Calendar cal = resource.getValueMap().get(property, Calendar.class);
            if (cal != null) {
                DateFormat df =
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, request.getLocale());
                return df.format(cal.getTime());
            }
        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    /**
     * @return the last modified by user
     */
    public String getLastModifiedBy() {
        String property = getSubPath() + "jcr:lastModifiedBy";
        return resource.getValueMap().get(property, "");
    }

    /**
     * @return the combined title value
     */
    public String getTitleValue() {
        String date = getLastModified();
        String user = getLastModifiedBy();
        if (!date.isEmpty() && !user.isEmpty()) {
            return date + " - " + user;
        }
        return date + user;
    }

    /**
     * @return true if date is valid
     */
    public boolean hasDate() {
        return !getLastModified().isEmpty();
    }
}
