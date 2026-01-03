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
package org.apache.sling.cms.ai.internal;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.ai.audit.AiAuditEntry;
import org.apache.sling.cms.ai.audit.AiAuditService;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link JcrAiAuditServiceImpl}.
 */
@ExtendWith(SlingContextExtension.class)
class JcrAiAuditServiceImplTest {

    private final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    private JcrAiAuditServiceImpl auditService;

    @BeforeEach
    void setUp() {
        // Register the service
        auditService = context.registerInjectActivateService(new JcrAiAuditServiceImpl());
    }

    @Test
    void testRecordAuditEntry() {
        // Create an audit entry
        AiAuditEntry entry = AiAuditEntry.builder()
                .userId("admin")
                .operationType(AiAuditEntry.OperationType.SUMMARIZE)
                .providerId("openai")
                .contentPath("/content/site/page")
                .sitePath("/content/site")
                .outcome(AiAuditEntry.Outcome.SUCCESS)
                .inputHash("abc123")
                .outputPreview("This is a summary...")
                .processingTimeMs(1500L)
                .build();

        // Record the entry
        String entryId = auditService.record(entry);

        // Verify the entry was recorded
        assertNotNull(entryId, "Entry ID should not be null");
        assertEquals(entry.getId(), entryId, "Entry ID should match");

        // Verify the JCR structure was created
        String auditPath = buildExpectedPath(entry);
        Resource auditResource = context.resourceResolver().getResource(auditPath);
        assertNotNull(auditResource, "Audit resource should exist at: " + auditPath);

        Resource contentResource = auditResource.getChild("jcr:content");
        assertNotNull(contentResource, "Content resource should exist");

        // Verify properties
        assertEquals("admin", contentResource.getValueMap().get("userId"));
        assertEquals("SUMMARIZE", contentResource.getValueMap().get("operationType"));
        assertEquals("openai", contentResource.getValueMap().get("providerId"));
        assertEquals("/content/site/page", contentResource.getValueMap().get("contentPath"));
    }

    @Test
    void testGetEntry() {
        // Create and record an entry
        AiAuditEntry entry = createTestEntry();
        String entryId = auditService.record(entry);

        // Retrieve the entry
        Optional<AiAuditEntry> retrieved = auditService.getEntry(entryId);

        // Verify
        assertTrue(retrieved.isPresent(), "Entry should be retrieved");
        assertEquals(entryId, retrieved.get().getId());
        assertEquals("admin", retrieved.get().getUserId());
        assertEquals(AiAuditEntry.OperationType.SUMMARIZE, retrieved.get().getOperationType());
        assertEquals("openai", retrieved.get().getProviderId());
    }

    @Test
    void testUpdateOutcome() {
        // Create and record an entry
        AiAuditEntry entry = createTestEntry();
        String entryId = auditService.record(entry);

        // Update the outcome
        auditService.updateOutcome(entryId, AiAuditEntry.Outcome.APPLIED);

        // Retrieve and verify
        Optional<AiAuditEntry> updated = auditService.getEntry(entryId);
        assertTrue(updated.isPresent(), "Updated entry should be retrieved");
        assertEquals(AiAuditEntry.Outcome.APPLIED, updated.get().getOutcome());
    }

    @Test
    void testGetEntriesForContent() {
        // Create multiple entries for the same content path
        String contentPath = "/content/site/page";

        for (int i = 0; i < 3; i++) {
            AiAuditEntry entry = AiAuditEntry.builder()
                    .userId("admin")
                    .operationType(AiAuditEntry.OperationType.SUMMARIZE)
                    .providerId("openai")
                    .contentPath(contentPath)
                    .outcome(AiAuditEntry.Outcome.SUCCESS)
                    .processingTimeMs(1000L + i * 100)
                    .build();
            auditService.record(entry);
        }

        // Create entry for different content
        AiAuditEntry otherEntry = AiAuditEntry.builder()
                .userId("admin")
                .operationType(AiAuditEntry.OperationType.SUMMARIZE)
                .providerId("openai")
                .contentPath("/content/site/other")
                .outcome(AiAuditEntry.Outcome.SUCCESS)
                .processingTimeMs(1000L)
                .build();
        auditService.record(otherEntry);

        // Query for entries
        List<AiAuditEntry> entries = auditService.getEntriesForContent(contentPath, 10);

        // Verify
        assertEquals(3, entries.size(), "Should retrieve 3 entries for the content path");
        entries.forEach(e -> assertEquals(contentPath, e.getContentPath()));
    }

