# Filter Validation Guide for /static/hello

## Test Location
**URL:** http://localhost:8082/cms/static/content.html/static/hello

## Pre-Validation Checklist

### 1. Verify Deployment
- [ ] Core module deployed successfully
- [ ] UI module deployed successfully
- [ ] Clear browser cache (Ctrl+Shift+Delete or Cmd+Shift+Delete)
- [ ] Hard refresh page (Ctrl+F5 or Cmd+Shift+R)

### 2. Check Browser Console
Open Developer Tools (F12) and check for:
```javascript
// Should not have errors
console.log('Checking for errors...')

// Verify content filter is present
document.querySelector('[data-component="content-filter"]')

// Verify filterable items exist
document.querySelectorAll('[data-filterable-item]').length
```

## Validation Steps

### Step 1: Inspect the Filter Component

1. **Open the page:** http://localhost:8082/cms/static/content.html/static/hello

2. **Open Browser DevTools** (F12) → **Elements/Inspector** tab

3. **Find the filter component:**
   - Look for `<div data-component="content-filter">`
   - Verify it has dropdown(s) with `data-filter` attributes

4. **Expected HTML structure:**
   ```html
   <div data-component="content-filter">
     <div class="content-filter__controls">
       <div>
         <div class="cms-select">
           <select id="content-type-filter" 
                   data-filter="mime-type"
                   class="cms-select__input">
             <option value="">All Types</option>
             <option value="image/">Images</option>
             <option value="video/">Videos</option>
             <option value="application/pdf,application/msword,application/vnd">Documents</option>
             <option value="folder">Folders</option>
           </select>
         </div>
       </div>
     </div>
   </div>
   ```

### Step 2: Inspect Grid/Table Items

1. **Find filterable items** in the DOM:
   - Look for elements with `data-filterable-item` attribute
   - These might be in a table (`<tr>`) or grid (`<div>`)

2. **Check item attributes:**
   ```html
   <!-- Example grid item -->
   <div class="contentnav__item" 
        data-filterable-item
        data-resource="/static/hello/image.jpg"
        data-type="/libs/sling-cms/components/general/..."
        data-mime-type="image/jpeg"
        data-is-folder="false"
        data-taxonomy=""
        data-page-status=""
        data-template=""
        data-modified-date="week">
   ```

3. **Verify attributes present:**
   - [ ] `data-filterable-item` - **REQUIRED**
   - [ ] `data-mime-type` - For type filtering
   - [ ] `data-is-folder` - For folder filtering
   - [ ] `data-modified-date` - For date filtering
   - [ ] Other attributes as applicable

### Step 3: Test Filtering Functionality

#### Test 1: Type Filter - Images

1. **Select "Images" from the type filter dropdown**

2. **Open Console** and run:
   ```javascript
   // Check filter value
   const filter = document.querySelector('[data-filter="mime-type"]');
   console.log('Filter value:', filter.value); // Should be "image/"
   
   // Count visible items
   const visible = document.querySelectorAll('[data-filterable-item]:not(.cms-filtered-out)');
   console.log('Visible items:', visible.length);
   
   // Check what's visible
   visible.forEach(item => {
     const mimeType = item.getAttribute('data-mime-type');
     console.log('Visible item:', mimeType);
     // Should only show items with mime-type starting with "image/"
   });
   
   // Check hidden items
   const hidden = document.querySelectorAll('[data-filterable-item].cms-filtered-out');
   console.log('Hidden items:', hidden.length);
   ```

3. **Visual Verification:**
   - [ ] Only image items are visible in the grid/table
   - [ ] Videos, documents, folders are hidden
   - [ ] No console errors

#### Test 2: Type Filter - Folders

1. **Select "Folders" from the dropdown**

2. **Console check:**
   ```javascript
   const visible = document.querySelectorAll('[data-filterable-item]:not(.cms-filtered-out)');
   visible.forEach(item => {
     console.log('Is folder:', item.getAttribute('data-is-folder'));
     // Should all be "true"
   });
   ```

3. **Visual Verification:**
   - [ ] Only folders are visible
   - [ ] Files are hidden

#### Test 3: Type Filter - All Types (Reset)

1. **Select "All Types"**

2. **Console check:**
   ```javascript
   const visible = document.querySelectorAll('[data-filterable-item]:not(.cms-filtered-out)');
   const total = document.querySelectorAll('[data-filterable-item]');
   console.log('Visible:', visible.length, 'Total:', total.length);
   // Should be equal
   ```

3. **Visual Verification:**
   - [ ] All items are visible again
   - [ ] No items have `cms-filtered-out` class

### Step 4: Test View Switching (if applicable)

If the page has both grid and table views:

#### Grid View
1. **Switch to Grid View**
2. **Select "Images" filter**
3. **Verify:**
   ```javascript
   // Grid items should have data-filterable-item
   document.querySelectorAll('.cms-grid [data-filterable-item]').length
   ```

