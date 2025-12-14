# Apache Sling CMS - Taxonomy & Asset Tagging End-to-End Guide

## Overview
This guide demonstrates the complete workflow for using the taxonomy system to tag assets in Apache Sling CMS, including all HTL conversions completed.

## Prerequisites
- Apache Sling CMS running on `http://localhost:8082`
- Login credentials: admin/admin
- All HTL conversions deployed successfully

## Part 1: Create Taxonomy Structure

### Step 1.1: Access Taxonomy Manager
1. Navigate to: `http://localhost:8082/cms/taxonomy/list.html/etc/taxonomy`
2. Click **"+ Taxonomy Item"** button
3. Enter taxonomy details:
   - **Name**: `product-categories`
   - **Title**: `Product Categories`
4. Click **Save**

### Step 1.2: Add Taxonomy Terms
1. Click on the newly created `product-categories` taxonomy
2. Add terms:
   - Click **"+ Term"**
   - **Name**: `electronics`
   - **Title**: `Electronics`
   - Click **Save**

3. Repeat for more terms:
   - `clothing` → `Clothing`
   - `books` → `Books`
   - `furniture` → `Furniture`

### Step 1.3: Add Sub-terms (Optional)
1. Click on `electronics` term
2. Click **"+ Term"** to add sub-term:
   - **Name**: `laptops`
   - **Title**: `Laptops`
3. Add more sub-terms:
   - `phones` → `Mobile Phones`
   - `tablets` → `Tablets`

## Part 2: Upload Assets

### Step 2.1: Navigate to Assets
1. Go to: `http://localhost:8082/cms/asset/content.html/content/starter/assets`
2. Click **"+ File"** or **"Upload"** button

### Step 2.2: Upload Images
Upload sample images:
- `laptop-product.jpg`
- `phone-product.jpg`
- `book-cover.jpg`
- `chair-furniture.jpg`

## Part 3: Tag Assets with Taxonomy

### Step 3.1: Open Asset Metadata Editor
1. In the asset browser (grid or table view), click on an uploaded image
2. Click the **"Metadata"** button or icon
3. The Asset Metadata Editor will open

### Step 3.2: Add Taxonomy Tags
1. In the metadata form, find the **"Taxonomy"** field
2. Start typing or click to open the taxonomy autocomplete
3. Select appropriate tags:
   - For `laptop-product.jpg`: Select `Electronics > Laptops`
   - For `phone-product.jpg`: Select `Electronics > Mobile Phones`
   - For `book-cover.jpg`: Select `Books`
   - For `chair-furniture.jpg`: Select `Furniture`

4. Click **Save** button

### Step 3.3: Verify Tags Display
- The taxonomy tags should now appear on the asset card
- No "Unexpected Exception" error should occur
- Tags display as comma-separated values in grid view

## Part 4: Test Grid View (HTL Converted)

### Step 4.1: Grid View Display
1. Navigate to: `http://localhost:8082/cms/asset/content.html/content/starter/assets`
2. Click the **Grid View** icon (if not already in grid view)
3. Verify:
   - ✅ All assets display with thumbnails
   - ✅ Taxonomy tags appear below each asset
   - ✅ Multiple tags are comma-separated
   - ✅ No JavaScript errors in browser console

### Step 4.2: Filter by Taxonomy
1. Use the content filter bar (if available)
2. Select a taxonomy term (e.g., "Electronics")
3. Verify:
   - ✅ Only assets tagged with that term are shown
   - ✅ Grid updates dynamically
   - ✅ No errors in logs

### Step 4.3: Pagination Test
1. If you have more than 60 assets, test pagination:
   - Click **"Next"** button at bottom
   - Verify:
     - ✅ Next page loads correctly
     - ✅ Page parameter updates in URL (`?page=1`)
     - ✅ "Previous" button appears
   - Click **"Previous"** button
   - Verify:
     - ✅ Returns to page 0
     - ✅ "Previous" button disappears

## Part 5: Test Table View (HTL Converted)

### Step 5.1: Switch to Table View
1. From the asset browser, click the **Table View** icon
2. Verify:
   - ✅ Assets display in table format
   - ✅ Taxonomy column shows tags
   - ✅ Table is sortable by clicking column headers
   - ✅ All data displays correctly

### Step 5.2: Table Pagination
1. Test pagination (if 60+ items):
   - Click **"Next"**
   - Verify page transitions smoothly
   - Click **"Previous"**
   - Verify returns to first page

## Part 6: Test Publish/Unpublish Actions

### Step 6.1: Publish an Asset
1. In grid or table view, find an asset
2. Click the **"Publish"** button (in card footer or table row)
3. Confirm publication
4. Verify:
   - ✅ Asset status changes to "Published"
   - ✅ "Unpublish" or "Republish" button appears
   - ✅ No errors occur

### Step 6.2: Unpublish an Asset
1. Click **"Unpublish"** button on a published asset
2. Confirm unpublication
3. Verify:
   - ✅ Asset status changes to "Not Published"
   - ✅ "Publish" button reappears

## Part 7: Verify HTL Conversions

### Step 7.1: Check Server Logs
1. Open terminal and run:
   ```bash
   tail -50 deployment/author/launcher/logs/error.log | grep -E "Exception|ERROR"
   ```
