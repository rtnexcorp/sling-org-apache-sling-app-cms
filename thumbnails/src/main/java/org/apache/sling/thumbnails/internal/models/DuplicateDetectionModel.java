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
package org.apache.sling.thumbnails.internal.models;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.DuplicateDetectionService;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.jetbrains.annotations.NotNull;

/**
 * Sling Model for duplicate detection management UI
 */
@Model(adaptables = Resource.class)
public class DuplicateDetectionModel {

    @SlingObject
    private Resource resource;

    @SlingObject
    private ResourceResolver resolver;

    @OSGiService
    private DuplicateDetectionService duplicateService;

    /**
     * Get the asset resource
     */
    public Resource getAsset() {
        return resource;
    }

    /**
     * Get duplicates for this asset
     */
    @NotNull
    public List<Resource> getDuplicates() {
        return duplicateService.findDuplicates(resource);
    }

    /**
     * Check if this asset has duplicates
     */
    public boolean hasDuplicates() {
        return duplicateService.hasDuplicates(resource);
    }

    /**
     * Get duplicate count for this asset
     */
    public int getDuplicateCount() {
        return duplicateService.getDuplicateCount(resource);
    }

    /**
     * Get stored hash for this asset
     */
    public String getStoredHash() {
        return duplicateService.getStoredHash(resource);
    }

    /**
     * Get all duplicate groups (for admin overview)
     */
    @NotNull
    public List<List<Resource>> getAllDuplicateGroups() {
        return duplicateService.findAllDuplicateGroups();
    }

    /**
     * Get total number of duplicate groups
     */
    public int getDuplicateGroupCount() {
        return duplicateService.findAllDuplicateGroups().size();
    }

    /**
     * Get total number of duplicate assets
     */
    public int getTotalDuplicateCount() {
        int total = 0;
        for (List<Resource> group : duplicateService.findAllDuplicateGroups()) {
            total += group.size();
        }
        return total;
    }
}
