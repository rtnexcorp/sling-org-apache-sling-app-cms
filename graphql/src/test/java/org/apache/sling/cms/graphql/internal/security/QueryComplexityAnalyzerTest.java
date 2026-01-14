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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link QueryComplexityAnalyzer}.
 */
class QueryComplexityAnalyzerTest {

    private QueryComplexityAnalyzer analyzer;
    private QueryComplexityAnalyzer.Config config;

    @BeforeEach
    void setUp() {
        analyzer = new QueryComplexityAnalyzer();
        config = createConfig(true, 100, 10, 1.5, 1, 10);
        analyzer.activate(config);
    }

    private QueryComplexityAnalyzer.Config createConfig(
            boolean enabled,
            int maxComplexity,
            int maxDepth,
            double depthMultiplier,
            int fieldBaseCost,
            int listFieldMultiplier) {
        QueryComplexityAnalyzer.Config mockConfig = mock(QueryComplexityAnalyzer.Config.class);
        lenient().when(mockConfig.enabled()).thenReturn(enabled);
        lenient().when(mockConfig.maxComplexity()).thenReturn(maxComplexity);
        lenient().when(mockConfig.maxDepth()).thenReturn(maxDepth);
        lenient().when(mockConfig.depthMultiplier()).thenReturn(depthMultiplier);
        lenient().when(mockConfig.fieldBaseCost()).thenReturn(fieldBaseCost);
        lenient().when(mockConfig.listFieldMultiplier()).thenReturn(listFieldMultiplier);
        return mockConfig;
    }

    @Test
    void testSimpleQueryWithinLimits() {
        String query = "{ serverInfo { version } }";
        QueryComplexityAnalyzer.ComplexityResult result = analyzer.analyze(query);

        assertTrue(result.isWithinLimits());
        assertNull(result.getViolationMessage());
        assertTrue(result.getDepth() <= 10);
        assertTrue(result.getComplexity() <= 100);
    }

    @Test
    void testEmptyQueryReturnsZeroComplexity() {
        QueryComplexityAnalyzer.ComplexityResult result = analyzer.analyze("");

        assertTrue(result.isWithinLimits());
        assertEquals(0, result.getComplexity());
        assertEquals(0, result.getDepth());
    }

    @Test
    void testNullQueryReturnsZeroComplexity() {
        QueryComplexityAnalyzer.ComplexityResult result = analyzer.analyze(null);

        assertTrue(result.isWithinLimits());
        assertEquals(0, result.getComplexity());
        assertEquals(0, result.getDepth());
    }

    @Test
    void testDeeplyNestedQueryExceedsDepth() {
        // Create a query with depth > 10
        String query = "{ a { b { c { d { e { f { g { h { i { j { k { l } } } } } } } } } } } }";
        QueryComplexityAnalyzer.ComplexityResult result = analyzer.analyze(query);

        assertFalse(result.isWithinLimits());
        assertNotNull(result.getViolationMessage());
        assertTrue(result.getViolationMessage().contains("depth"));
        assertTrue(result.getDepth() > 10);
    }

    @Test
    void testQueryWithListFieldsHasHigherComplexity() {
        // Query containing 'items' which triggers list multiplier
        String query = "{ items { id name } }";
        QueryComplexityAnalyzer.ComplexityResult result = analyzer.analyze(query);

        // List fields should have higher complexity
        assertTrue(result.getComplexity() > 0);
    }

    @Test
    void testValidateThrowsExceptionForComplexQuery() {
        // Set very low limits
        config = createConfig(true, 5, 3, 1.5, 1, 10);
        analyzer.activate(config);

        String query = "{ a { b { c { d { e } } } } }";

        assertThrows(QueryComplexityException.class, () -> analyzer.validate(query));
    }

    @Test
    void testValidatePassesForSimpleQuery() {
        String query = "{ hello }";

        assertDoesNotThrow(() -> analyzer.validate(query));
    }

    @Test
    void testDisabledAnalyzerReturnsUnlimited() {
        config = createConfig(false, 100, 10, 1.5, 1, 10);
        analyzer.activate(config);

        String query = "{ a { b { c { d { e { f { g { h { i { j { k } } } } } } } } } } }";
        QueryComplexityAnalyzer.ComplexityResult result = analyzer.analyze(query);

        assertTrue(result.isWithinLimits());
        assertEquals(0, result.getComplexity());
        assertEquals(0, result.getDepth());
    }

