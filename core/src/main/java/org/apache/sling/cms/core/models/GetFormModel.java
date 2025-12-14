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
import org.apache.sling.cms.Page;
import org.apache.sling.cms.PageManager;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Sling Model for the getform component.
 * Provides a GET form with configurable action, target and button.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class GetFormModel {

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @ValueMapValue
    @Default(values = "")
    private String action;

    @ValueMapValue
    @Default(values = "")
    private String target;

    @ValueMapValue
    @Default(values = "")
    private String load;

    @ValueMapValue
    @Default(values = "")
    private String button;

    /**
     * @return the form action URL
     */
    public String getAction() {
        if (action != null && !action.isEmpty()) {
            return action;
        }
        PageManager pageManager = resource.adaptTo(PageManager.class);
        if (pageManager != null) {
            Page page = pageManager.getPage();
            if (page != null) {
                return page.getPath() + ".html";
            }
        }
        return "";
    }

    /**
     * @return the target selector
     */
    public String getTarget() {
        return target;
    }

    /**
     * @return the load selector
     */
    public String getLoad() {
        return load;
    }

    /**
     * @return the translated button text
     */
    public String getButtonText() {
        if (button != null) {
            ResourceBundle bundle = request.getResourceBundle(request.getLocale());
            if (bundle != null && bundle.containsKey(button)) {
                return bundle.getString(button);
            }
        }
        return button;
    }
}
