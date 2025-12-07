<!-- Licensed to the Apache Software Foundation (ASF) under one or more contributor 
	license agreements. See the NOTICE file distributed with this work for additional 
	information regarding copyright ownership. The ASF licenses this file to 
	you under the Apache License, Version 2.0 (the "License"); you may not use 
	this file except in compliance with the License. You may obtain a copy of 
	the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required 
	by applicable law or agreed to in writing, software distributed under the 
	License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS 
	OF ANY KIND, either express or implied. See the License for the specific 
	language governing permissions and limitations under the License. -->
[Apache Sling](https://sling.apache.org) > [Sling CMS](https://github.com/apache/sling-org-apache-sling-app-cms) > [Developers](developers.md) > Dialog Tabs

# Dialog Tabs

The Tabs field type allows you to organize component dialog fields into multiple tabs, improving the authoring experience for components with many configuration options.

## Overview

When a component has multiple configuration fields, organizing them into logical groups using tabs makes the dialog more user-friendly and easier to navigate. The tabs component renders a boxed tab interface where each tab contains a group of related fields.

## Resource Type

**Resource Type:** `sling-cms/components/editor/fields/tabs`

## Structure

The tabs component uses child nodes to define individual tabs. Each child node under the tabs node represents a tab panel.

### Tab Properties

Each tab child node supports the following property:

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `title` | String | Yes | The display title of the tab shown in the tab bar |

### Tab Content

Each tab can contain any number of field nodes as its children. These fields will be rendered inside the tab panel using the container component.

## Usage Example

Here's a complete example of a component edit dialog with tabs:

### JSON Format (edit.json)

```json
{
    "jcr:primaryType": "nt:unstructured",
    "sling:resourceType": "sling-cms/components/editor/slingform",
    "button": "Save",
    "fields": {
        "jcr:primaryType": "nt:unstructured",
        "sling:resourceType": "sling-cms/components/general/container",
        "tabs": {
            "jcr:primaryType": "nt:unstructured",
            "sling:resourceType": "sling-cms/components/editor/fields/tabs",
            "basicInfo": {
                "jcr:primaryType": "nt:unstructured",
                "title": "Basic Info",
                "name": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/text",
                    "label": "Name",
                    "name": "name",
                    "required": true
                },
                "description": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/textarea",
                    "label": "Description",
                    "name": "description"
                }
            },
            "advancedTab": {
                "jcr:primaryType": "nt:unstructured",
                "title": "Advanced",
                "cssClass": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/text",
                    "label": "CSS Class",
                    "name": "cssClass"
                },
                "htmlId": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/text",
                    "label": "HTML ID",
                    "name": "htmlId"
                }
            }
        }
    }
}
```

### Node Structure (XML Format)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    jcr:primaryType="nt:unstructured"
    sling:resourceType="sling-cms/components/editor/slingform"
    button="Save">
    <fields
        jcr:primaryType="nt:unstructured"
        sling:resourceType="sling-cms/components/general/container">
        <tabs
            jcr:primaryType="nt:unstructured"
            sling:resourceType="sling-cms/components/editor/fields/tabs">
            <basicInfo
                jcr:primaryType="nt:unstructured"
                title="Basic Info">
                <name
                    jcr:primaryType="nt:unstructured"
                    sling:resourceType="sling-cms/components/editor/fields/text"
                    label="Name"
                    name="name"
                    required="true"/>
            </basicInfo>
            <advancedTab
                jcr:primaryType="nt:unstructured"
                title="Advanced">
                <cssClass
                    jcr:primaryType="nt:unstructured"
                    sling:resourceType="sling-cms/components/editor/fields/text"
                    label="CSS Class"
                    name="cssClass"/>
            </advancedTab>
        </tabs>
    </fields>
</jcr:root>
```

## Real-World Example: Author Component

Here's a more comprehensive example from the Author component that demonstrates organizing author information across two tabs:

```json
{
    "jcr:primaryType": "nt:unstructured",
    "sling:resourceType": "sling-cms/components/editor/slingform",
    "button": "Save Author",
    "fields": {
        "jcr:primaryType": "nt:unstructured",
        "sling:resourceType": "sling-cms/components/general/container",
        "tabs": {
            "jcr:primaryType": "nt:unstructured",
            "sling:resourceType": "sling-cms/components/editor/fields/tabs",
            "basicInfo": {
                "jcr:primaryType": "nt:unstructured",
                "title": "Basic Info",
                "name": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/text",
                    "label": "Author Name",
                    "name": "authorName",
                    "required": true
                },
                "jobTitle": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/text",
                    "label": "Job Title",
                    "name": "jobTitle"
                },
                "image": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/path",
                    "label": "Author Image",
                    "name": "authorImage",
                    "basePath": "/content",
                    "type": "file"
                },
                "bio": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/textarea",
                    "label": "Biography",
                    "name": "bio"
                },
                "email": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "sling-cms/components/editor/fields/text",
                    "label": "Email Address",
                    "name": "email"
                }
            },
            "socialTab": {
                "jcr:primaryType": "nt:unstructured",
                "title": "Social Links",
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
                        }
                    }
                }
            }
        }
    }
}
```

## Visual Appearance

The tabs component renders using the Bulma CSS framework's boxed tabs style:

- First tab is active by default
- Tabs are displayed horizontally in a boxed style
- Tab content is shown/hidden based on selection
- Smooth transitions between tab panels

## Best Practices

1. **Logical Grouping**: Group related fields together in the same tab
2. **Tab Naming**: Use clear, concise tab titles (e.g., "Basic Info", "Advanced", "SEO", "Social")
3. **Tab Order**: Place the most commonly used fields in the first tab
4. **Number of Tabs**: Limit to 3-5 tabs to avoid cluttering the interface
5. **Field Count**: Each tab should have a reasonable number of fields (5-10 is typical)
6. **Required Fields**: Consider placing required fields in the first tab for visibility

## Combining with Other Field Types

Tabs can contain any combination of field types:

- Text fields
- Textarea fields
- Select fields
- Path pickers
- Multifield (for repeating items)
- Rich text editors
- Hidden fields
- And more...

See [Editor Field Types](editor-field-types.md) for a complete list of available field types that can be used inside tabs.

## Related Documentation

- [Editor Field Types](editor-field-types.md) - Complete list of available field types
- [Custom Components](custom-components.md) - Creating custom components with dialogs
- [Multifield](multifield.md) - Using multifield components inside tabs
