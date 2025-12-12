# Apache Tika upgrade plan (Sling CMS)

This repo currently pins Apache Tika to **1.28.5** and cannot be moved to Tika 2.x without upgrading other OSGi bundles that **import `org.apache.tika.*` with version ranges `< 2.0`** (notably Jackrabbit Oak Lucene and Composum Nodes).

This document describes a **safe, staged plan** to reach the **latest Tika 2.x** (or newer) while minimizing production risk.

---

## 1) Goals

- Upgrade Apache Tika to the latest stable release (Tika 2.x+).
- Keep the Sling Feature model resolvable (no missing/unsatisfied OSGi imports).
- Maintain thumbnail generation and any content extraction behavior.
- Avoid security regressions (ensure CVEs addressed).

Non-goals:
- Forcing Tika 2.x into an OSGi runtime where upstream bundles hard-require Tika 1.x.

---

## 2) Current state (what to verify first)

### 2.1 Where Tika is referenced

Verify these places (paths may evolve):

- Root `pom.xml`: property for Tika version.
- `feature/src/main/features/base.json`: includes `org.apache.tika:tika-core` and possibly `tika-bundle`.
- `thumbnails/pom.xml`: uses `${tika-version}` (or similarly named property).
- `core/bnd.bnd` and `thumbnails/bnd.bnd`: may declare `Import-Package: org.apache.tika.*;version=...`.
- Any code using Tika APIs (e.g. `TikaFallbackProvider`).

### 2.2 Known hard blockers

You already hit the key issue: some bundles require Tika 1.x via OSGi import ranges such as:

- `org.apache.jackrabbit:oak-lucene` (often imports `org.apache.tika.*;version=[1.28,2)`).
- `com.composum.nodes:composum-nodes-commons` (commonly `version=[1.0,2)`).
- `org.apache.sling:org.apache.sling.jcr.webdav` (commonly `version=[1.0,2)`).

**Until those dependencies are upgraded to versions that import Tika 2.x packages (e.g. `[2,3)`), the feature model will not resolve with Tika 2.x.**

---

## 3) Strategy overview (two viable paths)

### Path A (recommended): Upgrade the blockers first, then upgrade Tika

1. Identify newer versions of Oak / Composum / Sling JCR WebDAV that are compatible with your Sling Starter baseline and import Tika 2.x.
2. Upgrade those bundles.
3. Switch feature model and Maven properties to Tika 2.x.
4. Fix any code-level Tika API changes.

Pros:
- Single Tika version in the runtime.
- Clean OSGi wiring and simplest operational model.

Cons:
- Requires upstream dependency upgrades which may cascade.

### Path B (fallback): Keep runtime on Tika 1.x, run Tika 2.x out-of-process

If you need Tika 2.x extraction features now, but can’t upgrade Oak/Composum yet:

- Keep OSGi runtime on Tika 1.28.5.
- Add an external Tika Server container (or other extraction microservice) at Tika 2.x.
- Call it from Sling CMS over HTTP for extraction.

Pros:
- No OSGi resolution conflicts.
- You can use latest Tika features immediately.

Cons:
- Operational complexity (extra service, network, security, latency).

---

## 4) Detailed plan (Path A)

### Phase 0 — Pre-flight

- Create a working branch (e.g. `upgrade/tika-2`).
- Ensure builds are green on current state.
- Capture integration test baseline (thumbnail generation, content extraction, search indexing).

Deliverables:
- Baseline build logs.
- Baseline: a list of current bundles and their `Import-Package` requirements (use Sling Feature analyzer output).

### Phase 1 — Inventory OSGi import constraints

1. Run feature analysis (the build already produces this).
2. Collect the exact bundles that constrain Tika to `<2.0`.

Deliverables:
- A table:
  - bundle
  - current version
  - imported package range for `org.apache.tika.*`
  - candidate upgraded version

### Phase 2 — Upgrade the blocking bundles

This is the critical phase.

1. Upgrade **Oak** (esp. `oak-lucene`) to a version that supports Tika 2.x in OSGi.
   - Risk: Oak upgrades can impact repository behavior, indexing, lucene compatibility.
