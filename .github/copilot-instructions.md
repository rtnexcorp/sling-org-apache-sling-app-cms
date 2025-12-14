# Apache Sling CMS - Copilot Instructions

## Project Architecture

Apache Sling CMS is a reference CMS built on Apache Sling, an OSGi-based web framework. The project uses a multi-module Maven structure:

| Module | Purpose |
|--------|---------|
| `api` | Public API interfaces (e.g., `Site`, `Page`, `PageManager`) |
| `core` | OSGi services, Sling Models, servlets, and business logic |
| `ui` | Content, scripts, components under `/libs/sling-cms` |
| `frontend` | JavaScript/CSS assets (built with Node.js/Gulp) |
| `reference` | Reference implementation for extending the CMS |
| `thumbnails` | **Digital Asset Management** - thumbnails, transformations, video support, renditions |
| `feature` | Sling Feature Model definitions (`.json` files) |
| `distribution` | **Content Publishing & Workflow** - content distribution, workflow management |

### Asset Management Consolidation (In Progress)
**All asset/DAM-related code should reside in the `thumbnails` module.** This includes:
- Thumbnail generation and image transformations
- Video frame extraction and processing
- Rendition management
- File metadata extraction
- Asset-related Sling Models and services

**Migration Plan**: Some asset-related code currently exists in `core` and `api`. When working on asset features:
1. New asset code → always add to `thumbnails` module
2. Existing asset code in `core`/`api` → plan migration to `thumbnails`
3. Asset-related UI components → consider `thumbnails` module's UI resources

**Current locations to consolidate** (migrate systematically):
- `core`: FileMetadataExtractor, rendition listeners, asset jobs
- `api`: Asset-related interfaces
- `ui`: DAM components under `/libs/sling-cms/components/cms/dam`

## Key Patterns

### OSGi-Compliant Dependencies (IMPORTANT)
**Always prefer OSGi-compliant libraries.** This is an OSGi-based application - avoid embedding non-OSGi JARs.

✅ **Preferred**: Use libraries that are already OSGi bundles:
- Apache Commons libraries (commons-io, commons-lang3)
- Apache Tika, PDFBox, POI (with ServiceMix wrappers)
- Jackson, SLF4J, Guava
- System tools via `ProcessBuilder` (e.g., FFmpeg for video)

❌ **Avoid**: Embedding non-OSGi libraries via `bnd.bnd -includeresource`
- Only embed as last resort when no OSGi alternative exists
- Example exception: `thumbnailator` (no OSGi version available)

**Finding OSGi bundles**: Check if library has `Bundle-SymbolicName` in MANIFEST.MF, or look for ServiceMix wrappers (`org.apache.servicemix.bundles`).

### OSGi R7/R8 Compliant Code (REQUIRED)
**Generate OSGi R7+ compliant code using Declarative Services (DS) annotations.**

✅ **Use these annotations** (from `org.osgi.service.component.annotations`):
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

❌ **Avoid deprecated Felix SCR annotations**:
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

### Reuse Existing Libraries (IMPORTANT)
**Do NOT write custom code when existing libraries provide the functionality.**

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

### OSGi Components & Sling Models
- **Services**: Use `@Component` annotation with explicit `service` attribute
- **Sling Models**: Declare in `bnd.bnd` via `Sling-Model-Packages` header
- **Package versioning**: API packages use `@Version` in `package-info.java`

```java
// Example: core/src/main/java/.../PageManagerImpl.java
@Model(adaptables = Resource.class, adapters = PageManager.class)
public class PageManagerImpl implements PageManager { ... }
```

### Content Structure
- CMS content lives under `/libs/sling-cms/content` (mapped to `/cms` URL)
- Components at `/libs/sling-cms/components` with `jcr:primaryType=sling:Component`
- Uses Sling Resource Merger: content can be overlaid via `/apps/sling-cms/content`

### Code Symmetry & Consistency
**Follow existing patterns in each module.** Before adding new code:
1. Study existing code in the same module for patterns, naming, structure
2. Match package organization, class naming conventions
3. Use similar approach for error handling, logging, testing
4. Keep consistent with existing Sling Models, servlets, and services in that module

### HTL Over JSP (REQUIRED)
**Always use HTL (Sightly) for new templates. Never write new JSP files.**

✅ **Use HTL** for all new component rendering:
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

❌ **Do NOT create new JSP files** - This is strictly enforced!

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

### SCSS/CSS Organization (REQUIRED)
**Create separate SCSS files for new components. Never add styles inline or to existing large files.**

