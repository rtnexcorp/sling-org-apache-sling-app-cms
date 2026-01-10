# Filter Mode "assets" Fix

## Problem

When `filterMode="assets"` was configured in contentactions component (e.g., in `/libs/sling-cms/content/static/content.json`), the old separate `assetfilter` component was being used instead of the new unified `contentfilter` component.

### Impact

- Asset filters in grid view were not working because the old `assetfilter.html` used hardcoded HTML with old data attributes
- The unified `contentfilter` component (which has proper data-filter attributes) was being bypassed
- This created inconsistency between table view (which worked) and grid view (which didn't)

## Root Cause

**File**: `ui/src/main/resources/jcr_root/libs/sling-cms/components/cms/contentactions/contentactions.html`

The contentactions component had separate conditions for each filter mode:

```html
<!-- OLD CODE - BEFORE FIX -->
<sly data-sly-test="${properties.filterMode == 'workflow'}">
    <sly data-sly-resource="${'workflowfilter' @ resourceType='sling-cms/components/cms/workflowfilter'}"></sly>
</sly>
<sly data-sly-test="${properties.filterMode == 'assets'}">
    <sly data-sly-resource="${'assetfilter' @ resourceType='sling-cms/components/cms/assetfilter'}"></sly>
</sly>
<sly data-sly-test="${properties.filterMode == 'sites'}">
    <sly data-sly-resource="${'sitefilter' @ resourceType='sling-cms/components/cms/sitefilter'}"></sly>
</sly>
<sly data-sly-test="${properties.filterMode == 'content'}">
    <sly data-sly-resource="${'contentfilter' @ resourceType='sling-cms/components/cms/contentfilter'}"></sly>
</sly>
```

This meant:
- `filterMode="assets"` → Used old `assetfilter` component ❌
- `filterMode="content"` → Used new `contentfilter` component ✅

## Solution

**Unified all filter modes to use the `contentfilter` component**, which is already designed to handle multiple modes via the `ContentFilter.java` model:

```html
<!-- NEW CODE - AFTER FIX -->
<!--/* Content Filter - Unified component for all modes */-->
<sly data-sly-test="${properties.filterMode}">
    <div class="level-item">
        <sly data-sly-resource="${'contentfilter' @ resourceType='sling-cms/components/cms/contentfilter', requestAttributes=properties}"></sly>
    </div>
</sly>
```

### How It Works

1. **ContentFilter.java** reads `filterMode` property from resource/request
2. Based on the mode, it initializes appropriate filters:
   - `filterMode="assets"` → Calls `initAssetFilters()`
   - `filterMode="sites"` → Calls `initSitesFilters()`
   - `filterMode="workflow"` → Calls `initWorkflowFilters()`
   - `filterMode="content"` or default → Calls `initContentFilters()`

3. **contentfilter.html** dynamically renders filters using `data-filter="${filter.dataAttribute}"`

4. **cms.contentfilter.js** reads these `data-filter` attributes and applies filtering

## Benefits of Unified Approach

✅ **Single source of truth**: All filter logic in one component (contentfilter)  
✅ **Consistent data attributes**: All filters use proper `data-filter="${attribute}"` format  
✅ **Easier maintenance**: Update filter logic in one place (ContentFilter.java)  
✅ **Grid view now works**: Asset grid items have matching data attributes  
✅ **Deprecation path**: Old assetfilter/sitefilter components can be deprecated

## Files Modified

1. **contentactions.html** - Changed to use unified contentfilter component
2. **AssetGridModel.java** - Added filter support methods (getAssetSize, getModifiedDate)
3. **assetgrid.html** - Added data attributes for filtering (data-filterable-item, data-asset-type, etc.)

## Deprecated Components

The following components should now be considered **deprecated** (can be removed in future):
- `/libs/sling-cms/components/cms/assetfilter/` - Replaced by contentfilter with mode="assets"
- `/libs/sling-cms/components/cms/sitefilter/` - Replaced by contentfilter with mode="sites"
- `/libs/sling-cms/components/cms/workflowfilter/` - Replaced by contentfilter with mode="workflow"

## Testing

Test at: http://localhost:8082/cms/static/content.html/static/hello

### Verify Asset Filters Work in Grid View

1. Switch to Grid View (if not already)
2. Use "Filter by Type" dropdown:
   - Select "Images" → Only image assets should show
   - Select "Videos" → Only video assets should show
   - Select "PDF Documents" → Only PDF files should show
3. Use "Filter by Size" dropdown:
   - Select "< 100 KB" → Only small files show
   - Select "1 MB - 10 MB" → Only medium files show
4. Use "Modified Date" dropdown:
   - Select "Today" → Only today's files show

### Browser Console Test

```javascript
// Check filter component exists
document.querySelector('[data-component="content-filter"]')

// Check asset grid items have proper attributes
document.querySelectorAll('[data-asset-type]').forEach(item => {
  console.log('Type:', item.dataset.assetType, 
              'Size:', item.dataset.assetSize,
              'Modified:', item.dataset.modifiedDate);
});

// Test filter selection
const typeFilter = document.querySelector('[data-filter="asset-type"]');
if (typeFilter) {
  typeFilter.value = 'image/';
  typeFilter.dispatchEvent(new Event('change'));
  console.log('Visible items:', 
    document.querySelectorAll('[data-filterable-item]:not(.cms-filtered-out)').length);
}
```

## Migration Notes

For any custom implementations using the old filter components:

**Before:**
```json
{
  "contentactions": {
    "sling:resourceType": "sling-cms/components/cms/contentactions",
    "filterMode": "assets"
  }
}
```

This would include the old `assetfilter` component.

**After:**
```json
{
  "contentactions": {
    "sling:resourceType": "sling-cms/components/cms/contentactions",
    "filterMode": "assets"
  }
}
```

Same configuration now uses the unified `contentfilter` component automatically! ✅

## Related Documentation

- [Filter Validation Checklist](filter-validation-checklist.md)
- [Filter Attribute Mapping](filter-attribute-mapping-test.md)
- [Content Filter Component](contentfilter-component.md)
- [Content Filter Usage](contentfilter-usage.md)
