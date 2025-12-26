# SPDX-License-Identifier: Apache-2.0

# Frontend and UI Module Analysis Report

**Date:** 2025-12-26
**Analyzed Modules:** `frontend/` and `ui/`
**Version:** 1.1.9-SNAPSHOT

## Executive Summary

This document provides a comprehensive analysis of the Apache Sling CMS frontend and UI modules, identifying architectural patterns, migration opportunities, and recommendations for improvement. The analysis reveals a well-structured frontend build system but significant technical debt in the UI module with 72.9% of templates still using legacy JSP instead of modern HTL.

**Key Findings:**
- ✅ Frontend build system is modern and well-architected (Vite + ES6 modules)
- ⚠️ 145 of 199 template files (72.9%) still use JSP instead of HTL
- ⚠️ Inconsistent template approach with some components having both JSP and HTL versions
- ✅ Good component organization and categorization
- ⚠️ Editor field components need systematic modernization (58 JSP files)

---

## Part 1: Frontend Module Analysis

### 1.1 Architecture Overview

**Module Location:** `/frontend`
**Purpose:** Build and package frontend assets (JavaScript, CSS, fonts, images)
**Build Tool:** Vite 6.4.1 (modern ES modules)
**Package Manager:** npm/bun (dual support)
**Output:** OSGi bundle with compiled assets served at `/static/sling-cms/`

### 1.2 Build Configuration

#### Maven Build Profiles

| Profile | Purpose | Command |
|---------|---------|---------|
| Default | Production build (minified) | `mvn clean install -P autoInstallBundle` |
| `dev` | Development build (unminified) | `mvn clean install -P autoInstallBundle,dev` |
| `watch` | Watch mode for live reload | `mvn clean install -P autoInstallBundle,watch` |
| `bun` | Production with Bun | `mvn clean install -P autoInstallBundle,bun` |
| `bun-dev` | Development with Bun | `mvn clean install -P autoInstallBundle,bun-dev` |

**Hot Deploy Command:**
```bash
mvn clean install -P autoInstallBundle -pl frontend -DskipTests
```

#### Vite Configuration Highlights

**Entry Points (3 bundles):**
1. `cms.bundle.min.js` - Main CMS application
2. `editor.bundle.min.js` - Page editor
3. `starter.bundle.min.js` - Getting started

**Output Format:** ES modules (requires `<script type="module">`)

**Code Splitting Strategy:**
- Manual chunks for `@tiptap/*` → `cms.tiptap-[hash].min.js`
- Manual chunks for `bulma` → `bulma.min.css`
- Automatic splitting for `xlsx`, `jszip`, `mammoth`

**Custom Vite Plugins:**
1. `clean-old-chunks` - Removes old hash-based files
2. `copy-open-sans-fonts` - Font asset copying
3. `copy-jam-icons-fonts` - Icon font copying
4. `copy-image-assets` - Image asset copying
5. `add-license-headers` - Apache License header injection

### 1.3 JavaScript Architecture

**Total Files:** 32 JavaScript modules

**Core Infrastructure:**
- `cms-module.js` - Namespace and core UI handlers
- `logger.js` - Debug logging with localStorage control
- `error-handler.js` - Centralized fetch error handling
- `sanitize.js` - HTML sanitization via DOMPurify
- `namespace.js` - Global `window.Sling.CMS` initialization

**Feature Modules (Rava-based):**
- `cms.draggable.js` - Drag and drop
- `cms.fields.js` - Form field handling (14KB, largest module)
- `cms.form.js` - AJAX form submission
- `cms.modal.js` - Modal dialogs
- `cms.nav.js` - Navigation
- `cms.toggle.js` - Toggle/collapse UI

**Specialized Components:**
- `cms.assetbrowser.js` - DAM asset browsing
- `cms.docpreview.js` - Document preview (Word, PDF, Excel)
- `cms.imagepreview.js` - Image zoom/preview
- `cms.tiptap.js` - Rich text editor integration
- `cms.pathbrowser.js` - Path selection
- `cms.search.js` - Content search

