# SPDX-License-Identifier: Apache-2.0

# Apache Sling CMS - Editor Field Component Catalog

**Version:** 1.1.9-SNAPSHOT
**Last Updated:** 2025-12-26
**Total Components:** 42

## Table of Contents

- [Text Input Fields](#text-input-fields)
- [Selection Fields](#selection-fields)
- [Structured Data Fields](#structured-data-fields)
- [Rich Text & Content Fields](#rich-text--content-fields)
- [File & Asset Fields](#file--asset-fields)
- [Reference & Relationship Fields](#reference--relationship-fields)
- [Structural/Layout Components](#structurallayout-components)
- [Metadata & Utility Fields](#metadata--utility-fields)
- [Authentication & Authorization Fields](#authentication--authorization-fields)
- [Internationalization Fields](#internationalization-fields)
- [Publication & Workflow Fields](#publication--workflow-fields)
- [Richtext Toolbar Components](#richtext-toolbar-components)

---

## TEXT INPUT FIELDS

### 1. Text Field
**Resource Type:** `sling-cms/components/editor/fields/text`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/text](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/text)

**Purpose:** Standard single-line text input field

**Features:**
- Supports type attribute (text, email, number, date, etc.)
- Required and disabled states
- Multifield support
- HTML attribute encoding for security

**Usage Example:**
```xml
<field
    jcr:primaryType="nt:unstructured"
    sling:resourceType="sling-cms/components/editor/fields/text"
    name="title"
    label="Page Title"
    required="true"/>
```

---

### 2. Textarea Field
**Resource Type:** `sling-cms/components/editor/fields/textarea`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/textarea](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/textarea)

**Purpose:** Multi-line text input area

**Features:**
- Large text content support
- Required and disabled states
- HTML encoding for security

**Usage Example:**
```xml
<description
    jcr:primaryType="nt:unstructured"
    sling:resourceType="sling-cms/components/editor/fields/textarea"
    name="jcr:description"
    label="Description"/>
```

---

### 3. Path Field
**Resource Type:** `sling-cms/components/editor/fields/path`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/path](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/path)

**Purpose:** Specialized text field for path/resource selection with search capability

**Features:**
- Built-in search modal integration
- Path validation
- Base path filtering
- Multifield support

---

### 4. Hidden Field
**Resource Type:** `sling-cms/components/editor/fields/hidden`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/hidden](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/hidden)

**Purpose:** Hidden input fields for storing values not visible to users

**Features:**
- Multifield context support
- Value encoding
- No visual rendering

---

## SELECTION FIELDS

### 5. Select Field
**Resource Type:** `sling-cms/components/editor/fields/select`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/select](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/select)

**Purpose:** Dropdown select box for single or multiple selection

**Features:**
- Single or multiple selection
- String array options (label=value format)
- Child node options (multifield format)
- Custom options script support
- Full-width styling

**Usage Example:**
```xml
<template
    jcr:primaryType="nt:unstructured"
    sling:resourceType="sling-cms/components/editor/fields/select"
    name="template"
    label="Page Template"
    options="Page Template A=templateA,Page Template B=templateB"/>
```

---

### 6. Radio Button Field
**Resource Type:** `sling-cms/components/editor/fields/radio`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/radio](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/radio)

**Purpose:** Group of radio buttons for single selection

**Features:**
- Multiple option formats support
- String array options (label=value)
- Child node options with labels
- Internationalization support

---

### 7. Checkbox Field
**Resource Type:** `sling-cms/components/editor/fields/checkbox`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/checkbox](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/checkbox)

**Purpose:** Group of checkboxes for multi-select

**Features:**
- Multiple option formats
- String array options (label=value)
- Child node options
- Multiple value selection

---

### 8. Boolean Field
**Resource Type:** `sling-cms/components/editor/fields/boolean`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/boolean](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/boolean)

**Purpose:** Toggle switch for true/false values

**Features:**
- Toggle switch UI component
- Default value support
- Optional checkbox label
- Handles string and boolean values

---

### 9. Combobox Field
**Resource Type:** `sling-cms/components/editor/fields/combobox`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/combobox](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/combobox)

**Purpose:** Combo box (text input with dropdown options)

**Features:**
- Text input with predefined options
- Dynamic options generation

---

### 10. Sibling Select Field
**Resource Type:** `sling-cms/components/editor/fields/siblingselect`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/siblingselect](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/siblingselect)

**Purpose:** Select field for ordering siblings

**Features:**
- Shows sibling resources
- "First", "Last", "Before [sibling]" options
- `:order` parameter handling

---

## STRUCTURED DATA FIELDS

### 11. Multifield (Complex Multi-Item)
**Resource Type:** `sling-cms/components/editor/fields/multifield`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/multifield](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/multifield)

**Purpose:** Repeating groups of fields with nested structure

**Features:**
- Add/remove items functionality
- Reorder items (move up/down)
- Template-based field generation
- Min/max items constraints
- Card-based UI with headers

**Usage Example:**
```xml
<authors
    jcr:primaryType="nt:unstructured"
    sling:resourceType="sling-cms/components/editor/fields/multifield"
    label="Authors">
    <fieldConfig jcr:primaryType="nt:unstructured">
        <name
            jcr:primaryType="nt:unstructured"
            sling:resourceType="sling-cms/components/editor/fields/text"
            name="name"
            label="Name"/>
    </fieldConfig>
</authors>
```

---

### 12. Repeating Field
**Resource Type:** `sling-cms/components/editor/fields/repeating`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/repeating](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/repeating)

