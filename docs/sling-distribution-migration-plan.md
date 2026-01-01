# SPDX-License-Identifier: Apache-2.0

# Sling Distribution Migration Plan

**Date:** 2025-12-31
**Status:** 📋 Planning
**Priority:** HIGH (Phase 1 - Foundation & Stability)

---

## Executive Summary

Apache Sling CMS currently has a **custom HTTP-based distribution implementation** that should be replaced with the **native Sling Content Distribution framework**. While Sling Distribution bundles are configured, they are not actively used. This document outlines the migration plan.

---

## Current State vs. Target State

### Current State ❌

```
┌──────────────────┐                    ┌──────────────────┐
│  Author          │                    │  Publisher       │
│                  │                    │                  │
│  Custom HTTP     │  JSON over HTTP    │  Custom Import   │
│  Distribution    ├───────────────────►│  Servlet         │
│  Service         │  Base64 Binaries   │                  │
│                  │  No Queue/Retry    │                  │
└──────────────────┘                    └──────────────────┘
```

**Problems:**
- ❌ Base64-encoded binaries (33% overhead)
- ❌ No persistent queue
- ❌ No retry mechanism
- ❌ No transaction support
- ❌ Custom code maintenance burden
- ❌ Sling Distribution bundles unused

### Target State ✅

```
┌──────────────────┐                    ┌──────────────────┐
│  Author          │                    │  Publisher       │
│                  │                    │                  │
│  Publication     │  Distribution      │  Package         │
│  Manager         │  Package           │  Importer        │
│  │               │  (FileVault)       │  │               │
│  ▼               │                    │  ▼               │
│  Distributor API │  ─────────────────►│  Queue Consumer  │
│  │               │  HTTP/JMS          │  │               │
│  ▼               │                    │  ▼               │
│  Queue           │                    │  JCR Install     │
└──────────────────┘                    └──────────────────┘
```

**Benefits:**
- ✅ Efficient binary handling (FileVault references)
- ✅ Persistent queue (survives restarts)
- ✅ Automatic retry with exponential backoff
- ✅ Transaction support (all-or-nothing)
- ✅ Built-in monitoring (JMX, queue status)
- ✅ Package versioning and rollback
- ✅ Less custom code to maintain

---

## Technical Analysis

### Existing Infrastructure

**Already Configured** (from [distribution.json](../feature/src/main/features/cms/distribution.json)):

1. **Bundles:**
   - `org.apache.sling.distribution.api:0.7.2`
   - `org.apache.sling.distribution.core:0.7.2`

2. **Virtual Resource Providers:**
   - `/libs/sling/distribution/settings/agents` - Agent configs
   - `/libs/sling/distribution/settings/exporters` - Exporter configs
   - `/libs/sling/distribution/settings/importers` - Importer configs
   - `/libs/sling/distribution/services/agents` - Agent runtime info

3. **Service User:**
   - User: `sling-package-install`
   - Privilege: `sling:publish`

4. **UI Components:**
   - Agent management UI (HTL)
   - Exporter management UI (HTL)
   - Importer management UI (HTL)

**Needs Implementation:**

1. **Distribution Agent Configuration** - OSGi factory config
2. **Integration with PublicationManager** - Use `Distributor` API
3. **Package Builder Configuration** - Optimize for binaries
4. **Queue Provider** - Priority-based or FIFO
5. **Transport Handler** - HTTP or JMS
6. **Remove Custom Implementation** - Delete ContentDistributionServiceImpl

---

## Implementation Steps

### Phase 1: Setup Distribution Agent (Week 1)

#### Step 1.1: Create Agent Configuration

Create OSGi factory configuration for forward replication agent.

**File:** `ui/src/main/resources/jcr_root/apps/sling-cms/config.author/org.apache.sling.distribution.agent.impl.ForwardDistributionAgentFactory~publish.cfg.json`

```json
{
  "name": "publish",
  "title": "Publish Agent",
  "details": "Forwards content from author to publisher instances",
  "enabled": true,
  "serviceName": "publish",
  "log.level": "info",

  "packageImporter.endpoints": [
    "http://localhost:8083/libs/sling/distribution/services/importers/default"
  ],

  "packageExporter.target": "(name=default)",
  "packageBuilder.target": "(name=default)",

  "queue.provider.target": "(name=priority)",
  "priorityQueues": [
    "high",
    "normal"
  ],

  "retry.strategy": "exponential",
  "retry.attempts": 3,
  "retry.delay": 5000,

  "allowed.roots": [
    "/content"
  ],

  "passiveQueues": []
}
```

#### Step 1.2: Create Package Builder Configuration

Configure package builder to handle binaries efficiently.

**File:** `ui/src/main/resources/jcr_root/apps/sling-cms/config.author/org.apache.sling.distribution.packaging.impl.ResourceDistributionPackageBuilderFactory~default.cfg.json`

