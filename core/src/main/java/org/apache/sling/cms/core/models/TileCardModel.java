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
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Sling Model for the tilecard component.
 * Displays a navigation tile with icon and link.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class TileCardModel {

    private static final String BRANDING_PATH = "/mnt/overlay/sling-cms/content/branding";
    private static final String DEFAULT_GRID_ICONS_BASE = "/static/sling-cms/thumbnails";

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String icon;

    @ValueMapValue
    private String link;

    /**
     * @return the translated title
     */
    public String getTitle() {
        if (title != null) {
            ResourceBundle bundle = request.getResourceBundle(request.getLocale());
            if (bundle != null && bundle.containsKey(title)) {
                return bundle.getString(title);
            }
        }
        return title;
    }

    /**
     * @return the raw title key (for data attribute)
     */
    public String getTitleKey() {
        return title;
    }

    /**
     * @return the resource path
     */
    public String getResourcePath() {
        return resource.getPath();
    }

    /**
     * @return the icon path
     */
    public String getIcon() {
        return icon;
    }

    /**
     * @return the link URL
     */
    public String getLink() {
        return link;
    }

    /**
     * @return the full icon image URL
     */
    public String getIconUrl() {
        Resource brandingResource = request.getResourceResolver().getResource(BRANDING_PATH);
        String gridIconsBase = DEFAULT_GRID_ICONS_BASE;
        if (brandingResource != null) {
            gridIconsBase = brandingResource.getValueMap().get("gridIconsBase", DEFAULT_GRID_ICONS_BASE);
        }
        return "/cms/file/preview.html" + gridIconsBase + icon;
    }
}
