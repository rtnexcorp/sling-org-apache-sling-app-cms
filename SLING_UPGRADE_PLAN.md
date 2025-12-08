# Apache Sling Upgrade Plan

## Overview

This document outlines the upgrade plan for updating Apache Sling dependencies to the latest released versions (Sling 13). 

**Important Clarification**: Sling 13 supports **Java 11, 17, and 21**. Java 8 support was dropped, but **Java 11 is still fully supported**.

## Current State Analysis

### Sling 13 Java Compatibility

| Java Version | Sling 13 Support |
|--------------|------------------|
| Java 8 | ❌ No longer supported |
| **Java 11** | ✅ **Fully supported** |
| Java 17 | ✅ Supported |
| Java 21 | ✅ Supported |

### Current vs Latest Sling 13 Versions

| Component | Current Version | Sling 13 Version | Change Type |
|-----------|-----------------|------------------|-------------|
| **Sling Parent** | `sling-bundle-parent:49` | `sling:64` | ⚠️ Update |
| **Sling API** | `2.27.6` | `2.27.6` | ✅ Current! |
| **Sling Engine** | `2.15.18` | `2.16.0` | ✅ Minor Update |
| **Sling Models API** | `1.5.0` | `1.5.0` | ✅ Current! |
| **Sling Models Impl** | `1.6.4` | `1.6.4` | ✅ Current! |
| **Sling Servlets Get** | `2.2.0` | `2.2.0` | ✅ Current! |
| **Sling Servlets Post** | `2.5.0` / `2.6.0` | `2.6.0` | ✅ Current! |
| **Sling Servlets Resolver** | `2.10.0` | `2.10.0` | ✅ Current! |
| **Sling i18n** | `2.5.18` / `2.6.2` | `2.6.6` | ✅ Minor Update |
| **Sling Auth Core** | `1.7.0` | `1.7.0` | ✅ Current! |
| **Sling Auth Form** | `1.0.24` | `1.0.24` | ✅ Current! |
| **Sling Resource Resolver** | `1.11.0` | `1.11.0` | ✅ Current! |
| **Sling Commons Johnzon** | `1.2.14` / `1.2.16` | `2.0.0` | ⚠️ Major Update (jakarta.json) |
| **Sling Commons Log** | `5.4.2` | `5.5.0` | ✅ Minor Update |
| **Sling Commons Scheduler** | `2.7.12` | `2.7.14` | ✅ Minor Update |
| **Sling Commons Threads** | `3.2.22` | `3.3.0` | ✅ Minor Update |
| **Sling XSS** | `2.3.8` | `2.3.8` | ✅ Current! |
| **Sling JCR Resource** | `3.3.2` | `3.3.2` | ✅ Current! |
| **Sling Installer Core** | `3.12.0` | `3.12.0` | ✅ Current! |
| **Sling Repoinit Parser** | `1.9.0` | `1.9.0` | ✅ Current! |
| **Sling JCR Repoinit** | `1.1.44` | `1.1.44` | ✅ Current! |

### Related Dependencies (Sling 13 versions)

| Component | Current Version | Sling 13 Version | Notes |
|-----------|-----------------|------------------|-------|
| **Jackrabbit** | `2.20.12` | `2.22.0` | ⚠️ Update |
| **Oak** | `1.58.0` | `1.72.0` | ⚠️ Major Update |
| **ASM** | `9.6` | `9.6` | ✅ Current |
| **SLF4J** | `1.7.36` | `1.7.36` | ✅ Current |
| **Jackson** | `2.15.3` | `2.18.2` | ⚠️ Update |
| **Composum Nodes** | `4.2.2` | `4.3.4` | ⚠️ Update |
| **Felix HTTP Jetty** | `5.1.2` | `5.1.26` | ⚠️ Update |
| **Felix HTTP Servlet API** | `3.0.0` | `3.0.0` | ✅ Current |
| **Commons Lang3** | `3.17.0` | `3.17.0` | ✅ Current |
| **Commons IO** | `2.18.0` | `2.18.0` | ✅ Current |

---

## ✅ Good News: Your Project is Already Close to Sling 13!

