# ContentFilter Component - Usage Guide

**Component**: `org.apache.sling.cms.core.models.ContentFilter`
**Location**: `core/src/main/java/org/apache/sling/cms/core/models/ContentFilter.java`
**Status**: ✅ Production Ready

## Overview

The ContentFilter is a flexible Sling Model that provides filtering capabilities across different CMS contexts: **Assets**, **Sites**, **Workflow**, and general **Content**. It automatically configures appropriate filters based on the context mode.

## Supported Modes

| Mode | Use Case | Filter Types |
|------|----------|-------------|
| **assets** | Digital Asset Management | Asset type, size, modified date |
| **sites** | Page/Site Management | Page status, template, modified date |
| **workflow** | Workflow Task Management | Process type, status, assignee |
| **content** | General Content Browsing | Content type |
| **custom** | Custom Filter Configuration | User-defined filters |

---

## HTL Component Usage

### Basic Usage

```html
<sly data-sly-use.filter="org.apache.sling.cms.core.models.ContentFilter">
    <div class="cms-content-filter">
        <sly data-sly-list.filterConfig="${filter.filterConfigs}">
            <div class="filter-item" id="${filterConfig.id}">
                <label>
                    <span class="cms-icon cms-icon--${filterConfig.icon}"></span>
                    ${filterConfig.label}
                </label>
                <select data-filter="${filterConfig.dataAttribute}">
                    <sly data-sly-list.option="${filterConfig.options}">
                        <option value="${option.value}">${option.label}</option>
                    </sly>
                </select>
            </div>
        </sly>
    </div>
</sly>
```

### Mode-Specific Usage

#### Assets Mode
```html
<!-- Component properties in .content.xml -->
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
    xmlns:jcr="http://www.jcp.org/jcr/1.0"
    jcr:primaryType="nt:unstructured"
    sling:resourceType="sling-cms/components/cms/contentfilter"
    filterMode="assets"/>
```

```html
<!-- HTL Template -->
<sly data-sly-use.filter="org.apache.sling.cms.core.models.ContentFilter">
    <div class="asset-filters">
        <h3>Filter Assets</h3>
        <sly data-sly-test="${filter.assetsMode}">
            <!-- Renders: Asset Type, Size, Modified Date filters -->
            <sly data-sly-list.filterConfig="${filter.filterConfigs}">
                <!-- Filter controls -->
            </sly>
        </sly>
    </div>
</sly>
```

**Available Filters:**
- **Asset Type**: All, Images, Videos, Audio, PDF, Office Documents
- **Asset Size**: < 100KB, 100KB-1MB, 1MB-10MB, > 10MB
- **Modified Date**: Today, This Week, This Month, This Year

#### Sites Mode
```html
<!-- Component properties -->
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
    filterMode="sites"/>
```

**Available Filters:**
- **Page Status**: All, Published, Draft, Scheduled
- **Template**: All Templates (dynamic - loaded from available templates)
- **Modified Date**: Today, This Week, This Month, This Year

#### Workflow Mode
```html
<!-- Component properties -->
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
    filterMode="workflow"/>
```

**Available Filters:**
- **Process Type**: All Processes (dynamic - loaded from deployed workflows)
- **Status**: All, Active, Suspended
- **Assignee**: All Users, My Tasks, Unassigned

---

## Component Configuration

