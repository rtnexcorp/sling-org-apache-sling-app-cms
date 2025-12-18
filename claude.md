# SPDX-License-Identifier: Apache-2.0

# Apache Sling CMS - Claude Code Context

## Project Overview

Apache Sling CMS is a **reference implementation** of a fully-featured Content Management System built on Apache Sling, an OSGi-based web framework. This is an **Apache Software Foundation (ASF) project** licensed under Apache License 2.0, which means it can be freely used, modified, and redistributed in both commercial and non-commercial projects.

### License & Usage Rights

- **License**: Apache License 2.0
- **Use**: Free for personal, commercial, and enterprise use
- **Modification**: Can be freely modified and extended
- **Distribution**: Can be redistributed with proper attribution
- **Attribution**: Must retain Apache license headers and NOTICE file
- **Contribution**: Contributions require Apache Contributor License Agreement (CLA)

### Project Purpose

This application showcases multiple Apache Sling features including Context Aware Configurations, Resource Mapping, Rewriter, and Resource Merging. It serves as:
1. A **reference implementation** for building CMS applications with Apache Sling
2. A **starting point** for custom CMS projects
3. A **learning resource** for Apache Sling best practices

**Technology Stack:**
- Java 21
- Apache Sling Framework (OSGi-based)
- Apache Jackrabbit Oak (JCR repository)
- Maven (multi-module project)
- Frontend: Node.js/Gulp, SCSS, Bulma CSS, Jam Icons
- Testing: JUnit 5, Mockito, Sling Mock, Cypress

**Version:** 1.1.9-SNAPSHOT

## Project Structure

This is a multi-module Maven project with the following modules:

| Module | Purpose |
|--------|---------|
| **`api/`** | Public API interfaces (e.g., `Site`, `Page`, `PageManager`) |
| **`core/`** | OSGi services, Sling Models, servlets, and business logic |
| **`ui/`** | Content, scripts, components under `/libs/sling-cms` |
| **`frontend/`** | JavaScript/CSS assets (built with Node.js/Gulp) |
| **`reference/`** | Reference implementation for extending the CMS |
| **`thumbnails/`** | **Digital Asset Management** - thumbnails, transformations, video support, renditions |
| **`feature/`** | Sling Feature Model definitions (`.json` files) |
| **`distribution/`** | **Content Publishing & Workflow** - content distribution, workflow management |
| **`login/`** | Authentication/login functionality |
| **`archetype/`** | Maven archetype for creating new projects |
| **`it/`** | Integration tests (Cypress E2E tests)

### Asset Management Consolidation (In Progress)

**⚠️ IMPORTANT: All asset/DAM-related code should reside in the `thumbnails` module.**

This includes:
- Thumbnail generation and image transformations
- Video frame extraction and processing
- Rendition management
- File metadata extraction
- Asset-related Sling Models and services

**Migration Plan**: Some asset-related code currently exists in `core` and `api`. When working on asset features:
1. New asset code → **always add to `thumbnails` module**
2. Existing asset code in `core`/`api` → plan migration to `thumbnails`
3. Asset-related UI components → consider `thumbnails` module's UI resources

**Current locations to consolidate** (migrate systematically):
- `core`: FileMetadataExtractor, rendition listeners, asset jobs
- `api`: Asset-related interfaces
- `ui`: DAM components under `/libs/sling-cms/components/cms/dam`

### Deployment & Infrastructure

- **`deployment/`** - Deployment configurations (author/publisher)
  - `./deployment/start-author.sh` - Start author instance (port 8082)
  - `./deployment/start-standalone.sh` - Start standalone instance (port 8080)
- **`docker/`** - Docker configurations
- **`helm/`** - Kubernetes Helm charts
- **`vagrant/`** - Vagrant development environment

## Development Environment

### Build & Run

**Local Sling Instance:**
- Login: http://localhost:8080/cms or http://localhost:8082/cms
- Default credentials: `admin/admin`

**Deployment Profiles:**
| Profile | Port | Use Case |
|---------|------|----------|
| `autoInstallBundle` | 8082 | Default/author instance |
| `standalone` | 8080 | Standalone instance |
| `renderer` | 8083 | Publisher instance |

