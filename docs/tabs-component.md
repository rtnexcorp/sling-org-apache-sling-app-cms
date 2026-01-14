# Generic Tabs Component

The generic tabs component provides a reusable, framework-agnostic solution for organizing content into tabbed panels. It replaces workflow-specific implementations with a flexible, maintainable component that can be used throughout the CMS.

## Overview

The tabs component consists of:
- **UI Component**: HTL template at `/libs/sling-cms/components/cms/tabs/`
- **Sling Model**: Java model for dynamic tab configuration
- **Styles**: Framework-agnostic SCSS in `_tabs.scss`
- **JavaScript**: Generic behavior in `cms.tabs.js`

## Design Principles

### Framework Abstraction
The component uses BEM naming (`cms-tabs`, `cms-tabs__content`) to abstract away framework-specific classes. This makes it easier to:
- Upgrade or switch CSS frameworks
- Maintain consistent styling
- Reduce coupling to Bulma or other frameworks

### Reusability
The component is designed to be reused in multiple contexts:
- Workflow dashboards
- Content editors
- Settings panels
- Any tabbed interface

### Flexibility
Supports both:
- **Static tabs**: Defined directly in HTL markup
- **Dynamic tabs**: Configured via content nodes and Sling Model

## Implementation Examples

### Example 1: Simple Static Tabs

```html
<div class="cms-tabs">
    <div class="tabs is-boxed">
        <ul>
            <li class="is-active" data-tab="general">
                <a>
                    <span class="icon"><em class="jam jam-cog"></em></span>
                    <span>General</span>
                </a>
            </li>
            <li data-tab="advanced">
                <a>
                    <span class="icon"><em class="jam jam-sliders-h"></em></span>
                    <span>Advanced</span>
                </a>
            </li>
        </ul>
    </div>

    <div class="cms-tabs__content">
        <div class="cms-tabs__panel is-active" data-panel="general">
            <!-- General settings content -->
        </div>
        <div class="cms-tabs__panel" data-panel="advanced">
            <!-- Advanced settings content -->
        </div>
    </div>
</div>
```

### Example 2: Full-Height Dashboard Tabs

```html
<div class="cms-tabs cms-tabs--full-height">
    <div class="tabs is-boxed">
        <ul>
            <li class="is-active" data-tab="overview">
                <a>
                    <span class="icon"><em class="jam jam-dashboard"></em></span>
                    <span>Overview</span>
                </a>
            </li>
            <li data-tab="analytics">
                <a>
                    <span class="icon"><em class="jam jam-pie-chart"></em></span>
                    <span>Analytics</span>
                </a>
            </li>
        </ul>
    </div>

    <div class="cms-tabs__content">
        <div class="cms-tabs__panel is-active" data-panel="overview">
            <sly data-sly-resource="${'overview' @ resourceType='your/dashboard'}"/>
        </div>
        <div class="cms-tabs__panel" data-panel="analytics">
            <sly data-sly-resource="${'analytics' @ resourceType='your/analytics'}"/>
        </div>
    </div>
</div>
```

### Example 3: Dynamic Tabs with Sling Model

Content structure in the repository:
```
/content/admin/settings
    - sling:resourceType = sling-cms/components/cms/tabs
    + tabs/
        + database
            - id = "database"
            - title = "Database"
            - icon = "database"
            - resourceName = "databasetab"
            - resourceType = "myapp/components/settings/database"
        + security
            - id = "security"
            - title = "Security"
            - icon = "shield"
            - resourceName = "securitytab"
            - resourceType = "myapp/components/settings/security"
```

HTL usage:
```html
<sly data-sly-resource="${resource @ resourceType='sling-cms/components/cms/tabs'}"/>
```

## CSS Modifiers

### `--full-height`
Creates a tab interface that takes up available viewport height with scrollable content area:
- Height: `calc(100vh - 200px)`
- Min-height: `500px`
- Scrollable content with custom scrollbar

Use case: Dashboard interfaces, monitoring tools

### `--compact`
Reduces padding in the content area:
- Padding: `1rem` instead of `1.5rem`

Use case: Nested tabs, tight layouts

## Migration Guide

### Migrating Workflow Tabs

**Before:**
```html
<div class="cms-workflow-dashboard-tabs">
    <div class="cms-workflow-dashboard-tabs__content">
        <div class="cms-workflow-dashboard-tabs__panel is-active" data-panel="tab1">
```

**After:**
```html
<div class="cms-tabs cms-tabs--full-height">
    <div class="cms-tabs__content">
        <div class="cms-tabs__panel is-active" data-panel="tab1">
```

### Steps to Migrate

1. Replace `cms-workflow-dashboard-tabs` with `cms-tabs cms-tabs--full-height`
2. Replace `cms-workflow-dashboard-tabs__content` with `cms-tabs__content`
3. Replace `cms-workflow-dashboard-tabs__panel` with `cms-tabs__panel`
4. Remove workflow-specific JavaScript imports
5. Keep the generic `cms.tabs.js` import

## JavaScript API

The component automatically initializes when the DOM loads. No manual setup required.

### Events

The component handles tab clicks internally. To add custom behavior:

```javascript
document.querySelectorAll('.cms-tabs li[data-tab]').forEach(tab => {
    tab.addEventListener('click', (e) => {
        const tabId = e.currentTarget.getAttribute('data-tab');
        console.log('Tab switched to:', tabId);
        // Custom logic here
    });
});
```

## Browser Support

- Modern browsers (Chrome, Firefox, Safari, Edge)
- Custom scrollbar styling: WebKit browsers only (graceful degradation)

## Accessibility

- Semantic HTML structure
- Keyboard navigation supported (native browser behavior)
- Consider adding ARIA attributes for enhanced accessibility:
  - `role="tablist"` on `<ul>`
  - `role="tab"` on `<li>`
  - `role="tabpanel"` on panels
  - `aria-selected` on active tab
  - `aria-hidden` on inactive panels

## Performance

- Lightweight: Minimal JavaScript footprint
- CSS-based show/hide (no JavaScript animations)
- Lazy resource loading: Content is only loaded when visible

## Future Enhancements

Potential improvements:
- Deep linking (URL hash support)
- Persistent tab state (localStorage)
- Animated transitions
- Vertical tab orientation
- Tab close buttons for dynamic tabs
- Drag-and-drop tab reordering

## Related Components

- Workflow Designer
- Settings Panels
- Content Editors
- Dashboard Components
