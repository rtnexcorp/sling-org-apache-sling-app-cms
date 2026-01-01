# Using Content Fragments in the Reference Site

This guide explains how to use content fragments created in the CMS within your reference site pages.

## Overview

Content fragments are structured, reusable content pieces stored at `/content/fragments/` that can be referenced and displayed across your site. They separate content structure from presentation, enabling content reuse across different channels.

## Table of Contents

1. [Creating Content Fragments](#creating-content-fragments)
2. [Exposing Content Fragments as JSON](#exposing-content-fragments-as-json)
3. [Using Content Fragments in HTL Components](#using-content-fragments-in-htl-components)
4. [Using the ContentFragment Component](#using-the-contentfragment-component)
5. [Advanced Usage](#advanced-usage)

---

## Creating Content Fragments

### 1. Create a Content Schema

First, create a content schema that defines the structure:

1. Navigate to **CMS** → **Configuration** → **Content Schemas**
2. Click **+ Content Schema**
3. Define fields like:
   - **title** (STRING)
   - **description** (TEXT)
   - **author** (STRING)
   - **publishDate** (DATE)
   - etc.

### 2. Create a Content Fragment

1. Navigate to **CMS** → **Manage** → **Content Fragments**
2. Click **+ Content Fragment**
3. Select your content schema
4. Fill in the fragment data
5. Save

Your content fragment is now stored at `/content/fragments/your-fragment-name`.

---

## Exposing Content Fragments as JSON

To expose content fragments as JSON for external systems or REST APIs, create a Sling servlet:

### Step 1: Create a JSON Servlet

```java
package org.apache.sling.cms.reference.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Servlet to expose content fragments as JSON.
 * 
 * Access at: /content/fragments/your-fragment.json
 */
@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.resourceTypes=nt:unstructured",
        "sling.servlet.selectors=api",
        "sling.servlet.extensions=json",
        "sling.servlet.methods=GET"
    }
)
public class ContentFragmentJsonServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        
        Resource resource = request.getResource();
        ValueMap properties = resource.getValueMap();
        
        // Filter out system properties
        Map<String, Object> fragmentData = new HashMap<>();
        properties.forEach((key, value) -> {
            if (!key.startsWith("jcr:") && !key.startsWith("sling:")) {
                fragmentData.put(key, value);
            }
        });
        
        // Add metadata
        fragmentData.put("_path", resource.getPath());
        fragmentData.put("_resourceType", resource.getResourceType());
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(gson.toJson(fragmentData));
    }
}
```

### Step 2: Access the JSON Endpoint

Access your content fragment as JSON:

```
GET /content/fragments/article-1.api.json

Response:
{
  "title": "My Article",
  "description": "Article description",
  "author": "John Doe",
  "publishDate": "2025-12-26",
  "schemaId": "article",
  "_path": "/content/fragments/article-1",
  "_resourceType": "nt:unstructured"
}
```

### Step 3: Secure the Endpoint (Optional)

To restrict access, add authentication requirements in your servlet or use ACLs:

```java
@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.resourceTypes=nt:unstructured",
        "sling.servlet.selectors=api",
        "sling.servlet.extensions=json",
        "sling.servlet.methods=GET",
        "sling.auth.requirements=/content/fragments"  // Require authentication
    }
)
```

---

## Using Content Fragments in HTL Components

### Method 1: Using ContentFragmentModel Directly

Create a custom component that loads a specific fragment:

**HTL Template** (`article-display.html`):
```html
<sly data-sly-use.fragment="${'org.apache.sling.cms.reference.models.ContentFragmentModel' @ fragmentPath='/content/fragments/article-1'}">
    <sly data-sly-test="${fragment.isLoaded}">
        <article class="article">
            <h1>${fragment.properties.title}</h1>
            <p class="meta">By ${fragment.properties.author} on ${fragment.properties.publishDate}</p>
            <div class="content">
                ${fragment.properties.content @ context='html'}
            </div>
        </article>
    </sly>
</sly>
```

### Method 2: Dynamic Fragment Path from Component Properties

**Component Dialog** (`.content.xml`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
    jcr:primaryType="nt:unstructured">
    <fields jcr:primaryType="nt:unstructured">
        <fragmentPath
            jcr:primaryType="nt:unstructured"
            sling:resourceType="sling-cms/components/editor/fields/path"
            label="Content Fragment Path"
            name="fragmentPath"
            required="true"/>
    </fields>
</jcr:root>
```

**HTL Template**:
```html
<sly data-sly-use.fragment="${'org.apache.sling.cms.reference.models.ContentFragmentModel' @ fragmentPath=properties.fragmentPath}">
    <sly data-sly-test="${fragment.isLoaded}">
        <h2>${fragment.title}</h2>
        <p>${fragment.properties.description}</p>
    </sly>
</sly>
```

### Method 3: Display Multiple Fragments from a Folder

```html
<sly data-sly-use.resourceList="${'org.apache.sling.cms.ResourceListModel' @ basePath='/content/fragments/articles'}">
    <div class="articles-grid">
        <sly data-sly-list.item="${resourceList.items}">
            <sly data-sly-use.fragment="${'org.apache.sling.cms.reference.models.ContentFragmentModel' @ fragmentPath=item.path}">
                <div class="article-card">
                    <h3>${fragment.properties.title}</h3>
                    <p>${fragment.properties.summary}</p>
                    <a href="/articles/${item.name}.html">Read more</a>
                </div>
            </sly>
        </sly>
    </div>
</sly>
```

---

## Using the ContentFragment Component

The reference site includes a pre-built `contentfragment` component that can be added to pages via the page editor.

### Step 1: Add Component to Page

1. Edit a page in the CMS
2. Add the **Content Fragment** component from the **Reference - General** group
3. Configure the fragment path in the dialog
4. Save

### Step 2: Customize the Display

Edit `/apps/reference/components/general/contentfragment/contentfragment.html` to customize how fragments are displayed:

```html
<sly data-sly-use.fragment="org.apache.sling.cms.reference.models.ContentFragmentModel">
    <sly data-sly-test="${fragment.isLoaded}">
        <!-- Custom article display -->
        <article class="blog-post">
            <h1 class="blog-post__title">${fragment.properties.title}</h1>
            <div class="blog-post__meta">
                <span class="author">${fragment.properties.author}</span>
                <time>${fragment.properties.publishDate}</time>
            </div>
            <div class="blog-post__content">
                ${fragment.properties.content @ context='html'}
            </div>
            <div class="blog-post__tags">
                <sly data-sly-list.tag="${fragment.properties.tags}">
                    <span class="tag">${tag}</span>
                </sly>
            </div>
        </article>
    </sly>
</sly>
```

---

## Advanced Usage

### Creating Schema-Specific Display Components

Create dedicated components for each content type:

**Article Fragment Component** (`article-fragment.html`):
```html
<sly data-sly-use.fragment="org.apache.sling.cms.reference.models.ContentFragmentModel">
    <sly data-sly-test="${fragment.schemaId == 'article'}">
        <article class="article" itemscope itemtype="http://schema.org/Article">
            <h1 itemprop="headline">${fragment.properties.title}</h1>
            <meta itemprop="author" content="${fragment.properties.author}">
            <time itemprop="datePublished" datetime="${fragment.properties.publishDate}">
                ${fragment.properties.publishDate @ format='MMMM dd, yyyy'}
            </time>
            <div itemprop="articleBody">
                ${fragment.properties.content @ context='html'}
            </div>
        </article>
    </sly>
</sly>
```

### Creating a Fragment List Component

Create a component that lists all fragments of a specific type:

**Sling Model** (`FragmentListModel.java`):
```java
@Model(adaptables = Resource.class)
public class FragmentListModel {

    @SlingObject
    private ResourceResolver resourceResolver;

    @ValueMapValue
    @Optional
    private String basePath = "/content/fragments";

    @ValueMapValue
    @Optional
    private String schemaId;

    public List<Resource> getFragments() {
        List<Resource> fragments = new ArrayList<>();
        Resource baseResource = resourceResolver.getResource(basePath);
        
        if (baseResource != null) {
            Iterable<Resource> children = baseResource.getChildren();
            for (Resource child : children) {
                ValueMap props = child.getValueMap();
                String fragmentSchema = props.get("schemaId", String.class);
                
                // Filter by schema if specified
                if (schemaId == null || schemaId.equals(fragmentSchema)) {
                    fragments.add(child);
                }
            }
        }
        
        return fragments;
    }
}
```

**HTL Template**:
```html
<sly data-sly-use.list="org.apache.sling.cms.reference.models.FragmentListModel">
    <div class="fragment-list">
        <sly data-sly-list.item="${list.fragments}">
            <sly data-sly-use.fragment="${'org.apache.sling.cms.reference.models.ContentFragmentModel' @ fragmentPath=item.path}">
                <div class="fragment-item">
                    <h3><a href="${item.path @ extension='html'}">${fragment.properties.title}</a></h3>
                    <p>${fragment.properties.summary}</p>
                </div>
            </sly>
        </sly>
    </div>
</sly>
```

### Caching Fragment Data

For high-performance scenarios, cache fragment data:

```java
@Model(adaptables = Resource.class, cache = true)
public class CachedContentFragmentModel {
    // Model implementation with caching enabled
}
```

---

## Best Practices

1. **Use Schemas**: Always define content schemas before creating fragments
2. **Organize by Type**: Create folders under `/content/fragments/` organized by content type (e.g., `/content/fragments/articles/`, `/content/fragments/products/`)
3. **Reusable Components**: Create reusable HTL components for common fragment display patterns
4. **JSON API**: Use the JSON servlet for exposing fragments to external systems
5. **Security**: Apply proper ACLs to `/content/fragments/` to control access
6. **Validation**: Leverage schema field validation to ensure data quality
7. **Naming**: Use descriptive, URL-friendly names for fragments

---

## Troubleshooting

### Fragment Not Loading

Check:
- Fragment path is correct
- Resource exists at the specified path
- User has read permissions on the fragment

### Properties Not Displaying

- Ensure property names in HTL match those defined in the schema
- Check for typos in property names
- Verify properties are saved in the fragment

### JSON Endpoint Not Working

- Verify servlet is deployed (`http://localhost:8080/system/console/components`)
- Check servlet binding matches your resource type
- Ensure proper selectors and extensions are used

---

## Related Documentation

- [Content Schemas Guide](content-schemas.md)
- [Content Fragments GUI Guide](content-fragments-gui.md)
- [HTL Template Language](https://github.com/adobe/htl-spec)
- [Sling Models Documentation](https://sling.apache.org/documentation/bundles/models.html)

---

## Support

For issues or questions:
- [Apache Sling CMS Documentation](https://github.com/apache/sling-org-apache-sling-app-cms)
- [Apache Sling Mailing Lists](https://sling.apache.org/project-information/mailing-lists.html)
