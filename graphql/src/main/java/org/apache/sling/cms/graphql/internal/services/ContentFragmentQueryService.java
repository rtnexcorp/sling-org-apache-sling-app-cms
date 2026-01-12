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
package org.apache.sling.cms.graphql.internal.services;

import javax.jcr.query.Query;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.graphql.internal.dto.ContentFragmentDTO;
import org.apache.sling.cms.graphql.internal.dto.FieldValueDTO;
import org.apache.sling.cms.graphql.internal.dto.FragmentConnectionDTO;
import org.apache.sling.cms.graphql.internal.dto.PageInfoDTO;
import org.apache.sling.cms.schema.ContentSchema;
import org.apache.sling.cms.schema.SchemaManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for querying content fragments with filtering, sorting, and pagination.
 * Encapsulates JCR query generation and result transformation.
 */
@Component(service = ContentFragmentQueryService.class)
public class ContentFragmentQueryService {

    private static final Logger log = LoggerFactory.getLogger(ContentFragmentQueryService.class);

    private static final String FRAGMENTS_BASE_PATH = "/content/fragments";
    private static final String FRAGMENT_RESOURCE_TYPE = "sling-cms/components/cms/fragment";
    private static final String SCHEMA_ID_PROPERTY = "schemaId";
    private static final String FIELDS_NODE = "fields";
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    private static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat ISO_DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @Reference
    private SchemaManager schemaManager;

    /**
     * Find a single fragment by its path/ID.
     *
     * @param resolver the resource resolver
     * @param fragmentId the fragment path
     * @return the fragment DTO, or null if not found
     */
    public ContentFragmentDTO getFragmentById(ResourceResolver resolver, String fragmentId) {
        if (fragmentId == null || fragmentId.isEmpty()) {
            return null;
        }

        Resource fragmentResource = resolver.getResource(fragmentId);
        if (fragmentResource == null) {
            log.debug("Fragment not found: {}", fragmentId);
            return null;
        }

        ValueMap properties = fragmentResource.getValueMap();
        String resourceType = properties.get("sling:resourceType", String.class);
        if (!FRAGMENT_RESOURCE_TYPE.equals(resourceType)) {
            log.debug("Resource is not a content fragment: {}", fragmentId);
            return null;
        }

        return mapResourceToDTO(fragmentResource);
    }

    /**
     * List fragments by schema type with filtering, sorting, and pagination.
     *
     * @param resolver the resource resolver
     * @param schemaType schema type filter (e.g., "article", "product")
     * @param pathFilter optional path prefix filter
     * @param filters field value filters
     * @param sortField field to sort by
     * @param sortDirection ASC or DESC
     * @param limit max results (default 10, max 100)
     * @param offset offset for pagination
     * @param cursor cursor for cursor-based pagination
     * @return paginated connection of fragments
     */
    public FragmentConnectionDTO listFragments(
            ResourceResolver resolver,
            String schemaType,
            String pathFilter,
            List<Map<String, String>> filters,
            String sortField,
            String sortDirection,
            Integer limit,
            Integer offset,
            String cursor) {

        String basePath = (pathFilter != null && !pathFilter.isEmpty()) ? pathFilter : FRAGMENTS_BASE_PATH;

        String query = buildQuery(schemaType, basePath, filters);
        log.debug("Executing JCR query: {}", query);

        List<Resource> allResults = new ArrayList<>();
        Iterator<Resource> resultIterator = resolver.findResources(query, Query.JCR_SQL2);
        while (resultIterator.hasNext()) {
            allResults.add(resultIterator.next());
        }

        // Apply sorting
        if (sortField != null && !sortField.isEmpty()) {
            boolean ascending = !"DESC".equalsIgnoreCase(sortDirection);
            allResults = sortFragments(allResults, sortField, ascending);
        }

        int totalCount = allResults.size();

        // Calculate pagination
        int effectiveLimit = calculateLimit(limit);
        int startOffset = calculateStartOffset(offset, cursor);
        int endOffset = Math.min(startOffset + effectiveLimit, totalCount);

        // Get page slice
        List<Resource> pageResults =
                (startOffset < totalCount) ? allResults.subList(startOffset, endOffset) : Collections.emptyList();

        // Map to DTOs
        List<ContentFragmentDTO> nodes =
                pageResults.stream().map(this::mapResourceToDTO).collect(Collectors.toList());

        // Build page info
        PageInfoDTO pageInfo = new PageInfoDTO(
                endOffset < totalCount,
                startOffset > 0,
                startOffset > 0 ? encodeCursor(startOffset) : null,
                pageResults.isEmpty() ? null : encodeCursor(endOffset - 1));

        return new FragmentConnectionDTO(nodes, pageInfo, totalCount);
    }

