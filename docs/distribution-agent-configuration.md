# SPDX-License-Identifier: Apache-2.0

# Distribution Agent Configuration

**Date:** 2025-12-31
**Status:** ✅ Configured
**Related:** [Sling Distribution Migration Plan](sling-distribution-migration-plan.md)

---

## Overview

This document describes the Distribution Agent configurations added to Apache Sling CMS to enable native Sling Content Distribution. These configurations provide the foundation for migrating from custom HTTP distribution to Sling Distribution framework.

---

## Configurations Added

### 1. Core Distribution Components

**File:** [feature/src/main/features/cms/distribution.json](../feature/src/main/features/cms/distribution.json)

#### Package Builder Configuration

```json
"org.apache.sling.distribution.packaging.impl.ResourceDistributionPackageBuilderFactory~default": {
  "name": "default",
  "type": "resource",
  "package.filters": ["/content"],
  "useBinaryReferences": true,
  "tempFsFolder": "/var/sling/distribution/packages"
}
```

**Purpose:** Creates distribution packages from JCR resources

**Key Features:**
- ✅ **`useBinaryReferences: true`** - Binaries NOT base64-encoded (efficient!)
- ✅ **Package filters** - Only include `/content` tree
- ✅ **Temp folder** - Packages stored in `/var/sling/distribution/packages`

#### Package Exporter Configuration

```json
"org.apache.sling.distribution.packaging.impl.exporter.LocalDistributionPackageExporterFactory~default": {
  "name": "default",
  "packageBuilder.target": "(name=default)"
}
```

**Purpose:** Exports packages using the configured package builder

**Links:** Uses the `default` package builder

#### Package Importer Configuration (Publisher Only)

**File:** [feature/src/main/features/runmodes/renderer.json](../feature/src/main/features/runmodes/renderer.json)

```json
"org.apache.sling.distribution.packaging.impl.importer.LocalDistributionPackageImporterFactory~default": {
  "name": "default",
  "packageInstaller.target": "(name=jcrinstaller)"
}
```

**Purpose:** Imports packages on publisher using JCR installer

**Links:** Uses `jcrinstaller` for package installation

**Note:** This configuration is in `renderer.json` (not `distribution.json`) because only publisher instances need to import packages.

#### Service User Permissions

```
create service user sling-package-install with path system/sling

set ACL for sling-package-install
  allow jcr:all on /content
  allow jcr:all on /conf
  allow jcr:all on /etc/taxonomy
  allow jcr:versionManagement on /content
end
```

**Purpose:** Service user for package installation with proper permissions

**Permissions:**
- Full access to `/content`, `/conf`, `/etc/taxonomy`
- Version management on `/content` for versioned packages

---

### 2. Forward Distribution Agent (Author)

**File:** [feature/src/main/features/runmodes/author.json](../feature/src/main/features/runmodes/author.json)

```json
"org.apache.sling.distribution.agent.impl.ForwardDistributionAgentFactory~publish": {
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
  "retry.attempts:Integer": 3,
  "retry.delay:Integer": 5000,

  "allowed.roots": [
    "/content",
    "/conf",
    "/etc/taxonomy"
  ],

  "passiveQueues": []
}
```

**Purpose:** Forward replication agent to push content from author to publisher

**Configuration Details:**

| Property | Value | Description |
|----------|-------|-------------|
| `name` | `publish` | Agent identifier (used in code) |
| `title` | `Publish Agent` | Display name in UI |
| `enabled` | `true` | Agent is active |
| `serviceName` | `publish` | OSGi service name |
| `log.level` | `info` | Logging level |

**Endpoints:**
- **Publisher:** `http://localhost:8083/libs/sling/distribution/services/importers/default`
- **Note:** Change for multi-publisher or production environments

**Queue Configuration:**
- **Provider:** Priority-based queue
- **Queues:** `high`, `normal` (allows prioritization)
- **Benefits:** Critical content can jump the queue

**Retry Configuration:**
- **Strategy:** Exponential backoff
- **Attempts:** 3 retries
- **Delay:** 5000ms (5 seconds initial delay)
- **Backoff:** 5s → 25s → 125s (exponential)

**Allowed Roots:**
- `/content` - Site content
- `/conf` - Context-aware configurations
- `/etc/taxonomy` - Taxonomy definitions

---

## How It Works

### Distribution Flow

```
┌─────────────────────────────────────────────────────────────┐
│ Author Instance (port 8082)                                 │
│                                                             │
│  1. PublicationManager.publish()                           │
│     ↓                                                       │
│  2. Distributor.distribute("publish", ...)                 │
│     ↓                                                       │
│  3. Package Builder                                        │
│     - Collects resources from /content                     │
│     - Creates binary references (NOT base64!)              │
│     - Builds FileVault package                             │
│     ↓                                                       │
│  4. Package Exporter                                       │
│     - Exports package to temp folder                       │
│     ↓                                                       │
│  5. Queue (Priority)                                       │
│     - Enqueues package for distribution                    │
│     - Persists queue (survives restarts)                   │
│     ↓                                                       │
│  6. Forward Agent ("publish")                              │
│     - Dequeues package                                     │
│     - HTTP POST to publisher endpoint                      │
│     - Retry on failure (3 attempts, exponential backoff)   │
│                                                             │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP POST
                       │ Package (FileVault format)
                       ↓
┌─────────────────────────────────────────────────────────────┐
│ Publisher Instance (port 8083)                              │
│                                                             │
│  7. Package Importer Endpoint                              │
│     /libs/sling/distribution/services/importers/default    │
│     ↓                                                       │
│  8. Package Importer                                       │
│     - Receives package                                     │
│     - Validates package                                    │
│     ↓                                                       │
│  9. JCR Installer                                          │
│     - Installs package to JCR                              │
│     - Creates/updates nodes                                │
│     - Handles binary references                            │
│     ↓                                                       │
│ 10. Content Published ✅                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Package Format (FileVault)

Distribution packages use Apache Jackrabbit FileVault format:

```
distribution-package.zip
├── META-INF/
│   ├── MANIFEST.MF
│   └── vault/
│       ├── config.xml
│       ├── filter.xml        # Package filters
│       └── properties.xml
└── jcr_root/
    └── content/
        └── mysite/
            ├── .content.xml  # Node metadata
            ├── page/
            │   └── .content.xml
            └── assets/
                ├── image.jpg  # Binary (NOT base64!)
                └── .content.xml
