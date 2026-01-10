# Filter Attribute Mapping Test

## Test Case: Image Filtering in Content Grid

### Setup
```html
<!-- Filter Dropdown (contentfilter.html) -->
<select data-filter="mime-type">
  <option value="">All Types</option>
  <option value="image/">Images</option>  <!-- Selected -->
</select>

<!-- Grid Item (contentgrid.html) -->
<div data-filterable-item 
     data-mime-type="image/jpeg"
     data-is-folder="false">
  <!-- Item content -->
</div>
```

### JavaScript Execution Flow

1. **Filter Change Event**
   ```javascript
   // User selects "Images" option
   filterValue = "image/"
   filterAttr = "mime-type"
   ```

2. **Apply Filters**
   ```javascript
   filters = { "mime-type": "image/" }
   
   items.forEach(item => {
     filterItem(item, filters)
   })
   ```

3. **Filter Item**
   ```javascript
   // For each filter
   Object.entries(filters).forEach(([attr, value]) => {
     // attr = "mime-type"
     // value = "image/"
     
     const itemValue = item.getAttribute(`data-${attr}`)
     // itemValue = item.getAttribute("data-mime-type")
     // itemValue = "image/jpeg"
     
     if (!matchesFilter(itemValue, value)) {
       visible = false
     }
   })
   ```

4. **Match Filter**
   ```javascript
   matchesFilter("image/jpeg", "image/")
   
   // Check: filterValue.endsWith('/')
   // "image/".endsWith('/') = true
   
   // Return: itemValue.startsWith(filterValue)
   // "image/jpeg".startsWith("image/") = true ✅
   ```

5. **Result**
   ```javascript
   visible = true
   item.style.display = ''  // Show item
   item.classList.remove('cms-filtered-out')
   ```

## Expected Behavior

✅ **PASS** - Image item is visible when "Images" filter is selected

## Attribute Mapping Reference

| Filter Label | Filter Value | data-filter | Item Attribute | Item Value Example | Match Logic |
|--------------|-------------|-------------|----------------|-------------------|-------------|
| Images | `image/` | `mime-type` | `data-mime-type` | `image/jpeg` | Prefix match |
| Videos | `video/` | `mime-type` | `data-mime-type` | `video/mp4` | Prefix match |
| PDF | `application/pdf` | `mime-type` | `data-mime-type` | `application/pdf` | Exact match |
| Documents | `application/pdf,application/msword,application/vnd` | `mime-type` | `data-mime-type` | `application/pdf` | Comma-separated prefix |
| Folders | `folder` | `mime-type` | `data-mime-type` | `folder` | Exact match |

## Asset Filter Mapping

| Filter Label | Filter Value | data-filter | Item Attribute | Item Value Example | Match Logic |
|--------------|-------------|-------------|----------------|-------------------|-------------|
| Images | `image/` | `asset-type` | `data-asset-type` | `image/png` | Prefix match |
| Videos | `video/` | `asset-type` | `data-asset-type` | `video/mp4` | Prefix match |
| < 100 KB | `0-100` | `asset-size` | `data-asset-size` | `0-100` | Exact match |
| 1-10 MB | `1024-10240` | `asset-size` | `data-asset-size` | `1024-10240` | Exact match |

## Common Pitfalls

### ❌ Mismatch: Wrong attribute name
```html
<!-- Filter -->
<select data-filter="type">  <!-- Wrong! -->

<!-- Item -->
<div data-mime-type="image/jpeg">  <!-- Won't match -->
```

### ✅ Correct: Matching attribute names
```html
<!-- Filter -->
<select data-filter="mime-type">  <!-- Correct -->

<!-- Item -->
<div data-mime-type="image/jpeg">  <!-- Matches! -->
```

### ❌ Wrong: Missing prefix slash
```html
<!-- Filter -->
<option value="image">Images</option>  <!-- Wrong! -->

<!-- Item -->
<div data-mime-type="image/jpeg">  <!-- Won't match (exact match fails) -->
```

### ✅ Correct: Include trailing slash for prefix matching
```html
<!-- Filter -->
<option value="image/">Images</option>  <!-- Correct -->

<!-- Item -->
<div data-mime-type="image/jpeg">  <!-- Matches via prefix! -->
```

## Verification Steps

1. **Open Browser Console**
2. **Run Test Query**
   ```javascript
   // Check filter exists
   const filter = document.querySelector('[data-filter="mime-type"]');
   console.log('Filter:', filter);
   
   // Check items exist
   const items = document.querySelectorAll('[data-filterable-item][data-mime-type]');
   console.log('Items with mime-type:', items.length);
   
   // Test filtering manually
   filter.value = 'image/';
   filter.dispatchEvent(new Event('change'));
   
   // Check visible items
   const visible = document.querySelectorAll('[data-filterable-item]:not(.cms-filtered-out)');
   console.log('Visible items:', visible.length);
   
   // Check if images are visible
   visible.forEach(item => {
     console.log('Visible item mime-type:', item.getAttribute('data-mime-type'));
   });
   ```

3. **Expected Console Output**
   ```
   Filter: <select data-filter="mime-type">
   Items with mime-type: 45
   Visible items: 12
   Visible item mime-type: image/jpeg
   Visible item mime-type: image/png
   Visible item mime-type: image/gif
   ...
   ```

## Conclusion

The attribute mapping is **CORRECT**:
- ✅ Filter uses `data-filter="mime-type"`
- ✅ Items use `data-mime-type="image/jpeg"`
- ✅ JavaScript converts `mime-type` → `data-mime-type`
- ✅ Prefix matching handles `image/` → `image/jpeg`

**The system should work as designed!**