    /**
     * Full-text search across fragments.
     *
     * @param resolver the resource resolver
     * @param searchTerm the search term
     * @param schemaType optional schema type filter
     * @param limit max results
     * @param offset pagination offset
     * @return paginated connection of matching fragments
     */
    public FragmentConnectionDTO searchFragments(
            ResourceResolver resolver, String searchTerm, String schemaType, Integer limit, Integer offset) {

        String query = buildSearchQuery(searchTerm, schemaType);
        log.debug("Executing search query: {}", query);

        List<Resource> allResults = new ArrayList<>();
        Iterator<Resource> resultIterator = resolver.findResources(query, Query.JCR_SQL2);
        while (resultIterator.hasNext()) {
            allResults.add(resultIterator.next());
        }

        int totalCount = allResults.size();

        // Calculate pagination
        int effectiveLimit = calculateLimit(limit);
        int startOffset = (offset != null && offset >= 0) ? offset : 0;
        int endOffset = Math.min(startOffset + effectiveLimit, totalCount);

        // Get page slice
        List<Resource> pageResults =
                (startOffset < totalCount) ? allResults.subList(startOffset, endOffset) : Collections.emptyList();

        // Map to DTOs
        List<ContentFragmentDTO> nodes =
                pageResults.stream().map(this::mapResourceToDTO).collect(Collectors.toList());

        // Build page info
        PageInfoDTO pageInfo = new PageInfoDTO(
                endOffset < totalCount,
                startOffset > 0,
                startOffset > 0 ? encodeCursor(startOffset) : null,
                pageResults.isEmpty() ? null : encodeCursor(endOffset - 1));

        return new FragmentConnectionDTO(nodes, pageInfo, totalCount);
    }

    /**
     * Build JCR-SQL2 query for listing fragments.
     */
    private String buildQuery(String schemaType, String basePath, List<Map<String, String>> filters) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM [nt:unstructured] AS f ");
        query.append("WHERE ISDESCENDANTNODE(f, '")
                .append(escapeJcrValue(basePath))
                .append("') ");
        query.append("AND f.[sling:resourceType] = '")
                .append(FRAGMENT_RESOURCE_TYPE)
                .append("' ");

        if (schemaType != null && !schemaType.isEmpty()) {
            query.append("AND f.[")
                    .append(SCHEMA_ID_PROPERTY)
                    .append("] = '")
                    .append(escapeJcrValue(schemaType))
                    .append("' ");
        }

        // Apply field filters
        if (filters != null && !filters.isEmpty()) {
            for (Map<String, String> filter : filters) {
                String filterClause = buildFilterClause(filter);
                if (filterClause != null) {
                    query.append("AND ").append(filterClause).append(" ");
                }
            }
        }