### Dialog Configuration (.content.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
    xmlns:jcr="http://www.jcp.org/jcr/1.0"
    xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    jcr:primaryType="nt:unstructured">
    <tabs jcr:primaryType="nt:unstructured">
        <general jcr:primaryType="nt:unstructured">
            <fields jcr:primaryType="nt:unstructured">
                <filterMode
                    jcr:primaryType="nt:unstructured"
                    sling:resourceType="sling-cms/components/editor/fields/select"
                    label="Filter Mode"
                    name="filterMode"
                    required="true">
                    <options jcr:primaryType="nt:unstructured">
                        <content
                            jcr:primaryType="nt:unstructured"
                            label="Content"
                            value="content"/>
                        <assets
                            jcr:primaryType="nt:unstructured"
                            label="Assets"
                            value="assets"/>
                        <sites
                            jcr:primaryType="nt:unstructured"
                            label="Sites"
                            value="sites"/>
                        <workflow
                            jcr:primaryType="nt:unstructured"
                            label="Workflow"
                            value="workflow"/>
                        <custom
                            jcr:primaryType="nt:unstructured"
                            label="Custom"
                            value="custom"/>
                    </options>
                </filterMode>
                <customFilters
                    jcr:primaryType="nt:unstructured"
                    sling:resourceType="sling-cms/components/editor/fields/multifield"
                    label="Custom Filters"
                    name="customFilters"
                    visibilityField="filterMode"
                    visibilityValues="[custom]">
                    <field
                        jcr:primaryType="nt:unstructured"
                        sling:resourceType="sling-cms/components/editor/fields/text"
                        label="Filter Config"
                        description="Format: filterId:label:icon:dataAttr:option1=value1,option2=value2"/>
                </customFilters>
            </fields>
        </general>
    </tabs>
</jcr:root>
```

---

## Custom Filter Configuration

### Format
```
filterId:label:icon:dataAttr:option1=value1,option2=value2,...
```

### Example: Custom Author Filter
```
filter-author:Filter by Author:user:author:=All Authors,admin=Administrator,editor=Editor
```

### Multiple Custom Filters
```java
customFilters = new String[] {
    "filter-author:Filter by Author:user:author:=All,admin=Admin,editor=Editor",
    "filter-priority:Priority:flag:priority:=All,high=High,medium=Medium,low=Low",
    "filter-category:Category:folder:category:=All,news=News,blog=Blog"
}
```

---

## JavaScript Integration

### Example: Filter Event Handling

```javascript
// contentfilter.js
(function() {
    'use strict';

    class ContentFilter {
        constructor(element) {
            this.element = element;
            this.filterSelects = element.querySelectorAll('select[data-filter]');
            this.init();
        }

        init() {
            this.filterSelects.forEach(select => {
                select.addEventListener('change', this.handleFilterChange.bind(this));
            });
        }

        handleFilterChange(event) {
            const filterType = event.target.getAttribute('data-filter');
            const filterValue = event.target.value;
            
            // Trigger custom event for content filtering
            const filterEvent = new CustomEvent('cms:filter', {
                detail: {
                    filterType: filterType,
                    filterValue: filterValue,
                    filters: this.getAllFilters()
                },
                bubbles: true
            });
            
            this.element.dispatchEvent(filterEvent);
        }

        getAllFilters() {
            const filters = {};
            this.filterSelects.forEach(select => {
                const filterType = select.getAttribute('data-filter');
                const value = select.value;
                if (value) {
                    filters[filterType] = value;
                }
            });
            return filters;
        }

        reset() {
            this.filterSelects.forEach(select => {
                select.value = '';
            });
            this.element.dispatchEvent(new CustomEvent('cms:filter:reset', { bubbles: true }));
        }
    }

    // Auto-initialize on DOM ready
    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('.cms-content-filter').forEach(element => {
            new ContentFilter(element);
        });
    });

    // Export for manual initialization
    window.CMSContentFilter = ContentFilter;
})();
```

### Listening to Filter Events

```javascript
// In your content list component
document.addEventListener('cms:filter', (event) => {
    const filters = event.detail.filters;
    
    // Apply filters to content list
    filterContentList(filters);
    
    console.log('Active filters:', filters);
});

