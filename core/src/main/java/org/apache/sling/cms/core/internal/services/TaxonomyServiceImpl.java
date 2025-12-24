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
package org.apache.sling.cms.core.internal.services;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.TaxonomyItem;
import org.apache.sling.cms.TaxonomyService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of TaxonomyService for managing taxonomy items.
 */
@Component(service = TaxonomyService.class)
public class TaxonomyServiceImpl implements TaxonomyService {

    private static final Logger log = LoggerFactory.getLogger(TaxonomyServiceImpl.class);

    @Override
    public List<TaxonomyItem> getAllTaxonomyItems(ResourceResolver resolver) {
        return getTaxonomyItems(resolver, TAXONOMY_ROOT);
    }

    @Override
    public List<TaxonomyItem> getTaxonomyItems(ResourceResolver resolver, String basePath) {
        List<TaxonomyItem> items = new ArrayList<>();

        try {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                log.warn("Unable to get JCR session from resolver");
                return items;
            }

            QueryManager queryManager = session.getWorkspace().getQueryManager();
            String query = String.format(
                    "SELECT * FROM [sling:Taxonomy] AS t WHERE ISDESCENDANTNODE(t, '%s') ORDER BY t.[jcr:title]",
                    basePath);
            Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
            QueryResult result = jcrQuery.execute();
            RowIterator rows = result.getRows();

            while (rows.hasNext()) {
                Row row = rows.nextRow();
                String path = row.getPath();
                Resource resource = resolver.getResource(path);
                if (resource != null) {
                    TaxonomyItem item = resource.adaptTo(TaxonomyItem.class);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Error querying taxonomy items from {}", basePath, e);
        }

        return items;
    }

    @Override
    public TaxonomyItem getTaxonomyItem(ResourceResolver resolver, String path) {
        Resource resource = resolver.getResource(path);
        if (resource != null) {
            return resource.adaptTo(TaxonomyItem.class);
        }
        return null;
    }

    @Override
    public List<TaxonomyItem> searchTaxonomy(ResourceResolver resolver, String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllTaxonomyItems(resolver);
        }

        List<TaxonomyItem> items = new ArrayList<>();
        String searchQuery = query.toLowerCase().trim();

        try {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                log.warn("Unable to get JCR session from resolver");
                return items;
            }

            QueryManager queryManager = session.getWorkspace().getQueryManager();
            String jcrQuery = String.format(
                    "SELECT * FROM [sling:Taxonomy] AS t WHERE ISDESCENDANTNODE(t, '%s') "
                            + "AND (LOWER(t.[jcr:title]) LIKE '%%%s%%' OR LOWER(NAME(t)) LIKE '%%%s%%') "
                            + "ORDER BY t.[jcr:title]",
                    TAXONOMY_ROOT, searchQuery, searchQuery);
            Query q = queryManager.createQuery(jcrQuery, Query.JCR_SQL2);
            QueryResult result = q.execute();
            RowIterator rows = result.getRows();

            while (rows.hasNext()) {
                Row row = rows.nextRow();
                String path = row.getPath();
                Resource resource = resolver.getResource(path);
                if (resource != null) {
                    TaxonomyItem item = resource.adaptTo(TaxonomyItem.class);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Error searching taxonomy for query: {}", query, e);
        }

        return items;
    }

    @Override
    public List<String> getTaggedContent(ResourceResolver resolver, String taxonomyPath, String basePath, int limit) {
        List<String> paths = new ArrayList<>();

        try {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                log.warn("Unable to get JCR session from resolver");
                return paths;
            }

            QueryManager queryManager = session.getWorkspace().getQueryManager();
            String query = String.format(
                    "SELECT * FROM [nt:base] AS c WHERE ISDESCENDANTNODE(c, '%s') " + "AND c.[sling:taxonomy] = '%s'",
                    basePath, taxonomyPath);
            Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
            jcrQuery.setLimit(limit);
            QueryResult result = jcrQuery.execute();
            RowIterator rows = result.getRows();

            while (rows.hasNext()) {
                Row row = rows.nextRow();
                paths.add(row.getPath());
            }
        } catch (RepositoryException e) {
            log.error("Error finding content tagged with: {}", taxonomyPath, e);
        }

        return paths;
    }
}
