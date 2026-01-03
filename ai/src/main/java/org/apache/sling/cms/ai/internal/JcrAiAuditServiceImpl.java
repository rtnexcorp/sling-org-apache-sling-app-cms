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

import javax.jcr.query.Query;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.cms.ai.audit.AiAuditEntry;
import org.apache.sling.cms.ai.audit.AiAuditService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JCR-based implementation of {@link AiAuditService}.
 * <p>
 * Stores audit entries under {@code /var/audit/ai} with structure:
 * <pre>
 * /var/audit/ai/
 *   ├── 2026/
 *   │   ├── 01/
 *   │   │   ├── 03/
 *   │   │   │   ├── {uuid} (sling:Folder)
 *   │   │   │   │   ├── jcr:content (nt:unstructured)
 *   │   │   │   │   │   ├── id = "uuid"
 *   │   │   │   │   │   ├── timestamp = Calendar
 *   │   │   │   │   │   ├── userId = "admin"
 *   │   │   │   │   │   ├── operationType = "SUMMARIZE"
 *   │   │   │   │   │   ├── providerId = "openai"
 *   │   │   │   │   │   ├── contentPath = "/content/site/page"
 *   │   │   │   │   │   ├── outcome = "SUCCESS"
 *   │   │   │   │   │   └── ...
 * </pre>
 * </p>
 */
@Component(service = AiAuditService.class)
public class JcrAiAuditServiceImpl implements AiAuditService {

    private static final Logger log = LoggerFactory.getLogger(JcrAiAuditServiceImpl.class);

    private static final String SERVICE_USER = "sling-cms-ai";
    private static final String NODE_TYPE = "sling:Folder";
    private static final String CONTENT_NODE = "jcr:content";
    private static final String CONTENT_NODE_TYPE = "nt:unstructured";

    // Property names
    private static final String PN_ID = "id";
    private static final String PN_TIMESTAMP = "timestamp";
    private static final String PN_USER_ID = "userId";
    private static final String PN_OPERATION_TYPE = "operationType";
    private static final String PN_PROVIDER_ID = "providerId";
    private static final String PN_CONTENT_PATH = "contentPath";
    private static final String PN_SITE_PATH = "sitePath";
    private static final String PN_PROMPT_TEMPLATE_ID = "promptTemplateId";
    private static final String PN_OUTCOME = "outcome";
    private static final String PN_INPUT_HASH = "inputHash";
    private static final String PN_OUTPUT_PREVIEW = "outputPreview";
    private static final String PN_ERROR_MESSAGE = "errorMessage";
    private static final String PN_PROCESSING_TIME_MS = "processingTimeMs";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Activate
    protected void activate() {
        log.info("JCR-based AI Audit Service activated");
    }

    @Override
    public String record(AiAuditEntry entry) {
        try (ResourceResolver resolver = getServiceResourceResolver()) {
            String auditPath = buildAuditPath(entry);
            createAuditResource(resolver, auditPath, entry);
            resolver.commit();
            log.debug("Recorded AI audit entry: {}", entry.getId());
            return entry.getId();
        } catch (LoginException e) {
            log.error("Failed to get service resource resolver for audit recording", e);
            return null;
        } catch (PersistenceException e) {
            log.error("Failed to persist audit entry: {}", entry.getId(), e);
            return null;
        }
    }

    @Override
    public void updateOutcome(String entryId, AiAuditEntry.Outcome outcome) {
        try (ResourceResolver resolver = getServiceResourceResolver()) {
            Optional<Resource> entryResource = findEntryResource(resolver, entryId);
            if (entryResource.isPresent()) {
                Resource contentResource = entryResource.get().getChild(CONTENT_NODE);
                if (contentResource != null) {
                    ModifiableValueMap properties = contentResource.adaptTo(ModifiableValueMap.class);
                    if (properties != null) {
                        properties.put(PN_OUTCOME, outcome.name());
                        resolver.commit();
                        log.debug("Updated outcome for audit entry {} to {}", entryId, outcome);
                    }
                }
            } else {
                log.warn("Audit entry not found for update: {}", entryId);
            }
        } catch (LoginException e) {
            log.error("Failed to get service resource resolver for outcome update", e);
        } catch (PersistenceException e) {
            log.error("Failed to update outcome for audit entry: {}", entryId, e);
        }
    }