document.addEventListener('cms:filter:reset', () => {
    // Reset content list to show all items
    resetContentList();
});
```

---

## API Reference

### Public Methods

#### `getFilterMode(): String`
Returns the current filter mode (content, assets, sites, workflow, custom).

#### `getFilterConfigs(): List<FilterConfig>`
Returns an unmodifiable list of filter configurations for the current mode.

#### `getTaxonomyOptions(): List<TaxonomyItem>`
Returns taxonomy options (available for content and sites modes).

#### `hasTaxonomyOptions(): boolean`
Checks if taxonomy options are available.

#### `isWorkflowMode(): boolean`
Returns true if in workflow mode.

#### `isAssetsMode(): boolean`
Returns true if in assets mode.

#### `isSitesMode(): boolean`
Returns true if in sites mode.

#### `isContentMode(): boolean`
Returns true if in content mode (default).

### FilterConfig Properties

| Property | Type | Description |
|----------|------|-------------|
| `id` | String | Unique filter identifier |
| `label` | String | Display label for the filter |
| `icon` | String | Icon name (jam-icons compatible) |
| `dataAttribute` | String | HTML data attribute name |
| `options` | FilterOption[] | Array of filter options |
| `dynamicOptions` | boolean | Whether options are loaded dynamically |

### FilterOption Properties

| Property | Type | Description |
|----------|------|-------------|
| `value` | String | Option value |
| `label` | String | Display label |

---

## Complete HTL Example

```html
<sly data-sly-use.filter="org.apache.sling.cms.core.models.ContentFilter">
    <div class="cms-content-filter" data-mode="${filter.filterMode}">
        
        <!-- Filter Header -->
        <div class="cms-content-filter__header">
            <h3 class="cms-content-filter__title">
                <span class="cms-icon cms-icon--filter"></span>
                Filters
            </h3>
            <button class="cms-button cms-button--text cms-filter-reset">
                <span class="cms-icon cms-icon--close"></span>
                Clear All
            </button>
        </div>

        <!-- Filter Controls -->
        <div class="cms-content-filter__controls">
            <sly data-sly-list.filterConfig="${filter.filterConfigs}">
                <div class="cms-filter-group" id="${filterConfig.id}">
                    <label class="cms-filter-group__label">
                        <span class="cms-icon cms-icon--${filterConfig.icon}"></span>
                        ${filterConfig.label}
                    </label>
                    
                    <sly data-sly-test="${filterConfig.dynamicOptions}">
                        <!-- Dynamic filter - load via AJAX -->
                        <select class="cms-select" 
                                data-filter="${filterConfig.dataAttribute}"
                                data-dynamic="true"
                                data-source="/bin/cms/filters/${filterConfig.dataAttribute}.json">
                            <sly data-sly-list.option="${filterConfig.options}">
                                <option value="${option.value}">${option.label}</option>
                            </sly>
                        </select>
                    </sly>
                    
                    <sly data-sly-test="${!filterConfig.dynamicOptions}">
                        <!-- Static filter options -->
                        <select class="cms-select" data-filter="${filterConfig.dataAttribute}">
                            <sly data-sly-list.option="${filterConfig.options}">
                                <option value="${option.value}">${option.label}</option>
                            </sly>
                        </select>
                    </sly>
                </div>
            </sly>
        </div>

        <!-- Taxonomy Filter (for content/sites modes) -->
        <sly data-sly-test="${filter.hasTaxonomyOptions}">
            <div class="cms-filter-group">
                <label class="cms-filter-group__label">
                    <span class="cms-icon cms-icon--tag"></span>
                    Tags
                </label>
                <select class="cms-select" data-filter="taxonomy" multiple>
                    <sly data-sly-list.taxonomy="${filter.taxonomyOptions}">
                        <option value="${taxonomy.path}">${taxonomy.title}</option>
                    </sly>
                </select>
            </div>
        </sly>

        <!-- Active Filters Display -->
        <div class="cms-content-filter__active" data-sly-test="${false}">
            <span class="cms-filter-tag">
                <span>Type: Images</span>
                <button class="cms-filter-tag__remove">×</button>
            </span>
        </div>

    </div>
</sly>
```

---

## SCSS Styling

Create `/frontend/src/main/frontend/scss/_contentfilter.scss`:

```scss
// Content Filter Component
@import 'variables';