2. Verify:
   - ✅ No `NumberFormatException` errors
   - ✅ No `SightlyException` errors
   - ✅ No `NullPointerException` errors

### Step 7.2: Browser Console
1. Open browser DevTools (F12)
2. Check Console tab
3. Verify:
   - ✅ No JavaScript errors
   - ✅ No 500 server errors
   - ✅ All AJAX requests succeed

## Part 8: Test i18n Container (HTL Converted)

### Step 8.1: Access i18n Dictionary
1. Navigate to: `http://localhost:8082/cms/i18n/content.html/etc/i18n`
2. Click on a language folder (e.g., `en`)
3. Verify:
   - ✅ All translation keys display in table
   - ✅ Each language has its own column
   - ✅ Input fields are editable

### Step 8.2: Add New Entry
1. Click **"+ Entry"** button
2. Add a new translation key
3. Verify:
   - ✅ New row appears in table
   - ✅ Can enter translations for each language
   - ✅ Save button works

### Step 8.3: Edit Translations
1. Modify translation values in the table
2. Click **"Save i18n Dictionary"** button
3. Verify:
   - ✅ Saves successfully
   - ✅ No errors in console or logs
   - ✅ Changes persist after page reload

## Expected Screenshots (Descriptions)

### Screenshot 1: Grid View with Taxonomy
**Location**: Asset browser in grid view
**Shows**:
- Multiple asset cards in a grid layout
- Each card displays:
  - Thumbnail image
  - Asset title
  - Last modified date
  - Taxonomy tags (comma-separated) like "Electronics, Laptops"
  - Publish/Unpublish buttons

### Screenshot 2: Table View with Taxonomy
**Location**: Asset browser in table view
**Shows**:
- Table with columns: #, Name, Type, Size, Modified, Taxonomy, Actions
- Taxonomy column displays tags for each asset
- Sortable column headers
- Pagination controls at bottom

### Screenshot 3: Asset Metadata Editor
**Location**: Metadata editor modal
**Shows**:
- Form fields for asset properties
- Taxonomy field with autocomplete
- Selected taxonomy tags displayed as chips/tags
- Save and Cancel buttons

### Screenshot 4: Taxonomy Manager
**Location**: Taxonomy admin interface
**Shows**:
- Tree structure of taxonomies
- Product Categories > Electronics > Laptops hierarchy
- Add Term buttons at each level
- Edit/Delete actions

### Screenshot 5: i18n Dictionary Editor
**Location**: i18n container view
**Shows**:
- Table with translation keys in rows
- Language columns (e.g., English, German, French)
- Editable input fields for each translation
- Add Entry button
- Save button at bottom

## Verification Checklist

### ✅ Functionality Working
- [ ] Assets can be uploaded
- [ ] Taxonomy terms can be created
- [ ] Assets can be tagged with multiple taxonomy terms
- [ ] Tags display correctly in grid view
- [ ] Tags display correctly in table view
- [ ] Grid view pagination works
- [ ] Table view pagination works
- [ ] Publish/unpublish actions work
- [ ] i18n dictionary editing works
- [ ] No NumberFormatException errors
- [ ] No SightlyException errors
- [ ] HTL i18n translations work

### ✅ HTL Conversions Completed
- [ ] `contentgrid.html` - Grid view rendering
- [ ] `contenttable.html` - Table view rendering
- [ ] `contentbreadcrumb.html` - Breadcrumb navigation
- [ ] `taxonomy/values.html` - Taxonomy value display
- [ ] `taxonomy/options.html` - Taxonomy autocomplete
- [ ] `taxonomy/item/item.html` - Individual tag rendering
- [ ] `i18ncontainer.html` - i18n dictionary editor

### ✅ Sling Models Created
- [ ] `ContentGridModel.java` - Grid view logic
- [ ] `ContentTableModel.java` - Table view logic
- [ ] `ContentBreadcrumb.java` - Breadcrumb logic
- [ ] `TaxonomyValueHelper.java` - Safe taxonomy display
- [ ] `TaxonomyOptions.java` - Taxonomy autocomplete queries
- [ ] `I18nContainerModel.java` - i18n container logic

## Troubleshooting

### Issue: Taxonomy tags don't display
**Solution**: Check that the asset has the `sling:taxonomy` property set in `jcr:content` node

### Issue: NumberFormatException in logs
**Solution**: Ensure all JSP files using `.class.array` have been converted to HTL or use `fn:contains(class.name, '[')`

### Issue: i18n translations not working
**Solution**: Verify HTL uses `@ i18n` context, not a separate i18n model

### Issue: Pagination doesn't work
**Solution**: Check that Sling Model has `getPreviousPage()` and `getNextPage()` methods (no arithmetic in HTL)

## Performance Notes
- Grid/table views use pagination (60 items per page)
- Taxonomy autocomplete uses JCR-SQL2 queries
- HTL templates are compiled and cached
- Clear cache: Restart Sightly bundle if needed

## Conclusion
All taxonomy and asset tagging functionality is now working with HTL conversions completed. The system handles:
- Safe array-to-string conversion for taxonomy
- Proper null handling
- XSS protection via HTL context escaping
- i18n support throughout
- Efficient pagination