        return query.toString();
    }

    /**
     * Build a filter clause for a single filter.
     */
    private String buildFilterClause(Map<String, String> filter) {
        String fieldName = filter.get("fieldName");
        String operator = filter.get("operator");
        String value = filter.get("value");

        if (fieldName == null || operator == null) {
            return null;
        }

        String escapedField = "f.[" + escapeJcrValue(fieldName) + "]";
        String escapedValue = escapeJcrValue(value);

        switch (operator.toUpperCase()) {
            case "EQ":
                return escapedField + " = '" + escapedValue + "'";
            case "NE":
                return escapedField + " <> '" + escapedValue + "'";
            case "CONTAINS":
                return "CONTAINS(" + escapedField + ", '" + escapedValue + "')";
            case "GT":
                return escapedField + " > '" + escapedValue + "'";
            case "GTE":
                return escapedField + " >= '" + escapedValue + "'";
            case "LT":
                return escapedField + " < '" + escapedValue + "'";
            case "LTE":
                return escapedField + " <= '" + escapedValue + "'";
            case "IS_NULL":
                return escapedField + " IS NULL";
            case "IS_NOT_NULL":
                return escapedField + " IS NOT NULL";
            default:
                log.warn("Unknown filter operator: {}", operator);
                return null;
        }
    }

    /**
     * Build JCR-SQL2 query for full-text search.
     */
    private String buildSearchQuery(String searchTerm, String schemaType) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM [nt:unstructured] AS f ");
        query.append("WHERE ISDESCENDANTNODE(f, '").append(FRAGMENTS_BASE_PATH).append("') ");
        query.append("AND f.[sling:resourceType] = '")
                .append(FRAGMENT_RESOURCE_TYPE)
                .append("' ");

        if (schemaType != null && !schemaType.isEmpty()) {
            query.append("AND f.[")
                    .append(SCHEMA_ID_PROPERTY)
                    .append("] = '")
                    .append(escapeJcrValue(schemaType))
                    .append("' ");
        }

        if (searchTerm != null && !searchTerm.isEmpty()) {
            String escapedTerm = escapeJcrContainsTerm(searchTerm);
            query.append("AND CONTAINS(f.*, '").append(escapedTerm).append("') ");
        }

        return query.toString();
    }

    /**
     * Sort fragments by a field.
     */
    private List<Resource> sortFragments(List<Resource> fragments, String sortField, boolean ascending) {
        Comparator<Resource> comparator = (r1, r2) -> {
            ValueMap vm1 = r1.getValueMap();
            ValueMap vm2 = r2.getValueMap();
            Object v1 = vm1.get(sortField);
            Object v2 = vm2.get(sortField);

            if (v1 == null && v2 == null) {
                return 0;
            }
            if (v1 == null) {
                return ascending ? -1 : 1;
            }
            if (v2 == null) {
                return ascending ? 1 : -1;
            }

            if (v1 instanceof Comparable && v2 instanceof Comparable) {
                @SuppressWarnings("unchecked")
                int result = ((Comparable<Object>) v1).compareTo(v2);
                return ascending ? result : -result;
            }

            return String.valueOf(v1).compareTo(String.valueOf(v2)) * (ascending ? 1 : -1);
        };

        return fragments.stream().sorted(comparator).collect(Collectors.toList());
    }

    /**
     * Transform a Resource to ContentFragmentDTO.
     */
    private ContentFragmentDTO mapResourceToDTO(Resource fragmentResource) {
        ContentFragmentDTO dto = new ContentFragmentDTO();
        ValueMap properties = fragmentResource.getValueMap();

        dto.setId(fragmentResource.getPath());
        dto.setName(fragmentResource.getName());
        dto.setSchemaId(properties.get(SCHEMA_ID_PROPERTY, String.class));
        dto.setTitle(properties.get("jcr:title", String.class));

        // Timestamps
        Calendar created = properties.get("jcr:created", Calendar.class);
        if (created != null) {
            dto.setCreated(formatDateTime(created));
        }

        Calendar modified = properties.get("jcr:lastModified", Calendar.class);
        if (modified != null) {
            dto.setModified(formatDateTime(modified));
        }

        dto.setCreatedBy(properties.get("jcr:createdBy", String.class));
        dto.setModifiedBy(properties.get("jcr:lastModifiedBy", String.class));

        // Extract fields
        List<FieldValueDTO> fields = extractFields(fragmentResource);
        dto.setFields(fields);

        return dto;
    }

    /**
     * Extract field values from fragment using the fields child node for type info.
     */
    private List<FieldValueDTO> extractFields(Resource fragment) {
        List<FieldValueDTO> fields = new ArrayList<>();
        ValueMap properties = fragment.getValueMap();
        Resource fieldsNode = fragment.getChild(FIELDS_NODE);

        if (fieldsNode == null) {
            // No fields metadata - try to use schema
            String schemaId = properties.get(SCHEMA_ID_PROPERTY, String.class);
            if (schemaId != null) {
                ContentSchema schema = schemaManager.getSchema(fragment, schemaId);
                if (schema != null) {
                    for (var schemaField : schema.getFields()) {
                        String fieldName = schemaField.getName();
                        Object value = properties.get(fieldName);
                        String type = schemaField.getType().name();
                        boolean multiple = schemaField.isMultiple();
                        fields.add(createFieldDTO(fieldName, value, type, multiple));
                    }
                }
            }
            return fields;
        }

        // Use fields node for type metadata
        for (Resource fieldNode : fieldsNode.getChildren()) {
            String fieldName = fieldNode.getName();
            ValueMap fieldProps = fieldNode.getValueMap();
            String fieldType = fieldProps.get("fieldType", "STRING");
            boolean multiple = fieldProps.get("multiple", false);

            Object value = properties.get(fieldName);
            fields.add(createFieldDTO(fieldName, value, fieldType, multiple));
        }

        return fields;
    }

    /**
     * Create a FieldValueDTO from a value and its type.
     */
    private FieldValueDTO createFieldDTO(String name, Object value, String type, boolean multiple) {
        FieldValueDTO dto = new FieldValueDTO();
        dto.setName(name);
        dto.setType(type);
        dto.setMultiple(multiple);

        if (value == null) {
            dto.setValue(null);
            dto.setValues(null);
            return dto;
        }

        if (multiple || value.getClass().isArray()) {
            List<String> values = serializeMultiValue(value, type);
            dto.setValues(values);
            dto.setValue(null);
        } else {
            dto.setValue(serializeValue(value, type));
            dto.setValues(null);
        }

        return dto;
    }

    /**
     * Serialize a single value based on type.
     */
    private String serializeValue(Object value, String type) {
        if (value == null) {
            return null;
        }

        switch (type.toUpperCase()) {
            case "DATE":
                if (value instanceof Calendar) {
                    synchronized (ISO_DATE_FORMAT) {
                        return ISO_DATE_FORMAT.format(((Calendar) value).getTime());
                    }
                }
                return String.valueOf(value);

            case "DATETIME":
                if (value instanceof Calendar) {
                    synchronized (ISO_DATETIME_FORMAT) {
                        return ISO_DATETIME_FORMAT.format(((Calendar) value).getTime());
                    }
                }
                return String.valueOf(value);

            case "BOOLEAN":
                if (value instanceof Boolean) {
                    return value.toString();
                }
                return String.valueOf(value);

            case "INTEGER":
            case "DECIMAL":
                return String.valueOf(value);

            default:
                return String.valueOf(value);
        }
    }

    /**
     * Serialize multi-valued field.
     */
    private List<String> serializeMultiValue(Object value, String type) {
        if (value == null) {
            return Collections.emptyList();
        }

        if (value instanceof String[]) {
            return Arrays.asList((String[]) value);
        }

        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            return Arrays.stream(array).map(v -> serializeValue(v, type)).collect(Collectors.toList());
        }

        // Single value treated as single-element array for multi-valued fields
        // Also handle comma-separated string (like tags)
        String strValue = String.valueOf(value);
        if (strValue.contains(",")) {
            return Arrays.stream(strValue.split(",")).map(String::trim).collect(Collectors.toList());
        }

        return Collections.singletonList(serializeValue(value, type));
    }

    /**
     * Format a Calendar to ISO-8601 datetime string.
     */
    private String formatDateTime(Calendar calendar) {
        synchronized (ISO_DATETIME_FORMAT) {
            return ISO_DATETIME_FORMAT.format(calendar.getTime());
        }
    }

    /**
     * Calculate effective limit with bounds.
     */
    private int calculateLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * Calculate start offset from offset or cursor.
     */
    private int calculateStartOffset(Integer offset, String cursor) {
        if (cursor != null && !cursor.isEmpty()) {
            return decodeCursor(cursor) + 1; // Start after cursor position
        }
        if (offset != null && offset >= 0) {
            return offset;
        }
        return 0;
    }

    /**
     * Encode an offset as a Base64 cursor.
     */
    private String encodeCursor(int offset) {
        String cursorData = "cursor:" + offset;
        return Base64.getEncoder().encodeToString(cursorData.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decode a Base64 cursor to an offset.
     */
    private int decodeCursor(String cursor) {
        try {
            String decoded = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
            if (decoded.startsWith("cursor:")) {
                return Integer.parseInt(decoded.substring(7));
            }
        } catch (Exception e) {
            log.warn("Invalid cursor format: {}", cursor);
        }
        return 0;
    }

    /**
     * Escape a user-supplied full-text search term for usage in a JCR-SQL2 CONTAINS() clause.
     *
     * <p>
     * This method is intentionally conservative:
     * <ul>
     * <li>Normalizes whitespace</li>
     * <li>Escapes backslashes and quotes used by the CONTAINS grammar</li>
     * <li>Doubles single quotes for safe insertion into the SQL2 string literal</li>
     * </ul>
     *
     * @param term the raw user input
     * @return an escaped term safe to embed in a single-quoted CONTAINS() string literal
     */
    private String escapeJcrContainsTerm(String term) {
        if (term == null) {
            return "";
        }

        String cleaned = term.trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            return "";
        }

        // Escape Lucene query special characters commonly supported by JCR repositories.
        // We escape '*' and '?' as well to avoid unintended wildcard semantics.
        String escaped = cleaned.replace("\\\\", "\\\\\\\\")
                .replace("\"", "\\\\\"")
                .replace("*", "\\\\*")
                .replace("?", "\\\\?")
                .replace("[", "\\\\[")
                .replace("]", "\\\\]")
                .replace("{", "\\\\{")
                .replace("}", "\\\\}")
                .replace("(", "\\\\(")
                .replace(")", "\\\\)")
                .replace(":", "\\\\:")
                .replace("^", "\\\\^")
                .replace("~", "\\\\~")
                .replace("!", "\\\\!")
                .replace("+", "\\\\+")
                .replace("-", "\\\\-")
                .replace("|", "\\\\|")
                .replace("&", "\\\\&")
                .replace("/", "\\\\/");

        // SQL2 string literal escaping.
        return escaped.replace("'", "''");
    }

    /**
     * Escape a value for use in JCR-SQL2 query.
     */
    private String escapeJcrValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }
}