✅ **Create separate SCSS files** for each new component:
```
frontend/src/main/frontend/scss/
├── cms.scss              # Main entry - imports all partials
├── _variables.scss       # Variables and mixins
├── _base.scss            # Base styles
├── _assets.scss          # Asset browser/grid styles
├── _mycomponent.scss     # NEW: Separate file for new component
└── ...
```

**Steps for adding component styles**:
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

❌ **Do NOT**:
- Add new component styles directly to `_assets.scss` or other existing files
- Use inline styles in HTL templates
- Create monolithic CSS files

### CSS Framework Abstraction (REQUIRED)
**Current framework: Bulma CSS + Jam Icons. Write code to minimize impact when upgrading or switching frameworks.**

✅ **Framework-Agnostic Patterns**:

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
@import '~bulma/sass/utilities/functions';
@import '~bulma/sass/utilities/derived-variables';

// Abstract framework variables
$cms-primary-color: $primary !default;
$cms-danger-color: $danger !default;
$cms-spacing-unit: 0.5rem !default;
$cms-border-radius: 4px !default;

// Abstract framework mixins
@mixin cms-button-base {
  @extend .button; // Bulma specific
}

@mixin cms-card {
  @extend .box; // Bulma specific
}

@mixin cms-flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}
```

**3. Component-Specific SCSS Uses Abstractions**
```scss
// _mycomponent.scss
@import 'variables';

.cms-panel {
  @include cms-card;
  padding: $cms-spacing-unit * 2;
  border-radius: $cms-border-radius;
  
  &--light {
    background-color: lighten($cms-primary-color, 40%);
  }
}

.cms-button {
  @include cms-button-base;
  
  &--primary {
    background-color: $cms-primary-color;
    color: white;
  }
  
  &--large {
    padding: $cms-spacing-unit * 2 $cms-spacing-unit * 3;
  }
}
```

**4. Icon Abstraction Pattern**
```html
<!-- ❌ WRONG: Direct Jam Icons classes -->
<span class="jam jam-check"></span>

<!-- ✅ RIGHT: Semantic icon classes -->
<span class="cms-icon cms-icon--check"></span>
<span class="cms-icon cms-icon--plus"></span>
<span class="cms-icon cms-icon--trash"></span>
```

```scss
// _icons.scss - Icon abstraction layer
.cms-icon {
  display: inline-block;
  width: 1em;
  height: 1em;
  
  // Current implementation: Jam Icons
  @extend .jam;
  
  &--check { @extend .jam-check; }
  &--plus { @extend .jam-plus; }
  &--trash { @extend .jam-trash; }
  &--edit { @extend .jam-write; }
  &--close { @extend .jam-close; }
  // Add more as needed
}

// When switching icon library, only update this file:
// .cms-icon {
//   font-family: 'NewIconFont';
//   &--check { content: '\e001'; }
//   &--plus { content: '\e002'; }
// }
```

**5. Grid/Layout Abstraction**
```html
<!-- ❌ WRONG: Direct Bulma grid classes -->
<div class="columns">
  <div class="column is-half">...</div>
  <div class="column is-half">...</div>
</div>

<!-- ✅ RIGHT: Semantic layout classes -->
<div class="cms-grid">
  <div class="cms-grid__col-6">...</div>
  <div class="cms-grid__col-6">...</div>
</div>
```

```scss
// _layout.scss
.cms-grid {
  @extend .columns; // Bulma specific
  
  &__col-6 {
    @extend .column;
    @extend .is-half; // Bulma specific
  }
  
  &__col-4 {
    @extend .column;
    @extend .is-one-third;
  }
}
```

**6. Utility Class Abstraction**
```scss
// _utilities.scss - Framework utility abstractions
.cms-text-center { text-align: center; }
.cms-text-right { text-align: right; }
.cms-hidden { display: none; }
.cms-flex { display: flex; }
.cms-flex-center { @include cms-flex-center; }

// Spacing utilities (framework-agnostic)
.cms-m-1 { margin: $cms-spacing-unit; }
.cms-m-2 { margin: $cms-spacing-unit * 2; }
.cms-p-1 { padding: $cms-spacing-unit; }
.cms-p-2 { padding: $cms-spacing-unit * 2; }
```

**Framework Upgrade/Migration Checklist**:
1. ✅ Update `_variables.scss` to map new framework variables
2. ✅ Update `_icons.scss` to map new icon library
3. ✅ Update SCSS `@extend` directives in abstraction files
4. ✅ HTL templates remain unchanged (using semantic classes)
5. ✅ Test all components in style guide/component library

**When Adding New Components**:
- [ ] Use `cms-*` prefix for all custom classes
- [ ] Define semantic classes in component SCSS file
- [ ] Use abstracted variables/mixins, not framework-specific values
- [ ] Never hardcode framework class names in HTL
- [ ] Document component dependencies in SCSS comments

**Example - Complete Component**:
```html
<!-- contentfilter.html -->
<div class="cms-content-filter">
  <div class="cms-content-filter__controls">
    <label class="cms-content-filter__label">
      <span class="cms-icon cms-icon--filter"></span>
      Filter:
    </label>
    <button class="cms-button cms-button--small">
      <span class="cms-icon cms-icon--close"></span>
      Clear
    </button>
  </div>
