# SPDX-License-Identifier: Apache-2.0

# Move/Copy Module Documentation

## Overview

The `cms.movecopy.js` module provides centralized functionality for move and copy operations in Apache Sling CMS. It handles form validation, destination path normalization, and provides a foundation for drag-and-drop functionality.

## Purpose

This module was created to:
1. **Fix the "precondition failed" error** - Ensures destination paths comply with Sling POST servlet requirements
2. **Centralize move/copy logic** - Single source of truth for all move/copy operations
3. **Enable future enhancements** - Provides hooks for drag-and-drop functionality
4. **Maintain clean architecture** - Separates concerns from general form handling

## Core Functionality

### 1. Destination Path Normalization

**Problem**: Sling's POST servlet requires destination paths to end with `/` when moving/copying to a parent directory.

**Solution**: The module automatically normalizes paths:

```javascript
// Input: "/content/mysite/pages"
// Output: "/content/mysite/pages/"
Sling.CMS.MoveCopy.normalizeDestinationPath(destPath);
```

### 2. Form Validation

Automatically validates and fixes move/copy forms before submission:

```javascript
// Called automatically by cms.form.js
Sling.CMS.MoveCopy.validateAndFixForm(form, formData);
```

### 3. Programmatic Move/Copy

Perform move/copy operations programmatically:

```javascript
// Move a page
await Sling.CMS.MoveCopy.performOperation(
  '/content/mysite/pages/oldpage',
  '/content/mysite/pages/newfolder',
  'move',
  true  // update references
);

// Copy an asset
await Sling.CMS.MoveCopy.performOperation(
  '/content/dam/image.jpg',
  '/content/dam/newfolder',
  'copy',
  false
);
```

## Drag-and-Drop Support (Future Enhancement)

The module includes a `dragDrop` object with methods ready for implementation:

### Making Elements Draggable

```javascript
const element = document.querySelector('.content-item');
const resourcePath = '/content/mysite/pages/mypage';
Sling.CMS.MoveCopy.dragDrop.makeDraggable(element, resourcePath);
```

### Making Elements Drop Targets

```javascript
const folder = document.querySelector('.folder-item');
const targetPath = '/content/mysite/pages/folder';

Sling.CMS.MoveCopy.dragDrop.makeDropTarget(folder, targetPath,
  async (sourcePath, targetPath, operation) => {
    // Handle the drop
    const response = await Sling.CMS.MoveCopy.performOperation(
      sourcePath,
      targetPath,
      operation,
      false
    );

    if (Sling.CMS.utils.ok(response)) {
      // Reload the view
      Sling.CMS.ui.reloadContext();
    }
  }
);
```

## SCSS Styling

The module comes with dedicated styles in `_movecopy.scss`:

### Available CSS Classes

- `.is-dragging` - Applied to elements being dragged
- `.is-drop-target` - Applied to valid drop targets during drag
- `.movecopy-modal` - Styles for move/copy modal dialogs
- `.drag-ghost` - Custom drag ghost/placeholder

### Drag-and-Drop Visual Feedback

```scss
// Draggable state
.is-dragging {
  opacity: 0.5;
  cursor: move;
}

// Drop target highlight
.is-drop-target {
  background-color: rgba(72, 95, 199, 0.1);
  border: 2px dashed #485fc7;
}
```

## Integration with Existing Code

### In cms.form.js

The module is integrated into form submission:

```javascript
// Before form submission
if (window.Sling.CMS.MoveCopy) {
  window.Sling.CMS.MoveCopy.validateAndFixForm(form, formData);
}
```

### Global Availability

The module is available globally:

```javascript
// Access anywhere in the application
window.Sling.CMS.MoveCopy.normalizeDestinationPath(path);
```

## Future Enhancements

### Implementing Drag-and-Drop for Content Tables

```javascript
// Initialize drag-and-drop for a content table
rava.bind('.content-table', {
  callbacks: {
    created() {
      const table = this;

      // Make rows draggable
      table.querySelectorAll('tr[data-path]').forEach(row => {
        const path = row.dataset.path;
        Sling.CMS.MoveCopy.dragDrop.makeDraggable(row, path);
      });

      // Make folder rows drop targets
      table.querySelectorAll('tr[data-type="folder"]').forEach(row => {
        const path = row.dataset.path;
        Sling.CMS.MoveCopy.dragDrop.makeDropTarget(row, path,
          async (sourcePath, targetPath, operation) => {
            const response = await Sling.CMS.MoveCopy.performOperation(
              sourcePath, targetPath, operation, true
            );

            if (Sling.CMS.utils.ok(response)) {
              Sling.CMS.ui.reloadContext();
            }
          }
        );
      });
    }
  }
});
```

### Keyboard Modifiers

The drag-and-drop implementation supports keyboard modifiers:

- **Default (no modifier)**: Move operation
- **Ctrl/Cmd + Drag**: Copy operation

## Testing

### Test Move Operation

1. Navigate to any content item in CMS
2. Click "Move/Copy" action
3. Enter destination: `/content/mysite/pages` (without trailing slash)
4. Select "Move" operation
5. Submit - the path is automatically corrected to `/content/mysite/pages/`

### Test Copy Operation

Same as above, but select "Copy" operation

### Test Programmatic API

Open browser console:

```javascript
// Test path normalization
Sling.CMS.MoveCopy.normalizeDestinationPath('/content/test')
// Returns: '/content/test/'

// Test move operation (replace with actual paths)
await Sling.CMS.MoveCopy.performOperation(
  '/content/mysite/pages/test',
  '/content/mysite/pages/',
  'move',
  false
);
```

## Troubleshooting

### "MoveCopy is undefined" Error

**Cause**: Module not loaded before usage

**Solution**: Ensure `cms.movecopy.js` is imported before `cms.form.js` in `cms-entry.js`

### Drag-and-Drop Not Working

**Cause**: Drag-and-drop initialization not called

**Solution**: Call `makeDraggable()` and `makeDropTarget()` after DOM is ready

### Path Still Missing Trailing Slash

**Cause**: Module loaded after form submission

**Solution**: Check import order in `cms-entry.js` - `movecopy` should come before `form`

## Architecture Decisions

### Why a Separate Module?

1. **Single Responsibility** - Form handling should not include move/copy business logic
2. **Reusability** - Logic can be used by forms, drag-and-drop, and API calls
3. **Testability** - Easier to test in isolation
4. **Future-Proof** - Prepared for drag-and-drop without modifying existing code

### Why Not Use Existing Draggable Module?

The existing `cms.draggable.js` handles modal dragging (window movement), not content item dragging. Move/copy drag-and-drop requires:
- Data transfer (resource paths)
- Drop target validation
- Operation type detection (move vs copy)
- Integration with Sling POST servlet

This is fundamentally different from window dragging.

## Files Modified

1. **Created**: `frontend/src/main/frontend/js/cms.movecopy.js` - Main module
2. **Created**: `frontend/src/main/frontend/scss/_movecopy.scss` - Styles
3. **Modified**: `frontend/src/main/frontend/js/cms.form.js` - Integration
4. **Modified**: `frontend/src/main/frontend/js/cms-entry.js` - Import
5. **Modified**: `frontend/src/main/frontend/scss/cms.scss` - Import styles
6. **Modified**: `frontend/pom.xml` - Fixed npm ci to include devDependencies

## Related Documentation

- [JSP to HTL Migration](../../../docs/jsp-to-htl-migration.md)
- [Frontend Development](../../../docs/developers.md#frontend-development)
- [Apache Sling POST Servlet](https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html)
