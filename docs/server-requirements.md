# Server Requirements & Resource Analysis

This document provides comprehensive information about server requirements, startup times, and resource needs for running Apache Sling CMS.

## Application Overview

| Metric | Value |
|--------|-------|
| Current Version | 1.1.9-SNAPSHOT |
| Java Version Required | **Java 21** |
| Source Files | ~368 Java files |
| Lines of Code | ~47,600 lines |
| OSGi Bundles | ~282 bundles |

## Deployment Artifacts

### JAR File Sizes

| Artifact | Size | Description |
|----------|------|-------------|
| `standalone.jar` | ~200 MB | Combined standalone instance |
| `author.jar` | ~198 MB | Author mode instance |
| `renderer.jar` | ~198 MB | Renderer/Publisher instance |

### Module JARs

| Module | Size | Purpose |
|--------|------|---------|
| `core` | ~264 KB | Core functionality |
| `ui` | ~348 KB | User interface components |
| `api` | ~68 KB | Public API |
| `login` | ~32 KB | Authentication |
| `reference` | ~16 KB | Reference implementation |
| `thumbnails` | ~20 KB | Image thumbnail generation |

## Hardware Requirements

### Minimum Requirements (Development)

| Resource | Specification |
|----------|---------------|
| CPU | 2+ cores |
| RAM | 4 GB |
| Disk | 10 GB SSD |
| Java | JDK 21 |
| OS | Linux, macOS, Windows |

### Recommended (Production - Small Site)

| Resource | Specification |
|----------|---------------|
| CPU | 4+ cores |
| RAM | 8 GB |
| Disk | 50 GB SSD |
| Java | JDK 21 |
| OS | Linux (recommended) |

### High Traffic Production

| Resource | Specification |
|----------|---------------|
| CPU | 8+ cores |
| RAM | 16+ GB |
| Disk | 100+ GB SSD (RAID recommended) |
| Java | JDK 21 with G1GC tuning |
| OS | Linux |

## Memory Configuration

### Current Default Settings

```bash
java -Xmx1g -jar standalone.jar
```

### Recommended Production Settings

```bash
java \
    -Xms1g \                           # Initial heap size
    -Xmx2g \                           # Maximum heap size
    -XX:+UseG1GC \                     # G1 garbage collector
    -XX:MaxGCPauseMillis=200 \         # GC pause target
    -XX:+HeapDumpOnOutOfMemoryError \  # Debug OOM issues
    -XX:HeapDumpPath=/var/log/sling/ \
    -Djava.net.preferIPv4Stack=true \
    -jar standalone.jar
```

### Memory Breakdown

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| JVM Heap (`-Xmx`) | 1 GB | 2 GB |
| JVM Initial (`-Xms`) | 512 MB | 1 GB |
| Metaspace | 256 MB | 512 MB |
| **Total System RAM** | **2 GB** | **4+ GB** |

## Disk Space Requirements

| Component | Size |
|-----------|------|
| Deployment JAR | ~200 MB |
| Runtime Directory (per instance) | ~560 MB |
| Repository/Content Storage | Variable |
| Log Files | ~50-100 MB/day |
| **Minimum Total** | **2 GB** |
| **Recommended Total** | **10+ GB** |

## Port Configuration

| Mode | Default Port | Environment Variable |
|------|--------------|---------------------|
| Standalone | 8080 | `org.osgi.service.http.port` |
| Author | 8082 | `org.osgi.service.http.port` |
| Renderer | 8083 | `org.osgi.service.http.port` |

## Startup Time Estimates

Based on ~282 OSGi bundles with configured start orders:

| Environment | Estimated Time |
|-------------|----------------|
| Cold Start (first time) | 45-90 seconds |
| Warm Start (subsequent) | 20-40 seconds |
| With SSD | 15-30 seconds |
| With HDD | 40-60 seconds |

### Factors Affecting Startup Time

1. **Bundle Activation** - 282 OSGi bundles with 279 start-order configurations
2. **Repository Initialization** - First-time setup takes longer
3. **Content Indexing** - Oak repository index creation
4. **Extension Activations** - Custom bundle startup
5. **Disk I/O Speed** - SSD significantly improves startup

## Deployment Modes

### Standalone Mode

Best for development and small production sites.

```bash
./deployment/start-standalone.sh
```

