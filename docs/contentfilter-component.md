# Content Filter Component

## Overview

The Content Filter component is a flexible, reusable filter component that can be configured for different use cases including:
- Content/Asset filtering (default)
- Workflow process filtering
- Custom filters

## Component Location

- **HTL Template**: `/libs/sling-cms/components/cms/contentfilter/contentfilter.html`
- **Sling Model**: `org.apache.sling.cms.core.models.ContentFilter`
- **SCSS Styles**: `frontend/src/main/frontend/scss/_contentfilter.scss`

## Usage

### 1. Content Mode (Default)

For filtering content, pages, and assets by type and taxonomy.

**JSON Configuration**:
```json
{
    "contentfilter": {
        "jcr:primaryType": "nt:unstructured",
        "sling:resourceType": "sling-cms/components/cms/contentfilter"
    }
}
```

**Or in HTL**:
```html
<sly data-sly-resource="${'contentfilter' @ resourceType='sling-cms/components/cms/contentfilter'}"/>
```

**Features**:
- Filter by type: Images, Videos, Documents, Folders
- Filter by taxonomy tags (if available)
- Uses `data-content-type-filter` and `data-content-tag-filter` attributes

### 2. Workflow Mode

For filtering workflow process instances by process type and status.

**JSON Configuration**:
```json
{
    "contentfilter": {
        "jcr:primaryType": "nt:unstructured",
        "sling:resourceType": "sling-cms/components/cms/contentfilter",
        "filterMode": "workflow"
    }
}
```

**Features**:
- Filter by process type (dynamically populated)
- Filter by status: Active, Suspended
- Uses `data-filter="process-type"` and `data-filter="status"` attributes
- Applies `cms-workflow-filter` CSS class

### 3. Custom Mode

For custom filter configurations specific to your needs.

**JSON Configuration**:
```json
{
    "contentfilter": {
        "jcr:primaryType": "nt:unstructured",
        "sling:resourceType": "sling-cms/components/cms/contentfilter",
        "filterMode": "custom",
        "customFilters": [
            "status-filter:Filter by Status:check:status:=All,active=Active,inactive=Inactive",
            "priority-filter:Filter by Priority:flag:priority:=All,high=High,medium=Medium,low=Low"
        ]
    }
}
```

**Custom Filter Format**:
```
filterId:label:icon:dataAttr:option1Value=option1Label,option2Value=option2Label
```

**Parameters**:
- `filterId` - HTML ID for the select element
- `label` - Display label (will be translated via i18n)
- `icon` - Icon name (from cms-icon set: filter, check, folder, flag, etc.)
- `dataAttr` - Data attribute name for JavaScript filtering
- `options` - Comma-separated list of value=label pairs

## Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `filterMode` | String | `content` | Filter mode: `content`, `workflow`, or `custom` |
| `customFilters` | String[] | - | Array of custom filter configurations (only for `custom` mode) |

## CSS Classes

### Base Classes
- `.content-filter` - Main container for content mode
- `.content-filter__controls` - Filter controls wrapper for content mode
- `.cms-workflow-filter` - Main container for workflow mode
- `.cms-workflow-filter__controls` - Filter controls wrapper for workflow mode

### Shared Semantic Classes
- `.cms-form-group` - Individual filter wrapper
- `.cms-form-label` - Filter label with icon
- `.cms-icon` - Icon wrapper
- `.cms-icon--{name}` - Specific icons (filter, check, folder, etc.)
- `.cms-select` - Select dropdown wrapper
- `.cms-select__input` - Select input element

## Styling

All filters use the consistent styling from `_contentfilter.scss`:
- Responsive layout (inline on tablet+, stacked on mobile)
- Proper spacing and alignment
- Icon integration
- Dropdown arrow styling
- Framework-agnostic semantic classes

## JavaScript Integration

### Content Mode
```javascript
// Filter by type
document.querySelector('[data-content-type-filter]').addEventListener('change', (e) => {
    const type = e.target.value;
    // Filter content by type
});

// Filter by taxonomy
document.querySelector('[data-content-tag-filter]').addEventListener('change', (e) => {
    const tagPath = e.target.value;
    // Filter content by taxonomy
});
```

