# Jakarta Namespace Migration Plan

## Overview

This document outlines the migration plan for transitioning from `javax.*` to `jakarta.*` namespaces in the Apache Sling CMS application. This migration is required for compatibility with Jakarta EE 9+ and modern Sling versions.

## Current State Analysis

### Namespaces Currently in Use

| Namespace | Usage Count | Migration Required |
|-----------|-------------|-------------------|
| `javax.servlet.*` | ~30+ files | ✅ Yes → `jakarta.servlet.*` |
| `javax.inject.*` | ~10 files | ✅ Yes → `jakarta.inject.*` |
| `javax.jcr.*` | ~50+ files | ❌ No (JCR API remains javax) |
| `javax.annotation.*` | ~5 files | ✅ Yes → `jakarta.annotation.*` |
| `jakarta.mail.*` | Already migrated | ✅ Done |
| `jakarta.activation.*` | Already migrated | ✅ Done |

### Important Note
**`javax.jcr.*` packages DO NOT migrate** - The JCR (Java Content Repository) API is not part of Jakarta EE and will remain in the `javax` namespace.

---

## Prerequisites

### 1. Dependency Updates Required

The following Sling/OSGi dependencies must be updated to Jakarta-compatible versions:

```xml
<!-- Current → Target versions (Jakarta EE 9 compatible) -->
org.apache.sling.api: 2.27.6 → 2.30.0+
org.apache.sling.engine: 2.15.18 → 2.16.0+
org.apache.sling.servlets.post: 2.5.0 → 2.6.0+
org.apache.felix.http.servlet-api: 3.0.0 → 3.0.0+ (jakarta.servlet 5.0 for Java 11)
```

### 2. Java Version Compatibility

| Jakarta EE Version | Java Requirement | Servlet API | Status |
|-------------------|------------------|-------------|--------|
| Jakarta EE 8 | Java 8+ | javax.servlet 4.0 | ❌ Still uses javax |
| **Jakarta EE 9** | **Java 11+** | **jakarta.servlet 5.0** | ✅ **Target Version** |
| Jakarta EE 9.1 | Java 11+ | jakarta.servlet 5.0 | ✅ Compatible |
| Jakarta EE 10 | Java 17+ | jakarta.servlet 6.0 | ❌ Requires Java upgrade |

**Decision**: Target **Jakarta EE 9/9.1** which fully supports **Java 11** (current project version).

```xml
<!-- Target versions for Java 11 compatibility -->
<jakarta.servlet-api-version>5.0.0</jakarta.servlet-api-version>
<jakarta.inject-api-version>2.0.1</jakarta.inject-api-version>
<jakarta.annotation-api-version>2.0.0</jakarta.annotation-api-version>
```

### 3. OSGi Framework
Ensure all OSGi bundles support Jakarta namespace. The Feature Model bundles in `feature/src/main/features/` need updates.

---

## Migration Phases

### Phase 1: Preparation (1-2 days)

#### 1.1 Backup and Branch
```bash
git checkout -b feature/jakarta-migration
```

#### 1.2 Update Parent POM Dependencies
Update `/pom.xml`:

```xml
<!-- Replace javax.servlet dependencies (Jakarta EE 9 for Java 11) -->
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>5.0.0</version>
    <scope>provided</scope>
</dependency>

<!-- Replace javax.annotation dependencies (Jakarta EE 9 for Java 11) -->
<dependency>
    <groupId>jakarta.annotation</groupId>
    <artifactId>jakarta.annotation-api</artifactId>
    <version>2.0.0</version>
    <scope>provided</scope>
</dependency>

<!-- Replace javax.inject dependencies -->
<dependency>
    <groupId>jakarta.inject</groupId>
    <artifactId>jakarta.inject-api</artifactId>
    <version>2.0.1</version>
    <scope>provided</scope>
</dependency>
```

---

### Phase 2: Code Migration (2-3 days)

#### 2.1 Files Requiring javax.servlet → jakarta.servlet Migration

**API Module** (`api/`):
- No direct servlet imports (uses Sling API abstractions)