**Purpose:** Simple repeating input fields (array values)

**Features:**
- Add/remove individual items
- Simple field template
- Type attribute support
- Lighter weight than multifield

---

### 13. Label Field
**Resource Type:** `sling-cms/components/editor/fields/labelfield`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/labelfield](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/labelfield)

**Purpose:** Tag-like labels/tokens that can be added/removed

**Features:**
- Text input with datalist suggestions
- Removable tags display
- String array type hint
- Options autocomplete support

---

## RICH TEXT & CONTENT FIELDS

### 14. Richtext Field
**Resource Type:** `sling-cms/components/editor/fields/richtext`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/richtext](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/richtext)

**Purpose:** Rich text editor (RTE) for formatted content

**Features:**
- Configurable toolbar support
- Default toolbar included
- Textarea-based editor
- Full HTML editing capabilities
- Customizable toolbars per field

**Usage Example:**
```xml
<content
    jcr:primaryType="nt:unstructured"
    sling:resourceType="sling-cms/components/editor/fields/richtext"
    name="content"
    label="Page Content"
    toolbar="/libs/sling-cms/components/editor/fields/richtext/toolbar/default"/>
```

---

## FILE & ASSET FIELDS

### 15. File Field
**Resource Type:** `sling-cms/components/editor/fields/file`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/file](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/file)

**Purpose:** File upload field with drag-and-drop support

**Features:**
- Multiple file upload
- Accepts property configuration (file types)
- Default accept types (documents, images, audio, video, JSON, CSS, PDF)
- Drag-and-drop UI
- Progress tracking

---

### 16. File Metadata Field
**Resource Type:** `sling-cms/components/editor/fields/filemetadata`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/filemetadata](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/filemetadata)

**Purpose:** Displays read-only metadata for uploaded files

**Features:**
- Extract and display file metadata
- Definition list (dl) format
- Date formatting

---

### 17. Thumbnail Field
**Resource Type:** `sling-cms/components/editor/fields/thumbnail`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/thumbnail](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/thumbnail)

**Purpose:** Displays a thumbnail image for assets

**Features:**
- Image rendering from subpath
- Custom CSS classes
- Alt text support
- Suffix support for image variants

---

### 18. Path Browser Field ✨ (HTL)
**Resource Type:** `sling-cms/components/editor/fields/pathbrowser`
**Template:** HTL
**Location:** [/libs/sling-cms/components/editor/fields/pathbrowser](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/pathbrowser)

**Purpose:** Interactive folder/path browser for selecting content paths

