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

import java.text.DateFormat;
import java.util.Calendar;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model for the Last Modified Column component
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class LastModifiedColumnModel {

    private static final Logger log = LoggerFactory.getLogger(LastModifiedColumnModel.class);

    @SlingObject
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    private String lastModified;
    private String lastModifiedBy;
    private boolean hasData;

    @PostConstruct
    protected void init() {
        try {
            String subPath = "";
            Object subPathAttr = request.getAttribute("subPath");
            if (subPathAttr != null) {
                subPath = subPathAttr.toString();
            }

            String modifiedProperty = subPath + "jcr:lastModified";
            String modifiedByProperty = subPath + "jcr:lastModifiedBy";

            Calendar lastModifiedCal = resource.getValueMap().get(modifiedProperty, Calendar.class);
            if (lastModifiedCal != null) {
                DateFormat dateFormat =
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, request.getLocale());
                lastModified = dateFormat.format(lastModifiedCal.getTime());
                lastModifiedBy = resource.getValueMap().get(modifiedByProperty, String.class);
                hasData = true;
            } else {
                hasData = false;
            }
        } catch (Exception e) {
            log.debug("Failed to get last modified date", e);
            hasData = false;
        }
    }

    public String getLastModified() {
        return lastModified;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public boolean isHasData() {
        return hasData;
    }

    public String getTitle() {
        if (hasData && lastModified != null) {
            return lastModifiedBy != null ? lastModified + " - " + lastModifiedBy : lastModified;
        }
        return " - ";
    }
}
