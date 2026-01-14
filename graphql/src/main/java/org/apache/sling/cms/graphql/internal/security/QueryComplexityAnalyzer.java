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
package org.apache.sling.cms.graphql.internal.security;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analyzes GraphQL query complexity to prevent denial-of-service attacks.
 * Calculates complexity based on query depth, field count, and nesting level.
 *
 * <p>Complexity is calculated as:
 * <ul>
 *   <li>Each field adds 1 to complexity</li>
 *   <li>Each nesting level multiplies complexity by depth multiplier</li>
 *   <li>List fields add additional complexity based on estimated result size</li>
 * </ul>
 *
 * <p>Example configuration:
 * <pre>
 * {
 *   "enabled": true,
 *   "maxComplexity": 100,
 *   "maxDepth": 10,
 *   "depthMultiplier": 1.5
 * }
 * </pre>
 */
@Component(service = QueryComplexityAnalyzer.class)
@Designate(ocd = QueryComplexityAnalyzer.Config.class)
public class QueryComplexityAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(QueryComplexityAnalyzer.class);

    @ObjectClassDefinition(name = "Sling CMS GraphQL Query Complexity Configuration")
    public @interface Config {

        @AttributeDefinition(
                name = "Enable Complexity Analysis",
                description = "Enable query complexity analysis and limiting")
        boolean enabled() default true;

        @AttributeDefinition(
                name = "Maximum Complexity",
                description = "Maximum allowed query complexity score (0 = unlimited)")
        int maxComplexity() default 100;

        @AttributeDefinition(
                name = "Maximum Depth",
                description = "Maximum allowed query nesting depth (0 = unlimited)")
        int maxDepth() default 10;

        @AttributeDefinition(
                name = "Depth Multiplier",
                description = "Multiplier applied to complexity for each nesting level")
        double depthMultiplier() default 1.5;

        @AttributeDefinition(name = "Field Base Cost", description = "Base complexity cost per field")
        int fieldBaseCost() default 1;

        @AttributeDefinition(name = "List Field Multiplier", description = "Multiplier for fields that return lists")
        int listFieldMultiplier() default 10;
    }

    private volatile Config config;

    @Activate
    protected void activate(Config config) {
        this.config = config;
        log.info(
                "Query Complexity Analyzer activated - Enabled: {}, Max Complexity: {}, Max Depth: {}",
                config.enabled(),
                config.maxComplexity(),
                config.maxDepth());
    }

    /**
     * Analyzes a GraphQL query string and returns complexity metrics.
     *
     * @param query the GraphQL query string
     * @return complexity analysis result
     */
    public ComplexityResult analyze(String query) {
        if (!config.enabled()) {
            return ComplexityResult.unlimited();
        }

        if (query == null || query.trim().isEmpty()) {
            return new ComplexityResult(0, 0, true, null);
        }

        int depth = calculateDepth(query);
        int complexity = calculateComplexity(query, depth);

        boolean withinLimits = true;
        String violationMessage = null;

        if (config.maxDepth() > 0 && depth > config.maxDepth()) {
            withinLimits = false;
            violationMessage =
                    String.format("Query depth %d exceeds maximum allowed depth of %d", depth, config.maxDepth());
        } else if (config.maxComplexity() > 0 && complexity > config.maxComplexity()) {
            withinLimits = false;
            violationMessage = String.format(
                    "Query complexity %d exceeds maximum allowed complexity of %d", complexity, config.maxComplexity());
        }

        log.debug(
                "Query complexity analysis - Depth: {}, Complexity: {}, Within Limits: {}",
                depth,
                complexity,
                withinLimits);

        return new ComplexityResult(complexity, depth, withinLimits, violationMessage);
    }

    /**
     * Validates that a query is within complexity limits.
     *
     * @param query the GraphQL query string
     * @throws QueryComplexityException if query exceeds complexity limits
     */
    public void validate(String query) throws QueryComplexityException {
        ComplexityResult result = analyze(query);
        if (!result.isWithinLimits()) {
            log.warn("Query complexity violation: {}", result.getViolationMessage());
            throw new QueryComplexityException(result.getViolationMessage(), result);
        }
    }

    /**
     * Calculates the nesting depth of a GraphQL query.
     */
    private int calculateDepth(String query) {
        int maxDepth = 0;
        int currentDepth = 0;

        for (char c : query.toCharArray()) {
            if (c == '{') {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
            } else if (c == '}') {
                currentDepth--;
            }
        }

        return maxDepth;
    }

    /**
     * Calculates the complexity score of a GraphQL query.
     */
    private int calculateComplexity(String query, int depth) {
        int fieldCount = countFields(query);
        double baseComplexity = fieldCount * config.fieldBaseCost();

        // Apply depth multiplier
        double depthFactor = Math.pow(config.depthMultiplier(), Math.max(0, depth - 1));
        double complexity = baseComplexity * depthFactor;

        // Check for list-returning fields and apply an additional cost.
        // Multiplying the entire query complexity makes common pagination patterns (edges/nodes)
        // exceed limits too aggressively.
        if (containsListFields(query)) {
            complexity += config.listFieldMultiplier();
        }

        return (int) Math.ceil(complexity);
    }

    /**
     * Counts the number of fields in a query.
     */
    private int countFields(String query) {
        // Remove query wrapper and count field names
        String cleaned = query.replaceAll("[{}()\\[\\]]", " ")
                .replaceAll("query|mutation|subscription", " ")
                .replaceAll("\"[^\"]*\"", " ") // Remove string literals
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isEmpty()) {
            return 0;
        }

        // Count words that look like field names (alphanumeric, starting with letter)
        int count = 0;
        for (String token : cleaned.split("\\s+")) {
            if (token.matches("^[a-zA-Z][a-zA-Z0-9_]*$") && !isKeyword(token)) {
                count++;
            }
        }

        return Math.max(1, count);
    }

    /**
     * Checks if a token is a GraphQL keyword.
     */
    private boolean isKeyword(String token) {
        return token.equals("true")
                || token.equals("false")
                || token.equals("null")
                || token.equals("on")
                || token.equals("fragment");
    }

    /**
     * Checks if the query contains fields that return lists.
     */
    private boolean containsListFields(String query) {
        // Common list field patterns
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("list")
                || lowerQuery.contains("items")
                || lowerQuery.contains("edges")
                || lowerQuery.contains("nodes")
                || lowerQuery.contains("all")
                || lowerQuery.contains("search")
                || lowerQuery.contains("filter");
    }

    /**
     * Returns the current configuration.
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Result of query complexity analysis.
     */
    public static class ComplexityResult {
        private final int complexity;
        private final int depth;
        private final boolean withinLimits;
        private final String violationMessage;

        public ComplexityResult(int complexity, int depth, boolean withinLimits, String violationMessage) {
            this.complexity = complexity;
            this.depth = depth;
            this.withinLimits = withinLimits;
            this.violationMessage = violationMessage;
        }

        public static ComplexityResult unlimited() {
            return new ComplexityResult(0, 0, true, null);
        }

        public int getComplexity() {
            return complexity;
        }

        public int getDepth() {
            return depth;
        }

        public boolean isWithinLimits() {
            return withinLimits;
        }

        public String getViolationMessage() {
            return violationMessage;
        }

        @Override
        public String toString() {
            return String.format(
                    "ComplexityResult{complexity=%d, depth=%d, withinLimits=%s, violation='%s'}",
                    complexity, depth, withinLimits, violationMessage);
        }
    }
}