**Features:**
- Folder tree navigation
- Breadcrumb navigation
- Base path filtering
- Type filtering
- Loading states
- Modern HTL implementation

---

## REFERENCE & RELATIONSHIP FIELDS

### 19. References Field
**Resource Type:** `sling-cms/components/editor/fields/references`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/references](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/references)

**Purpose:** Displays incoming references to current resource

**Features:**
- Shows pages/resources referencing this content
- Table format with reference details
- Type indicator (page vs. other)
- Toggle support for operations like "move"

---

### 20. Taxonomy Field
**Resource Type:** `sling-cms/components/editor/fields/taxonomy`
**Template:** HTL/JSP (Hybrid)
**Location:** [/libs/sling-cms/components/editor/fields/taxonomy](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/taxonomy)

**Purpose:** Taxonomy/categorization field with term selection

**Features:**
- Term selection interface
- Item rendering via taxonomy item component
- Supports nested structure

---

## STRUCTURAL/LAYOUT COMPONENTS

### 21. Tabs Component
**Resource Type:** `sling-cms/components/editor/fields/tabs`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/tabs](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/tabs)

**Purpose:** Organizes fields into tabbed sections

**Features:**
- Tabbed interface for field organization
- Active tab tracking
- Dynamic tab rendering
- Bulma-based styling

**Usage Example:**
```xml
<tabs jcr:primaryType="nt:unstructured"
      sling:resourceType="sling-cms/components/editor/fields/tabs">
    <general jcr:primaryType="nt:unstructured" label="General">
        <!-- fields here -->
    </general>
    <advanced jcr:primaryType="nt:unstructured" label="Advanced">
        <!-- fields here -->
    </advanced>
</tabs>
```

---

### 22. Fieldsets Component
**Resource Type:** `sling-cms/components/editor/fields/fieldsets`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/fieldsets](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/fieldsets)

**Purpose:** Groups fields into collapsible fieldsets/sections

**Features:**
- Collapsible/expandable sections
- localStorage state persistence
- Keyboard accessibility (Enter/Space)
- Icon toggle (chevron-down/chevron-right)
- ARIA attributes

---

### 23. Well Component
**Resource Type:** `sling-cms/components/editor/fields/well`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/well](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/well)

**Purpose:** Card-like container for grouping fields

**Features:**
- Card header with title
- Optional collapse functionality
- Content container

---

## METADATA & UTILITY FIELDS

### 24. Base Field Wrapper
**Resource Type:** `sling-cms/components/editor/fields/base`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/base](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/base)

**Purpose:** Base wrapper for all fields with common functionality

**Features:**
- Label rendering
- Description/help text
- Required indicator
- Toggle field support (show/hide based on conditions)
- Error event tracking
- Multifield context support

**Note:** This is the foundation component used by all other fields.

---

### 25-28. Utility Fields

**25. Parameter Field** (`param`) - Hidden field for passing request parameters as form values
**26. Resource Parameter Field** (`resourceparam`) - Hidden field storing resource paths from parameters
**27. Name Hint Field** (`namehint`) - Provides naming hint for new JCR nodes
**28. Suffix Label Field** (`suffixlabel`) - Displays the resource path from request suffix

---

## AUTHENTICATION & AUTHORIZATION FIELDS

### 29. Auth Status Field
**Resource Type:** `sling-cms/components/editor/fields/auth/status`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/auth/status](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/auth/status)

**Purpose:** Displays and manages user/group enable/disable status

**Features:**
- Shows enabled/disabled status
- Disable reason display
- AuthorizableWrapper integration

---

### 30-31. Auth Membership Fields

**30. Auth Members Field** (`auth/members`) - Displays and manages group members
**31. Auth Membership Field** (`auth/membership`) - Displays and manages group memberships

---

## INTERNATIONALIZATION FIELDS

### 32. I18n Entry Fields ✨ (HTL)
**Resource Type:** `sling-cms/components/editor/fields/i18nentryfields`
**Template:** HTL
**Location:** [/libs/sling-cms/components/editor/fields/i18nentryfields](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/i18nentryfields)