**Build Commands:**
```bash
# Full build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Format code (REQUIRED before committing)
mvn spotless:apply

# Deploy to author instance (port 8082)
mvn clean install -P autoInstallBundle -DskipTests

# Hot deploy single module (faster during development)
mvn clean install -P autoInstallBundle -pl core -DskipTests -Dbnd.baseline.skip=true

# Deploy all modules
mvn clean install -P autoInstallBundle -pl api,core,login,ui,reference,thumbnails -DskipTests -Dbnd.baseline.skip=true
```

**Starting Sling CMS:**
```bash
./deployment/start-standalone.sh    # Port 8080
./deployment/start-author.sh        # Port 8082
```

### Key Technologies

**Java Dependencies:**
- Apache Jackrabbit Oak: 1.86.0
- SLF4J: 1.7.36
- Jackson: 2.18.2
- Groovy: 4.0.24
- Guava: 33.3.1-jre
- Apache Commons IO: 2.21.0
- Apache Commons Lang3: 3.20.0
- PDFBox: 3.0.6
- JSoup: 1.21.2

**Testing:**
- JUnit: 6.1.0-M1
- Mockito: 5.21.0
- Sling Mock: 3.5.2
- Cypress (for integration tests)

## Architecture Patterns

### Critical Rules

#### 1. OSGi R7/R8 Compliant Code (REQUIRED)

**✅ ALWAYS use these annotations** (from `org.osgi.service.component.annotations`):
```java
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.AttributeDefinition;
```

**❌ NEVER use deprecated Felix SCR annotations**:
- `org.apache.felix.scr.annotations.*` (deprecated)

**Service Component Example**:
```java
@Component(service = MyService.class, configurationPolicy = ConfigurationPolicy.OPTIONAL)
@Designate(ocd = MyServiceImpl.Config.class)
public class MyServiceImpl implements MyService {

    @ObjectClassDefinition(name = "My Service Configuration")
    public @interface Config {
        @AttributeDefinition(name = "Enabled", description = "Enable the service")
        boolean enabled() default true;
    }

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private volatile List<MyExtension> extensions;

    @Activate
    protected void activate(Config config) {
        // initialization
    }

    @Modified
    protected void modified(Config config) {
        // configuration changed
    }

    @Deactivate
    protected void deactivate() {
        // cleanup
    }
}
```

**Key OSGi R7+ Patterns**:
- Use `@Reference` for dependency injection (not constructor injection)
- Use `@Designate` + `@ObjectClassDefinition` for configuration
- Use `ConfigurationPolicy.OPTIONAL` or `REQUIRE` as appropriate
- Use `ReferenceCardinality.MULTIPLE` with `ReferencePolicy.DYNAMIC` for plugin patterns
- Prefer `volatile` fields for dynamic references

#### 2. OSGi-Compliant Dependencies (CRITICAL)

**⚠️ This is an OSGi-based application - ALWAYS prefer OSGi-compliant libraries!**

**✅ Preferred: Use libraries that are already OSGi bundles:**
- Apache Commons libraries (commons-io, commons-lang3)
- Apache Tika, PDFBox, POI (with ServiceMix wrappers)
- Jackson, SLF4J, Guava
- System tools via `ProcessBuilder` (e.g., FFmpeg for video)

**❌ AVOID: Embedding non-OSGi libraries via `bnd.bnd -includeresource`**
- Only embed as last resort when no OSGi alternative exists
- Example exception: `thumbnailator` (no OSGi version available)

**Finding OSGi bundles**:
1. Check if library has `Bundle-SymbolicName` in MANIFEST.MF
2. Look for ServiceMix wrappers: `org.apache.servicemix.bundles:org.apache.servicemix.bundles.<library>`

**Adding External Dependencies:**

**Strongly prefer OSGi-compliant libraries:**
1. Check if library is an OSGi bundle (has `Bundle-SymbolicName` in MANIFEST.MF)
2. Look for ServiceMix wrappers: `org.apache.servicemix.bundles:org.apache.servicemix.bundles.<library>`
3. Add to `pom.xml` with `<scope>provided</scope>` for OSGi bundles
4. Add to `feature/*.json` for runtime availability

