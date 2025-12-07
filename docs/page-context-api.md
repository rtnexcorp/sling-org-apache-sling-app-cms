# Page Context API

The `PageContext` interface provides a unified way to check the current instance type and page mode across Sling Models, JSP, and HTL.

## Overview

`PageContext` answers two key questions:
1. **What instance type am I on?** - Author, Renderer, or Standalone
2. **What page mode am I in?** - Edit mode or Preview mode

## API Reference

### Instance Type Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getInstanceType()` | `INSTANCE_TYPE` | Returns the enum value (AUTHOR, RENDERER, STANDALONE) |
| `isAuthor()` | `boolean` | True if running on Author instance |
| `isRenderer()` | `boolean` | True if running on Renderer instance |
| `isStandalone()` | `boolean` | True if running on Standalone instance |

### Page Mode Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getPageMode()` | `PageMode` | Returns the enum value (EDIT, PREVIEW) |
| `isEditMode()` | `boolean` | True if page is being edited in CMS |
| `isPreviewMode()` | `boolean` | True if page is being previewed |

### Computed Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `isAuthoringEnabled()` | `boolean` | True if in edit mode on Author/Standalone |
| `isPublishMode()` | `boolean` | True if on Renderer or in preview mode |

## Usage Examples

### 1. In HTL (Sightly)

```html
<!-- Directly use PageContext interface -->
<sly data-sly-use.ctx="org.apache.sling.cms.PageContext">
    
    <!-- Check page mode -->
    <sly data-sly-test="${ctx.editMode}">
        <div class="edit-toolbar">Edit Mode</div>
    </sly>
    
    <!-- Check instance type -->
    <sly data-sly-test="${ctx.author}">
        <p>Running on Author</p>
    </sly>
    <sly data-sly-test="${ctx.renderer}">
        <p>Running on Renderer</p>
    </sly>
    
    <!-- Combined check -->
    <sly data-sly-test="${ctx.authoringEnabled}">
        <button>Edit this component</button>
    </sly>
</sly>
```

### 2. In JSP

The `slingPageContext` variable is automatically available when you include `global.jsp`:

```jsp
<%@include file="/libs/sling-cms/global.jsp" %>

<!-- Check page mode -->
<c:if test="${slingPageContext.editMode}">
    <div class="edit-toolbar">Edit Mode</div>
</c:if>

<!-- Check instance type -->
<c:choose>
    <c:when test="${slingPageContext.author}">
        <p>Running on Author</p>
    </c:when>
    <c:when test="${slingPageContext.renderer}">
        <p>Running on Renderer</p>
    </c:when>
    <c:otherwise>
        <p>Running on Standalone</p>
    </c:otherwise>
</c:choose>

<!-- Combined check -->
<c:if test="${slingPageContext.authoringEnabled}">
    <button>Edit this component</button>
</c:if>
```

### 3. In Sling Model

#### Option A: Inject PageContext using @Self

Since `PageContext` is a Sling Model adapted from the same `SlingHttpServletRequest`, use `@Self` to inject it:

```java
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.cms.PageContext;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

@Model(adaptables = SlingHttpServletRequest.class)
public class MyComponent {

    @Self
    private PageContext pageContext;

    public boolean isShowEditButton() {
        return pageContext != null && pageContext.isAuthoringEnabled();
    }

    public boolean isAuthorInstance() {
        return pageContext != null && pageContext.isAuthor();
    }
    
    public String getInstanceInfo() {
        if (pageContext == null) return "Unknown";
        return pageContext.getInstanceType().name();
    }
}
```

#### Option B: Use OSGi service (for checking instance type only)

```java
import org.apache.sling.cms.publication.PublicationManagerFactory;
import org.apache.sling.cms.publication.INSTANCE_TYPE;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;

@Model(adaptables = Resource.class)
public class MyResourceModel {

    @OSGiService
    private PublicationManagerFactory publicationManagerFactory;

    public boolean isAuthor() {
        return publicationManagerFactory.getInstanceType() == INSTANCE_TYPE.AUTHOR;
    }
}
```

### 4. In Java Servlet or Filter

```java
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.cms.CMSConstants;
import org.apache.sling.cms.publication.PublicationManagerFactory;
import org.apache.sling.cms.publication.INSTANCE_TYPE;

@Component(service = Servlet.class)
public class MyServlet extends SlingSafeMethodsServlet {

    @Reference
    private PublicationManagerFactory publicationManagerFactory;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        // Check edit mode
        boolean isEditMode = "true".equals(request.getAttribute(CMSConstants.ATTR_EDIT_ENABLED));
        
        // Check instance type
        INSTANCE_TYPE instanceType = publicationManagerFactory.getInstanceType();
        boolean isAuthor = instanceType == INSTANCE_TYPE.AUTHOR;
        
        // Your logic here
    }
}
```

## Common Use Cases

### Show/Hide Edit Controls

```html
<sly data-sly-use.ctx="org.apache.sling.cms.PageContext">
    <sly data-sly-test="${ctx.authoringEnabled}">
        <div class="component-toolbar">
            <button class="edit-btn">Edit</button>
            <button class="delete-btn">Delete</button>
        </div>
    </sly>
</sly>
```

### Different Content for Author vs Renderer

```html
<sly data-sly-use.ctx="org.apache.sling.cms.PageContext">
    <sly data-sly-test="${ctx.author}">
        <!-- Author-specific content: drafts, unpublished items -->
        <div class="draft-indicator">Draft</div>
    </sly>
    <sly data-sly-test="${ctx.renderer}">
        <!-- Renderer-specific: analytics, third-party scripts -->
        <script src="analytics.js"></script>
    </sly>
</sly>
```

### Placeholder Content in Edit Mode

```html
<sly data-sly-use.ctx="org.apache.sling.cms.PageContext">
    <sly data-sly-test="${properties.image}">
        <img src="${properties.image}" alt="${properties.alt}"/>
    </sly>
    <sly data-sly-test="${!properties.image && ctx.editMode}">
        <div class="placeholder">
            Click to add an image
        </div>
    </sly>
</sly>
```

### Conditional Analytics Loading

```html
<sly data-sly-use.ctx="org.apache.sling.cms.PageContext">
    <!-- Only load analytics on Renderer/Publish -->
    <sly data-sly-test="${ctx.publishMode}">
        <script>
            // Google Analytics or similar
        </script>
    </sly>
</sly>
```

## Demo Components

Demo components are available in the reference site at `/content/apache/sling-apache-org/page-context-demo.html`:

| Component | Path | Description |
|-----------|------|-------------|
| **HTL** | `reference/components/general/pagecontextdemo-htl/` | Uses `PageContext` directly in HTL |
| **Sling Model** | `reference/components/general/pagecontextdemo-model/` | Uses `PageContextDemoModel` wrapper |
| **JSP** | `reference/components/general/pagecontextdemo/` | Uses `slingPageContext` from global.jsp |

### Reference Sling Model

The `PageContextDemoModel` in `reference/src/main/java/org/apache/sling/cms/reference/models/PageContextDemoModel.java` demonstrates the recommended patterns for using PageContext in your own Sling Models.

## Instance Type Values

| Value | Description |
|-------|-------------|
| `AUTHOR` | Content authoring instance, has full CMS UI |
| `RENDERER` | Content delivery instance, receives published content |
| `STANDALONE` | Single instance for both authoring and delivery |

## Page Mode Values

| Value | Description |
|-------|-------------|
| `EDIT` | Page is being edited in CMS editor, edit UI visible |
| `PREVIEW` | Page is being previewed, no edit UI |