**Core Module** (`core/`):
| File | Changes Required |
|------|------------------|
| `core/src/main/java/org/apache/sling/cms/core/insights/impl/InsightsWebConsole.java` | `javax.servlet.*` → `jakarta.servlet.*` |
| `core/src/test/java/org/apache/sling/cms/core/internal/servlets/PreviewFileServletTest.java` | `javax.servlet.*` → `jakarta.servlet.*` |
| `core/src/test/java/org/apache/sling/cms/core/internal/servlets/DownloadFileServletTest.java` | `javax.servlet.*` → `jakarta.servlet.*` |
| `core/src/test/java/org/apache/sling/cms/core/internal/filters/EditIncludeFilterTest.java` | `javax.servlet.*` → `jakarta.servlet.*` |
| `core/src/test/java/org/apache/sling/cms/core/internal/filters/CMSSecurityConfigInstanceTest.java` | `javax.servlet.*` → `jakarta.servlet.*` |

**Reference Module** (`reference/`):
| File | Changes Required |
|------|------------------|
| `reference/src/main/java/org/apache/sling/cms/reference/forms/impl/FormHandler.java` | `javax.servlet.*` → `jakarta.servlet.*` |
| `reference/src/test/java/org/apache/sling/cms/reference/forms/impl/FormHandlerTest.java` | `javax.servlet.*` → `jakarta.servlet.*` |

**Distribution Module** (`distribution/`):
| File | Changes Required |
|------|------------------|
| `distribution/src/main/java/org/apache/sling/cms/distribution/impl/ContentImportServlet.java` | `javax.servlet.*` → `jakarta.servlet.*` |

#### 2.2 Files Requiring javax.inject → jakarta.inject Migration

| File | Changes Required |
|------|------------------|
| `core/src/main/java/org/apache/sling/cms/core/models/ContentBreadcrumb.java` | `javax.inject.Inject` → `jakarta.inject.Inject` |
| `core/src/main/java/org/apache/sling/cms/core/models/QueryDebugger.java` | `javax.inject.Inject` → `jakarta.inject.Inject` |
| `reference/src/main/java/org/apache/sling/cms/reference/models/Search.java` | `javax.inject.*` → `jakarta.inject.*` |
| `reference/src/main/java/org/apache/sling/cms/reference/models/ItemList.java` | `javax.inject.*` → `jakarta.inject.*` |
| `reference/src/main/java/org/apache/sling/cms/reference/forms/impl/FormRequestImpl.java` | `javax.inject.Inject` → `jakarta.inject.Inject` |

#### 2.3 Files NOT Requiring Migration (javax.jcr.*)

These files use `javax.jcr.*` which **remains unchanged**:
- All JCR API usage (`javax.jcr.Node`, `javax.jcr.Session`, `javax.jcr.RepositoryException`, etc.)
- `javax.jcr.query.Query`
- `javax.jcr.version.*`

---

### Phase 3: POM File Updates (1 day)

#### 3.1 Root pom.xml Changes

```xml
<!-- REMOVE these properties -->
<servlet-api-version>2.5</servlet-api-version>
<jsp-api-version>2.0</jsp-api-version>

<!-- ADD these properties (Jakarta EE 9 versions for Java 11) -->
<jakarta.servlet-api-version>5.0.0</jakarta.servlet-api-version>
<jakarta.inject-api-version>2.0.1</jakarta.inject-api-version>
<jakarta.annotation-api-version>2.0.0</jakarta.annotation-api-version>

<!-- UPDATE dependency management -->
<!-- Remove -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>servlet-api</artifactId>
    <version>${servlet-api-version}</version>
</dependency>
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>jsp-api</artifactId>
    <version>${jsp-api-version}</version>
</dependency>

<!-- Add (Jakarta EE 9 - Java 11 compatible) -->
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>${jakarta.servlet-api-version}</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>jakarta.inject</groupId>
    <artifactId>jakarta.inject-api</artifactId>
    <version>${jakarta.inject-api-version}</version>
    <scope>provided</scope>
</dependency>
```

#### 3.2 Module-Specific POM Updates

