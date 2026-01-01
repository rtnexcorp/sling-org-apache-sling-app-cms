# New-age CMS – AI (Composable, Governed, Vendor-neutral)

This doc defines how AI should fit into a “new-age CMS” without becoming a black box or a vendor-locked marketing suite.

AI is treated as:
- a **pluggable provider**
- invoked by **content automation jobs** or explicit **author actions**
- governed by **policies, review gates, and audit logs**

---

## Goals

1. **Vendor neutral**: switch providers without changing templates/components.
2. **Author-first**: AI suggests; humans approve.
3. **Auditable**: every AI-assisted change is traceable.
4. **Safe by default**: protect PII, enforce permissions, and avoid leaking content.
5. **Composable**: small services that integrate with Sling Models, CAConfig, and jobs.

---

## Non-goals

- Not building a closed, proprietary “AI CMS suite”.
- Not auto-publishing AI content without review.

---

## AI feature set (what we enable)

### 1) Writing assistance
- Title suggestions
- Short/long summaries
- Tone rewriting (formal, concise)
- Meta description suggestions

### 2) Taxonomy and classification
- Tag suggestions mapped to existing taxonomy
- Category suggestions

### 3) Translation assistance (drafts)
- Create draft translations per locale
- Compare view + human review

### 4) Asset assistance
- Alt-text suggestions for images
- Caption suggestions

### 5) Support for knowledge workflows
- “Explain this page” summary for reviewers
- “What changed?” summaries for version diffs

---

## Architecture (recommended)

### A) Provider interface (OSGi service)
Create a small set of interfaces in `api` (or `core` if you want to iterate first), implemented in `core`.

**Example capability interfaces** (conceptual):
- `AiTextService` (summarize, rewrite, translate)
- `AiClassificationService` (tags/categories)
- `AiImageService` (alt-text/captions)

Key requirement:
- Implementations must be **swappable** via OSGi DS + configuration.

### B) Provider implementations
Keep multiple providers possible:
- `RulesBasedProvider` (non-AI fallback)
- `ExternalAiProviderX` (calls an external API)

**Rule**: the CMS should work even if AI is disabled.

### C) Invocation paths
AI should be invoked only via:
1. **Content Automation jobs** (scheduled or event-triggered)
2. **Explicit author actions** (button click “Suggest summary”)

### D) Storage model
All AI output should be stored as:
- draft fields (not directly overwriting published content)
- or versioned changes with explicit approval

Suggested:
- store drafts under `jcr:content/aiDrafts/*` (or a dedicated child node)
- store approved results into the normal field locations

---

## Governance & safety

### 1) Permissions
- Only allow AI actions to users/groups with explicit permission.
- Support per-site enablement under `/conf/{site}`.

### 2) Human review gates
Modes:
- **Suggest-only**: AI output shown in UI; author clicks “Apply”.
- **Draft mode**: write to draft fields; reviewer approves.

### 3) Audit logs
Record:
- request context (site/page)
- user who triggered
- provider used
- prompt template ID
- before/after (or diff)
- timestamps

### 4) Data handling / privacy
Guidelines:
- Never send unpublished restricted content unless explicitly allowed.
- Strip or mask sensitive fields (PII) using policy.
- Allow an “offline mode” where AI is fully disabled.

### 5) Explainability
UI should show:
- Which prompt template was used
- Which segments/context influenced output (if any)
- Confidence / warnings

---

## Configuration model (recommended)

Store AI configuration under CAConfig:

- `/conf/{site}/ai/`
  - `enabled` (boolean)
  - `provider` (name/id)
  - `policy` (PII masking rules, allowed paths)
  - `prompts/*` (prompt templates and parameters)

Prompt templates should be managed content-side so they are:
- reviewable
- versionable
- testable

---

## UI/UX recommendations

- Add “AI Assist” panels in page properties / asset properties.
- Provide per-field actions:
  - Suggest title
  - Suggest summary
  - Suggest tags
  - Suggest alt text
- Always display an **Apply** button, never auto-apply by default.

---

## Roadmap

### Phase 1 (safe + useful)
- Interface + provider abstraction
- Suggest title/summary for pages (suggest-only)
- Audit logging

### Phase 2
- Tag suggestions mapped to taxonomy
- Alt-text suggestions for images

### Phase 3
- Draft translations + compare view

### Phase 4
- “Assist in workflows” (review summaries, change summaries)

---

## Relationship to content automation

AI is a subset of content automation.

- Content automation defines triggers, jobs, policies, reports.
- AI defines providers, governance, prompts, and UI affordances.

See also: `new-age-cms-content-automation.md`.
