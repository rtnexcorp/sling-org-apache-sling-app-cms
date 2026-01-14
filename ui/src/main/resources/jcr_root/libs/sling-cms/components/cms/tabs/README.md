# Generic Tabs Component

A reusable tab component for organizing content into multiple panels with tab navigation.

## Features

- Generic and reusable across different contexts
- Icon support for tabs (using Jam Icons)
- Flexible content loading via Sling resources
- Framework-agnostic styling with BEM methodology
- Full-height and compact variants
- Smooth scrolling with custom scrollbar styling

## Usage

### Basic Usage (Static Tabs in HTL)

```html
<div class="cms-tabs">
    <div class="tabs is-boxed">
        <ul>
            <li class="is-active" data-tab="tab1">
                <a>
                    <span class="icon"><em class="jam jam-document"></em></span>
                    <span>Tab 1</span>
                </a>
            </li>
            <li data-tab="tab2">
                <a>
                    <span class="icon"><em class="jam jam-cogs"></em></span>
                    <span>Tab 2</span>
                </a>
            </li>
        </ul>
    </div>

    <div class="cms-tabs__content">
        <div class="cms-tabs__panel is-active" data-panel="tab1">
            <sly data-sly-resource="${'tab1content' @ resourceType='your/component'}"/>
        </div>
        <div class="cms-tabs__panel" data-panel="tab2">
            <sly data-sly-resource="${'tab2content' @ resourceType='your/component'}"/>
        </div>
    </div>
</div>
```

### With Sling Model (Dynamic Tabs)

Create a content structure:
```
/content/yourpage/tabs
    - sling:resourceType = sling-cms/components/cms/tabs
    + tabs/
        + tab1
            - id = "definitions"
            - title = "Definitions"
            - icon = "document"
            - resourceName = "definitionstab"
            - resourceType = "your/component/path"
        + tab2
            - id = "instances"
            - title = "Instances"
            - icon = "cogs"
            - resourceName = "instancestab"
            - resourceType = "your/component/path"
```

Include in HTL:
```html
<sly data-sly-resource="${'tabs' @ resourceType='sling-cms/components/cms/tabs'}"/>
```

## Modifiers

### Full Height Tabs
For dashboard-style layouts that take up remaining viewport height:
```html
<div class="cms-tabs cms-tabs--full-height">
    <!-- tabs content -->
</div>
```

### Compact Tabs
For tabs with less padding:
```html
<div class="cms-tabs cms-tabs--compact">
    <!-- tabs content -->
</div>
```

## CSS Classes

| Class | Purpose |
|-------|---------|
| `.cms-tabs` | Main container |
| `.cms-tabs__content` | Content area container |
| `.cms-tabs__panel` | Individual tab panel |
| `.cms-tabs--full-height` | Modifier for full viewport height |
| `.cms-tabs--compact` | Modifier for compact padding |

## JavaScript API

The component automatically initializes via the `cms.tabs.js` script. No manual initialization required.

## Examples

### Workflow Dashboard Tabs
See: `/libs/sling-cms/components/cms/workflow/dashboardtabs/`

### Simple Content Tabs
```html
<div class="cms-tabs cms-tabs--compact">
    <div class="tabs is-boxed">
        <ul>
            <li class="is-active" data-tab="overview">
                <a><span>Overview</span></a>
            </li>
            <li data-tab="details">
                <a><span>Details</span></a>
            </li>
        </ul>
    </div>
    <div class="cms-tabs__content">
        <div class="cms-tabs__panel is-active" data-panel="overview">
            <p>Overview content...</p>
        </div>
        <div class="cms-tabs__panel" data-panel="details">
            <p>Detailed content...</p>
        </div>
    </div>
</div>
```

## Framework Abstraction

The component uses framework-agnostic CSS classes (`cms-tabs`, `cms-tabs__content`, etc.) to minimize impact when upgrading or switching CSS frameworks. All Bulma-specific styles are isolated to the SCSS layer and can be easily swapped out.
