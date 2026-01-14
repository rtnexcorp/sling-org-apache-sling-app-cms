# Asset Filter - JavaScript Implementation

## Current Implementation

The asset filter functionality is already fully implemented in Apache Sling CMS. Here's an overview:

## File Locations

### JavaScript
- **Main Module**: `frontend/src/main/frontend/js/cms.assetbrowser.js`
- **Related**: `frontend/src/main/frontend/js/cms.contentfilter.js` (generic content filtering)

### Styles
- **Main Styles**: `frontend/src/main/frontend/scss/_assets.scss`

## Features Implemented

### 1. Search Filtering
- Real-time search with 300ms debounce
- Searches asset names (case-insensitive)
- Updates count display automatically

```javascript
// Search functionality (already implemented)
searchInput.addEventListener('input', (e) => {
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    this.filterAssets(browser);
  }, 300);
});
```

### 2. MIME Type Filtering
- Filter by file type (images, videos, documents, etc.)
- Supports multiple MIME type prefixes (e.g., "image/", "video/")
- Comma-separated filter values

```javascript
// Type filter match
let matchesType = true;
if (typeFilters.length > 0 && !isFolder) {
  matchesType = typeFilters.some(filter => mimeType.startsWith(filter));
}
```

### 3. Taxonomy/Tag Filtering
- Filter by taxonomy tags
- Supports hierarchical tag matching
- Shows assets with matching or child tags

```javascript
// Tag filter match
if (tagFilterValue && !isFolder) {
  const taxonomyPaths = taxonomy.split(',').filter(t => t.trim());
  matchesTag = taxonomyPaths.some(path => 
    path === tagFilterValue || path.startsWith(tagFilterValue + '/')
  );
}
```

### 4. View Toggle
- Grid view (default)
- List view (detailed)
- Preference saved to localStorage
- Automatic restoration on page load

```javascript
// Store preference
localStorage.setItem('sling-cms-asset-view', view);
```

### 5. Empty State Handling
- Shows/hides empty state message
- Updates visible item count
- Dynamic UI feedback

## Data Attributes Required

For assets to be filterable, the HTML must include these data attributes:

```html
<div class="asset-item" 
     data-name="example.jpg"
     data-mime-type="image/jpeg"
     data-is-folder="false"
     data-taxonomy="/tags/nature,/tags/landscape">
  <!-- Asset card content -->
</div>
```

## HTML Structure

### Filter Bar
```html
<div class="asset-filter-bar" data-component="asset-browser">
  <div class="asset-filter-bar__search">
    <input type="text" 
           class="input" 
           placeholder="Search assets..." 
           data-asset-search>
  </div>
  
  <div class="asset-filter-bar__filters">
    <select data-asset-type-filter>
      <option value="">All Types</option>
      <option value="image/">Images</option>
      <option value="video/">Videos</option>
      <option value="application/pdf">PDFs</option>
    </select>
    
    <select data-asset-tag-filter>
      <option value="">All Tags</option>
      <!-- Dynamic tag options -->
    </select>
  </div>
  
  <div class="asset-filter-bar__view-toggle">
    <div class="buttons">
      <button class="button asset-view-btn is-selected" data-view="grid">
        <span class="icon"><i class="jam jam-grid"></i></span>
      </button>
      <button class="button asset-view-btn" data-view="list">
        <span class="icon"><i class="jam jam-menu"></i></span>
      </button>
    </div>
  </div>
  
  <div class="asset-filter-bar__count">
    <span data-asset-count>0</span> items
  </div>
</div>
```

### Asset Grid
```html
<div class="asset-grid" data-view="grid">
  <div class="asset-grid__items">
    <!-- Asset items go here -->
  </div>
  
  <div class="asset-grid__empty" style="display: none;">
    <div class="has-text-centered">
      <span class="icon is-large"><i class="jam jam-files"></i></span>
      <p>No assets found matching your filters</p>
    </div>
  </div>
</div>
```

## API Methods

### Initialize All Browsers
```javascript
Sling.CMS.AssetBrowser.init();
```

