# New-age CMS – Platform Feature Status (Implemented vs Planned)

This document is a **gap analysis** based on platform/operations documentation:
- `docs/administration.md`
- `docs/deployment-models.md`
- `docs/author-publish.md`
- `docs/securing.md`
- `docs/building.md`
- `docs/github-actions-ci.md`
- `docs/users.md` / `docs/admin-tools.md` (general platform operations)

Scope:
- Deployment topology, security, admin capabilities, publishing workflow, build/CI.

> Note: This is derived from documentation claims; validate in runtime and code as needed.

---

## Implemented (already documented / expected to exist)

### Deployment models & run modes
- ✅ Standalone instance support
- ✅ Author / Renderer instance separation
- ✅ Run modes (`author`, `renderer`) and dedicated JARs per mode
- ✅ Start scripts under `deployment/` documented
- ✅ Build profiles (`fast`, `unified`, `bun`, default)

### Publishing / distribution
- ✅ Standalone publish model via `sling:published=true`
- ✅ Author → Renderer publishing via HTTP-based Content Distribution
- ✅ Publication status fields:
  - `sling:published`
  - `sling:lastPublication`
  - `sling:lastPublicationType`
- ✅ UI actions: publish, unpublish, bulk publish (documented)
- ✅ Java API usage with `PublicationManager`

### Security
- ✅ CMS Security Filter configuration (host domain, allowed patterns, optional group)
- ✅ Sling Referrer Filter configuration
- ✅ Apache HTTPD hardening guidance + security headers
- ✅ Proxy path scoping guidance (don’t expose `/system`, `/etc`, etc.)

### Operations / administration
- ✅ Admin documentation entry points in `administration.md`
- ✅ Admin tooling section exists (`admin-tools.md`) (capabilities described there)

### CI / build
- ✅ GitHub Actions CI guidance (documented)
- ✅ Maven build guidance / profiles (documented)

---

## Partially implemented / unclear (needs validation)

### Observability
- ⚠️ Logging exists, but docs don’t clearly define:
  - request tracing / correlation IDs
  - structured logs
  - health endpoints / readiness probes
  - metrics (Micrometer/Prometheus)

### Environment promotion
- ⚠️ Author/Renderer topology exists, but docs don’t define a full “environments” model:
  - dev/stage/prod promotion
  - gated approvals
  - content freeze windows

### Secrets & config management
- ⚠️ Docs talk about OSGi configs; unclear if there is guidance for:
  - secrets handling (vault, env vars, file-based)
  - config-as-code patterns

### Backup / restore strategy
- ⚠️ Not clearly covered as a platform capability in docs.

---

## Gaps / New-age platform features to implement (recommended)

### 1) Observability baseline (logs + health + metrics)
**What**: Make production operations simple and standard.

Deliverables:
- Health checks: readiness/liveness endpoints
- Request correlation ID support (per request)
- Metrics export integration (pluggable if you want to avoid heavy deps)
- Basic dashboards documentation

### 2) Content quality / policy checks
**What**: Automated checks that prevent low-quality output.

Examples:
- broken link detection
- missing alt text
- large asset warnings
- stale content reports

Deliverables:
- Report generator service
- UI entry in Admin Tools for running checks

### 3) Audit log + change history UX
**What**: Human-friendly “who changed what” across pages, assets, configs.

Deliverables:
- Audit trail view + filters
- Export capability

### 4) Environment promotion workflows
**What**: Treat deployments as pipelines, not manual steps.

Deliverables:
- Named environments (dev/stage/prod)
- Promotion actions with approvals
- Rollback guidance

### 5) Config-as-code guardrails
**What**: Repeatable deployments.

Deliverables:
- Documented conventions for storing OSGi configs/content packages
- Validation tool that checks config completeness

---

## Suggested phased roadmap (small, shippable slices)

### Phase 1 (ops basics)
- Health/readiness + correlation IDs

### Phase 2 (visibility)
- Metrics + structured logging guidance

### Phase 3 (governance)
- Content quality checks + reports

### Phase 4 (enterprise ops)
- Audit log UX + environment promotion

---

## Notes on doc cleanup (optional)

Suggested documentation additions:
- A single “Production checklist” page (security, headers, ports, backups, monitoring).
- Observability guide (logs/metrics/tracing).
- Environment promotion guide.

