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
package org.apache.sling.cms;

import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service for working with the CMS taxonomy system.
 * Provides methods to retrieve, search, and manage taxonomy items
 * used for content tagging and categorization.
 */
@ProviderType
public interface TaxonomyService {

    /**
     * The root path where taxonomy items are stored.
     */
    String TAXONOMY_ROOT = "/etc/taxonomy";

    /**
     * The resource type for taxonomy items.
     */
    String TAXONOMY_RESOURCE_TYPE = "sling:Taxonomy";

    /**
     * The property name used to store taxonomy references on content.
     */
    String TAXONOMY_PROPERTY = "sling:taxonomy";

    /**
     * Gets all available taxonomy items from the taxonomy tree.
     *
     * @param resolver the resource resolver to use
     * @return list of all taxonomy items, sorted alphabetically by title
     */
    List<TaxonomyItem> getAllTaxonomyItems(ResourceResolver resolver);

    /**
     * Gets taxonomy items at or below the specified path.
     *
     * @param resolver the resource resolver to use
     * @param basePath the base path to search from (e.g., "/etc/taxonomy/categories")
     * @return list of taxonomy items under the specified path
     */
    List<TaxonomyItem> getTaxonomyItems(ResourceResolver resolver, String basePath);

    /**
     * Finds a taxonomy item by its path.
     *
     * @param resolver the resource resolver to use
     * @param path     the path of the taxonomy item
     * @return the taxonomy item, or null if not found
     */
    TaxonomyItem getTaxonomyItem(ResourceResolver resolver, String path);

    /**
     * Searches for taxonomy items matching the given query string.
     * Matches against taxonomy titles using case-insensitive contains.
     *
     * @param resolver the resource resolver to use
     * @param query    the search query
     * @return list of matching taxonomy items
     */
    List<TaxonomyItem> searchTaxonomy(ResourceResolver resolver, String query);

    /**
     * Gets content resources that have been tagged with the specified taxonomy item.
     *
     * @param resolver     the resource resolver to use
     * @param taxonomyPath the path of the taxonomy item
     * @param basePath     the base path to search for tagged content (e.g., "/content")
     * @param limit        maximum number of results to return
     * @return list of resource paths tagged with this taxonomy
     */
    List<String> getTaggedContent(ResourceResolver resolver, String taxonomyPath, String basePath, int limit);
}
