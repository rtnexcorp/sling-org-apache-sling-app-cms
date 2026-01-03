# AI-Enhanced Text Field Component

## Overview

The **Text Field with AI Suggestions** component provides intelligent content generation capabilities for page titles, summaries, and meta descriptions using AI services.

## Component Location

- **Path**: `/libs/sling-cms/components/editor/fields/ai/text`
- **Type**: HTL Component with Sling Model
- **Group**: SlingCMS-FieldConfig

## Features

- ✨ **AI-Powered Suggestions**: Generate titles, summaries, or meta descriptions with one click
- 🔄 **Multiple AI Providers**: Supports OpenAI, Azure OpenAI, Anthropic Claude, and Ollama
- 📝 **Context-Aware**: Uses page content to generate relevant suggestions
- 🎯 **Flexible Content Source**: Can pull content from specific fields or entire form
- ♿ **Accessible**: Full ARIA support and keyboard navigation

## Usage in Page Templates

### Basic Title Field with AI

```json
{
  "jcr:primaryType": "nt:unstructured",
  "sling:resourceType": "sling-cms/components/editor/fields/ai/text",
  "label": "Page Title",
  "name": "jcr:title",
  "required": true,
  "aiSuggestionType": "title",
  "contentSource": "jcr:description"
}
```

### Summary Field with AI

```json
{
  "jcr:primaryType": "nt:unstructured",
  "sling:resourceType": "sling-cms/components/editor/fields/ai/text",
  "label": "Summary",
  "name": "summary",
  "type": "text",
  "aiSuggestionType": "summary",
  "placeholder": "Enter or generate a summary",
  "help": "Click 'AI Suggest' to generate a summary from page content"
}
```

### Meta Description with AI

```json
{
  "jcr:primaryType": "nt:unstructured",
  "sling:resourceType": "sling-cms/components/editor/fields/ai/text",
  "label": "Meta Description",
  "name": "metaDescription",
  "aiSuggestionType": "metaDescription",
  "contentSource": "jcr:description",
  "placeholder": "SEO meta description",
  "help": "Recommended: 150-160 characters"
}
```

## Component Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `label` | String | No | Label text displayed above the field |
| `name` | String | Yes | Field name attribute (e.g., `jcr:title`) |
| `type` | String | No | HTML input type (default: `text`) |
| `aiSuggestionType` | String | Yes* | Type of AI suggestion: `title`, `summary`, or `metaDescription` |
| `contentSource` | String | No | Name of field to use as content source for AI |
| `placeholder` | String | No | Placeholder text |
| `help` | String | No | Help text displayed below field |
| `required` | Boolean | No | Whether field is required (default: false) |
| `disabled` | Boolean | No | Whether field is disabled (default: false) |

\* Required for AI functionality to be enabled

## AI Suggestion Types

### `title`
Generates a concise, engaging title (typically 50-60 characters) based on the content.

**Best for**: Page titles, article headlines, section headers

### `summary`
Creates a comprehensive summary (2-3 sentences) highlighting main points.

**Best for**: Page summaries, article abstracts, content descriptions

### `metaDescription`
Produces SEO-optimized meta descriptions (150-160 characters).

**Best for**: SEO meta descriptions, search result snippets

## Content Source Behavior

The `contentSource` property controls where the AI gets content to analyze:

1. **Specific Field**: If `contentSource` is set (e.g., `"jcr:description"`), AI uses that field's value
2. **All Form Fields**: If `contentSource` is empty/not set, AI uses all text inputs and textareas in the form
3. **Validation**: If no content is available, user sees an error message

## Example: Complete Page Template Configuration

