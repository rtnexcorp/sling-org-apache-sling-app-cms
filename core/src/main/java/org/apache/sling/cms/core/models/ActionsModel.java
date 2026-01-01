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
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ActionsModel {

    private static final Logger LOG = LoggerFactory.getLogger(ActionsModel.class);

    @Self
    private SlingHttpServletRequest request;

    public String getPagePath() {
        if (request == null) {
            LOG.warn("ActionsModel: request is null");
            return "";
        }
        String suffix = request.getRequestPathInfo().getSuffix();
        LOG.debug("ActionsModel.getPagePath() returning suffix: {}", suffix);
        return suffix != null ? suffix : "";
    }

    public void setActionConfig(String actionConfigPath) {
        if (request != null && actionConfigPath != null) {
            request.setAttribute("actionConfigPath", actionConfigPath);
            LOG.debug("ActionsModel: set actionConfigPath={}", actionConfigPath);
        }
    }
}
