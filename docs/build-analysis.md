# Apache Sling CMS Build Analysis

This document provides a comprehensive analysis of build and deployment times for the Apache Sling CMS project.

For deployment instructions, see [Deployment Models](deployment-models.md).

## Quick Reference

| Build Tool | Total Time | Frontend Time | Feature Time | Improvement |
|------------|------------|---------------|--------------|-------------|
| **npm** (default) | 42.58s | 6.60s | 19.96s | baseline |
| **Bun** | 33.88s | 3.44s | 18.96s | **20% faster** |
| **Bun + Fast** | 22.17s | 3.44s | 6.92s | **48% faster** |

## Build Environment

- **Date**: December 11, 2025
- **Java Version**: 21
- **Maven Version**: 3.x
- **Node.js Version**: v20.18.1
- **Bun Version**: 1.3.4
- **Build Command**: `mvn clean install -DskipTests -Dbnd.baseline.skip=true`
- **Machine**: macOS (arm64 / Apple Silicon)

## Full Project Build Comparison

| Build Tool | Total Time | Frontend Time | Improvement |
|------------|------------|---------------|-------------|
| **npm** (default) | 42.58s | 6.60s | baseline |
| **Bun** | 33.88s | 3.44s | **20% faster overall** |

## Module Build Times (with Bun)

| # | Module | Build Time | % of Total | Description |
|---|--------|------------|------------|-------------|
| 1 | **Apache Sling - CMS** (Parent POM) | 2.67s | 7.9% | Parent POM with shared configuration |
| 2 | **Apache Sling - CMS API** | 1.43s | 4.2% | API interfaces and models (58 Java files) |
| 3 | **Apache Sling - CMS Core** | 1.85s | 5.5% | Core implementation (97 Java files) |
| 4 | **Apache Sling - Login Fragment** | 0.07s | 0.2% | Login UI fragment |
| 5 | **Apache Sling - CMS Frontend** | 3.44s | 10.2% | Frontend assets (JS, CSS, Vite build) |
| 6 | **Apache Sling - CMS UI** | 0.40s | 1.2% | UI components and HTL templates |
| 7 | **Apache Sling - CMS Reference** | 1.12s | 3.3% | Reference implementation |
| 8 | **Apache Sling - CMS Distribution** | 0.21s | 0.6% | Content distribution module |
| 9 | **Apache Sling Thumbnail Support** | 1.55s | 4.6% | Thumbnail generation |
| 10 | **Apache Sling - CMS Feature Model** | 18.96s | 56.0% | Feature aggregation and packaging |
| 11 | **Apache Sling - CMS Archetype** | 0.78s | 2.3% | Maven archetype |
| 12 | **Apache Sling - CMS Integration Tests** | 0.08s | 0.2% | Integration tests (skipped) |

## Build Time Distribution (Bun)

```
Feature Model  ████████████████████████████████████████████████████████ 56.0%
Frontend       ██████████ 10.2%
Parent POM     ████████ 7.9%
Core           ██████ 5.5%
Thumbnails     █████ 4.6%
API            ████ 4.2%
Reference      ███ 3.3%
Archetype      ██ 2.3%
UI             █ 1.2%
Distribution   █ 0.6%
Login          ▏ 0.2%
IT             ▏ 0.2%
```

## Single Module Deployment Times

Hot deployment to running Sling server (port 8082):

| Module | npm Time | Bun Time | Improvement |
|--------|----------|----------|-------------|
| **Frontend** | 8.78s | 6.17s | **30% faster** |
| **Core** | 2.90s | - | Java only |
| **UI** | 2.76s | - | Java only |
| **API** | 2.59s | - | Java only |

## Watch.sh Auto-Deploy Performance

When using `watch.sh` for development:

### Frontend Module (with Bun ⚡)
| Step | Time |
|------|------|
| bun install (cached) | 28-82ms |
| Vite build | ~1.7s |
| Maven packaging | ~2s |
| Sling deploy | ~1s |
| **Total** | **~5-6s** |

### Java Modules (Core, API, UI, etc.)
| Step | Time |
|------|------|
| spotless:apply | ~0.5s |
| Java compile | ~1s |
| Package bundle | ~0.5s |
| Sling deploy | ~0.5s |
| **Total** | **~2.5-3s** |

## Frontend Build Breakdown

### Package Installation
| Tool | Fresh Install | Cached |
|------|---------------|--------|
| npm ci | ~2-3s | ~1s |
| bun install | ~100ms | ~28ms |

### Vite Build Output
| Asset Type | Size | Gzipped |
|------------|------|---------|
| CSS (cms.min.css) | 698.58 kB | 69.03 kB |
| CSS (bulma.min.css) | 678.76 kB | 66.44 kB |
| JS (tiptap chunk) | 352.89 kB | 109.37 kB |
| JS (cms.bundle.min.js) | 23.15 kB | 6.28 kB |
| Fonts (jam-icons) | ~900 kB | 186 kB |

## Optimization Recommendations

### 1. Use Bun for Frontend (20% overall improvement)
```bash
mvn clean install -P bun -DskipTests -Dbnd.baseline.skip=true
```

### 2. Skip Feature Model During Development (saves ~19s)
```bash
mvn install -pl !feature -DskipTests -Dbnd.baseline.skip=true
```