**Only if no OSGi alternative exists:**
1. Add to `pom.xml` with `<scope>compile</scope>`
2. Embed via `bnd.bnd`: `-includeresource: lib/name.jar=name-*.jar;lib:=true`
3. Document why embedding was necessary

#### 3. Reuse Existing Libraries (CRITICAL)

**❌ DO NOT write custom code when existing libraries provide the functionality.**

**Search order for functionality:**
1. **Apache Sling APIs** - `org.apache.sling.api.*`, `org.apache.sling.commons.*`
2. **JCR/Jackrabbit** - `javax.jcr.*`, `org.apache.jackrabbit.*`
3. **OSGi Core** - `org.osgi.framework.*`, `org.osgi.service.*`
4. **Apache Commons** - `commons-io`, `commons-lang3`, `commons-text`
5. **Apache Tika** - File type detection, metadata extraction
6. **Apache POI** - Office document processing
7. **Apache PDFBox** - PDF processing
8. **Only then** - Write custom implementation

**Common Sling utilities to use:**
- `ResourceUtil` - Resource path manipulation, type checking
- `JcrUtil` - JCR node operations
- `StreamUtils` / `IOUtils` - Stream handling
- `StringUtils` - String operations (use Apache Commons)
- `ResourceResolver` - Resource operations, queries

**Example - WRONG vs RIGHT:**
```java
// ❌ WRONG: Writing custom file type detection
String mimeType = detectMimeType(inputStream);

// ✅ RIGHT: Use Apache Tika
@Reference
private TikaService tikaService;
String mimeType = tikaService.detect(inputStream);

// ❌ WRONG: Custom path manipulation
String parentPath = path.substring(0, path.lastIndexOf('/'));

// ✅ RIGHT: Use ResourceUtil
String parentPath = ResourceUtil.getParent(path);
```

#### 4. HTL Over JSP (MANDATORY)

**⚠️ ALWAYS use HTL (Sightly) for new templates. NEVER write new JSP files.**

**✅ Use HTL** for all new component rendering:
```html
<sly data-sly-use.model="org.apache.sling.cms.core.models.MyModel">
    <div class="my-component">
        <h1>${model.title}</h1>
        <sly data-sly-list.item="${model.items}">
            <p>${item.name}</p>
        </sly>
    </div>
</sly>
```

**❌ Do NOT create new JSP files** - This is strictly enforced!

**New Component File Structure**:
When creating a new component, always create:
1. `.content.xml` - Component definition
2. `<componentname>.html` - HTL template (NOT `.jsp`)
3. Sling Model in appropriate module for business logic

**Example - Creating a new component**:
```
ui/src/main/resources/jcr_root/libs/sling-cms/components/cms/mycomponent/
├── .content.xml          # Component definition
└── mycomponent.html      # HTL template (REQUIRED - not JSP!)
```

**JSP to HTL Migration**: When modifying existing JSP files, convert them to HTL:
- Look for opportunities during bug fixes or feature additions
- JSP files are located under `/libs/sling-cms/components/`
- Create equivalent `.html` HTL file and remove the `.jsp`
- Use Sling Models for business logic instead of scriptlets
- **MUST follow guidance in**: `/docs/jsp-to-htl-migration.md`

### Sling-Specific Patterns

1. **Resource-based Architecture**
   - Content stored in JCR (Java Content Repository)
   - Resources accessed via Sling Resource API
   - Content structure in `jcr_root/`
   - CMS content lives under `/libs/sling-cms/content` (mapped to `/cms` URL)
   - Components at `/libs/sling-cms/components` with `jcr:primaryType=sling:Component`
   - Uses Sling Resource Merger: content can be overlaid via `/apps/sling-cms/content`

2. **Sling Models**
   - Used for adapting resources to Java objects
   - Annotation-based configuration
   - Declare in `bnd.bnd` via `Sling-Model-Packages` header
   - Example: `@Model(adaptables = Resource.class, adapters = PageManager.class)`

3. **OSGi Services**
   - Modular service architecture
   - Declarative Services (DS) components
   - Service interfaces in `api/`, implementations in `core/`
   - Use `@Component` annotation with explicit `service` attribute