    @Override
    public Optional<AiAuditEntry> getEntry(String entryId) {
        try (ResourceResolver resolver = getServiceResourceResolver()) {
            Optional<Resource> entryResource = findEntryResource(resolver, entryId);
            return entryResource.map(this::resourceToAuditEntry);
        } catch (LoginException e) {
            log.error("Failed to get service resource resolver for entry retrieval", e);
            return Optional.empty();
        }
    }

    @Override
    public List<AiAuditEntry> getEntriesForContent(String contentPath, int maxResults) {
        String queryString = String.format(
                "SELECT * FROM [%s] AS node WHERE ISDESCENDANTNODE(node, '%s') AND node.[%s] = '%s' ORDER BY node.[%s] DESC",
                CONTENT_NODE_TYPE, AUDIT_ROOT, PN_CONTENT_PATH, escapeSql(contentPath), PN_TIMESTAMP);
        return executeQuery(queryString, maxResults);
    }

    @Override
    public List<AiAuditEntry> getEntriesForUser(String userId, int maxResults) {
        String queryString = String.format(
                "SELECT * FROM [%s] AS node WHERE ISDESCENDANTNODE(node, '%s') AND node.[%s] = '%s' ORDER BY node.[%s] DESC",
                CONTENT_NODE_TYPE, AUDIT_ROOT, PN_USER_ID, escapeSql(userId), PN_TIMESTAMP);
        return executeQuery(queryString, maxResults);
    }

    @Override
    public List<AiAuditEntry> getEntriesInRange(Instant from, Instant to, int maxResults) {
        Calendar fromCal = Calendar.getInstance();
        fromCal.setTimeInMillis(from.toEpochMilli());
        Calendar toCal = Calendar.getInstance();
        toCal.setTimeInMillis(to.toEpochMilli());

        String queryString = String.format(
                "SELECT * FROM [%s] AS node WHERE ISDESCENDANTNODE(node, '%s') AND node.[%s] >= CAST('%s' AS DATE) AND node.[%s] < CAST('%s' AS DATE) ORDER BY node.[%s] DESC",
                CONTENT_NODE_TYPE,
                AUDIT_ROOT,
                PN_TIMESTAMP,
                DateTimeFormatter.ISO_INSTANT.format(from.atOffset(ZoneOffset.UTC)),
                PN_TIMESTAMP,
                DateTimeFormatter.ISO_INSTANT.format(to.atOffset(ZoneOffset.UTC)),
                PN_TIMESTAMP);
        return executeQuery(queryString, maxResults);
    }

    @Override
    public List<AiAuditEntry> getEntriesByOperationType(AiAuditEntry.OperationType operationType, int maxResults) {
        String queryString = String.format(
                "SELECT * FROM [%s] AS node WHERE ISDESCENDANTNODE(node, '%s') AND node.[%s] = '%s' ORDER BY node.[%s] DESC",
                CONTENT_NODE_TYPE, AUDIT_ROOT, PN_OPERATION_TYPE, operationType.name(), PN_TIMESTAMP);
        return executeQuery(queryString, maxResults);
    }

