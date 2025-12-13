<!-- Licensed to the Apache Software Foundation (ASF) under one or more contributor
	license agreements. See the NOTICE file distributed with this work for additional
	information regarding copyright ownership. The ASF licenses this file to
	you under the Apache License, Version 2.0 (the "License"); you may not use
	this file except in compliance with the License. You may obtain a copy of
	the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required
	by applicable law or agreed to in writing, software distributed under the
	License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
	OF ANY KIND, either express or implied. See the License for the specific
	language governing permissions and limitations under the License. -->
[Apache Sling](https://sling.apache.org) > [Sling CMS](https://github.com/apache/sling-org-apache-sling-app-cms) > [Administration](administration.md) > Digital Asset Management

# Digital Asset Management (DAM) Enhancement Plan

This document provides a comprehensive analysis of the existing asset management capabilities in Sling CMS and outlines a plan for extending the **Thumbnails Module** to provide full Digital Asset Management functionality.

## Architecture Principles

This enhancement follows these core architectural principles:

| Principle | Description |
|-----------|-------------|
| **Platform Independence** | No native dependencies; pure Java implementations with optional native acceleration |
| **OSGi Service Pattern** | All components are OSGi services with proper lifecycle management |
| **Strategy Pattern** | Pluggable providers/handlers via SPI (Service Provider Interface) |
| **Graceful Degradation** | Features work with reduced capability when optional dependencies unavailable |
| **Configuration-Driven** | Behavior controlled via OSGi configuration, not code changes |
| **Backward Compatibility** | Existing APIs and configurations continue to work |

## Quick Reference: Key Dependencies

| Library | Current Version | Module | Purpose | Platform |
|---------|-----------------|--------|---------|----------|
| `thumbnailator` | 0.4.20 | thumbnails | Image transformation | Pure Java ✅ |
| `pdfbox` | 2.0.32 / 3.0.6* | thumbnails | PDF thumbnails | Pure Java ✅ |
| `poi-ooxml` | 5.5.1 | thumbnails | Office document thumbnails | Pure Java ✅ |
| `tika-core` | 1.28.5 | thumbnails | MIME detection, text extraction | Pure Java ✅ |
| `commons-io` | 2.21.0 | all | File I/O utilities | Pure Java ✅ |
| `jackson-databind` | 2.18.2 | all | JSON processing | Pure Java ✅ |
| **FFmpeg** | (system) | thumbnails | Video frame extraction | System Tool ✅ |

*pdfbox: thumbnails uses 2.0.32, parent pom has 3.0.6

### Platform Independence Strategy

For video thumbnail support, we use **FFmpeg via ProcessBuilder**:

- **OSGi Compliant** - Uses system-installed FFmpeg via `ProcessBuilder` (no embedded native libraries)
- **Graceful Degradation** - Returns null if FFmpeg is not available on the system
- **Broad Format Support** - Supports all video formats that FFmpeg supports

This approach follows the OSGi-compliant library guidelines - using system tools via ProcessBuilder rather than embedding non-OSGi libraries.

## Table of Contents

1. [Current State Analysis](#current-state-analysis)
2. [Thumbnails Module Deep Dive](#thumbnails-module-deep-dive)
3. [Static Content Console Analysis](#static-content-console-analysis)
4. [Gap Analysis](#gap-analysis)
5. [Extension Plan](#extension-plan)
6. [Implementation Roadmap](#implementation-roadmap)

---

## Current State Analysis

### Existing Infrastructure

The Sling CMS already has a solid foundation for digital asset management through two key components:

| Component | Location | Purpose |
|-----------|----------|---------|
| **Static Content Console** | `/cms/static/content.html/static` | UI for browsing/managing files |
| **Thumbnails Module** | `thumbnails/` | Image transformation & thumbnail generation |
| **File API** | `api/` + `core/` | File model and metadata extraction |

### Static Content Console Features

The Static Content Console (`/cms/static/content.html/static`) currently supports:

- ✅ File upload (`sling:File` type)
- ✅ Folder creation and management
- ✅ File download
- ✅ Image preview modal
- ✅ File optimization
- ✅ Move/Copy operations
- ✅ Version management
- ✅ Reference tracking
- ✅ Delete operations
- ✅ Publish status tracking

### Supported Resource Types

| Type | Upload | Preview | Thumbnail | Metadata |
|------|--------|---------|-----------|----------|
| `sling:File` | ✅ | ✅ | ✅ | ✅ |
| `nt:file` | ✅ | ❌ | ✅ | ⚠️ Limited |
| `sling:Folder` | ✅ | N/A | N/A | N/A |
| `sling:OrderedFolder` | ✅ | N/A | N/A | N/A |

---

## Thumbnails Module Deep Dive

The `thumbnails` module is the cornerstone for DAM functionality. Here's a detailed analysis:

### Architecture Overview

```
thumbnails/
├── src/main/java/org/apache/sling/thumbnails/
│   ├── ThumbnailSupport.java          # Configuration for supported types
│   ├── Transformer.java               # Transformation execution interface
│   ├── RenditionSupport.java          # Rendition storage/retrieval
│   ├── Transformation.java            # Transformation definition model
│   ├── OutputFileFormat.java          # Output format enum (PNG, JPG, GIF)
│   ├── extension/
│   │   ├── ThumbnailProvider.java     # SPI for thumbnail generation
│   │   └── TransformationHandler.java # SPI for transformation steps
│   └── internal/
│       ├── TransformServlet.java      # URL-based transformation servlet
│       ├── DynamicTransformServlet.java
│       ├── TransformerImpl.java       # Core transformation logic
│       ├── TransformationCache.java   # Caching layer
│       ├── providers/                 # Thumbnail providers
│       │   ├── ImageThumbnailProvider.java
│       │   ├── PdfThumbnailProvider.java
│       │   ├── SlideShowThumbnailProvider.java
│       │   └── TikaFallbackProvider.java
│       └── transformers/              # Transformation handlers
│           ├── ResizeHandler.java
│           ├── CropHandler.java
│           ├── RotateHandler.java
│           ├── FlipHandler.java
│           ├── ScaleHandler.java
│           ├── ColorizeHandler.java
│           ├── GreyscaleHandler.java
│           └── TransparencyHandler.java
```

### Key Interfaces

#### 1. ThumbnailSupport

```java
public interface ThumbnailSupport {
    // Get supported resource types (e.g., "nt:file", "sling:File")
    Set<String> getSupportedTypes();
    
    // Get types that can persist renditions
    Set<String> getPersistableTypes();
    
    // Get rendition storage path for a type
    String getRenditionPath(String resourceType);
    
    // Get meta type property path (for MIME type lookup)
    String getMetaTypePropertyPath(String resourceType);
}
```

**Current Configuration:**
```
supportedTypes = ["nt:file=jcr:content/jcr:mimeType", "sling:File=jcr:content/jcr:mimeType"]
persistableTypes = ["sling:File=jcr:content/renditions"]
```

#### 2. ThumbnailProvider SPI

```java
public interface ThumbnailProvider {
    // Check if this provider handles the resource
    boolean applies(Resource resource, String metaType);
    
    // Generate thumbnail InputStream
    InputStream getThumbnail(Resource resource) throws IOException;
}
```

**Current Implementations:**

| Provider | MIME Types | Library Used |
|----------|------------|--------------|
| `ImageThumbnailProvider` | `image/*` | Direct stream |
| `PdfThumbnailProvider` | `application/pdf` | Apache PDFBox |
| `SlideShowThumbnailProvider` | `application/vnd.ms-powerpoint`, `application/vnd.openxmlformats-officedocument.presentationml.presentation` | Apache POI |
| `TikaFallbackProvider` | `*/*` (lowest priority) | Apache Tika |

#### 3. TransformationHandler SPI

```java
public interface TransformationHandler {
    // Resource type for configuration
    String getResourceType();
    
    // Execute transformation step
    void handle(InputStream in, OutputStream out, TransformationHandlerConfig config) 
        throws IOException;
}
```

**Current Handlers:**

| Handler | Resource Type | Parameters |
|---------|--------------|------------|
| `ResizeHandler` | `sling/thumbnails/transformers/resize` | `width`, `height`, `keepAspectRatio` |
| `CropHandler` | `sling/thumbnails/transformers/crop` | `width`, `height`, `position` |
| `RotateHandler` | `sling/thumbnails/transformers/rotate` | `degrees` |
| `FlipHandler` | `sling/thumbnails/transformers/flip` | `horizontal`, `vertical` |
| `ScaleHandler` | `sling/thumbnails/transformers/scale` | `scale` |
| `ColorizeHandler` | `sling/thumbnails/transformers/colorize` | `red`, `green`, `blue`, `alpha` |
| `GreyscaleHandler` | `sling/thumbnails/transformers/greyscale` | none |
| `TransparencyHandler` | `sling/thumbnails/transformers/transparency` | `alpha` |

#### 4. RenditionSupport

```java
public interface RenditionSupport {
    // Check if rendition exists
    boolean renditionExists(Resource file, String renditionName);
    
    // Get rendition content
    InputStream getRenditionContent(Resource file, String renditionName);
    
    // List all renditions
    List<Resource> listRenditions(Resource file);
    
    // Set/update rendition
    void setRendition(Resource file, String renditionName, InputStream content);
    
    // Check if file supports renditions
    boolean supportsRenditions(Resource file);
}
```

### Transformation URL Pattern

```
/content/path/to/file.jpg.transform/transformation-name.png
                         ^^^^^^^^^ ^^^^^^^^^^^^^^^^^^^^^ ^^^
                         extension  transformation name   output format
```

Example: `/static/images/photo.jpg.transform/thumbnail.png`

### Transformation Configuration

Transformations are defined under `/conf/{site}/files/transformations/`:

```json
{
    "jcr:primaryType": "nt:unstructured",
    "sling:resourceType": "sling/thumbnails/transformation",
    "name": "thumbnail",
    "handlers": {
        "resize": {
            "jcr:primaryType": "nt:unstructured",
            "sling:resourceType": "sling/thumbnails/transformers/resize",
            "width": 200,
            "height": 200,
            "keepAspectRatio": true
        }
    }
}
```

### Dependencies

#### Current Thumbnails Module Dependencies

| Library | Version | Purpose | Managed In |
|---------|---------|---------|------------|
| `thumbnailator` | 0.4.20 | Core image transformation | thumbnails/pom.xml |
| `pdfbox` | 2.0.32 | PDF thumbnail generation | thumbnails/pom.xml |
| `poi-ooxml` | 5.5.1 | PowerPoint/Excel thumbnail generation | parent pom (${poi-version}) |
| `poi-scratchpad` | 5.5.1 | Legacy PPT support | parent pom (${poi-version}) |
| `tika-core` | 1.28.5 | MIME type detection, fallback text extraction | parent pom (${tika-version}) |
| `commons-compress` | 1.27.1 | Archive handling | thumbnails/pom.xml |
| `commons-io` | 2.21.0 | File/stream utilities | parent pom (${commons-io-version}) |
| `jackson-databind` | 2.18.2 | JSON processing | parent pom (${jackson.version}) |

#### Shared Dependencies with Other Modules

| Library | Version | Used By | Notes |
|---------|---------|---------|-------|
| `sling-api` | 2.27.2 | all modules | Core Sling API |
| `sling-models-api` | 2.0.0 | core, thumbnails | Sling Models |
| `sling-caconfig-api` | 1.3.0 | core, thumbnails | Context-aware configuration |
| `commons-lang3` | 3.20.0 | all modules | String/Object utilities |
| `slf4j-api` | 2.0.17 | all modules | Logging |
| `guava` | 33.3.1-jre | core, ui | Google utilities |

#### Test Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| `sling-mock-junit5` | 3.5.2 | Sling testing |
| `sling-mock-oak` | 4.1.0-1.86.0 | Oak repository mock |
| `mockito-core` | 5.21.0 | Mocking framework |
| `junit-jupiter` | 6.1.0-M1 | Testing framework |

### Recommended Additional Dependencies for DAM

Video thumbnail support uses **FFmpeg via ProcessBuilder** for OSGi compliance:

#### FFmpeg-Based Approach (OSGi Compliant)

```xml
<!-- No additional Maven dependencies required -->
<!-- FFmpeg must be installed on the system -->
```

**Why FFmpeg via ProcessBuilder?**
- ✅ OSGi compliant - no embedded non-OSGi JARs
- ✅ Broad format support (MP4, WebM, MOV, AVI, MKV, etc.)
- ✅ High-quality frame extraction
- ✅ Widely available on all platforms
- ✅ Easy to install via package managers

---

## Static Content Console Analysis

### UI Configuration Structure

Location: `/libs/sling-cms/content/static/content.json`

```
content.json
├── jcr:content
│   └── container
│       ├── contentactions        # Add File/Folder buttons
│       ├── contentbreadcrumb     # Navigation breadcrumb
│       └── contenttable          # File listing table
│           ├── columns           # Column definitions
│           └── types             # Type-specific configurations
│               ├── sling:File    # Full file actions
│               ├── nt:file       # Basic file actions
│               ├── sling:OrderedFolder
│               └── sling:Folder
```

### Available Actions by Type

#### sling:File
- Edit Properties (`/cms/file/edit.html`)
- Preview Image (`/cms/file/imagepreview.html`)
- Optimize File (`/cms/file/optimize.html`)
- Download (`/cms/file/download.html`)
- Move/Copy (`/cms/shared/movecopy.html`)
- References (`/cms/shared/references.html`)
- Manage Versions (`/cms/shared/versions.html`)
- Delete (`/cms/shared/delete.html`)

#### nt:file (limited)
- Download
- Move/Copy
- References
- Delete

#### Folders
- Edit Properties
- Move/Copy
- References
- Delete

---

## Gap Analysis

### Missing DAM Features

| Feature | Priority | Complexity | Notes |
|---------|----------|------------|-------|
| **Video Thumbnails** | High | Medium | Need FFmpeg or similar |
| **Audio Waveforms** | Medium | Medium | Need audio processing library |
| **Automatic Renditions** | High | Low | Event listener on upload |
| **Asset Tagging** | High | Low | New property + UI |
| **Search/Filter** | High | Medium | Query builder + UI |
| **Bulk Upload Progress** | Medium | Low | Enhanced upload component |
| **Focal Point** | Medium | Low | Property + crop adjustment |
| **Usage Analytics** | Low | Medium | Reference tracking enhancement |
| **Metadata Editor** | Medium | Low | Enhanced edit dialog |
| **Asset Collections** | Low | Medium | New content structure |

### Missing Thumbnail Providers

| File Type | MIME Type | Priority | Library Needed |
|-----------|-----------|----------|----------------|
| Video | `video/*` | High | OpenCV (JavaCV) |
| Audio | `audio/*` | Medium | (waveform image) |
| Word | `application/msword`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | Medium | Apache POI |
| Excel | `application/vnd.ms-excel`, `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` | Low | Apache POI |
| SVG | `image/svg+xml` | Low | Batik / direct render |

---

## Extension Plan

### Architecture Overview

The DAM enhancement follows a **layered architecture** with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────────┐
│                        UI Layer                                  │
│  (HTL Components, JavaScript, CSS)                              │
├─────────────────────────────────────────────────────────────────┤
│                     API Layer (Public)                           │
│  ThumbnailProvider, TransformationHandler, RenditionSupport     │
│  (Stable interfaces - @ConsumerType)                            │
├─────────────────────────────────────────────────────────────────┤
│                  Service Layer (Internal)                        │
│  VideoFrameExtractor, MetadataExtractor, RenditionGenerator     │
│  (Implementation details - can change)                          │
├─────────────────────────────────────────────────────────────────┤
│               Provider Layer (Pluggable)                         │
│  ImageProvider, PdfProvider, VideoProvider, DocProvider         │
│  (Strategy pattern - easily extensible)                         │
├─────────────────────────────────────────────────────────────────┤
│              Library Abstraction Layer                           │
│  Pure Java ←→ Native Accelerated (auto-selection)               │
│  (Platform independence via abstraction)                        │
└─────────────────────────────────────────────────────────────────┘
```

### Design Patterns Used

| Pattern | Application | Benefit |
|---------|-------------|---------|
| **Strategy** | ThumbnailProvider implementations | Easy to add new file type support |
| **Chain of Responsibility** | Provider selection by MIME type | Fallback mechanism |
| **Factory** | VideoFrameExtractorFactory | Runtime implementation selection |
| **Adapter** | Library abstraction wrappers | Platform independence |
| **Template Method** | Base provider classes | Code reuse, consistent behavior |

---

### Phase 1: Enhanced Thumbnail Providers

#### 1.1 Video Thumbnail Provider (Platform-Independent Architecture)

**Design Goals:**
- ✅ Works on all platforms without native dependencies (default mode)
- ✅ Automatically uses native acceleration when available
- ✅ Graceful degradation if libraries unavailable
- ✅ OSGi-compliant with proper service lifecycle

**Architecture:**

```
VideoThumbnailProvider
        │
        ▼
VideoFrameExtractor (SPI)
        │
        ▼
┌──────────────────────┐
│ FFmpegFrameExtractor │
│ (ProcessBuilder)     │
│ Priority: 100        │
└──────────────────────┘
```

**SPI Interface for Video Frame Extraction:**

```java
package org.apache.sling.thumbnails.extension;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * SPI for extracting frames from video files.
 * Implementations should use OSGi-compliant approaches (e.g., system tools via ProcessBuilder).
 */
@ConsumerType
public interface VideoFrameExtractor {
    
    /**
     * Check if this extractor is available on the current system.
     * @return true if the required tools (e.g., FFmpeg) are available
     */
    boolean isAvailable();
    
    /**
     * Get the priority of this extractor. Higher priority extractors
     * are preferred when multiple are available.
     * @return priority value (higher = preferred)
     */
    int getPriority();
    
    /**
     * Get supported video MIME types.
     * @return set of supported MIME types
     */
    Set<String> getSupportedTypes();
    
    /**
     * Extract the best frame from a video file.
     * @param videoFile the video file
     * @return the extracted frame as BufferedImage, or null if extraction fails
     * @throws IOException if extraction fails
     */
    BufferedImage extractFrame(File videoFile) throws IOException;
}
```

**FFmpeg Implementation (OSGi Compliant - Uses ProcessBuilder):**

```java
package org.apache.sling.thumbnails.internal.providers.video;

import org.apache.sling.thumbnails.extension.VideoFrameExtractor;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg-based video frame extractor using ProcessBuilder.
 * OSGi compliant - uses system-installed FFmpeg, no embedded native libraries.
 */
@Component(
    service = VideoFrameExtractor.class,
    configurationPid = "org.apache.sling.thumbnails.video.FFmpegVideoFrameExtractor"
)
@Designate(ocd = FFmpegVideoFrameExtractor.Config.class)
public class FFmpegVideoFrameExtractor implements VideoFrameExtractor {
    
    private static final Logger LOG = LoggerFactory.getLogger(FFmpegVideoFrameExtractor.class);
    
    private static final Set<String> SUPPORTED_TYPES = Set.of(
        "video/mp4", "video/webm", "video/quicktime", "video/x-msvideo",
        "video/x-matroska", "video/avi", "video/mpeg", "video/x-m4v"
    );
    
    @ObjectClassDefinition(name = "FFmpeg Video Frame Extractor Configuration")
    public @interface Config {
        @AttributeDefinition(name = "FFmpeg Path", description = "Path to FFmpeg executable")
        String ffmpegPath() default "ffmpeg";
        
        @AttributeDefinition(name = "Frame Position", description = "Position to extract frame (seconds)")
        int framePositionSeconds() default 5;
        
        @AttributeDefinition(name = "Timeout", description = "Extraction timeout (seconds)")
        int timeoutSeconds() default 30;
    }
    
    private Config config;
    private boolean available;
    
    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        this.available = checkFFmpegAvailable();
        LOG.info("FFmpeg video frame extractor {}", available ? "activated" : "not available");
    }
    
    private boolean checkFFmpegAvailable() {
        try {
            Process process = new ProcessBuilder(config.ffmpegPath(), "-version")
                .redirectErrorStream(true)
                .start();
            return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            LOG.debug("FFmpeg not available: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean isAvailable() {
        return available;
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
    
    @Override
    public Set<String> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }
    
    @Override
    public BufferedImage extractFrame(File videoFile) throws IOException {
        if (!available) {
            throw new IOException("FFmpeg is not available");
        }
        
        Path outputFile = Files.createTempFile("video-frame-", ".png");
        try {
            ProcessBuilder pb = new ProcessBuilder(
                config.ffmpegPath(),
                "-ss", String.valueOf(config.framePositionSeconds()),
                "-i", videoFile.getAbsolutePath(),
                "-vframes", "1",
                "-y",
                outputFile.toString()
            );
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            boolean completed = process.waitFor(config.timeoutSeconds(), TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                throw new IOException("FFmpeg timed out");
            }
            
            if (process.exitValue() != 0 || !Files.exists(outputFile)) {
                throw new IOException("FFmpeg failed with exit code: " + process.exitValue());
            }
            
            return ImageIO.read(outputFile.toFile());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("FFmpeg interrupted", e);
        } finally {
            Files.deleteIfExists(outputFile);
        }
    }
}
```

**Video Thumbnail Provider (Uses SPI Pattern):**

```java
package org.apache.sling.thumbnails.internal.providers;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.thumbnails.extension.ThumbnailProvider;
import org.apache.sling.thumbnails.extension.VideoFrameExtractor;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Video thumbnail provider using FFmpeg-based frame extraction.
 * OSGi compliant - uses system tools via ProcessBuilder.
 */
@Component(
    service = ThumbnailProvider.class,
    immediate = true,
    configurationPid = "org.apache.sling.thumbnails.VideoThumbnailProvider"
)
public class VideoThumbnailProvider implements ThumbnailProvider {
    
    private static final Logger LOG = LoggerFactory.getLogger(VideoThumbnailProvider.class);
    
    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile List<VideoFrameExtractor> extractors = new ArrayList<>();
    
    @Override
    public boolean applies(Resource resource, String metaType) {
        try {
            MimeType mt = new MimeType(metaType);
            return mt.match("video/*");
        } catch (MimeTypeParseException e) {
            return false;
        }
    }
    
    @Override
    public InputStream getThumbnail(Resource resource) throws IOException {
        // Find best available extractor
        VideoFrameExtractor extractor = findBestExtractor(resource);
        if (extractor == null) {
            LOG.warn("No video frame extractor available for resource: {}", resource.getPath());
            return null;
        }
        
        Path tempFile = null;
        try (InputStream is = resource.adaptTo(InputStream.class)) {
            if (is == null) return null;
            
            // Write to temp file (FFmpeg requires file access)
            tempFile = Files.createTempFile("video-thumb-", ".tmp");
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            
            BufferedImage thumbnail = extractor.extractFrame(tempFile.toFile());
            
            if (thumbnail != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(thumbnail, "png", baos);
                return new ByteArrayInputStream(baos.toByteArray());
            }
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    LOG.debug("Failed to delete temp file: {}", tempFile, e);
                }
            }
        }
        return null;
    }
    
    private VideoFrameExtractor findBestExtractor(Resource resource) {
        String mimeType = getMimeType(resource);
        
        return extractors.stream()
            .filter(VideoFrameExtractor::isAvailable)
            .filter(e -> e.getSupportedTypes().stream()
                .anyMatch(type -> mimeTypeMatches(mimeType, type)))
            .max(Comparator.comparingInt(VideoFrameExtractor::getPriority))
            .orElse(null);
    }
    
    private String getMimeType(Resource resource) {
        return resource.getValueMap().get("jcr:content/jcr:mimeType", String.class);
    }
    
    private boolean mimeTypeMatches(String actual, String pattern) {
        if (actual == null || pattern == null) return false;
        if (pattern.endsWith("/*")) {
            return actual.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return actual.equals(pattern);
    }
}
```

**Platform Support Matrix:**

| Platform | FFmpeg Available | Installation |
|----------|-----------------|--------------|
| Linux x86_64 | ✅ | `apt install ffmpeg` or `yum install ffmpeg` |
| Linux ARM64 | ✅ | `apt install ffmpeg` |
| macOS x86_64 | ✅ | `brew install ffmpeg` |
| macOS ARM64 (M1/M2) | ✅ | `brew install ffmpeg` |
| Windows x86_64 | ✅ | Download from ffmpeg.org |
| Docker/Container | ✅ | Add to Dockerfile: `RUN apt-get install -y ffmpeg` |
| Cloud Functions | ⚠️ | May require custom layer |

---

#### 1.2 Word Document Thumbnail Provider

Uses existing Apache POI dependency (`poi-ooxml` 5.5.1 already in thumbnails module):

```java
package org.apache.sling.thumbnails.internal.providers;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.thumbnails.ThumbnailProvider;
import org.osgi.service.component.annotations.Component;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

@Component(service = ThumbnailProvider.class, immediate = true)
public class WordThumbnailProvider implements ThumbnailProvider {
    
    private static final int THUMBNAIL_WIDTH = 400;
    private static final int THUMBNAIL_HEIGHT = 550;
    
    @Override
    public boolean applies(Resource resource, String metaType) {
        try {
            MimeType mt = new MimeType(metaType);
            return mt.match("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } catch (MimeTypeParseException e) {
            return false;
        }
    }
    
    @Override
    public InputStream getThumbnail(Resource resource) throws IOException {
        try (InputStream is = resource.adaptTo(InputStream.class)) {
            if (is == null) return null;
            
            XWPFDocument document = new XWPFDocument(is);
            BufferedImage thumbnail = renderDocumentPreview(document);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "png", baos);
            return new ByteArrayInputStream(baos.toByteArray());
        }
    }
    
    private BufferedImage renderDocumentPreview(XWPFDocument document) {
        BufferedImage image = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, 
            BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // White background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        
        // Render text preview
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        
        int y = 20;
        for (XWPFParagraph para : document.getParagraphs()) {
            String text = para.getText();
            if (text != null && !text.trim().isEmpty()) {
                // Word wrap
                for (String line : wrapText(text, g2d.getFontMetrics(), THUMBNAIL_WIDTH - 40)) {
                    g2d.drawString(line, 20, y);
                    y += 14;
                    if (y > THUMBNAIL_HEIGHT - 20) break;
                }
            }
            if (y > THUMBNAIL_HEIGHT - 20) break;
        }
        
        g2d.dispose();
        return image;
    }
    
    private String[] wrapText(String text, FontMetrics fm, int maxWidth) {
        // Simple word wrapping implementation
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (fm.stringWidth(line + " " + word) > maxWidth) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                if (line.length() > 0) line.append(" ");
                line.append(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines.toArray(new String[0]);
    }
}
```

**Note:** For legacy `.doc` format, use `poi-scratchpad` (also already available):

```java
// For .doc files (application/msword)
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
```

### Phase 2: Automatic Rendition Generation ✅ IMPLEMENTED

Automatic rendition generation is now fully implemented with the following components:

#### 2.1 AutoRenditionConfig Interface (API)

```java
package org.apache.sling.thumbnails;

/**
 * Configuration interface for automatic rendition generation.
 */
@ProviderType
public interface AutoRenditionConfig {
    /** Returns whether automatic rendition generation is enabled. */
    boolean isEnabled();
    
    /** Returns the transformation names to apply automatically. */
    String[] getTransformationNames();
    
    /** Returns the MIME type patterns (supports wildcards like "image/*"). */
    String[] getSupportedMimeTypes();
    
    /** Returns the content paths under which auto-renditions are generated. */
    String[] getContentPaths();
}
```

#### 2.2 AutoRenditionListener (Resource Change Listener)

```java
package org.apache.sling.thumbnails.internal;

/**
 * Resource Change Listener that queues jobs for automatic rendition generation.
 */
@Component(
    service = {ResourceChangeListener.class, ExternalResourceChangeListener.class},
    property = {ResourceChangeListener.CHANGES + "=ADDED"},
    immediate = true)
public class AutoRenditionListener implements ResourceChangeListener, ExternalResourceChangeListener {
    
    @Reference
    private JobManager jobManager;
    
    @Reference
    private AutoRenditionConfig autoRenditionConfig;
    
    @Override
    public void onChange(List<ResourceChange> changes) {
        // Filter by configured paths, supported types, and MIME patterns
        // Queue AutoRenditionJobConsumer jobs for each matching resource
    }
}
```

#### 2.3 AutoRenditionJobConsumer (Sling Job Consumer)

```java
package org.apache.sling.thumbnails.internal;

/**
 * Sling Job Consumer for generating renditions in the background.
 */
@Component(
    service = JobConsumer.class,
    property = {JobConsumer.PROPERTY_TOPICS + "=" + AutoRenditionJobConsumer.TOPIC})
public class AutoRenditionJobConsumer implements JobConsumer {
    
    public static final String TOPIC = "org/apache/sling/thumbnails/AutoRendition";
    
    @Reference
    private Transformer transformer;
    
    @Reference
    private RenditionSupport renditionSupport;
    
    @Override
    public JobResult process(Job job) {
        // Get transformation from cache
        // Transform the resource
        // Save the rendition
    }
}
```

#### 2.4 OSGi Configuration

PID: `org.apache.sling.thumbnails.internal.AutoRenditionConfigImpl`

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | Boolean | `true` | Enable automatic rendition generation |
| `transformationNames` | String[] | `["thumbnail"]` | Transformations to apply |
| `supportedMimeTypes` | String[] | `["image/*"]` | MIME patterns (supports wildcards) |
| `contentPaths` | String[] | `["/content", "/static"]` | Content paths to monitor |

#### 2.5 Rendition Configuration

Transformations are defined under `/conf/{site}/files/transformations/`:

```
/conf/{site}/files/transformations/
├── thumbnail/     # 200x200 (default)
├── small/         # 480px width
├── medium/        # 960px width  
├── large/         # 1920px width
└── webp/          # WebP format conversion
```

### Phase 3: Enhanced Asset UI

#### 3.1 Asset Card View Component

New component: `sling-cms/components/cms/assetcard`

Features:
- Thumbnail preview (uses transformation servlet)
- Quick metadata display
- Type indicator icon
- Size/dimensions display
- Tag pills

#### 3.2 Asset Filter Bar

New component: `sling-cms/components/cms/assetfilter`

Filters:
- File type (Image, Video, Document, Audio, Other)
- Date range
- Tags
- Size range
- Dimensions (for images)

#### 3.3 Enhanced Metadata Editor

Extend `/cms/file/edit.html` to show:
- EXIF data for images
- Duration for video/audio
- Page count for documents
- Custom metadata fields
- Tag management

### Phase 4: Asset API Extensions

#### 4.1 Enhanced Asset Interface

```java
public interface Asset extends File {
    
    /**
     * Get the asset type category
     */
    AssetType getAssetType();
    
    /**
     * Get all renditions for this asset
     */
    List<Rendition> getRenditions();
    
    /**
     * Get specific rendition by name
     */
    Rendition getRendition(String name);
    
    /**
     * Get all tags assigned to this asset
     */
    List<String> getTags();
    
    /**
     * Get focal point coordinates (0-1 range)
     */
    FocalPoint getFocalPoint();
    
    /**
     * Get all pages/components referencing this asset
     */
    List<Resource> getUsages();
    
    /**
     * Get asset dimensions (for images/videos)
     */
    Dimensions getDimensions();
    
    /**
     * Get duration in seconds (for audio/video)
     */
    Long getDuration();
}

public enum AssetType {
    IMAGE("image/*"),
    VIDEO("video/*"),
    AUDIO("audio/*"),
    DOCUMENT("application/pdf", "application/msword", ...),
    PRESENTATION("application/vnd.ms-powerpoint", ...),
    SPREADSHEET("application/vnd.ms-excel", ...),
    ARCHIVE("application/zip", ...),
    OTHER("*/*");
}
```

---

## Extensibility Guide

### Adding a New Thumbnail Provider

To add support for a new file type, implement the `ThumbnailProvider` interface:

```java
package org.apache.sling.thumbnails.internal.providers;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.thumbnails.extension.ThumbnailProvider;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.io.*;

/**
 * Template for creating a new thumbnail provider.
 * 
 * Key requirements:
 * 1. Implement ThumbnailProvider interface
 * 2. Register as OSGi component
 * 3. Use service.ranking for priority control
 * 4. Handle errors gracefully (return null, don't throw)
 */
@Component(
    service = ThumbnailProvider.class,
    immediate = true,
    property = {
        "service.ranking:Integer=50"  // Adjust priority as needed
    },
    configurationPolicy = ConfigurationPolicy.OPTIONAL
)
@Designate(ocd = MyThumbnailProviderConfig.class)
public class MyThumbnailProvider implements ThumbnailProvider {
    
    @Override
    public boolean applies(Resource resource, String metaType) {
        try {
            MimeType mt = new MimeType(metaType);
            return mt.match("application/my-type");
        } catch (MimeTypeParseException e) {
            return false;
        }
    }
    
    @Override
    public InputStream getThumbnail(Resource resource) throws IOException {
        // Implementation here
        // Return null if thumbnail cannot be generated
        return null;
    }
}

@ObjectClassDefinition(name = "My Thumbnail Provider Configuration")
@interface MyThumbnailProviderConfig {
    @AttributeDefinition(name = "Enabled", description = "Enable this provider")
    boolean enabled() default true;
}
```

### Adding a New Transformation Handler

To add a new image transformation:

```java
package org.apache.sling.thumbnails.internal.transformers;

import org.apache.sling.thumbnails.TransformationHandlerConfig;
import org.apache.sling.thumbnails.extension.TransformationHandler;
import org.osgi.service.component.annotations.Component;

import java.io.*;

@Component(service = TransformationHandler.class, immediate = true)
public class MyTransformationHandler implements TransformationHandler {
    
    public static final String RESOURCE_TYPE = "sling/thumbnails/transformers/my-transform";
    
    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }
    
    @Override
    public void handle(InputStream inputStream, OutputStream outputStream, 
                       TransformationHandlerConfig config) throws IOException {
        // Use Thumbnailator or other pure-Java libraries
        // Avoid native dependencies for portability
    }
}
```

### Best Practices for Extensions

| Practice | Description |
|----------|-------------|
| **Pure Java First** | Prefer pure Java libraries over native bindings |
| **Graceful Degradation** | Return null or placeholder instead of throwing exceptions |
| **OSGi Configuration** | Make behavior configurable via OSGi |
| **Service Ranking** | Use `service.ranking` for priority control |
| **Lazy Loading** | Load optional dependencies only when needed |
| **Resource Cleanup** | Always close streams and delete temp files |
| **Logging** | Use SLF4J for consistent logging |

---

## Implementation Roadmap

### Sprint 1: Video Support (2 weeks) ✅ COMPLETED

1. ~~Add FFmpeg/JavaCV dependency evaluation~~ → ✅ Selected FFmpeg via ProcessBuilder (OSGi compliant)
2. ✅ Implement `VideoFrameExtractor` SPI in `extension` package
3. ✅ Implement `FFmpegVideoFrameExtractor` using ProcessBuilder
4. ✅ Implement `VideoThumbnailProvider` with SPI pattern
5. ✅ Add comprehensive unit tests (11 tests)
6. ✅ All 102 tests passing
7. ✅ Supports common video formats (MP4, WebM, MOV, AVI, MKV, etc.)

### Sprint 2: Auto-Renditions (2 weeks) ✅ COMPLETED

1. ✅ Implement `AutoRenditionListener` (ResourceChangeListener for detecting new files)
2. ✅ Implement `AutoRenditionJobConsumer` (Sling Job consumer for background processing)
3. ✅ Implement `AutoRenditionConfig` interface and `AutoRenditionConfigImpl` (OSGi configuration)
4. ✅ Add comprehensive unit tests (23 new tests)
5. ✅ All 126 tests passing
6. OSGi-configurable:
   - Enable/disable auto-renditions
   - Configure transformation names to apply
   - Configure MIME type patterns (supports wildcards like `image/*`)
   - Configure content paths

### Sprint 3: Asset UI Enhancement (3 weeks)

1. Create asset card view component
2. Implement filter bar with search
3. Add grid/list view toggle
4. Enhance metadata editor modal

### Sprint 4: Tagging & Search (2 weeks)

1. Add tag property support
2. Create tag management UI
3. Implement tag-based filtering
4. Add quick search functionality

### Sprint 5: API & Polish (2 weeks)

1. Implement enhanced Asset interface
2. Add usage tracking
3. Implement focal point selection
4. Performance optimization & testing

---

## Configuration Reference

### ThumbnailSupport OSGi Configuration

PID: `org.apache.sling.thumbnails.internal.ThumbnailSupportImpl`

```
supportedTypes=["nt:file=jcr:content/jcr:mimeType","sling:File=jcr:content/jcr:mimeType"]
persistableTypes=["sling:File=jcr:content/renditions"]
errorResourcePath="/static/sling-cms/thumbnails/file.png"
errorSuffix="/file.png"
```

### Default Transformation Handlers

| Handler | Resource Type |
|---------|---------------|
| Resize | `sling/thumbnails/transformers/resize` |
| Crop | `sling/thumbnails/transformers/crop` |
| Rotate | `sling/thumbnails/transformers/rotate` |
| Flip | `sling/thumbnails/transformers/flip` |
| Scale | `sling/thumbnails/transformers/scale` |
| Colorize | `sling/thumbnails/transformers/colorize` |
| Greyscale | `sling/thumbnails/transformers/greyscale` |
| Transparency | `sling/thumbnails/transformers/transparency` |

---

## Architectural Decision Records (ADRs)

### ADR-001: OSGi-Compliant Video Processing

**Status:** Accepted

**Context:** Video thumbnail generation requires extracting frames from video files. Most Java libraries for video processing (JCodec, JavaCV, etc.) are not OSGi-compliant and would require embedding non-OSGi JARs.

**Decision:** Use FFmpeg via `ProcessBuilder` for video frame extraction:
- FFmpeg is a system tool invoked via `ProcessBuilder`
- No embedded non-OSGi libraries required
- Follows existing pattern used for other system tools

**Consequences:**
- ✅ Fully OSGi compliant - no embedded native libraries
- ✅ Broad format support (all formats FFmpeg supports)
- ✅ High-quality frame extraction
- ✅ Easy to install on all platforms
- ⚠️ Requires FFmpeg to be installed on the system
- ⚠️ Returns null gracefully if FFmpeg not available

### ADR-002: OSGi Service Provider Interface (SPI) Pattern

**Status:** Accepted

**Context:** Need to support multiple thumbnail providers and transformation handlers that can be easily extended.

**Decision:** Use OSGi's `@ConsumerType` annotation and multiple service implementations with `service.ranking` for priority control.

**Consequences:**
- ✅ Easy to add new providers without modifying existing code
- ✅ Providers can be enabled/disabled via OSGi config
- ✅ Priority-based selection allows override of default implementations
- ✅ Follows existing Sling patterns

### ADR-003: Graceful Degradation Strategy

**Status:** Accepted

**Context:** Optional native libraries may not be available in all deployment environments.

**Decision:** 
- Check library availability at component activation
- Use factory pattern to select best available implementation
- Return null/placeholder when no suitable provider available
- Never throw exceptions for missing optional dependencies

**Consequences:**
- ✅ Application never fails due to missing optional dependencies
- ✅ Transparent fallback to lower-quality alternatives
- ✅ Easy to diagnose via logging
- ⚠️ Requires careful null handling throughout

### ADR-004: Configuration-Driven Behavior

**Status:** Accepted

**Context:** Different deployments may need different thumbnail generation strategies.

**Decision:** All significant behavior should be configurable via OSGi Configuration Admin:
- Sample positions for video frame extraction
- Face detection enable/disable
- Timeout values
- Provider enable/disable

**Consequences:**
- ✅ No code changes needed for behavior adjustments
- ✅ Different configs for dev/staging/prod
- ✅ Can tune performance vs. quality tradeoffs
- ⚠️ More configuration options to document

---

## Related Documentation

- [Image Transformations](image-transformations.md)
- [Configure File Editor](configure-file-editor.md)
- [Managing Content](managing-content.md)

---

*Last Updated: December 13, 2025*