4. **HTL/Sightly Templates**
   - Server-side templating language
   - Component rendering
   - Always use HTL for new components (never JSP)

### Key API Concepts

**Core Abstractions:**
- **Page** - Content page representation
- **Site** - Site configuration and structure
- **Component** - Reusable UI components
- **File** - Digital asset management
- **PageTemplate** - Page type definitions
- **ComponentPolicy** - Component configuration policies

**Managers:**
- **PageManager** - Page CRUD operations
- **SiteManager** - Site management
- **ComponentManager** - Component operations
- **FileManager** - Asset operations
- **PageTemplateManager** - Template management

**Specialized Services:**
- **TaxonomyService** - Taxonomy/categorization
- **PublishableResource** - Content publication workflow
- **CMSJobManager** - Background job execution
- **FileMetadataExtractor** - Asset metadata extraction

## Coding Conventions

### Code Symmetry & Consistency

**⚠️ CRITICAL: Follow existing patterns in each module.**

Before adding new code:
1. Study existing code in the same module for patterns, naming, structure
2. Match package organization, class naming conventions
3. Use similar approach for error handling, logging, testing
4. Keep consistent with existing Sling Models, servlets, and services in that module

### Java Code

1. **Package Structure:**
   - API interfaces: `org.apache.sling.cms.*` in `api/` module
   - Implementations: `org.apache.sling.cms.core.*` in `core/` module (often in `internal/` packages)

2. **OSGi Annotations:**
   - Use OSGi R7/R8 annotations (see Critical Rules above)
   - `@Component`, `@Service`, `@Reference` for declarative services
   - Never use deprecated Felix SCR annotations

3. **Code Quality:**
   - **REQUIRED**: Use Spotless for formatting: `mvn spotless:apply`
   - Follow Apache Sling coding conventions
   - Java 21 language features available
   - Import order: Standard Java imports first, javax imports order (ImageIO before java.awt)

4. **Documentation:**
   - JavaDoc for public APIs
   - Package-info.java files for package documentation and `@Version` annotations

5. **API Versioning:**
   - Update `@Version` in `package-info.java` when adding new APIs
   - Minor version bump (1.0.0 → 1.1.0) for new interfaces/methods
   - Use `-Dbnd.baseline.skip=true` during development

### Frontend Code - SCSS/CSS Organization (REQUIRED)

**⚠️ CRITICAL: Create separate SCSS files for new components. Never add styles inline or to existing large files.**

#### File Structure
```
frontend/src/main/frontend/scss/
├── cms.scss              # Main entry - imports all partials
├── _variables.scss       # Variables and mixins
├── _base.scss            # Base styles
├── _assets.scss          # Asset browser/grid styles
├── _mycomponent.scss     # NEW: Separate file for new component
└── ...
```

#### Steps for Adding Component Styles

1. Create new file: `frontend/src/main/frontend/scss/_mycomponent.scss`
2. Add `@import 'mycomponent';` to `cms.scss`
3. Use BEM naming convention: `.my-component`, `.my-component__element`, `.my-component--modifier`

**Example - New component SCSS file** (`_contentfilter.scss`):
```scss
// Content Filter Component
// Provides filtering UI for content navigation

.content-filter {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;

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

**❌ Do NOT:**
- Add new component styles directly to `_assets.scss` or other existing files
- Use inline styles in HTL templates
- Create monolithic CSS files

#### CSS Framework Abstraction (REQUIRED)

**Current framework: Bulma CSS + Jam Icons**

Write code to minimize impact when upgrading or switching frameworks.

**✅ Framework-Agnostic Patterns:**

**1. Use Semantic HTL Classes - Not Framework Classes Directly**
```html
<!-- ❌ WRONG: Direct Bulma classes in HTL -->
<div class="box has-background-light">
  <button class="button is-primary is-large">Save</button>
</div>

<!-- ✅ RIGHT: Semantic classes mapped in SCSS -->
<div class="cms-panel cms-panel--light">
  <button class="cms-button cms-button--primary cms-button--large">Save</button>