</div>
```

```scss
// _contentfilter.scss
@import 'variables';

.cms-content-filter {
  display: flex;
  gap: $cms-spacing-unit;
  margin-bottom: $cms-spacing-unit * 2;
  padding: $cms-spacing-unit;
  background-color: lighten($cms-primary-color, 45%);
  border-radius: $cms-border-radius;
  
  &__controls {
    display: flex;
    align-items: center;
    gap: $cms-spacing-unit;
  }
  
  &__label {
    font-size: 0.875rem;
    color: darken($cms-primary-color, 10%);
    font-weight: 600;
  }
}
```

**Benefits of This Approach**:
- 🎯 HTL templates are framework-agnostic (99% unchanged during migration)
- 🔄 Framework changes isolated to SCSS abstraction layer
- 📦 Easy to create a theme or skin by changing variables
- 🧪 Easier to test - semantic class names are meaningful
- 📚 Better code readability and maintainability

### Embedding Non-OSGi Libraries
For libraries without OSGi metadata (e.g., thumbnailator), embed in bundle via `bnd.bnd`:
```
-includeresource: lib/thumbnailator.jar=thumbnailator-[[0-9\.]]*.jar;lib:=true
```

## Build & Development

### Full Build
```bash
mvn clean install                    # Build everything
mvn clean install -DskipTests       # Skip tests
```

### Hot Deploy to Running Instance
Use VS Code tasks (`Ctrl+Shift+B`) or command line:
```bash
# Deploy single module to author instance (port 8082)
mvn clean install -P autoInstallBundle -pl core -DskipTests -Dbnd.baseline.skip=true

# Deploy all modules
mvn clean install -P autoInstallBundle -pl api,core,login,ui,reference,thumbnails -DskipTests -Dbnd.baseline.skip=true
```

### Deployment Profiles
| Profile | Port | Use Case |
|---------|------|----------|
| `autoInstallBundle` | 8082 | Default/author |
| `standalone` | 8080 | Standalone instance |
| `renderer` | 8083 | Publisher instance |

### Starting Sling CMS
```bash
./deployment/start-standalone.sh    # Port 8080
./deployment/start-author.sh        # Port 8082
```
Login: http://localhost:8080/cms (admin/admin)

## Code Style

- **Spotless enforced**: Run `mvn spotless:apply` before committing
- **Java 21**: Target version set in parent pom
- **Import order**: Standard Java imports first, javax imports order (ImageIO before java.awt)

## Testing

- **Unit tests**: JUnit 5 + Mockito + Sling Mock in each module
- **Integration tests**: Cypress E2E tests in `it/` module
```bash
cd it && npm test                   # Run Cypress tests (requires running instance)
```

## Important Files

- `pom.xml` (root): Version properties, dependency management, build profiles
- `*/bnd.bnd`: OSGi bundle configuration, package exports, embedded JARs
- `*/src/main/features/*.json`: Sling Feature Model for runtime bundles
- `*/package-info.java`: API versioning for semantic versioning compliance

## Common Tasks

### Adding a New Service
1. Create interface in `api/src/main/java/.../api/`
2. Implement in `core/src/main/java/.../internal/`
3. Register with `@Component(service = YourInterface.class)`
4. Bump package version in `package-info.java` if API changed

### Adding External Dependencies
**Strongly prefer OSGi-compliant libraries:**
1. Check if library is an OSGi bundle (has `Bundle-SymbolicName` in MANIFEST.MF)
2. Look for ServiceMix wrappers: `org.apache.servicemix.bundles:org.apache.servicemix.bundles.<library>`
3. Add to `pom.xml` with `<scope>provided</scope>` for OSGi bundles
4. Add to `feature/*.json` for runtime availability

**Only if no OSGi alternative exists:**
1. Add to `pom.xml` with `<scope>compile</scope>`
2. Embed via `bnd.bnd`: `-includeresource: lib/name.jar=name-*.jar;lib:=true`
3. Document why embedding was necessary

### Baseline Version Errors
When adding new APIs, update `@Version` in `package-info.java`:
- Minor version bump (1.0.0 → 1.1.0) for new interfaces/methods
- Use `-Dbnd.baseline.skip=true` during development