```json
{
  "name": "default",
  "type": "resource",
  "package.filters": [
    "/content"
  ],
  "useBinaryReferences": true,
  "tempFsFolder": "/var/sling/distribution/packages"
}
```

#### Step 1.3: Create Package Exporter Configuration

**File:** `ui/src/main/resources/jcr_root/apps/sling-cms/config.author/org.apache.sling.distribution.packaging.impl.exporter.LocalDistributionPackageExporterFactory~default.cfg.json`

```json
{
  "name": "default",
  "packageBuilder.target": "(name=default)"
}
```

#### Step 1.4: Create Package Importer Configuration (Publisher)

**File:** `ui/src/main/resources/jcr_root/apps/sling-cms/config.publish/org.apache.sling.distribution.packaging.impl.importer.LocalDistributionPackageImporterFactory~default.cfg.json`

```json
{
  "name": "default",
  "packageInstaller.target": "(name=jcrinstaller)"
}
```

### Phase 2: Integrate with PublicationManager (Week 2)

#### Step 2.1: Create Sling Distribution Publication Manager

**File:** `distribution/src/main/java/org/apache/sling/cms/distribution/impl/SlingDistributionPublicationManager.java`

```java
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
package org.apache.sling.cms.distribution.impl;

import org.apache.sling.cms.PublishableResource;
import org.apache.sling.cms.publication.PUBLICATION_MODE;
import org.apache.sling.cms.publication.PublicationException;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.Distributor;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of PublicationManager using native Sling Distribution framework.
 * Replaces the custom HTTP-based distribution with proper Sling Distribution integration.
 */
public class SlingDistributionPublicationManager extends StandalonePublicationManager {

    private static final Logger LOG = LoggerFactory.getLogger(SlingDistributionPublicationManager.class);
    private static final String AGENT_NAME = "publish";

    @Reference(target = "(name=publish)")
    private Distributor distributor;

    public SlingDistributionPublicationManager(EventAdmin eventAdmin) {
        super(eventAdmin);
    }

    @Override
    public void publish(PublishableResource resource) throws PublicationException {
        LOG.info("Publishing via Sling Distribution: {}", resource.getPath());

        try {
            // Create distribution request for ADD (publish)
            DistributionRequest request = new SimpleDistributionRequest(
                    DistributionRequestType.ADD,
                    resource.getPath()
            );

            // Distribute via Sling Distribution framework
            DistributionResponse response = distributor.distribute(
                    AGENT_NAME,
                    resource.getResource().getResourceResolver(),
                    request
            );

            // Check if distribution was successful
            if (!response.isSuccessful()) {
                throw new PublicationException(
                        "Distribution failed: " + response.getMessage()
                );
            }

            LOG.debug("Distribution successful, updating publication metadata");
            // Update local publication metadata
            super.publish(resource);

        } catch (PublicationException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to publish via Sling Distribution", e);
            throw new PublicationException("Failed to publish content", e);
        }
    }

    @Override
    public void unpublish(PublishableResource resource) throws PublicationException {
        LOG.info("Unpublishing via Sling Distribution: {}", resource.getPath());

        try {
            // Create distribution request for DELETE (unpublish)
            DistributionRequest request = new SimpleDistributionRequest(
                    DistributionRequestType.DELETE,
                    resource.getPath()
            );

            // Distribute via Sling Distribution framework
            DistributionResponse response = distributor.distribute(
                    AGENT_NAME,
                    resource.getResource().getResourceResolver(),
                    request
            );

            // Check if distribution was successful
            if (!response.isSuccessful()) {
                throw new PublicationException(
                        "Distribution delete failed: " + response.getMessage()
                );
            }

            LOG.debug("Distribution delete successful, updating publication metadata");
            // Update local publication metadata
            super.unpublish(resource);

        } catch (PublicationException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to unpublish via Sling Distribution", e);
            throw new PublicationException("Failed to unpublish content", e);
        }
    }

    @Override
    public PUBLICATION_MODE getPublicationMode() {
        return PUBLICATION_MODE.CONTENT_DISTRIBUTION;
    }
}
```

#### Step 2.2: Update PublicationManagerFactoryImpl

Update factory to use Sling Distribution instead of custom HTTP distribution.

**File:** `distribution/src/main/java/org/apache/sling/cms/distribution/impl/PublicationManagerFactoryImpl.java`

```java
// Replace HttpDistributionPublicationManager instantiation with:

if (config.publication_mode() == PUBLICATION_MODE.CONTENT_DISTRIBUTION) {
    // Use Sling Distribution framework
    publicationManager = new SlingDistributionPublicationManager(eventAdmin);
} else {
    // Standalone mode
    publicationManager = new StandalonePublicationManager(eventAdmin);
}
```

### Phase 3: Testing & Validation (Week 3)

#### Test Cases

