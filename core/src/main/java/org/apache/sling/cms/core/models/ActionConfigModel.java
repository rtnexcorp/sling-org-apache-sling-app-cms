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
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ActionConfigModel {

    private static final Logger LOG = LoggerFactory.getLogger(ActionConfigModel.class);

    @Self
    private SlingHttpServletRequest request;

    public ValueMap getConfig() {
        String selector = request.getRequestPathInfo().getSelectorString();
        if (selector == null || selector.isEmpty()) {
            LOG.warn("No selector found for action config");
            return ValueMap.EMPTY;
        }

        Resource resource = request.getResource();
        if (resource == null) {
            LOG.warn("No resource found");
            return ValueMap.EMPTY;
        }

        Resource parent = resource.getParent();
        if (parent == null) {
            LOG.warn("No parent resource found");
            return ValueMap.EMPTY;
        }

        Resource actionsResource = parent.getChild("actions");
        if (actionsResource == null) {
            LOG.warn("No actions resource found at {}/actions", parent.getPath());
            return ValueMap.EMPTY;
        }

        Resource actionConfig = actionsResource.getChild(selector);
        if (actionConfig == null) {
            LOG.warn("No action config found at {}/{}", actionsResource.getPath(), selector);
            return ValueMap.EMPTY;
        }

        LOG.debug("Found action config at {}", actionConfig.getPath());
        return actionConfig.getValueMap();
    }
}
