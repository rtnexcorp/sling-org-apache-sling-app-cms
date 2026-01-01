# SPDX-License-Identifier: Apache-2.0

# Path Browser Field Component

## Overview

The **Path Browser** is a visual folder/file picker component inspired by macOS Finder. It provides a one-step, user-friendly interface for selecting paths in Apache Sling CMS, replacing traditional text-based path entry with a browseable tree/list view.

## Features

- ✅ **macOS Finder-like UI** - Visual folder browser with familiar interface
- ✅ **Single-click selection** - Select folders/pages with one click
- ✅ **Double-click navigation** - Navigate into folders by double-clicking
- ✅ **Breadcrumb navigation** - Quick navigation through folder hierarchy
- ✅ **Search/filter** - Real-time filtering of visible items
- ✅ **Type filtering** - Show only folders, pages, or assets based on configuration
- ✅ **Base path** - Start browsing from a specific location
- ✅ **Responsive** - Works on desktop and mobile devices
- ✅ **Keyboard accessible** - Fully accessible interface

## Usage in Component Definitions

### Basic Usage (Folder Selection)

```json
{
    "jcr:primaryType": "nt:unstructured",
    "sling:resourceType": "sling-cms/components/editor/fields/pathbrowser",
    "label": "Select Folder:",
    "name": "folderPath",
    "type": "sling:Folder,sling:OrderedFolder,nt:folder",
    "basePath": "/content",
    "required": true
}
```

### Page Selection

```json
{
    "sling:resourceType": "sling-cms/components/editor/fields/pathbrowser",
    "label": "Select Page:",
    "name": "pagePath",
    "type": "sling:Page",
    "basePath": "/content/mysite",
    "required": false
}
```

### Asset Selection

```json
{
    "sling:resourceType": "sling-cms/components/editor/fields/pathbrowser",
    "label": "Select Image:",
    "name": "imagePath",
    "type": "sling:File,nt:file",
    "basePath": "/content/dam",
    "required": true
}
```

### Mixed Types

```json
{
    "sling:resourceType": "sling-cms/components/editor/fields/pathbrowser",
    "label": "Select Content:",
    "name": "contentPath",
    "type": "sling:Page,sling:Folder",
    "basePath": "/content"
}
```

## Configuration Properties

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `name` | String | Yes | - | Form field name (will be submitted) |
| `label` | String | No | - | Field label displayed to user |
| `type` | String | No | (all types) | Comma-separated list of JCR node types to filter (e.g., "sling:Folder,nt:folder") |
| `basePath` | String | No | "/content" | Starting path for browsing |
| `required` | Boolean | No | false | Whether field is required |
| `value` | String | No | - | Initial/default value |

## Supported Node Types

Common node types for filtering:

### Folders
- `sling:Folder` - Sling folder
- `sling:OrderedFolder` - Ordered Sling folder
- `nt:folder` - JCR folder

### Pages
- `sling:Page` - Sling page

### Files/Assets
- `sling:File` - Sling file
- `nt:file` - JCR file

### Multiple Types
Use comma-separated list: `"sling:Folder,sling:Page,sling:File"`

## User Interface

### Components

1. **Selected Path Display**
   - Shows currently selected path
   - Folder icon indicator
   - Click to open browser

2. **Browser Panel**
   - Breadcrumb navigation
   - Search/filter input
   - Scrollable item list
   - Action buttons (Cancel/Select)

3. **Item List**
   - Folders shown with folder icon
   - Files shown with document icon
   - Type labels (e.g., "Folder", "Page")
   - Single-click to select
   - Double-click to navigate (containers only)

### User Interactions

| Action | Result |
|--------|--------|
| Click dropdown button | Opens browser panel |
| Single-click item | Selects the item |
| Double-click folder | Navigates into folder |
| Click breadcrumb | Navigates to that level |
| Type in search | Filters visible items |
| Click Select button | Confirms selection and closes |
| Click Cancel | Closes without changing selection |

## JavaScript API

The pathbrowser is initialized automatically via `cms.pathbrowser.js`.

