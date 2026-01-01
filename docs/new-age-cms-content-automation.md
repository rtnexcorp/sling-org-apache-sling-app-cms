# New-age CMS – Content Automation

Content automation is the set of features that reduces manual effort for authors and editors by automatically:
- creating and enriching content
- enforcing quality and policy
- routing approvals and publishing
- keeping content fresh and consistent

This is intentionally designed to be **composable** (small building blocks) and **transparent** (configurable in `/conf`, auditable, and debuggable).

---

## Goals

1. **Reduce author workload** (auto-generate metadata, summaries, translations, renditions).
2. **Improve quality** (broken links, missing alt text, policy adherence).
3. **Accelerate publishing** (scheduled publish/unpublish, approvals, bulk workflows).
4. **Maintain consistency** (schemas, templates, governed taxonomies).
5. **Keep it safe** (human review gates, audit trails, explainability).

---

## Core building blocks (recommended architecture)

### 1) Automation triggers
Where automation starts.

**Trigger types**
- Content events: create/update/delete of pages/assets
- Workflow actions: submit/approve/reject
- Schedules: run nightly (stale content checks)
- On-demand: author clicks “Run checks” / “Generate summary”

**Implementation hint**
- Use Sling event/job patterns (existing Sling/OSGi job infrastructure) rather than ad-hoc threads.

### 2) Automation jobs
A job is an idempotent unit of work.

Examples:
- Extract metadata from an uploaded asset
- Generate responsive renditions
- Validate a page against schema
- Detect broken links
- Generate SEO meta fields

**Job requirements**
- Idempotent (safe to retry)
- Observable (logs + job status)
- Configurable (enable/disable per site)

### 3) Policies (rules)
Policies decide what is allowed/required.

Examples:
- “All images must have alt text”
- “Pages cannot be published if broken links exist”
- “Only licensed assets may be used on public pages”

Policies should be stored in `/conf/{site}/policies/*`.

### 4) Human review gates
Automation should assist, not hide behavior.

Examples:
- “Generate summary” suggests text but requires author approval
- “Auto-translate” creates a draft variant requiring review

### 5) Audit & explainability
Every automation action should be traceable.

Store:
- who triggered it
- what changed
- before/after snapshot (or a diff)
- timestamps and job identifiers

---

## Feature set (what content automation includes)

### A) Content quality automation (high-value, low-risk)
1. **Broken link detection**
   - Scan pages for internal links that no longer resolve
   - Offer quick-fix suggestions when a replacement exists

2. **Accessibility automation**
   - Flag missing alt text
   - Warn when headings are out of order
   - Detect empty buttons/links

3. **SEO hygiene**
   - Title length checks
   - Missing meta description
   - Duplicate page titles under a site

4. **Content freshness**
   - Detect stale pages (not updated for N days)
   - Generate “review needed” tasks

**Output UX**
- A “Content Quality” report page under Admin Tools
- Per-page panel indicating issues and suggestions

### B) Metadata automation (assets + pages)
1. **Assets**
   - Extract EXIF/IPTC/XMP
   - Normalize fields to `jcr:content/metadata/*`

2. **Pages**
   - Auto-tagging suggestions based on taxonomy and page text
   - Auto-summary suggestions for listings

### C) Content creation assistance (optional, pluggable)
Keep vendor lock-in out by making providers pluggable.

1. **Generate summary** from page content
2. **Suggest title** for a page
3. **Suggest tags** based on taxonomy
4. **Rewrite tone** (formal/informal)

Provider pattern:
- Define `ContentAssistProvider` OSGi service interface
- Implementation can be: local rules, external AI, or none

### D) Automated translations (lightweight)
1. Produce draft translation variants
2. Track translation status
3. Provide compare view

### E) Workflow automation
1. **Approval flows**
   - Define per-site approval steps
   - Auto-assign reviewers by group

2. **Scheduled publish/unpublish**
   - Schedule future publish
   - Auto-unpublish expired campaigns

3. **Bulk operations**
   - Bulk publish/unpublish
   - Bulk metadata updates

---

## Configuration model (recommended)

Store automation configuration under site config:

- `/conf/{site}/automation/`
  - `jobs/*` (enabled, schedule, parameters)
  - `policies/*` (requirements, severity)
  - `providers/*` (which assist provider to use)

Example concept:
- `policies/alt-text-required`
- `jobs/link-checker`
- `jobs/stale-content-detector`

---

## Proposed roadmap (incremental)

### Phase 1: Quality checks + reports (no AI)
- Broken link detection
- Alt-text checks
- Stale content detector
- Admin Tools UI: run checks + export report

### Phase 2: Metadata automation
- Asset metadata extraction pipeline
- Optional tag suggestions (rule-based)

### Phase 3: Workflow gates
- Prevent publish if critical policy failures
- Scheduled publish/unpublish

### Phase 4: Assist providers (optional)
- Pluggable summary/title generators
- Translation draft generation

---

## Success criteria
- Authors spend less time on repetitive edits
- Fewer broken pages/assets in production
- Clear, observable automation actions (no surprises)
- Site owners can enable/disable per job and per policy