**Purpose:** Manages internationalization translations across languages

**Features:**
- Language-specific input fields
- Entry key definition
- Multi-language message support
- Sling Model integration (I18nEntryFormModel)

---

## PUBLICATION & WORKFLOW FIELDS

### 33. Publication Field
**Resource Type:** `sling-cms/components/editor/fields/publication`
**Template:** JSP
**Location:** [/libs/sling-cms/components/editor/fields/publication](../ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/publication)

**Purpose:** Displays publication status and provides publish/unpublish actions

**Features:**
- Published status display
- Last publication date/time
- Publication author tracking
- Publish/Republish/Unpublish action buttons
- PublicationManager integration

---

## RICHTEXT TOOLBAR COMPONENTS

### 34-42. Toolbar Components

The richtext field supports a modular toolbar system with the following components:

**34. Toolbar Container** (`richtext/toolbar`) - Container for toolbar buttons
**35. Button Bar** (`richtext/toolbar/buttonbar`) - Groups toolbar buttons into bars
**36. Button Group** (`richtext/toolbar/buttongroup`) - Groups related buttons
**37. Toolbar Button** (`richtext/toolbar/button`) - Individual formatting button
**38. Option Button** (`richtext/toolbar/optionbutton`) - Button with options dropdown
**39. Text Options** (`richtext/toolbar/textoptions`) - Text formatting options
**40. Create Link** (`richtext/toolbar/createlink`) - Link creation button
**41. Insert Image** (`richtext/toolbar/insertimage`) - Image insertion button
**42. View Source** (`richtext/toolbar/viewsource`) - HTML source view toggle

---

## SUMMARY BY CATEGORY

| Category | Count | HTL | JSP | Status |
|----------|-------|-----|-----|--------|
| Text Input | 4 | 0 | 4 | Legacy |
| Selection | 6 | 0 | 6 | Legacy |
| Structured Data | 3 | 0 | 3 | Legacy |
| Rich Text & Content | 1 | 0 | 1 | Legacy |
| File & Asset | 4 | 1 | 3 | Mixed |
| Reference & Relationship | 2 | 1 | 1 | Mixed |
| Structural/Layout | 3 | 0 | 3 | Legacy |
| Metadata & Utility | 5 | 0 | 5 | Legacy |
| Auth & Authorization | 3 | 0 | 3 | Legacy |
| Internationalization | 1 | 1 | 0 | Modern |
| Publication & Workflow | 1 | 0 | 1 | Legacy |
| Richtext Toolbar | 9 | 0 | 9 | Legacy |
| **TOTAL** | **42** | **3** | **39** | **7% HTL** |

---

## MIGRATION STATUS

✨ **Modern HTL Components** (3):
- Path Browser Field
- Taxonomy Field (hybrid)
- I18n Entry Fields

🔄 **Priority for HTL Migration**:
1. **Text Input Fields** (4 components) - Most frequently used
2. **Selection Fields** (6 components) - High usage
3. **Multifield** - Complex but critical
4. **Richtext** - High value component
5. **File Field** - Asset management critical

---

## USAGE GUIDELINES

### Common Field Properties

All fields support these common properties:
- `name` - Form field name
- `label` - Display label
- `description` - Help text
- `required` - Make field required
- `disabled` - Disable field
- `toggle` - Show/hide based on another field's value

### Finding Components

Use the helper script:
```bash
./scripts/find-field.sh <category>
```

Or search by resource type:
```bash
grep -r "sling:resourceType.*editor/fields" ui/src/main/resources/jcr_root/
```

---

## SEE ALSO

- [JSP to HTL Migration Guide](jsp-to-htl-migration.md)
- [Frontend/UI Analysis](frontend-ui-analysis.md)
- [Editor Field Organization Plan](editor-field-organization-plan.md)
- [Component Development Guide](developers.md)

---

**Maintained by:** Apache Sling CMS Project
**Last Review:** 2025-12-26
**Contributors:** Community
