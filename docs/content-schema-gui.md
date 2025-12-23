# Content Schema GUI - User Guide

## Overview

The Content Schema Management system provides a complete GUI for creating, editing, and managing content type definitions (schemas) with validation rules. This allows content authors to define structured content types like articles, products, events, etc.

## Accessing Schema Management

1. **Login to CMS**: http://localhost:8082/cms
2. **Navigate**: Click **Manage > Content Schemas** in the left navigation menu
3. **Default Location**: Schemas are stored at `/conf/global/site/schemas/`

## Features

### 1. Schema List View

**URL**: `/cms/schema/list.html/conf/global/site/schemas`

Displays all content schemas with:
- Schema ID (technical name)
- Title (human-readable name)
- Description
- Version
- Actions (Edit, Delete)

**Actions**:
- **Create Schema**: Click "+ Content Schema" button
- **Edit Schema**: Click "Edit" button on any schema
- **Delete Schema**: Click "Delete" button (with confirmation)

### 2. Create Schema

**URL**: Opens modal from list view

**Fields**:
- **Title*** (required): Human-readable name (e.g. "Article", "Product")
- **Schema ID*** (required): Technical identifier (e.g. "article", "product")
- **Description**: Detailed description of the schema
- **Version**: Schema version (default: "1.0")
- **Enabled**: Whether the schema is active

**Example**:
```
Title: Blog Article
Schema ID: blog-article
Description: Schema for blog posts with title, content, and metadata
Version: 1.0
Enabled: ✓
```

### 3. Edit Schema

**URL**: `/cms/schema/edit.html/conf/global/site/schemas/{schema-id}`

**Two Sections**:

#### Schema Properties
- Title
- Description
- Version
- Enabled status

#### Schema Fields Management
- **List all fields** in the schema
- **Add new field**: Click "+ Field" button
- **Edit field**: Click "Edit" on any field
- **Delete field**: Click "Delete" on any field (with confirmation)

**Field Table Columns**:
- Field Name (technical name)
- Label (display name)
- Type (string, text, richtext, etc.)
- Required (yes/no)
- Order (sort order)

### 4. Create Field

**URL**: Opens modal from edit schema page

**Basic Properties**:
- **Field Name*** (required): Technical name (e.g. "title", "price")
  - Pattern: Must start with letter, alphanumeric only
- **Label*** (required): Human-readable label
- **Description**: Help text for content authors
- **Field Type*** (required): Select from 12 types:
  - **String**: Short text (product name, title)
  - **Text**: Long text (description)
  - **Rich Text**: Formatted text with HTML
  - **Boolean**: True/false checkbox
  - **Integer**: Whole numbers (stock quantity)
  - **Decimal**: Numbers with decimals (price)
  - **Date**: Date only
  - **Date/Time**: Date and time
  - **Reference**: Link to other content
  - **Tag**: Taxonomy/tags
  - **Asset**: File/image upload
  - **Select**: Dropdown selection

**Options**:
- **Required**: Must have a value
- **Multiple Values**: Allow array of values
- **Order**: Sort order (10, 20, 30...)
- **Default Value**: Pre-filled value

**Example - Product Price Field**:
```
Field Name: price
Label: Product Price
Description: Price in USD
Type: Decimal
Required: ✓
Multiple Values: ☐
Order: 40
Default Value: 0.00
```

### 5. Edit Field

**URL**: Opens modal from field list

**All Basic Properties** (same as create)

**PLUS: Validation Rules**:
- **Min Length**: Minimum characters (for text fields)
- **Max Length**: Maximum characters (for text fields)
- **Min Value**: Minimum number (for integer/decimal)
- **Max Value**: Maximum number (for integer/decimal)
- **Pattern**: Regular expression for validation

**Example - SKU Field Validation**:
```
Field Name: sku
Label: SKU
Type: String
Required: ✓
Validation Rules:
  Pattern: ^[A-Z0-9-]{5,20}$
```