Your project is already using many of the same versions as Sling 13. The upgrade is straightforward and **does NOT require Jakarta Servlet migration** (that's planned for Sling 14+).

### Key Sling 13 Changes:
1. **Dropped Java 8** - You're on Java 11, so no issue ✅
2. **jakarta.json** - JSON processing moved to Jakarta JSON API
3. **Oak 1.72.0** - Major Oak upgrade with performance improvements
4. **Felix HTTP Jetty 5.1.26** - Updated Jetty with security fixes

---

## Upgrade Path Options

### Option 1: Safe Incremental Update (Java 11 Compatible) 🟢

Update to latest javax-compatible versions (before Jakarta migration):

```xml
<!-- Maximum versions compatible with Java 11 and javax namespace -->
<properties>
    <!-- Sling Dependencies - Pre-Jakarta -->
---

## Upgrade Path: Sling 13 (Java 11 Compatible) ✅

Since Sling 13 fully supports Java 11, here's the recommended upgrade:

### Updates Required

```xml
<properties>
    <!-- Keep Java 11 - Sling 13 supports it! -->
    <sling.cms.java.version>11</sling.cms.java.version>
    
    <!-- Sling 13 Compatible Updates -->
    <org-apache-sling-engine-version>2.16.0</org-apache-sling-engine-version>
    <org-apache-sling-i18n-version>2.6.6</org-apache-sling-i18n-version>
    <org-apache-sling-commons-scheduler-version>2.7.14</org-apache-sling-commons-scheduler-version>
    <org-apache-sling-commons-threads-version>3.3.0</org-apache-sling-commons-threads-version>
    <org-apache-sling-commons-log-version>5.5.0</org-apache-sling-commons-log-version>
    <org-apache-sling-commons-johnzon-version>2.0.0</org-apache-sling-commons-johnzon-version>
    
    <!-- Jackrabbit/Oak Updates (Sling 13) -->
    <jackrabbit.version>2.22.0</jackrabbit.version>
    <oak.version>1.72.0</oak.version>
    
    <!-- Other Sling 13 Updates -->
    <jackson.version>2.18.2</jackson.version>
    <composum.nodes.version>4.3.4</composum.nodes.version>
</properties>
```

### Phase 1: Preparation (1 day)

1. **Create Branch**
   ```bash
   git checkout -b feature/sling-13-upgrade
   ```

2. **No Java Change Required** - Your Java 11 is fully supported!

### Phase 2: POM Updates (1 day)

Update `/pom.xml` with Sling 13 versions:

```xml
<!-- Parent POM (optional, for latest build tooling) -->
<parent>
    <groupId>org.apache.sling</groupId>
    <artifactId>sling-bundle-parent</artifactId>
    <version>52</version>  <!-- or keep 49 for minimal changes -->
</parent>

<!-- Updated properties -->
<org-apache-sling-engine-version>2.16.0</org-apache-sling-engine-version>
<org-apache-sling-i18n-version>2.6.6</org-apache-sling-i18n-version>
<org-apache-sling-commons-scheduler-version>2.7.14</org-apache-sling-commons-scheduler-version>
<org-apache-sling-commons-threads-version>3.3.0</org-apache-sling-commons-threads-version>
<org-apache-sling-commons-log-version>5.5.0</org-apache-sling-commons-log-version>

<!-- Jackrabbit/Oak (significant update) -->
<jackrabbit.version>2.22.0</jackrabbit.version>
<oak.version>1.72.0</oak.version>

<!-- Other updates -->
<jackson.version>2.18.2</jackson.version>
<composum.nodes.version>4.3.4</composum.nodes.version>
```

### Phase 3: Feature Model Updates (1 day)

Update `feature/src/main/features/base.json` with Sling 13 versions:

```json
// base.json updates (Sling 13)
{
    "id":"org.apache.sling:org.apache.sling.engine:2.16.0",
    "id":"org.apache.sling:org.apache.sling.i18n:2.6.6",
    "id":"org.apache.sling:org.apache.sling.commons.scheduler:2.7.14",
    "id":"org.apache.sling:org.apache.sling.commons.threads:3.3.0",
    "id":"org.apache.felix:org.apache.felix.http.jetty:5.1.26",
    // Oak updates
    "id":"org.apache.jackrabbit:oak-api:1.72.0",
    "id":"org.apache.jackrabbit:oak-core:1.72.0",
    // ... other oak bundles
}
```

### Phase 4: Testing (1-2 days)

1. **Build Verification**
   ```bash
   mvn clean install -DskipTests
   ```

2. **Unit Tests**
   ```bash
   mvn test
   ```

3. **Integration Tests**
   ```bash
   cd it && npm test
   ```

4. **Manual Testing**
   - Start local instance
   - Test CMS functionality
   - Test authentication
   - Test content authoring

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Oak 1.72.0 compatibility | Medium | Test repository operations thoroughly |
| Feature model bundle resolution | Medium | Test feature aggregation |
| Third-party bundle compatibility | Low | Check Composum and other bundles |
| Performance changes | Low | Monitor after upgrade |

---

## ⚠️ Migration Complexity Analysis (Updated)

After attempting the Sling 13 migration, the following critical issues were discovered:

### The javax.json → jakarta.json Challenge

Sling 13's upgrade to Commons Johnzon 2.0.0 introduces a **breaking change** - it uses `jakarta.json` namespace instead of `javax.json`. This affects many components:

#### Code Changes Required

Files using javax.json imports need updating:
```java
// OLD (current)
import javax.json.Json;
import javax.json.JsonArrayBuilder;

// NEW (Sling 13)
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
```

Affected files in this project:
- `core/src/main/java/org/apache/sling/cms/core/internal/servlets/PathSuggestionServlet.java`
- `core/src/main/java/org/apache/sling/cms/core/insights/impl/providers/HTMLValdiatorInsightProvider.java`
- `core/src/main/java/org/apache/sling/cms/core/insights/impl/providers/PageSpeedInsightProvider.java`

#### Bundle Version Updates Required

Many Sling bundles in the feature model need jakarta-compatible versions:

| Bundle | Current | Required for Sling 13 |
|--------|---------|----------------------|
| org.apache.sling.adapter | 2.2.0 | 2.2.2+ |
| org.apache.sling.bundleresource.impl | 2.3.4 | 2.3.6+ |
| org.apache.sling.caconfig.impl | 1.6.0 | 1.7.0+ |
| org.apache.sling.discovery.base | 2.0.14 | 2.0.16+ |
| org.apache.sling.discovery.commons | 1.0.28 | 1.0.30+ |
| org.apache.sling.distribution.core | 0.7.2 | TBD |
| org.apache.sling.feature | 1.3.0 | 1.4.0+ |
| org.apache.sling.fileoptim | 0.9.4 | TBD |
| org.apache.sling.fsresource | 2.2.0 | 2.3.0+ |
| org.apache.sling.installer.factory.feature | 0.7.0 | TBD |
| org.apache.sling.jcr.oak.server | 1.4.0 | 1.5.0+ |
| org.apache.sling.xss | 2.3.8 | 2.4.0+ |
| org.apache.felix.cm.json | 1.0.8 | 2.0.0+ |

#### Deprecated Modules Removed

- `jackrabbit-jcr-rmi` - Removed in Jackrabbit 2.22.0 (must remove from oak_base.json)

#### Dependency Version Bumps Required

For Oak 1.72.0 compatibility:
- `commons-codec` needs upgrade to 1.17.x
- `commons-io` needs upgrade to 2.17.x

### Recommended Strategy

**Option A: Defer Full Sling 13 Migration**
- Stay on current versions with security patches only
- Wait for clearer migration path from Apache Sling community

**Option B: Phased Migration**
1. Phase 1: Update non-JSON dependencies (Felix HTTP Jetty, Jackson, etc.)
2. Phase 2: Update Oak and Jackrabbit
3. Phase 3: Migrate javax.json → jakarta.json (code + bundles)
4. Phase 4: Full integration testing

**Option C: Fork Sling Starter 13**
- Reference the official Sling Starter 13 feature model
- Copy compatible bundle versions from official release

---

## Timeline Estimate (Sling 13 Upgrade)

| Phase | Duration | Description |
|-------|----------|-------------|
| Phase 1 | 1 day | Create branch, preparation |
| Phase 2 | 1 day | POM dependency updates |
| Phase 3 | 1 day | Feature model updates |
| Phase 4 | 1-2 days | Testing and validation |
| **Total** | **4-5 days** | Full Sling 13 upgrade |

---

## Future: Jakarta EE Migration (Sling 14+)

The Jakarta Servlet migration (`javax.servlet` → `jakarta.servlet`) is planned for **Sling 14** (in development). 

When Sling 14 is released:
- Will require **Java 17+**
- Will need full Jakarta namespace migration
- See `JAKARTA_MIGRATION_PLAN.md` for preparation

---

## Next Steps

1. **Create Branch**: `git checkout -b feature/sling-13-upgrade`
2. **Apply POM Updates**: Update versions as shown above
3. **Update Feature Models**: Update boot.json and base.json
4. **Test**: Build and run tests
5. **Deploy**: Test in staging environment

---

## References

- [Apache Sling 13 Release Notes](https://sling.apache.org/news/sling-13-released.html)
- [Apache Sling Downloads](https://sling.apache.org/downloads.cgi)
- [Sling Starter GitHub](https://github.com/apache/sling-org-apache-sling-starter)
- [JAKARTA_MIGRATION_PLAN.md](./JAKARTA_MIGRATION_PLAN.md) (for future Sling 14 upgrade)
