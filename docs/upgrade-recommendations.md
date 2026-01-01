<!-- Licensed to the Apache Software Foundation (ASF) under one or more contributor
	license agreements. See the NOTICE file distributed with this work for additional
	information regarding copyright ownership. The ASF licenses this file to
	you under the Apache License, Version 2.0 (the "License"); you may not use
	this file except in compliance with the License. You may obtain a copy of
	the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required
	by applicable law or agreed to in writing, software distributed under the
	License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
	OF ANY KIND, either express or implied. See the License for the specific
	language governing permissions and limitations under the License. -->

# Upgrade Recommendations & Implementation Plan

This document captures actionable recommendations to modernize and harden the Sling CMS codebase with an emphasis on **low-risk, high-impact** improvements.

## Goals

1. Reduce long-term maintenance cost.
2. Improve security posture (especially XSS/escaping risk).
3. Improve UI resiliency and consistency.
4. Make future UI framework upgrades (Bulma/Jam Icons) require minimal code change.
5. Consolidate Digital Asset Management (DAM) code into the `thumbnails` module per current architecture direction.

## Current State (Observed)

- UI still contains many **JSP-based components** under `ui/src/main/resources/jcr_root/libs/**`.
- UI templates use **Bulma CSS classes** and **Jam Icons classes** directly in markup.
- JS improvements have begun (see `ui/HIGH_PRIORITY_IMPROVEMENTS.md`) but a few remaining modules still need the same error-handling patterns.
- DAM capabilities are spread across modules; future-facing target is DAM logic in `thumbnails`.

## Recommendations (Priority Order)

### 1) Complete JSP → HTL Migration (Highest Priority)

**Why**
- JSPs increase risk of escaping mistakes and make future refactors harder.
- HTL provides strong XSS defaults and a cleaner separation of concerns.

**What to do**
- Establish a "no new JSP" rule (already in `.github/copilot-instructions.md`).
- Convert existing JSPs incrementally:
  - UI admin console components (`/libs/sling-cms/components/**`)
  - Thumbnails configuration UI (`/libs/sling/thumbnails/**`)
  - Reference module (optional; lower priority)

**How to do it (pattern)**
1. Create/convert template to `*.html` (HTL).
2. Move any complex logic into a Sling Model.
3. For component configuration passed via request attributes, read it in the model (HTL has limitations).
4. Remove JSP or keep *only* as temporary shim (prefer removing).

**Success criteria**
- Significantly reduced number of `.jsp` files in `ui`.
- No scriptlet logic remains.
- Consistent use of Sling Models for view logic.

---

### 2) CSS Framework Abstraction (Bulma + Jam Icons) (High Priority)

**Why**
- Direct Bulma/Jam usage in templates makes framework upgrades costly.

**What to do**
- Replace direct framework classes in templates with semantic classes:
  - `cms-button`, `cms-card`, `cms-grid`, `cms-icon cms-icon--*`, etc.
- Map semantic classes to Bulma/Jam using SCSS abstraction files.

**Implementation checklist**
- [ ] Ensure abstraction files exist and are considered the single integration point:
  - `_variables.scss` (mapped colors, spacing, radii)
  - `_icons.scss` (Jam mapping)
  - `_layout.scss` (grid mapping)
  - `_utilities.scss` (spacing/text helpers)
- [ ] Convert templates gradually (touch-when-needed) starting with high-usage areas:
  - Global nav
  - Action buttons
  - Editor field widgets

**Success criteria**
- HTL templates do not contain `jam jam-*` nor Bulma-specific layout class names.
- Framework swaps are mostly isolated to SCSS abstraction files.

---

### 3) DAM/Asset Code Consolidation into `thumbnails` Module (High Priority)

**Why**
- The repository states a consolidation plan: DAM-related logic should live in `thumbnails`.

**What to do**
- Audit for asset/DAM-specific services currently in `core` / `api`.
- Migrate implementations to `thumbnails` and keep only minimal cross-module contracts.

**Rules**
- New asset features → always implement in `thumbnails`.
- If `core` has asset concerns → plan/execute migration.

**Success criteria**
- Asset metadata extraction, rendition jobs, and transformations are owned by `thumbnails`.
- Fewer DAM-specific classes in `core`.

---

### 4) Finish Centralized JS Error Handling Rollout (Medium Priority)

**Why**
- Consistent timeout + user messaging reduces UI incident rate.

**Current state**
- Implemented utilities already documented in `ui/HIGH_PRIORITY_IMPROVEMENTS.md`.
- Remaining JS modules likely still have raw `fetch()`/async without centralized handling:
  - `cms.form.js`
  - `cms-module.js`
  - `cms.page.js`
  - `cms.job.js`

**What to do**
- Convert remaining fetch/async usage to `window.SlingCMS.errorHandler.fetchWithErrorHandling(...)`.

**Success criteria**
- No direct `fetch()` calls without standardized handling.

---

### 5) Add Repository Guardrails (Medium Priority)

**Why**
- Prevent regressions while migration is ongoing.

**Recommended guardrails**
- CI check: fail if new `*.jsp` files are introduced under `ui/src/main/resources/jcr_root/libs/**`.
- Optional CI check: warn/fail on direct Bulma/Jam class usage in templates.

---

### 6) Dependencies + Build Toolchain Upgrades (Needs a Dedicated Pass)

**Why**
- Keep security fixes current.
- Keep build reproducible.

**What to do**
- Run Maven `versions:*` plugins and decide upgrades based on:
  - OSGi compatibility
  - Sling platform compatibility
  - Security advisories

**Suggested next-step commands** (run locally)
- `mvn -DskipTests versions:display-dependency-updates`
- `mvn -DskipTests versions:display-plugin-updates`

**Success criteria**
- A tracked list of dependency upgrades, with CVE notes as needed.

## Suggested Execution Plan (Phased)

### Phase 1 (1–2 weeks)
- JSP → HTL conversions for highest-traffic admin UI components.
- Start icon/class abstraction for touched components.

### Phase 2 (2–4 weeks)
- Thumbnails module consolidation (migrate 1–2 core services per PR).
- Finish JS error-handling rollout.

### Phase 3 (ongoing)
- Add guardrails.
- Dependency upgrades + regression testing.

## Tracking

Use this checklist for progress:

- [ ] JSP → HTL conversion backlog created
- [ ] First 10 JSPs migrated successfully
- [ ] Semantic CSS abstraction used in new/modified templates
- [ ] Jam icon abstraction (`cms-icon`) used in new/modified templates
- [ ] DAM services migration plan created
- [ ] JS error handler applied to remaining modules
- [ ] CI guardrails enabled
- [ ] Dependency update report reviewed

---

*Last Updated: December 14, 2025*
