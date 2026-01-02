# AI-Enhanced Blog Post Component

## Overview

A comprehensive reference component demonstrating **all four types** of AI-powered fields in Apache Sling CMS: AI Text, AI Textarea, AI Richtext, and AI Select. This component showcases intelligent content generation across different field types.

## Component Details

**Location:** `/apps/reference/components/general/ai-blog-post`

**Sling Model:** `org.apache.sling.cms.reference.models.AiBlogPostModel`

**Files:**
- `.content.xml` - Component metadata (JCR node definition)
- `edit.json` - Editor form with AI-enhanced fields
- `ai-blog-post.html` - HTL rendering template using Sling Model
- `README.md` - This documentation file

## AI-Enhanced Fields

This component showcases **all four types** of AI-powered fields:

### 1. Title Field (AI Text Input)
```json
{
  "sling:resourceType": "sling-cms/components/editor/fields/ai/text",
  "aiSuggestionType": "title",
  "contentSource": "content"
}
```
- **Field Type**: Single-line text input
- **AI Function**: Generates compelling page titles (50-70 characters)
- **Use Case**: Blog post titles, article headlines

### 2. Excerpt Field (AI Textarea)
```json
{
  "sling:resourceType": "sling-cms/components/editor/fields/ai/textarea",
  "aiSuggestionType": "excerpt",
  "contentSource": "content",
  "rows": 4
}
```
- **Field Type**: Multi-line textarea
- **AI Function**: Creates longer excerpts/summaries (150-300 words)
- **Use Case**: Article excerpts, detailed summaries

### 3. Introduction Field (AI Richtext Editor)
```json
{
  "sling:resourceType": "sling-cms/components/editor/fields/ai/richtext",
  "aiSuggestionType": "description",
  "contentSource": "content"
}
```
- **Field Type**: WYSIWYG rich text editor
- **AI Function**: Generates formatted introductions with HTML
- **Use Case**: Engaging introductions, formatted descriptions

### 4. Category Field (AI Select Dropdown)
```json
{
  "sling:resourceType": "sling-cms/components/editor/fields/ai/select",
  "aiSuggestionType": "category",
  "contentSource": "content"
}
```
- **Field Type**: Dropdown select
- **AI Function**: Suggests best matching category from predefined options
- **Use Case**: Auto-categorization, content classification
- **Options**: Technology, Business, Design, Development, Marketing, AI, CMS, Tutorial, News, Other

### 5. Meta Description Field (AI Text Input)
```json
{
  "sling:resourceType": "sling-cms/components/editor/fields/ai/text",
  "aiSuggestionType": "metaDescription",
  "contentSource": "content"
}
```
- **Field Type**: Single-line text input
- **AI Function**: SEO-optimized meta descriptions (150-160 characters)
- **Use Case**: Search engine snippets, social media previews

## Sample Page

**URL:** `http://localhost:8082/content/reference/en/ai-blog-sample.html`

The sample page includes:
- 2 pre-populated blog post examples
- Instructions for using AI suggestions
- Configuration guidance
- Links to detailed documentation

## Usage Instructions

### For Content Authors:

1. **Navigate to the sample page:**
   - Go to: `/content/reference/en/ai-blog-sample.html`
   
2. **Edit a blog post:**
   - Click the edit icon on any blog post component
   
3. **Fill in the content:**
   - Write your blog post content in the rich text editor first
   
4. **Use AI Suggestions:**
   - Click the ✨ "AI Suggest" button next to:
     - Title field (AI Text)
     - Excerpt field (AI Textarea)
     - Introduction field (AI Richtext)
     - Category field (AI Select)
     - Meta Description field (AI Text)
   
5. **Review and save:**
   - Review the AI-generated suggestions
   - Edit as needed
   - Save the component

### For Developers:

**Using the component in templates:**
```json
{
  "jcr:primaryType": "nt:unstructured",
  "sling:resourceType": "reference/components/general/ai-blog-post",
  "title": "My Blog Post",
  "content": "<p>Main content here...</p>"
}
```

**Adding to page via CMS:**
1. Edit page in CMS
2. Add component → Reference → AI-Enhanced Blog Post
3. Configure fields
4. Use AI suggestions as needed

## Component Fields

| Field | Type | Required | AI-Enhanced | AI Type | Description |
|-------|------|----------|-------------|---------|-------------|
| Title | Text | Yes | ✅ AI Text | title | Blog post title |
| Excerpt | Textarea | Yes | ✅ AI Textarea | excerpt | Longer summary/excerpt |
| Author | Text | Yes | ❌ | - | Author name |
| Publish Date | Date | Yes | ❌ | - | Publication date |
| Content | Rich Text | Yes | ❌ | - | Main blog content (source for AI) |
| Introduction | Rich Text | No | ✅ AI Richtext | description | AI-generated formatted introduction |
| Meta Description | Text | No | ✅ AI Text | metaDescription | SEO meta description |
| Category | Select | Yes | ✅ AI Select | category | Auto-categorization with 10 options |
| Tags | Text | No | ❌ | - | Comma-separated tags |

## Rendering

The component renders as a styled article with:
- Header with title, author, date, and tags
- Summary box (highlighted)
- Main content area
- Embedded meta description for SEO

## CSS Styling

Custom styles included in the HTL template:
- `.blog-post` - Main article container
- `.blog-post-header` - Title and metadata
- `.blog-post-meta` - Author/date display
- `.blog-post-summary` - Highlighted summary box
- `.blog-post-content` - Main content area
- `.tags` - Tag display

## Testing

1. **Deploy:** Already deployed via Maven
2. **Configure AI:** Set up at least one AI service (see main docs)
3. **Access:** Visit `/content/reference/en/ai-blog-sample.html`
4. **Edit:** Click edit on any blog post
5. **Test AI:** Write content, then click AI Suggest buttons
6. **Verify:** Check that suggestions appear correctly

## Related Documentation

- [AI Suggest Field Documentation](/docs/ai-suggest-field.md)
- [AI Demo Page](/content/reference/en/ai-suggest-demo.html)
- [Editor Field Types](/docs/editor-field-types.md)

## Next Steps

1. Configure an AI service provider (OpenAI, Azure, Claude, or Ollama)
2. Test all four AI field types on the sample page
3. Create your own components using AI Text, Textarea, Richtext, or Select fields
4. Customize the blog post styling to match your design system
5. Explore combining multiple AI field types in your forms

## Notes

- AI suggestions require at least one configured AI service
- Content field must be filled before generating suggestions
- All AI-generated content should be reviewed before publishing
- The component uses the standard PageProperties model for rendering
