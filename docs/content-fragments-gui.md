# Content Fragments - User Guide

## Overview

Content Fragments provide a way to create structured, reusable content based on predefined Content Types (Schemas). This feature allows you to separate content structure from presentation, making it easier to reuse content across different channels and contexts.

## Key Concepts

- **Content Type/Schema**: A template that defines the structure of your content (e.g., Article, Product)
- **Content Fragment**: An instance of a Content Type filled with actual content data
- **Field**: A single piece of data within a fragment (e.g., title, price, description)

## Creating Content Fragments

### Step 1: Access Content Fragments

1. Navigate to the CMS start page: `/cms`
2. In the left navigation menu, under **Manage**, click **Content Fragments**
3. You'll see the Content Fragments list page at `/cms/fragment/list.html/content/fragments`

### Step 2: Create a New Fragment

1. Click the **Create Fragment** button
2. A modal dialog will appear with the following fields:
   - **Content Type**: Select the schema for your fragment (e.g., Article, Product)
   - **Name**: Enter a URL-friendly technical name (e.g., `my-article-2024`)
   - **Title**: Enter a human-readable title

3. Click **Create Fragment**
4. The fragment will be created at `/content/fragments/{name}`

### Step 3: Edit Fragment Content

After creating a fragment, you'll be directed to the edit page where you can:

1. Update the fragment title
2. Fill in all fields defined by the Content Type
3. Each field will render with the appropriate input control based on its type:
   - **String**: Single-line text input
   - **Text**: Multi-line textarea
   - **Boolean**: Checkbox
   - **Integer**: Number input (whole numbers)
   - **Decimal**: Number input (with decimals)
   - **Date**: Date picker
   - **DateTime**: Date and time picker
   - **Select**: Dropdown with predefined options
   - **Reference**: Path selector for linking to other content
   - **Asset**: Asset picker for images/files
   - **Tag**: Tag selector

4. Click **Save Fragment** to save your changes

## Managing Content Fragments

### Viewing Fragments

The Content Fragments list shows:
- **Name**: The technical name (clickable to edit)
- **Title**: The display title
- **Schema**: The Content Type used
- **Modified**: Last modification date
- **Actions**: Edit and Delete buttons

### Editing Fragments

1. Click the fragment **name** or the **Edit** button (pencil icon)
2. Modify the fields as needed
3. Click **Save Fragment**

### Deleting Fragments

1. Click the **Delete** button (trash icon) next to the fragment
2. Confirm the deletion in the modal dialog
3. The fragment will be permanently removed

## Example: Creating a Blog Article

Let's create a blog article using the Article Content Type:

1. Go to **Manage** → **Content Fragments**
2. Click **Create Fragment**
3. Select:
   - **Content Type**: Article
   - **Name**: `introducing-content-fragments`
   - **Title**: "Introducing Content Fragments in Sling CMS"
4. Click **Create Fragment**

5. On the edit page, fill in the Article fields:
   - **Title**: "Introducing Content Fragments in Sling CMS"
   - **Summary**: "Learn how to use the new Content Fragments feature..."
   - **Body**: (rich text) Full article content
   - **Author**: Select or enter author reference
   - **Publish Date**: Select today's date
   - **Category**: Select "Technology"
   - **Tags**: Add tags like "cms", "content", "tutorial"
   - **Featured Image**: Select an image asset
   - **Featured**: Check if this should be featured
6. Click **Save Fragment**

## Best Practices

1. **Use Descriptive Names**: Choose clear, URL-friendly names for fragments (e.g., `spring-sale-2024`, not `frag1`)
2. **Fill Required Fields**: Required fields are marked and must be filled before saving
3. **Follow Schema Guidelines**: Each field may have validation rules (min/max length, patterns) - follow them for best results
4. **Reuse Fragments**: Fragments can be referenced from other content, making them reusable components
5. **Organize by Type**: Use consistent naming conventions to group related fragments

## Next Steps

- Learn about [Content Schemas](content-schema-gui.md) to create custom Content Types
- Explore field types and validation rules in [Content Schemas Technical Guide](content-schemas.md)
- Reference fragments from Pages and Components using the Reference field type

## Troubleshooting

### "No schema found for this fragment"
- Ensure the `schemaId` property is set correctly
- Verify the schema exists at `/conf/global/schemas/{schemaId}`

### Missing Fields in Editor
- Check that the schema has fields defined
- Verify the schema is enabled (`enabled=true`)

### Validation Errors
- Review field validation rules in the schema definition
- Ensure required fields are filled
- Check min/max length requirements
- Verify number fields are within allowed ranges

## Storage Structure

Content Fragments are stored in the JCR repository at:
```
/content/fragments/
  ├── {fragment-name}/
  │   ├── jcr:title: "Display Title"
  │   ├── schemaId: "article"
  │   ├── sling:resourceType: "sling-cms/components/cms/fragment"
  │   ├── {field1}: "value1"
  │   ├── {field2}: "value2"
  │   └── ...
```

Each fragment is an `nt:unstructured` node with:
- **jcr:title**: The display title
- **schemaId**: Reference to the Content Type
- **sling:resourceType**: Component type (`sling-cms/components/cms/fragment`)
- **Field properties**: One property per field defined in the schema
