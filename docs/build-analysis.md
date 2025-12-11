# Apache Sling CMS - Build Analysis

This document provides an analysis of the build times for each module in the Apache Sling CMS project.

## Build Environment

- **Date**: December 11, 2025
- **Java Version**: 21
- **Maven Version**: 3.x
- **Build Command**: `mvn clean install -DskipTests -Dbnd.baseline.skip=true`
- **Machine**: macOS

## Module Build Times

| # | Module | Build Time | % of Total | Description |
|---|--------|------------|------------|-------------|
| 1 | **Apache Sling - CMS** (Parent POM) | 2.991s | 7.7% | Parent POM with shared configuration |
| 2 | **Apache Sling - CMS API** | 1.423s | 3.7% | API interfaces and models (58 Java files) |
| 3 | **Apache Sling - CMS Core** | 2.006s | 5.1% | Core implementation (97 Java files) |
| 4 | **Apache Sling - Login Fragment** | 0.068s | 0.2% | Login UI fragment |
| 5 | **Apache Sling - CMS Frontend** | 6.123s | 15.7% | Frontend assets (JS, CSS, Vite build) |
| 6 | **Apache Sling - CMS UI** | 0.543s | 1.4% | UI components and HTL templates |
| 7 | **Apache Sling - CMS Reference** | 1.269s | 3.3% | Reference implementation |
| 8 | **Apache Sling - CMS Distribution** | 0.264s | 0.7% | Content distribution module |
| 9 | **Apache Sling Thumbnail Support** | 1.992s | 5.1% | Thumbnail generation |
| 10 | **Apache Sling - CMS Feature Model** | 19.956s | 51.2% | Feature aggregation and packaging |
| 11 | **Apache Sling - CMS Archetype** | 0.871s | 2.2% | Maven archetype |
| 12 | **Apache Sling - CMS Integration Tests** | 0.072s | 0.2% | Integration tests (skipped) |

## Total Build Time

| Metric | Value |
|--------|-------|
| **Total Build Time** | **38.961 seconds** |
| **Modules Built** | 12 |
| **Average per Module** | 3.25 seconds |

## Build Time Distribution

```
Feature Model  ████████████████████████████████████████████████████ 51.2%
Frontend       ████████████████ 15.7%
Parent POM     ████████ 7.7%
Core           █████ 5.1%
Thumbnails     █████ 5.1%
API            ████ 3.7%
Reference      ███ 3.3%
Archetype      ██ 2.2%
UI             █ 1.4%
Distribution   █ 0.7%
Login          ▏ 0.2%
IT             ▏ 0.2%
```

## Analysis & Observations

### 1. Feature Model (51.2% - 19.96s)
The **Feature Model** module takes the most time because it:
- Aggregates multiple feature JSON files
- Analyzes features for dependencies and capabilities
- Creates multiple feature archives (standalone, author, renderer, upgrade)
- Builds multiple assembly JARs

### 2. Frontend (15.7% - 6.12s)
The **Frontend** module is the second slowest because it:
- Installs Node.js and npm (if needed)
- Runs `npm ci` to install dependencies
- Executes Vite build for production (`npm run build:prod`)
- Compiles SCSS to CSS
- Bundles JavaScript modules

### 3. Fast Modules (<1s)
Several modules build very quickly:
- **Login Fragment** (0.068s) - Minimal resources
- **Integration Tests** (0.072s) - Tests skipped
- **Distribution** (0.264s) - Small codebase
- **UI** (0.543s) - HTL templates only

### 4. Java Compilation Modules
Modules with Java code compile quickly:
- **API** (1.42s) - 58 Java files
- **Core** (2.01s) - 97 Java files
- **Reference** (1.27s) - Moderate codebase
- **Thumbnails** (1.99s) - Image processing code

## Optimization Recommendations

### Quick Wins

1. **Skip Feature Model during development**
   ```bash
   mvn clean install -pl !feature -DskipTests
   ```
   This can save ~20 seconds per build.

2. **Build only changed modules**
   ```bash
   mvn install -pl core -DskipTests -Dbnd.baseline.skip=true
   ```

3. **Use the dev profile for Frontend**
   ```bash
   mvn install -pl frontend -P dev -DskipTests
   ```
   Uses development build instead of production.

4. **Use Bun for faster Frontend builds** ⚡
   ```bash
   mvn install -pl frontend -P bun -DskipTests -Dbnd.baseline.skip=true
   ```
   Bun provides faster package installation and script execution.

### Bun vs npm Comparison

| Build Type | npm Time | Bun Time | Improvement |
|------------|----------|----------|-------------|
| Clean build (fresh install) | 10.7s | 9.4s | **~12% faster** |
| Incremental build (cached) | 8.0s | 5.3s | **~34% faster** |

**Why Bun is faster:**
- Global module cache with hard links
- Parallel dependency downloads
- Native TypeScript/JSX transpiler
- Faster startup time

**To install Bun:**
```bash
curl -fsSL https://bun.sh/install | bash
```

**Available Bun profiles:**
- `-P bun` - Production build with Bun
- `-P bun-dev` - Development build with Bun

### Module-Specific Builds

| Use Case | Command | Time Saved |
|----------|---------|------------|
| API changes | `mvn install -pl api -DskipTests` | ~37s |
| Core changes | `mvn install -pl core -DskipTests` | ~36s |
| UI changes | `mvn install -pl ui -DskipTests` | ~38s |
| Frontend changes | `mvn install -pl frontend -DskipTests` | ~33s |

### VS Code Tasks

Use the pre-configured VS Code tasks for hot deployment:
- **Auto Deploy - Core** - Deploys core module
- **Auto Deploy - UI** - Deploys UI module
- **Auto Deploy - Frontend** - Deploys frontend module
- **Deploy All Modules** - Deploys api, core, login, ui, reference, thumbnails

## Build Profiles

| Profile | Description |
|---------|-------------|
| `autoInstallBundle` | Deploy bundle to running server |
| `dev` | Development build (frontend) |
| `watch` | Watch mode for frontend changes |
| `bun` | Use Bun for production frontend build ⚡ |
| `bun-dev` | Use Bun for development frontend build ⚡ |

## Full Build vs Incremental

| Build Type | Command | Time |
|------------|---------|------|
| Full Clean Build | `mvn clean install -DskipTests` | ~39s |
| Incremental Build | `mvn install -DskipTests` | ~25s* |
| Single Module | `mvn install -pl <module> -DskipTests` | 1-7s |

*Incremental builds are faster because Maven skips unchanged modules.

## Recommendations for Developers

1. **During active development**: Build only the module you're working on
2. **Before committing**: Run full build to ensure no regressions
3. **For frontend work**: Use `npm run dev` in the frontend directory for hot reload
4. **For testing deployment**: Use VS Code tasks with `autoInstallBundle` profile

---

*Last updated: December 11, 2025*
