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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.core.beans.Tab;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the generic tabs component. Provides tab configuration and
 * panel content management.
 */
@Model(adaptables = {SlingHttpServletRequest.class, Resource.class})
public class TabsModel {

    @SlingObject
    private Resource resource;

    private String tabsId;
    private List<Tab> tabs;

    @PostConstruct
    protected void init() {
        this.tabsId = "tabs-" + UUID.randomUUID().toString();
        this.tabs = new ArrayList<>();

        // Load tabs from child resources
        Resource tabsResource = resource.getChild("tabs");
        if (tabsResource != null) {
            tabsResource.getChildren().forEach(tabResource -> {
                ValueMap properties = tabResource.getValueMap();
                String id = properties.get("id", tabResource.getName());
                String title = properties.get("title", "");
                String icon = properties.get("icon", String.class);
                String resourceName = properties.get("resourceName", "");
                String resourceType = properties.get("resourceType", String.class);

                tabs.add(new Tab(id, title, icon, resourceName, resourceType));
            });
        }
    }

    public String getTabsId() {
        return tabsId;
    }

    public List<Tab> getTabs() {
        return Collections.unmodifiableList(tabs);
    }
}