**api/pom.xml**, **core/pom.xml**, **reference/pom.xml**, **distribution/pom.xml**, **ui/pom.xml**:
- Replace `javax.servlet:servlet-api` with `jakarta.servlet:jakarta.servlet-api`
- Replace `javax.servlet:javax.servlet-api` with `jakarta.servlet:jakarta.servlet-api`

---

### Phase 4: Feature Model Updates (1 day)

#### 4.1 boot.json Updates

Update `feature/src/main/features/boot.json`:
```json
{
    "id":"org.apache.felix:org.apache.felix.http.servlet-api:3.0.0",
    "start-order":"1"
}
```
Change to (Jakarta EE 9 / Java 11 compatible):
```json
{
    "id":"org.apache.felix:org.apache.felix.http.servlet-api:3.0.0",
    "start-order":"1"
}
```
> **Note**: Felix HTTP Servlet API 3.0.0 already provides jakarta.servlet 5.0 support. Check for Jakarta-compatible version at time of migration.
```

#### 4.2 base.json Updates

Update Sling servlet bundles in `feature/src/main/features/base.json`:
```json
{
    "id":"org.apache.sling:org.apache.sling.servlets.get:2.2.0" → "3.0.0+",
    "id":"org.apache.sling:org.apache.sling.servlets.post:2.6.0" → "2.7.0+",
    "id":"org.apache.sling:org.apache.sling.servlets.resolver:2.10.0" → "2.11.0+"
}
```

---

### Phase 5: Testing (2-3 days)

#### 5.1 Build Verification
```bash
# Clean build
mvn clean install

# Run all tests
mvn test

# Run integration tests
mvn verify -Pit
```

#### 5.2 Runtime Testing Checklist

- [ ] CMS starts without errors
- [ ] Admin console accessible
- [ ] Content editing works
- [ ] Form submissions work
- [ ] File uploads work
- [ ] User authentication works
- [ ] Content distribution works

#### 5.3 OSGi Bundle Verification
```bash
# Check bundle states in Felix console
http://localhost:8080/system/console/bundles
```

Verify no bundles in `Installed` state (should be `Active`).

---

## Automated Migration Script

Create a script to automate import replacements:

```bash
#!/bin/bash
# jakarta-migrate.sh

# Find and replace javax.servlet imports
find . -name "*.java" -type f -exec sed -i '' \
    -e 's/import javax\.servlet\./import jakarta.servlet./g' \
    -e 's/import javax\.inject\./import jakarta.inject./g' \
    -e 's/import javax\.annotation\./import jakarta.annotation./g' \
    {} \;

# Note: Do NOT replace javax.jcr imports!
echo "Migration complete. Please review changes and run tests."
```

---

## Rollback Plan

If issues occur:

1. **Revert code changes**:
   ```bash
   git checkout main -- .
   ```

2. **Restore dependencies**:
   - Revert POM changes
   - Rebuild with original dependencies

3. **Document issues** for future migration attempt

---

## Detailed Risk Assessment

### Risk Matrix

| Risk ID | Risk Description | Probability | Impact | Risk Score | Priority |
|---------|------------------|-------------|--------|------------|----------|
| R1 | OSGi Bundle Resolution Failure | High (70%) | Critical | 🔴 High | P1 |
| R2 | Sling API Version Incompatibility | Medium (50%) | Critical | 🔴 High | P1 |
| R3 | Runtime ClassNotFoundException | Medium (40%) | High | 🟠 Medium | P2 |
| R4 | Third-party Library Conflicts | Medium (35%) | Medium | 🟠 Medium | P2 |
| R5 | Test Suite Failures | High (60%) | Medium | 🟠 Medium | P2 |
| R6 | Feature Model Bundle Mismatch | Medium (45%) | High | 🟠 Medium | P2 |
| R7 | Servlet Filter Chain Issues | Low (25%) | High | 🟡 Low | P3 |
| R8 | JSP/JSTL Compatibility | Low (20%) | Medium | 🟡 Low | P3 |
| R9 | Performance Degradation | Low (15%) | Low | 🟢 Low | P4 |
| R10 | Documentation Gaps | Medium (40%) | Low | 🟢 Low | P4 |

---

### Detailed Risk Analysis

#### R1: OSGi Bundle Resolution Failure 🔴 HIGH

**Description**: OSGi bundles may fail to resolve due to missing or incompatible package imports/exports after namespace change.

**Affected Components**:
- `org.apache.sling.cms.core`
- `org.apache.sling.cms.reference`
- `org.apache.sling.cms.distribution`
- All servlet-related bundles

**Symptoms**:
```
org.osgi.framework.BundleException: Unable to resolve bundle
  missing requirement: osgi.wiring.package; 
  filter:="(&(osgi.wiring.package=jakarta.servlet)(version>=6.0.0))"
