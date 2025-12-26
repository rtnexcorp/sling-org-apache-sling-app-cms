# Thumbnail URL Pattern Fix

## Issue Summary
The `AssetCard.java` Sling Model was using incorrect URL patterns for thumbnail transformations and static icons, inherited from the old JSP implementation.

---

## URL Pattern Analysis

### Transform Servlet Registration
**Location:** `thumbnails/src/main/java/org/apache/sling/thumbnails/internal/TransformServlet.java`

**Servlet Configuration:**
```java
properties.put("sling.servlet.methods", new String[] {"GET"});
properties.put("sling.servlet.extensions", "transform");
properties.put("sling.servlet.resourceTypes", thumbnailSupport.getSupportedTypes());
```

**URL Pattern:** `{assetPath}.transform/{transformation-name}.{extension}`

**Example:**
- `/content/dam/image.jpg.transform/auto-rotate-thumbnail.png`
- `/content/dam/video.mp4.transform/sling-cms-thumbnail.png`

---

## Incorrect URLs (Old JSP Pattern)

### ❌ Wrong Pattern in AssetCard.java (Before Fix)
```java
// WRONG: Added unnecessary /cms/file/preview.html prefix
return "/cms/file/preview.html" + getPath() + ".transform/auto-rotate-thumbnail.png";
// Resulted in: /cms/file/preview.html/content/dam/image.jpg.transform/auto-rotate-thumbnail.png
```

### ❌ Wrong Static Icon Path (Before Fix)
```java
// WRONG: Non-existent path
return "/static/sling-cms/img/icons/file.png";
```

### ❌ Wrong Branding Resource Path (Before Fix)
```java
// WRONG: Incorrect branding resource location
Resource branding = resourceResolver.getResource("/mnt/overlay/sling-cms/content/branding");
```

---

## Correct URLs (Fixed)

### ✅ Correct Transform URLs
```java
// Image with auto-rotate
return getPath() + ".transform/auto-rotate-thumbnail.png";
// Example: /content/dam/image.jpg.transform/auto-rotate-thumbnail.png

// Video/PDF/Other
return getPath() + ".transform/sling-cms-thumbnail.png";
// Example: /content/dam/video.mp4.transform/sling-cms-thumbnail.png

// Fallback thumbnail
return getPath() + ".transform/sling-cms-thumbnail.png";
```

### ✅ Correct Static Icon URL
```java
// Static file icon for unsupported types
return "/static/sling-cms/thumbnails/file.png";

// With branding config
Resource branding = resourceResolver.getResource("/conf/global/sling-cms/branding");
String gridIconsBase = branding.getValueMap().get("gridIconsBase", "/static/sling-cms/thumbnails");
return gridIconsBase + "/file.png";
```

---

## Evidence from Other HTL Files

### Correct Pattern in `assetmetadataeditor.html`
```html
<a href="${meta.assetPath}.transform/sling-cms-thumbnail/image.png" 
   class="tag is-info" target="_blank" title="600x480 PNG">thumbnail</a>

<a href="${meta.assetPath}.transform/auto-rotate-thumbnail/image.png" 
   class="tag is-info" target="_blank" title="Auto-rotated 600x480 PNG">auto-rotate</a>
```

### Correct Pattern in `searchresults.html`
```html
<img src="${result.path}.transform/sling-cms-thumbnail.png" 
     loading="lazy" 
     alt="${result.title}">
```

### Incorrect Pattern in `ContentGridModel.java` (Needs Fix)
```java
// ContentGridModel also has the same issue - using /cms/file/preview.html prefix
return "/cms/file/preview.html" + getPath() + ".transform/auto-rotate-thumbnail.png";
// This should be fixed in a separate commit
```

---

## Static Files Location

**Directory:** `frontend/src/main/resources/jcr_root/static/sling-cms/thumbnails/`

**Available Icons:**
- `file.png` - Generic file icon
- `folder.png` - Folder icon
- `page.png` - Page icon
- `site.png` - Site icon
- `publish-agent.png` - Publish agent icon
- `importer.png` - Importer icon
- `exporter.png` - Exporter icon
- `bulk-publication.png` - Bulk publication icon

**URL Pattern:** `/static/sling-cms/thumbnails/{icon-name}.png`

---

## Changes Made to AssetCard.java