### Workflow Mode
```javascript
// Filter by process type
document.querySelector('[data-filter="process-type"]').addEventListener('change', (e) => {
    const processType = e.target.value;
    // Filter instances by process type
});

// Filter by status
document.querySelector('[data-filter="status"]').addEventListener('change', (e) => {
    const status = e.target.value;
    // Filter instances by status
});
```

### Custom Mode
```javascript
// Use data-{dataAttr} attribute specified in configuration
document.querySelector('[data-{dataAttr}]').addEventListener('change', (e) => {
    const value = e.target.value;
    // Handle custom filter
});
```

## Examples

### Example 1: Simple Content Filter
```json
{
    "jcr:primaryType": "sling:Page",
    "jcr:content": {
        "container": {
            "contentfilter": {
                "sling:resourceType": "sling-cms/components/cms/contentfilter"
            }
        }
    }
}
```

### Example 2: Workflow Filter
```json
{
    "jcr:primaryType": "sling:Page",
    "jcr:content": {
        "container": {
            "workflowfilter": {
                "sling:resourceType": "sling-cms/components/cms/contentfilter",
                "filterMode": "workflow"
            }
        }
    }
}
```

### Example 3: Custom Project Filter
```json
{
    "jcr:primaryType": "sling:Page",
    "jcr:content": {
        "container": {
            "projectfilter": {
                "sling:resourceType": "sling-cms/components/cms/contentfilter",
                "filterMode": "custom",
                "customFilters": [
                    "project-status:Project Status:flag:project-status:=All,active=Active,archived=Archived,draft=Draft",
                    "project-team:Team:users:team:=All Teams,engineering=Engineering,design=Design,marketing=Marketing"
                ]
            }
        }
    }
}
```

## Extensibility

### Adding New Filter Modes

To add a new filter mode, modify the `ContentFilter` Sling Model:

1. Add a new init method:
```java
private void initMyCustomFilters() {
    filterConfigs.add(new FilterConfig(
        "my-filter-id",
        "My Filter Label",
        "icon-name",
        "my-data-attr",
        new FilterOption[]{
            new FilterOption("", "All Items"),
            new FilterOption("value1", "Label 1"),
            new FilterOption("value2", "Label 2")
        },
        false
    ));
}
```

2. Call it from the `init()` method:
```java
} else if ("mymode".equals(filterMode)) {
    initMyCustomFilters();
}
```

### Adding New Icons

Icons use the semantic class pattern: `.cms-icon--{name}`

Available icons (from jam-icons):
- `filter` - General filter icon
- `check` - Checkmark/status icon
- `folder` - Folder/taxonomy icon
- `flag` - Priority/flag icon
- `users` - Team/user icon
- `calendar` - Date/time icon
- `search` - Search icon
- etc.

Add new icon mappings in `_grid-abstraction.scss`:
```scss
.cms-icon--myicon { @extend .jam-myicon; }
```

## Best Practices

1. **Use Semantic Classes**: Always use `cms-*` prefixed classes for framework independence
2. **i18n Labels**: All labels support internationalization via `@ i18n`
3. **Data Attributes**: Use descriptive data attributes for JavaScript filtering
4. **Consistent Icons**: Use appropriate semantic icons that match the filter purpose
5. **Mobile First**: Filters automatically stack on mobile, inline on tablet+
6. **Accessibility**: All selects include proper `aria-label` attributes

## Migration Guide

### From Custom Filter to Enhanced ContentFilter

**Before** (Custom Filter):
```html
<div class="my-custom-filter">
    <select id="my-filter">
        <option value="">All</option>
        <option value="val1">Value 1</option>
    </select>
</div>
```

**After** (Enhanced ContentFilter):
```json
{
    "myfilter": {
        "sling:resourceType": "sling-cms/components/cms/contentfilter",
        "filterMode": "custom",
        "customFilters": [
            "my-filter:My Filter:filter:my-filter:=All,val1=Value 1"
        ]
    }
}
```

**Benefits**:
- ✅ Consistent styling across all pages
- ✅ Automatic responsive behavior
- ✅ Icon support out of the box
- ✅ Proper accessibility attributes
- ✅ Framework-agnostic classes
- ✅ Less custom code to maintain
