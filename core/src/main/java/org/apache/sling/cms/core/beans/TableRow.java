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
package org.apache.sling.cms.core.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

/**
 * Represents a row in the content table.
 */
public class TableRow {
    private final Resource resource;
    private final Resource typeConfig;
    private final int rowNumber;
    private final ValueMap properties;

    public TableRow(Resource resource, Resource typeConfig, int rowNumber) {
        this.resource = resource;
        this.typeConfig = typeConfig;
        this.rowNumber = rowNumber;
        this.properties = resource.getValueMap();
    }

    public String getPath() {
        return resource.getPath();
    }

    public String getTypePath() {
        return typeConfig.getPath();
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getRowNumberFormatted() {
        return String.format("%04d", rowNumber);
    }

    public String getMimeType() {
        Resource contentResource = resource.getChild("jcr:content");
        return contentResource != null ? contentResource.getValueMap().get("jcr:mimeType", "") : "";
    }

    public boolean isFolder() {
        String type = properties.get("jcr:primaryType", String.class);
        return "sling:OrderedFolder".equals(type) || "sling:Folder".equals(type) || "nt:folder".equals(type);
    }

    public String getTaxonomyString() {
        Resource contentResource = resource.getChild("jcr:content");
        if (contentResource == null) {
            return "";
        }

        Object taxonomy = contentResource.getValueMap().get("sling:taxonomy");
        if (taxonomy == null) {
            return "";
        }

        if (taxonomy instanceof String[]) {
            return String.join(",", (String[]) taxonomy);
        } else if (taxonomy instanceof String) {
            return (String) taxonomy;
        }
        return "";
    }

    public List<ColumnConfig> getColumnConfigs() {
        Resource columnsResource = typeConfig.getChild("columns");
        if (columnsResource != null) {
            return StreamSupport.stream(columnsResource.getChildren().spliterator(), false)
                    .map(ColumnConfig::new)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * Get page status for filtering (published, draft, scheduled).
     * @return page status or empty string
     */
    public String getPageStatus() {
        Resource contentResource = resource.getChild("jcr:content");
        if (contentResource == null) {
            return "";
        }
        // Check if published property exists
        boolean published = contentResource.getValueMap().get("published", false);
        return published ? "published" : "draft";
    }

    /**
     * Get template name for filtering.
     * @return template name or empty string
     */
    public String getTemplate() {
        Resource contentResource = resource.getChild("jcr:content");
        if (contentResource == null) {
            return "";
        }
        String resourceType = contentResource.getValueMap().get("sling:resourceType", "");
        // Extract template name from resource type path
        if (resourceType.contains("/")) {
            String[] parts = resourceType.split("/");
            return parts[parts.length - 1];
        }
        return resourceType;
    }

    /**
     * Get modified date category for filtering (today, week, month, year).
     * @return date category or empty string
     */
    public String getModifiedDate() {
        Resource contentResource = resource.getChild("jcr:content");
        if (contentResource == null) {
            return "";
        }

        java.util.Calendar modified = contentResource.getValueMap().get("jcr:lastModified", java.util.Calendar.class);
        if (modified == null) {
            return "";
        }

        java.util.Calendar now = java.util.Calendar.getInstance();
        long diffMillis = now.getTimeInMillis() - modified.getTimeInMillis();
        long diffDays = diffMillis / (1000 * 60 * 60 * 24);

        if (diffDays < 1) {
            return "today";
        } else if (diffDays < 7) {
            return "week";
        } else if (diffDays < 30) {
            return "month";
        } else if (diffDays < 365) {
            return "year";
        }
        return "";
    }
}