    @Test
    void testGetEntriesForUser() {
        // Create entries for different users
        String userId = "testuser";

        for (int i = 0; i < 2; i++) {
            AiAuditEntry entry = AiAuditEntry.builder()
                    .userId(userId)
                    .operationType(AiAuditEntry.OperationType.SUMMARIZE)
                    .providerId("openai")
                    .contentPath("/content/site/page" + i)
                    .outcome(AiAuditEntry.Outcome.SUCCESS)
                    .processingTimeMs(1000L)
                    .build();
            auditService.record(entry);
        }

        // Create entry for different user
        AiAuditEntry otherEntry = AiAuditEntry.builder()
                .userId("otheruser")
                .operationType(AiAuditEntry.OperationType.SUMMARIZE)
                .providerId("openai")
                .contentPath("/content/site/page")
                .outcome(AiAuditEntry.Outcome.SUCCESS)
                .processingTimeMs(1000L)
                .build();
        auditService.record(otherEntry);

        // Query for user entries
        List<AiAuditEntry> entries = auditService.getEntriesForUser(userId, 10);

        // Verify
        assertEquals(2, entries.size(), "Should retrieve 2 entries for the user");
        entries.forEach(e -> assertEquals(userId, e.getUserId()));
    }

    @Test
    void testGetEntriesByOperationType() {
        // Create entries with different operation types
        AiAuditEntry summarizeEntry = AiAuditEntry.builder()
                .userId("admin")
                .operationType(AiAuditEntry.OperationType.SUMMARIZE)
                .providerId("openai")
                .contentPath("/content/site/page1")
                .outcome(AiAuditEntry.Outcome.SUCCESS)
                .processingTimeMs(1000L)
                .build();
        auditService.record(summarizeEntry);

        AiAuditEntry rewriteEntry = AiAuditEntry.builder()
                .userId("admin")
                .operationType(AiAuditEntry.OperationType.REWRITE)
                .providerId("openai")
                .contentPath("/content/site/page2")
                .outcome(AiAuditEntry.Outcome.SUCCESS)
                .processingTimeMs(1500L)
                .build();
        auditService.record(rewriteEntry);

        // Query by operation type
        List<AiAuditEntry> summarizeEntries =
                auditService.getEntriesByOperationType(AiAuditEntry.OperationType.SUMMARIZE, 10);
        List<AiAuditEntry> rewriteEntries =
                auditService.getEntriesByOperationType(AiAuditEntry.OperationType.REWRITE, 10);

        // Verify
        assertEquals(1, summarizeEntries.size(), "Should have 1 summarize entry");
        assertEquals(
                AiAuditEntry.OperationType.SUMMARIZE, summarizeEntries.get(0).getOperationType());

        assertEquals(1, rewriteEntries.size(), "Should have 1 rewrite entry");
        assertEquals(AiAuditEntry.OperationType.REWRITE, rewriteEntries.get(0).getOperationType());
    }

    @Test
    void testGetEntriesInRange() {
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

        // Create entry from 2 days ago
        AiAuditEntry oldEntry = AiAuditEntry.builder()
                .timestamp(twoDaysAgo)
                .userId("admin")
                .operationType(AiAuditEntry.OperationType.SUMMARIZE)
                .providerId("openai")
                .contentPath("/content/site/page1")
                .outcome(AiAuditEntry.Outcome.SUCCESS)
                .processingTimeMs(1000L)
                .build();
        auditService.record(oldEntry);

        // Create entry from yesterday
        AiAuditEntry recentEntry = AiAuditEntry.builder()
                .timestamp(yesterday)
                .userId("admin")
                .operationType(AiAuditEntry.OperationType.SUMMARIZE)
                .providerId("openai")
                .contentPath("/content/site/page2")
                .outcome(AiAuditEntry.Outcome.SUCCESS)
                .processingTimeMs(1000L)
                .build();
        auditService.record(recentEntry);

        // Query for entries in range
        List<AiAuditEntry> entries = auditService.getEntriesInRange(
                twoDaysAgo.minus(1, ChronoUnit.HOURS), yesterday.plus(1, ChronoUnit.HOURS), 10);

        // Verify
        assertEquals(2, entries.size(), "Should retrieve entries in the time range");
    }