1. **Single Resource Publication**
   - Publish a page
   - Verify package created
   - Verify queue entry
   - Verify successful distribution
   - Verify content on publisher

2. **Deep Publication**
   - Publish page with children
   - Verify all descendants included
   - Verify package structure

3. **Binary Asset Publication**
   - Publish large image (> 10MB)
   - Verify binary reference (not base64)
   - Verify file size efficiency
   - Verify integrity (checksum)

4. **Unpublish**
   - Unpublish content
   - Verify DELETE request
   - Verify removal from publisher

5. **Bulk Publication**
   - Use BulkPublicationJob
   - Verify multiple resources queued
   - Verify sequential processing
   - Verify job completion

6. **Failure Scenarios**
   - Publisher offline
   - Verify queue persistence
   - Verify retry mechanism
   - Verify eventual success

7. **Queue Management**
   - View queue status via UI
   - Verify priority queues
   - Verify item counts
   - Verify agent state

### Phase 4: Cleanup (Week 4)

#### Remove Obsolete Code

1. **Delete Custom Distribution Service**
   - ❌ `ContentDistributionServiceImpl.java`
   - ❌ `ContentDistributionService.java` (interface)
   - ❌ `ContentImportServlet.java`
   - ❌ `HttpDistributionPublicationManager.java`
   - ❌ `ContentDistributionConfig.java`

2. **Update Dependencies**
   - Remove unused HTTP client dependencies
   - Remove Gson dependency (if only used for distribution)

3. **Documentation**
   - Update publication system docs
   - Add Sling Distribution configuration guide
   - Update troubleshooting guide

---

## Configuration Guide (Post-Migration)

### Author Instance Configuration

**File:** `org.apache.sling.cms.distribution.impl.PublicationConfig.cfg.json`

```json
{
  "instance.type": "AUTHOR",
  "publication.mode": "CONTENT_DISTRIBUTION"
}
```

### Publisher Instance Configuration

**File:** `org.apache.sling.cms.distribution.impl.PublicationConfig.cfg.json`

```json
{
  "instance.type": "RENDERER",
  "publication.mode": "CONTENT_DISTRIBUTION"
}
```

### Service User Permissions

```
create service user sling-package-install

set ACL for sling-package-install
  allow jcr:read on /
  allow rep:write on /content
  allow jcr:versionManagement on /content
end
```

---

## Rollback Plan

If issues arise during migration:

1. **Revert Code Changes**
   - Git revert to previous commit
   - Redeploy previous version

2. **Switch Configuration**
   - Keep both implementations available
   - Use feature flag to switch:
   ```json
   {
     "use.sling.distribution": false  // Use custom HTTP
   }
   ```

3. **Gradual Migration**
   - Run both in parallel
   - Compare results
   - Gradually switch traffic

---

## Success Criteria

Migration is successful when:

- ✅ All publication tests pass
- ✅ Binary assets < 33% smaller (no base64)
- ✅ Queue visible and functional in UI
- ✅ Retry mechanism verified (3 attempts)
- ✅ No custom distribution code remaining
- ✅ Performance equal or better
- ✅ Zero data loss or corruption
- ✅ Documentation complete

---

## Timeline

| Phase | Duration | Deliverables |
|-------|----------|--------------|
| **Phase 1** | Week 1 | Agent configs, package builder setup |
| **Phase 2** | Week 2 | SlingDistributionPublicationManager implementation |
| **Phase 3** | Week 3 | Complete testing, validation |
| **Phase 4** | Week 4 | Cleanup, documentation, deployment |

**Total:** ~4 weeks for complete migration

---

## Risks & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Data loss during migration** | HIGH | Comprehensive backup, parallel run, gradual cutover |
| **Performance degradation** | MEDIUM | Load testing, package size optimization, queue tuning |
| **Breaking changes in API** | LOW | Thorough testing, feature flags for rollback |
| **Publisher incompatibility** | MEDIUM | Version alignment, compatibility testing |

---

## References

- [Sling Content Distribution Documentation](https://sling.apache.org/documentation/bundles/content-distribution.html)
- [FileVault Package Format](https://jackrabbit.apache.org/filevault/overview.html)
- [Publication System Analysis](publication-system-analysis.md)
- [JSP to HTL Migration](jsp-to-htl-publication-migration.md)

---

## Next Steps

1. ✅ Document analysis complete (this document)
2. 🔲 Create feature branch: `feature/sling-distribution-migration`
3. 🔲 Implement Phase 1 (Agent configuration)
4. 🔲 Implement Phase 2 (PublicationManager integration)
5. 🔲 Execute Phase 3 (Testing)
6. 🔲 Execute Phase 4 (Cleanup)
7. 🔲 Code review and merge

---

**Status:** 📋 Ready for Implementation
**Assigned To:** TBD
**Target Completion:** Q1 2026