```

**Mitigation Strategies**:
1. Update `bnd.bnd` files with correct Import-Package directives
2. Test bundle resolution incrementally (one module at a time)
3. Use OSGi resolver analysis tools
4. Verify all transitive dependencies export Jakarta packages

**Contingency**:
- Keep javax and jakarta packages in parallel during transition
- Use Eclipse Transformer for bytecode conversion as fallback

---

#### R2: Sling API Version Incompatibility 🔴 HIGH

**Description**: Current Sling dependencies may not support Jakarta namespaces.

**Current vs Required Versions**:

| Dependency | Current | Required for Jakarta | Status |
|------------|---------|---------------------|--------|
| org.apache.sling.api | 2.27.6 | 2.30.0+ | ⚠️ Needs Update |
| org.apache.sling.engine | 2.15.18 | 2.16.0+ | ⚠️ Needs Update |
| org.apache.sling.servlets.post | 2.5.0 | 2.7.0+ | ⚠️ Needs Update |
| org.apache.sling.models.api | 1.5.0 | 1.6.0+ | ⚠️ Needs Update |
| org.apache.felix.http.servlet-api | 3.0.0 | 4.0.0+ | ⚠️ Needs Update |

**Mitigation Strategies**:
1. Review Apache Sling release notes for Jakarta compatibility
2. Test with Sling Starter 12+ which includes Jakarta support
3. Create compatibility matrix before migration
4. Engage Sling community for guidance

**Contingency**:
- Delay migration until all Sling dependencies are Jakarta-ready
- Fork and patch incompatible Sling components if critical

---

#### R3: Runtime ClassNotFoundException 🟠 MEDIUM

**Description**: Classes may fail to load at runtime due to split packages or incorrect classloader behavior.

**Common Scenarios**:
```java
// May fail if jakarta.servlet not properly exported
java.lang.ClassNotFoundException: jakarta.servlet.http.HttpServletRequest