#### Table View
1. **Switch to Table View**
2. **Select "Videos" filter**
3. **Verify:**
   ```javascript
   // Table rows should have data-filterable-item
   document.querySelectorAll('table tr[data-filterable-item]').length
   ```

### Step 5: Check for Common Issues

#### Issue 1: No items match filter
```javascript
// Debug script
const filter = document.querySelector('[data-filter="mime-type"]');
const items = document.querySelectorAll('[data-filterable-item]');

console.log('Filter attribute:', filter.getAttribute('data-filter'));
console.log('Filter value:', filter.value);

items.forEach((item, index) => {
  console.log(`Item ${index}:`, {
    hasMimeType: item.hasAttribute('data-mime-type'),
    mimeType: item.getAttribute('data-mime-type'),
    hasFilterableItem: item.hasAttribute('data-filterable-item')
  });
});
```

**Expected output:**
```
Filter attribute: mime-type
Filter value: image/
Item 0: { hasMimeType: true, mimeType: "image/jpeg", hasFilterableItem: true }
Item 1: { hasMimeType: true, mimeType: "video/mp4", hasFilterableItem: true }
...
```

#### Issue 2: Filter not working
```javascript
// Check if JavaScript loaded
console.log('ContentFilter:', typeof Sling !== 'undefined' && Sling.CMS && Sling.CMS.ContentFilter);

// Check if filter initialized
const filterElement = document.querySelector('[data-component="content-filter"]');
console.log('Filter element:', filterElement);

// Manually trigger filter
const select = document.querySelector('[data-filter="mime-type"]');
select.value = 'image/';
select.dispatchEvent(new Event('change', { bubbles: true }));
```

#### Issue 3: Wrong items filtered
```javascript
// Verify matching logic
function testMatch(itemValue, filterValue) {
  if (filterValue.endsWith('/')) {
    return itemValue.startsWith(filterValue);
  }
  return itemValue === filterValue;
}

console.log('Test: image/jpeg matches image/', testMatch('image/jpeg', 'image/')); // Should be true
console.log('Test: video/mp4 matches image/', testMatch('video/mp4', 'image/')); // Should be false
```

## Expected Results Summary

| Filter Selection | Expected Visible Items | Hidden Items |
|-----------------|----------------------|--------------|
| All Types | All items | None |
| Images | Items with `data-mime-type` starting with "image/" | All others |
| Videos | Items with `data-mime-type` starting with "video/" | All others |
| Documents | Items with `data-mime-type` matching PDF/Word/etc. | All others |
| Folders | Items with `data-is-folder="true"` | All files |

## Troubleshooting

### Problem: Filter dropdown doesn't appear
**Cause:** ContentFilter component not included or not rendering

**Solution:**
1. Check if the page includes the filter component
2. Verify ContentFilter.java is deployed
3. Check for errors in the Sling logs

### Problem: Items don't filter
**Cause:** Missing `data-filterable-item` or mismatched attribute names

**Solution:**
```javascript
// Check items have required attribute
document.querySelectorAll('[data-filterable-item]').length

// Check if items are missing mime-type
const itemsWithoutMimeType = Array.from(document.querySelectorAll('[data-filterable-item]'))
  .filter(item => !item.hasAttribute('data-mime-type'));
console.log('Items missing mime-type:', itemsWithoutMimeType.length);
```

### Problem: All items disappear when filtering
**Cause:** Attribute values don't match filter expectations

**Solution:**
```javascript
// Check what values exist
const items = document.querySelectorAll('[data-filterable-item]');
const mimeTypes = new Set();
items.forEach(item => {
  const mt = item.getAttribute('data-mime-type');
  if (mt) mimeTypes.add(mt);
});
console.log('Unique mime-types:', Array.from(mimeTypes));
```

## Manual Testing Checklist

- [ ] Page loads without errors
- [ ] Filter dropdown is visible
- [ ] Filter has options (All Types, Images, Videos, etc.)
- [ ] Selecting "Images" shows only images
- [ ] Selecting "Videos" shows only videos
- [ ] Selecting "Folders" shows only folders
- [ ] Selecting "All Types" shows everything
- [ ] Switching between table and grid view preserves filtering
- [ ] No JavaScript console errors
- [ ] Filter count updates (if displayed)

## Success Criteria

✅ **PASS** if:
- All test scenarios work as expected
- No console errors appear
- Items filter correctly based on selection
- Reset to "All Types" shows all items

❌ **FAIL** if:
- Filter dropdown doesn't appear
- Items don't filter when selection changes
- Wrong items are filtered
- Console shows JavaScript errors

## Next Steps After Validation

If validation **PASSES**:
- [ ] Document test results
- [ ] Test on other paths (assets, pages, etc.)
- [ ] Test with larger datasets
- [ ] Test performance with 100+ items

If validation **FAILS**:
- [ ] Copy console errors
- [ ] Note which specific test failed
- [ ] Check if deployment completed successfully
- [ ] Review browser network tab for failed resource loads
- [ ] Check Sling error logs at http://localhost:8082/system/console/logs
