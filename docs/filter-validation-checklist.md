# Filter Validation Checklist

## Overview
This document provides a comprehensive checklist to validate that all filtering functionality works correctly across table and grid views for both content and assets.

## Filter System Architecture

### Data Flow
1. **Filter Component** (`contentfilter.html` / `assetfilter.html`) 
   - Renders dropdowns with `data-filter="attribute-name"`
   - Values populate from `ContentFilter` model

2. **Filterable Items** (Grid/Table rows)
   - Must have `data-filterable-item` attribute
   - Must have corresponding `data-{attribute-name}` attributes matching filter

3. **JavaScript** (`cms.contentfilter.js`)
   - Looks for `[data-filterable-item]` elements
   - Reads `data-filter` from dropdown
   - Matches against item's `data-{attribute}` values
   - Shows/hides items based on match

## Validation Checklist

### ✅ Content Grid (`/libs/sling-cms/components/cms/contentgrid`)

**HTML Template** (`contentgrid.html`):
- [x] Items have `data-filterable-item` attribute
- [x] Items have `data-resource` (path)
- [x] Items have `data-type` (type path)
- [x] Items have `data-mime-type` (mime type)
- [x] Items have `data-is-folder` (boolean)
- [x] Items have `data-taxonomy` (comma-separated tags)
- [x] Items have `data-page-status` (published/draft)
- [x] Items have `data-template` (template name)
- [x] Items have `data-modified-date` (today/week/month/year/older)

**Java Model** (`ContentGridModel.java`):
- [x] `GridItem.getTypePath()` - Returns type config path
- [x] `GridItem.getPageStatus()` - Returns "published" or "draft"
- [x] `GridItem.getTemplate()` - Extracts template name
- [x] `GridItem.getModifiedDate()` - Returns date category

### ✅ Content Table (`/libs/sling-cms/components/cms/contenttable`)

**HTML Template** (`contenttable.html`):
- [x] Rows have `data-filterable-item` attribute
- [x] Rows have all necessary data attributes (already working)

**Java Model** (`ContentTableModel.java`):
- [x] All methods already exist (reference implementation)

### ✅ Asset Grid (`/libs/sling-cms/components/cms/assetgrid`)

**HTML Template** (`assetgrid.html`):
- [x] Items have `data-filterable-item` attribute
- [x] Items have `data-asset-item` attribute (fallback pattern)
- [x] Items have `data-name` (lowercase name)
- [x] Items have `data-mime-type` (mime type)
- [x] Items have `data-asset-type` (mime type for filtering)
- [x] Items have `data-asset-size` (0-100, 100-1024, 1024-10240, 10240-)
- [x] Items have `data-modified-date` (today/week/month/year/older)
- [x] Items have `data-is-folder` (boolean)
- [x] Items have `data-taxonomy` (comma-separated tags)

**Java Model** (`AssetGridModel.java`):
- [x] `AssetItem.getAssetSize()` - Returns size category
- [x] `AssetItem.getModifiedDate()` - Returns date category

### ✅ Content Filter Component (`/libs/sling-cms/components/cms/contentfilter`)

**HTML Template** (`contentfilter.html`):
- [x] Uses `data-component="content-filter"`
- [x] Dynamically renders filters from model
- [x] Each filter has `data-filter="{dataAttribute}"`
- [x] Supports dynamic option loading with `data-dynamic-options`

**Java Model** (`ContentFilter.java`):
- [x] `initContentFilters()` - Sets `mime-type` as data attribute
- [x] `initSitesFilters()` - Defines page-status, template, modified-date filters
- [x] `initAssetFilters()` - Already correct (asset-type, asset-size, modified-date)
- [x] `getFilterConfigs()` - Returns list of FilterConfig objects

### ✅ Asset Filter Component (`/libs/sling-cms/components/cms/assetfilter`)

**HTML Template** (`assetfilter.html`):
- [x] Uses `data-component="content-filter"`  
- [x] Has `data-filter="asset-type"`
- [x] Has `data-filter="asset-size"`
- [x] Has `data-filter="modified-date"`
- [x] Filter values match data attribute values exactly

## Test Scenarios

### 1. Content Grid Filtering (Sites Mode)

**Setup:**
- Navigate to a page with content grid view
- Ensure `filterMode="sites"` is set on filter component

**Test Cases:**
- [ ] **Page Status Filter**
  - Select "Published" → Only items with `data-page-status="published"` show
  - Select "Draft" → Only items with `data-page-status="draft"` show
  - Select "All Pages" → All items show

- [ ] **Template Filter**
  - Select a template → Only items with matching `data-template` show
  - Select "All Templates" → All items show

- [ ] **Modified Date Filter**
  - Select "Today" → Only items with `data-modified-date="today"` show
  - Select "This Week" → Items with `today` or `week` show
  - Select "This Month" → Items with `today`, `week`, or `month` show

### 2. Content Table Filtering

**Setup:**
- Navigate to a page with content table view
- Ensure filter component is present

**Test Cases:**
- [ ] **Type Filter**
  - Select "Images" → Only items with `data-mime-type` starting with "image/" show
  - Select "Folders" → Only items with `data-is-folder="true"` show