**Key Dependencies:**
- **TipTap** (v3.13.0) - Rich text editing
- **Bulma CSS** (v1.0.4) - CSS framework
- **Jam Icons** (v2.0.0) - Icon library
- **DOMPurify** (v3.3.0) - XSS prevention
- **Rava** - Declarative event binding
- **Mammoth** (v1.11.0) - Word document conversion
- **XLSX** (v0.18.5) - Excel spreadsheet handling
- **JSZip** (v3.10.1) - ZIP file handling

**Architectural Pattern:**
```javascript
// Declarative event binding with Rava
rava.bind(".cms-component", {
  events: {
    click: (event) => { /* handler */ }
  },
  ready: (element) => { /* initialization */ }
});
```

### 1.4 SCSS Architecture

**Total Files:** 17 SCSS files

**Main Entry:**
- `cms.scss` (8KB) - Imports all partials using `@use`

**Framework Configuration:**
- `_bulma-config.scss` - Bulma 1.0 customization
  - Brand colors: Magenta (#9E2165), Indigo, Violet, Crimson, Orange
  - Font: Open Sans
- `_variables.scss` - Custom SCSS variables and utilities

**Component Stylesheets (BEM naming):**
- `_assets.scss` (6.5KB) - Asset browser and grid
- `_contentfilter.scss` (1.5KB) - Content filtering controls
- `_search.scss` (5KB) - Search interface
- `_toggle.scss` (2.3KB) - Toggle/collapse components
- `_tiptap.scss` (2.7KB) - Rich text editor
- `_movecopy.scss` - Move/copy operations
- `_pathbrowser.scss` - Path browser
- `_preview.scss` - Content preview
- `_fieldsets.scss` - Form fieldsets
- `_grid.scss` - Grid utilities
- `_fonts.scss` - Font declarations
- `_overrides.scss` - Bulma overrides

**BEM Naming Example:**
```scss
.content-filter {
  display: flex;

  &__controls {
    display: flex;
    gap: 0.5rem;
  }

  &__label {
    font-size: 0.875rem;
    color: #666;
  }
}
```

**Responsive Breakpoints:**
- Mobile: < 768px
- Tablet: ≥ 769px
- Uses `@media screen and (min-width: $tablet)`

### 1.5 Asset Organization

**Fonts:**
- Open Sans (4 weights: Regular, Light, SemiBold, Bold)
- Jam Icons fonts (from node_modules)

**Images:**
- Favicons (multiple sizes)
- PWA manifest (site.webmanifest)
- Logos: sling-logo.png, sling-logo.svg, asf-logo.svg
- Branding: gradient.jpg

### 1.6 Strengths ✅

1. **Modern Build System**
   - Vite 6.x with fast HMR
   - ES6 modules throughout
   - Efficient code splitting
   - Production optimizations (terser, drop console)

2. **Clean Architecture**
   - Modular JavaScript with clear separation of concerns
   - Declarative event binding (Rava framework)
   - Centralized utilities (logger, error handler, sanitization)
   - Component-based SCSS organization

3. **Security Best Practices**
   - DOMPurify for XSS prevention
   - ESLint for code quality
   - Apache License headers on all generated files

4. **Framework Abstraction**
   - Semantic CSS classes (`cms-*` prefix)
   - BEM naming convention
   - Bulma abstraction in `_bulma-config.scss`
   - Easy framework migration path

5. **Developer Experience**
   - Watch mode for live development
   - Bun support for faster builds
   - Bundle analyzer (`npm run analyze`)
   - Multiple build profiles

### 1.7 Issues & Recommendations ⚠️

#### Issue 1: License Header Typo
**File:** `cms.form.js` (line 2)
**Problem:** Extra 'w' character: `w * Licensed...`
**Fix:** Remove 'w' character

**Recommendation:**
```diff
- w * Licensed to the Apache Software Foundation (ASF)
+ * Licensed to the Apache Software Foundation (ASF)
```

#### Issue 2: File Permissions
**Files Affected:**
- `_movecopy.scss`, `_pathbrowser.scss`, `_preview.scss`
- `cms.i18n.js`, `cms.movecopy.js`, `cms.pathbrowser.js`, `cms.preview.js`

**Problem:** Restrictive permissions (600/700 instead of 644)

**Recommendation:**
```bash
chmod 644 frontend/src/main/frontend/scss/_movecopy.scss
chmod 644 frontend/src/main/frontend/scss/_pathbrowser.scss
chmod 644 frontend/src/main/frontend/scss/_preview.scss
chmod 644 frontend/src/main/frontend/js/cms.i18n.js
chmod 644 frontend/src/main/frontend/js/cms.movecopy.js
chmod 644 frontend/src/main/frontend/js/cms.pathbrowser.js
chmod 644 frontend/src/main/frontend/js/cms.preview.js
```

#### Issue 3: Large Field Module
**File:** `cms.fields.js` (14KB)
**Problem:** Largest JavaScript module, handles many different field types

**Recommendation:**
- Consider splitting into smaller modules per field type
- Create field registry pattern for extensibility
- Example structure:
  ```
  js/fields/
  ├── index.js (registry)
  ├── text-field.js
  ├── select-field.js
  ├── date-field.js
  └── autocomplete-field.js
  ```

#### Issue 4: Editor Module Size
**File:** `editor.js` (14KB)
**Problem:** Large editor module

**Recommendation:**
- Split editor functionality into smaller modules
- Use dynamic imports for heavy editor features
- Lazy load editor components when needed

#### Issue 5: Documentation Gaps
**Missing Documentation:**
- No README in `frontend/` explaining build system
- No contribution guide for frontend developers
- No component development guide

**Recommendation:**
Create `frontend/README.md` with:
- Build system overview
- Development workflow
- Component creation guide
- Testing instructions

### 1.8 Performance Optimization Opportunities

1. **Dynamic Imports**
   - Lazy load TipTap editor (only when rich text field used)
   - Lazy load document preview (only when previewing)
   - Lazy load XLSX/Mammoth (only for office docs)

2. **CSS Optimization**
   - Consider CSS purging for unused Bulma components
   - Tree-shake icon fonts (only include used icons)

3. **Caching Strategy**
   - Implement service worker for offline support
   - Long-term caching for font/image assets

4. **Bundle Analysis**
   ```bash
   cd frontend/src/main/frontend
   npm run analyze
   ```

---

## Part 2: UI Module Analysis

### 2.1 Architecture Overview

**Module Location:** `/ui`
**Purpose:** JCR content, components, templates, and configurations
**Content Root:** `src/main/resources/jcr_root/`
**Served At:** `/libs/sling-cms/` (with `/apps` overlay support)

### 2.2 Template Technology Statistics

**Total Template Files:** 193 *(Updated 2025-12-26 after duplicate resolution)*

| Technology | Count | Percentage |
|------------|-------|------------|
| JSP | 139 | 72.0% |
| HTL | 54 | 28.0% |

**Progress:** 6 duplicate JSP files removed (2025-12-26)

**By Component Category:**

| Category | JSP | HTL | Total | JSP % |
|----------|-----|-----|-------|-------|
| editor | 58 | 6 | 64 | 90.6% |
| cms | 25 | 38 | 63 | 39.7% |
| caconfig | 20 | 0 | 20 | 100% |
| pages | 15 | 1 | 16 | 93.7% |
| publication | 7 | 0 | 7 | 100% |
| jobs | 5 | 0 | 5 | 100% |
| general | 3 | 1 | 4 | 75% |
| insights | 2 | 0 | 2 | 100% |
| dam | 0 | 1 | 1 | 0% |
| thumbnails | 10 | 6 | 16 | 62.5% |

### 2.3 Component Organization

**Top-Level Categories (9):**

1. **caconfig** - Context-Aware Configuration UI (20 JSP files)
2. **cms** - Core CMS components (63 files, 43 sub-components)
3. **dam** - Digital Asset Management (1 file)
4. **editor** - Field editors and configuration (64 files)
5. **general** - General-purpose components (4 files)
6. **insights** - Reporting/analytics (2 files)
7. **jobs** - Background job management (5 files)
8. **pages** - Page-level layouts (16 files)
9. **publication** - Content publishing workflow (7 files)

**CMS Component Subcategories:**
- **Asset Management:** assetcard, assetfilterbar, assetgrid, assetmetadataeditor, imagepreview
- **Content Management:** contentactions, contentbreadcrumb, contentfilter, contentgrid, contentsearch, contenttable
- **Editors:** fragmenteditor, pageeditor, pageproperties
- **Navigation:** globalsearch, pageeditbar, staticnav
- **Page Elements:** blank, columns (8 types), pagewrapper, schema
- **Utilities:** actions, getform, includeconfig, optimizefile, querydebug, references, versionmanager

### 2.4 Migration Status

#### Files with Both JSP and HTL Versions

**✅ RESOLVED (2025-12-26):** All duplicate template files have been eliminated.

**Previously Duplicated (now resolved):**
1. ~~**editor/scripts/finalize**~~ - JSP removed, HTL kept
2. ~~**editor/scripts/init**~~ - JSP removed, HTL kept
3. ~~**general/textelement**~~ - JSP removed, HTL kept
4. ~~**thumbnails/transformers/crop**~~ - JSP removed, HTL kept
5. ~~**thumbnails/transformers/resize**~~ - JSP removed, HTL kept
6. ~~**thumbnails/transformers/rotate**~~ - JSP removed, HTL kept

**Resolution Details:** See [duplicate-template-resolution.md](duplicate-template-resolution.md)

**Status:** ✅ Zero duplicate templates remaining

#### High-Priority Migration Targets

**100% JSP Categories (Must Migrate):**
1. **caconfig** (20 files) - Context-aware configuration UI
2. **publication** (7 files) - Publishing workflow
3. **jobs** (5 files) - Job management
4. **insights** (2 files) - Analytics

**90%+ JSP Categories (High Priority):**
1. **pages** (15 JSP, 1 HTL) - Core layout templates
2. **editor** (58 JSP, 6 HTL) - Field editors

**Total Migration Target:** 107 JSP files (73.8% of all JSP files)

### 2.5 Editor Field Components Deep Dive

**Location:** `/components/editor/fields/`
**Total Files:** 64 (58 JSP, 6 HTL)

**Field Types (all JSP):**
- Text inputs: text, textarea, texteditor, textfield
- Rich content: richtext, markdown, code
- Selection: select, radio, checkbox, multiselect, toggle, tags
- Files: filelist, image
- Paths: pathfield, path, pagepicker, sitepicker
- References: reference, referencesearch
- Dates: datepicker, datetime, timestamp
- Numbers: number
- Taxonomy: taxonomyselect, taxonomymultiselect
- Special: browsefield, autocomplete, publication, suffixfield, repeating

**Scripts (mixed):**
- init (JSP + HTL) ⚠️ Duplicate
- finalize (JSP + HTL) ⚠️ Duplicate
- options (JSP)
- optionsgenerated (JSP)

**Toolbar Components (all JSP):**
- collapsetoggle, contentfragment, contentreference, filedownload, includeedit, pageproperties, pageversiondetail, versionnavigation

### 2.6 Content Structure

**Content Organization:** `/libs/sling-cms/content/`

**Key Content Areas:**
- **admin** - Administrative UI
- **auth** - Authentication pages
- **config** - System configuration
- **dam** - DAM structure
- **delivery** - Content delivery
- **editor** - Editor configurations
- **file** - File management
- **fragment** - Content fragments
- **i18n** - Internationalization (186KB i18n.json)
- **jobs** - Job configurations
- **page** - Page templates/workflows
- **publication** - Publishing workflows
- **schema** - Schema definitions
- **shared** - Shared resources
- **site** - Site configurations
- **taxonomy** - Taxonomy definitions
- **transformations** - Image transformations

**Configuration Files:**
- Rewriter: `/config/rewriter/reference-mapping-rewriter.json`
- Taxonomy: `/etc/taxonomy/`
- i18n: `/etc/i18n/` (sling-cms.json, my-website.json)

### 2.7 Global Resources

**global.jsp** (1,499 bytes)
**Purpose:** Global JSP include used by all legacy templates
**Contents:**
- JSP directives (page contentType, session)
- Tag library declarations (JSTL, Sling)
- Object adaptations (ValueMap, PageContext)
- Branding resource loading

**Issue:** Creates tight coupling for all JSP templates. HTL migration requires refactoring this pattern.

### 2.8 Strengths ✅

1. **Clear Categorization**
   - Logical separation by functional area
   - Content separated from components
   - Configuration centralized

2. **Modern Components (CMS Category)**
   - 60% HTL adoption (38 of 63 files)
   - Good examples of modern component development

3. **Systematic Organization**
   - i18n resources well-organized
   - Taxonomy definitions centralized
   - Configuration files in dedicated locations

4. **Component Definition**
   - `.content.xml` files for component metadata
   - Proper `sling:Component` types

### 2.9 Critical Issues ⚠️

#### Issue 1: Duplicate Template Implementations

**Affected Components:**
1. `editor/scripts/finalize` (JSP + HTL)
2. `editor/scripts/init` (JSP + HTL)
3. `general/textelement` (JSP + HTL)
4. Thumbnails: transformation, crop, rotate, resize (JSP + HTL)

**Problem:**
- Unclear which version Sling uses
- Maintenance overhead (must update both)
- Potential behavior inconsistencies

**Recommendation:**
1. Determine active version for each component
2. Delete inactive version
3. If both are used, consolidate to single HTL version
4. Document decision in migration plan

**Investigation Command:**
```bash
# Check component usage in content
find ui/src/main/resources/jcr_root -name "*.json" -o -name "*.xml" | \
  xargs grep -l "editor/scripts/finalize\|editor/scripts/init\|textelement"
```

#### Issue 2: Large Legacy JSP Codebase

**Statistics:**
- 145 JSP files (72.9%)
- 107 files in 100% JSP categories
- 58 JSP files in editor alone

**Impact:**
- Maintenance burden
- Security concerns (JSP scriptlets)
- Inconsistent development patterns
- Difficult debugging
- Modern tooling incompatible

**Recommendation:**
See "Migration Strategy" section below.

#### Issue 3: Editor Field Component Organization

**Problem:**
- 58+ files in single directory
- No logical grouping
- Hard to find specific field type
- No naming convention consistency

**Recommendation:**
Reorganize editor fields:
```
components/editor/fields/
├── text/
│   ├── text/
│   ├── textarea/
│   ├── texteditor/
│   └── richtext/
├── selection/
│   ├── select/
│   ├── radio/
│   ├── checkbox/
│   └── multiselect/
├── reference/
│   ├── pathfield/
│   ├── pagepicker/
│   ├── sitepicker/
│   └── reference/
├── taxonomy/
│   ├── taxonomyselect/
│   └── taxonomymultiselect/
├── datetime/
│   ├── datepicker/
│   ├── datetime/
│   └── timestamp/
├── file/
│   ├── filelist/
│   └── image/
├── scripts/
│   ├── init/
│   ├── finalize/
│   └── options/
└── toolbar/
    ├── collapsetoggle/
    ├── contentfragment/
    └── ...
```

#### Issue 4: Asset Management Component Scatter

**Problem:**
Asset components split across multiple locations:
- `cms/assetcard`, `cms/assetfilterbar`, `cms/assetgrid` (ui module)
- `cms/assetmetadataeditor` (ui module)
- `dam/cache/statistics` (ui module)
- Thumbnail services (thumbnails module)

**Recommendation:**
Consolidate all DAM/asset UI components:
```
components/dam/
├── browser/        (from cms/assetgrid)
├── card/           (from cms/assetcard)
├── filterbar/      (from cms/assetfilterbar)
├── metadata/       (from cms/assetmetadataeditor)
├── preview/        (from cms/imagepreview)
├── cache/          (existing)
└── transformations/ (from thumbnails module)
```

#### Issue 5: Global JSP Dependency

**File:** `global.jsp`
**Problem:** All JSP templates depend on this centralized include

**Impact:**
- Makes HTL migration harder
- Creates implicit dependencies
- Hides required imports
- Not compatible with HTL approach

**Recommendation:**
1. Document all dependencies in `global.jsp`
2. Create HTL equivalent using `data-sly-use` and `data-sly-include`
3. Phase out `global.jsp` as JSP files migrate

#### Issue 6: Incomplete Component Metadata

**Problem:** Only 16 components have `.content.xml` definitions

**Impact:**
- Unclear component registration
- Missing metadata for tooling
- Inconsistent component discovery

**Recommendation:**
1. Audit all components for `.content.xml`
2. Add missing component definitions
3. Standardize component metadata:
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
             xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
             jcr:primaryType="sling:Component"
             jcr:title="Component Title"
             componentGroup="Sling CMS"
             jcr:description="Component description"/>
   ```

---

## Part 3: Migration Strategy

### 3.1 Migration Phases

#### Phase 1: Foundation (Weeks 1-2)
**Goal:** Resolve duplicate templates and establish patterns

**Tasks:**
1. ✅ Investigate duplicate JSP/HTL components
2. ✅ Choose canonical version for each
3. ✅ Delete unused duplicates
4. ✅ Document HTL migration patterns
5. ✅ Create HTL component template

**Affected Files:**
- `editor/scripts/finalize` (JSP + HTL)
- `editor/scripts/init` (JSP + HTL)
- `general/textelement` (JSP + HTL)
- Thumbnails: crop, rotate, resize, transformation

**Output:**
- Migration decision document
- HTL component template
- Updated `/docs/jsp-to-htl-migration.md`

#### Phase 2: High-Impact Categories (Weeks 3-6)
**Goal:** Migrate 100% JSP categories

**Priority 1 - caconfig (20 files):**
- All Context-Aware Configuration UI
- High visibility to administrators
- Standardize configuration patterns

**Priority 2 - pages (15 files):**
- Core layout templates
- High usage across all page types
- Foundation for all page rendering

**Priority 3 - publication (7 files):**
- Publishing workflow UI
- Critical for content management

**Expected Outcome:**
- 42 JSP files → HTL
- 28.9% reduction in JSP codebase
- Modernize critical infrastructure

#### Phase 3: Editor Fields (Weeks 7-12)
**Goal:** Systematically migrate editor field components

**Approach:**
1. Group fields by type (text, selection, reference, etc.)
2. Migrate one group at a time
3. Create reusable HTL patterns
4. Test extensively (affects all content editing)

**Priority Order:**
1. Text fields (8 components) - Most common
2. Selection fields (6 components) - High usage
3. Reference fields (6 components) - Complex
4. Taxonomy fields (2 components)
5. Date/time fields (3 components)
6. File fields (2 components)
7. Special fields (remaining)

**Expected Outcome:**
- 37+ field editors → HTL
- 40% reduction in JSP codebase
- Consistent field implementation patterns

#### Phase 4: Remaining Components (Weeks 13-16)
**Goal:** Complete migration

**Targets:**
- jobs (5 files)
- insights (2 files)
- general (2 remaining files)
- cms (25 remaining files)
- thumbnails (10 remaining files)

**Expected Outcome:**
- 100% HTL codebase
- Delete `global.jsp`
- Complete migration documentation

### 3.2 Migration Guidelines

#### HTL Component Template

**File Structure:**
```
component-name/
├── .content.xml           # Component definition
├── component-name.html    # HTL template
└── README.md             # Component documentation (optional)
```

**HTL Template Pattern:**
```html
<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->
<sly data-sly-use.model="org.apache.sling.cms.core.models.MyModel"
     data-sly-use.i18n="org.apache.sling.cms.i18n.I18NDictionary">

  <div class="my-component">
    <h2>${model.title @ context='html'}</h2>

    <sly data-sly-list.item="${model.items}">
      <div class="my-component__item">
        ${item.name @ context='text'}
      </div>
    </sly>
  </div>

</sly>
```

**Component Definition (.content.xml):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          jcr:primaryType="sling:Component"
          jcr:title="My Component"
          jcr:description="Component description"
          componentGroup="Sling CMS"/>
```

#### JSP to HTL Conversion Checklist

- [ ] Read existing JSP to understand logic
- [ ] Identify Java logic → Create Sling Model if needed
- [ ] Convert scriptlets to HTL expressions
- [ ] Replace `<c:forEach>` with `data-sly-list`
- [ ] Replace `<c:if>` with `data-sly-test`
- [ ] Replace `<c:choose>` with multiple `data-sly-test`
- [ ] Use context-aware XSS protection (`@ context`)
- [ ] Test with original content
- [ ] Add Apache License header
- [ ] Delete JSP file after verification
- [ ] Update component documentation

#### Common JSP → HTL Patterns

**Iteration:**
```jsp
<!-- JSP -->
<c:forEach var="item" items="${items}">
  <div>${item.name}</div>
</c:forEach>

<!-- HTL -->
<sly data-sly-list.item="${items}">
  <div>${item.name}</div>
</sly>
```

**Conditionals:**
```jsp
<!-- JSP -->
<c:if test="${not empty title}">
  <h1>${title}</h1>
</c:if>

<!-- HTL -->
<h1 data-sly-test="${title}">${title}</h1>
```

**XSS Protection:**
```jsp
<!-- JSP -->
${sling:encode(title, 'HTML')}

<!-- HTL -->
${title @ context='html'}
```

**Resource Inclusion:**
```jsp
<!-- JSP -->
<sling:include path="component" resourceType="sling-cms/components/cms/mycomponent"/>

<!-- HTL -->
<sly data-sly-resource="${'component' @ resourceType='sling-cms/components/cms/mycomponent'}"/>
```

### 3.3 Testing Strategy

**For Each Migrated Component:**

1. **Unit Tests**
   - Test Sling Model (if created)
   - Test HTL rendering (Sling Mock)

2. **Integration Tests**
   - Create test content
   - Verify rendering matches JSP output
   - Test all component variations

3. **Visual Regression**
   - Screenshot comparison (before/after)
   - Cross-browser testing

4. **User Acceptance**
   - Test in author environment
   - Verify all features work
   - Test edge cases

**Test Checklist:**
- [ ] Component renders without errors
- [ ] All properties display correctly
- [ ] Interactive features work (if any)
- [ ] XSS protection effective
- [ ] Performance acceptable
- [ ] Accessible (WCAG compliance)
- [ ] Works in all supported browsers

---

## Part 4: Recommendations Summary

### 4.1 Immediate Actions (Week 1)

1. **Fix Frontend Issues**
   - [ ] Fix license header typo in `cms.form.js`
   - [ ] Correct file permissions (7 files)
   - [ ] Run `mvn spotless:apply`

2. **✅ Resolve Duplicate Templates** *(COMPLETED 2025-12-26)*
   - [x] Investigate which version is active
   - [x] Delete inactive duplicates (6 JSP files removed)
   - [x] Document decision (see [duplicate-template-resolution.md](duplicate-template-resolution.md))

3. **Create Documentation**
   - [ ] Frontend README
   - [ ] Component development guide
   - [ ] HTL migration guide updates

### 4.2 Short-Term Goals (Weeks 2-8)

1. **Migrate High-Priority Categories**
   - [ ] caconfig (20 files) → HTL
   - [ ] pages (15 files) → HTL
   - [ ] publication (7 files) → HTL

2. **Reorganize Components**
   - [ ] Group editor fields by type
   - [ ] Consolidate asset components
   - [ ] Add missing `.content.xml` files

3. **Improve Build System**
   - [ ] Split large JavaScript modules
   - [ ] Add dynamic imports for heavy components
   - [ ] Document build profiles

### 4.3 Medium-Term Goals (Weeks 9-16)

1. **Complete JSP Migration**
   - [ ] Migrate all editor fields
   - [ ] Migrate jobs category
   - [ ] Migrate remaining cms components
   - [ ] Delete `global.jsp`

2. **Optimize Performance**
   - [ ] Implement lazy loading
   - [ ] Add service worker
   - [ ] Optimize CSS (purge unused)
   - [ ] Tree-shake icon fonts

3. **Enhance Developer Experience**
   - [ ] Create component generator script
   - [ ] Add Storybook for component development
   - [ ] Improve hot reload workflow

### 4.4 Long-Term Goals (Months 4-6)

1. **Architecture Improvements**
   - [ ] Consider micro-frontend architecture
   - [ ] Evaluate Web Components
   - [ ] Implement design system

2. **Framework Evolution**
   - [ ] Evaluate Bulma alternatives
   - [ ] Consider CSS-in-JS
   - [ ] Modernize icon system

3. **Testing Infrastructure**
   - [ ] Add visual regression testing
   - [ ] Improve Cypress coverage
   - [ ] Add accessibility testing

---

## Part 5: Metrics & Success Criteria

### 5.1 Current Baseline

| Metric | Current | Target |
|--------|---------|--------|
| JSP Files | 139 (72.0%) *(was 145)* | 0 (0%) |
| HTL Files | 54 (28.0%) | 193 (100%) |
| Duplicate Templates | 0 files ✅ | 0 files |
| Files without `.content.xml` | ~183 | 0 |
| JavaScript Modules > 10KB | 2 files | 0 files |
| SCSS Files with Incorrect Permissions | 7 files | 0 files |

### 5.2 Phase Success Criteria

**Phase 1 - Foundation:**
- ✅ Zero duplicate templates
- ✅ HTL component template documented
- ✅ Migration guide updated

**Phase 2 - High-Impact:**
- ✅ caconfig: 0% JSP
- ✅ pages: 0% JSP
- ✅ publication: 0% JSP
- ✅ Overall: <50% JSP

**Phase 3 - Editor Fields:**
- ✅ editor: <25% JSP
- ✅ Field components reorganized
- ✅ Overall: <25% JSP

**Phase 4 - Complete:**
- ✅ 0% JSP across all categories
- ✅ `global.jsp` deleted
- ✅ All components have `.content.xml`
- ✅ 100% HTL codebase

### 5.3 Quality Gates

**Before Merging Each Migration PR:**
- [ ] All tests pass
- [ ] No console errors
- [ ] Visual regression check passes
- [ ] Code review approved
- [ ] Performance metrics acceptable
- [ ] Accessibility check passes
- [ ] Documentation updated

---

## Part 6: Risk Assessment

### 6.1 Migration Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| Breaking existing content | High | Extensive testing, staged rollout |
| Performance degradation | Medium | Performance testing, benchmarking |
| Developer unfamiliarity with HTL | Medium | Training, documentation, pair programming |
| Scope creep | Medium | Strict phase boundaries, focused PRs |
| Regression bugs | High | Comprehensive test coverage |
| Timeline overrun | Low | Realistic estimates, buffer time |

### 6.2 Technical Debt

**Current Technical Debt:**
- 145 JSP files requiring migration
- 7 duplicate template files
- ~183 components without metadata
- Large monolithic JavaScript modules
- Inconsistent component organization
- Global JSP dependency pattern

**Debt Reduction Strategy:**
- Systematic migration (not big-bang)
- Incremental improvements
- Test-driven migration
- Documentation alongside code

---

## Part 7: Resources & References

### 7.1 Documentation

**Existing Docs:**
- `/docs/jsp-to-htl-migration.md` - Migration guide
- `/docs/developers.md` - Developer documentation
- `frontend/src/main/frontend/js/cms.movecopy.README.md` - Move/copy feature

**Recommended Reading:**
- HTL Specification: https://github.com/adobe/htl-spec
- Sling HTL: https://sling.apache.org/documentation/bundles/scripting/scripting-htl.html
- AEM HTL: https://experienceleague.adobe.com/docs/experience-manager-htl/

### 7.2 Tools

**Development:**
- VSCode HTL Extension
- Sling HTL REPL
- Sling Mock for testing

**Testing:**
- Cypress (E2E)
- JUnit 5 (unit)
- Sling Mock (integration)

**Build:**
- Maven
- Vite
- npm/bun

### 7.3 Key Files Reference

**Frontend Module:**
- `frontend/pom.xml` - Maven build
- `frontend/src/main/frontend/package.json` - npm dependencies
- `frontend/src/main/frontend/vite.config.js` - Vite configuration
- `frontend/src/main/frontend/scss/cms.scss` - Main stylesheet
- `frontend/src/main/frontend/js/cms-entry.js` - Main JS entry

**UI Module:**
- `ui/pom.xml` - Maven build
- `ui/bnd.bnd` - OSGi bundle config
- `ui/src/main/resources/jcr_root/libs/sling-cms/components/` - Components
- `ui/src/main/resources/jcr_root/libs/sling-cms/content/` - Content
- `ui/src/main/resources/jcr_root/libs/sling-cms/global.jsp` - Global JSP

---

## Conclusion

This analysis reveals a well-architected frontend build system but significant technical debt in the UI module. The migration from JSP to HTL is essential for:

- **Maintainability** - Modern, declarative templates
- **Security** - Built-in XSS protection
- **Performance** - Better caching, pre-compilation
- **Developer Experience** - Cleaner syntax, better tooling
- **Future-proofing** - JSP is deprecated in modern Sling

The proposed 4-phase migration strategy provides a systematic approach to eliminate 145 JSP files while minimizing risk and maintaining system stability.

**Next Steps:**
1. Review and approve migration strategy
2. Create JIRA tickets for each phase
3. Assign resources and set timeline
4. Begin Phase 1 (Foundation)

---

**Report Author:** Claude Code Analysis
**Date:** 2025-12-26
**Version:** 1.0
