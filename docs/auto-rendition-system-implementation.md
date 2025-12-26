# SPDX-License-Identifier: Apache-2.0

# Auto-Rendition System Implementation Guide

## Overview

The auto-rendition system automatically generates image transformations (renditions) when digital assets are uploaded to Apache Sling CMS. This system provides real-time thumbnail generation and image processing without manual intervention.

**Key Features:**
- Automatic rendition generation on file upload
- Support for images, PDFs, and videos
- Configurable transformation pipelines
- Metadata-aware processing
- Thread-safe concurrent job execution

## System Architecture

### Core Components

#### 1. AutoRenditionListener
**Location:** `thumbnails/src/main/java/org/apache/sling/thumbnails/internal/AutoRenditionListener.java`

**Purpose:** Resource change listener that detects file uploads and queues rendition jobs.

**Implementation:**
- Implements `ResourceChangeListener` to detect new sling:File resources
- Implements `EventHandler` to coordinate with metadata extraction
- Filters resources by:
  - Content paths (`/content`, `/static`)
  - MIME types (image/*, application/pdf, video/*)
  - Resource type (sling:File)

**Event Flow:**
```
File Upload → ResourceChange.ADDED → isSupported() → matchesMimeType()
           → Check metadata exists
           → Queue rendition job OR wait for metadata extraction
```

#### 2. AutoRenditionJobConsumer
**Location:** `thumbnails/src/main/java/org/apache/sling/thumbnails/internal/AutoRenditionJobConsumer.java`

**Purpose:** Background job processor that generates renditions.

**Job Topic:** `org/apache/sling/thumbnails/AutoRendition`

**Implementation:**
- Processes jobs asynchronously using Sling Job Manager
- Retrieves transformation from TransformationCache
- Generates rendition and persists to JCR at `jcr:content/renditions/<name>`

#### 3. TransformationCache
**Location:** `thumbnails/src/main/java/org/apache/sling/thumbnails/internal/TransformationCache.java`

**Purpose:** Thread-safe cache for transformation lookup and retrieval.

**Recent Fixes (December 2025):**
- ✅ Changed from HashMap to ConcurrentHashMap for thread safety
- ✅ Replaced SQL2 query with direct path lookups for performance
- ✅ Uses service resolver for /conf path access
- ✅ Caches transformation paths, not resolved resources

**Lookup Strategy:**
```java
String[] searchPaths = {
    "/conf/global/dam/transformations/" + transformationName,
    "/libs/conf/global/dam/transformations/" + transformationName,
    "/apps/conf/global/dam/transformations/" + transformationName
};
```

#### 4. AutoRenditionConfig
**Location:** `thumbnails/src/main/java/org/apache/sling/thumbnails/AutoRenditionConfig.java`

**Purpose:** OSGi configuration interface for auto-rendition behavior.

**Configuration Properties:**
- `enabled` - Enable/disable auto-rendition generation
- `transformationNames` - Transformations to apply automatically
- `supportedMimeTypes` - MIME type patterns (supports wildcards)
- `contentPaths` - Content paths to monitor for uploads

## Configuration

### Default Configuration

**Feature Model:** `feature/src/main/features/cms/thumbnails.json`

```json
{
  "org.apache.sling.thumbnails.internal.AutoRenditionConfigImpl": {
    "enabled:Boolean": true,
    "transformationNames": [
      "sling-cms-thumbnail",
      "auto-rotate-thumbnail"
    ],
    "supportedMimeTypes": [
      "image/*",
      "application/pdf",
      "video/*"
    ],
    "contentPaths": [
      "/content",
      "/static"
    ]
  }
}
```

### OSGi Configuration

Configuration can also be provided via OSGi config files:

**Location:** `ui/src/main/resources/jcr_root/apps/sling-cms/osgiconfig/config/`

**File:** `org.apache.sling.thumbnails.internal.AutoRenditionConfigImpl.cfg.json`

```json
{
  "enabled": true,
  "transformationNames": [
    "sling-cms-thumbnail",
    "auto-rotate-thumbnail"
  ],
  "supportedMimeTypes": [
    "image/*",
    "application/pdf",
    "video/*"
  ],
  "contentPaths": [
    "/content",
    "/static"
  ]
}
```

### Transformations

**Standard Transformations:**

| Name | Description | Output |
|------|-------------|--------|
| `sling-cms-thumbnail` | Standard thumbnail | 600x480 PNG |
| `auto-rotate-thumbnail` | Auto-rotated thumbnail using EXIF orientation | 600x480 PNG |

**Transformation Definitions:** `ui/src/main/resources/jcr_root/conf/global/dam/transformations.json`

## Metadata Coordination

### Event-Based Synchronization

The auto-rendition system coordinates with metadata extraction to prevent race conditions:

1. **File Upload** → ResourceChangeListener detects new file
2. **Check Metadata** → If metadata exists, queue rendition jobs immediately
3. **Wait for Metadata** → If metadata doesn't exist, wait for extraction event
4. **Metadata Extracted** → EventHandler receives `org/apache/sling/cms/metadata/EXTRACTED` event
5. **Queue Jobs** → Rendition jobs queued after metadata is available

**Event Topic:** `org/apache/sling/cms/metadata/EXTRACTED`

**Coordination Logic:**
```java
private void processResource(Resource resource, ResourceResolver resolver) {
    Resource metadataResource = resource.getChild("jcr:content/metadata");

    if (metadataResource != null) {
        // Metadata exists, queue immediately
        queueRenditionJob(resource);
    } else {
        // Wait for metadata extraction event
        // Jobs will be queued via handleEvent()
    }
}
```

## Technical Implementation Details

### Thread Safety

**Issue:** ConcurrentModificationException in TransformationCache

**Root Cause:** HashMap was accessed concurrently by multiple AutoRendition job threads.

**Fix:** Changed to ConcurrentHashMap
```java
// Before (Thread-unsafe)
private final Map<String, Optional<String>> cache = new HashMap<>();

// After (Thread-safe)
private final Map<String, Optional<String>> cache = new ConcurrentHashMap<>();
```

### Performance Optimization

**Issue:** SQL2 query for transformation lookup was slow and required JCR indexing.

**Original Query:**
```java
Iterator<Resource> transformations = serviceResolver.findResources(
    "SELECT * FROM [nt:unstructured] WHERE (ISDESCENDANTNODE([/conf]) OR ...) AND [name]='" + transformationName + "'",
    Query.JCR_SQL2);
```

**Fix:** Direct path lookups
```java
String[] searchPaths = {
    "/conf/global/dam/transformations/" + transformationName,
    "/libs/conf/global/dam/transformations/" + transformationName,
    "/apps/conf/global/dam/transformations/" + transformationName
};

for (String path : searchPaths) {
    Resource transformation = serviceResolver.getResource(path);
    if (transformation != null && "sling/thumbnails/transformation".equals(transformation.getResourceType())) {
        return Optional.of(path);
    }
}
```

### Resource Access Permissions

**Issue:** Request resolver couldn't access /conf paths for transformations.

**Root Cause:** User-based resolvers often lack read permissions to /conf configuration nodes.

**Fix:** Use service resolver for transformation lookup
```java
// Get transformation path from cache
Optional<String> cachedPath = cache.computeIfAbsent(name, this::findTransformation);

// Use service resolver to retrieve and adapt resource
try (ResourceResolver serviceResolver = transformationServiceUser.getTransformationServiceUser()) {
    Resource transformationResource = serviceResolver.getResource(cachedPath.get());
    if (transformationResource != null) {
        return Optional.ofNullable(transformationResource.adaptTo(Transformation.class));
    }
}
```

## API Changes (Version 2.0.0)

### Breaking Changes

**Package:** `org.apache.sling.thumbnails.extension`

**TransformationHandler Interface:**
```java
// REMOVED (1.x)
void handle(InputStream inputStream, OutputStream outputStream,
            TransformationHandlerConfig config);

// ADDED (2.0)
void handle(InputStream inputStream, OutputStream outputStream,
            TransformationHandlerConfig config, Map<String, Object> metadata);
```

**Reason:** Added metadata parameter to enable metadata-aware transformations (e.g., EXIF orientation).

**Version Bump:** Both packages bumped from 1.1.0 to 2.0.0 due to MAJOR API changes.

**Files Modified:**
- `thumbnails/src/main/java/org/apache/sling/thumbnails/package-info.java`
- `thumbnails/src/main/java/org/apache/sling/thumbnails/extension/package-info.java`

## Usage

### Automatic Renditions on Upload

When a file is uploaded to monitored paths (`/content`, `/static`):

1. **Supported File Types:**
   - Images: image/* (JPEG, PNG, GIF, etc.)
   - PDFs: application/pdf
   - Videos: video/* (MP4, WebM, etc.)

2. **Rendition Storage:**
   ```
   /content/dam/my-image.png
   └── jcr:content
       └── renditions
           ├── sling-cms-thumbnail.png
           └── auto-rotate-thumbnail.png
   ```

3. **Accessing Renditions:**
   - Direct path: `/content/dam/my-image.png/jcr:content/renditions/sling-cms-thumbnail.png`
   - Transform API: `/content/dam/my-image.png.transform/sling-cms-thumbnail.png`

### Manual Transformation

Files can also be transformed on-demand via Transform API:

**Syntax:** `<asset-path>.transform/<transformation-name>.<output-format>`

**Examples:**
- `/content/dam/logo.png.transform/sling-cms-thumbnail.png`
- `/static/test/image.jpg.transform/auto-rotate-thumbnail.png`

## Troubleshooting

### No Renditions Generated

**Check 1: Auto-Rendition Enabled**
```
OSGi Console → Configuration → Auto Rendition Configuration
→ enabled = true
```

**Check 2: Correct Paths**
```
File uploaded to /content or /static?
contentPaths configuration includes upload path?
```

**Check 3: MIME Type Supported**
```
File MIME type matches supportedMimeTypes pattern?
image/* matches image/png, image/jpeg, etc.
```

**Check 4: Transformations Exist**
```
curl http://localhost:8082/conf/global/dam/transformations/sling-cms-thumbnail.json
Should return transformation definition
```

**Check 5: Jobs Running**
```
OSGi Console → Sling → Jobs
Check for org/apache/sling/thumbnails/AutoRendition jobs
```

### ConcurrentModificationException

**Symptom:** Exception in TransformationCache.getTransformation()

**Fix:** Update to latest version with ConcurrentHashMap implementation.

### Transformation Not Found

**Symptom:** "Unable to find transformation: /transformation-name"

**Possible Causes:**
1. Transformation not loaded in JCR
2. TransformationCache lookup failing
3. Service resolver access issue

**Fix:**
1. Check `/conf/global/dam/transformations/` in JCR browser
2. Review TransformationCache logs for detailed error messages
3. Ensure sling-thumbnails service user has read access to /conf

## Development

### Adding Custom Transformations

1. **Define Transformation** in `ui/src/main/resources/jcr_root/conf/global/dam/transformations.json`

2. **Add to Auto-Rendition Config:**
```json
{
  "transformationNames": [
    "sling-cms-thumbnail",
    "auto-rotate-thumbnail",
    "my-custom-transformation"
  ]
}
```

3. **Deploy and Test:**
```bash
mvn clean install -P autoInstallBundle -pl ui,thumbnails -DskipTests
```

### Creating Transformation Handlers

Implement `TransformationHandler` interface:

```java
@Component(
    service = TransformationHandler.class,
    property = {
        TransformationHandler.PROPERTY_TRANSFORMATION_TYPE + "=my-handler"
    }
)
public class MyTransformationHandler implements TransformationHandler {

    @Override
    public void handle(InputStream inputStream, OutputStream outputStream,
                      TransformationHandlerConfig config, Map<String, Object> metadata) {
        // Implementation
    }
}
```

## Performance Considerations

### Cache Behavior

- **Cache Type:** ConcurrentHashMap (thread-safe)
- **Cache Key:** Transformation name (e.g., "sling-cms-thumbnail")
- **Cache Value:** Optional<String> (transformation path)
- **Cache Invalidation:**
  - On transformation resource CHANGED events
  - Every 3600 seconds (1 hour) via scheduler

### Job Execution

- **Job Queue:** Sling Job Manager (persistent, fault-tolerant)
- **Concurrency:** Multiple jobs can execute in parallel
- **Retries:** Automatic retry on transient failures
- **Job Properties:**
  - `path` - Asset resource path
  - `transformation` - Transformation name

## Service User Configuration

**Service User:** `sling-thumbnails`

**Required Permissions:**
- Read access to /conf/global/dam/transformations
- Read/write access to content paths (/content, /static)
- Read access to /libs/conf (for fallback transformations)

**Mapping Configuration:** `feature/src/main/features/cms/thumbnails.json`
```json
{
  "org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended~sling-thumbnails": {
    "user.mapping": [
      "org.apache.sling.thumbnails:sling-thumbnails=[sling-thumbnails]",
      "org.apache.sling.thumbnails:sling-cms-thumbnails=[sling-thumbnails]"
    ]
  }
}
```

## References

- [TransformationCache.java](../thumbnails/src/main/java/org/apache/sling/thumbnails/internal/TransformationCache.java)
- [AutoRenditionListener.java](../thumbnails/src/main/java/org/apache/sling/thumbnails/internal/AutoRenditionListener.java)
- [AutoRenditionJobConsumer.java](../thumbnails/src/main/java/org/apache/sling/thumbnails/internal/AutoRenditionJobConsumer.java)
- [Feature Model Configuration](../feature/src/main/features/cms/thumbnails.json)
- [OSGi Configuration](../ui/src/main/resources/jcr_root/apps/sling-cms/osgiconfig/config/org.apache.sling.thumbnails.internal.AutoRenditionConfigImpl.cfg.json)

## Change Log

### Version 2.0.0 (December 2025)

**Breaking Changes:**
- Added metadata parameter to TransformationHandler.handle() method
- Package versions bumped to 2.0.0

**Bug Fixes:**
- Fixed ConcurrentModificationException in TransformationCache
- Replaced SQL2 query with direct path lookups for better performance
- Fixed service resolver access for /conf paths
- Improved metadata coordination with EventHandler

**Configuration:**
- Added AutoRenditionConfigImpl to feature model
- Default transformations: sling-cms-thumbnail, auto-rotate-thumbnail
- Default paths: /content, /static
- Default MIME types: image/*, application/pdf, video/*
