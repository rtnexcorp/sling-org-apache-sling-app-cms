# Multifield Widget Example

This example shows how to use the multifield widget in a component's edit dialog.

## Real-World Example: Author Component

The Author component (`/apps/reference/components/general/author`) uses multifield to manage social links. See the full implementation at:
- Component: `reference/src/main/resources/jcr_root/apps/reference/components/general/author/`
- Edit dialog: `edit.json`
- JSP: `author.jsp`

## Example: Links Component Edit Dialog

```json
{
    "jcr:primaryType": "nt:unstructured",
    "sling:resourceType": "sling-cms/components/editor/slingform",
    "button": "Save",
    "fields": {
        "jcr:primaryType": "nt:unstructured",
        "sling:resourceType": "sling-cms/components/general/container",
        "title": {
            "jcr:primaryType": "nt:unstructured",
            "sling:resourceType": "sling-cms/components/editor/fields/text",
            "label": "Title",
            "name": "title"
        },
        "links": {
            "jcr:primaryType": "nt:unstructured",
            "sling:resourceType": "sling-cms/components/editor/fields/multifield",
            "label": "Links",
            "name": "links",
            "minItems": "1",
            "maxItems": "10",
            "template": {
                "jcr:primaryType": "nt:unstructured",
                "linkTitle": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/text",
                    "label": "Link Title",
                    "name": "linkTitle",
                    "required": true
                },
                "linkUrl": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/path",
                    "label": "Link URL",
                    "name": "linkUrl",
                    "required": true
                },
                "openInNewTab": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/select",
                    "label": "Open in New Tab",
                    "name": "openInNewTab",
                    "options": {
                        "yes": {
                            "jcr:primaryType": "nt:unstructured",
                            "label": "Yes",
                            "value": "true"
                        },
                        "no": {
                            "jcr:primaryType": "nt:unstructured",
                            "label": "No",
                            "value": "false"
                        }
                    }
                }
            }
        }
    }
}
```

## Data Structure

When saved, the multifield data will be stored as child nodes under the specified name:

```
/content/site/page/jcr:content/component
    title="Navigation Links"
    /links
        /item_0
            linkTitle="Home"
            linkUrl="/content/site/home"
            openInNewTab="false"
        /item_1
            linkTitle="About Us"
            linkUrl="/content/site/about"
            openInNewTab="false"
        /item_2
            linkTitle="External Link"
            linkUrl="https://example.com"
            openInNewTab="true"
```

## Component JSP Example

To render the multifield data in your component:

```jsp
<%@include file="/libs/sling-cms/global.jsp"%>
<c:set var="linksResource" value="${sling:getRelativeResource(resource, 'links')}" />
<c:if test="${linksResource != null}">
    <nav>
        <ul>
            <c:forEach var="link" items="${sling:listChildren(linksResource)}">
                <c:set var="linkProps" value="${sling:adaptTo(link,'org.apache.sling.api.resource.ValueMap')}" />
                <li>
                    <a href="${sling:encode(linkProps.linkUrl,'HTML_ATTR')}"
                       <c:if test="${linkProps.openInNewTab == 'true'}">target="_blank" rel="noopener noreferrer"</c:if>>
                        <sling:encode value="${linkProps.linkTitle}" mode="HTML" />
                    </a>
                </li>
            </c:forEach>
        </ul>
    </nav>
</c:if>
```

## Features

- **Add/Remove Items**: Users can add new items and remove existing ones
- **Reorder Items**: Items can be moved up or down using the arrow buttons
- **Flexible Fields**: Each item can contain any combination of field types (text, select, path, textarea, etc.)
- **Min/Max Items**: Optional constraints on the number of items allowed
- **Visual Feedback**: Card-based layout with clear item numbering

## Author Component Details

The author component edit dialog (`edit.json`) demonstrates multifield with select and text fields:

```json
{
    "socialLinks": {
        "jcr:primaryType": "nt:unstructured",
        "sling:resourceType": "sling-cms/components/editor/fields/multifield",
        "label": "Social Links",
        "name": "socialLinks",
        "minItems": "0",
        "maxItems": "10",
        "template": {
            "jcr:primaryType": "nt:unstructured",
            "platform": {
                "jcr:primaryType": "nt:unstructured",
                "sling:resourceType": "sling-cms/components/editor/fields/select",
                "label": "Platform",
                "name": "platform",
                "required": true,
                "options": {
                    "jcr:primaryType": "nt:unstructured",
                    "twitter": { "label": "Twitter / X", "value": "twitter" },
                    "linkedin": { "label": "LinkedIn", "value": "linkedin" },
                    "github": { "label": "GitHub", "value": "github" }
                }
            },
            "url": {
                "jcr:primaryType": "nt:unstructured",
                "sling:resourceType": "sling-cms/components/editor/fields/text",
                "label": "Profile URL",
                "name": "url",
                "required": true
            }
        }
    }
}
```

### Rendering Multifield Data in JSP

```jsp
<c:set var="socialLinksResource" value="${sling:getRelativeResource(resource, 'socialLinks')}" />
<c:if test="${socialLinksResource != null}">
    <div class="author-social-links">
        <c:forEach var="link" items="${sling:listChildren(socialLinksResource)}">
            <c:set var="linkProps" value="${sling:adaptTo(link,'org.apache.sling.api.resource.ValueMap')}" />
            <a href="${sling:encode(linkProps.url,'HTML_ATTR')}" 
               class="social-link social-link--${sling:encode(linkProps.platform,'HTML_ATTR')}"
               target="_blank" rel="noopener noreferrer">
                <sling:encode value="${linkProps.platform}" mode="HTML" />
            </a>
        </c:forEach>
    </div>
</c:if>
```
