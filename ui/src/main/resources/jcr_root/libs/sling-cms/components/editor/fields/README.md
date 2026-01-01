# SPDX-License-Identifier: Apache-2.0

# Editor Field Components

This directory contains all field components used in the Apache Sling CMS editor.

## Directory Structure

### Current Organization (Flat - Legacy)
All field components are currently in this root directory for backward compatibility.

### Proposed Organization (Categorized - Future)
New subdirectories are available for organized migration:

```
fields/
├── text/              - Text input fields
├── selection/         - Selection/choice fields
├── reference/         - Reference and relationship fields
├── taxonomy/          - Taxonomy/categorization fields
├── datetime/          - Date and time fields
├── file/              - File and asset fields
├── scripts/           - Utility scripts (init, finalize, etc.)
├── toolbar/           - Richtext toolbar components
├── structural/        - Layout components (tabs, fieldsets, well)
├── utility/           - Utility fields (hidden, param, etc.)
├── auth/              - Authentication/authorization fields
├── i18n/              - Internationalization fields
└── publication/       - Publication workflow fields
```

## Migration Strategy

### Phase 1: Structure Created (CURRENT)
- ✅ Directory structure created
- ✅ Documentation in place
- ⏳ Components remain in root for compatibility

### Phase 2: Gradual Migration (FUTURE)
When migrating a component:
1. Copy (don't move) component to new category directory
2. Update component's sling:resourceSuperType to point to new location
3. Test thoroughly
4. Update content references gradually
5. Keep old location as deprecated for 6+ months
6. Remove old location after deprecation period

### Phase 3: Cleanup (LONG-TERM)
- Remove deprecated old locations
- Update all documentation
- Archive migration guide

## Finding Components

**Current Location (use these resource types):**
```
sling-cms/components/editor/fields/text
sling-cms/components/editor/fields/select
sling-cms/components/editor/fields/multifield
... etc
```

**Future Location (after migration):**
```
sling-cms/components/editor/fields/text/text
sling-cms/components/editor/fields/selection/select
sling-cms/components/editor/fields/structural/multifield
... etc
```

## Component Categories

### Text Input Fields (Root Level - Current)
- `text/` - Single-line text input
- `textarea/` - Multi-line text input
- `path/` - Path selection with search
- `hidden/` - Hidden input fields

### Selection Fields (Root Level - Current)
- `select/` - Dropdown selection
- `radio/` - Radio button group
- `checkbox/` - Checkbox group
- `boolean/` - Toggle switch
- `combobox/` - Combo box (text + dropdown)
- `siblingselect/` - Sibling ordering selection

### Structured Data Fields (Root Level - Current)
- `multifield/` - Repeating field groups
- `repeating/` - Simple repeating fields
- `labelfield/` - Tag/label fields

### Rich Text Fields (Root Level - Current)
- `richtext/` - Rich text editor
- `richtext/toolbar/` - Toolbar components

### File & Asset Fields (Root Level - Current)
- `file/` - File upload
- `filemetadata/` - File metadata display
- `thumbnail/` - Thumbnail display
- `pathbrowser/` - Path browser

### Reference Fields (Root Level - Current)
- `references/` - Incoming references display
- `taxonomy/` - Taxonomy selection

### Structural/Layout (Root Level - Current)
- `tabs/` - Tabbed sections
- `fieldsets/` - Collapsible fieldsets
- `well/` - Card container

### Utility Fields (Root Level - Current)
- `base/` - Base field wrapper
- `param/` - Request parameter field
- `resourceparam/` - Resource parameter field
- `namehint/` - Node naming hint
- `suffixlabel/` - Suffix path display

### Authentication Fields (Root Level - Current)
- `auth/status/` - User/group status
- `auth/members/` - Group members
- `auth/membership/` - Group membership

### Internationalization (Root Level - Current)
- `i18nentryfields/` - I18n translation fields

### Publication (Root Level - Current)
- `publication/` - Publication status and actions

## See Also

- [Editor Field Catalog](../../../../../../../docs/editor-field-catalog.md) - Complete component reference
- [Editor Field Organization Plan](../../../../../../../docs/editor-field-organization-plan.md) - Migration strategy
- [JSP to HTL Migration Guide](../../../../../../../docs/jsp-to-htl-migration.md) - Template migration

## Notes

- **All components remain at root level** for backward compatibility
- **New directories are prepared** for future organized migration
- **No immediate changes required** - this is foundation work
- **Migration will be gradual** when components are updated/migrated to HTL