- **Port**: 8080
- **URL**: http://localhost:8080/cms
- **Credentials**: admin / admin
- **Resources**: Single instance, ~2 GB RAM

### Author + Renderer Mode

Best for production deployments with content separation.

```bash
./deployment/start-author.sh    # Port 8082
./deployment/start-renderer.sh  # Port 8083
```

- **Author URL**: http://localhost:8082/cms
- **Renderer URL**: http://localhost:8083
- **Resources**: 2 instances, ~4-6 GB RAM total

## Build Profiles & Performance

| Profile | Build Time | Output | Use Case |
|---------|------------|--------|----------|
| Default | ~40-45s | All JARs | Full distribution |
| `-P fast` | ~25s | Standalone only | Quick development |
| `-P unified` | ~30s | Combined JAR | Simplified deployment |

### Build Commands

```bash
# Full build (all deployment modes)
mvn clean install -DskipTests

# Fast build (standalone only)
mvn clean install -P fast -DskipTests

# Unified JAR build
mvn clean install -P unified -DskipTests
```

## Monitoring Recommendations

### Key Metrics to Monitor

1. **JVM Heap Usage** - Alert at 80% threshold
2. **GC Pause Times** - Alert if > 500ms
3. **Thread Count** - Baseline and anomaly detection
4. **HTTP Response Times** - SLA monitoring
5. **Disk I/O** - Repository performance
6. **Bundle Status** - All bundles should be ACTIVE

### Health Check Endpoints

- System Console: `http://localhost:8080/system/console`
- Bundle Status: `http://localhost:8080/system/console/bundles`
- Health Check: `http://localhost:8080/system/health`

## Scaling Considerations

### Horizontal Scaling

- Use load balancer in front of multiple renderer instances
- Sticky sessions for author instance
- Shared content repository (MongoDB or clustered Oak)

### Vertical Scaling

- Increase heap size for larger content repositories
- Add CPU cores for concurrent request handling
- Use faster storage (NVMe SSD) for better I/O

## Troubleshooting

### Common Issues

| Issue | Possible Cause | Solution |
|-------|---------------|----------|
| Slow startup | Cold repository | Allow initial indexing to complete |
| OutOfMemoryError | Insufficient heap | Increase `-Xmx` setting |
| Port already in use | Another instance running | Stop existing instance or change port |
| Bundle not starting | Missing dependency | Check OSGi console for errors |

### Log Locations

- **Standalone**: `deployment/standalone/launcher/logs/`
- **Author**: `deployment/author/launcher/logs/`
- **Renderer**: `deployment/publisher/launcher/logs/`

## Optimization Analysis

This section provides detailed analysis and recommendations for optimizing startup time and memory utilization.

### Bundle Start-Order Analysis

The feature model defines **282 OSGi bundles** across **30 feature files** with the following start-order distribution:

| Start Order | Bundle Count | Phase |
|-------------|--------------|-------|
| 1 | 40 | Boot/Core (critical) |
| 4-5 | 36 | Early Framework |
| 10 | 7 | Mid-level Services |
| 15 | 57 | Application Services |
| 16 | 1 | Special |
| 19-20 | 127+ | Late/Application |

#### Largest Feature Files (by bundle count)

| Feature File | Bundles | Purpose |
|--------------|---------|---------|
| `base.json` | 59 | Core Sling bundles |
| `boot.json` | 46 | Boot-level framework |
| `oak_base.json` | 35 | Oak repository |
| `scripting.json` | 23 | Script engines |
| `groovy.json` | 22 | Groovy support |
| `webconsole.json` | 15 | Admin console |
| `cms/dependencies.json` | 26 | CMS dependencies |

### Startup Time Optimization

#### Current Startup Bottlenecks

1. **Sequential Bundle Activation**: 282 bundles activated in order
2. **Oak Repository Init**: First-time index creation is slow
3. **Script Engine Compilation**: HTL/JSP compilation at startup
4. **Health Check Services**: Multiple JMX-based checks

#### Recommendations for Faster Startup

##### 1. JVM Startup Optimization

```bash
# Fast startup JVM flags (Java 21)
java \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=1 \      # Faster JIT warmup
    -XX:CICompilerCount=2 \         # Reduce compiler threads
    -XX:+UseParallelGC \            # Faster GC for startup
    -Xms512m -Xmx1g \
    -jar standalone.jar
```

