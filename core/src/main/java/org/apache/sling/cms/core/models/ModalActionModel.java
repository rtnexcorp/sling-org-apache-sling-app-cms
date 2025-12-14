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

import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the modal action button component.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ModalActionModel {

    @SlingObject
    private SlingHttpServletRequest request;

    @Inject
    private Resource resource;

    /**
     * Action configuration resource.
     *
     * <p>
     * In the page edit bar, action configuration is passed via request attribute "actionConfig".
     * In other contexts, fall back to the current resource.
     */
    public Resource getConfig() {
        Object cfg = request.getAttribute("actionConfig");
        if (cfg instanceof Resource) {
            return (Resource) cfg;
        }
        return resource;
    }

    public ValueMap getConfigProperties() {
        Resource cfg = getConfig();
        return cfg != null ? cfg.getValueMap() : ValueMap.EMPTY;
    }

    public String getTitle() {
        return getConfigProperties().get("title", "");
    }

    public String getAjaxPath() {
        return getConfigProperties().get("ajaxPath", ".Main-Content form");
    }

    public String getIcon() {
        return getConfigProperties().get("icon", "cog");
    }

    public String getPrefix() {
        return getConfigProperties().get("prefix", "");
    }

    public String getSuffix() {
        return getConfigProperties().get("suffix", "");
    }

    /**
     * Path of the resource the action should operate on.
     */
    public String getTargetPath() {
        return resource != null ? resource.getPath() : "";
    }

    public String getHref() {
        return getPrefix() + getTargetPath() + getSuffix();
    }
}
