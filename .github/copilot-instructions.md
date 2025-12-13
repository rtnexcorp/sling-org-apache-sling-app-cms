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

❌ **Do NOT create new JSP files**

**JSP to HTL Migration**: When modifying existing JSP files, convert them to HTL:
- Look for opportunities during bug fixes or feature additions
- JSP files are located under `/libs/sling-cms/components/`
- Create equivalent `.html` HTL file and remove the `.jsp`
- Use Sling Models for business logic instead of scriptlets

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
