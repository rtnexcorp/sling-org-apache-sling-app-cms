# SCSS Best Practices - Apache Sling CMS

## Table of Contents
- [Variable System](#variable-system)
- [Creating New SCSS Files](#creating-new-scss-files)
- [Color Variables Reference](#color-variables-reference)
- [Component Patterns](#component-patterns)
- [Framework Abstraction](#framework-abstraction)

---

## Variable System

### Core Principle
**All new SCSS files MUST import and use `variables.scss` for colors, spacing, and other design tokens.**

### Purpose
- **Centralized Design System**: Single source of truth for colors, typography, spacing
- **Easy Rebranding**: Change colors globally by updating `variables.scss`
- **Framework Independence**: Abstract Bulma-specific values for easier framework migration
- **Consistency**: Ensure visual consistency across all components

---

## Creating New SCSS Files

### Required Steps

#### 1. Import Variables at the Top
```scss
@use 'variables' as *;

// Your component styles below
.my-component {
  color: $grey-dark;
  background: $white;
}
```

#### 2. Use Variables Instead of Hardcoded Colors
```scss
// ❌ WRONG: Hardcoded colors
.my-button {
  background: #3273dc;
  color: #ffffff;
  border: 1px solid #dbdbdb;
}

// ✅ CORRECT: Using variables
.my-button {
  background: $link;
  color: $white;
  border: 1px solid $grey-lighter;
}
```

#### 3. Add Import to Main SCSS File
Edit `frontend/src/main/frontend/scss/cms.scss`:
```scss
@import 'mycomponent';  // Add your new file
```

#### 4. File Naming Convention
- Use lowercase with hyphens: `_my-component.scss`
- Prefix with underscore for partials: `_workflow-dashboard.scss`
- Use semantic names: `_content-filter.scss` not `_filter.scss`

---

## Color Variables Reference

### Grey Scale
```scss
$grey-darker: hsl(0, 0%, 21%);   // #363636 - Dark text, headers
$grey-dark: hsl(0, 0%, 29%);     // #4a4a4a - Body text
$grey: hsl(0, 0%, 48%);          // #7a7a7a - Secondary text, help text
$grey-light: hsl(0, 0%, 71%);    // #b5b5b5 - Placeholders
$grey-lighter: hsl(0, 0%, 86%);  // #dbdbdb - Borders, dividers
```

### White Variants
```scss
$white: hsl(0, 0%, 100%);        // #ffffff - Pure white
$white-bis: hsl(0, 0%, 98%);     // #fafafa - Off-white backgrounds
$white-ter: hsl(0, 0%, 96%);     // #f5f5f5 - Light backgrounds, code blocks
```

### Semantic Colors
```scss
$link: #3273dc;                  // Primary blue - links, primary actions
$link-hover: #2366d1;            // Hover state for links/buttons
$link-active: #1d5ec5;           // Active state for links/buttons
$success: #48c774;               // Success green - positive actions
$warning: #ffdd57;               // Warning yellow - cautions
$danger: #f14668;                // Danger red - errors, destructive actions
```

### Message/Notification Colors
```scss
// Light backgrounds for messages
$info-light: #eff5fb;            // Info message background
$success-light: #effaf3;         // Success message background
$warning-light: #fffbeb;         // Warning message background
$danger-light: #feecf0;          // Error message background
$suspended-bg: #fff4e6;          // Suspended state background

// Text colors for messages
$info-text: #1d72aa;             // Info text
$success-text: #257942;          // Success text
$warning-text: #947600;          // Warning text
$danger-text: #cc0f35;           // Error text
```

### Priority Badge Colors
```scss
$priority-high-bg: #ffe5e5;      // High priority background
$priority-high-text: #cc0000;    // High priority text
$priority-normal-bg: #e8f4fd;    // Normal priority background
$priority-normal-text: $link;    // Normal priority text
$priority-low-bg: #e8f8e8;       // Low priority background
$priority-low-text: #23d160;     // Low priority text
```

### UI Components
```scss
$scrollbar-track: #f1f1f1;       // Scrollbar track background
$scrollbar-thumb: #c1c1c1;       // Scrollbar thumb
$scrollbar-thumb-hover: #a8a8a8; // Scrollbar thumb on hover
```

### Data Visualization (Stat Cards)
```scss
$stat-color-1: #667eea;          // Purple gradient start
$stat-color-1-dark: #764ba2;     // Purple gradient end

$stat-color-2: #11998e;          // Teal gradient start
$stat-color-2-light: #38ef7d;    // Teal gradient end

$stat-color-3: #f5576c;          // Pink gradient start
$stat-color-3-light: #f093fb;    // Pink gradient end

$stat-color-4: #4facfe;          // Blue gradient start
$stat-color-4-light: #00f2fe;    // Blue gradient end
```

### Code Highlighting
```scss
$code-color: #da1e28;            // Primary code highlight (red)
$code-color-alt: #e91e63;        // Alternative code highlight (pink)
```

---

## Component Patterns

### Message/Notification Components
```scss
.cms-message {
  padding: 0.75rem 1rem;
  border-radius: 4px;

  &--info {
    background: $info-light;
    color: $info-text;
    border-left: 4px solid $link;
  }

  &--success {
    background: $success-light;
    color: $success-text;
    border-left: 4px solid $success;
  }

  &--warning {
    background: $warning-light;
    color: $warning-text;
    border-left: 4px solid $warning;
  }

  &--error {
    background: $danger-light;
    color: $danger-text;
    border-left: 4px solid $danger;
  }
}
```

### Form Components
```scss
.cms-form-input {
  border: 1px solid $grey-lighter;
  color: $grey-dark;
  background: $white;

  &:focus {
    border-color: $link;
    box-shadow: 0 0 0 0.125em rgba(50, 115, 220, 0.25);
  }

  &::placeholder {
    color: $grey-light;
  }
}

.cms-form-help {
  color: $grey;
  font-size: 0.8125rem;
}
```

### Button States
```scss
.cms-button {
  &--primary {
    background: $link;
    color: $white;
    border-color: $link;

    &:hover {
      background: $link-hover;
      border-color: $link-hover;
    }

    &:active {
      background: $link-active;
      border-color: $link-active;
    }
  }
}
```

### Custom Scrollbars
```scss
.my-scrollable-component {
  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-track {
    background: $scrollbar-track;
  }

  &::-webkit-scrollbar-thumb {
    background: $scrollbar-thumb;
    border-radius: 4px;

    &:hover {
      background: $scrollbar-thumb-hover;
    }
  }
}
```

---

## Framework Abstraction

### Why Abstraction Matters
Apache Sling CMS currently uses **Bulma CSS** and **Jam Icons**. By abstracting framework-specific classes and values into semantic variables, we can:
- Switch CSS frameworks with minimal code changes
- Upgrade Bulma versions easily
- Replace icon libraries without touching component HTL files
- Create custom themes by changing variables

### Best Practices

#### Use Semantic Class Names in HTL
```html
<!-- ❌ WRONG: Direct Bulma classes -->
<button class="button is-primary is-large">Save</button>

<!-- ✅ CORRECT: Semantic CMS classes -->
<button class="cms-button cms-button--primary cms-button--large">Save</button>
```

#### Map Framework Variables in SCSS
```scss
// In variables.scss - abstraction layer
$link: #3273dc;  // Maps to Bulma's $primary

// In component SCSS
.cms-button--primary {
  background: $link;  // Framework-agnostic
}
```

#### Abstract Grid Systems
```scss
// ❌ WRONG: Direct Bulma grid
<div class="columns">
  <div class="column is-half">...</div>
</div>

// ✅ CORRECT: Semantic grid
<div class="cms-grid">
  <div class="cms-grid__col-6">...</div>
</div>
```

---

## Checklist for New SCSS Files

- [ ] File named with underscore prefix: `_mycomponent.scss`
- [ ] First line: `@use 'variables' as *;`
- [ ] No hardcoded hex colors (use variables)
- [ ] No hardcoded spacing (use `$cms-spacing-unit` multiples)
- [ ] Semantic class names with `cms-` prefix
- [ ] BEM naming convention: `block__element--modifier`
- [ ] Added to `cms.scss` imports
- [ ] Tested build: `mvn clean install -P autoInstallBundle -pl frontend`
- [ ] Documented any new component patterns

---

## Examples from Codebase

### Good Examples
- `_workflow-dashboard.scss` - Complete variable usage, stat cards
- `_workflow-designer.scss` - Message components, scrollbars
- `_workflow-instances.scss` - Tables, badges, notifications
- `_workflow-startform.scss` - Forms, buttons, sections
- `_workflow-taskinbox.scss` - Tabs, cards, priority badges

### Pattern to Follow
```scss
@use 'variables' as *;

// ============================================
// Component Name
// ============================================

.cms-mycomponent {
  background: $white;
  border: 1px solid $grey-lighter;
  color: $grey-dark;
  padding: 1rem;
  
  &__header {
    background: $grey-darker;
    color: $white;
  }
  
  &__body {
    background: $white-ter;
  }
  
  &--variant {
    border-color: $link;
  }
}
```

---

## Common Mistakes to Avoid

### ❌ Don't Do This
```scss
// No @use statement
.my-component {
  color: #363636;  // Hardcoded color
  background: #fff;  // Hardcoded white
  border: 1px solid #dbdbdb;  // Hardcoded grey
}
```

### ✅ Do This Instead
```scss
@use 'variables' as *;

.cms-my-component {
  color: $grey-dark;
  background: $white;
  border: 1px solid $grey-lighter;
}
```

---

## Migration Guide

### Converting Existing Files
1. Add `@use 'variables' as *;` at the top
2. Search for hex colors: `/\#[0-9a-fA-F]{3,6}/`
3. Replace with appropriate variable from reference above
4. Test build and visual appearance
5. Commit with clear message: "refactor: use variables in _component.scss"

### Finding the Right Variable
| Hex Color | Variable | Use Case |
|-----------|----------|----------|
| `#ffffff` | `$white` | Backgrounds, text on dark |
| `#f5f5f5` | `$white-ter` | Light backgrounds, code blocks |
| `#363636` | `$grey-darker` | Dark text, headers |
| `#4a4a4a` | `$grey-dark` | Body text |
| `#7a7a7a` | `$grey` | Secondary text |
| `#b5b5b5` | `$grey-light` | Placeholders |
| `#dbdbdb` | `$grey-lighter` | Borders |
| `#3273dc` | `$link` | Primary blue |
| `#48c774` | `$success` | Success green |
| `#ffdd57` | `$warning` | Warning yellow |
| `#f14668` | `$danger` | Error red |

---

## Benefits of This Approach

✅ **Maintainability**: Change one variable, update entire application
✅ **Consistency**: Same colors everywhere, no visual discrepancies  
✅ **Flexibility**: Easy to create themes or rebrand
✅ **Framework Independence**: Not locked into Bulma forever
✅ **Documentation**: Variables are self-documenting (`$success-light` vs `#effaf3`)
✅ **Testability**: Easier to verify color compliance
✅ **Accessibility**: Centralized control for contrast ratios

---

## Related Documentation
- [Frontend UI Analysis](frontend-ui-analysis.md)
- [JSP to HTL Migration](jsp-to-htl-migration.md)
- [Copilot Instructions](../.github/copilot-instructions.md) - See SCSS/CSS Organization section

---

**Remember**: Every new SCSS file starts with `@use 'variables' as *;` and uses variables for all colors!
