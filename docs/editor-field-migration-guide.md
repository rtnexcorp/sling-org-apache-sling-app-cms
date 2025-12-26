# SPDX-License-Identifier: Apache-2.0

# Editor Field Component Migration Guide

**Date:** 2025-12-26
**Status:** Directory Structure Created - Ready for Gradual Migration
**Approach:** Copy-First, Non-Breaking, Gradual Migration

## Overview

This guide explains how to safely migrate editor field components from the flat structure to the new organized directory structure.

## Directory Structure Status

### ✅ Phase 1: Structure Created (COMPLETED 2025-12-26)

New category directories have been created:

```
ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/
├── text/              ✅ Created
├── selection/         ✅ Created
├── reference/         ✅ Created
├── taxonomy/          ✅ Created
├── datetime/          ✅ Created
├── file/              ✅ Created (existing)
├── scripts/           ✅ Created
├── toolbar/           ✅ Created
├── structural/        ✅ Created
├── utility/           ✅ Created
├── auth/              ✅ Created (existing)
├── i18n/              ✅ Created
└── publication/       ✅ Created (existing)
```

### ⏳ Phase 2: Gradual Migration (IN PROGRESS - As Needed)

Components will be migrated one-by-one as they are updated or converted to HTL.

**Current Status:** All components remain at root level for backward compatibility.

---

## Migration Process

### When to Migrate a Component

Migrate a component when:
1. **Converting JSP to HTL** - Perfect time to reorganize
2. **Major component update** - Significant changes warrant reorganization
3. **Creating new component** - Start in correct location
4. **Fixing bugs** - Opportunistic migration during bug fixes

### Migration Steps (Non-Breaking)

#### Step 1: Copy Component to New Location

```bash
# Example: Migrating 'text' field component
cd ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields

# Copy (don't move) to new location
cp -r text/ text/text/

# Verify structure
ls -la text/text/
```

**Important:** COPY, don't move! Keep old location working.

#### Step 2: Update Component Definition

Edit the new component's `.content.xml` to add deprecation info for old location:

**Old Location** (`fields/text/.content.xml`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          jcr:primaryType="sling:Component"
          jcr:title="Text Field"
          jcr:description="Single-line text input (DEPRECATED - Use sling-cms/components/editor/fields/text/text)"
          componentGroup="Sling CMS - Editor Fields (Deprecated)"/>
```

**New Location** (`fields/text/text/.content.xml`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          jcr:primaryType="sling:Component"
          jcr:title="Text Field"
          jcr:description="Single-line text input field"
          componentGroup="Sling CMS - Editor Fields - Text Input"
          fieldCategory="text"
          fieldType="input"/>
```

#### Step 3: Create Resource Type Alias (Optional)

For backward compatibility, you can use Sling's resource type aliasing:

**Old component** can super-type to new:
```xml
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          sling:resourceSuperType="sling-cms/components/editor/fields/text/text"/>
```

#### Step 4: Test Thoroughly

```bash
# Build and deploy
mvn clean install -P autoInstallBundle -pl ui -DskipTests

# Test both resource types work:
# - Old: sling-cms/components/editor/fields/text
# - New: sling-cms/components/editor/fields/text/text

# Verify in browser:
# - Create/edit content using the field
# - Check both paths resolve correctly
# - Verify no console errors
```

#### Step 5: Update Documentation

Update the field catalog:
```markdown
### Text Field
**Resource Type (NEW):** `sling-cms/components/editor/fields/text/text`
**Resource Type (DEPRECATED):** `sling-cms/components/editor/fields/text`
**Migration Status:** ✅ Migrated to new structure
```

#### Step 6: Gradual Content Updates (Optional)

**Option A: Let content update naturally**
- Old references continue working
- New content uses new path
- Eventually update in bulk migration

**Option B: Scripted migration**
```bash
# Find all content using old resource type
grep -r "sling:resourceType.*editor/fields/text\"" \
  ui/src/main/resources/jcr_root/ > old-refs.txt

# Update to new resource type
# (Manual or scripted replacement)
```

#### Step 7: Deprecation Period

**Minimum 6 months** before removing old location:
1. Add deprecation notice to old component
2. Update component group to "(Deprecated)"
3. Log warning when old type is used
4. Document migration in release notes

#### Step 8: Remove Old Location (After Deprecation)

After 6+ months and verification:
```bash
# Archive old component
git mv fields/text/ fields/_archived/text-deprecated/

# Or delete completely
rm -rf fields/text/

# Document removal in release notes
```

---

## Migration Priority

### High Priority (Migrate First)

These are frequently used and benefit most from organization:

1. **Text Fields** (4 components)
   - text → text/text
   - textarea → text/textarea
   - path → text/path
   - hidden → utility/hidden

2. **Selection Fields** (6 components)
   - select → selection/select
   - checkbox → selection/checkbox
   - radio → selection/radio
   - boolean → selection/boolean
   - combobox → selection/combobox
   - siblingselect → selection/siblingselect

3. **Multifield** (1 component)
   - multifield → structural/multifield

### Medium Priority

4. **Richtext** (1 component)
   - richtext → text/richtext
   - richtext/toolbar → toolbar/ (already organized)

5. **File Fields** (3 components)
   - file → file/file
   - filemetadata → file/metadata
   - thumbnail → file/thumbnail
   - pathbrowser → file/pathbrowser

### Low Priority

6. **Utility Fields** (5 components)
   - base → utility/base
   - param → utility/param
   - resourceparam → utility/resourceparam
   - namehint → utility/namehint
   - suffixlabel → utility/suffixlabel

7. **Other Categories** (as needed)

---

## Example: Complete Migration of 'text' Field

### Before (Current State)
```
fields/
├── text/
│   ├── .content.xml
│   └── field.jsp
└── ... (other components)
```

**Resource Type:** `sling-cms/components/editor/fields/text`

### After Migration
```
fields/
├── text/                    (deprecated - for compatibility)
│   ├── .content.xml         (updated with deprecation notice)
│   └── field.jsp            (delegates to new location)
├── text/
│   └── text/                (new organized location)
│       ├── .content.xml
│       └── text.html        (migrated to HTL)
└── ... (other components)
```

**Resource Types:**
- **New (Recommended):** `sling-cms/components/editor/fields/text/text`
- **Old (Deprecated):** `sling-cms/components/editor/fields/text` → delegates to new

### Migration Timeline Example

| Week | Action |
|------|--------|
| 1 | Copy component to new location |
| 1 | Update .content.xml with metadata |
| 1 | Test both paths work |
| 1 | Update documentation |
| 2-26 | Deprecation period (6 months) |
| 26 | Remove old location if no issues |

---

## Rollback Plan

If issues are found after migration:

1. **Old path still works** - No immediate breakage
2. **Revert .content.xml** changes to old component
3. **Remove new location** temporarily
4. **Investigate** issues before retry
5. **Re-migrate** when ready

---

## Tools & Scripts

### Find Component Usage

```bash
#!/bin/bash
# find-component-usage.sh - Find where a component is used

COMPONENT_PATH=$1

echo "Searching for: $COMPONENT_PATH"
grep -r "sling:resourceType.*${COMPONENT_PATH}" \
  ui/src/main/resources/jcr_root/ \
  | cut -d: -f1 \
  | sort \
  | uniq
```

### Migrate Component Script

```bash
#!/bin/bash
# migrate-field-component.sh - Migrate a field component

COMPONENT=$1
CATEGORY=$2

SOURCE="fields/${COMPONENT}"
DEST="fields/${CATEGORY}/${COMPONENT}"

# Copy component
cp -r "$SOURCE" "$DEST"

echo "Migrated: $COMPONENT"
echo "  From: $SOURCE"
echo "  To: $DEST"
echo ""
echo "Next steps:"
echo "1. Update $DEST/.content.xml"
echo "2. Test both paths work"
echo "3. Update documentation"
```

---

## Current Status Summary

| Category | Directory | Status | Ready for Migration |
|----------|-----------|--------|---------------------|
| Text Input | `text/` | ✅ Created | Yes |
| Selection | `selection/` | ✅ Created | Yes |
| Reference | `reference/` | ✅ Created | Yes |
| Taxonomy | `taxonomy/` | ✅ Exists | Yes |
| DateTime | `datetime/` | ✅ Created | Yes |
| File | `file/` | ✅ Exists | Yes |
| Scripts | `scripts/` | ✅ Created | Yes |
| Toolbar | `toolbar/` | ✅ Created | Yes |
| Structural | `structural/` | ✅ Created | Yes |
| Utility | `utility/` | ✅ Created | Yes |
| Auth | `auth/` | ✅ Exists | Yes |
| I18n | `i18n/` | ✅ Created | Yes |
| Publication | `publication/` | ✅ Exists | Yes |

**All directories ready for migration!** ✅

---

## Best Practices

1. **Always copy first, never move directly**
2. **Test both old and new paths work**
3. **Minimum 6-month deprecation period**
4. **Update documentation immediately**
5. **Combine with JSP→HTL migration when possible**
6. **One component at a time** - don't batch migrate
7. **Monitor logs** for usage of deprecated paths
8. **Gradual content updates** - no big-bang migrations

---

## See Also

- [Editor Field Catalog](editor-field-catalog.md) - All components documented
- [Editor Field Organization Plan](editor-field-organization-plan.md) - Overall strategy
- [JSP to HTL Migration](jsp-to-htl-migration.md) - Template migration
- [Frontend/UI Analysis](frontend-ui-analysis.md) - Full analysis

---

**Status:** Directory structure ready. Components can now be migrated gradually as they are updated.
