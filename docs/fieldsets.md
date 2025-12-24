# Fieldsets Component

The Fieldsets component provides collapsible sections in editor dialogs, allowing you to organize form fields into logical groups that can be expanded or collapsed by users.

## Overview

- **Component Path**: `sling-cms/components/editor/fields/fieldsets`
- **Use Case**: Organizing complex forms with multiple sections
- **UI Pattern**: Bulma-style collapsible boxes with chevron icons
- **State Persistence**: Remembers collapsed/expanded state in localStorage

## Features

✅ **Collapsible sections** - Click to expand/collapse field groups  
✅ **Persistent state** - Remembers user's collapsed/expanded preferences  
✅ **Keyboard accessible** - Tab + Enter/Space to toggle  
✅ **ARIA compliant** - Screen reader friendly  
✅ **Visual feedback** - Animated chevron icons and hover effects  
✅ **Dynamic loading** - Works in modals and AJAX-loaded content  

## Basic Usage

### Structure

```json
{
    "jcr:primaryType": "nt:unstructured",
    "sling:resourceType": "sling-cms/components/editor/slingform",
    "button": "Save",
    "fields": {
        "jcr:primaryType": "nt:unstructured",
        "sling:resourceType": "sling-cms/components/general/container",
        "fieldsets": {
            "jcr:primaryType": "nt:unstructured",
            "sling:resourceType": "sling-cms/components/editor/fields/fieldsets",
            "section1": {
                "jcr:primaryType": "nt:unstructured",
                "title": "Basic Information",
                "field1": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/text",
                    "label": "Name",
                    "name": "name"
                }
            },
            "section2": {
                "jcr:primaryType": "nt:unstructured",
                "title": "Advanced Settings",
                "field2": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/textarea",
                    "label": "Description",
                    "name": "description"
                }
            }
        }
    }
}
```

### Key Points

1. **Container Structure**: Fieldsets must be a direct child of a `container` component
2. **Child Nodes**: Each child node of `fieldsets` becomes a collapsible section
3. **Title Property**: Each section should have a `title` property for the header label
4. **Field Organization**: Fields are organized as child nodes under each section

## Complete Example

Here's a real-world example from an author component:

```json
{
    "jcr:primaryType": "nt:unstructured",
    "sling:resourceType": "sling-cms/components/editor/slingform",
    "button": "Save Author",
    "fields": {
        "jcr:primaryType": "nt:unstructured",
        "sling:resourceType": "sling-cms/components/general/container",
        "fieldsets": {
            "jcr:primaryType": "nt:unstructured",
            "sling:resourceType": "sling-cms/components/editor/fields/fieldsets",
            
            "authorInfo": {
                "jcr:primaryType": "nt:unstructured",
                "title": "Author Details",
                "name": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/text",
                    "label": "Author Name",
                    "name": "authorName",
                    "required": true
                },
                "jobTitle": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/text",
                    "label": "Job Title",
                    "name": "jobTitle"
                },
                "bio": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/textarea",
                    "label": "Biography",
                    "name": "bio"
                }
            },
            
            "socialProfiles": {
                "jcr:primaryType": "nt:unstructured",
                "title": "Social Links",
                "socialLinks": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/multifield",
                    "label": "Social Links",
                    "name": "socialLinks",
                    "template": {
                        "jcr:primaryType": "nt:unstructured",
                        "platform": {
                            "jcr:primaryType": "nt:unstructured",
                            "sling:resourceType": "sling-cms/components/editor/fields/select",
                            "label": "Platform",
                            "name": "platform"
                        },
                        "url": {
                            "jcr:primaryType": "nt:unstructured",
                            "sling:resourceType": "sling-cms/components/editor/fields/text",
                            "label": "URL",
                            "name": "url"
                        }
                    }
                }
            },
            
            "advancedSettings": {
                "jcr:primaryType": "nt:unstructured",
                "title": "Advanced Settings",
                "enabled": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/checkbox",
                    "label": "Enable Author Profile",
                    "name": "enabled"
                }
            }
        }
    }
}
```

## Visual Appearance

### Expanded State
```
┌─────────────────────────────────────┐
│ ▼  Author Details                   │  ← Clickable header (light gray)
├─────────────────────────────────────┤
│ Author Name: [_________________]     │
│ Job Title:   [_________________]     │  ← Form fields
│ Biography:   [_________________]     │
│              [_________________]     │
└─────────────────────────────────────┘
```

