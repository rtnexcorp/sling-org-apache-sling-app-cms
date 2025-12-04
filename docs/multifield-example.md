# Multifield Widget Example

This example shows how to use the multifield widget in a component's edit dialog.

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