</div>
```

**2. Create SCSS Mixins/Variables for Framework Features**
```scss
// _variables.scss - Abstraction layer
@import '~bulma/sass/utilities/initial-variables';

// Abstract framework variables
$cms-primary-color: $primary !default;
$cms-spacing-unit: 0.5rem !default;

// Abstract framework mixins
@mixin cms-button-base {
  @extend .button; // Bulma specific
}
```

**3. Icon Abstraction Pattern**
```html
<!-- ❌ WRONG: Direct Jam Icons classes -->
<span class="jam jam-check"></span>

<!-- ✅ RIGHT: Semantic icon classes -->
<span class="cms-icon cms-icon--check"></span>
```

**When Adding New Components**:
- Use `cms-*` prefix for all custom classes
- Define semantic classes in component SCSS file
- Use abstracted variables/mixins, not framework-specific values
- Never hardcode framework class names in HTL

### JavaScript Code

1. **ES6+ modules**
2. **Linting**: ESLint (`.eslintrc.js`)
3. **Build**: Node.js/Gulp (not Vite/Rollup as previously mentioned)
4. **Location**: `frontend/src/main/frontend/js/`

### Assets

- Images: `frontend/src/main/frontend/img/`
- Fonts: `frontend/src/main/frontend/fonts/`

## Common Development Tasks

### Adding a New Service

1. Create interface in `api/src/main/java/.../api/`
2. Implement in `core/src/main/java/.../internal/`
3. Register with `@Component(service = YourInterface.class)`
4. Use OSGi R7/R8 annotations (see Critical Rules)
5. Add unit tests (JUnit 5 + Mockito + Sling Mock)
6. Bump package version in `package-info.java` if API changed

### Adding a New Component

**File Structure:**
```
ui/src/main/resources/jcr_root/libs/sling-cms/components/cms/mycomponent/
├── .content.xml          # Component definition
└── mycomponent.html      # HTL template (NEVER .jsp!)
```

**Steps:**
1. Create component directory in `ui/src/main/resources/jcr_root/libs/sling-cms/components/cms/`
2. Add `.content.xml` with `jcr:primaryType=sling:Component`
3. Create HTL template (`.html` file, never JSP)
4. Create Sling Model in appropriate module if needed
5. Create separate SCSS file in `frontend/src/main/frontend/scss/_mycomponent.scss`
6. Import SCSS in `cms.scss`
7. Use semantic CSS classes (e.g., `cms-mycomponent`, not Bulma classes directly)

### Working with Content

**JCR Content Location:**
- UI content: `ui/src/main/resources/jcr_root/`
- Content structure defined in `.content.xml` files
- Component definitions in `libs/sling-cms/components/`
- CMS content lives under `/libs/sling-cms/content` (mapped to `/cms` URL)
- Can be overlaid via `/apps/sling-cms/content` (Sling Resource Merger)

### Frontend Development

1. Navigate to `frontend/src/main/frontend/`
2. Install dependencies: `npm install`
3. Make changes to JavaScript (`js/`) or SCSS (`scss/`)
4. Build: `npm run build` (or use Gulp)
5. Deploy module: `mvn clean install -P autoInstallBundle -pl frontend -DskipTests`

## Testing

### Unit Tests
- **JUnit 5** tests in each module's `src/test/java/`
- **Mockito** for mocking dependencies (version 5.21.0)
- **Sling Mock** for resource testing (version 3.5.2)

### Integration Tests
- **Cypress** E2E tests in `it/` module
- Tests require running instance

### Running Tests
```bash
# Run all tests
mvn clean verify

# Run specific module tests
cd core && mvn test

