# Multifield Widget

The multifield widget allows content authors to add multiple sets of fields to a component. This is useful for creating repeatable content structures like social links, authors, tags, or any list of complex items.

## Overview

A multifield creates child nodes under the component content. Each item is stored as a separate node (e.g., `item_0`, `item_1`, `item_2`).

### Content Structure Example

```
/content/mysite/mypage/jcr:content/container/author
├── authorName: "Jane Doe"
├── bio: "Software Engineer"
└── socialLinks/
    ├── item_0/
    │   ├── platform: "twitter"
    │   ├── url: "https://twitter.com/janedoe"
    │   └── label: "Follow me"
    ├── item_1/
    │   ├── platform: "linkedin"
    │   ├── url: "https://linkedin.com/in/janedoe"
    │   └── label: "Connect"
    └── item_2/
        ├── platform: "github"
        ├── url: "https://github.com/janedoe"
        └── label: "My code"
```

## Usage in Component Dialog (edit.json)

Add a multifield to your component's `edit.json`:

```json
{
    "jcr:primaryType": "nt:unstructured",
    "sling:resourceType": "sling-cms/components/editor/slingform",
    "button": "Save",
    "fields": {
        "jcr:primaryType": "nt:unstructured",
        "sling:resourceType": "sling-cms/components/general/container",
        "myMultifield": {
            "jcr:primaryType": "nt:unstructured",
            "sling:resourceType": "sling-cms/components/editor/fields/multifield",
            "label": "My Items",
            "name": "items",
            "minItems": "0",
            "maxItems": "10",
            "template": {
                "jcr:primaryType": "nt:unstructured",
                "title": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/text",
                    "label": "Title",
                    "name": "title",
                    "required": true
                },
                "description": {
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

## Multifield Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `sling:resourceType` | String | Yes | Must be `sling-cms/components/editor/fields/multifield` |
| `label` | String | Yes | Display label for the field |
| `name` | String | Yes | Property name for storing items (e.g., `socialLinks`) |
| `minItems` | String | No | Minimum number of items required |
| `maxItems` | String | No | Maximum number of items allowed |
| `template` | Node | Yes | Container for the fields in each item |

## Supported Field Types in Template

You can use any standard field type inside the multifield template:

- `sling-cms/components/editor/fields/text` - Text input
- `sling-cms/components/editor/fields/textarea` - Multi-line text
- `sling-cms/components/editor/fields/select` - Dropdown select
- `sling-cms/components/editor/fields/path` - Path browser
- `sling-cms/components/editor/fields/hidden` - Hidden field
- `sling-cms/components/editor/fields/richtext` - Rich text editor

## Reading Multifield Values in HTL

### Simple Iteration

```html
<sly data-sly-use.templates="core/wcm/components/commons/v1/templates.html">
<div data-sly-test="${resource.getChild('items')}" class="items-list">
    <ul data-sly-list.item="${resource.getChild('items').listChildren}">
        <li>
            <strong>${item.valueMap.title}</strong>
            <p>${item.valueMap.description}</p>
        </li>
    </ul>
</div>
</sly>
```

### Social Links in HTL

```html
<sly data-sly-test.socialLinks="${resource.getChild('socialLinks')}">
    <div class="social-links">
        <a data-sly-list.link="${socialLinks.listChildren}"
           href="${link.valueMap.url}"
           class="social-link social-link--${link.valueMap.platform}"
           target="_blank" 
           rel="noopener noreferrer">
            ${link.valueMap.label || link.valueMap.platform}
        </a>
    </div>
</sly>
```

## Reading Multifield Values in JSP

### Simple Iteration

```jsp
<%@include file="/libs/sling-cms/global.jsp"%>
<c:set var="itemsResource" value="${sling:getRelativeResource(resource, 'items')}" />
<c:if test="${itemsResource != null}">
    <ul>
        <c:forEach var="item" items="${sling:listChildren(itemsResource)}">
            <c:set var="itemProps" value="${sling:adaptTo(item,'org.apache.sling.api.resource.ValueMap')}" />
            <li>
                <strong>${sling:encode(itemProps.title,'HTML')}</strong>
                <p>${sling:encode(itemProps.description,'HTML')}</p>
            </li>
        </c:forEach>
    </ul>