// Or NoClassDefFoundError for nested classes
java.lang.NoClassDefFoundError: jakarta/servlet/ServletException
```

**Affected Areas**:
- Servlet initialization
- Filter chain execution
- Model injection with `@Inject`
- OSGi service lookups

**Mitigation Strategies**:
1. Comprehensive integration testing
2. Add explicit package imports in OSGi manifests
3. Review classloader delegation policies
4. Test with verbose class loading enabled

**Contingency**:
- Add runtime workarounds using reflection
- Bundle required packages directly (embed dependencies)

---

#### R4: Third-party Library Conflicts 🟠 MEDIUM

**Description**: Transitive dependencies may still use javax namespace, causing conflicts.

**Known Risk Areas**:

| Library | Current Version | Jakarta Support | Risk |
|---------|-----------------|-----------------|------|
| Handlebars | 4.4.0 | Unknown | Medium |
| Jsoup | 1.18.3 | N/A (no servlet) | Low |
| Apache POI | 5.4.0 | Unknown | Low |
| Apache Tika | 1.28.5 | No (needs 2.x+) | High |
| Apache PDFBox | 2.0.32 | Unknown | Low |

**Mitigation Strategies**:
1. Run `mvn dependency:tree` to analyze transitive dependencies
2. Use `mvn dependency:analyze` for conflict detection
3. Exclude conflicting transitive dependencies
4. Update third-party libraries to Jakarta-compatible versions

**Contingency**:
- Use Maven Shade plugin to relocate conflicting packages
- Create adapter classes for incompatible libraries

---

#### R5: Test Suite Failures 🟠 MEDIUM

**Description**: Unit and integration tests may fail due to mock object incompatibilities.

**Affected Test Infrastructure**:
- Sling Mock (uses servlet mocks)
- Mockito mocks for HttpServletRequest/Response
- JUnit test runners with servlet context

**Current Test Dependencies**:
```xml
<org-apache-sling-testing-sling-mock-junit4-version>3.5.2</org-apache-sling-testing-sling-mock-junit4-version>
<mockito-core-version>4.11.0</mockito-core-version>
```

**Required Updates**:
```xml
<!-- Need Jakarta-compatible versions -->
<org-apache-sling-testing-sling-mock-junit4-version>3.6.0+</org-apache-sling-testing-sling-mock-junit4-version>
<mockito-core-version>5.x+</mockito-core-version>
```

**Mitigation Strategies**:
1. Update test framework dependencies first
2. Run tests incrementally by module
3. Fix mock setup code for Jakarta types
4. Add test utilities for common patterns

**Contingency**:
- Temporarily disable failing tests with `@Ignore`
- Create test stubs for critical paths

---

#### R6: Feature Model Bundle Mismatch 🟠 MEDIUM

**Description**: Feature model JSON files may reference bundles that don't support Jakarta.

**Files at Risk**:
- `feature/src/main/features/boot.json`
- `feature/src/main/features/base.json`
- `feature/src/main/features/cms/*.json`

**Example Issues**:
```json
// This bundle may not export jakarta.servlet
{
    "id":"org.apache.felix:org.apache.felix.http.servlet-api:3.0.0"
}
```

**Mitigation Strategies**:
1. Audit all bundle versions in feature files
2. Use feature analyser to detect package conflicts
3. Test standalone deployment before full integration
4. Maintain bundle version compatibility matrix

**Contingency**:
- Create custom feature model with tested bundle versions
- Use feature model merging to override incompatible bundles

---

#### R7: Servlet Filter Chain Issues 🟡 LOW

**Description**: Custom servlet filters may break due to interface signature changes.

**Affected Files**:
```
core/src/main/java/org/apache/sling/cms/core/internal/filters/*.java
```

**API Changes**:
```java
// javax.servlet.Filter
void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)

// jakarta.servlet.Filter (same signature, different package)
void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
```

**Mitigation**: Direct import replacement should work; minimal code changes needed.

---

#### R8: JSP/JSTL Compatibility 🟡 LOW

**Description**: JSP pages using JSTL may need updates for Jakarta.

**Current Configuration**:
```xml
<jstl-version>1.2_1</jstl-version>
<!-- Uses org.apache.geronimo.bundles:jstl -->
```

**Required Update**:
```xml
<!-- Need Jakarta JSTL -->
<dependency>
    <groupId>org.glassfish.web</groupId>
    <artifactId>jakarta.servlet.jsp.jstl</artifactId>
    <version>3.0.1</version>
</dependency>
```

**Mitigation**: Most JSP code doesn't require changes, only dependencies.

---

#### R9: Performance Degradation 🟢 LOW

**Description**: Unlikely but possible performance impact from different implementations.

**Monitoring Points**:
- Servlet response times
- Bundle start times
- Memory usage patterns

**Mitigation**: Baseline performance metrics before migration.

---

#### R10: Documentation Gaps 🟢 LOW

**Description**: Internal documentation may reference javax packages.

**Affected Areas**:
- README files
- Inline code comments
- Wiki/Confluence pages

**Mitigation**: Search and update documentation post-migration.

---

### Risk Response Summary

| Risk | Response Strategy | Owner | Due Date |
|------|-------------------|-------|----------|
| R1 | Mitigate - Test incrementally | Dev Team | Phase 4 |
| R2 | Mitigate - Version audit | Tech Lead | Phase 1 |
| R3 | Accept - Fix as encountered | Dev Team | Phase 5 |
| R4 | Mitigate - Dependency analysis | Dev Team | Phase 1 |
| R5 | Mitigate - Update test frameworks | QA Team | Phase 2 |
| R6 | Mitigate - Feature analysis | Dev Team | Phase 4 |
| R7 | Accept - Low probability | Dev Team | Phase 2 |
| R8 | Mitigate - Update JSTL deps | Dev Team | Phase 3 |
| R9 | Monitor - Baseline metrics | Ops Team | Phase 5 |
| R10 | Accept - Post-migration task | Doc Team | Post-Phase 5 |

---

### Pre-Migration Checklist

Before starting the migration, complete this checklist:

- [ ] **Dependency Audit**: Run `mvn dependency:tree > deps.txt` and review
- [ ] **Bundle Analysis**: Run feature model analyser on current state
- [ ] **Test Coverage**: Ensure >80% test coverage on affected code
- [ ] **Backup**: Create tagged release of current state
- [ ] **Environment**: Set up isolated test environment
- [ ] **Communication**: Notify stakeholders of migration timeline
- [ ] **Rollback Plan**: Document and test rollback procedure
- [ ] **Performance Baseline**: Record current performance metrics

### Go/No-Go Criteria

**GO Criteria** (all must be true):
- [ ] All Sling dependencies verified Jakarta-compatible
- [ ] Test environment validated
- [ ] Rollback procedure tested
- [ ] Team availability confirmed for migration window
- [ ] Stakeholder approval obtained

**NO-GO Criteria** (any one triggers delay):
- [ ] Critical Sling dependency lacks Jakarta support
- [ ] Blocking third-party library issues identified
- [ ] Test coverage below 70%
- [ ] No available rollback window

---

## Timeline Summary

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase 1: Preparation | 1-2 days | None |
| Phase 2: Code Migration | 2-3 days | Phase 1 |
| Phase 3: POM Updates | 1 day | Phase 2 |
| Phase 4: Feature Model | 1 day | Phase 3 |
| Phase 5: Testing | 2-3 days | Phase 4 |
| **Total** | **7-10 days** | |

---

## References

- [Jakarta EE 9 Migration Guide](https://jakarta.ee/resources/jakarta-ee-9-migration-guide/)
- [Apache Sling Jakarta Migration](https://sling.apache.org/documentation/development/jakarta.html)
- [Eclipse Transformer](https://github.com/eclipse/transformer) - Automated bytecode transformation tool
- [Apache Felix HTTP Service](https://felix.apache.org/documentation/subprojects/apache-felix-http-service.html)

---

## Appendix: Complete File List

### Files with javax.servlet imports (to migrate):
```
core/src/main/java/org/apache/sling/cms/core/insights/impl/InsightsWebConsole.java
core/src/test/java/org/apache/sling/cms/core/internal/servlets/PreviewFileServletTest.java
core/src/test/java/org/apache/sling/cms/core/internal/servlets/DownloadFileServletTest.java
core/src/test/java/org/apache/sling/cms/core/internal/filters/EditIncludeFilterTest.java
core/src/test/java/org/apache/sling/cms/core/internal/filters/CMSSecurityConfigInstanceTest.java
distribution/src/main/java/org/apache/sling/cms/distribution/impl/ContentImportServlet.java
reference/src/main/java/org/apache/sling/cms/reference/forms/impl/FormHandler.java
reference/src/test/java/org/apache/sling/cms/reference/forms/impl/FormHandlerTest.java
```

### Files with javax.inject imports (to migrate):
```
core/src/main/java/org/apache/sling/cms/core/models/ContentBreadcrumb.java
core/src/main/java/org/apache/sling/cms/core/models/QueryDebugger.java
reference/src/main/java/org/apache/sling/cms/reference/models/Search.java
reference/src/main/java/org/apache/sling/cms/reference/models/ItemList.java
reference/src/main/java/org/apache/sling/cms/reference/forms/impl/FormRequestImpl.java
```

### Files with javax.jcr imports (NO migration needed):
```
# These stay as javax.jcr.*
core/src/main/java/org/apache/sling/cms/core/models/VersionInfo.java
core/src/main/java/org/apache/sling/cms/core/models/QueryDebugger.java
core/src/main/java/org/apache/sling/cms/core/i18n/impl/I18NProviderImpl.java
reference/src/main/java/org/apache/sling/cms/reference/models/Search.java
reference/src/main/java/org/apache/sling/cms/reference/models/ItemList.java
# ... and many more test files
```

---

*Document Version: 1.0*
*Created: December 8, 2025*
*Last Updated: December 8, 2025*