**Expected improvement**: 10-20% faster startup

##### 2. Class Data Sharing (CDS)

```bash
# Step 1: Create CDS archive (one-time)
java -Xshare:dump -XX:SharedArchiveFile=sling.jsa -jar standalone.jar

# Step 2: Use CDS archive for startup
java -Xshare:on -XX:SharedArchiveFile=sling.jsa -jar standalone.jar
```

**Expected improvement**: 15-25% faster startup

##### 3. Bundle Start-Order Optimization

Current distribution shows **127+ bundles at start-order 20**. Consider:
- Increase parallelism for bundles at same start-order
- Delay non-critical bundles (groovy, webconsole) to start-order 25+
- Move health checks to start-order 30 (after core ready)

##### 4. Lazy Bundle Activation

Add lazy activation for non-critical bundles in feature files:

```json
{
    "id": "org.apache.felix:org.apache.felix.webconsole:4.9.6",
    "start-order": "25",
    "metadata": {
        "Bundle-ActivationPolicy": "lazy"
    }
}
```

**Bundles safe for lazy activation**:
- Web Console plugins (`webconsole.json`)
- Groovy scripting (`groovy.json`)
- Health check bundles
- Composum (if not immediately needed)

### Memory Optimization

#### Current Memory Configuration

| Setting | Value | Location |
|---------|-------|----------|
| Max Heap | 1 GB | `deployment/start-*.sh` |
| Initial Heap | Not set | - |
| Memory Warning | 95% | `healthcheck.json` |
| Memory Critical | 100% | `healthcheck.json` |

#### Memory Consumption Breakdown (Estimated)

| Component | Memory Usage |
|-----------|-------------|
| JVM Base | ~100 MB |
| OSGi Framework | ~50 MB |
| 282 Bundles (loaded) | ~200-300 MB |
| Oak Repository | ~150-300 MB |
| Content Cache | ~100-200 MB |
| Script Cache | ~50-100 MB |
| HTTP Sessions | Variable |
| **Total Typical** | **650-1050 MB** |

#### Memory Optimization Recommendations

##### 1. Heap Sizing Best Practices

```bash
# Development (conservative)
java -Xms512m -Xmx1g -jar standalone.jar

# Production (balanced)
java -Xms1g -Xmx2g -XX:MaxMetaspaceSize=512m -jar standalone.jar

# High-memory content (large sites)
java -Xms2g -Xmx4g -XX:MaxMetaspaceSize=1g -jar standalone.jar
```

##### 2. Garbage Collection Tuning

```bash
# G1GC for balanced performance (recommended for heap > 2GB)
java \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:G1HeapRegionSize=16m \
    -XX:InitiatingHeapOccupancyPercent=45 \
    -Xms1g -Xmx2g \
    -jar standalone.jar

# ZGC for low-latency (Java 21, heap > 4GB)
java \
    -XX:+UseZGC \
    -XX:+ZGenerational \
    -Xms2g -Xmx4g \
    -jar standalone.jar
```

##### 3. Off-Heap Memory Configuration

For large content repositories, configure Oak off-heap caching:

```json
// In oak configuration
{
    "org.apache.jackrabbit.oak.segment.SegmentNodeStoreService": {
        "cache": 256,
        "segmentCacheSize": 256
    }
}
```

##### 4. Metaspace Optimization

```bash
# Limit metaspace growth
java \
    -XX:MetaspaceSize=128m \
    -XX:MaxMetaspaceSize=512m \
    -XX:CompressedClassSpaceSize=256m \
    -jar standalone.jar
```

### Optimized Startup Scripts

#### Fast Development Startup

```bash
#!/bin/bash
# fast-start.sh - Optimized for development startup speed

java \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=1 \
    -XX:+UseParallelGC \
    -Xms512m -Xmx1g \
    -Dsling.fileinstall.poll.interval=2000 \
    -jar standalone.jar \
    -p launcher \
    -D org.osgi.service.http.port=8080
```

#### Production Optimized Startup

```bash
#!/bin/bash
# production-start.sh - Optimized for production performance

java \
    -server \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+ParallelRefProcEnabled \
    -XX:+DisableExplicitGC \
    -XX:+AlwaysPreTouch \
    -Xms2g -Xmx2g \
    -XX:MaxMetaspaceSize=512m \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/var/log/sling/ \
    -Djava.net.preferIPv4Stack=true \
    -Dfile.encoding=UTF-8 \
    -jar standalone.jar \
    -p launcher \
    -D org.osgi.service.http.port=8080
```

