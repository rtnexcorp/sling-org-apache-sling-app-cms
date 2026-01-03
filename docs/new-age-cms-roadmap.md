# New-age CMS – Development Roadmap

This roadmap consolidates the feature gaps and planned work from the new-age CMS documentation into a phased delivery plan.

**Source Documents:**
- [Backlog](new-age-cms-backlog.md)
- [DAM Feature Status](new-age-cms-dam-feature-status.md)
- [Sites Feature Status](new-age-cms-sites-feature-status.md)
- [Platform Feature Status](new-age-cms-platform-feature-status.md)
- [Content Automation](new-age-cms-content-automation.md)
- [AI Integration](new-age-cms-ai.md)
- [Personalisation](new-age-cms-personalisation-feature-status.md)

---

## Vision

Build a modern, developer-friendly CMS that:
- **Don't build AEM-like solution** – build something unique for the new age
- Prioritizes **delivery** over authoring complexity
- Makes **automation** assist authors, not replace them
- Keeps **AI optional** with human-in-the-loop patterns
- Supports **headless** and traditional rendering equally
- Is **lightweight, composable, and transparent**

---

## Phase 1: Foundation (Q1)
*Build the operational and architectural baseline*

### Platform (Ops Baseline)
| Feature | ID | Modules | Effort | Notes |
|---------|-----|---------|--------|-------|
| Health/Readiness endpoints | NA-PLAT-001 | `core`, `feature` | S | ✅ Already using Felix Health Checks; Helm probes updated |
| Request correlation IDs | NA-PLAT-002 | `core` | M | Add MDC-based correlation for request tracing |
| Structured logging guidance | - | `docs` | S | Document log patterns for production |

### Content Quality (No-AI Automation)
| Feature | ID | Modules | Effort | Notes |
|---------|-----|---------|--------|-------|
| Content quality reports | NA-AUTO-001 | `core`, `ui` | M | Admin Tools extension; report generator |
| Broken link detection | NA-AUTO-002 | `core` | M | Crawler job + report integration |
| Alt-text policy check | NA-AUTO-003 | `core`, `thumbnails` | S | Flag images without alt text |

### DAM Delivery
| Feature | ID | Modules | Effort | Notes |
|---------|-----|---------|--------|-------|
| Delivery presets (profiles) | NA-DAM-001 | `thumbnails`, `core` | M | `/conf/{site}/dam/delivery-presets/*` |
| Smart renditions | NA-DAM-002 | `thumbnails` | L | On-demand + caching + invalidation |

### Sites Foundation
| Feature | ID | Modules | Effort | Notes |
|---------|-----|---------|--------|-------|
| Content Types/Schemas | NA-SITE-001 | `core`, `ui` | L | Schema definitions + validation on save |

**Phase 1 Exit Criteria:**
- [ ] Health endpoints return meaningful status
- [ ] Correlation IDs appear in logs
- [ ] Content quality report runs manually
- [ ] One delivery preset working end-to-end
- [ ] Schema validation blocks invalid content

---

## Phase 2: Build on Foundation (Q2)
*Add intelligence and deeper integrations*

### AI Provider Abstraction
| Feature | ID | Modules | Effort | Status | Notes |
|---------|-----|---------|--------|--------|-------|
| AI provider abstraction | NA-AI-001 | `api`, `core` | M | ✅ Completed | OSGi services; CMS works when AI disabled |
| Suggest title/summary | NA-AI-002 | `core`, `ui` | M | ✅ Completed | Human-in-the-loop only; author approves |

### Platform Observability
| Feature | ID | Modules | Effort | Notes |
|---------|-----|---------|--------|-------|
| Metrics export (pluggable) | NA-PLAT-003 | `core` | M | Dropwizard/Micrometer adapter |

### Sites Authoring
| Feature | ID | Modules | Effort | Notes |
|---------|-----|---------|--------|-------|
| Schema-driven dialogs | NA-SITE-002 | `ui`, `core` | L | Auto-generate dialog from schema |
| References graph | NA-SITE-003 | `core`, `ui` | M | "Where used" before move/delete |

### DAM Metadata
| Feature | ID | Modules | Effort | Notes |
|---------|-----|---------|--------|-------|
| Metadata pipeline | NA-DAM-003 | `thumbnails`, `core` | M | Tika extraction on upload |
| Metadata search | NA-DAM-004 | `core`, `ui` | M | Index + search by tags/keywords |

**Phase 2 Exit Criteria:**
- [ ] AI suggestions work with at least one provider
- [ ] Metrics exported to Prometheus-compatible endpoint
- [ ] Dialogs can be generated from schema definitions
- [ ] "Where used" shows before destructive actions
- [ ] Metadata auto-extracted and searchable

---

## Phase 3: Governance & Personalisation (Q3)
*Add enterprise governance and targeting capabilities*