- [ ] **Combined Filters**
  - Select "Images" AND "Published" → Only published images show
  - Clear filters → All items show

### 3. Asset Grid Filtering

**Setup:**
- Navigate to asset browser with grid view
- Ensure assetfilter component is present

**Test Cases:**
- [ ] **Asset Type Filter**
  - Select "Images" → Only items with `data-asset-type` starting with "image/" show
  - Select "Videos" → Only items with `data-asset-type` starting with "video/" show
  - Select "PDF Documents" → Only PDFs show

- [ ] **Asset Size Filter**
  - Select "< 100 KB" → Only items with `data-asset-size="0-100"` show
  - Select "1 MB - 10 MB" → Only items with `data-asset-size="1024-10240"` show

- [ ] **Modified Date Filter**  
  - Select "Today" → Only items with `data-modified-date="today"` show
  - Select "This Week" → Items with `today` or `week` show

- [ ] **Combined Filters**
  - Select "Images" AND "< 100 KB" → Only small images show
  - Select "Videos" AND "This Month" → Only recent videos show

### 4. JavaScript Validation

**Browser Console Tests:**
```javascript
// Check if content filter is initialized
document.querySelector('[data-component="content-filter"]')

// Check if filterable items exist
document.querySelectorAll('[data-filterable-item]').length

// Check if filter dropdown has correct attribute
document.querySelector('[data-filter="asset-type"]')

// Manually trigger filter
const filter = document.querySelector('[data-filter="asset-type"]');
filter.value = 'image/';
filter.dispatchEvent(new Event('change'));

// Check filtered results
document.querySelectorAll('[data-filterable-item]:not(.cms-filtered-out)').length
```

## Attribute Mapping Reference

### Content Filters (Sites Mode)
| Filter Name | `data-filter` Value | Item Attribute | Values |
|------------|-------------------|----------------|--------|
| Page Status | `page-status` | `data-page-status` | published, draft |
| Template | `template` | `data-template` | template name |
| Modified Date | `modified-date` | `data-modified-date` | today, week, month, year, older |

### Content Filters (Content Mode)
| Filter Name | `data-filter` Value | Item Attribute | Values |
|------------|-------------------|----------------|--------|
| Type | `mime-type` | `data-mime-type` | image/, video/, application/pdf, folder |

### Asset Filters
| Filter Name | `data-filter` Value | Item Attribute | Values |
|------------|-------------------|----------------|--------|
| Asset Type | `asset-type` | `data-asset-type` | image/, video/, audio/, application/pdf, etc. |
| Asset Size | `asset-size` | `data-asset-size` | 0-100, 100-1024, 1024-10240, 10240- |
| Modified Date | `modified-date` | `data-modified-date` | today, week, month, year, older |

## Common Issues & Solutions

### Issue: Filters not working in grid view
**Symptoms:** Dropdowns appear but selecting options doesn't filter items

**Solution:**
1. Check items have `data-filterable-item` attribute
2. Verify data attributes match filter's `data-filter` value
3. Check browser console for JavaScript errors
4. Verify `cms.contentfilter.js` is loaded

### Issue: Wrong items filtered
**Symptoms:** Selecting "Images" filters videos or vice versa

**Solution:**
1. Check `data-asset-type` or `data-mime-type` values are correct
2. Verify filter options use correct value patterns (e.g., "image/" not "image")
3. Check model methods return expected values

### Issue: Filters work in table but not grid
**Symptoms:** Table view filters correctly, grid view doesn't

**Solution:**
1. Ensure grid items have all required data attributes
2. Check grid HTML uses same attribute names as table
3. Verify both use `data-filterable-item` attribute

## Deployment Checklist

Before marking filters as complete:
- [ ] Build and deploy Core module: `mvn clean install -P autoInstallBundle -pl core`
- [ ] Build and deploy UI module: `mvn clean install -P autoInstallBundle -pl ui`
- [ ] Clear browser cache and reload
- [ ] Test all scenarios above
- [ ] Check browser console for errors
- [ ] Verify filter counts update correctly
- [ ] Test on different resource types (pages, assets, folders)

## Browser Testing Matrix

| Browser | Version | Status |
|---------|---------|--------|
| Chrome | Latest | ⏳ Pending |
| Firefox | Latest | ⏳ Pending |
| Safari | Latest | ⏳ Pending |
| Edge | Latest | ⏳ Pending |

## Performance Notes

- Filters work client-side, no server requests
- Large result sets (1000+ items) may cause lag
- Consider pagination for better UX with large datasets
- Filter counts update on every filter change

## Files Modified

### Core Module
1. `core/.../ContentGridModel.java` - Added filter methods
2. `core/.../AssetGridModel.java` - Added filter methods  
3. `core/.../ContentFilter.java` - Fixed data attribute for content filters

### UI Module
1. `ui/.../contentgrid/contentgrid.html` - Added data attributes
2. `ui/.../assetgrid/assetgrid.html` - Added data attributes
3. `ui/.../contentfilter/contentfilter.html` - Refactored to use model

## Next Steps

1. ✅ Deploy all changes
2. ⏳ Run manual test scenarios
3. ⏳ Document any issues found
4. ⏳ Create automated tests (optional)
5. ⏳ Update user documentation