### Initialize Single Browser
```javascript
const browserElement = document.querySelector('[data-component="asset-browser"]');
Sling.CMS.AssetBrowser.initBrowser(browserElement);
```

### Filter Assets Manually
```javascript
const browserElement = document.querySelector('[data-component="asset-browser"]');
Sling.CMS.AssetBrowser.filterAssets(browserElement);
```

## Events

### Auto-initialization
- On DOMContentLoaded
- On `slingcms:reload` custom event (for AJAX content)

### Filter Updates
- Input event (search) - 300ms debounced
- Change event (dropdowns) - immediate

## MIME Type Filter Examples

Common MIME type filters you can use:

```javascript
const mimeTypeFilters = {
  'All Images': 'image/',
  'JPEG Images': 'image/jpeg',
  'PNG Images': 'image/png',
  'All Videos': 'video/',
  'MP4 Videos': 'video/mp4',
  'All Documents': 'application/',
  'PDFs': 'application/pdf',
  'Word Documents': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'Excel Spreadsheets': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'All Audio': 'audio/',
  'All Text': 'text/'
};
```

## Styling

The asset filter uses semantic CSS classes that abstract the underlying Bulma framework:

```scss
// Custom classes following project guidelines
.asset-filter-bar { }         // Filter container
.asset-filter-bar__search { } // Search input wrapper
.asset-filter-bar__filters { } // Filter controls wrapper
.asset-filter-bar__count { }  // Item count display
.asset-view-btn { }           // View toggle button
.asset-grid { }               // Grid container
.asset-item { }               // Individual asset card
```

## Potential Enhancements

While the current implementation is comprehensive, here are potential enhancements:

### 1. Advanced Filters
```javascript
// Add date range filter
filterByDateRange: function(from, to) {
  // Filter by upload date or modified date
}

// Add size filter
filterBySize: function(minSize, maxSize) {
  // Filter by file size
}
```

### 2. URL Persistence
```javascript
// Save filter state to URL query params
updateURLWithFilters: function(filters) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value) params.set(key, value);
  });
  window.history.replaceState({}, '', `?${params}`);
}
```

### 3. Batch Operations
```javascript
// Select multiple assets with checkboxes
selectAsset: function(assetElement, selected) {
  assetElement.classList.toggle('is-selected', selected);
  this.updateBatchActions();
}
```

### 4. Sort Options
```javascript
// Add sorting functionality
sortAssets: function(sortBy, direction) {
  // Sort by name, date, size, type
}
```

## Testing

To test the asset filter:

1. Navigate to `/cms/content.html/content/starter/dam` (or any asset folder)
2. Use the search box to filter by name
3. Use type filter dropdown to filter by MIME type
4. Use tag filter to filter by taxonomy
5. Toggle between grid and list view
6. Verify count updates correctly
7. Verify empty state shows when no matches

## Browser Compatibility

The implementation uses:
- `querySelector/querySelectorAll` - Widely supported
- `dataset` API - IE11+
- `localStorage` - IE8+
- Arrow functions - ES6 (transpiled if needed)
- `forEach` on NodeList - Modern browsers or polyfilled

## Performance Considerations

- **Debouncing**: Search has 300ms debounce to prevent excessive filtering
- **Direct DOM manipulation**: Uses style.display for fast show/hide
- **Lazy loading**: Images use `loading="lazy"` attribute
- **Event delegation**: Uses efficient event handling patterns

## Integration with Other Modules

The asset filter integrates with:

1. **ContentFilter** (`cms.contentfilter.js`) - Generic filtering framework
2. **Navigation** (`cms.nav.js`) - Content navigation search
3. **PathBrowser** (`cms.pathbrowser.js`) - Asset selection dialogs

## Conclusion

The asset filter JavaScript is **fully implemented and production-ready**. It provides comprehensive filtering capabilities with:

- ✅ Real-time search
- ✅ MIME type filtering
- ✅ Taxonomy/tag filtering
- ✅ View toggling (grid/list)
- ✅ Count updates
- ✅ Empty state handling
- ✅ LocalStorage persistence
- ✅ Event-driven architecture
- ✅ Framework-agnostic design

No additional JavaScript development is needed unless you want to add the enhancement features listed above.
