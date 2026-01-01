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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
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
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.CMSConstants;
import org.apache.sling.cms.TaxonomyItem;
import org.apache.sling.cms.TaxonomyService;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model for advanced search with filters for tags, date range, author, and content type.
 * Provides taxonomy badges for search results.
 */
@ProviderType
@Model(adaptables = SlingHttpServletRequest.class)
public class AdvancedSearchResults {

    private static final Logger log = LoggerFactory.getLogger(AdvancedSearchResults.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final int DEFAULT_LIMIT = 20;

    private final SlingHttpServletRequest request;
    private final ResourceResolver resolver;

    @OSGiService
    private TaxonomyService taxonomyService;

    // Search parameters
    private String term;
    private String taxonomyPath;
    private String author;
    private String contentType;
    private Date dateFrom;
    private Date dateTo;
    private String path;
    private int limit;

    public AdvancedSearchResults(SlingHttpServletRequest request) {
        this.request = request;
        this.resolver = request.getResourceResolver();
        parseParameters();
    }

    private void parseParameters() {
        // Search term (supports both 'q' and 'term' parameters)
        String searchTerm = request.getParameter("q");
        if (StringUtils.isEmpty(searchTerm)) {
            searchTerm = request.getParameter("term");
        }
        if (StringUtils.isNotEmpty(searchTerm)) {
            this.term = Text.escapeIllegalXpathSearchChars(searchTerm).replace("'", "''");
        }

        // Taxonomy/tag filter
        this.taxonomyPath = request.getParameter("taxonomy");

        // Author filter
        String authorParam = request.getParameter("author");
        if (StringUtils.isNotEmpty(authorParam)) {
            this.author = authorParam.replace("'", "''");
        }

        // Content type filter (page, file, all)
        this.contentType = request.getParameter("type");
        if (StringUtils.isEmpty(this.contentType)) {
            this.contentType = "all";
        }

        // Date range filters
        String dateFromParam = request.getParameter("dateFrom");
        String dateToParam = request.getParameter("dateTo");
        try {
            if (StringUtils.isNotEmpty(dateFromParam)) {
                this.dateFrom = DATE_FORMAT.parse(dateFromParam);
            }
            if (StringUtils.isNotEmpty(dateToParam)) {
                this.dateTo = DATE_FORMAT.parse(dateToParam);
            }
        } catch (ParseException e) {
            log.warn("Invalid date format in search parameters", e);
        }

        // Path restriction
        this.path = request.getParameter("path");
        if (StringUtils.isEmpty(this.path)) {
            this.path = "/content";
        }

        // Result limit
        String limitParam = request.getParameter("limit");
        this.limit = DEFAULT_LIMIT;
        if (StringUtils.isNotEmpty(limitParam)) {
            try {
                this.limit = Integer.parseInt(limitParam);
            } catch (NumberFormatException e) {
                log.warn("Invalid limit parameter: {}", limitParam);
            }
        }
    }

    private String buildQuery() {
        StringBuilder query = new StringBuilder();

        // Determine node type based on content type filter
        String nodeType;
        switch (contentType.toLowerCase()) {
            case "page":
                nodeType = CMSConstants.NT_PAGE;
                break;
            case "file":
                nodeType = "nt:file";
                break;
            default:
                nodeType = "nt:hierarchyNode";
        }

        query.append("SELECT * FROM [").append(nodeType).append("] AS s WHERE ");

        List<String> conditions = new ArrayList<>();

        // Path restriction
        conditions.add("ISDESCENDANTNODE([" + path + "])");

        // Text search
        if (StringUtils.isNotEmpty(term)) {
            conditions.add("(CONTAINS(s.[jcr:content/jcr:title], '" + term + "')"
                    + " OR CONTAINS(s.[jcr:title], '" + term + "')"
                    + " OR CONTAINS(s.[jcr:content/jcr:description], '" + term + "')"
                    + " OR s.[jcr:content/jcr:title] LIKE '%" + term + "%'"
                    + " OR LOCALNAME(s) LIKE '%" + term.toLowerCase() + "%')");
        }

        // Taxonomy filter
        if (StringUtils.isNotEmpty(taxonomyPath)) {
            conditions.add("s.[jcr:content/sling:taxonomy] = '" + taxonomyPath + "'");
        }

        // Author filter
        if (StringUtils.isNotEmpty(author)) {
            conditions.add("(s.[jcr:content/jcr:lastModifiedBy] = '" + author + "'"
                    + " OR s.[jcr:content/jcr:createdBy] = '" + author + "')");
        }

        // Date range filters
        if (dateFrom != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(dateFrom);
            conditions.add("s.[jcr:content/jcr:lastModified] >= CAST('" + formatDateForQuery(cal) + "' AS DATE)");
        }
        if (dateTo != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(dateTo);
            // Add one day to include the end date
            cal.add(Calendar.DAY_OF_MONTH, 1);
            conditions.add("s.[jcr:content/jcr:lastModified] < CAST('" + formatDateForQuery(cal) + "' AS DATE)");
        }

        query.append(String.join(" AND ", conditions));
        query.append(" ORDER BY s.[jcr:content/jcr:lastModified] DESC");

        return query.toString();
    }

    private String formatDateForQuery(Calendar cal) {
        return String.format(
                "%04d-%02d-%02dT00:00:00.000Z",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Checks if any search criteria is provided.
     */
    public boolean hasSearchCriteria() {
        return StringUtils.isNotEmpty(term)
                || StringUtils.isNotEmpty(taxonomyPath)
                || StringUtils.isNotEmpty(author)
                || dateFrom != null
                || dateTo != null;
    }

    /**
     * Returns search results as a List for HTL iteration.
     */
    public List<SearchResultItem> getResults() {
        if (!hasSearchCriteria()) {
            return Collections.emptyList();
        }

        String query = buildQuery();
        log.debug("Advanced search query: {}", query);

        Iterator<Resource> it = resolver.findResources(query, Query.JCR_SQL2);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.NONNULL), false)
                .limit(limit)
                .map(r -> new SearchResultItem(r, taxonomyService))
                .collect(Collectors.toList());
    }

    /**
     * Returns search results as raw Resource list for backward compatibility.
     * Used by searchresults component.
     */
    public List<Resource> getResultList() {
        if (!hasSearchCriteria()) {
            return Collections.emptyList();
        }

        String query = buildQuery();
        log.debug("Search query: {}", query);

        Iterator<Resource> it = resolver.findResources(query, Query.JCR_SQL2);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.NONNULL), false)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get all available taxonomy items for the tag dropdown.
     */
    public List<TaxonomyItem> getTaxonomyOptions() {
        if (taxonomyService != null) {
            return taxonomyService.getAllTaxonomyItems(resolver);
        }
        return Collections.emptyList();
    }

