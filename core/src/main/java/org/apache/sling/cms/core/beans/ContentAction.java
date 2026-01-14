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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

/**
 * Represents an action button configuration for a content item.
 * Actions are configurable buttons that appear on content items (e.g., Edit, Delete, Publish).
 */
public class ContentAction {

    private final String resourceType;
    private final String itemPath;
    private final Resource actionResource;
    private final String title;
    private final String icon;
    private final String prefix;
    private final String suffix;
    private final String ajaxPath;
    private final boolean openInNew;

    /**
     * Constructor for ContentAction
     *
     * @param actionResource The resource containing action configuration
     * @param itemPath The path of the content item this action applies to
     */
    public ContentAction(Resource actionResource, String itemPath) {
        this.actionResource = actionResource;
        this.itemPath = itemPath;
        ValueMap vm = actionResource.getValueMap();
        this.resourceType = vm.get("sling:resourceType", String.class);
        this.title = vm.get("title", String.class);
        this.icon = vm.get("icon", String.class);
        this.prefix = vm.get("prefix", "");
        this.suffix = vm.get("suffix", "");
        this.ajaxPath = vm.get("ajaxPath", String.class);
        this.openInNew = vm.get("new", false);
    }

    /**
     * Get the Sling resource type for this action
     *
     * @return Resource type string
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Get the path of the content item this action applies to
     *
     * @return Item path
     */
    public String getItemPath() {
        return itemPath;
    }

    /**
     * Get the action configuration resource
     *
     * @return Action resource
     */
    public Resource getActionResource() {
        return actionResource;
    }

    /**
     * Get the display title/label for the action button
     *
     * @return Action title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the icon class/name for the action button
     *
     * @return Icon class (e.g., "jam-pencil")
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Get the URL prefix for constructing the action URL
     *
     * @return URL prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Get the URL suffix for constructing the action URL
     *
     * @return URL suffix
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * Get the AJAX path if this action should be executed via AJAX
     *
     * @return AJAX path or null
     */
    public String getAjaxPath() {
        return ajaxPath;
    }

    /**
     * Check if this action should open in a new window/tab
     *
     * @return true if should open in new window
     */
    public boolean isOpenInNew() {
        return openInNew;
    }
}
