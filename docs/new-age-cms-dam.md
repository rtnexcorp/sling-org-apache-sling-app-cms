# New-age CMS – Digital Asset Management (DAM)

## Goal
Build a modern DAM experience focused on **delivery**, **automation**, **governance**, and **developer composability** (not a monolithic AEM-style DAM).

## Principles
- **OSGi-friendly**: prefer OSGi bundles; keep DAM code in `thumbnails` module.
- **Composable**: features are small services/models/components that can be independently enabled.
- **Delivery-first**: optimize for fast image/video delivery and caching.
- **Transparent**: no “magic”; configuration lives in content (`/conf`) and is auditable.

## Feature candidates
### 1. Delivery presets (profiles)
**What**: Named presets like `web-card`, `hero`, `thumb` with format, size, quality, crop mode.

**User value**
- Consistent media usage across sites/components.
- Easier performance tuning without editing templates.

**Implementation sketch**
- Config stored under `/conf/.../dam/delivery-presets/*`.
- Resolver logic chooses rendition based on preset + source asset.
- Cache key includes preset + lastModified + transformation params.

### 2. Smart renditions (on-demand generation + caching)
**What**: Create renditions on first request, persist and/or cache.

**Implementation sketch**
- Servlet endpoint: `/cms/assets/rendition.<preset>.<ext>` or similar.
- Background job optional for heavy transforms.
- Use existing thumbnail/rendition infrastructure in `thumbnails`.

### 3. Metadata pipeline
**What**: Extract and normalize metadata (Tika + optional enrichers).

**Implementation sketch**
- Ingest hook/listener for new binaries.
- Write results to `jcr:content/metadata/*`.
- Provide UI display + searchable fields.

### 4. Governance: licensing, expiry, usage policy
**What**: License/expiry fields with scheduled enforcement.

**Implementation sketch**
- Scheduled job checks expiry and unpublishes / blocks delivery.
- “Where used” view to assess impact.

### 5. Collections & saved searches
**What**: Collections are references to assets; saved searches persist queries.

**Implementation sketch**
- Store collection nodes under user profile or `/content/dam/collections`.
- UI: add/remove assets, share collection.

### 6. Duplicate detection
**What**: Hash binaries; find duplicates across folders.

**Implementation sketch**
- Store hash under metadata.
- Query by hash; UI highlights duplicates.

## Suggested incremental roadmap
1. Delivery presets + on-demand renditions
2. Metadata pipeline + UI display
3. Governance (expiry/licensing) + scheduled enforcement
4. Collections + duplicate detection

## Open questions
- Should preset config be global (`/conf/global`) or per site?
- Renditions: persist in JCR, store on filesystem, or hybrid?