**Additional Properties** (for select fields):
- **Options**: Comma-separated list of values
  - Example: `electronics, clothing, books, home, sports`

## Common Use Cases

### Use Case 1: Blog Article Schema

**Schema**:
- ID: `article`
- Title: "Blog Article"

**Fields**:
1. **title** (string, required, 5-200 chars)
2. **summary** (text, required, 50-500 chars)
3. **body** (richtext, required, 100+ chars)
4. **author** (reference, required)
5. **publishDate** (datetime, required)
6. **category** (select: Technology, Business, Lifestyle)
7. **tags** (tag, multiple)
8. **featuredImage** (asset, required)

### Use Case 2: E-Commerce Product Schema

**Schema**:
- ID: `product`
- Title: "Product"

**Fields**:
1. **productName** (string, required, 3-200 chars)
2. **sku** (string, required, pattern: ^[A-Z0-9-]{5,20}$)
3. **description** (richtext, required, 50+ chars)
4. **price** (decimal, required, min: 0.01)
5. **stockQuantity** (integer, required, min: 0)
6. **category** (select: electronics, clothing, books, home, sports, toys)
7. **productImages** (asset, multiple)
8. **featured** (boolean, default: false)

### Use Case 3: Event Schema

**Schema**:
- ID: `event`
- Title: "Event"

**Fields**:
1. **eventName** (string, required)
2. **description** (richtext, required)
3. **startDate** (datetime, required)
4. **endDate** (datetime, required)
5. **location** (string, required)
6. **capacity** (integer, min: 1)
7. **registrationUrl** (string, pattern: URL regex)
8. **tags** (tag, multiple)

## Workflow

### Creating a New Content Type

1. **Navigate**: Manage > Content Schemas
2. **Create Schema**: Click "+ Content Schema"
3. **Fill Details**:
   - Title: "Event"
   - Schema ID: "event"
   - Description: "Schema for events and conferences"
   - Version: "1.0"
   - Enabled: ✓
4. **Save**: Click "Create Schema"
5. **Edit Schema**: Automatically opens or click "Edit"
6. **Add Fields**: Click "+ Field" to add each field
7. **Configure Field**:
   - Set name, label, type
   - Set validation rules
   - Set options (for select fields)
8. **Save Field**: Click "Create Field"
9. **Repeat**: Add all fields
10. **Done**: Navigate back to schema list

### Editing an Existing Schema

1. **Navigate**: Manage > Content Schemas
2. **Find Schema**: Locate in list
3. **Edit**: Click "Edit" button
4. **Modify**:
   - Update schema properties (top section)
   - Add/edit/delete fields (bottom section)
5. **Save**: Click "Save Schema" or "Update Field"

### Deleting a Schema

1. **Navigate**: Manage > Content Schemas
2. **Find Schema**: Locate in list
3. **Delete**: Click "Delete" button
4. **Confirm**: Confirm deletion in modal

**⚠️ Warning**: Deleting a schema does NOT delete content created with that schema. Content validation will fail if schema is removed.

## Technical Details

### Storage Location

Schemas are stored in the JCR repository at:
```
/conf/global/site/schemas/
├── article/
│   ├── jcr:primaryType = nt:unstructured
│   ├── sling:resourceType = sling-cms/components/cms/schema
│   ├── jcr:title = "Article Schema"
│   ├── version = "1.0"
│   └── fields/
│       ├── title/
│       ├── body/
│       └── ...
└── product/
    └── ...
```

### Field Structure

Each field is stored as a node under the schema's `fields/` folder:
```
/conf/global/site/schemas/article/fields/title/
├── jcr:primaryType = nt:unstructured
├── label = "Title"
├── type = "string"
├── required = true
├── order = 10
└── validation/
    ├── minLength = 5
    └── maxLength = 200
```

### Sling Model Adaptation

