# AI Taxonomy Field Component

## Overview

The AI Taxonomy Field is an enhanced taxonomy selection component that integrates AI-powered suggestions to help content authors automatically tag and categorize content based on its semantic meaning and context.

## Location

```
ui/src/main/resources/jcr_root/libs/sling-cms/components/editor/fields/ai/taxonomy/
├── taxonomy.html         # HTL template
├── edit.json            # Configuration dialog
└── README.md            # Documentation
```

## Features

- **Taxonomy Selection**: Multi-select taxonomy items with autocomplete
- **AI-Powered Suggestions**: Automatically suggest relevant taxonomy terms based on content analysis
- **Multiple AI Modes**:
  - Content-based: Analyzes existing content to suggest taxonomies
  - Semantic Analysis: Uses semantic understanding for taxonomy suggestions
  - Context-aware: Considers page context and related content
- **Flexible Configuration**: Customizable taxonomy base paths and suggestion sources
- **User-Friendly Interface**: Tag-based UI with easy add/remove functionality

## Usage

### Adding to a Component Dialog

```json
{
  "taxonomy": {
    "jcr:primaryType": "nt:unstructured",
    "sling:resourceType": "sling-cms/components/editor/fields/ai/taxonomy",
    "label": "Tags",
    "name": "cq:tags",
    "aiSuggestionType": "content-taxonomy",
    "contentSource": "jcr:description",
    "placeholder": "Select tags...",
    "required": true,
    "help": "Select or AI-generate tags for this content"
  }
}
```

### Configuration Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `label` | String | Yes | Display label for the field |
| `name` | String | Yes | Property name to store taxonomy paths |
| `basePath` | String | No | Base path for taxonomy items (defaults to site settings) |
| `aiSuggestionType` | String | No | AI suggestion mode: `content-taxonomy`, `semantic-taxonomy`, `context-taxonomy`, or empty to disable |
| `contentSource` | String | No | Field name to analyze for suggestions (e.g., `jcr:title`, `jcr:description`) |
| `placeholder` | String | No | Placeholder text for input (default: "Select taxonomy...") |
| `required` | Boolean | No | Whether field is required |
| `help` | String | No | Help text shown below field |

## AI Suggestion Modes

### Content-based (`content-taxonomy`)
Analyzes the content of specified fields to suggest relevant taxonomy terms based on keywords and topics.

### Semantic Analysis (`semantic-taxonomy`)
Uses semantic understanding and natural language processing to identify concepts and themes for taxonomy suggestions.

### Context-aware (`context-taxonomy`)
Considers the page context, site structure, and related content to suggest contextually appropriate taxonomies.

## Technical Architecture

### Sling Model
- **Class**: `org.apache.sling.cms.core.models.AiTaxonomyField`
- **Adaptable**: `SlingHttpServletRequest`
- **Location**: `core/src/main/java/org/apache/sling/cms/core/models/AiTaxonomyField.java`

The model provides:
- Field configuration properties
- Selected taxonomy items with titles
- Available taxonomy options from repository
- Multifield context support

### Frontend Assets

#### JavaScript
- **File**: `frontend/src/main/frontend/js/cms.ai-taxonomy.js`
- **Features**:
  - Add/remove taxonomy items
  - AI suggestion handling
  - Enter key support for quick add
  - Event delegation for dynamic dialogs

#### SCSS
- **File**: `frontend/src/main/frontend/scss/_ai-taxonomy.scss`
- **Styles**:
  - AI suggestion button styling
  - Tag-based item display
  - Responsive layout
  - Animation effects

### HTL Template
- **File**: `taxonomy.html`
- **Pattern**: Uses Sling Model for all data
- **Features**:
  - Framework-agnostic semantic classes
  - Accessible markup
  - Progressive enhancement

## Data Storage

Taxonomy selections are stored as a multi-value String array property containing the JCR paths of selected taxonomy items:

```
{
  "cq:tags": [
    "/content/taxonomies/topics/technology",
    "/content/taxonomies/topics/ai",
    "/content/taxonomies/categories/tutorial"
  ]
}
```

## AI Endpoint

The component expects an AI suggestion endpoint at:

```
POST /bin/cms/ai/suggest-taxonomy.json

Request:
- type: AI suggestion type (content-taxonomy, semantic-taxonomy, context-taxonomy)
- content: Content to analyze
- taxonomyBase: (optional) Base path for taxonomies

Response:
{
  "success": true,
  "suggestions": [
    {
      "path": "/content/taxonomies/topics/ai",
      "title": "Artificial Intelligence",
      "confidence": 0.95
    }
  ],
  "provider": "OpenAI GPT-4"
}
```

## Integration with Existing Taxonomy System

The AI Taxonomy Field integrates seamlessly with Sling CMS's existing taxonomy infrastructure:

- Queries `sling:Taxonomy` nodes from the repository
- Uses site settings for default taxonomy root path
- Displays taxonomy titles from `jcr:title` property
- Stores selections as JCR paths compatible with other taxonomy components

## Examples

### Simple Taxonomy Selection
```json
{
  "tags": {
    "sling:resourceType": "sling-cms/components/editor/fields/ai/taxonomy",
    "label": "Tags",
    "name": "tags"
  }
}
```

### With AI Content Analysis
```json
{
  "tags": {
    "sling:resourceType": "sling-cms/components/editor/fields/ai/taxonomy",
    "label": "Tags",
    "name": "tags",
    "aiSuggestionType": "content-taxonomy",
    "contentSource": "jcr:description",
    "help": "Select tags or click 'AI Suggest' to auto-generate"
  }
}
```

### With Custom Taxonomy Base
```json
{
  "categories": {
    "sling:resourceType": "sling-cms/components/editor/fields/ai/taxonomy",
    "label": "Categories",
    "name": "categories",
    "basePath": "/content/taxonomies/categories",
    "aiSuggestionType": "semantic-taxonomy"
  }
}
```

## Browser Support

- Modern browsers (Chrome, Firefox, Safari, Edge)
- Requires JavaScript enabled
- Uses native HTML5 `<datalist>` for autocomplete
- Graceful degradation for non-JS scenarios

## Accessibility

- Semantic HTML markup
- ARIA labels and roles
- Keyboard navigation support (Enter to add, Tab navigation)
- Screen reader friendly
- Color contrast compliant

## Future Enhancements

- [ ] Batch taxonomy operations
- [ ] Taxonomy hierarchy visualization
- [ ] Custom taxonomy creation from AI suggestions
- [ ] Confidence score display for AI suggestions
- [ ] Multi-language taxonomy support
- [ ] Taxonomy analytics and usage tracking

## Related Documentation

- [AI Suggest Field Documentation](../../../../../../../../../docs/ai-suggest-field.md)
- [Taxonomy Management](../../../../../../../../../docs/managing-taxonomy.md)
- [Editor Field Types](../../../../../../../../../docs/editor-field-types.md)