    @Override
    public int cleanup(int retentionDays) {
        Instant cutoffDate = Instant.now().minusSeconds(retentionDays * 24L * 3600L);
        try (ResourceResolver resolver = getServiceResourceResolver()) {
            String queryString = String.format(
                    "SELECT * FROM [%s] AS node WHERE ISDESCENDANTNODE(node, '%s') AND node.[%s] < CAST('%s' AS DATE)",
                    CONTENT_NODE_TYPE,
                    AUDIT_ROOT,
                    PN_TIMESTAMP,
                    DateTimeFormatter.ISO_INSTANT.format(cutoffDate.atOffset(ZoneOffset.UTC)));

            Iterator<Resource> results = resolver.findResources(queryString, Query.JCR_SQL2);
            int deletedCount = 0;
            while (results.hasNext()) {
                Resource contentResource = results.next();
                Resource parentResource = contentResource.getParent();
                if (parentResource != null) {
                    resolver.delete(parentResource);
                    deletedCount++;
                }
            }
            if (deletedCount > 0) {
                resolver.commit();
                log.info("Cleaned up {} audit entries older than {} days", deletedCount, retentionDays);
            }
            return deletedCount;
        } catch (LoginException e) {
            log.error("Failed to get service resource resolver for cleanup operation", e);
            return 0;
        } catch (PersistenceException e) {
            log.error("Failed to cleanup audit entries", e);
            return 0;
        }
    }

