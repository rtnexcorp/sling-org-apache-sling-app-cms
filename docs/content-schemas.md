# Content Schemas Implementation

## Overview

Content Schemas provide a first-class schema definition system for pages and content fragments in Apache Sling CMS. This feature enables schema-driven authoring, consistent validation, and predictable content structure.

## Features

- **Schema Definitions**: Define content types with field specifications, types, and validation rules
- **Field Types**: Support for multiple field types (string, text, richtext, boolean, integer, decimal, date, datetime, reference, tag, asset, select, radio, checkbox, hidden, object, array)
- **Validation**: Built-in validation for required fields, type checking, min/max values, pattern matching, etc.
- **Inheritance**: Schemas can extend other schemas for reusability
- **CAConfig Integration**: Schemas are stored using Context-Aware Configuration under `/conf/{site}/schemas/`

## Architecture

### API Interfaces (in `api` module)

- **ContentSchema**: Main schema interface defining schema structure
- **SchemaField**: Field definition within a schema
- **FieldType**: Enumeration of supported field types
- **SchemaManager**: Service for managing and retrieving schemas
- **ValidationResult**: Result of schema validation with errors and warnings

### Implementation (in `core` module)

- **ContentSchemaImpl**: Sling Model implementation of ContentSchema
- **SchemaFieldImpl**: Sling Model implementation of SchemaField
- **SchemaManagerImpl**: OSGi service for schema management
- **SchemaValidator**: Validation logic for content against schemas

## Schema Structure

Schemas are stored at `/conf/{site}/site/schemas/{schema-id}` with the following structure:

```
/conf/global/site/schemas/article
  - jcr:primaryType: nt:unstructured
  - sling:resourceType: sling-cms/components/cms/schema
  - title: "Article"
  - description: "A blog article or news item"
  - version: "1.0"
  - enabled: true
  + fields/
    + title/
      - label: "Title"
      - type: "string"
      - required: true
      - order: 10
      + validation/
        - minLength: 5
        - maxLength: 100
    + body/
      - label: "Body Content"
      - type: "richtext"
      - required: true
      - order: 20
    + author/
      - label: "Author"
      - type: "reference"
      - required: true
      - order: 30
    + publishDate/
      - label: "Publish Date"
      - type: "datetime"
      - required: true
      - order: 40
    + tags/
      - label: "Tags"
      - type: "tag"
      - multiple: true
      - order: 50
    + featuredImage/
      - label: "Featured Image"
      - type: "asset"
      - order: 60
      + validation/
        - required: false
```

## Field Types

| Field Type | Description | JCR Type |
|------------|-------------|----------|
| STRING | Single-line text | String |
| TEXT | Multi-line text | String |
| RICHTEXT | Rich formatted text | String |
| BOOLEAN | True/false | Boolean |
| INTEGER | Whole number | Long |
| DECIMAL | Decimal number | Double |
| DATE | Date only | Date |
| DATETIME | Date and time | Date |
| REFERENCE | Reference to another resource | String (path) |
| TAG | Taxonomy/tag reference | String/String[] |
| ASSET | Asset/file reference | String (path) |
| SELECT | Dropdown selection | String |
| RADIO | Radio button selection | String |
| CHECKBOX | Checkbox selection | String[] |
| HIDDEN | Hidden field | String |
| OBJECT | Nested object | nt:unstructured |
| ARRAY | Array of objects | nt:unstructured |

## Validation Rules

Validation rules are defined in the `validation` sub-node of each field:

- **required**: Field must have a value
- **minLength**: Minimum string length
- **maxLength**: Maximum string length
- **min**: Minimum numeric value
- **max**: Maximum numeric value
- **pattern**: Regular expression pattern to match

## Usage Examples

### Get a Schema

```java
@Reference
private SchemaManager schemaManager;

// Get schema by ID
ContentSchema schema = schemaManager.getSchema(resource, "article");

// Get all schemas
List<ContentSchema> schemas = schemaManager.getAllSchemas(resource);
```

### Validate Content

```java
// Validate content against a schema
ValidationResult result = schemaManager.validate(contentResource, schema);

if (!result.isValid()) {
    for (ValidationResult.ValidationError error : result.getErrors()) {
        log.error("Validation error in field {}: {}",
            error.getFieldName(), error.getMessage());
    }
}
```

### Access Schema from Content

```java
// Get schema associated with content
ContentSchema schema = schemaManager.getSchemaForResource(contentResource);

// Access schema fields
for (SchemaField field : schema.getFields()) {
    String name = field.getName();
    FieldType type = field.getType();
    boolean required = field.isRequired();
}
```

### Use in Sling Models

```java
@Model(adaptables = Resource.class)
public class ArticleModel {
    
    @Reference
    private SchemaManager schemaManager;
    
    @Self
    private Resource resource;
    
    private ContentSchema schema;
    
    @PostConstruct
    protected void init() {
        schema = schemaManager.getSchemaForResource(resource);
    }
    
    public ContentSchema getSchema() {
        return schema;
    }
    
    public boolean isValid() {
        if (schema == null) {
            return true; // No schema means no validation
        }
        ValidationResult result = schemaManager.validate(resource, schema);
        return result.isValid();
    }
}
```

## HTL Component Example

Create a component that displays validation errors:

```html
<sly data-sly-use.model="org.apache.sling.cms.core.models.ArticleModel">
    <sly data-sly-test="${!model.valid}">
        <div class="validation-errors">
            <h3>Validation Errors:</h3>
            <ul>
                <sly data-sly-list.error="${model.validationErrors}">
                    <li>${error.fieldName}: ${error.message}</li>
                </sly>
            </ul>
        </div>
    </sly>
</sly>
```

## Creating a New Schema

1. **Create schema node structure** at `/conf/{site}/schemas/{schema-id}`
2. **Define schema properties**:
   - `title`: Human-readable title
   - `description`: Schema description
   - `version`: Version number
   - `enabled`: Enable/disable schema
3. **Add fields** under `fields/` node with field properties
4. **Add validation rules** under `fields/{field-name}/validation/`

## Best Practices

1. **Use semantic field names**: Use clear, descriptive names (e.g., `publishDate` not `date1`)
2. **Version your schemas**: Increment version when making breaking changes
3. **Provide good descriptions**: Help authors understand what each field is for
4. **Set appropriate validation**: Balance strictness with usability
5. **Use inheritance**: Create base schemas for common patterns
6. **Order fields logically**: Use the `order` property to control field sequence
7. **Validate on save**: Implement save listeners that validate against schemas

## Integration with Page Templates

Schemas can be associated with page templates by setting a `schemaId` property:

```
/conf/global/templates/article
  - sling:resourceType: sling-cms/components/cms/template
  - title: "Article Template"
  - schemaId: "article"
```

## Future Enhancements

- **Auto-generated dialogs**: Generate component dialogs from schema definitions
- **Schema migration tools**: Tools to migrate content when schemas change
- **Advanced validation**: Custom validation rules and validators
- **Schema versioning**: Better support for schema evolution
- **JSON Schema export**: Export schemas as JSON Schema format
- **Schema inheritance UI**: Visual editor for schema inheritance

## Related Documentation

- [Page Editing](page-editing.md)
- [Templates](templates.md)
- [Managing Content](managing-content.md)
- [Component Development](custom-components.md)