2. Upgrade **Composum Nodes** to a version that supports Tika 2.x.
   - Risk: UI/components behavior regressions.
3. Upgrade **Sling JCR WebDAV** if it still imports Tika `<2`.

Validation:
- Feature model resolves.
- Repository starts.
- Basic authoring and search/indexing works.

Risks / issues to anticipate:
- Transitive dependency conflicts (Guava, SLF4J, Jackson, PDFBox, POI) if newer Oak/Composum pull newer libs.
- API/behavior changes across Oak versions.

Rollback:
- Revert bundle upgrades; keep Tika pinned to 1.28.5.

### Phase 3 — Upgrade Tika artifacts (Maven + Feature model)

1. Update root property (e.g. `tika.version` / `tika-version`) to latest Tika.
2. Update feature files to include the matching Tika bundles.
   - Prefer a single approach: either use the OSGi-ready `tika-bundle` or explicit module list, but keep the runtime consistent.

Notes:
- Tika 2.x splits/changes some artifacts and optional parsers.
- Ensure you include whatever parsers you relied on previously (PDF, Office, etc.).

### Phase 4 — Fix code for Tika 2.x API changes

Tika 2.x contains breaking changes. Expect modifications in code similar to:

- `WriteOutContentHandler` / write-limit exception handling.
- Parser/context behavior.
- Class/package availability depending on chosen Tika artifacts.

Action items:
- Update code (e.g. `TikaFallbackProvider`) and unit tests.
- Ensure OSGi imports in `bnd.bnd` match the package versions exported by the selected Tika bundles.

### Phase 5 — OSGi metadata / bnd alignment

1. Align `Import-Package` constraints:
   - Avoid overly-broad ranges like `[1.28,3)` if your runtime is now firmly 2.x; prefer `[2,3)` to prevent wiring unexpected 1.x.
2. Re-run baseline plugin and feature analyzer.

### Phase 6 — Runtime validation

- Start author/publish and validate:
  - Thumbnail generation for PDFs, DOCX, PPT, plain text.
  - Any indexing/search features relying on text extraction (especially Lucene/Oak).
  - Performance: extraction time, memory.

Operational issues to anticipate:
- Parser CVEs / vulnerable transitive libs.
- Increased memory usage from parser set.
- Font handling / headless rendering issues (if any parser triggers AWT usage).

### Phase 7 — Security and licensing

- Re-run dependency scanning (GitHub Dependabot/CodeQL + Maven enforcer if used).
- Confirm Apache RAT exclusions still valid.
- Confirm NOTICE/license requirements for any new parser libs.

---

## 5) Detailed plan (Path B: external Tika)

If Path A can’t be completed soon:

1. Keep Tika in OSGi at 1.28.5 (to satisfy Oak/Composum).
2. Run Tika 2.x server separately:
   - Containerized service with locked-down network policy.
   - Configure timeouts, max upload size, content-type allowlist.
3. Add a Sling service that calls the external Tika endpoint for extraction.
4. Gate it behind a feature flag so you can roll back quickly.

Risks:
- SSRF/file exfiltration if endpoint is exposed.
- Larger operational surface.

---

## 6) “Gotchas” checklist

- **OSGi import ranges**: the single most common failure mode.
- **Oak Lucene compatibility**: extraction often influences indexing; test thoroughly.
- **Parser coverage**: Tika 2.x may require explicitly adding parser modules you previously got via `tika-bundle`.
- **Security**: Tika pulls complex parsers; keep versions patched.
- **Memory/CPU**: extraction can spike; consider concurrency limits.
- **Headless/AWT**: some conversions might require headless mode (set `-Djava.awt.headless=true` where appropriate).

---

## 7) Acceptance criteria

- `mvn -B clean install` passes for all modules.
- Feature model analysis passes (no unsatisfied `Import-Package`).
- Thumbnails generated for representative samples.
- No known critical CVEs introduced (or have documented mitigations).
- Documented rollback path.
