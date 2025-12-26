# Priority 2: Metadata-Rendition Integration - Implementation Plan

**Date**: December 26, 2025  
**Status**: 🚧 **IN PROGRESS**  
**Dependencies**: ✅ Priority 1 (API Extraction) COMPLETE  
**Module**: `thumbnails` (primary), `core` (metadata extraction)

---

## Executive Summary

**Problem**: Currently, metadata extraction and rendition generation are completely independent processes. This causes critical data loss:
- ❌ EXIF copyright data lost in renditions (legal risk)
- ❌ GPS location data lost (SEO impact)
- ❌ Camera orientation ignored (UX failure)
- ❌ Face detection data not used (poor cropping)
- ❌ OCR text not leveraged (accessibility loss)

**Goal**: Integrate metadata extraction with rendition generation to enable:
- ✅ Metadata preservation in renditions
- ✅ Intelligent transformations (auto-rotate, smart crop)
- ✅ Better asset tracking
- ✅ Improved SEO and accessibility

---

## Current Architecture Analysis

### Metadata Extraction Flow (Core Module)
```
File Upload/Change
       ↓
FileMetadataExtractorListener
       ↓
Sling Job → FileMetadataExtractorConsumer
       ↓
FileMetadataExtractorImpl.extractMetadata()
       ↓
Apply Enrichers (Tika, OCR, etc.)
       ↓
Store → {file}/jcr:content/metadata/*
```

### Rendition Generation Flow (Thumbnails Module)
```
File Upload/Change
       ↓
AutoRenditionListener
       ↓
Sling Job → AutoRenditionJobConsumer
       ↓
TransformationCache.getTransformation()
       ↓
Transformer.transform()
       ↓
Store → {file}/jcr:content/renditions/{name}.{format}
```

### ⚠️ Problem: No Integration