### 1. Fixed getThumbnailUrl()
```java
// Before
return "/cms/file/preview.html" + getPath() + ".transform/auto-rotate-thumbnail.png";

// After
return getPath() + ".transform/auto-rotate-thumbnail.png";
```

### 2. Fixed getFallbackThumbnailUrl()
```java
// Before
return "/cms/file/preview.html" + getPath() + ".transform/sling-cms-thumbnail.png";

// After
return getPath() + ".transform/sling-cms-thumbnail.png";
```

### 3. Fixed getDefaultIconUrl()
```java
// Before
Resource branding = resourceResolver.getResource("/mnt/overlay/sling-cms/content/branding");
return "/static/sling-cms/img/icons/file.png";

// After
Resource branding = resourceResolver.getResource("/conf/global/sling-cms/branding");
return "/static/sling-cms/thumbnails/file.png";
```

---

## Why /cms/file/preview.html Was Wrong

### What /cms/file/preview.html Is For
The `/cms/file/preview.html` path is likely a component or servlet that provides **file preview functionality** (viewing file contents), not transformation.

### Transform Servlet Registration
The `TransformServlet` is registered on the **extension** `transform`, which means:
- It intercepts URLs ending with `.transform`
- It applies directly to the file resource path
- No prefix needed - Sling's resource resolution handles routing

### Example Flow
1. **User requests:** `/content/dam/image.jpg.transform/auto-rotate-thumbnail.png`
2. **Sling resolves resource:** `/content/dam/image.jpg` (nt:file)
3. **Checks resource type:** `nt:file` (supported by TransformServlet)
4. **Checks extension:** `.transform` (matches servlet registration)
5. **TransformServlet handles request:** Applies transformation and returns image

---

## Testing Checklist

After deploying the Core module, test these URLs:

### Direct Transform URLs (Should Work)
- ✅ `/content/dam/test-image.jpg.transform/auto-rotate-thumbnail.png`
- ✅ `/content/dam/test-video.mp4.transform/sling-cms-thumbnail.png`
- ✅ `/content/dam/test-pdf.pdf.transform/sling-cms-thumbnail.png`

### Static Icon URLs (Should Work)
- ✅ `/static/sling-cms/thumbnails/file.png`
- ✅ `/static/sling-cms/thumbnails/folder.png`
- ✅ `/static/sling-cms/thumbnails/page.png`

### Old Pattern (Should NOT Work)
- ❌ `/cms/file/preview.html/content/dam/image.jpg.transform/auto-rotate-thumbnail.png`

---

## Deployment

Deploy the updated Core module:
```bash
mvn clean install -P autoInstallBundle -pl core -DskipTests -Dbnd.baseline.skip=true
```

Or use VS Code task:
- `Ctrl+Shift+B` → "Auto Deploy - Core"

---

## Related Issues to Fix

### ContentGridModel.java Also Has Wrong URLs
**File:** `core/src/main/java/org/apache/sling/cms/core/models/ContentGridModel.java`

**Lines with incorrect pattern:**
- Line 221: `return "/cms/file/preview.html" + getPath() + ".transform/auto-rotate-thumbnail.png";`
- Line 223: `return "/cms/file/preview.html" + getPath() + ".transform/sling-cms-thumbnail.png";`
- Line 225: `return "/cms/file/preview.html" + getBrandingGridIconsBase() + "/site.png";`
- Line 227: `return "/cms/file/preview.html" + getBrandingGridIconsBase() + "/folder.png";`
- Line 235: `return "/cms/file/preview.html" + template + "/thumbnail.transform/sling-cms-thumbnail.png";`
- Line 239: `return "/cms/file/preview.html" + getBrandingGridIconsBase() + "/page.png";`
- Line 241: `return "/cms/file/preview.html" + getBrandingGridIconsBase() + "/file.png";`

**Action:** Create a separate PR to fix ContentGridModel.java with the same pattern.

---

## Summary

✅ **Fixed AssetCard.java** to use correct transformation and static icon URLs  
✅ **Removed unnecessary prefix** `/cms/file/preview.html` from transformation URLs  
✅ **Updated branding path** from `/mnt/overlay/sling-cms/content/branding` to `/conf/global/sling-cms/branding`  
✅ **Fixed static icon path** from `/static/sling-cms/img/icons/` to `/static/sling-cms/thumbnails/`  
⚠️ **ContentGridModel.java still needs fixing** - same pattern issues

The asset card component will now generate correct thumbnail URLs that work with the TransformServlet!
