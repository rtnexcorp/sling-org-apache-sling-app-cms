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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Sling Model for the slingform component that handles form action determination
 * and encoding configuration.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SlingFormModel {

    @SlingObject
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String encoding;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String actionSuffix;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String callback;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Boolean addDate;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String button;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Boolean skipcancel;

    private String formAction;

    @PostConstruct
    protected void init() {
        // Default encoding
        if (encoding == null || encoding.isEmpty()) {
            encoding = "multipart/form-data";
        }

        // Determine form action
        // First check for formActionPath request attribute (set by propertieseditor for jcr:content)
        Object formActionPathAttr = request.getAttribute("formActionPath");
        if (formActionPathAttr != null) {
            formAction = formActionPathAttr.toString();
        } else {
            // Fall back to using the request suffix
            formAction = request.getRequestPathInfo().getSuffix();
        }
    }

    public String getEncoding() {
        return encoding;
    }

    public String getFormAction() {
        return formAction;
    }

    public String getActionSuffix() {
        return actionSuffix != null ? actionSuffix : "";
    }

    public String getCallback() {
        return callback;
    }

    public boolean isAddDate() {
        return addDate == null || addDate;
    }

    public String getButtonLabel() {
        return button != null ? button : "Save";
    }

    public boolean isSkipCancel() {
        return skipcancel != null && skipcancel;
    }

    public String getReferer() {
        return request.getHeader("referer");
    }
}
