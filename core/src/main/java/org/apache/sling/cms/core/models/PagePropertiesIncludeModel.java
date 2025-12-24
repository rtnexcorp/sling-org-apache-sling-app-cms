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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Sling Model for the Page Properties Include component.
 * Provides the list of field resources from the suffix resource's "fields" child.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class PagePropertiesIncludeModel {

    @SlingObject
    private SlingHttpServletRequest request;

    /**
     * Gets the list of field resources from the suffix resource's "fields" child.
     *
     * @return list of field resources, or empty list if not found
     */
    public List<Resource> getFields() {
        Resource suffixResource = request.getRequestPathInfo().getSuffixResource();
        if (suffixResource == null) {
            return Collections.emptyList();
        }

        Resource fieldsResource = suffixResource.getChild("fields");
        if (fieldsResource == null) {
            return Collections.emptyList();
        }

        List<Resource> fields = new ArrayList<>();
        fieldsResource.listChildren().forEachRemaining(fields::add);
        return fields;
    }
}