# Run integration tests (requires running instance)
cd it && npm test
```

## Important Files & Locations

### Configuration
- **Maven**: `pom.xml` (root and module-level)
  - Root pom: Version properties, dependency management, build profiles
- **OSGi Bundle**: `*/bnd.bnd` - OSGi bundle configuration, package exports, embedded JARs
- **Sling Feature**: `*/src/main/features/*.json` - Sling Feature Model for runtime bundles
- **API Versioning**: `*/package-info.java` - API versioning for semantic versioning compliance
- **OSGi Config**: `ui/src/main/resources/jcr_root/conf/`

### Documentation
- **User docs**: `docs/`
  - [Quickstart](docs/quickstart.md)
  - [Administration](docs/administration.md)
  - [Developers](docs/developers.md)
  - **[JSP to HTL Migration](docs/jsp-to-htl-migration.md)** - MUST READ for component work
- **API JavaDocs**: Generated in `target/apidocs/`

### Build Artifacts
- All build outputs: `target/` (gitignored)
- Distribution JAR: `distribution/target/`
- Frontend build: `frontend/src/main/frontend/dist/` (gitignored)

## Internationalization (i18n)

- i18n support available
- Helper tools in `i18n-helper/`
- Translation files in content structure
- Recent commits include i18n improvements

## CI/CD

- GitHub Actions workflows in `.github/workflows/`
- SonarCloud integration for code quality
- CodeQL for security scanning
- Automated builds and testing

## Useful Resources

- **Documentation:** See `docs/` directory
  - [Quickstart](docs/quickstart.md)
  - [Administration](docs/administration.md)
  - [Developers](docs/developers.md)
- **Apache Sling:** https://sling.apache.org/
- **Issue Tracker:** https://issues.apache.org/jira (Component: "App CMS")
- **Contributing:** https://sling.apache.org/contributing.html

## Apache License Compliance

### When Creating New Files

**For Java source files and POM files**, use the full Apache License header:

```java
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
```

**For configuration/script files** (`.md`, `.sh`, `.yml`, etc.), use SPDX identifier:
```
# SPDX-License-Identifier: Apache-2.0
```

**For XML files** (non-POM), use Apache header in XML comments:
```xml
<!-- Licensed to the Apache Software Foundation (ASF) under one or more contributor
     license agreements. See the NOTICE file distributed with this work for additional
     information regarding copyright ownership. The ASF licenses this file to you under
     the Apache License, Version 2.0 (the "License"); you may not use this file except
     in compliance with the License. You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or
     agreed to in writing, software distributed under the License is distributed on an
     "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
     implied. See the License for the specific language governing permissions and
     limitations under the License. -->