    /**
     * Builds the audit path based on timestamp: /var/audit/ai/YYYY/MM/DD/{uuid}
     */
    private String buildAuditPath(AiAuditEntry entry) {
        Instant timestamp = entry.getTimestamp();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);
        String datePath = formatter.format(timestamp);
        return String.format("%s/%s/%s", AUDIT_ROOT, datePath, entry.getId());
    }

    /**
     * Creates an audit resource in the repository.
     */
    private Resource createAuditResource(ResourceResolver resolver, String path, AiAuditEntry entry)
            throws PersistenceException {
        // Create parent folders if needed
        String parentPath = ResourceUtil.getParent(path);
        Resource parent = ResourceUtil.getOrCreateResource(resolver, parentPath, NODE_TYPE, NODE_TYPE, true);

        // Create audit entry folder
        Map<String, Object> folderProps = new HashMap<>();
        folderProps.put("jcr:primaryType", NODE_TYPE);
        Resource auditFolder = resolver.create(parent, ResourceUtil.getName(path), folderProps);

        // Create jcr:content node with audit data
        Map<String, Object> contentProps = buildAuditProperties(entry);
        resolver.create(auditFolder, CONTENT_NODE, contentProps);

        return auditFolder;
    }

    /**
     * Builds property map from AiAuditEntry.
     */
    private Map<String, Object> buildAuditProperties(AiAuditEntry entry) {
        Map<String, Object> props = new HashMap<>();
        props.put("jcr:primaryType", CONTENT_NODE_TYPE);
        props.put(PN_ID, entry.getId());

        Calendar timestamp = Calendar.getInstance();
        timestamp.setTimeInMillis(entry.getTimestamp().toEpochMilli());
        props.put(PN_TIMESTAMP, timestamp);

        if (entry.getUserId() != null) {
            props.put(PN_USER_ID, entry.getUserId());
        }
        props.put(PN_OPERATION_TYPE, entry.getOperationType().name());
        if (entry.getProviderId() != null) {
            props.put(PN_PROVIDER_ID, entry.getProviderId());
        }
        if (entry.getContentPath() != null) {
            props.put(PN_CONTENT_PATH, entry.getContentPath());
        }
        if (entry.getSitePath() != null) {
            props.put(PN_SITE_PATH, entry.getSitePath());
        }
        if (entry.getPromptTemplateId() != null) {
            props.put(PN_PROMPT_TEMPLATE_ID, entry.getPromptTemplateId());
        }
        props.put(PN_OUTCOME, entry.getOutcome().name());
        if (entry.getInputHash() != null) {
            props.put(PN_INPUT_HASH, entry.getInputHash());
        }
        if (entry.getOutputPreview() != null) {
            props.put(PN_OUTPUT_PREVIEW, entry.getOutputPreview());
        }
        if (entry.getErrorMessage().isPresent()) {
            props.put(PN_ERROR_MESSAGE, entry.getErrorMessage().get());
        }
        props.put(PN_PROCESSING_TIME_MS, entry.getProcessingTimeMs());

        return props;
    }

    /**
     * Finds an audit entry resource by ID.
     */
    private Optional<Resource> findEntryResource(ResourceResolver resolver, String entryId) {
        // Search under /var/audit/ai for the entry
        String queryString = String.format(
                "SELECT * FROM [%s] AS node WHERE ISDESCENDANTNODE(node, '%s') AND node.[%s] = '%s'",
                CONTENT_NODE_TYPE, AUDIT_ROOT, PN_ID, escapeSql(entryId));

        Iterator<Resource> results = resolver.findResources(queryString, Query.JCR_SQL2);
        if (results.hasNext()) {
            Resource contentResource = results.next();
            return Optional.ofNullable(contentResource.getParent());
        }
        return Optional.empty();
    }

    /**
     * Converts a Resource to AiAuditEntry.
     */
    private AiAuditEntry resourceToAuditEntry(Resource resource) {
        Resource contentResource = resource.getChild(CONTENT_NODE);
        if (contentResource == null) {
            return null;
        }

        Map<String, Object> props = contentResource.getValueMap();
        AiAuditEntry.Builder builder = AiAuditEntry.builder();

        builder.id(getString(props, PN_ID));

        Calendar timestamp = (Calendar) props.get(PN_TIMESTAMP);
        if (timestamp != null) {
            builder.timestamp(Instant.ofEpochMilli(timestamp.getTimeInMillis()));
        }

        builder.userId(getString(props, PN_USER_ID));

        String operationType = getString(props, PN_OPERATION_TYPE);
        if (operationType != null) {
            builder.operationType(AiAuditEntry.OperationType.valueOf(operationType));
        }

        builder.providerId(getString(props, PN_PROVIDER_ID));
        builder.contentPath(getString(props, PN_CONTENT_PATH));
        builder.sitePath(getString(props, PN_SITE_PATH));
        builder.promptTemplateId(getString(props, PN_PROMPT_TEMPLATE_ID));

        String outcome = getString(props, PN_OUTCOME);
        if (outcome != null) {
            builder.outcome(AiAuditEntry.Outcome.valueOf(outcome));
        }

        builder.inputHash(getString(props, PN_INPUT_HASH));
        builder.outputPreview(getString(props, PN_OUTPUT_PREVIEW));
        builder.errorMessage(getString(props, PN_ERROR_MESSAGE));

        Long processingTime = (Long) props.get(PN_PROCESSING_TIME_MS);
        if (processingTime != null) {
            builder.processingTimeMs(processingTime);
        }

        return builder.build();
    }

    /**
     * Executes a JCR-SQL2 query and returns audit entries.
     */
    private List<AiAuditEntry> executeQuery(String queryString, int maxResults) {
        try (ResourceResolver resolver = getServiceResourceResolver()) {
            Iterator<Resource> results = resolver.findResources(queryString, Query.JCR_SQL2);
            List<AiAuditEntry> entries = new ArrayList<>();
            int count = 0;
            while (results.hasNext() && count < maxResults) {
                Resource contentResource = results.next();
                Resource parentResource = contentResource.getParent();
                if (parentResource != null) {
                    AiAuditEntry entry = resourceToAuditEntry(parentResource);
                    if (entry != null) {
                        entries.add(entry);
                        count++;
                    }
                }
            }
            return entries;
        } catch (LoginException e) {
            log.error("Failed to get service resource resolver for query", e);
            return Collections.emptyList();
        }
    }

    /**
     * Gets a ResourceResolver with service user privileges.
     */
    private ResourceResolver getServiceResourceResolver() throws LoginException {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);
        return resolverFactory.getServiceResourceResolver(authInfo);
    }

    /**
     * Safely gets a string property.
     */
    private String getString(Map<String, Object> props, String key) {
        Object value = props.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Escapes SQL special characters for JCR-SQL2 queries.
     */
    private String escapeSql(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }
}