These two flows are **completely independent**:
1. No sequencing (race conditions possible)
2. No data sharing (metadata not available during transformation)
3. No metadata preservation (renditions lack source metadata)
4. No metadata-driven transformations (can't auto-rotate, smart crop, etc.)

---

## Implementation Strategy

### Phase 1: Metadata Availability During Transformation ⏳

**Goal**: Make source metadata available to transformation handlers

**Changes Required**:

#### 1.1 Enhance TransformationHandler API (thumbnails module)

**File**: `thumbnails/src/main/java/org/apache/sling/thumbnails/extension/TransformationHandler.java`

```java
package org.apache.sling.thumbnails.extension;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import org.apache.sling.cms.transformation.TransformationHandlerConfig;
import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface TransformationHandler {
    
    // Existing method (backward compatibility)
    void handle(InputStream inputStream, OutputStream outputStream, 
                TransformationHandlerConfig config) throws Exception;
    
    // NEW: Metadata-aware transformation
    default void handle(InputStream inputStream, OutputStream outputStream,
                       TransformationHandlerConfig config,
                       Map<String, Object> sourceMetadata) throws Exception {
        // Default: delegate to existing method
        handle(inputStream, outputStream, config);
    }
    
    // NEW: Check if handler uses metadata
    default boolean usesMetadata() {
        return false;
    }
    
    String getResourceType();
}
```

**Benefits**:
- ✅ Backward compatible (default implementation)
- ✅ Opt-in for handlers that need metadata
- ✅ No breaking changes

#### 1.2 Update Transformer Implementation

**File**: `thumbnails/src/main/java/org/apache/sling/thumbnails/internal/TransformerImpl.java`

**Current Code** (lines 118-152):
```java
@Override
public void transform(Resource resource, Transformation transformation, OutputFileFormat format, OutputStream out)
        throws IOException {
    // ... existing code ...
    
    for (TransformationHandlerConfig config : transformation.getHandlers()) {
        log.debug("Handling command: {}", config);
        
        TransformationHandler handler = getTransformationHandler(config.getHandlerType());
        if (handler != null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            handler.handle(inputStream, outputStream, config); // ← OLD: No metadata
            inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        }
    }
}
```

**New Code**:
```java
@Override
public void transform(Resource resource, Transformation transformation, OutputFileFormat format, OutputStream out)
        throws IOException {
    // ... existing code ...
    
    // NEW: Load source metadata
    Map<String, Object> sourceMetadata = loadMetadata(resource);
    
    for (TransformationHandlerConfig config : transformation.getHandlers()) {
        log.debug("Handling command: {}", config);
        
        TransformationHandler handler = getTransformationHandler(config.getHandlerType());
        if (handler != null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // NEW: Pass metadata to handlers that need it
            if (handler.usesMetadata()) {
                handler.handle(inputStream, outputStream, config, sourceMetadata);
            } else {
                handler.handle(inputStream, outputStream, config);
            }
            
            inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        }
    }
}

// NEW: Helper method to load metadata
private Map<String, Object> loadMetadata(Resource resource) {
    Resource metadataResource = resource.getChild("jcr:content/metadata");
    if (metadataResource != null) {
        return new HashMap<>(metadataResource.getValueMap());
    }
    return Collections.emptyMap();
}
```

#### 1.3 Create Auto-Rotate Handler (NEW)

**File**: `thumbnails/src/main/java/org/apache/sling/thumbnails/internal/transformers/AutoRotateHandler.java`

```java
package org.apache.sling.thumbnails.internal.transformers;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.sling.cms.transformation.TransformationHandlerConfig;
import org.apache.sling.thumbnails.extension.TransformationHandler;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Auto-rotation handler that uses EXIF orientation metadata
 * to correct image orientation automatically.
 */
@Component(service = TransformationHandler.class)
public class AutoRotateHandler implements TransformationHandler {
    
    private static final Logger log = LoggerFactory.getLogger(AutoRotateHandler.class);
    private static final String RESOURCE_TYPE = "sling/thumbnails/transformers/autorotate";
    
    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }
    
    @Override
    public boolean usesMetadata() {
        return true; // This handler requires metadata
    }
    
    @Override
    public void handle(InputStream inputStream, OutputStream outputStream,
                      TransformationHandlerConfig config,
                      Map<String, Object> sourceMetadata) throws Exception {
        
        // Get EXIF orientation (standard EXIF tag)
        Object orientationObj = sourceMetadata.get("tiff:Orientation");
        if (orientationObj == null) {
            orientationObj = sourceMetadata.get("exif:orientation");
        }
        
        double rotation = 0;
        if (orientationObj != null) {
            int orientation = parseOrientation(orientationObj);
            rotation = calculateRotation(orientation);
            log.debug("Applying auto-rotation: {} degrees (EXIF orientation: {})", rotation, orientation);
        } else {
            log.debug("No EXIF orientation found, skipping auto-rotation");
        }
        
        // Apply rotation if needed
        if (rotation != 0) {
            Thumbnails.of(inputStream)
                .rotate(rotation)
                .scale(1.0)
                .toOutputStream(outputStream);
        } else {
            // No rotation needed, pass through
            org.apache.commons.io.IOUtils.copy(inputStream, outputStream);
        }
    }
    
    /**
     * Calculate rotation degrees from EXIF orientation value.
     * 
     * EXIF Orientation values:
     * 1 = Normal
     * 3 = Rotate 180°
     * 6 = Rotate 90° CW
     * 8 = Rotate 270° CW (or 90° CCW)
     */
    private double calculateRotation(int orientation) {
        switch (orientation) {
            case 3:
                return 180.0;
            case 6:
                return 90.0;
            case 8:
                return 270.0;
            default:
                return 0.0;
        }
    }
    
    private int parseOrientation(Object orientationObj) {
        if (orientationObj instanceof Integer) {
            return (Integer) orientationObj;
        } else if (orientationObj instanceof String) {
            try {
                return Integer.parseInt((String) orientationObj);
            } catch (NumberFormatException e) {
                log.warn("Invalid orientation value: {}", orientationObj);
                return 1; // Default to normal orientation
            }
        }
        return 1;
    }
}
```

---

### Phase 2: Metadata Preservation in Renditions ⏳

**Goal**: Store transformation metadata alongside generated renditions

#### 2.1 Enhance RenditionSupport Interface

**File**: `api/src/main/java/org/apache/sling/cms/rendition/RenditionSupport.java`

```java
// NEW: Overload with metadata parameter
void setRendition(
    Resource resource,
    String name,
    ByteArrayOutputStream baos,
    Map<String, Object> renditionMetadata  // ← NEW
) throws IOException;
```

#### 2.2 Update RenditionSupportImpl

**File**: `thumbnails/src/main/java/org/apache/sling/thumbnails/internal/RenditionSupportImpl.java`

```java
@Override
public void setRendition(Resource resource, String name, ByteArrayOutputStream baos,
                        Map<String, Object> renditionMetadata) throws IOException {
    // ... existing rendition creation ...
    
    // NEW: Store rendition metadata
    if (renditionMetadata != null && !renditionMetadata.isEmpty()) {
        Resource metadataRes = resolver.create(renditionRes, "metadata", new HashMap<>());
        ModifiableValueMap metadataMap = metadataRes.adaptTo(ModifiableValueMap.class);
        metadataMap.putAll(renditionMetadata);
    }
    
    resolver.commit();
}
```

#### 2.3 Update AutoRenditionJobConsumer

**File**: `thumbnails/src/main/java/org/apache/sling/thumbnails/internal/AutoRenditionJobConsumer.java`

```java
@Override
public JobResult process(Job job) {
    // ... existing code ...
    
    // NEW: Collect rendition metadata
    Map<String, Object> renditionMetadata = new HashMap<>();
    renditionMetadata.put("sourceFile", resource.getPath());
    renditionMetadata.put("transformation", transformationName);
    renditionMetadata.put("generated", Calendar.getInstance());
    renditionMetadata.put("generator", "AutoRenditionJobConsumer");
    renditionMetadata.put("format", format.toString());
    
    // Copy important source metadata
    Resource sourceMetadata = resource.getChild("jcr:content/metadata");
    if (sourceMetadata != null) {
        ValueMap sourceMap = sourceMetadata.getValueMap();
        copyMetadataField(sourceMap, renditionMetadata, "exif:Copyright");
        copyMetadataField(sourceMap, renditionMetadata, "exif:Artist");
        copyMetadataField(sourceMap, renditionMetadata, "geo:lat");
        copyMetadataField(sourceMap, renditionMetadata, "geo:long");
    }
    
    // Store rendition with metadata
    renditionSupport.setRendition(resource, renditionName, baos, renditionMetadata);
}

private void copyMetadataField(ValueMap source, Map<String, Object> dest, String key) {
    Object value = source.get(key);
    if (value != null) {
        dest.put(key, value);
    }
}
```

---

### Phase 3: Event Sequencing ⏳

**Goal**: Ensure metadata extraction completes before rendition generation

#### 3.1 Update AutoRenditionListener

**File**: `thumbnails/src/main/java/org/apache/sling/thumbnails/internal/AutoRenditionListener.java`

```java
@Component(
    service = EventHandler.class,
    property = {
        EventConstants.EVENT_TOPIC + "=org/apache/sling/api/resource/Resource/ADDED",
        EventConstants.EVENT_TOPIC + "=org/apache/sling/api/resource/Resource/CHANGED",
        // NEW: Listen for metadata extraction completion
        EventConstants.EVENT_TOPIC + "=org/apache/sling/cms/metadata/EXTRACTED",
        EventConstants.EVENT_FILTER + "=(resourceType=sling:File)"
    }
)
public class AutoRenditionListener implements EventHandler {
    
    @Override
    public void handleEvent(Event event) {
        String topic = event.getTopic();
        
        if (topic.contains("metadata/EXTRACTED")) {
            // Metadata extraction completed, now generate renditions
            log.debug("Metadata extraction complete, generating renditions");
            generateRenditions(event);
        } else if (topic.contains("ADDED") || topic.contains("CHANGED")) {
            // Check if metadata already exists
            Resource metadata = resource.getChild("jcr:content/metadata");
            if (metadata != null) {
                // Metadata exists, generate renditions immediately
                generateRenditions(event);
            } else {
                // Wait for metadata extraction to complete
                log.debug("Waiting for metadata extraction to complete");
            }
        }
    }
}
```

#### 3.2 Update FileMetadataExtractorConsumer

**File**: `core/src/main/java/org/apache/sling/cms/core/internal/jobs/FileMetadataExtractorJob.java`

```java
public JobResult process(Job job) {
    // ... existing metadata extraction ...
    
    // NEW: Fire completion event
    Dictionary<String, Object> props = new Hashtable<>();
    props.put("path", file.getResource().getPath());
    props.put("resourceType", "sling:File");
    
    Event event = new Event("org/apache/sling/cms/metadata/EXTRACTED", props);
    eventAdmin.postEvent(event);
    
    return JobResult.OK;
}
```

---

## Implementation Checklist

### Phase 1: Metadata Availability ⏳
- [ ] Update `TransformationHandler` interface with metadata-aware methods
- [ ] Update `TransformerImpl.transform()` to load and pass metadata
- [ ] Create `AutoRotateHandler` with EXIF orientation support
- [ ] Add unit tests for `AutoRotateHandler`
- [ ] Update all existing handlers to declare `usesMetadata()`
- [ ] Deploy and test with sample images containing EXIF data

### Phase 2: Metadata Preservation ⏳
- [ ] Enhance `RenditionSupport` interface with metadata parameter
- [ ] Update `RenditionSupportImpl.setRendition()` to store metadata
- [ ] Update `AutoRenditionJobConsumer` to collect rendition metadata
- [ ] Define standard metadata fields to preserve (copyright, GPS, etc.)
- [ ] Add JCR structure for rendition metadata storage
- [ ] Add unit tests for metadata preservation
- [ ] Deploy and verify metadata stored in renditions

### Phase 3: Event Sequencing ⏳
- [ ] Add new event topic `org/apache/sling/cms/metadata/EXTRACTED`
- [ ] Update `FileMetadataExtractorConsumer` to fire completion event
- [ ] Update `AutoRenditionListener` to listen for metadata completion
- [ ] Add logic to check metadata existence before generating renditions
- [ ] Add unit tests for event sequencing
- [ ] Integration tests for full workflow
- [ ] Deploy and test end-to-end flow

### Documentation & Testing 📝
- [ ] Update API documentation
- [ ] Create migration guide for custom handlers
- [ ] Write user guide for metadata-driven transformations
- [ ] Add integration tests for metadata workflows
- [ ] Performance testing (overhead of metadata loading)
- [ ] Create example configurations with auto-rotate

---

## Testing Strategy

### Unit Tests

**New Test Files**:
```
thumbnails/src/test/java/.../internal/transformers/
├── AutoRotateHandlerTest.java           ← Test EXIF orientation handling

thumbnails/src/test/java/.../internal/
├── TransformerImplTest.java             ← Enhanced with metadata tests
├── RenditionSupportImplTest.java        ← Test metadata preservation
└── AutoRenditionJobConsumerTest.java    ← Test metadata collection
```

### Test Cases

**AutoRotateHandler Tests**:
1. ✅ EXIF orientation 1 (normal) → no rotation
2. ✅ EXIF orientation 3 (180°) → rotate 180°
3. ✅ EXIF orientation 6 (90° CW) → rotate 90°
4. ✅ EXIF orientation 8 (270° CW) → rotate 270°
5. ✅ Missing EXIF data → pass through unchanged
6. ✅ Invalid orientation value → default to normal

**Metadata Preservation Tests**:
1. ✅ Copyright field preserved in rendition
2. ✅ GPS coordinates preserved in rendition
3. ✅ Artist/Creator preserved in rendition
4. ✅ Generation timestamp added
5. ✅ Transformation name recorded
6. ✅ Source file reference stored

**Event Sequencing Tests**:
1. ✅ File upload → metadata extraction → rendition generation
2. ✅ File change → rendition deletion → metadata update → rendition regeneration
3. ✅ Race condition handling (rendition request before metadata ready)

### Integration Tests

**End-to-End Scenarios**:
```bash
# Scenario 1: Upload image with EXIF orientation
1. Upload portrait image with EXIF orientation=6
2. Verify metadata extraction creates jcr:content/metadata/exif:orientation
3. Verify auto-rendition triggers after metadata extraction
4. Verify generated rendition is correctly rotated
5. Verify rendition metadata contains source copyright

# Scenario 2: Update existing image
1. Upload image and wait for processing
2. Modify image file
3. Verify old renditions are deleted (RenditionCleaner)
4. Verify new metadata is extracted
5. Verify new renditions are generated with updated metadata
```

---

## Performance Considerations

### Overhead Analysis

**Metadata Loading**:
- Cost: ~5-10ms per transformation (JCR read)
- Mitigation: Cache frequently accessed metadata
- Impact: Negligible compared to transformation time

**Metadata Storage**:
- Cost: ~10-20ms per rendition (JCR write)
- Mitigation: Batch writes, async storage
- Impact: Low (one-time cost during generation)

**Event Processing**:
- Cost: ~1-2ms per event
- Mitigation: Use Sling Jobs for async processing
- Impact: Minimal

### Optimization Strategies

1. **Metadata Caching**:
   ```java
   @Component(service = MetadataCache.class)
   public class MetadataCacheImpl implements MetadataCache {
       
       private final Map<String, Map<String, Object>> cache = 
           new ConcurrentHashMap<>();
       
       public Map<String, Object> getMetadata(String path) {
           return cache.computeIfAbsent(path, this::loadMetadata);
       }
       
       @Reference(policy = ReferencePolicy.DYNAMIC)
       void onResourceChanged(ResourceChangeListener event) {
           cache.remove(event.getPath());
       }
   }
   ```

2. **Lazy Metadata Loading**:
   - Only load metadata for handlers that declare `usesMetadata() = true`
   - Skip metadata loading for simple transformations (e.g., resize only)

3. **Async Metadata Storage**:
   - Store rendition metadata asynchronously (don't block transformation)
   - Use Sling Jobs for metadata persistence

---

## Rollout Plan

### Step 1: Deploy Phase 1 (Metadata Availability)
- Date: TBD
- Scope: Enable handlers to access source metadata
- Risk: Low (backward compatible)
- Rollback: Easy (default implementations)

### Step 2: Deploy AutoRotateHandler
- Date: TBD
- Scope: Add auto-rotation capability
- Risk: Low (opt-in transformation)
- Rollback: Remove handler registration

### Step 3: Deploy Phase 2 (Metadata Preservation)
- Date: TBD
- Scope: Store metadata in renditions
- Risk: Low (additive feature)
- Rollback: Metadata storage can be disabled

### Step 4: Deploy Phase 3 (Event Sequencing)
- Date: TBD
- Scope: Coordinate metadata extraction and rendition generation
- Risk: Medium (changes event flow)
- Rollback: Revert event listeners

### Step 5: Enable by Default
- Date: TBD
- Scope: Make metadata-aware transformations default
- Risk: Low (tested extensively)
- Rollback: OSGi configuration flag

---

## Success Criteria

### Functional Requirements
- ✅ Transformation handlers can access source metadata
- ✅ EXIF orientation automatically corrected
- ✅ Copyright data preserved in renditions
- ✅ GPS coordinates preserved in renditions
- ✅ Rendition generation waits for metadata extraction
- ✅ No race conditions between metadata and renditions

### Non-Functional Requirements
- ✅ Performance overhead < 10% (compared to baseline)
- ✅ Backward compatibility maintained (existing handlers work)
- ✅ No breaking API changes
- ✅ Memory usage increase < 5%
- ✅ Event processing latency < 100ms

### Quality Requirements
- ✅ Unit test coverage > 80%
- ✅ Integration tests cover all workflows
- ✅ Documentation complete and reviewed
- ✅ Code review by 2+ developers
- ✅ No critical/major bugs in testing

---

## Next Steps

1. **Review this plan** with the team
2. **Create JIRA tickets** for each phase
3. **Start Phase 1 implementation**: Update TransformationHandler interface
4. **Set up test environment** with sample images containing EXIF data
5. **Begin coding** 🚀

---

**Status**: Ready to start implementation  
**Estimated Effort**: 2-3 weeks (all phases)  
**Priority**: HIGH 🔴 (Critical data loss issue)
