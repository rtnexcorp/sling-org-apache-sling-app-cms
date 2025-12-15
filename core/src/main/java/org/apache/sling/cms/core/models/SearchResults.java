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

import javax.jcr.query.Query;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.CMSConstants;
import org.apache.sling.models.annotations.Model;
import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model for retrieving the search results
 */
@ProviderType
@Model(adaptables = SlingHttpServletRequest.class)
public class SearchResults {

    private static final Logger log = LoggerFactory.getLogger(SearchResults.class);

    private String type = CMSConstants.NT_PAGE;
    private String path = null;
    private String term = null;
    private boolean useFullText = true;
    private SlingHttpServletRequest request;

    public SearchResults(SlingHttpServletRequest request) {
        if (StringUtils.isNotEmpty(request.getParameter("type"))) {
            type = request.getParameter("type");
        }

        if (StringUtils.isNotEmpty(request.getParameter("path"))) {
            path = request.getParameter("path");
        }

        // Support both 'term' and 'q' parameters for flexibility
        String searchTerm = request.getParameter("term");
        if (StringUtils.isEmpty(searchTerm)) {
            searchTerm = request.getParameter("q");
            // When using 'q' param (from start page), default to searching all hierarchy nodes
            if (StringUtils.isNotEmpty(searchTerm) && StringUtils.isEmpty(request.getParameter("type"))) {
                type = "nt:hierarchyNode";
            }
        }

        // Allow explicit control of fulltext via parameter (default to true for Lucene)
        String fulltextParam = request.getParameter("fulltext");
        if (StringUtils.isNotEmpty(fulltextParam)) {
            useFullText = Boolean.parseBoolean(fulltextParam);
        }

        if (StringUtils.isNotEmpty(searchTerm)) {
            term = Text.escapeIllegalXpathSearchChars(searchTerm).replace("'", "''");
        } else {
            term = "";
        }
        this.request = request;
    }

    private String buildQuery() {
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM [").append(type).append("] AS s WHERE ");

        if (useFullText) {
            // Full-text search using CONTAINS on indexed properties
            // Search across title, description, name, and content properties
            query.append("(CONTAINS(s.[jcr:content/jcr:title], '")
                    .append(term)
                    .append("')")
                    .append(" OR CONTAINS(s.[jcr:title], '")
                    .append(term)
                    .append("')")
                    .append(" OR CONTAINS(s.[jcr:content/jcr:description], '")
                    .append(term)
                    .append("')")
                    .append(" OR s.[jcr:content/jcr:title] LIKE '%")
                    .append(term)
                    .append("%'")
                    .append(" OR LOCALNAME(s) LIKE '%")
                    .append(term.toLowerCase())
                    .append("%')");
        } else {
            // Property-based search using LIKE (no fulltext index required)
            String lowerTerm = term.toLowerCase();
            query.append("(LOWER(s.[jcr:title]) LIKE '%")
                    .append(lowerTerm)
                    .append("%'")
                    .append(" OR LOWER(s.[jcr:content/jcr:title]) LIKE '%")
                    .append(lowerTerm)
                    .append("%'")
                    .append(" OR LOWER(NAME(s)) LIKE '%")
                    .append(lowerTerm)
                    .append("%')");
        }

        if (StringUtils.isNotEmpty(path)) {
            query.append(" AND ISDESCENDANTNODE([").append(path).append("])");
        }

        return query.toString();
    }

    public Iterator<Resource> getResults() {
        if (StringUtils.isEmpty(term)) {
            return java.util.Collections.emptyIterator();
        }
        String query = buildQuery();
        log.debug("Searching for content with {}", query);
        return request.getResourceResolver().findResources(query, Query.JCR_SQL2);
    }

    /**
     * Returns search results as a List for easier HTL iteration.
     * Limited to 20 results.
     *
     * @return List of matching resources
     */
    public List<Resource> getResultList() {
        if (StringUtils.isEmpty(term)) {
            return java.util.Collections.emptyList();
        }
        String query = buildQuery();
        log.debug("Searching for content with {}", query);
        Iterator<Resource> it = request.getResourceResolver().findResources(query, Query.JCR_SQL2);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.NONNULL), false)
                .limit(20)
                .collect(Collectors.toList());
    }
}
