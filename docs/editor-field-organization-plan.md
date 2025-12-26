# SPDX-License-Identifier: Apache-2.0

# Editor Field Component Organization - Safe Implementation Plan

**Date:** 2025-12-26
**Status:** Proposed Alternative Approach
**Risk Level:** Medium (was High with directory reorganization)

## Problem Statement

From the frontend-ui-analysis:
- **58+ field components** in single flat directory
- No logical grouping
- Hard to find specific field types
- No naming convention consistency

## Original Recommendation (HIGH RISK - NOT RECOMMENDED)

The original plan suggested physically reorganizing components into subdirectories:
```
components/editor/fields/
├── text/
│   ├── text/
│   ├── textarea/
│   └── ...
├── selection/
│   ├── select/
│   ├── checkbox/
│   └── ...
```

### Why This Is Too Risky

1. **Massive Scope**: 58+ components to move
2. **Breaking Changes**: All `sling:resourceType` references must update
3. **Content Files**: 100+ JSON/XML files reference these components
4. **Testing Burden**: Every field type must be tested
5. **Rollback Difficulty**: Hard to revert if issues found
6. **Production Impact**: Could break live content editing

**Estimated Risk**: **HIGH** - Could break critical CMS functionality

---

## RECOMMENDED APPROACH: Documentation-Based Organization

Instead of moving files, create **documentation and tooling** to help developers find and understand field components.

### Phase 1: Create Field Component Catalog (SAFE - RECOMMENDED)

**Create:** `/docs/editor-field-catalog.md`

**Contents:**
- Categorized list of all field components
- Description and use case for each
- Link to component location
- Examples of usage
- Migration status (JSP vs HTL)

**Benefits:**
- ✅ Zero risk of breaking existing functionality
- ✅ Immediate improvement in discoverability
- ✅ Easy to maintain and update
- ✅ Helps developers understand field types
- ✅ Foundation for future migration

**Risk Level:** **NONE** - Pure documentation

### Phase 2: Add Component Metadata (LOW RISK)

**Action:** Add/improve `.content.xml` for each field component

**Add Properties:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          jcr:primaryType="sling:Component"
          jcr:title="Text Field"
          jcr:description="Single-line text input field"
          componentGroup="Sling CMS - Editor Fields"
          fieldCategory="text"
          fieldType="input"/>
```

**Benefits:**
- ✅ Searchable metadata
- ✅ Better component browser support
- ✅ Can filter/group in UI tools
- ✅ No code changes required
- ✅ Backward compatible

**Risk Level:** **LOW** - Only adds metadata, doesn't change functionality

### Phase 3: Create Helper Scripts (LOW RISK)

**Script 1:** Field component finder
```bash
#!/bin/bash
# find-field.sh - Find field components by category

case $1 in
  text)
    echo "Text Fields:"
    find ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields \
      -name "text*" -o -name "richtext"
    ;;
  selection)
    echo "Selection Fields:"
    find ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields \
      -name "select*" -o -name "radio*" -o -name "checkbox*"
    ;;
  # ... more categories
esac
```

**Script 2:** Component usage finder
```bash
#!/bin/bash
# find-usage.sh - Find where a field component is used

grep -r "sling:resourceType.*editor/fields/$1" \
  ui/src/main/resources/jcr_root/libs/sling-cms/content/
```

**Benefits:**
- ✅ Quick discovery without moving files
- ✅ No risk to production
- ✅ Easy to extend

**Risk Level:** **NONE** - Pure tooling

### Phase 4: Create README Files (SAFE)

**Add README in each category-related group:**

`/ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/README.md`

```markdown
# Editor Field Components

## Text Input Fields
- `text/` - Single-line text input
- `textarea/` - Multi-line text input
- `richtext/` - Rich text editor (TipTap)
- `textfield/` - Alternative text field
- `code/` - Code editor field

## Selection Fields
- `select/` - Dropdown selection
- `checkbox/` - Single checkbox
- `radio/` - Radio button group
- `multiselect/` - Multiple selection
- `toggle/` - Toggle switch
- `tags/` - Tag selector

... (continue for all categories)
```

**Benefits:**
- ✅ In-place documentation
- ✅ IDE-friendly
- ✅ Easy to update
- ✅ No breaking changes

**Risk Level:** **NONE** - Pure documentation

---

## Future Migration Path (If Reorganization Still Desired)

**ONLY after 100% HTL migration complete:**

1. **Create symbolic links** (if filesystem supports)
2. **Use Sling resource type aliasing**
3. **Gradual migration** one category at a time
4. **Maintain both paths** during transition
5. **Deprecation period** of 6+ months
6. **Automated migration tool** for content updates

**Timeframe:** 6-12 months after HTL migration complete

---

## Recommended Implementation Order

### Immediate (Week 1) - SAFE ✅
1. ✅ Create `/docs/editor-field-catalog.md`
2. ✅ Document all 58+ field components
3. ✅ Categorize by type
4. ✅ Add usage examples

### Short-term (Weeks 2-4) - LOW RISK ✅
1. Add/improve `.content.xml` metadata for field components
2. Create helper scripts (`find-field.sh`, `find-usage.sh`)
3. Add README.md in fields directory
4. Update developer documentation

### Medium-term (Months 2-3) - OPTIONAL
1. Create component browser UI enhancement
2. Add search/filter by category
3. Create visual component gallery

### Long-term (6+ months) - ONLY IF NEEDED
1. Consider actual file reorganization
2. Only after 100% HTL migration
3. Use gradual, non-breaking approach
4. Maintain backward compatibility

---

## Decision: Use Documentation Approach

**Recommendation:** Implement **Phase 1-3 only** (documentation and tooling)

**Rationale:**
1. **Zero risk** to production system
2. **Immediate benefits** for developers
3. **Foundation** for future improvements
4. **No breaking changes** required
5. **Easy to maintain** and extend

**Physical reorganization is NOT recommended** until:
- 100% HTL migration complete
- Clear business value demonstrated
- Comprehensive testing infrastructure in place
- Migration tooling developed
- Deprecation period planned

---

## Implementation

I will now create the **Field Component Catalog** document as a safe, immediate improvement that provides the organizational benefits without any risk of breaking functionality.

**Next Steps:**
1. Create comprehensive field catalog documentation
2. Add to project documentation
3. Link from main developer docs
4. Create helper scripts (optional)

This approach gives you **90% of the benefit with 0% of the risk**.