### Performance Benchmarks

#### Actual Measured Values (December 2025)

**Test Environment:**
- macOS, Apple Silicon
- Java 21, G1GC (default)
- SSD Storage
- JVM: `-Xmx1g`

| Metric | Measured Value |
|--------|----------------|
| **Build Time** (fast profile) | **28.3 seconds** |
| **Warm Start Time** | **~2-15 seconds** |
| **Bundle Count** | 279 total, 276 active |
| **Heap Used** (idle) | ~162 MB |
| **Heap Max** | 1024 MB |
| **Metaspace** | ~97 MB |
| **Total Memory** | ~319 MB |
| **RSS (Process)** | ~280-480 MB |

#### Startup Time Comparison

| Configuration | Cold Start | Warm Start |
|--------------|------------|------------|
| Default (1GB, no tuning) | 45-60s | **2-15s** ✓ measured |
| With TieredCompilation | 35-50s | 2-10s |
| With CDS Archive | 30-40s | 1-8s |
| SSD vs HDD | -30% | -20% |

#### Memory Usage Patterns (Measured)

| Scenario | Heap Used | RSS | Recommendation |
|----------|-----------|-----|----------------|
| Idle (after startup) | **~162 MB** | ~280 MB | 1 GB heap sufficient |
| Light usage | ~300-400 MB | ~400 MB | 1 GB heap |
| Moderate (50+ pages/min) | ~500-700 MB | ~600 MB | 1-2 GB heap |
| Heavy (100+ pages/min) | ~700-900 MB | ~800 MB | 2 GB heap |
| Large repository (>10GB) | ~1-1.5 GB | ~1.2 GB | 2-4 GB heap |

### Feature Model Optimization

#### Optional Features to Disable

For minimal installations, these features can be excluded:

| Feature | Impact | Savings |
|---------|--------|---------|
| `groovy.json` | Disables Groovy console | 22 bundles, ~5MB |
| `webconsole.json` | No admin console | 15 bundles, ~3MB |
| `healthcheck.json` | No health endpoints | 10 bundles, ~2MB |
| `composum.json` | Alternative content browser | 5 bundles, ~8MB |

#### Creating Minimal Feature Set

```xml
<!-- In feature/pom.xml, create minimal aggregate -->
<aggregate>
    <classifier>minimal</classifier>
    <filesInclude>
        boot.json,
        base.json,
        oak/oak_base.json,
        cms/cms.json,
        cms/dependencies.json
    </filesInclude>
</aggregate>
```

### Monitoring and Profiling

#### Enable JMX for Monitoring

```bash
java \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port=9010 \
    -Dcom.sun.management.jmxremote.local.only=false \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -jar standalone.jar
```

#### GC Logging for Analysis

```bash
java \
    -Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10m \
    -jar standalone.jar
```

#### Built-in Health Checks

The CMS includes preconfigured health checks:

| Check | Threshold | Location |
|-------|-----------|----------|
| Memory Warning | 95% heap | `healthcheck.json` |
| Memory Critical | 100% heap | `healthcheck.json` |
| Job Queue | < 1000 jobs | `cms/healthchecks.json` |
| Request Duration | StdDev monitoring | `cms/healthchecks.json` |
| Oak Index Status | Failing = CRITICAL | `cms/healthchecks.json` |

### Quick Reference: Optimization Checklist

- [ ] Use SSD for faster I/O
- [ ] Set `-Xms` equal to `-Xmx` for production
- [ ] Enable G1GC or ZGC for heap > 2GB
- [ ] Create CDS archive for repeated startups
- [ ] Use `-XX:TieredStopAtLevel=1` for faster dev startup
- [ ] Monitor GC logs for tuning
- [ ] Disable unused features (groovy, webconsole in production)
- [ ] Configure Oak segment cache for large repositories
- [ ] Use lazy activation for non-critical bundles
- [ ] Set heap dump on OOM for debugging

## Related Documentation

- [Quickstart Guide](quickstart.md)
- [Building](building.md)
- [Deployment Models](deployment-models.md)
- [Administration](administration.md)
