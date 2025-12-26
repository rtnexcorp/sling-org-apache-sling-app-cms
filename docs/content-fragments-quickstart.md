# Quick Start: Using Content Fragments in Pages

## Step 1: Create a Content Fragment

1. Go to **CMS** → **Manage** → **Content Fragments**
2. Click **+ Content Fragment**
3. Fill in your content (e.g., Article with title, author, content)
4. Save - your fragment is now at `/content/fragments/my-article`

## Step 2: Display Fragment in a Page (3 Ways)

### Option A: Use the ContentFragment Component (Easiest)

1. Edit any page in the CMS
2. Add component: **Reference - General** → **Content Fragment**
3. Configure the `fragmentPath` property to `/content/fragments/my-article`
4. Save and view the page

### Option B: Custom Component (More Control)

Create a new HTL file anywhere in your site (e.g., `article-display.html`):

```html
<sly data-sly-use.fragment="${'org.apache.sling.cms.reference.models.ContentFragmentModel' @ fragmentPath='/content/fragments/my-article'}">
    <article data-sly-test="${fragment.isLoaded}">
        <h1>${fragment.properties.title}</h1>
        <p class="byline">By ${fragment.properties.author}</p>
        <div>${fragment.properties.content @ context='html'}</div>
    </article>
</sly>
```

### Option C: Include in Existing Component

Add to any existing HTL component:

```html
<!-- Your existing component code -->
<div class="sidebar">
    <!-- Load and display a featured article fragment -->
    <sly data-sly-use.featured="${'org.apache.sling.cms.reference.models.ContentFragmentModel' @ fragmentPath='/content/fragments/featured-article'}">
        <div class="featured" data-sly-test="${featured.isLoaded}">
            <h3>Featured: ${featured.properties.title}</h3>
            <p>${featured.properties.summary}</p>
            <a href="/articles/${featured.properties.slug}.html">Read more →</a>
        </div>
    </sly>
</div>
```

## Step 3: Access as JSON API

Get your fragment as JSON for external systems:

```bash
# Create the servlet first (see docs/using-content-fragments.md)
# Then access:
curl http://localhost:8080/content/fragments/my-article.api.json

# Returns:
{
  "title": "My Article Title",
  "author": "John Doe",
  "content": "<p>Article content...</p>",
  "schemaId": "article",
  "_path": "/content/fragments/my-article"
}
```

## Common Property Access Patterns

```html
<sly data-sly-use.fragment="${'org.apache.sling.cms.reference.models.ContentFragmentModel' @ fragmentPath=properties.fragmentPath}">
    
    <!-- Simple property -->
    ${fragment.properties.title}
    
    <!-- Get property as string -->
    ${fragment.getString('author')}
    
    <!-- Check if loaded -->
    <div data-sly-test="${fragment.isLoaded}">...</div>
    
    <!-- Get schema ID -->
    ${fragment.schemaId}
    
    <!-- Loop through all properties -->
    <sly data-sly-list.prop="${fragment.properties}">
        ${prop.key}: ${prop.value}
    </sly>
    
    <!-- HTML content (with proper context) -->
    ${fragment.properties.content @ context='html'}
    
    <!-- Date formatting -->
    ${fragment.properties.publishDate @ format='yyyy-MM-dd'}
</sly>
```

## Real-World Example: Blog Article

### 1. Create Schema

- **title** (STRING, required)
- **author** (STRING, required)
- **publishDate** (DATE, required)
- **summary** (TEXT, required)
- **content** (TEXT, required)
- **tags** (STRING, multivalue)

### 2. Create Fragment

Path: `/content/fragments/articles/getting-started`

### 3. Display on Page

```html
<sly data-sly-use.article="${'org.apache.sling.cms.reference.models.ContentFragmentModel' @ fragmentPath='/content/fragments/articles/getting-started'}">
    <article class="blog-post" data-sly-test="${article.isLoaded}">
        <header>
            <h1>${article.properties.title}</h1>
            <div class="meta">
                <span class="author">By ${article.properties.author}</span>
                <time datetime="${article.properties.publishDate}">
                    ${article.properties.publishDate @ format='MMMM dd, yyyy'}
                </time>
            </div>
        </header>
        
        <div class="summary">
            <p>${article.properties.summary}</p>
        </div>
        
        <div class="content">
            ${article.properties.content @ context='html'}
        </div>
        
        <footer class="tags">
            <sly data-sly-list.tag="${article.properties.tags}">
                <a href="/tags/${tag}.html" class="tag">${tag}</a>
            </sly>
        </footer>
    </article>
    
    <!-- Error state -->
    <div class="error" data-sly-test="${!article.isLoaded}">
        Article not found.
    </div>
</sly>
```

## Need More?

See full documentation: [docs/using-content-fragments.md](using-content-fragments.md)

- Multiple fragments display
- JSON API setup
- Fragment lists
- Advanced patterns
- Troubleshooting
