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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model for action button configuration.
 * Provides access to action button properties (title, icon, prefix, suffix, etc.)
 * from the current resource's ValueMap.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ActionButtonModel {

    private static final Logger LOG = LoggerFactory.getLogger(ActionButtonModel.class);

    @SlingObject
    private Resource resource;

    /**
     * Returns the configuration ValueMap from the current action button resource.
     * This provides access to properties like title, icon, prefix, suffix, ajaxPath, etc.
     *
     * @return ValueMap containing action button configuration properties
     */
    public ValueMap getConfig() {
        if (resource == null) {
            LOG.warn("ActionButtonModel: resource is null");
            return ValueMap.EMPTY;
        }

        LOG.debug("ActionButtonModel: returning config from resource {}", resource.getPath());
        return resource.getValueMap();
    }
}
