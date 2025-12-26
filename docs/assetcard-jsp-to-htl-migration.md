# Asset Card JSP to HTL Migration Summary

## Overview
Successfully converted `assetcard.jsp` to HTL (`assetcard.html`) with framework-agnostic styling patterns.

---

## Files Created/Modified

### 1. **Sling Model: `AssetCard.java`**
**Location:** `core/src/main/java/org/apache/sling/cms/core/models/AssetCard.java`

**Purpose:** Provides all business logic and data for the asset card component

**Key Methods:**
- `getTitle()` - Asset title (from jcr:title or filename fallback)
- `getMimeType()` - Asset MIME type (e.g., "image/png", "video/mp4")
- `getFileType()` - File extension from MIME type
- `getPath()` - Asset JCR path
- `getFileSize()` / `getFormattedFileSize()` - File size in bytes and human-readable format
- `getLastModified()` - Formatted last modified date
- `isImage()` / `isVideo()` / `isPdf()` - Type checking methods
- `getThumbnailUrl()` - Primary thumbnail URL (auto-rotate for images)
- `getFallbackThumbnailUrl()` - Fallback thumbnail for error handling
- `getDefaultIconUrl()` - Default file icon for unsupported types
- `getActionConfigs()` - List of action configs for overlay buttons

---

### 2. **HTL Template: `assetcard.html`**
**Location:** `ui/src/main/resources/jcr_root/libs/sling-cms/components/cms/assetcard/assetcard.html`

**Key Changes from JSP:**
- ✅ Uses Sling Model instead of JSTL tags
- ✅ Semantic CSS classes (`cms-asset-card`, `cms-icon--video`)
- ✅ HTL `data-sly-test` instead of `c:choose/c:when`
- ✅ HTL `data-sly-list` instead of `c:forEach`
- ✅ No inline framework classes (Bulma abstracted away)
- ✅ Removed `fn:startsWith()` - replaced with Sling Model methods

**Structure:**
```html
<div class="cms-asset-card">
  <div class="cms-asset-card__image">
    <figure class="cms-asset-figure">
      <!-- Conditional rendering for image/video/pdf/default -->
      <img class="cms-asset-thumbnail">
      <span class="cms-asset-badge">...</span>
    </figure>
    <div class="cms-asset-card__overlay">
      <!-- Action buttons -->
    </div>
  </div>
  <div class="cms-asset-card__content">
    <p class="cms-asset-card__title">...</p>
    <div class="cms-asset-card__meta">
      <span class="cms-tag">...</span>
      <span class="cms-asset-card__size">...</span>
    </div>
    <small class="cms-asset-card__date">...</small>
  </div>
</div>
```

---

### 3. **SCSS Styles: `_assetcard.scss`**
**Location:** `frontend/src/main/frontend/scss/_assetcard.scss`

**Key Features:**
- Framework-agnostic semantic class names
- BEM naming convention (`cms-asset-card__image`, `cms-asset-card--modifier`)
- Hover effects (shadow, transform)
- 5:4 aspect ratio figure for thumbnails
- Overlay with fade-in animation
- Responsive badge positioning
- Tag component styles (reusable)

**Classes Defined:**
- `.cms-asset-card` - Main card container
- `.cms-asset-card__image` - Image container with overlay
- `.cms-asset-card__overlay` - Hover overlay for actions
- `.cms-asset-card__actions` - Action buttons container
- `.cms-asset-card__content` - Card content area
- `.cms-asset-card__title` - Asset title (truncated)
- `.cms-asset-card__meta` - Metadata container (tags, size)
- `.cms-asset-card__size` - File size display
- `.cms-asset-card__date` - Last modified date
- `.cms-asset-figure` - 5:4 aspect ratio figure
- `.cms-asset-thumbnail` - Image with object-fit cover
- `.cms-asset-badge` - Badge for video/PDF indicators
- `.cms-tag` - Reusable tag component

---

### 4. **Icon Addition: `_icons.scss`**
**Added Icon:**
- `cms-icon--video` (unicode `\ec4e` - jam-video-camera)

**Updated File:** `frontend/src/main/frontend/scss/_icons.scss`

---

### 5. **Main SCSS Import: `cms.scss`**
**Added Import:**
```scss
@use "assetcard";
```

---

## Migration Patterns Applied