Schemas can be adapted to `ContentSchema` Sling Model:
```java
Resource schemaResource = resourceResolver.getResource("/conf/global/site/schemas/article");
ContentSchema schema = schemaResource.adaptTo(ContentSchema.class);

// Use schema
String title = schema.getTitle();
List<SchemaField> fields = schema.getFields();
ValidationResult result = schema.validate(contentResource);
```

## API Integration

### SchemaManager Service

Get schemas programmatically:
```java
@Reference
private SchemaManager schemaManager;

// Get specific schema
ContentSchema schema = schemaManager.getSchema("global", "article");

// Get all schemas for a site
List<ContentSchema> schemas = schemaManager.getAllSchemas("global");

// Validate content
ValidationResult result = schemaManager.validate(contentResource, schema);
if (!result.isValid()) {
    for (ValidationError error : result.getErrors()) {
        log.error("Validation error: {}", error.getMessage());
    }
}
```

### Validation Service

Validate content against schema:
```java
@Reference
private SchemaValidator validator;

ValidationResult result = validator.validate(contentResource, schema);

if (result.isValid()) {
    // Content is valid
} else {
    // Handle validation errors
    List<ValidationError> errors = result.getErrors();
}
```

## Best Practices

### Naming Conventions

**Schema IDs**:
- Use lowercase with hyphens: `blog-article`, `product-listing`
- Be descriptive: `event` not `evt`
- Avoid special characters

**Field Names**:
- Use camelCase: `firstName`, `publishDate`
- Start with letter
- Alphanumeric only

### Field Order

Use increments of 10 for field order:
- 10, 20, 30, 40...
- Allows inserting fields between existing ones

### Validation Patterns

**Common Regex Patterns**:
- **Email**: `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$`
- **URL**: `^https?://.*$`
- **SKU**: `^[A-Z0-9-]{5,20}$`
- **Phone**: `^\+?[1-9]\d{1,14}$`
- **Postal Code (US)**: `^\d{5}(-\d{4})?$`

### Schema Versioning

When making breaking changes:
1. Increment version: `1.0` → `2.0`
2. Update `jcr:description` to document changes
3. Consider migration path for existing content

## Troubleshooting

### Schema Not Appearing in List

**Check**:
1. Schema exists at `/conf/global/site/schemas/{schema-id}`
2. Has `sling:resourceType = "sling-cms/components/cms/schema"`
3. Parent folder has `jcr:primaryType = "nt:unstructured"`

### Field Validation Not Working

**Check**:
1. Validation rules stored under `{field}/validation/`
2. Property names match: `minLength`, `maxLength`, `min`, `max`, `pattern`
3. Pattern is valid regex

### Cannot Create Schema

**Check**:
1. User has write permissions to `/conf/global/site/schemas/`
2. Schema ID is unique
3. All required fields filled

## Future Enhancements

Planned features:
- [ ] Schema-driven dialog generation
- [ ] Visual field editor with drag-and-drop
- [ ] Field type templates
- [ ] Schema inheritance
- [ ] Content migration tools
- [ ] Validation error preview
- [ ] Export/import schemas as JSON
- [ ] Schema marketplace/library

## Navigation Structure

```
CMS Start Page
└── Manage Section
    └── Content Schemas (/cms/schema/list.html/conf/global/site/schemas)
        ├── List View
        │   ├── Create Schema (+)
        │   ├── Edit Schema (per item)
        │   └── Delete Schema (per item)
        └── Edit Schema View (/cms/schema/edit.html/conf/global/site/schemas/{id})
            ├── Schema Properties Form
            └── Fields Section
                ├── Create Field (+)
                ├── Edit Field (per field)
                └── Delete Field (per field)
```

## Additional Resources

- **Documentation**: `/docs/content-schemas.md` - Complete technical guide
- **API Reference**: JavaDocs for `org.apache.sling.cms.schema` package
- **Example Schemas**: `/conf/global/site/schemas/article`, `/conf/global/site/schemas/product`

---

**Author**: Apache Sling CMS Team
**Version**: 1.0
**Last Updated**: December 23, 2025
