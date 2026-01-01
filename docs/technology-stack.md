# Technology Stack & Tools Analysis

This document provides a comprehensive review of all tools and technologies used in Apache Sling CMS, along with recommendations for improvements and modernization.

## Table of Contents

1. [Overview](#overview)
2. [Core Platform](#core-platform)
3. [Build & Development Tools](#build--development-tools)
4. [Frontend Technologies](#frontend-technologies)
5. [Testing Framework](#testing-framework)
6. [DevOps & Deployment](#devops--deployment)
7. [Security](#security)
8. [Version Summary](#version-summary)
9. [Recommendations](#recommendations)

---

## Overview

Apache Sling CMS is built on the Apache Sling web framework, which provides a RESTful web application framework based on Java Content Repository (JCR) technology. The project uses a modern multi-module Maven build system with comprehensive frontend tooling.

| Category | Primary Technology |
|----------|-------------------|
| **Language** | Java 21 |
| **Framework** | Apache Sling |
| **Repository** | Apache Jackrabbit Oak |
| **Build Tool** | Maven 3.x |
| **Frontend** | Vite + Rollup |
| **Testing** | JUnit + Cypress |
| **Container** | Docker |
| **Orchestration** | Kubernetes (Helm) |

---

## Core Platform

### Java Runtime

| Component | Version | Status |
|-----------|---------|--------|
| Java SE | **21** | ✅ Current LTS |
| Target Bytecode | 21 | ✅ Modern |

**Analysis**: Using Java 21 (latest LTS) is excellent for performance and security.

### Apache Sling Framework

| Component | Version | Purpose |
|-----------|---------|---------|
| Sling API | 2.27.6 | Core Sling interfaces |
| Sling Engine | 2.15.18 | Request processing |
| Sling Models | 1.5.0 | Annotation-driven models |
| Sling Scripting API | 2.2.2 | Script engine support |
| Sling I18n | 2.5.18 | Internationalization |
| Sling Distribution | 0.7.2 | Content distribution |
| Sling CAConfig | 1.3.0 | Context-Aware Configuration |

### Content Repository

| Component | Version | Purpose |
|-----------|---------|---------|
| Apache Jackrabbit | 2.22.2 | JCR implementation |
| Apache Jackrabbit Oak | **1.78.0** | Modern JCR backend |
| Oak Lucene | 1.78.0 | Full-text search (see [search.md](search.md)) |
| Oak Segment | 1.78.0 | Segment store |

**Analysis**: Oak 1.78.0 is recent and provides excellent performance with the segment-tar storage model.

### OSGi Framework

| Component | Version | Purpose |
|-----------|---------|---------|
| Apache Felix Framework | 7.0.5 | OSGi runtime |
| Felix ConfigAdmin | 1.9.26 | Configuration management |
| Felix EventAdmin | 1.6.4 | Event handling |
| Felix SCR | (bundled) | Declarative Services |
| OSGi Annotations | 8.1.0 | Component annotations |

**Analysis**: Using modern OSGi R8 annotations and latest Felix framework.

### Scripting Engines

| Engine | Version | Usage |
|--------|---------|-------|
| **HTL (Sightly)** | 1.4.22 | Primary templating |
| **JSP** | 2.6.2 | Legacy templates (218 files) |
| FreeMarker | 2.3.32 | Optional templating |
| Thymeleaf | 3.0.15 | Optional templating |
| Groovy | 4.0.24 | Groovy console |
| JavaScript (Rhino) | 1.7.14 | Server-side JS |

**Analysis**: Strong support for multiple scripting languages. HTL is the recommended modern approach.

---

## Build & Development Tools

### Maven Build System

| Plugin | Version | Purpose |
|--------|---------|---------|
| sling-bundle-parent | 62 | Parent POM |
| sling-maven-plugin | 3.0.2 | Bundle deployment |
| bnd-maven-plugin | 7.0.0 | OSGi bundle creation |
| slingfeature-maven-plugin | 1.7.2 | Feature model assembly |
| animal-sniffer-plugin | - | API compatibility |
| apache-rat-plugin | 0.16.1 | License checking |
| maven-resources-plugin | - | Resource handling |

### Frontend Build Tools

| Tool | Version | Purpose |
|------|---------|---------|
| **Vite** | 6.3.5 | Modern build tool |
| Rollup | 4.9.1 | Module bundler |
| PostCSS | 8.5.6 | CSS processing |
| Sass (embedded) | 1.93.3 | CSS preprocessing |
| Node.js | 20.18.1 | JavaScript runtime |
| npm | 10.8.2 | Package manager |
| Bun | ≥1.0.0 | Alternative runtime (optional) |

**Analysis**: Vite 6.x is cutting-edge. The project supports both npm and Bun for flexibility.

---

## Frontend Technologies

### CSS Framework

| Library | Version | Purpose |
|---------|---------|---------|
| **Bulma** | 1.0.4 | CSS framework |
| Jam Icons | 2.0.0 | Icon library |

### JavaScript Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| **Tiptap** | 3.13.0 | Rich text editor |
| Handlebars | 4.7.6 | Client-side templating |
| DOMPurify | 3.3.0 | XSS sanitization |
| js-autocomplete | 1.0.4 | Autocomplete widget |
| rava | 2.1.0 | Event handling |
| sorttable | 1.0.2 | Table sorting |

### Tiptap Extensions (Rich Text Editor)

The project uses a comprehensive set of Tiptap extensions for the rich text editor:

- Core editing: Document, Paragraph, Text, History
- Formatting: Bold, Italic, Underline, Text Align
- Structure: Heading, Blockquote, Code Block
- Lists: Bullet List, Ordered List, List Item
- Media: Image, Link, Hard Break

**Analysis**: Tiptap 3.x is the latest major version - excellent choice for modern rich text editing.

---

## Testing Framework

### Backend Testing

| Tool | Version | Purpose |
|------|---------|---------|
| JUnit | 4.x | Unit testing |
| Mockito | 5.14.2 | Mocking framework |
| Sling Mock | 3.5.2 | Sling testing |
| Sling Mock Oak | 4.1.0 | Repository mocking |

### Frontend/E2E Testing

| Tool | Version | Purpose |
|------|---------|---------|
| **Cypress** | 12.2.0 | End-to-end testing |
| Cypress Audit | 1.1.0 | Performance auditing |
| Pa11y (Cypress) | 1.3.1 | Accessibility testing |
| Chai | 4.3.7 | Assertion library |

**Analysis**: Cypress with Pa11y integration provides excellent E2E testing with accessibility checks.

---

## DevOps & Deployment

### Containerization

| Tool | Version | Purpose |
|------|---------|---------|
| **Docker** | - | Container runtime |
| Docker Compose | 3.4 | Multi-container orchestration |
| Eclipse Temurin | 21 (image) | Base JRE image |

**Docker Images:**
- `cms/Dockerfile` - Main CMS image
- `webcache/` - Apache HTTP cache layer

### Container Orchestration

| Tool | Version | Purpose |
|------|---------|---------|
| **Helm** | v3 | Kubernetes package manager |
| Kubernetes | - | Container orchestration |

**Helm Charts:**
- `slingcms-standalone` - Standalone deployment chart

### Virtualization

| Tool | Purpose |
|------|---------|
| Vagrant | Development VMs |
| VirtualBox | VM provider |
| CentOS 7 | Base VM image |

### CI/CD

| Tool | Purpose |
|------|---------|
| **Jenkins** | CI/CD pipeline |
| `slingOsgiBundleBuild()` | ASF standard pipeline |

---

## Security

### Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| OWASP Encoder | 1.2.3 | Output encoding |
| DOMPurify | 3.3.0 | HTML sanitization |
| ESAPI | (bundled) | Security controls |

### Security Features

- Form-based authentication
- JAAS integration (Apache Felix JAAS 1.0.2)
- XSS protection via OWASP encoder
- CSRF protection
- Content Security Policy support

---

## Document Processing

| Library | Version | Purpose |
|---------|---------|---------|
| Apache Tika | 1.28.5 | Content detection |
| Apache PDFBox | 3.0.3 | PDF processing |
| Apache POI | 5.4.0 | Office documents |
| Thumbnailator | 0.4.20 | Image thumbnails |
| JSoup | 1.18.3 | HTML parsing |

---

## JSON & Data Processing

| Library | Version | Purpose |
|---------|---------|---------|
| Jackson | 2.18.2 | JSON processing |
| Apache Johnzon | 2.0.0 | JSON-P/JSON-B |
| Commons Text | 1.13.0 | Text utilities |
| Commons Lang3 | 3.17.0 | Java utilities |
| Commons IO | 2.18.0 | I/O utilities |
| Google Guava | 33.3.1-jre | Collections/utilities |

---

## Version Summary

### Current Versions (December 2025)

| Category | Technology | Version | Latest Available | Status |
|----------|------------|---------|------------------|--------|
| **Runtime** | Java | 21 | 21 LTS | ✅ Current |
| **Framework** | Sling API | 2.27.6 | 2.27.x | ✅ Current |
| **Repository** | Oak | 1.78.0 | 1.78.x | ✅ Current |
| **Build** | Vite | 6.3.5 | 6.x | ✅ Current |
| **CSS** | Bulma | 1.0.4 | 1.0.x | ✅ Current |
| **Editor** | Tiptap | 3.13.0 | 3.x | ✅ Current |
| **Testing** | Cypress | 12.2.0 | 14.x | ⚠️ Update Available |
| **Testing** | Mockito | 5.14.2 | 5.x | ✅ Current |
| **JSON** | Jackson | 2.18.2 | 2.18.x | ✅ Current |
| **Utils** | Guava | 33.3.1 | 33.x | ✅ Current |
| **Docker** | Temurin | 21 | 21 | ✅ Updated |

---

## Recommendations

### ✅ Completed

#### 1. ~~Update Docker Base Image to Java 21~~ ✅ DONE

**Updated**: `eclipse-temurin:11-jre` → `eclipse-temurin:21-jre`

```dockerfile
# docker/cms/Dockerfile
FROM eclipse-temurin:21-jre
```

**Status**: Completed December 2025. Docker image now matches project Java 21 requirement.

### 🔴 High Priority

#### 2. Upgrade Cypress to Latest

**Current**: 12.2.0
**Recommended**: 14.x

```json
// it/package.json
"cypress": "^14.0.0"
```

**Rationale**: Major improvements in performance, debugging, and component testing.

#### 3. Migrate JSP to HTL

**Current State**: 218 JSP files, 18 HTL files
**Recommendation**: Gradually migrate JSP templates to HTL

**Rationale**: 
- HTL is more secure (auto-escaping)
- Better separation of concerns
- Modern best practice for Sling/AEM

### 🟡 Medium Priority

#### 4. Consider JUnit 5 Migration

**Current**: JUnit 4.x with Sling Mock
**Recommended**: JUnit 5 (Jupiter)

**Rationale**: 
- Better test organization with `@Nested`
- Parameterized tests improvements
- Extension model more flexible

#### 5. Add GitHub Actions as Alternative CI

**Current**: Jenkins only
**Recommended**: Add `.github/workflows/` for GitHub Actions

```yaml
# .github/workflows/build.yml
name: Build
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - run: mvn clean verify
```

**Rationale**: Enables community contributions with PR checks.

#### 6. Update Apache Tika

**Current**: 1.28.5
**Recommended**: 2.9.x (Tika 2.x series)

**Rationale**: Tika 2.x has better performance and security fixes.

### 🟢 Low Priority / Nice to Have

#### 7. Consider Bun as Default for Frontend

**Current**: npm primary, Bun optional
**Recommendation**: Evaluate Bun as primary

**Rationale**: 
- 3-4x faster package installation
- Native TypeScript support
- Already configured in project

#### 8. Add TypeScript Support

**Current**: Pure JavaScript frontend
**Recommended**: Add TypeScript for type safety

```json
// frontend/src/main/frontend/package.json
"devDependencies": {
    "typescript": "^5.7.0",
    "@types/node": "^22.0.0"
}
```

#### 9. Consider Helm Chart for Author-Renderer Mode

**Current**: Only standalone Helm chart
**Recommended**: Add author-renderer Helm chart

#### 10. Add OpenTelemetry for Observability

**Recommendation**: Add OpenTelemetry Java agent support

```bash
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name=sling-cms \
     -jar standalone.jar
```

---

## Technology Lifecycle Status

| Technology | Status | Action |
|------------|--------|--------|
| Java 21 | ✅ Active LTS | Maintain |
| Oak 1.78 | ✅ Active | Maintain |
| Vite 6 | ✅ Active | Maintain |
| Bulma 1.0 | ✅ Active | Maintain |
| Tiptap 3 | ✅ Active | Maintain |
| JSP | ⚠️ Legacy | Migrate to HTL |
| JUnit 4 | ⚠️ Maintenance | Consider JUnit 5 |
| Cypress 12 | ⚠️ Outdated | Upgrade to 14 |
| Tika 1.x | ⚠️ Legacy | Upgrade to 2.x |
| Docker Java 21 | ✅ Updated | Completed |

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     Apache Sling CMS Stack                      │
├─────────────────────────────────────────────────────────────────┤
│  Frontend Layer                                                 │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐              │
│  │ Bulma   │ │ Tiptap  │ │ Vite    │ │ Cypress │              │
│  │ CSS     │ │ Editor  │ │ Build   │ │ Tests   │              │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘              │
├─────────────────────────────────────────────────────────────────┤
│  Scripting Layer                                                │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐              │
│  │ HTL     │ │ JSP     │ │Freemark │ │ Groovy  │              │
│  │(Sightly)│ │(Legacy) │ │   er    │ │ Console │              │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘              │
├─────────────────────────────────────────────────────────────────┤
│  Application Layer                                              │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Apache Sling Framework                     │    │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │    │
│  │  │  Models  │ │ Servlets │ │  Filters │ │ Services │  │    │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │    │
│  └────────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────┤
│  OSGi Layer                                                     │
│  ┌────────────────────────────────────────────────────────┐    │
│  │           Apache Felix Framework (OSGi R8)              │    │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │    │
│  │  │ConfigAdm │ │EventAdmin│ │   SCR    │ │  JAAS    │  │    │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │    │
│  └────────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────┤
│  Repository Layer                                               │
│  ┌────────────────────────────────────────────────────────┐    │
│  │            Apache Jackrabbit Oak 1.78                   │    │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │    │
│  │  │  JCR API │ │  Lucene  │ │ Segment  │ │  Query   │  │    │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │    │
│  └────────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────┤
│  Infrastructure Layer                                           │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐              │
│  │ Docker  │ │  Helm   │ │ Jenkins │ │ Vagrant │              │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘              │
├─────────────────────────────────────────────────────────────────┤
│  Runtime: Java 21 (Eclipse Temurin)                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## Related Documentation

- [Server Requirements](server-requirements.md)
- [Building](building.md)
- [Deployment Models](deployment-models.md)
- [Developers Guide](developers.md)

---

*Last Updated: December 2025*
