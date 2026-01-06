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
package org.apache.sling.cms.workflow.internal.expression;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sling.cms.workflow.WorkflowException;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple expression evaluator for BPMN condition expressions.
 *
 * <p>Supports basic ${variable} syntax and simple comparisons:</p>
 * <ul>
 *   <li>${approved == true}</li>
 *   <li>${count > 5}</li>
 *   <li>${status == 'active'}</li>
 * </ul>
 *
 * <p>For complex expressions, use Flowable engine which supports full JUEL.</p>
 */
@Component(service = SimpleExpressionEvaluator.class)
public class SimpleExpressionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(SimpleExpressionEvaluator.class);

    // Pattern to match ${variable == value} or ${variable != value}
    // NOTE: Order matters! Two-character operators (==, !=, >=, <=) must come before single-character ones (>, <)
    private static final Pattern COMPARISON_PATTERN =
            Pattern.compile("\\$\\{\\s*(\\w+)\\s*(==|!=|>=|<=|>|<)\\s*(.+?)\\s*\\}");

    // Pattern to match just ${variable}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{\\s*(\\w+)\\s*\\}");

    /**
     * Evaluate a condition expression against process variables.
     *
     * @param expression Condition expression (e.g., "${approved == true}")
     * @param variables Process variables
     * @return true if condition is met
     */
    public boolean evaluate(String expression, Map<String, Object> variables) {
        if (expression == null || expression.trim().isEmpty()) {
            log.warn("Empty expression, defaulting to true");
            return true;
        }

        try {
            expression = expression.trim();

            // Try comparison pattern first
            Matcher comparisonMatcher = COMPARISON_PATTERN.matcher(expression);
            if (comparisonMatcher.matches()) {
                return evaluateComparison(
                        comparisonMatcher.group(1), // variable name
                        comparisonMatcher.group(2), // operator
                        comparisonMatcher.group(3), // expected value
                        variables);
            }

            // Try simple variable pattern
            Matcher variableMatcher = VARIABLE_PATTERN.matcher(expression);
            if (variableMatcher.matches()) {
                String varName = variableMatcher.group(1);
                Object value = variables.get(varName);
                // Handle string "true"/"false" values
                if (value instanceof String) {
                    return Boolean.parseBoolean((String) value);
                }
                return Boolean.TRUE.equals(value);
            }

            log.warn("Unsupported expression format: {}, defaulting to true", expression);
            return true;

        } catch (Exception e) {
            log.error("Failed to evaluate expression: " + expression, e);
            throw new WorkflowException("Expression evaluation failed: " + expression, e);
        }
    }

    /**
     * Evaluate a comparison expression.
     */
    private boolean evaluateComparison(
            String varName, String operator, String expectedValue, Map<String, Object> variables) {
        Object actualValue = variables.get(varName);

        // Parse actual value if it's a string (common case when variables come from map)
        if (actualValue instanceof String) {
            actualValue = parseValue((String) actualValue);
        }

        log.debug("Evaluating: {} {} {} (actual value: {})", varName, operator, expectedValue, actualValue);

        // Parse expected value
        Object expected = parseValue(expectedValue);

        // Handle null cases
        if (actualValue == null) {
            return "null".equals(expectedValue) || "!=".equals(operator);
        }

        // Perform comparison based on operator
        switch (operator) {
            case "==":
                return compare(actualValue, expected) == 0;
            case "!=":
                return compare(actualValue, expected) != 0;
            case ">":
                return compare(actualValue, expected) > 0;
            case "<":
                return compare(actualValue, expected) < 0;
            case ">=":
                return compare(actualValue, expected) >= 0;
            case "<=":
                return compare(actualValue, expected) <= 0;
            default:
                log.warn("Unknown operator: {}", operator);
                return false;
        }
    }

    /**
     * Parse string value to appropriate type.
     */
    private Object parseValue(String value) {
        value = value.trim();

        // Boolean
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }

        // Null
        if ("null".equalsIgnoreCase(value)) {
            return null;
        }

        // String (remove quotes)
        if (value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }

        // Try number
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            // Not a number, return as string
            return value;
        }
    }

    /**
     * Compare two values.
     *
     * @return negative if a < b, 0 if a == b, positive if a > b
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compare(Object a, Object b) {
        // Same reference or both null
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }

        // Boolean comparison
        if (a instanceof Boolean && b instanceof Boolean) {
            return ((Boolean) a).compareTo((Boolean) b);
        }

        // Number comparison
        if (a instanceof Number && b instanceof Number) {
            double aNum = ((Number) a).doubleValue();
            double bNum = ((Number) b).doubleValue();
            return Double.compare(aNum, bNum);
        }

        // String comparison
        if (a instanceof Comparable && b instanceof Comparable) {
            try {
                return ((Comparable) a).compareTo(b);
            } catch (ClassCastException e) {
                // Fall through to string comparison
            }
        }

        // Default to string comparison
        return a.toString().compareTo(b.toString());
    }
}
