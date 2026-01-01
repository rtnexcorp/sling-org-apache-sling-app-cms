# New-age CMS – DAM Feature Status (Implementation Tracking)

This document tracks the implementation status of DAM (Digital Asset Management) features for Apache Sling CMS, focusing on "new-age" CMS capabilities.

---

## Validation Summary

| Feature | Status | Evidence |
|---------|--------|----------|
| Global search across folders | ✅ Done | `SearchResults.java` with no path restriction |
| Metadata indexed | ✅ Done | `IndexCreator.java` - `allProperties` pattern |
| Tag/taxonomy search | ✅ Done | `slingTaxonomy` indexed + `TaxonomyService` |
| Client-side filtering | ✅ Done | `cms.nav.js` with type/tag filters |
| Full-text Lucene search | ✅ Done | `CONTAINS()` queries in SearchResults |
| **Search UI Enhancement** | ✅ **Done** | **Advanced filters, tag dropdowns, taxonomy badges** |
| Rendition strategy (multi-format) | ✅ Done | PDF, video, Office doc thumbnails |
| OCR metadata extraction | ✅ Done | `OCRMetadataEnricher` with Tesseract support |
| File version history | ✅ Done | Version restore functionality |

---

## ✅ VALIDATED - Search & Metadata Features (Confirmed in Codebase)

### Search & filtering depth
- ✅ **Server-side search**: `SearchResults.java` uses JCR-SQL2 with Lucene `CONTAINS()`
- ✅ **Client-side filtering**: `cms.nav.js` provides instant filtering for loaded items
  - Filter by filename/text
  - Filter by MIME type  
  - Filter by taxonomy/tags (`data-asset-tag-filter`)

### Metadata indexing & discovery
- ✅ **VALIDATED**: Metadata IS indexed and searchable via Lucene
- ✅ **`slingTaxonomy`** property indexed at `jcr:content/sling:taxonomy` (analyzed)
- ✅ **`allProperties`** pattern indexes ALL `jcr:content/*` metadata
- ✅ **TaxonomyService.getTaggedContent()** - Find content by tag across folders

### Lucene Index Configuration (IndexCreator.java)

| Property | Path | Indexed | Analyzed | Notes |
|----------|------|---------|----------|-------|
| jcr:title | `jcr:content/jcr:title` | ✅ | ✅ | 2x boost |
| jcr:description | `jcr:content/jcr:description` | ✅ | ✅ | |
| sling:taxonomy | `jcr:content/sling:taxonomy` | ✅ | ✅ | Tag search |
| allProperties | `jcr:content/*` (regex) | ✅ | ✅ | All metadata |
| nodeName | `LOCALNAME()` | ✅ | ✅ | Filename search |

### Search API (SearchResults.java)

| Parameter | Default | Description |
|-----------|---------|-------------|
| `q` or `term` | required | Search query |
| `type` | `sling:Page` | Node type (`nt:hierarchyNode` for all) |
| `path` | none | Restrict to path |
| `fulltext` | `true` | Use Lucene vs LIKE |

---

## ✅ COMPLETED - Additional Validated Features

### Rendition Strategy
- ✅ **VALIDATED/IMPLEMENTED**: Auto-renditions now support images, PDFs, videos, and Office documents
  - `AutoRenditionConfigImpl.java` expanded default MIME types
  - `PdfThumbnailProvider` - Uses PDFBox for PDF first-page rendering
  - `VideoThumbnailProvider` - Uses FFmpeg/JCodec for video frame extraction
  - `SlideShowThumbnailProvider` - PowerPoint (PPT/PPTX) via Apache POI
  - `WordThumbnailProvider` - Word documents (DOC/DOCX) via Apache POI
  - `SpreadsheetThumbnailProvider` - Excel spreadsheets (XLS/XLSX) via Apache POI
  - `TikaFallbackProvider` - Generic text extraction fallback

### Search UI Enhancement
- ✅ **IMPLEMENTED**: Search UI has been enhanced with all requested features:
  - ✅ Dedicated "Search by Tag" filtering in navigation (`cms.nav.js`)
  - ✅ Advanced search filters (MIME type, tags/taxonomy)
  - ✅ Real-time client-side filtering in content browser
  - ✅ Taxonomy badges displayed in search results
  - ✅ Date range filtering support
  - ✅ Type-based filtering (images, videos, documents, etc.)

### Search & Global Discovery
- ✅ **Global search across all folders** via `SearchResults` model using Lucene indexes
- ✅ **Metadata indexed and searchable** - `allProperties` pattern indexes `jcr:content/*`
- ✅ **Taxonomy/tags indexed** - `slingTaxonomy` property is indexed and analyzed
- ✅ **Full-text search** - Uses `CONTAINS()` with Oak Lucene for fast queries
- ✅ **Client-side filtering** - Real-time filtering in content navigation (`cms.nav.js`)
  - Filter by filename/text
  - Filter by MIME type
  - Filter by taxonomy/tags

### TaxonomyService API
- ✅ `getAllTaxonomyItems()` - Get all taxonomy items
- ✅ `searchTaxonomy(query)` - Search taxonomy by name
- ✅ `getTaggedContent(taxonomyPath, basePath, limit)` - Find content by tag across folders

---

## Validated (confirmed in codebase)

### Lucene Indexes (IndexCreator.java)
The following properties are indexed for search:

| Index | Property | Analyzed | Notes |
|-------|----------|----------|-------|
| slingPage | `jcr:content/jcr:title` | ✅ | 2x boost |
| slingPage | `jcr:content/jcr:description` | ✅ | |
| slingPage | `jcr:content/sling:taxonomy` | ✅ | Tag search |
| slingPage | `jcr:content/*` (allProperties) | ✅ | All metadata |
| slingFile | Same as above | ✅ | Files/assets |
| ntHierarchyNode | Same as above | ✅ | All content |
| slingTaxonomy | `jcr:title` | ✅ | Taxonomy items |

### Search Capabilities (SearchResults.java)
- ✅ Supports `q` or `term` parameter
- ✅ Supports `type` parameter (default: `sling:Page`, or `nt:hierarchyNode` for global)
- ✅ Supports `path` parameter for scoped search
- ✅ Supports `fulltext` parameter (default: true for Lucene)

---

## Partially implemented / needs enhancementd)

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
- ✅ Preset-driven transformations configured globally under:
  - `/conf/global/dam/transformations/`
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
- ✅ **VALIDATED/IMPLEMENTED**: Auto-renditions now support images, PDFs, videos, and Office documents
  - Supported MIME types configured in `AutoRenditionConfigImpl.java`
  - See thumbnails module providers for implementation details

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

### Phase 2 - ✅ DONE (metadata search already works!)
- ~~Metadata pipeline (Tika extraction) + metadata search~~
- **Status**: Lucene indexes ALL `jcr:content/*` properties including taxonomy/tags
- **Remaining**: Consider adding Tika auto-extraction on upload (optional enhancement)

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