</c:if>
```

### Social Links Example

```jsp
<%@include file="/libs/sling-cms/global.jsp"%>
<c:set var="socialLinks" value="${sling:getRelativeResource(resource, 'socialLinks')}" />
<c:if test="${socialLinks != null}">
    <div class="social-links">
        <c:forEach var="link" items="${sling:listChildren(socialLinks)}">
            <c:set var="linkProps" value="${sling:adaptTo(link,'org.apache.sling.api.resource.ValueMap')}" />
            <a href="${sling:encode(linkProps.url,'HTML_ATTR')}" 
               class="social-link social-link--${sling:encode(linkProps.platform,'HTML_ATTR')}"
               target="_blank" rel="noopener">
                <span class="icon icon-${sling:encode(linkProps.platform,'HTML_ATTR')}"></span>
                <span>${sling:encode(not empty linkProps.label ? linkProps.label : linkProps.platform,'HTML')}</span>
            </a>
        </c:forEach>
    </div>
</c:if>
```

## Complete Example: Author Component

### Component Definition (author.json)

```json
{
    "jcr:primaryType": "sling:Component",
    "jcr:title": "Author",
    "jcr:description": "Displays author information with social links",
    "componentType": "General"
}
```

### Edit Dialog (edit.json)

```json
{
    "jcr:primaryType": "nt:unstructured",
    "sling:resourceType": "sling-cms/components/editor/slingform",
    "button": "Save Author",
    "fields": {
        "jcr:primaryType": "nt:unstructured",
        "sling:resourceType": "sling-cms/components/general/container",
        "name": {
            "jcr:primaryType": "nt:unstructured",
            "sling:resourceType": "sling-cms/components/editor/fields/text",
            "label": "Author Name",
            "name": "authorName",
            "required": true
        },
        "bio": {
            "jcr:primaryType": "nt:unstructured",
            "sling:resourceType": "sling-cms/components/editor/fields/textarea",
            "label": "Biography",
            "name": "bio"
        },
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
                        "twitter": {
                            "jcr:primaryType": "nt:unstructured",
                            "label": "Twitter / X",
                            "value": "twitter"
                        },
                        "linkedin": {
                            "jcr:primaryType": "nt:unstructured",
                            "label": "LinkedIn",
                            "value": "linkedin"
                        },
                        "github": {
                            "jcr:primaryType": "nt:unstructured",
                            "label": "GitHub",
                            "value": "github"
                        }
                    }
                },
                "url": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/text",
                    "label": "Profile URL",
                    "name": "url",
                    "required": true
                },
                "label": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/text",
                    "label": "Display Label",
                    "name": "label"
                }
            }
        }
    }
}
```

### Component JSP (author.jsp)

```jsp
<%@include file="/libs/sling-cms/global.jsp"%>
<div class="author">
    <h3 class="author__name">${sling:encode(properties.authorName,'HTML')}</h3>
    
    <c:if test="${not empty properties.bio}">
        <p class="author__bio">${sling:encode(properties.bio,'HTML')}</p>
    </c:if>
    
    <c:set var="socialLinks" value="${sling:getRelativeResource(resource, 'socialLinks')}" />
    <c:if test="${socialLinks != null && fn:length(sling:listChildren(socialLinks)) > 0}">
        <div class="author__social">
            <c:forEach var="link" items="${sling:listChildren(socialLinks)}">
                <c:set var="linkProps" value="${sling:adaptTo(link,'org.apache.sling.api.resource.ValueMap')}" />
                <a href="${sling:encode(linkProps.url,'HTML_ATTR')}" target="_blank" rel="noopener">
                    ${sling:encode(not empty linkProps.label ? linkProps.label : linkProps.platform,'HTML')}
                </a>
            </c:forEach>
        </div>
    </c:if>
</div>
```

## Features

### Add/Remove Items
- Click "Add Item" to add a new multifield item
- Click the trash icon to remove an item

### Reorder Items
- Use the up/down arrow buttons to reorder items
- Items are saved in the order displayed

### Validation
- Set `required: true` on template fields for validation
- Use `minItems` and `maxItems` to control item count

## Notes

- Multifield items are stored as child nodes under the multifield name
- Item names are auto-generated as `item_0`, `item_1`, etc.
- When editing, existing values are automatically loaded into the form
- The multifield handles deletion of items via the Sling POST Servlet

## Demo Page

A working multifield demo is available in the reference site:
- **Demo Page**: `/content/apache/sling-apache-org/multifield-demo.html`
- **Author Component**: `reference/src/main/resources/jcr_root/apps/reference/components/general/author/`

## See Also

- [Editor Field Types](editor-field-types.md) - Reference for all available field types
- [Custom Components](custom-components.md) - Guide to creating custom components