```json
{
  "jcr:primaryType": "nt:unstructured",
  "sling:resourceType": "sling-cms/components/caconfig/template/config",
  "jcr:title": "Blog Post Template",
  "fields": {
    "jcr:primaryType": "nt:unstructured",
    "sling:resourceType": "sling-cms/components/general/container",
    
    "title": {
      "jcr:primaryType": "nt:unstructured",
      "sling:resourceType": "sling-cms/components/editor/fields/ai/text",
      "label": "Title",
      "name": "jcr:title",
      "required": true,
      "aiSuggestionType": "title",
      "contentSource": "description"
    },

    "description": {
      "jcr:primaryType": "nt:unstructured",
      "sling:resourceType": "sling-cms/components/editor/fields/textarea",
      "label": "Description",
      "name": "jcr:description",
      "required": true,
      "rows": 5
    },

    "summary": {
      "jcr:primaryType": "nt:unstructured",
      "sling:resourceType": "sling-cms/components/editor/fields/ai/text",
      "label": "Summary",
      "name": "summary",
      "aiSuggestionType": "summary",
      "help": "Auto-generated from description"
    },

    "metaDescription": {
      "jcr:primaryType": "nt:unstructured",
      "sling:resourceType": "sling-cms/components/editor/fields/ai/text",
      "label": "Meta Description",
      "name": "metaDescription",
      "aiSuggestionType": "metaDescription",
      "contentSource": "jcr:description",
      "help": "SEO meta description (150-160 chars)"
    }
  }
}
```

## Technical Architecture

### Sling Model
- **Class**: `org.apache.sling.cms.core.models.AiSuggestField`
- **Adaptables**: `Resource.class`, `SlingHttpServletRequest.class`
- **Package**: Exported via `Sling-Model-Packages` in `core/bnd.bnd`

### HTL Template
- **File**: `text-ai.html`
- **Features**: Semantic HTML, ARIA attributes, i18n support

### JavaScript
- **File**: `ui/src/main/frontend/js/cms.ai.js`
- **Framework**: Rava.js binding
- **Endpoint**: `/bin/cms/ai/suggest.json`

### SCSS Styling
- **File**: `frontend/src/main/frontend/scss/_ai-suggest.scss`
- **Framework**: Bulma CSS with abstraction layer
- **Classes**: `.cms-ai-suggest-field`, `.cms-ai-suggest-btn`

### Backend Servlet
- **Class**: `org.apache.sling.cms.ai.servlets.AiSuggestionServlet`
- **Path**: `/bin/cms/ai/suggest`
- **Method**: POST
- **Response**: JSON with `success`, `suggestion`, and `provider` fields

## AI Provider Configuration

The component automatically uses the best available AI service:

1. **External API Services** (priority):
   - OpenAI GPT-4/3.5
   - Azure OpenAI Service
   - Anthropic Claude
   - Ollama (local)

2. **Rules-Based Fallback** (if no external service available)

Configure AI providers via OSGi configuration at:
`http://localhost:8082/system/console/configMgr`

## Styling Customization

Override styles by creating custom SCSS:

```scss
// Custom AI button styling
.cms-ai-suggest-btn {
  background-color: your-color;
  
  &:hover {
    background-color: your-hover-color;
  }
}

// Custom AI icon
.cms-icon--ai::before {
  content: '🤖'; // Your custom icon
}
```

## Accessibility

The component follows WCAG 2.1 AA standards:

- ✓ Keyboard navigation support
- ✓ ARIA labels and attributes
- ✓ Focus indicators
- ✓ Screen reader announcements
- ✓ High contrast support

## Browser Support

- Chrome/Edge 90+
- Firefox 88+
- Safari 14+

## Troubleshooting

### AI Suggestion Button Not Appearing
- Verify `aiSuggestionType` is set to `title`, `summary`, or `metaDescription`
- Check component configuration in template

### "No content available" Error
- Ensure there's content in the form (either in `contentSource` field or other fields)
- Add placeholder content for testing

### AI Service Not Available
- Check OSGi configuration for AI providers
- Verify at least one AI service is enabled and configured
- Check logs: `tail -f crx-quickstart/logs/error.log`

### Suggestions Are Generic
- Provide more detailed content in source fields
- Use specific `contentSource` field instead of entire form
- Consider enabling external AI providers (OpenAI, Azure, Anthropic) for better quality

## Related Documentation

- [AI Module Documentation](./new-age-cms-ai.md)
- [Editor Field Types](./editor-field-types.md)
- [Templates Documentation](./templates.md)
- [JSP to HTL Migration](./jsp-to-htl-migration.md)
