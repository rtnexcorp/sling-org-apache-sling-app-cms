# New-age CMS – Prioritized Backlog

This backlog is derived from the “new-age CMS” docs and feature-status gap analyses.

## How to use this file
- **Priority**: P0 (now) → P3 (later)
- **Modules**: where work should live (follow project conventions; DAM work should go to `thumbnails`)
- **Doc Ref**: source rationale / status doc

---

## P0 (Foundation / Highest ROI)

| ID | Pillar | Feature | Modules | Status | Doc Ref | Notes |
|---|---|---|---|---|---|---|
| NA-PLAT-001 | Platform | Health/Readiness endpoints | `core`, `feature` | Not Started | `docs/new-age-cms-platform-feature-status.md` | Ops baseline |
| NA-PLAT-002 | Platform | Request correlation IDs | `core` | Not Started | `docs/new-age-cms-platform-feature-status.md` | Observability baseline |
| NA-AUTO-001 | Automation | Content quality reports (Admin Tools) | `core`, `ui` | Not Started | `docs/new-age-cms-content-automation.md` | Start with reports; no AI |
| NA-AUTO-002 | Automation | Broken link detection | `core` | Not Started | `docs/new-age-cms-content-automation.md` | Integrate with quality report |
| NA-AUTO-003 | Automation | Alt-text policy check | `core`, `thumbnails` | Not Started | `docs/new-age-cms-content-automation.md` | Pairs well with DAM |
| NA-SITE-001 | Sites | Content Types/Schemas (definition + validation) | `core`, `ui` | Not Started | `docs/new-age-cms-sites-feature-status.md` | Foundation for headless + automation |
| NA-DAM-001 | DAM | Delivery presets (profiles) | `thumbnails`, `core` | Not Started | `docs/new-age-cms-dam-feature-status.md` | Delivery-first |
| NA-DAM-002 | DAM | Smart renditions (on-demand + caching + invalidation) | `thumbnails` | Not Started | `docs/new-age-cms-dam-feature-status.md` | Deterministic cache key |

---

## P1 (Build on foundation)

| ID | Pillar | Feature | Modules | Status | Doc Ref | Notes |
|---|---|---|---|---|---|---|
| NA-AI-001 | AI | AI provider abstraction (OSGi) | `api`, `core` | Not Started | `docs/new-age-cms-ai.md` | Keep CMS functional when disabled |
| NA-AI-002 | AI | Suggest title/summary (suggest-only) | `core`, `ui` | ✅ Completed | `docs/new-age-cms-ai.md` | Human-in-the-loop only |
| NA-PLAT-003 | Platform | Metrics export (pluggable) | `core` | Not Started | `docs/new-age-cms-platform-feature-status.md` | After correlation IDs |
| NA-SITE-002 | Sites | Schema-driven dialogs | `ui`, `core` | Not Started | `docs/new-age-cms-sites-feature-status.md` | Build on schema validation |
| NA-SITE-003 | Sites | References graph + impact analysis | `core`, `ui` | Not Started | `docs/new-age-cms-sites-feature-status.md` | Complements move/copy/delete |
| NA-DAM-003 | DAM | Metadata pipeline (Tika extraction) | `thumbnails`, `core` | Not Started | `docs/new-age-cms-dam-feature-status.md` | Job-based |
| NA-DAM-004 | DAM | Metadata search/indexing | `core`, `ui` | Not Started | `docs/new-age-cms-dam-feature-status.md` | Depends on metadata pipeline |

---

## P2 (Advanced authoring + governance)

| ID | Pillar | Feature | Modules | Status | Doc Ref | Notes |
|---|---|---|---|---|---|---|
| NA-PERS-001 | Personalisation | Segments (rules → segment IDs) | `core` | Not Started | `docs/new-age-cms-personalisation-feature-status.md` | Start minimal inputs |
| NA-PERS-002 | Personalisation | Targeted variants per component | `core`, `ui` | Not Started | `docs/new-age-cms-personalisation-feature-status.md` | Requires authoring UX |
| NA-SITE-004 | Sites | Preview tokens + draft rendering | `core`, `ui` | Not Started | `docs/new-age-cms-sites-feature-status.md` | After schemas |
| NA-DAM-005 | DAM | Governance (license/expiry enforcement) | `thumbnails`, `core`, `ui` | Not Started | `docs/new-age-cms-dam-feature-status.md` | Add “where used” warnings |
| NA-AUTO-004 | Automation | Scheduled publish/unpublish | `core`, `distribution` | Not Started | `docs/new-age-cms-content-automation.md` | Ties into governance |

---

## P3 (Personalisation maturity + enterprise ops)

| ID | Pillar | Feature | Modules | Status | Doc Ref | Notes |
|---|---|---|---|---|---|---|
| NA-PERS-003 | Personalisation | A/B experiments + bucketing | `core`, `ui` | Not Started | `docs/new-age-cms-personalisation-feature-status.md` | After segments + variants |
| NA-PERS-004 | Personalisation | Event capture + dashboards | `core`, `ui` | Not Started | `docs/new-age-cms-personalisation-feature-status.md` | Integrate with content-insights |
| NA-SITE-005 | Sites | Multi-site rollouts (lightweight) | `core`, `ui` | Not Started | `docs/new-age-cms-sites-feature-status.md` | After references graph |
| NA-SITE-006 | Sites | i18n/translation workflow (light) | `core`, `ui` | Not Started | `docs/new-age-cms-sites-feature-status.md` | Pairs with AI translations later |
| NA-DAM-006 | DAM | Collections & saved searches | `core`, `ui` | Not Started | `docs/new-age-cms-dam-feature-status.md` | User productivity |
| NA-DAM-007 | DAM | Duplicate detection (hashing) | `thumbnails`, `core`, `ui` | Not Started | `docs/new-age-cms-dam-feature-status.md` | Needs storage/indexing |
| NA-PLAT-004 | Platform | Audit log UX | `core`, `ui` | Not Started | `docs/new-age-cms-platform-feature-status.md` | Complements automation+AI |
| NA-PLAT-005 | Platform | Environment promotion workflows | `core`, `distribution`, `docs` | Not Started | `docs/new-age-cms-platform-feature-status.md` | Docs + tooling |

