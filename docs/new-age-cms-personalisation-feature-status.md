# New-age CMS – Personalisation Feature Status (Implemented vs Planned)

This document is a **gap analysis** for personalisation capabilities.

Sources used:
- There is no dedicated personalisation doc in the existing set.
- This analysis therefore uses:
  - Site and authoring docs for what targeting would need to integrate with (`page-editing.md`, `templates.md`).
  - DAM/content docs for what could be personalised (assets/pages).
  - `docs/new-age-cms-personalisation.md` as the proposed direction.

> Note: This is intentionally conservative: if it’s not clearly documented, it is marked as “not implemented / unclear”.

---

## Implemented (already documented / expected to exist)

### Segmentation / targeting
- ❌ Not documented as an existing feature.

### Content variants
- ❌ Not documented as an existing feature.

### Experiments (A/B)
- ❌ Not documented as an existing feature.

### Analytics / attribution
- ⚠️ `content-insights.md` exists, but it does not describe personalisation-specific tracking.

---

## Available building blocks (can be reused)

These are platform features you can leverage to implement personalisation cleanly:

- ✅ Context-Aware Configuration (CAConfig) for storing segment and experiment definitions under `/conf/...`
- ✅ Component-based page composition (variants can be modeled as child resources)
- ✅ Sling Models to evaluate request context and select variants during rendering
- ✅ Author/Renderer topology + publish workflow (targeting rules can be authored on author, published to renderer)

---

## Gaps / New-age personalisation features to implement (recommended)

### 1) Segments (rules → segment IDs)
**What**: Define segments as rule sets that evaluate a request.

Inputs (minimal viable):
- request path
- query parameters
- cookies
- logged-in user / groups

Deliverables:
- Segment definitions under `/conf/{site}/personalisation/segments/*`
- `SegmentService` (OSGi service) that returns active segments for a request
- Admin UI listing segments + rule editor

### 2) Targeted variants per component
**What**: A component can render `default` content or a variant for a matching segment.

Deliverables:
- Convention for storing variants, e.g.:
  - `component/variants/<segmentId>`
- Renderer logic:
  - select best match (priority order)
  - fallback to `default`
- Authoring UX for creating/editing variants

### 3) Experiments (A/B and simple multivariate)
**What**: Traffic split across variants with sticky assignment.

Deliverables:
- Experiment definitions under `/conf/{site}/personalisation/experiments/*`
- Bucketing (cookie-based) with deterministic assignment
- Guardrails: allow only published variants to enter experiment

### 4) Event capture + minimal dashboards
**What**: Capture impressions/clicks and show simple results.

Deliverables:
- Lightweight event endpoint (authoring UI + renderer)
- Storage strategy (simple JCR nodes or append-only logs)
- Basic dashboards (per experiment)

### 5) Governance and safety
**What**: Make personalisation safe and debuggable.

Deliverables:
- Debug mode: show matched segments + chosen variant
- Preview mode: force segment via query param (author-only)
- Audit trail for segment/experiment changes

---

## Suggested phased roadmap (small, shippable slices)

### Phase 1
- SegmentService + segment definitions in `/conf`
- Debug endpoint / UI indicator (matched segments)

### Phase 2
- Variant authoring + rendering convention (`variants/<segmentId>`)

### Phase 3
- A/B experiments + cookie bucketing

### Phase 4
- Event capture + dashboards

### Phase 5
- Governance (previews, audit, rollout controls)

---

## Notes on docs to add (optional)

If you want to make personalisation a first-class area in docs:
- `docs/personalisation.md` (author guide)
- `docs/experimentation.md`
- Extend `docs/content-insights.md` with experiment metrics