```

**Key Benefits:**
- ✅ Binaries stored as actual files (not base64)
- ✅ Metadata in XML (human-readable)
- ✅ Package versioning support
- ✅ Can be installed manually (via Package Manager)

---

## Monitoring

### Queue Status

View queue status via UI:
- **URL:** `http://localhost:8082/cms/publication/agents.html`
- **Agent:** Click "Publish Agent"
- **Queue Info:** Shows:
  - Queue depth (items pending)
  - Queue state (idle/running/blocked)
  - Item count per endpoint

### JMX Monitoring

Distribution agents expose JMX beans for monitoring:

**Bean:** `org.apache.sling.distribution:type=agent,id=publish`

**Metrics:**
- `QueueSize` - Number of items in queue
- `ProcessedItems` - Total processed items
- `FailedItems` - Total failed items
- `State` - Agent state (running/idle/paused)

### Logs

**Log File:** `logs/error.log`

**Log Pattern:**
```
INFO [org.apache.sling.distribution.agent.impl.ForwardDistributionAgentImpl]
  Publishing content: /content/mysite/page

INFO [org.apache.sling.distribution.packaging.impl.ResourceDistributionPackageBuilder]
  Building package for paths: [/content/mysite/page]

INFO [org.apache.sling.distribution.queue.impl.DistributionQueue]
  Added item to queue: publish-normal

INFO [org.apache.sling.distribution.transport.impl.HttpDistributionTransport]
  Distributing package to http://localhost:8083

INFO [org.apache.sling.distribution.agent.impl.ForwardDistributionAgentImpl]
  Distribution successful
```

---

## Configuration for Different Environments

### Development (Current)

**Publisher Endpoint:** `http://localhost:8083`

**Use Case:** Local development, single author + single publisher

### Staging

**Publisher Endpoints:**
```json
"packageImporter.endpoints": [
  "http://staging-publisher-1:8080/libs/sling/distribution/services/importers/default",
  "http://staging-publisher-2:8080/libs/sling/distribution/services/importers/default"
]
```

**Use Case:** Pre-production testing with multiple publishers

### Production

**Publisher Endpoints:**
```json
"packageImporter.endpoints": [
  "https://publisher-1.example.com/libs/sling/distribution/services/importers/default",
  "https://publisher-2.example.com/libs/sling/distribution/services/importers/default",
  "https://publisher-3.example.com/libs/sling/distribution/services/importers/default"
]
```

**Additional Configuration:**
- Use HTTPS (not HTTP)
- Configure authentication (mutual TLS)
- Increase retry attempts for network reliability
- Add monitoring/alerting

---

## Troubleshooting

### Agent Not Visible in UI

**Symptom:** Agent doesn't appear in `/cms/publication/agents.html`

**Solution:**
1. Check agent is enabled: `"enabled": true`
2. Verify OSGi config deployed: `/system/console/configMgr`
3. Check logs for errors: `grep "ForwardDistributionAgent" logs/error.log`

### Queue Growing (Items Not Processed)

**Symptom:** Queue depth increasing, items not distributed

**Possible Causes:**
1. **Publisher offline** - Check publisher is running at configured endpoint
2. **Network issue** - Verify connectivity: `curl http://localhost:8083`
3. **Authentication failure** - Check credentials/permissions
4. **Package too large** - Check package size, may timeout

**Solution:**
- Check agent state in JMX
- Review error logs
- Test endpoint manually
- Verify service user permissions

### Binaries Still Base64-Encoded

**Symptom:** Large package sizes, binaries encoded

**Solution:**
- Verify `useBinaryReferences: true` in package builder config
- Restart author instance to pick up config
- Check FileVault version supports binary references

### Retry Not Working

**Symptom:** Failed items not retrying

**Solution:**
- Verify retry configuration:
  - `retry.strategy: exponential`
  - `retry.attempts:Integer: 3`
- Check queue provider supports retry
- Review agent logs for retry attempts

---

## Next Steps

Now that agent configuration is complete, proceed with:

1. ✅ **Phase 1 Complete** - Agent configured
2. 🔲 **Phase 2** - Integrate with PublicationManager
   - Create `SlingDistributionPublicationManager`
   - Use `Distributor` API
   - See [Migration Plan](sling-distribution-migration-plan.md#phase-2-integrate-with-publicationmanager-week-2)
3. 🔲 **Phase 3** - Testing
4. 🔲 **Phase 4** - Remove custom HTTP distribution

---

## References

- [Sling Content Distribution Documentation](https://sling.apache.org/documentation/bundles/content-distribution.html)
- [FileVault Package Format](https://jackrabbit.apache.org/filevault/overview.html)
- [Distribution Agent Configuration](https://sling.apache.org/documentation/bundles/distribution/configuration.html)
- [Migration Plan](sling-distribution-migration-plan.md)

---

**Status:** ✅ Configuration Complete
**Next:** Implement SlingDistributionPublicationManager
