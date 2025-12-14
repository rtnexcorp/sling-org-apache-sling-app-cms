# New-age CMS – DAM Feature Status (Implemented vs Planned)

This document is a **gap analysis** based on `docs/digital-asset-management.md`.

Scope:
- Focuses on DAM capabilities (assets, thumbnails, renditions, transformations, metadata, UI).
- Marks what appears already supported vs what is a candidate for “new-age CMS” upgrades.

> Note: This is derived from documentation claims; it should be validated against runtime behavior and module code (`thumbnails`, `ui`, `core`) as needed.

---

## Implemented (already documented / expected to exist)

### Core asset management
- ✅ Centralized media storage and browsing (Asset Manager UI)
- ✅ Upload (single & bulk / drag-and-drop)
- ✅ Folder creation and organization
- ✅ Move/Copy/Rename via UI actions
- ✅ Download

### Thumbnails & previews
- ✅ Automatic thumbnails for many file types (images, videos, PDF, Office docs)
- ✅ Generic icon fallback for unsupported previews

### Image transformations (on-demand)
- ✅ URL-based transformations: `.../asset.jpg.transform/<preset>.png`
- ✅ Preset-driven transformations configured per-site under:
  - `/conf/{site}/files/transformations/`
- ✅ Caching behavior mentioned (generated on request, cached)

### Automatic renditions (image presets)
- ✅ Auto-renditions can be enabled, with configured transformation names
- ✅ Renditions view in asset properties (as documented)

### Video support (thumbnails)
- ✅ Video thumbnail generation via FFmpeg (frame extraction)
- ✅ Configurable FFmpeg path, frame position, timeout

### Metadata (manual)
- ✅ Metadata editing UI (title, description, alt text, tags/keywords, copyright, creator, source)
- ✅ Read-only info panel fields (size, mime type, dates, dimensions)

### Versioning
- ✅ File version history + restore (as documented)

---

## Partially implemented / unclear (needs validation)

### Search & filtering depth
- ⚠️ Filtering is documented as “type to filter by filename” and basic type filters.
  - Validate: Is it only client-side filtering of current folder, or repo-wide search?

### Metadata indexing & discovery
- ⚠️ Metadata fields exist, but indexing/searching by metadata is not clearly documented.
  - Validate: Can users search by tags/keywords across folders?

### Rendition strategy
- ⚠️ Auto-renditions exist for images, but not for PDFs/videos/Office.
  - Validate: Is that a limitation of pipeline vs just docs?

---

## Gaps / New-age features to implement (recommended)

### 1) Delivery presets (profiles) beyond “transformations”
**What**: A higher-level “delivery profile” abstraction (format + sizes + quality + cropping) designed for front-end delivery.

Why it matters:
- Today: transformations/presets exist, but they are mostly image-processing rules.
- New-age: delivery presets become *productized* and safe for authors/devs.

Deliverables:
- `/conf/{site}/dam/delivery-presets/*`
- A resolver that maps `preset + asset -> best rendition` (webp/avif fallback)

### 2) Smart renditions (on-demand generation with persistence + invalidation)
**What**: Generate renditions on first request, persist/cache, invalidate when originals change.

Deliverables:
- Deterministic cache key
- Rendition store policy (JCR vs filesystem vs hybrid)
- Admin controls (purge/invalidate)

### 3) Metadata pipeline (automatic extraction + enrichment)
**What**: Move from “manual metadata” to automated extraction + optional enrichers.

Examples:
- EXIF/IPTC/XMP extraction
- OCR for images/PDFs (optional)
- Video duration/codec extraction

Deliverables:
- Listener/job on asset upload/update
- Standard location: `jcr:content/metadata/*`

### 4) Governance (rights, licensing, expiry)
**What**: Enforce license/expiry and prevent accidental publishing/use.

Deliverables:
- Scheduled job to unpublish/disable delivery
- UI warnings + “where used” impact view

### 5) Collections & saved searches
**What**: Collections are references (not copies). Saved searches store query definitions.

Deliverables:
- Content structures + UI panels
- Sharing/permissions model

### 6) Duplicate detection
**What**: Hash binaries and find duplicates.

Deliverables:
- Store hash metadata
- UI for “duplicates”

---

## Suggested phased roadmap (small, shippable slices)

### Phase 1 (fast win)
- Delivery presets + deterministic rendition URLs
- Smart rendition cache store (basic)

### Phase 2
- Metadata pipeline (Tika extraction) + metadata search

### Phase 3
- Governance (expiry/licensing) + scheduled enforcement

### Phase 4
- Collections/saved searches + duplicate detection

---

## Notes on doc cleanup (optional)

From `digital-asset-management.md`, consider tightening language where behavior is not guaranteed:
- “Sling CMS maintains version history for all files” → confirm it’s enabled everywhere or qualify it.
- Renditions section → confirm UI actually shows renditions for all contexts.
- Filtering/search → clarify whether it’s folder-only filtering or global.

