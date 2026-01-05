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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for SimpleExpressionEvaluator.
 */
class SimpleExpressionEvaluatorTest {

    private SimpleExpressionEvaluator evaluator;
    private Map<String, Object> variables;

    @BeforeEach
    void setUp() {
        evaluator = new SimpleExpressionEvaluator();
        variables = new HashMap<>();
    }

    @Test
    void testBooleanEquality() {
        variables.put("approved", "true");
        assertTrue(evaluator.evaluate("${approved == true}", variables));

        variables.put("approved", "false");
        assertFalse(evaluator.evaluate("${approved == true}", variables));
    }

    @Test
    void testStringEquality() {
        variables.put("status", "active");
        assertTrue(evaluator.evaluate("${status == 'active'}", variables));
        assertFalse(evaluator.evaluate("${status == 'inactive'}", variables));
    }

    @Test
    void testNumericComparison() {
        variables.put("count", "10");
        assertTrue(evaluator.evaluate("${count > 5}", variables));
        assertFalse(evaluator.evaluate("${count < 5}", variables));
        assertTrue(evaluator.evaluate("${count >= 10}", variables));
    }

    @Test
    void testSimpleVariableAccess() {
        variables.put("approved", "true");
        assertTrue(evaluator.evaluate("${approved}", variables));

        variables.put("approved", "false");
        assertFalse(evaluator.evaluate("${approved}", variables));
    }
}