.cms-content-filter {
  background-color: lighten($cms-primary-color, 45%);
  border-radius: $cms-border-radius;
  padding: $cms-spacing-unit * 2;
  margin-bottom: $cms-spacing-unit * 2;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $cms-spacing-unit * 2;
    padding-bottom: $cms-spacing-unit;
    border-bottom: 1px solid darken($cms-primary-color, 10%);
  }

  &__title {
    display: flex;
    align-items: center;
    gap: $cms-spacing-unit;
    margin: 0;
    font-size: 1.125rem;
    font-weight: 600;
  }

  &__controls {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: $cms-spacing-unit * 1.5;
  }

  &__active {
    display: flex;
    flex-wrap: wrap;
    gap: $cms-spacing-unit;
    margin-top: $cms-spacing-unit * 2;
    padding-top: $cms-spacing-unit * 2;
    border-top: 1px solid darken($cms-primary-color, 10%);
  }
}

.cms-filter-group {
  display: flex;
  flex-direction: column;
  gap: $cms-spacing-unit * 0.5;

  &__label {
    display: flex;
    align-items: center;
    gap: $cms-spacing-unit * 0.5;
    font-size: 0.875rem;
    font-weight: 600;
    color: darken($cms-primary-color, 20%);
  }
}

.cms-filter-tag {
  display: inline-flex;
  align-items: center;
  gap: $cms-spacing-unit * 0.5;
  padding: $cms-spacing-unit * 0.5 $cms-spacing-unit;
  background-color: $cms-primary-color;
  color: white;
  border-radius: $cms-border-radius;
  font-size: 0.875rem;

  &__remove {
    background: none;
    border: none;
    color: white;
    cursor: pointer;
    font-size: 1.25rem;
    line-height: 1;
    padding: 0;
    margin-left: $cms-spacing-unit * 0.5;

    &:hover {
      opacity: 0.8;
    }
  }
}

.cms-filter-reset {
  display: flex;
  align-items: center;
  gap: $cms-spacing-unit * 0.5;
}
```

Don't forget to add to `cms.scss`:
```scss
@import 'contentfilter';
```

---

## Testing

### Unit Test Example

```java
@ExtendWith(MockitoExtension.class)
class ContentFilterTest {

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private TaxonomyService taxonomyService;

    @Test
    void testAssetsMode() {
        ContentFilter filter = new ContentFilter();
        // Set filterMode = "assets"
        filter.init();

        assertTrue(filter.isAssetsMode());
        assertFalse(filter.isWorkflowMode());
        assertFalse(filter.isSitesMode());

        List<FilterConfig> configs = filter.getFilterConfigs();
        assertEquals(3, configs.size());
        assertEquals("filter-asset-type", configs.get(0).getId());
        assertEquals("filter-asset-size", configs.get(1).getId());
        assertEquals("filter-modified", configs.get(2).getId());
    }

    @Test
    void testWorkflowMode() {
        ContentFilter filter = new ContentFilter();
        // Set filterMode = "workflow"
        filter.init();

        assertTrue(filter.isWorkflowMode());
        assertEquals(3, filter.getFilterConfigs().size());
    }
}
```

---

## Migration Notes

### From Previous ContentFilter Implementation

If upgrading from an older version:

1. **Update HTL templates** to use new mode-specific checks:
   ```html
   <!-- OLD -->
   <sly data-sly-test="${filter.workflowMode}">
   
   <!-- NEW -->
   <sly data-sly-test="${filter.isWorkflowMode}">
   ```

2. **Update component properties**:
   ```xml
   <!-- Add filterMode property -->
   filterMode="assets"
   ```

3. **CSS classes** should use semantic `cms-*` prefixes as per coding guidelines.

---

## Related Documentation

- [ContentFilter Component Documentation](/docs/contentfilter-component.md)
- [JSP to HTL Migration Guide](/docs/jsp-to-htl-migration.md)
- [CSS Framework Abstraction Guidelines](/.github/copilot-instructions.md#css-framework-abstraction)

---

**Last Updated**: January 10, 2026
**Component Version**: 1.1.9-SNAPSHOT
