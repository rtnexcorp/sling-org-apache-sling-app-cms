# New-age CMS – Sites Feature Status (Implemented vs Planned)

This document is a **gap analysis** based on the existing Sites-related documentation:
- `docs/configure-site.md`
- `docs/page-editing.md`
- `docs/templates.md`
- `docs/managing-content.md`

Scope:
- Site groups / sites, pages, templates, authoring UX, component model.

> Note: This is derived from documentation claims; validate in runtime and code as needed.

---

## Implemented (already documented / expected to exist)

### Multi-site foundation
- ✅ Site Groups (tenant/grouping concept)
- ✅ Sites belong to Site Groups
- ✅ Per-site configuration via Context-Aware Configuration (CAConfig)
  - Default `/conf/global`
  - Custom site configuration supported

### URL & rendering configuration
- ✅ Primary URL per site (for published URL generation)
- ✅ Sling Mappings guidance for multi-site friendly URLs
- ✅ Rewriter configuration mentioned (doctype + rewritten attributes)

### Page authoring
- ✅ Page editor UI
  - Page-level actions: properties edit, versions, move/copy, delete
  - In-context component editor
- ✅ Containers and component insertion via modal

### Templates
- ✅ Templates live under site configuration
- ✅ Template fields include:
  - Title
  - Allowed Paths (regex)
  - Component Policies
  - Template Content (JSON)
- ✅ Template Content supports variables from configuration fields
- ✅ Handlebar templating for template content generation

### General content operations
(Per `managing-content.md`)
- ✅ Console pattern: left nav, create bar, breadcrumb, filter, table/list, actions
- ✅ Pages/Folders/Files supported inside sites
- ✅ Move/Copy content with optional reference update
- ✅ Delete with optional replacement and reference update
- ✅ Version management for content

---

## Partially implemented / unclear (needs validation)

### Filtering and search scope
- ⚠️ “Filter for filtering content” is documented.
  - Validate: is it folder-only filtering or global search?
  - Validate: is search queryable via API for integrations?

### References and dependency visibility
- ⚠️ References are updated on move/copy/delete.
  - Validate: is there a “Where used” UI for authors before they change content?

### Environment support
- ⚠️ Docs describe author/publish/renderer topology, but not a full “environments” model.
  - Validate: is there a first-class concept of draft/stage/prod across the tooling?

---

## Gaps / New-age features to implement (recommended)

### 1) Content Types / Schemas (schema-driven authoring) ✅ **IMPLEMENTED (Dec 2025)**
**What**: A first-class schema definition for pages/content fragments.

**Status**: **Fully implemented** - Ready for production use

**Completed Implementation**:
- ✅ ContentSchema API with SchemaField definitions
- ✅ SchemaManager OSGi service for schema management
- ✅ Validation framework with ValidationResult and detailed error reporting
- ✅ 20+ field types supported (string, text, richtext, boolean, integer, decimal, date, datetime, reference, tag, asset, select, radio, checkbox, hidden, object, array)
- ✅ Built-in validation rules:
  - Required fields
  - String length (minLength, maxLength)
  - Numeric ranges (min, max)
  - Pattern matching (regex)
  - Type validation
- ✅ CAConfig integration for schema storage at `/conf/{site}/schemas/*`
- ✅ Schema inheritance support (parent schemas)
- ✅ Enable/disable schemas per environment
- ✅ Example schemas: article and product
- ✅ Comprehensive unit tests
- ✅ Full documentation in [content-schemas.md](content-schemas.md)

**Usage Example**:
```java
@Reference
private SchemaManager schemaManager;

ContentSchema schema = schemaManager.getSchema(resource, "article");
ValidationResult result = schemaManager.validate(contentResource, schema);

if (!result.isValid()) {
    for (ValidationResult.ValidationError error : result.getErrors()) {
        log.error("Validation error: {} - {}", 
            error.getFieldName(), error.getMessage());
    }
}
```

**Next Steps for Enhancement**:
- Auto-generated dialogs from schema definitions
- Schema migration tools for content evolution
- Visual schema editor in CMS admin UI
- JSON Schema export/import
- Custom validation rule plugins

**Documentation**: See [docs/content-schemas.md](content-schemas.md)

**Related Code**:
- API: `api/src/main/java/org/apache/sling/cms/schema/`
- Implementation: `core/src/main/java/org/apache/sling/cms/core/internal/schema/`
- Examples: `ui/src/main/resources/jcr_root/conf/global/site/schemas/`

### 2) Preview tokens + draft rendering
**What**: Preview unpublished changes without full publish.

Deliverables:
- Signed preview tokens
- Draft resolution strategy
- Clear UI entry points

### 3) References graph + impact analysis
**What**: Show "Referenced by / References" before moving/deleting/publishing.

Deliverables:
- Reference index (lightweight)
- UI panel for impact analysis

### 4) Multi-site rollouts (lightweight MSM alternative)
**What**: Controlled propagation of shared templates/policies/config across sites.

Deliverables:
- Inheritance rules
- “Promote changes” workflow
- Conflict detection

### 5) i18n / translation workflow (light)
**What**: Translation status + fallback rules per field.

Deliverables:
- Translation status metadata
- Compare view across locales
- Field fallback policy

### 6) Headless delivery endpoints (optional, composable)
**What**: JSON endpoints for pages/fragments with stable contracts.

Deliverables:
- Content type-driven serializers
- Cache headers / ETags

---

## Suggested phased roadmap (small, shippable slices)

### Phase 1
- Content Types/Schemas (definition + validation)

### Phase 2
- Schema-driven dialogs + consistent authoring patterns

### Phase 3
- References graph + impact analysis

### Phase 4
- Preview tokens + draft rendering

### Phase 5
- Multi-site rollouts + i18n workflow

---

## Notes on doc cleanup (optional)

Potential doc improvements / clarifications:
- Clarify whether filtering is folder-local or global.
- Clarify where templates/policies are expected to live across site configs.
- Add a short section on “references”: what is updated automatically vs what authors can preview.