### 3. Build Only Changed Module
```bash
# Frontend with Bun
mvn install -pl frontend -P autoInstallBundle,bun -DskipTests -Dbnd.baseline.skip=true

# Java modules
mvn install -pl core -P autoInstallBundle -DskipTests -Dbnd.baseline.skip=true
```

### 4. Use watch.sh for Automatic Deployment
```bash
./deployment/watch.sh
```
- Detects file changes automatically
- Uses Bun for frontend if available
- Deploys only the changed module

## VS Code Tasks

Use the pre-configured VS Code tasks for hot deployment:

| Task | Module | Build Tool | Approx Time |
|------|--------|------------|-------------|
| Auto Deploy - API | api | Maven | ~2.6s |
| Auto Deploy - Core | core | Maven | ~2.9s |
| Auto Deploy - UI | ui | Maven | ~2.8s |
| Auto Deploy - Login | login | Maven | ~1s |
| Auto Deploy - Frontend | frontend | npm | ~8.8s |
| Auto Deploy - Frontend (Bun) ⚡ | frontend | Bun | ~6.2s |
| Auto Deploy - Reference | reference | Maven | ~3s |
| Auto Deploy - Thumbnails | thumbnails | Maven | ~3s |
| Deploy All Modules | all | Maven | ~15s |

## Build Profiles

| Profile | Description |
|---------|-------------|
| `autoInstallBundle` | Deploy bundle to running server |
| `dev` | Development build (frontend) - no minification |
| `watch` | Watch mode for frontend changes |
| `bun` | Use Bun for production frontend build ⚡ |
| `bun-dev` | Use Bun for development frontend build ⚡ |
| `fast` | Fast feature model build (standalone only) ⚡ |
| `unified` | Single JAR with all profiles (standalone, author, renderer) ⚡ |

## Build Scenarios

| Scenario | Command | Time |
|----------|---------|------|
| Full Clean Build (npm) | `mvn clean install -DskipTests` | ~42s |
| Full Clean Build (Bun) | `mvn clean install -P bun -DskipTests` | ~34s |
| **Full Build (Bun + Fast)** | `mvn clean install -P bun,fast -DskipTests` | **~22s** |
| **Unified JAR Build** | `mvn clean install -P bun,unified -DskipTests` | **~16s** |
| Skip Feature Model | `mvn install -pl !feature -DskipTests` | ~15s |
| Single Java Module | `mvn install -pl core -DskipTests` | ~3s |
| Single Frontend (npm) | `mvn install -pl frontend -DskipTests` | ~9s |
| Single Frontend (Bun) | `mvn install -pl frontend -P bun -DskipTests` | ~6s |

## Unified JAR (Multi-Profile)

Instead of building 3 separate JARs (~207 MB each), use the `unified` profile to build a single JAR with all profiles:

```bash
# Build unified JAR
mvn clean install -pl feature -P unified -DskipTests

# Output: feature/target/org.apache.sling.cms-1.1.9-SNAPSHOT.jar (~590 MB)
```

### Running Different Profiles

```bash
# Default: standalone mode
java -jar org.apache.sling.cms-1.1.9-SNAPSHOT.jar

# Author mode
java -jar org.apache.sling.cms-1.1.9-SNAPSHOT.jar -P author
java -jar org.apache.sling.cms-1.1.9-SNAPSHOT.jar --profile author
java -jar org.apache.sling.cms-1.1.9-SNAPSHOT.jar --profile=author

# Renderer mode
java -jar org.apache.sling.cms-1.1.9-SNAPSHOT.jar -P renderer
```

### Profile Comparison

| Approach | JARs Created | Total Size | Use Case |
|----------|--------------|------------|----------|
| Default Build | 3 (standalone, author, renderer) | ~621 MB | Production distribution |
| `fast` Profile | 1 (standalone only) | ~207 MB | Development |
| `unified` Profile | 1 (all modes) | ~590 MB | Simplified deployment |

## Bottleneck Analysis

### Feature Model (56% → 31% with fast profile)
The biggest bottleneck. Default build:
- Processes 30 feature JSON files
- Creates 6 aggregated features (standalone, author, renderer, composite-seed, composite-runtime, upgrade)
- Runs 5 feature scans (analyse-features)
- Creates 4 feature archives (FAR files)
- Builds 3 large assembly JARs (~207 MB each!)

**Optimization**: Use `-P fast` profile for development:
```bash
mvn clean install -P fast -DskipTests
```

The `fast` profile:
- Builds only standalone aggregate
- Skips analyse-features goal
- Skips author and renderer assembly JARs
- **Reduces from ~19s to ~7s (63% faster)**

### Frontend (10% with Bun, 16% with npm)
Second slowest module due to:
- Package installation (npm ci / bun install)
- Vite build and bundling
- SCSS compilation
- Asset optimization

**Recommendation**: Use Bun profile for 30-48% improvement

## Developer Workflow Recommendations

| Activity | Recommended Approach | Time |
|----------|---------------------|------|
| Active Java development | Use watch.sh | ~3s per change |
| Active frontend development | Use watch.sh with Bun | ~5-6s per change |
| Quick test | Build single module | ~3-9s |
| Pre-commit validation | Full build with Bun | ~34s |
| CI/CD pipeline | Full build | ~42s |

---

*Last updated: December 11, 2025*