```

### Attribution Requirements

When modifying or extending this project:
1. **Retain** all existing Apache license headers in files
2. **Preserve** the NOTICE file when distributing
3. **Include** LICENSE file in distributions
4. **Document** any modifications in NOTICE or separate file
5. **Do not** use Apache trademarks without permission

## Tips for Claude Code

### Critical Reminders

1. **ALWAYS add Apache license headers** - Required for all new source files
2. **ALWAYS use OSGi R7/R8 annotations** - Never deprecated Felix SCR annotations
3. **NEVER create new JSP files** - Always use HTL (`.html` files)
4. **NEVER embed non-OSGi libraries** - Unless absolutely no alternative exists
5. **ALWAYS create separate SCSS files** - Never add to existing large files
6. **ALWAYS use semantic CSS classes** - Never use Bulma/Jam classes directly in HTL
7. **ALWAYS follow existing patterns** - Study the module before adding code
8. **ALWAYS reuse existing libraries** - Check Sling/Commons/Tika/etc. first
9. **ALWAYS run `mvn spotless:apply`** - Before committing any code

### When Modifying Java Code

1. Check both `api/` and `core/` modules
2. Use OSGi R7/R8 annotations (see Critical Rules section)
3. Ensure OSGi service references are correct
4. Follow package structure conventions (implementations in `internal/`)
5. Add unit tests (JUnit 5 + Mockito + Sling Mock)
6. Update `package-info.java` version if changing APIs
7. Run `mvn spotless:apply` before committing
8. Use `-Dbnd.baseline.skip=true` during development if needed

### When Working with Components

1. **NEVER create `.jsp` files** - Always use HTL (`.html`)
2. Read `/docs/jsp-to-htl-migration.md` when migrating JSP
3. Create separate SCSS file for component styles
4. Use semantic CSS class names with `cms-` prefix
5. Create Sling Model for business logic (not scriptlets)
6. Follow BEM naming in SCSS
7. Use abstracted variables/mixins, not framework-specific values

### When Working with Content

1. Content structure is JCR-based (hierarchical)
2. `.content.xml` files define node structure
3. Properties and node types follow JCR specifications
4. Content lives under `/libs/sling-cms/content` → `/cms` URL
5. Can be overlaid via `/apps/sling-cms/content`

### When Adding Dependencies

1. **FIRST**: Check if it's OSGi-compliant (has `Bundle-SymbolicName`)
2. **SECOND**: Look for ServiceMix wrapper
3. **THIRD**: Add to `pom.xml` with `<scope>provided</scope>`
4. **FOURTH**: Add to `feature/*.json` for runtime
5. **LAST RESORT**: Embed via `bnd.bnd` and document why

### When Working with Assets/DAM

⚠️ **All new asset/DAM code goes in `thumbnails` module** - not `core` or `api`

### When Debugging

- Sling Console: http://localhost:8082/system/console (or 8080/8083)
- Felix Web Console for OSGi troubleshooting
- Sling Resource Resolver for content inspection
- Check logs in console

### Hot Deploy Workflow

For fastest development iteration:
```bash
# Make changes to code
# Run Spotless
mvn spotless:apply

# Hot deploy single module (e.g., core)
mvn clean install -P autoInstallBundle -pl core -DskipTests -Dbnd.baseline.skip=true

# Test in browser at localhost:8082/cms
```

### Recent Development Focus

- i18n support improvements
- DAM (Digital Asset Management) fixes (in `thumbnails` module)
- Search functionality enhancements
- Preset management

### Common Pitfalls to Avoid

❌ Forgetting Apache license headers in new files
❌ Using Felix SCR annotations instead of OSGi R7/R8
❌ Creating new JSP files instead of HTL
❌ Adding styles to existing large SCSS files
❌ Using Bulma classes directly in HTL
❌ Embedding non-OSGi libraries without checking for alternatives
❌ Writing custom code when libraries provide functionality
❌ Adding asset code to `core` instead of `thumbnails`
❌ Forgetting to run `mvn spotless:apply`
❌ Not following existing code patterns in the module
❌ Removing or modifying existing license headers

## Extending This Project

### For Personal/Commercial Use

This project can be freely used and extended for:
- **Personal projects** - Learn, experiment, build custom solutions
- **Commercial products** - Build commercial CMS solutions
- **Enterprise applications** - Internal or customer-facing CMS systems
- **Derivative works** - Create modified versions or extensions

### Best Practices for Extensions

1. **Create separate modules** for custom functionality
2. **Use `/apps` overlay** for UI customizations (not `/libs`)
3. **Keep customizations separate** from core Sling CMS code
4. **Document changes** in your project's README or NOTICE
5. **Maintain upgrade path** by avoiding modifications to core files

### Example Extension Structure

```
my-custom-cms/
├── pom.xml                          # Your parent POM
├── core/                            # Your custom core bundle
│   └── src/main/java/com/mycompany/cms/
├── ui/                              # Your custom UI
│   └── src/main/resources/jcr_root/
│       └── apps/                    # Use /apps overlay, not /libs
│           └── my-custom-cms/
├── feature/                         # Your feature model
│   └── includes sling-cms feature   # Include Apache Sling CMS as dependency
└── LICENSE                          # Apache 2.0 license
```

### Redistribution Requirements

If you redistribute this software (modified or unmodified):

1. **Include Apache License 2.0** - Include LICENSE file
2. **Include NOTICE file** - Document Apache Sling CMS usage
3. **Retain license headers** - Don't remove from source files
4. **Document modifications** - If you changed the code
5. **Trademark compliance** - Don't imply Apache endorsement

### Contributing Back to Apache Sling CMS

To contribute improvements back to this project:

1. **Sign Apache CLA** - Individual or Corporate Contributor License Agreement
2. **Create JIRA ticket** - https://issues.apache.org/jira (Component: "App CMS")
3. **Follow contribution guidelines** - https://sling.apache.org/contributing.html
4. **Submit Pull Request** - With reference to JIRA ticket
5. **Participate in review** - Address feedback from committers

**Benefits of contributing:**
- Improvements maintained by community
- Features available in official releases
- Recognition in Apache community
- Better long-term support for your use case
