# New-age CMS – Sites

## Goal
Build a **fast, composable, multi-channel** sites platform that stays simple:
- content types + components
- real preview
- clear relationships
- easy multi-site + i18n

## Principles
- **Content types over hardcoded dialogs** (schema-driven authoring).
- **Preview and environments are first-class** (draft/stage/prod).
- **References are visible** (“where used”, impact analysis).

## Feature candidates
### 1. Content Types / Schemas
**What**: Define field schemas for pages/content fragments.

**Key capabilities**
- Field definitions (type, label, required, allowed values)
- Validation on save
- Generated dialogs automatically

**Implementation sketch**
- Store schemas under `/conf/.../schemas/...`.
- Provide a Sling Model + validation service.
- UI uses schema to render consistent authoring.

### 2. Live Preview with draft tokens
**What**: Preview drafts without publishing.

**Implementation sketch**
- Signed preview token stored in cookie or query param.
- Render pipeline uses token to resolve draft content.

### 3. Multi-site management
**What**: Multi-site rollouts without heavy MSM.

**Implementation sketch**
- Simple inheritance rules:
  - overlays in `/apps`
  - per-site config in `/conf/<site>`
- “Promote changes” workflow for shared templates.

### 4. i18n + translation workflow (light)
**What**: Track translation status and allow field-level fallback.

### 5. Relationship graph & impact analysis
**What**: Show references between pages/assets/components.

**Implementation sketch**
- Index references (`sling:reference` style) in a lightweight store.
- UI panel for “Referenced by / References”.

## Suggested incremental roadmap
1. Content types + validation service
2. Generated dialogs + consistent UI
3. Preview tokens + environment support
4. References graph + impact analysis