### Events

The component uses standard form events:

```javascript
// Listen for value changes
const input = document.querySelector('input[name="folderPath"]');
input.addEventListener('change', (e) => {
  console.log('Selected path:', e.target.value);
});
```

## Styling

Styles are defined in `frontend/src/main/frontend/scss/_pathbrowser.scss`.

### CSS Classes

| Class | Purpose |
|-------|---------|
| `.pathbrowser-container` | Main container |
| `.pathbrowser-selected` | Selected path display |
| `.pathbrowser-panel` | Dropdown browser panel |
| `.pathbrowser-breadcrumb` | Breadcrumb navigation |
| `.pathbrowser-item` | Individual item in list |
| `.pathbrowser-item.is-selected` | Currently selected item |
| `.pathbrowser-item.is-container` | Navigable container item |

### Customization

To customize styling, override the SCSS variables or classes:

```scss
// Custom folder icon color
.pathbrowser-item .jam-folder {
  color: #your-color;
}

// Custom selection highlight
.pathbrowser-item.is-selected {
  background-color: #your-bg-color;
  border-left-color: #your-accent-color;
}
```

## Example: Move/Copy Dialog

The pathbrowser is used in the Move/Copy dialog:

**File**: `ui/src/main/resources/jcr_root/libs/sling-cms/content/shared/movecopy.json`

```json
{
    "destination": {
        "jcr:primaryType": "nt:unstructured",
        "sling:resourceType": "sling-cms/components/editor/fields/pathbrowser",
        "label": "Destination Folder:",
        "name": ":dest",
        "type": "sling:Folder,sling:OrderedFolder,nt:folder",
        "basePath": "/content",
        "required": true
    }
}
```

## Technical Implementation

### HTL Template
- **File**: `pathbrowser.html`
- Uses Sling Model `FieldProps` for configuration
- Renders hidden input + visual browser UI
- Accessible markup with ARIA labels

### JavaScript Module
- **File**: `cms.pathbrowser.js`
- Binds to `.pathbrowser-container` elements
- Fetches children via indexed `/bin/cms/paths` endpoint for performance
- Falls back to `.json` for individual node details
- Handles navigation, selection, filtering

### Sling Model
- **File**: `core/src/main/java/org/apache/sling/cms/core/models/FieldProps.java`
- Provides field properties to HTL
- Supports injection of common field attributes

### Styles
- **File**: `_pathbrowser.scss`
- Finder-like visual design
- Responsive layout
- Hover/selection states

## Browser Compatibility

- ✅ Modern browsers (Chrome, Firefox, Safari, Edge)
- ✅ Responsive design (mobile-friendly)
- ✅ Keyboard navigation support
- ✅ Screen reader compatible

## Performance

- **Lazy loading**: Only loads children when folder is opened
- **Client-side filtering**: Fast search without server requests
- **Efficient rendering**: Minimal DOM updates

## Accessibility

- ARIA labels and roles
- Keyboard navigation support
- Focus management
- Screen reader announcements

## Troubleshooting

### Browser panel doesn't open
- Check JavaScript console for errors
- Ensure `cms.pathbrowser.js` is loaded
- Verify `.pathbrowser-container` element exists

### No folders shown
- Check `type` filter configuration
- Verify base path exists and is accessible
- Check browser console for fetch errors
- Ensure `/bin/cms/paths` endpoint is available (PathSuggestionServlet)
- Verify PathSuggestionServlet typeFilters configuration includes needed types

### Selection not working
- Check that hidden input has correct `name` attribute
- Verify form is using proper encoding
- Check JavaScript console for errors

## Future Enhancements

Planned features:
- Tree view mode (hierarchical display)
- Recent selections
- Favorites/bookmarks
- Copy-paste path support
- Upload to folder integration
- New folder creation inline

## Related Components

- **path** - Original text-based path field with autocomplete
- **assetbrowser** - Asset-specific browser
- **movecopy** - Uses pathbrowser for destination selection

## License

Apache License 2.0 - See project LICENSE file
