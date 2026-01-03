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
package org.apache.sling.cms.ai.audit;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Service for recording and retrieving AI operation audit logs.
 * <p>
 * This service ensures that every AI-assisted change is traceable
 * and compliant with governance requirements.
 * </p>
 */
@ProviderType
public interface AiAuditService {

    /**
     * The root path where audit entries are stored.
     */
    String AUDIT_ROOT = "/var/audit/ai";

    /**
     * Records an audit entry for an AI operation.
     *
     * @param entry the audit entry to record
     * @return the ID of the recorded entry
     */
    String record(AiAuditEntry entry);

    /**
     * Updates an existing audit entry (e.g., when user applies or rejects suggestion).
     *
     * @param entryId the ID of the entry to update
     * @param outcome the new outcome
     */
    void updateOutcome(String entryId, AiAuditEntry.Outcome outcome);

    /**
     * Retrieves an audit entry by ID.
     *
     * @param entryId the audit entry ID
     * @return an Optional containing the entry if found
     */
    Optional<AiAuditEntry> getEntry(String entryId);

    /**
     * Retrieves audit entries for a specific content path.
     *
     * @param contentPath the content path to search
     * @param maxResults  the maximum number of results
     * @return a list of audit entries, newest first
     */
    List<AiAuditEntry> getEntriesForContent(String contentPath, int maxResults);

    /**
     * Retrieves audit entries for a specific user.
     *
     * @param userId     the user ID to search
     * @param maxResults the maximum number of results
     * @return a list of audit entries, newest first
     */
    List<AiAuditEntry> getEntriesForUser(String userId, int maxResults);

    /**
     * Retrieves audit entries within a time range.
     *
     * @param from       the start of the time range (inclusive)
     * @param to         the end of the time range (exclusive)
     * @param maxResults the maximum number of results
     * @return a list of audit entries, newest first
     */
    List<AiAuditEntry> getEntriesInRange(Instant from, Instant to, int maxResults);

    /**
     * Retrieves audit entries for a specific operation type.
     *
     * @param operationType the operation type to search
     * @param maxResults    the maximum number of results
     * @return a list of audit entries, newest first
     */
    List<AiAuditEntry> getEntriesByOperationType(AiAuditEntry.OperationType operationType, int maxResults);

    /**
     * Cleans up audit entries older than the specified retention period.
     *
     * @param retentionDays the number of days to retain entries
     * @return the number of entries deleted
     */
    int cleanup(int retentionDays);
}
