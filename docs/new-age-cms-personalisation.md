# New-age CMS – Personalisation

## Goal
Deliver **simple, transparent personalisation** (segments + variants + experiments) without a heavy “marketing suite”.

## Principles
- **Configuration in content** (`/conf`), not hidden tooling.
- **Auditable rules** and deterministic evaluation.
- **Pluggable**: segments and resolvers are OSGi services.

## Feature candidates
### 1. Segments
**What**: Define segments using simple rules.

**Rule inputs**
- URL/path
- query params
- cookies
- device hints (UA parsing)
- logged-in user/groups
- optional geo (external provider)

**Implementation sketch**
- `SegmentService` evaluates request → segment IDs.
- Segment definitions stored under `/conf/.../personalisation/segments/*`.

### 2. Targeted variants per component
**What**: Component supports `default` + per-segment overrides.

**Implementation sketch**
- Variant selection resolves child resource `/variants/<segmentId>`.
- Falls back to default.

### 3. Experiments (A/B)
**What**: Split traffic across variants with sticky bucketing.

**Implementation sketch**
- Bucket assignment stored in cookie.
- Capture events (impression/click) using lightweight endpoint.

### 4. Content insights + segment dashboards
**What**: Show segment performance, recent events, and variant outcomes.

## Suggested incremental roadmap
1. Segment definitions + evaluation API
2. Variant rendering for components
3. A/B experiments + event capture
4. Simple dashboards