### Collapsed State
```
┌─────────────────────────────────────┐
│ ▶  Author Details                   │  ← Clickable header (lighter gray)
└─────────────────────────────────────┘
```

## Properties

### Fieldsets Component
| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `sling:resourceType` | String | Yes | Must be `sling-cms/components/editor/fields/fieldsets` |

### Individual Fieldset (Child Nodes)
| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `title` | String | Recommended | The header text shown for the section. If not provided, defaults to "Section N" |

## Best Practices

### ✅ Do:
- Group related fields together logically
- Use descriptive titles that clearly indicate the section's purpose
- Put frequently used fields in the first (top) section
- Keep sections focused - don't mix unrelated fields
- Use fieldsets for forms with 10+ fields or 3+ logical groups

### ❌ Don't:
- Create sections with only 1-2 fields (not worth the complexity)
- Use overly long titles
- Nest fieldsets inside other fieldsets (not supported)
- Forget the `title` property (it will show as "Section 1", "Section 2", etc.)

## Working with Multifields

Fieldsets work great with multifields for complex, repeatable data:

```json
"contactInfo": {
    "title": "Contact Information",
    "phoneNumbers": {
        "sling:resourceType": "sling-cms/components/editor/fields/multifield",
        "label": "Phone Numbers",
        "name": "phoneNumbers",
        "template": {
            "type": { /* ... */ },
            "number": { /* ... */ }
        }
    }
}
```

## Debugging

If fieldsets aren't working:

1. **Check the browser console** (in iframe if dialog is in iframe):
   - Look for `[FIELDSETS]` debug logs
   - Verify `Found headers: X` shows the correct count

2. **Verify HTML structure**:
   ```javascript
   // In browser console (or iframe console)
   document.querySelectorAll('.editor-fieldset__header[data-toggle]')
   ```
   Should return elements equal to the number of sections.

3. **Check localStorage**:
   ```javascript
   // View saved states
   Object.keys(localStorage).filter(k => k.startsWith('cms-fieldset-'))
   ```

4. **Clear saved state** (if sections won't expand):
   ```javascript
   Object.keys(localStorage)
     .filter(k => k.startsWith('cms-fieldset-'))
     .forEach(k => localStorage.removeItem(k));
   location.reload();
   ```

## Technical Implementation

### HTML Structure
```html
<div class="editor-fieldsets">
  <div class="box editor-fieldset" data-fieldset-id="...">
    <div class="editor-fieldset__header" data-toggle="...">
      <span class="icon"><i class="jam jam-chevron-down"></i></span>
      <strong class="editor-fieldset__title">Section Title</strong>
    </div>
    <div class="editor-fieldset__content" id="...">
      <!-- Fields here -->
    </div>
  </div>
</div>
```

### JavaScript Features
- **Pure vanilla JS** - No framework dependencies
- **Event delegation** - Handles dynamically loaded content
- **localStorage persistence** - Per-fieldset state storage
- **MutationObserver** - Auto-initializes on dynamic content
- **Keyboard navigation** - Full ARIA support

### Styling
- Uses Bulma's `.box` component for consistent styling
- Chevron icons from Jam Icons
- Smooth CSS transitions for collapse/expand animations
- Responsive design with mobile optimizations

## Comparison with Tabs

| Feature | Fieldsets | Tabs |
|---------|-----------|------|
| **Purpose** | Organize groups of fields | Switch between mutually exclusive views |
| **Visibility** | Multiple sections can be open | Only one tab visible at a time |
| **State** | Individual sections remembered | Active tab remembered |
| **Use Case** | Long forms with logical sections | Different aspects of same content |
| **Example** | "Basic Info" + "Advanced Settings" | "Content" / "Metadata" / "Permissions" |

## Related Components

- **Tabs**: `sling-cms/components/editor/fields/tabs` - For mutually exclusive sections
- **Container**: `sling-cms/components/general/container` - Required parent for fieldsets
- **Multifield**: `sling-cms/components/editor/fields/multifield` - Repeatable field groups (works inside fieldsets)

## Migration from Legacy Implementation

If upgrading from an older version with button-based fieldsets:

1. The new implementation uses `<div>` headers instead of `<button>` elements
2. No changes needed to your `.json` configuration files
3. State is preserved in localStorage (same keys)
4. Visual appearance is improved with Bulma styling

## See Also

- [Editor Field Types](editor-field-types.md)
- [Dialog Tabs](dialog-tabs.md)
- [Multifield Component](multifield.md)
