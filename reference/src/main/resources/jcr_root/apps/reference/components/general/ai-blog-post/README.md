# AI-Enhanced Blog Post Component

## Overview

A reference component demonstrating the use of AI-powered text fields in Apache Sling CMS. This component uses a Sling Model for clean HTL rendering and leverages the text-ai field for intelligent content generation.

## Component Details

**Location:** `/apps/reference/components/general/ai-blog-post`

**Sling Model:** `org.apache.sling.cms.reference.models.AiBlogPostModel`

**Files:**
- `.content.xml` - Component metadata (JCR node definition)
- `edit.json` - Editor form with AI-enhanced fields
- `ai-blog-post.html` - HTL rendering template using Sling Model
- `README.md` - This documentation file

## AI-Enhanced Fields

The component includes three AI-powered fields:

### 1. Title Field
```json
{
  "sling:resourceType": "sling-cms/components/editor/fields/text-ai",
  "aiSuggestionType": "title",
  "contentSource": "content"
}
```
- Generates compelling page titles (50-70 characters)
- Analyzes the main content field

### 2. Summary Field
```json
{
  "sling:resourceType": "sling-cms/components/editor/fields/text-ai",
  "aiSuggestionType": "summary",
  "contentSource": "content"
}
```
- Creates concise summaries (150-200 words)
- Extracts key points from content

### 3. Meta Description Field
```json
{
  "sling:resourceType": "sling-cms/components/editor/fields/text-ai",
  "aiSuggestionType": "metaDescription",
  "contentSource": "content"
}
```
- Generates SEO-optimized meta descriptions (150-160 characters)
- Designed for search engine snippets

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
     - Title field
     - Summary field
     - Meta Description field
   
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

| Field | Type | Required | AI-Enhanced | Description |
|-------|------|----------|-------------|-------------|
| Title | Text | Yes | ✅ | Blog post title |
| Summary | Text | Yes | ✅ | Brief summary |
| Author | Text | Yes | ❌ | Author name |
| Publish Date | Date | Yes | ❌ | Publication date |
| Content | Rich Text | Yes | ❌ | Main blog content |
| Meta Description | Text | No | ✅ | SEO meta description |
| Tags | Text | No | ❌ | Comma-separated tags |

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
2. Test the AI suggestions on the sample page
3. Create your own components using the text-ai field
4. Customize the blog post styling to match your design system

## Notes

- AI suggestions require at least one configured AI service
- Content field must be filled before generating suggestions
- All AI-generated content should be reviewed before publishing
- The component uses the standard PageProperties model for rendering