    /**
     * Get top 10 taxonomy items for the quick tag search panel.
     */
    public List<TaxonomyItem> getTopTaxonomyOptions() {
        List<TaxonomyItem> allItems = getTaxonomyOptions();
        if (allItems.size() <= 10) {
            return allItems;
        }
        return allItems.subList(0, 10);
    }

    /**
     * Get the current search term.
     */
    public String getTerm() {
        return request.getParameter("q") != null ? request.getParameter("q") : request.getParameter("term");
    }

    /**
     * Get the current taxonomy filter.
     */
    public String getTaxonomyPath() {
        return taxonomyPath;
    }

    /**
     * Get the current author filter.
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Get the current content type filter.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Get the current date from filter.
     */
    public String getDateFrom() {
        return request.getParameter("dateFrom");
    }

    /**
     * Get the current date to filter.
     */
    public String getDateTo() {
        return request.getParameter("dateTo");
    }

    /**
     * Wrapper class for search result items with taxonomy badges.
     */
    public static class SearchResultItem {
        private final Resource resource;
        private final TaxonomyService taxonomyService;
        private List<TaxonomyItem> taxonomyItems;

        public SearchResultItem(Resource resource, TaxonomyService taxonomyService) {
            this.resource = resource;
            this.taxonomyService = taxonomyService;
        }

        public Resource getResource() {
            return resource;
        }

        public String getPath() {
            return resource.getPath();
        }

        public String getName() {
            return resource.getName();
        }

        public String getTitle() {
            ValueMap vm = resource.getValueMap();
            String title = vm.get("jcr:content/jcr:title", String.class);
            if (title == null) {
                title = vm.get("jcr:title", String.class);
            }
            return title != null ? title : resource.getName();
        }

        public String getDescription() {
            ValueMap vm = resource.getValueMap();
            return vm.get("jcr:content/jcr:description", String.class);
        }

        public String getResourceType() {
            return resource.getResourceType();
        }

        public String getLastModifiedBy() {
            ValueMap vm = resource.getValueMap();
            return vm.get("jcr:content/jcr:lastModifiedBy", String.class);
        }

        public Calendar getLastModified() {
            ValueMap vm = resource.getValueMap();
            return vm.get("jcr:content/jcr:lastModified", Calendar.class);
        }

        /**
         * Get the last modified date formatted as "MMM d, yyyy".
         */
        public String getLastModifiedFormatted() {
            Calendar cal = getLastModified();
            if (cal != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
                return sdf.format(cal.getTime());
            }
            return null;
        }

        public boolean isPage() {
            return "sling:Page".equals(resource.getResourceType());
        }

        public boolean isFile() {
            String type = resource.getResourceType();
            return "nt:file".equals(type) || "sling:File".equals(type);
        }

        public boolean isFolder() {
            String type = resource.getResourceType();
            return type != null && (type.contains("Folder") || "nt:folder".equals(type));
        }

        /**
         * Get taxonomy badges for this result.
         */
        public List<TaxonomyItem> getTaxonomyItems() {
            if (taxonomyItems == null) {
                taxonomyItems = new ArrayList<>();
                if (taxonomyService != null) {
                    ValueMap vm = resource.getValueMap();
                    String[] taxonomyPaths = vm.get("jcr:content/sling:taxonomy", String[].class);
                    if (taxonomyPaths == null) {
                        taxonomyPaths = vm.get("sling:taxonomy", String[].class);
                    }
                    if (taxonomyPaths != null) {
                        for (String path : taxonomyPaths) {
                            TaxonomyItem item = taxonomyService.getTaxonomyItem(resource.getResourceResolver(), path);
                            if (item != null) {
                                taxonomyItems.add(item);
                            }
                        }
                    }
                }
            }
            return taxonomyItems;
        }

        public boolean hasTaxonomy() {
            return !getTaxonomyItems().isEmpty();
        }
    }
}