### Personalisation Foundation
| Feature | ID | Modules | Effort | Notes |
|---------|-----|---------|--------|-------|
| Segments (rules → IDs) | NA-PERS-001 | `core` | M | Minimal inputs (geo, device, etc.) |
| Targeted variants | NA-PERS-002 | `core`, `ui` | L | Per-component variant authoring |

### Sites Preview & Draft
| Feature | ID | Modules | Effort | Notes |
|---------|-----|---------|--------|-------|
| Preview tokens | NA-SITE-004 | `core`, `ui` | M | Signed tokens + draft rendering |

### DAM Governance
| Feature | ID | Modules | Effort | Notes |
|---------|-----|---------|--------|-------|
| License/expiry enforcement | NA-DAM-005 | `thumbnails`, `core`, `ui` | M | Warnings + publish blocking |

### Automation Scheduling
| Feature | ID | Modules | Effort | Notes |
|---------|-----|---------|--------|-------|
| Scheduled publish/unpublish | NA-AUTO-004 | `core`, `distribution` | M | Sling scheduler + workflow |

**Phase 3 Exit Criteria:**
- [ ] Segments defined and evaluated at request time
- [ ] Components can have targeted variants
- [ ] Preview tokens allow safe external sharing
- [ ] Expired assets block publishing
- [ ] Scheduled publish works reliably

---

## Phase 4: Enterprise Maturity (Q4)
*Scale to enterprise use cases*

### Personalisation Analytics
| Feature | ID | Modules | Effort | Notes |
|---------|-----|---------|--------|-------|
| A/B experiments | NA-PERS-003 | `core`, `ui` | L | Bucketing + variant assignment |
| Event capture | NA-PERS-004 | `core`, `ui` | M | Integrate with content-insights |

### Multi-site & i18n
| Feature | ID | Modules | Effort | Notes |
|---------|-----|---------|--------|-------|
| Multi-site rollouts | NA-SITE-005 | `core`, `ui` | L | Blueprint/inheritance patterns |
| Translation workflow | NA-SITE-006 | `core`, `ui` | L | AI-assisted + review gates |

### DAM Productivity
| Feature | ID | Modules | Effort | Notes |
|---------|-----|---------|--------|-------|
| Collections & saved searches | NA-DAM-006 | `core`, `ui` | M | User productivity |
| Duplicate detection | NA-DAM-007 | `thumbnails`, `core`, `ui` | M | Hash-based detection |

### Platform Enterprise
| Feature | ID | Modules | Effort | Notes |
|---------|-----|---------|--------|-------|
| Audit log UX | NA-PLAT-004 | `core`, `ui` | M | Searchable audit trail |
| Environment promotion | NA-PLAT-005 | `core`, `distribution`, `docs` | L | Dev→Stage→Prod workflows |

**Phase 4 Exit Criteria:**
- [ ] A/B experiments assignable and trackable
- [ ] Multi-site inheritance working
- [ ] Translation workflow with AI suggestions
- [ ] Duplicate assets detected on upload
- [ ] Full audit trail searchable by admins

---

## Module Ownership

| Module | Primary Responsibility |
|--------|----------------------|
| `api` | Public interfaces, AI provider contracts |
| `core` | Services, models, automation jobs, schemas |
| `ui` | Authoring UI, admin tools, dialogs |
| `thumbnails` | DAM: renditions, metadata extraction, governance |
| `distribution` | Publishing, workflow, scheduling |
| `frontend` | CSS/JS for authoring experience |
| `feature` | Runtime bundle configuration |

---

## Effort Legend

| Size | Description |
|------|-------------|
| S | < 1 week; isolated change |
| M | 1-2 weeks; multiple components |
| L | 2-4 weeks; cross-cutting or new subsystem |
| XL | > 4 weeks; major feature requiring design |

---

## Quick Wins (Can Start Now)

These items have minimal dependencies and high value:

1. **Content quality report skeleton** – Admin Tools page that runs checks
2. **Alt-text policy check** – Simple query for images without alt text
3. **Broken link detector** – Crawler job with report output
4. **Delivery preset MVP** – One preset with format/quality/size
5. **Schema definition format** – Document the JSON schema structure

---

## Anti-Patterns to Avoid

| Don't | Do Instead |
|-------|------------|
| Build AEM-like solution | Build something unique for the new age |
| Copy enterprise CMS complexity | Keep it lightweight and composable |
| AI-first features | Human-in-the-loop; AI assists |
| Embed non-OSGi libraries | Use OSGi bundles or ServiceMix wrappers |
| Monolithic components | Small, focused services |
| Hidden automation | Auditable, configurable, transparent |
| Over-engineer authoring UX | Focus on delivery and developer experience |

---

## Related Documentation

- [DAM Proposal](new-age-cms-dam.md)
- [Sites Proposal](new-age-cms-sites.md)
- [Platform Proposal](new-age-cms-platform.md)
- [Personalisation Proposal](new-age-cms-personalisation.md)
- [AI Integration](new-age-cms-ai.md)
- [Content Automation](new-age-cms-content-automation.md)