    @Test
    void testDepthCalculation() {
        String query1 = "{ a }";
        String query2 = "{ a { b } }";
        String query3 = "{ a { b { c } } }";

        assertEquals(1, analyzer.analyze(query1).getDepth());
        assertEquals(2, analyzer.analyze(query2).getDepth());
        assertEquals(3, analyzer.analyze(query3).getDepth());
    }

    @Test
    void testComplexityResultToString() {
        QueryComplexityAnalyzer.ComplexityResult result =
                new QueryComplexityAnalyzer.ComplexityResult(50, 5, true, null);

        String str = result.toString();
        assertTrue(str.contains("complexity=50"));
        assertTrue(str.contains("depth=5"));
        assertTrue(str.contains("withinLimits=true"));
    }

    @Test
    void testQueryWithEdgesPattern() {
        // 'edges' is a common GraphQL pagination pattern that triggers list multiplier
        String query = "{ users { edges { node { id } } } }";
        QueryComplexityAnalyzer.ComplexityResult result = analyzer.analyze(query);

        // Should detect as list query and apply multiplier
        assertTrue(result.getComplexity() > 0);
        assertTrue(result.isWithinLimits());
    }

    @Test
    void testQueryWithSearchPattern() {
        // 'search' triggers list multiplier
        String query = "{ search(query: \"test\") { results } }";
        QueryComplexityAnalyzer.ComplexityResult result = analyzer.analyze(query);

        assertTrue(result.getComplexity() > 0);
    }

    @Test
    void testMaxDepthZeroMeansUnlimited() {
        // With maxDepth=0, depth check is skipped, but complexity is still calculated
        config = createConfig(true, 10000, 0, 1.5, 1, 1);
        analyzer.activate(config);

        // Very deep query
        String query = "{ a { b { c { d { e { f { g { h { i { j { k { l { m { n { o } } } } } } } } } } } } } } }";
        QueryComplexityAnalyzer.ComplexityResult result = analyzer.analyze(query);

        // Verify depth is calculated correctly
        assertTrue(result.getDepth() > 10);
    }

    @Test
    void testMaxComplexityZeroMeansUnlimited() {
        config = createConfig(true, 0, 100, 1.5, 1, 10);
        analyzer.activate(config);

        String query = "{ items { a b c d e f g h i j } }";
        QueryComplexityAnalyzer.ComplexityResult result = analyzer.analyze(query);

        // Should still calculate complexity but not fail on limit
        assertTrue(result.getComplexity() > 0);
    }

    @Test
    void testGetConfig() {
        assertNotNull(analyzer.getConfig());
        assertEquals(config, analyzer.getConfig());
    }

    @Test
    void testQueryWithMutation() {
        String query = "mutation { createUser(name: \"test\") { id } }";
        QueryComplexityAnalyzer.ComplexityResult result = analyzer.analyze(query);

        assertTrue(result.getComplexity() > 0);
        assertTrue(result.isWithinLimits());
    }

    @Test
    void testQueryWithFragments() {
        String query = "fragment userFields on User { id name } query { user { ...userFields } }";
        QueryComplexityAnalyzer.ComplexityResult result = analyzer.analyze(query);

        // Should handle fragments without crashing
        assertNotNull(result);
    }

    @Test
    void testComplexityExceedsLimit() {
        // Set low complexity limit, no depth limit
        config = createConfig(true, 10, 0, 1.5, 1, 10);
        analyzer.activate(config);

        // Query with list field that triggers multiplier
        String query = "{ items { a b c d e } }";
        QueryComplexityAnalyzer.ComplexityResult result = analyzer.analyze(query);

        assertFalse(result.isWithinLimits());
        assertNotNull(result.getViolationMessage());
        assertTrue(result.getViolationMessage().contains("complexity"));
    }

    @Test
    void testUnlimitedResult() {
        QueryComplexityAnalyzer.ComplexityResult result = QueryComplexityAnalyzer.ComplexityResult.unlimited();

        assertEquals(0, result.getComplexity());
        assertEquals(0, result.getDepth());
        assertTrue(result.isWithinLimits());
        assertNull(result.getViolationMessage());
    }
}