    @Test
    void testCleanup() {
        Instant now = Instant.now();
        Instant oldDate = now.minus(40, ChronoUnit.DAYS);

        // Create old entry (beyond retention)
        AiAuditEntry oldEntry = AiAuditEntry.builder()
                .timestamp(oldDate)
                .userId("admin")
                .operationType(AiAuditEntry.OperationType.SUMMARIZE)
                .providerId("openai")
                .contentPath("/content/site/page1")
                .outcome(AiAuditEntry.Outcome.SUCCESS)
                .processingTimeMs(1000L)
                .build();
        String oldEntryId = auditService.record(oldEntry);

        // Create recent entry (within retention)
        AiAuditEntry recentEntry = createTestEntry();
        String recentEntryId = auditService.record(recentEntry);

        // Cleanup entries older than 30 days
        int deletedCount = auditService.cleanup(30);

        // Verify
        assertEquals(1, deletedCount, "Should delete 1 old entry");

        // Verify old entry is gone
        Optional<AiAuditEntry> oldRetrieved = auditService.getEntry(oldEntryId);
        assertTrue(oldRetrieved.isEmpty(), "Old entry should be deleted");

        // Verify recent entry still exists
        Optional<AiAuditEntry> recentRetrieved = auditService.getEntry(recentEntryId);
        assertTrue(recentRetrieved.isPresent(), "Recent entry should still exist");
    }

    @Test
    void testMaxResults() {
        // Create 10 entries
        for (int i = 0; i < 10; i++) {
            AiAuditEntry entry = AiAuditEntry.builder()
                    .userId("admin")
                    .operationType(AiAuditEntry.OperationType.SUMMARIZE)
                    .providerId("openai")
                    .contentPath("/content/site/page")
                    .outcome(AiAuditEntry.Outcome.SUCCESS)
                    .processingTimeMs(1000L)
                    .build();
            auditService.record(entry);
        }

        // Query with maxResults = 5
        List<AiAuditEntry> entries = auditService.getEntriesForContent("/content/site/page", 5);

        // Verify
        assertEquals(5, entries.size(), "Should limit results to maxResults");
    }

    @Test
    void testErrorMessageHandling() {
        // Create entry with error message
        AiAuditEntry entry = AiAuditEntry.builder()
                .userId("admin")
                .operationType(AiAuditEntry.OperationType.SUMMARIZE)
                .providerId("openai")
                .contentPath("/content/site/page")
                .outcome(AiAuditEntry.Outcome.FAILURE)
                .errorMessage("API rate limit exceeded")
                .processingTimeMs(100L)
                .build();

        String entryId = auditService.record(entry);

        // Retrieve and verify
        Optional<AiAuditEntry> retrieved = auditService.getEntry(entryId);
        assertTrue(retrieved.isPresent());
        assertTrue(retrieved.get().getErrorMessage().isPresent());
        assertEquals(
                "API rate limit exceeded", retrieved.get().getErrorMessage().get());
    }

    // Helper methods

    private AiAuditEntry createTestEntry() {
        return AiAuditEntry.builder()
                .userId("admin")
                .operationType(AiAuditEntry.OperationType.SUMMARIZE)
                .providerId("openai")
                .contentPath("/content/site/page")
                .sitePath("/content/site")
                .outcome(AiAuditEntry.Outcome.SUCCESS)
                .inputHash("test-hash")
                .outputPreview("Test summary")
                .processingTimeMs(1500L)
                .build();
    }

    private String buildExpectedPath(AiAuditEntry entry) {
        Instant timestamp = entry.getTimestamp();
        String year =
                String.format("%04d", timestamp.atZone(java.time.ZoneOffset.UTC).getYear());
        String month =
                String.format("%02d", timestamp.atZone(java.time.ZoneOffset.UTC).getMonthValue());
        String day =
                String.format("%02d", timestamp.atZone(java.time.ZoneOffset.UTC).getDayOfMonth());
        return String.format("%s/%s/%s/%s/%s", AiAuditService.AUDIT_ROOT, year, month, day, entry.getId());
    }
}