### ❌ JSP Pattern (Old)
```jsp
<c:set var="mimeType" value="${asset.valueMap['jcr:content/jcr:mimeType']}" />
<c:choose>
    <c:when test="${fn:startsWith(mimeType, 'image/')}">
        <img src="..." class="asset-thumbnail">
    </c:when>
    <c:when test="${fn:startsWith(mimeType, 'video/')}">
        <img src="..." class="asset-thumbnail">
        <span class="asset-badge asset-badge--video">
            <i class="jam jam-video-camera"></i>
        </span>
    </c:when>
</c:choose>
```

### ✅ HTL Pattern (New)
```html
<sly data-sly-use.model="org.apache.sling.cms.core.models.AssetCard">
    <sly data-sly-test="${model.isImage}">
        <img src="${model.thumbnailUrl}" class="cms-asset-thumbnail">
    </sly>
    <sly data-sly-test="${model.isVideo}">
        <img src="${model.thumbnailUrl}" class="cms-asset-thumbnail">
        <span class="cms-asset-badge cms-asset-badge--video">
            <span class="cms-icon cms-icon--video"></span>
        </span>
    </sly>
</sly>
```

---

## Framework Abstraction Benefits

### Before (Bulma Coupling)
```html
<div class="card is-linked">
    <div class="card-image">
        <figure class="image is-5by4">
            <i class="jam jam-video-camera"></i>
        </figure>
    </div>
    <div class="card-content">
        <span class="tag is-light is-small">...</span>
    </div>
</div>
```

### After (Framework-Agnostic)
```html
<div class="cms-asset-card">
    <div class="cms-asset-card__image">
        <figure class="cms-asset-figure">
            <span class="cms-icon cms-icon--video"></span>
        </figure>
    </div>
    <div class="cms-asset-card__content">
        <span class="cms-tag cms-tag--light cms-tag--small">...</span>
    </div>
</div>
```

**Switching CSS frameworks:** Only update `_assetcard.scss`, HTL templates unchanged!

---

## Deployment Instructions

### 1. Deploy Core Module (Sling Model)
```bash
mvn clean install -P autoInstallBundle -pl core -DskipTests -Dbnd.baseline.skip=true
```

### 2. Deploy Frontend Module (SCSS)
```bash
mvn clean install -P autoInstallBundle -pl frontend -DskipTests -Dbnd.baseline.skip=true
```

### 3. Deploy UI Module (HTL Template)
```bash
mvn clean install -P autoInstallBundle -pl ui -DskipTests -Dbnd.baseline.skip=true
```

### Or Use VS Code Tasks:
- `Ctrl+Shift+B` → "Auto Deploy - Core"
- `Ctrl+Shift+B` → "Auto Deploy - Frontend"
- `Ctrl+Shift+B` → "Auto Deploy - UI"

---

## Testing Checklist

After deployment, test the following:

- [ ] Navigate to DAM browser: `http://localhost:8082/cms/dam/content.html`
- [ ] Verify asset cards render correctly
- [ ] Check image thumbnails load with auto-rotate transformation
- [ ] Verify video badge displays with camera icon
- [ ] Verify PDF badge displays
- [ ] Test hover overlay effect (actions appear)
- [ ] Check file size formatting (MB, KB, B)
- [ ] Verify last modified date displays
- [ ] Test action buttons in overlay (if configured)
- [ ] Check responsive layout on mobile breakpoints
- [ ] Verify no console errors

---

## Key Improvements

1. **Separation of Concerns**: Business logic in Sling Model, presentation in HTL
2. **Framework Independence**: Easy to swap Bulma for Bootstrap, Tailwind, etc.
3. **Icon Abstraction**: Semantic icon names, not framework-specific classes
4. **Maintainability**: BEM naming, modular SCSS, clear component structure
5. **Performance**: Lazy loading images, efficient HTL rendering
6. **Standards Compliance**: OSGi R7+, HTL best practices, no deprecated JSP patterns

---

## Related Files to Consider Migrating

Other JSP files in the asset browser that could be migrated next:
- `ui/src/main/resources/jcr_root/libs/sling-cms/components/cms/assetbrowser/*.jsp`
- `ui/src/main/resources/jcr_root/libs/sling-cms/components/cms/assetgrid/*.jsp`
- `ui/src/main/resources/jcr_root/libs/sling-cms/components/cms/assetactions/*.jsp`

Follow the same pattern:
1. Create Sling Model for business logic
2. Convert JSP to HTL with semantic classes
3. Create `_componentname.scss` with framework-agnostic styles
4. Add icon mappings if needed
5. Import SCSS in `cms.scss`
6. Deploy and test
