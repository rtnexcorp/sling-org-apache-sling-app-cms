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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.TaxonomyItem;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Sling Model implementation of TaxonomyItem interface.
 */
@Model(adaptables = Resource.class, adapters = TaxonomyItem.class)
public class TaxonomyItemModel implements TaxonomyItem {

    @Self
    private Resource resource;

    @ValueMapValue(name = "jcr:title")
    private String title;

    @Override
    public String getPath() {
        return resource.getPath();
    }

    @Override
    public String getTitle() {
        if (StringUtils.isNotBlank(title)) {
            return title;
        }
        return resource.getName();
    }

    @Override
    public String getName() {
        return resource.getName();
    }

    @Override
    public String getParentPath() {
        Resource parent = resource.getParent();
        if (parent != null && parent.getPath().startsWith("/etc/taxonomy")) {
            return parent.getPath();
        }
        return null;
    }

    @Override
    public int getDepth() {
        String path = resource.getPath();
        if (path.startsWith("/etc/taxonomy/")) {
            String relativePath = path.substring("/etc/taxonomy/".length());
            return StringUtils.countMatches(relativePath, '/');
        }
        return 0;
    }

    @Override
    public Resource getResource() {
        return resource;
    }
}
