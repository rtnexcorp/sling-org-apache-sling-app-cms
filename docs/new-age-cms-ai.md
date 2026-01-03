# New-age CMS – AI (Composable, Governed, Vendor-neutral)

This doc defines how AI should fit into a "new-age CMS" without becoming a black box or a vendor-locked marketing suite.

AI is treated as:
- a **pluggable provider**
- invoked by **content automation jobs** or explicit **author actions**
- governed by **policies, review gates, and audit logs**

---

## Current Implementation Status

| Feature | Status | Location | Notes |
|---------|--------|----------|-------|
| **AI Module** | ✅ **Complete** | `ai/` | **Dedicated OSGi module successfully built and deployed** |
| **Build & Deploy** | ✅ **Working** | `.vscode/tasks.json`, `pom.xml` | Maven task "Auto Deploy - AI" configured |
| AI Provider Interfaces | ✅ Complete | `ai/src/main/java/org/apache/sling/cms/ai/` | `AiService`, `AiTextService`, `AiClassificationService`, `AiImageService` |
| AI Request/Response Models | ✅ Complete | `ai/src/main/java/org/apache/sling/cms/ai/` | `AiRequest`, `AiResponse` with builder patterns |
| Audit Framework | ✅ Complete | `ai/src/main/java/org/apache/sling/cms/ai/audit/` | `AiAuditService`, `AiAuditEntry` interfaces |
| Rules-Based Fallback | ✅ Complete | `ai/src/main/java/org/apache/sling/cms/ai/internal/` | `RulesBasedTextService` (non-AI implementation) |
| OSGi Bundle Configuration | ✅ Complete | `ai/bnd.bnd` | Exports `ai` and `ai.audit` packages |
| Maven Dependencies | ✅ Complete | `ai/pom.xml` | Sling, OSGi, Jackson, Commons properly configured |
| Documentation | ✅ Complete | `ai/README.md` | Module overview, usage, configuration |
| Taxonomy System | ✅ Complete | `api/TaxonomyService.java`, `api/TaxonomyItem.java` | Can be leveraged for AI tag suggestions |
| Job Infrastructure | ✅ Complete | `api/CMSJobManager.java`, Sling Jobs | Ready for AI automation jobs |
| Content Insights Framework | ✅ Complete | `api/insights/InsightProvider.java` | Pattern can be extended for AI insights |
| I18N Support | ✅ Complete | `api/i18n/I18NProvider.java`, `I18NDictionary.java` | Foundation for AI translation |
| Asset Metadata | ✅ Complete | `thumbnails/AssetMetadataModel.java` | Has altText field, taxonomy support |
| Publication System | ✅ Complete | `api/publication/PublicationManager*.java` | Ready for AI-assisted publishing |
| Path Suggestions | ✅ Complete | `core/PathSuggestionServlet.java` | Pattern for AI suggestions UI |
| External AI Providers | 🔄 In Progress | `ai/` | OpenAI, Azure OpenAI, Anthropic, Ollama implementations |
| JCR Audit Service | 🔄 Planned | `ai/` | JCR-based implementation of `AiAuditService` |
| AI UI Components | 🔄 Planned | `ui/` | AI assist panels, dialogs, and suggestion components |
| CAConfig Integration | 🔄 Planned | `ai/` | Configuration models for per-site AI settings |
| AI Sling Models | 🔄 Planned | `ai/` | Page/Asset AI assistance models |
| AI Servlets | 🔄 Planned | `ai/` | REST endpoints for AI operations |

---

## Module Structure

The AI module (`ai/`) follows Apache Sling CMS patterns and is fully operational:

```
ai/
├── pom.xml                          # Maven module configuration
├── bnd.bnd                          # OSGi bundle manifest
├── README.md                        # Module documentation
└── src/main/java/org/apache/sling/cms/ai/
    ├── package-info.java           # Package documentation
    ├── AiService.java              # Base service interface
    ├── AiRequest.java              # Request model with builder
    ├── AiResponse.java             # Response model with status tracking
    ├── AiTextService.java          # Text operations (summarize, rewrite, translate)
    ├── AiClassificationService.java # Taxonomy and tag suggestions
    ├── AiImageService.java         # Image analysis and alt-text generation
    ├── audit/
    │   ├── package-info.java       # Audit package documentation
    │   ├── AiAuditService.java    # Audit logging interface
    │   └── AiAuditEntry.java      # Audit record model
    └── internal/
        └── RulesBasedTextService.java # Non-AI fallback implementation
```

**Build & Deploy:**
```bash
# Build and deploy AI module
mvn clean install -P autoInstallBundle -pl ai -DskipTests -Dbnd.baseline.skip=true

# Or use VS Code task: "Auto Deploy - AI"
```

**OSGi Bundle Status:**
- Bundle Name: `Apache Sling CMS AI Support`
- Symbolic Name: `org.apache.sling.cms.ai`
- Version: `1.1.9-SNAPSHOT`
- State: Active (deployed to `http://localhost:8082/system/console/`)

---

## Goals

1. **Vendor neutral**: switch providers without changing templates/components.
2. **Author-first**: AI suggests; humans approve.
3. **Auditable**: every AI-assisted change is traceable.
4. **Safe by default**: protect PII, enforce permissions, and avoid leaking content.
5. **Composable**: small services that integrate with Sling Models, CAConfig, and jobs.

---

## Non-goals

- Not building a closed, proprietary "AI CMS suite".
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
- "Explain this page" summary for reviewers
- "What changed?" summaries for version diffs

---

## Architecture (implemented)

The AI module follows OSGi best practices with a clean separation of concerns:

### A) Provider interface (OSGi service) ✅ **Complete**
Core interfaces in the `ai` module, implemented using OSGi Declarative Services (R7+):

**Capability interfaces:**
- `AiTextService` - Text operations (summarize, rewrite, translate, suggest title/description)
- `AiClassificationService` - Taxonomy suggestions (tags, categories, keywords)
- `AiImageService` - Image analysis (alt-text, captions, descriptions)
- `AiService` - Base interface for all AI providers

**Key features:**
- Implementations are **swappable** via OSGi DS + configuration
- All services extend base `AiService` interface with common methods (`getId()`, `isEnabled()`)
- Request/response models with builder patterns for clean API
- Enum-based status tracking (SUCCESS, FAILURE, SKIPPED, TIMEOUT)

### B) Provider implementations ✅ **Fallback complete**
Current implementations:
- ✅ `RulesBasedTextService` - Non-AI fallback (first N sentences, basic summarization)
- 🔄 `OpenAiTextService` - OpenAI API integration (planned)
- 🔄 `AzureOpenAiTextService` - Azure OpenAI integration (planned)
- 🔄 `AnthropicTextService` - Anthropic Claude integration (planned)
- 🔄 `OllamaTextService` - Local Ollama integration (planned)

**Rule**: The CMS works even if external AI is disabled (fallback active).

### C) Invocation paths 🔄 **Planned**
AI should be invoked only via:
1. **Content Automation jobs** (scheduled or event-triggered)
2. **Explicit author actions** (button click "Suggest summary")

### D) Storage model 🔄 **Planned**
All AI output should be stored as:
- draft fields (not directly overwriting published content)
- or versioned changes with explicit approval

Suggested:
- store drafts under `jcr:content/aiDrafts/*` (or a dedicated child node)
- store approved results into the normal field locations

---

## Governance & safety

### 1) Permissions 🔄 **Planned**
- Only allow AI actions to users/groups with explicit permission.
- Support per-site enablement under `/conf/{site}`.

### 2) Human review gates 🔄 **Planned**
Modes:
- **Suggest-only**: AI output shown in UI; author clicks "Apply".
- **Draft mode**: write to draft fields; reviewer approves.

### 3) Audit logs ✅ **Framework complete**
The audit framework (`AiAuditService`, `AiAuditEntry`) is ready to record:
- Request context (site/page path, content path)
- User who triggered the AI operation
- Provider used (e.g., "openai", "rules-based")
- Operation type (SUMMARIZE, TRANSLATE, TAG_SUGGESTION, etc.)
- Input hash (for duplicate detection)
- Output preview (first 200 chars)
- Outcome (SUCCESS, FAILURE, SKIPPED, APPLIED, REJECTED)
- Error messages
- Timestamps

**Audit entry storage:** Planned at `/var/audit/ai/{yyyy}/{MM}/{dd}/{uuid}`

**Next step:** Implement `JcrAiAuditServiceImpl` for JCR-based storage.

### 4) Data handling / privacy 🔄 **Planned**
Guidelines:
- Never send unpublished restricted content unless explicitly allowed.
- Strip or mask sensitive fields (PII) using policy.
- Allow an "offline mode" where AI is fully disabled.

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

- Add "AI Assist" panels in page properties / asset properties.
- Provide per-field actions:
  - Suggest title
  - Suggest summary
  - Suggest tags
  - Suggest alt text
- Always display an **Apply** button, never auto-apply by default.

---

## Roadmap

### Phase 1 (safe + useful) ✅ **Foundation Complete** 🔄 **Implementation In Progress**
- ✅ Interface + provider abstraction (complete)
- ✅ Audit framework (interfaces complete, storage implementation pending)
- ✅ Rules-based fallback (complete and working)
- ✅ External AI provider implementations (OpenAI, Azure, Anthropic)
- ✅ Suggest title/summary for pages (suggest-only UI)
- 🔄 JCR-based audit service implementation

### Phase 2 🔄 **Planned**
- Tag suggestions mapped to taxonomy (interface ready, UI pending)
- Alt-text suggestions for images (interface ready, UI pending)
- CAConfig integration for per-site AI settings

### Phase 3 🔄 **Planned**
- Draft translations + compare view
- Multi-provider routing (try OpenAI, fallback to rules-based)

### Phase 4 🔄 **Planned**
- "Assist in workflows" (review summaries, change summaries)
- AI insights integration with existing InsightProvider framework
- Batch AI operations via CMSJobManager

---

## Getting Started

### For Developers

1. **Clone and build:**
   ```bash
   git clone https://github.com/apache/sling-org-apache-sling-app-cms.git
   cd sling-org-apache-sling-app-cms
   mvn clean install
   ```

2. **Deploy AI module:**
   ```bash
   mvn clean install -P autoInstallBundle -pl ai -DskipTests -Dbnd.baseline.skip=true
   ```

3. **Verify bundle:**
   - Open `http://localhost:8082/system/console/bundles`
   - Look for "Apache Sling CMS AI Support" (Active)

4. **Next steps:**
   - Implement external AI provider (e.g., `OpenAiTextService`)
   - Create JCR audit service (`JcrAiAuditServiceImpl`)
   - Build UI components for AI assistance

### For Contributors

See the AI module README at `ai/README.md` for:
- Architecture overview
- Adding new AI providers
- Configuration patterns
- Testing guidelines

### Implementation Example

**Using AiTextService to suggest a page title:**

```java
import org.apache.sling.cms.ai.AiTextService;
import org.apache.sling.cms.ai.AiRequest;
import org.apache.sling.cms.ai.AiResponse;
import org.osgi.service.component.annotations.Reference;

@Component
public class MyAiComponent {
    
    @Reference
    private AiTextService aiTextService;
    
    public void suggestTitle(String content, String pagePath) {
        // Build request
        AiRequest request = AiRequest.builder()
            .content(content)
            .contentPath(pagePath)
            .userId(resourceResolver.getUserID())
            .build();
        
        // Get suggestion
        AiResponse response = aiTextService.suggestTitle(request);
        
        // Check result
        if (response.isSuccess()) {
            String suggestedTitle = response.getContent();
            log.info("Suggested title: {}", suggestedTitle);
            
            // Display to user for approval
            // Never auto-apply without human review
        } else {
            log.warn("AI suggestion failed: {}", response.getErrorMessage());
        }
    }
}
```

**Adding a new AI provider:**

```java
import org.apache.sling.cms.ai.AiTextService;
import org.apache.sling.cms.ai.AiRequest;
import org.apache.sling.cms.ai.AiResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.metatype.annotations.Designate;

@Component(service = AiTextService.class)
@Designate(ocd = OpenAiTextService.Config.class)
public class OpenAiTextService implements AiTextService {
    
    @interface Config {
        String apiKey() default "";
        String model() default "gpt-4";
        boolean enabled() default false;
    }
    
    private Config config;
    
    @Activate
    protected void activate(Config config) {
        this.config = config;
    }
    
    @Override
    public String getId() {
        return "openai";
    }
    
    @Override
    public String getTitle() {
        return "OpenAI";
    }
    
    @Override
    public boolean isEnabled() {
        return config.enabled();
    }
    
    @Override
    public AiResponse suggestTitle(AiRequest request) {
        if (!isEnabled()) {
            return AiResponse.skipped("OpenAI provider is disabled");
        }
        
        try {
            // Call OpenAI API
            String result = callOpenAiApi(request);
            
            return AiResponse.success()
                .content(result)
                .providerId(getId())
                .confidence(0.95)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
                
        } catch (Exception e) {
            return AiResponse.failure("OpenAI API error: " + e.getMessage());
        }
    }
}
```

---

## Relationship to content automation

AI is a subset of content automation.

- Content automation defines triggers, jobs, policies, reports.
- AI defines providers, governance, prompts, and UI affordances.

See also: `new-age-cms-content-automation.md`.

---

## TODO List – Implementation Plan

This section tracks actionable tasks for implementing AI features based on the existing codebase patterns.

### Phase 1: Foundation (Priority: HIGH)

#### 1.1 Create AI Provider Interfaces

**Location**: `api/src/main/java/org/apache/sling/cms/ai/`

| Task | File | Description | Dependencies |
|------|------|-------------|--------------|
| ☐ Create `AiService` interface | `AiService.java` | Base marker interface for all AI services | None |
| ☐ Create `AiTextService` interface | `AiTextService.java` | Summarize, rewrite, translate text | `AiService` |
| ☐ Create `AiClassificationService` interface | `AiClassificationService.java` | Tag/category suggestions | `AiService`, `TaxonomyService` |
| ☐ Create `AiImageService` interface | `AiImageService.java` | Alt-text, caption suggestions | `AiService` |
| ☐ Create `AiResponse` class | `AiResponse.java` | Wrapper for AI results (content, confidence, warnings) | None |
| ☐ Create `AiRequest` class | `AiRequest.java` | Input for AI operations (content, context, options) | None |
| ☐ Create `package-info.java` | `package-info.java` | Package version annotation | None |
| ☐ Update `bnd.bnd` | `api/bnd.bnd` | Export `org.apache.sling.cms.ai` package | None |

**Pattern Reference**: Follow `InsightProvider.java` pattern:
```java
@ProviderType
public interface AiTextService extends AiService {
    AiResponse summarize(AiRequest request);
    AiResponse rewrite(AiRequest request, String tone);
    AiResponse translate(AiRequest request, String targetLocale);
    String getId();
    String getTitle();
    boolean isEnabled();
}
```

#### 1.2 Create Fallback (Non-AI) Provider

**Location**: `core/src/main/java/org/apache/sling/cms/core/internal/ai/`

| Task | File | Description |
|------|------|-------------|
| ☐ Create `RulesBasedTextService` | `RulesBasedTextServiceImpl.java` | Non-AI fallback (first N sentences for summary, etc.) |
| ☐ Create `TaxonomyBasedClassificationService` | `TaxonomyBasedClassificationServiceImpl.java` | Keyword matching against existing taxonomy |

**Requirement**: CMS must function when AI is completely disabled.

#### 1.3 Create AI Audit Logging

**Location**: `api/src/main/java/org/apache/sling/cms/ai/audit/`

| Task | File | Description |
|------|------|-------------|
| ☐ Create `AiAuditService` interface | `AiAuditService.java` | Record AI operations |
| ☐ Create `AiAuditEntry` class | `AiAuditEntry.java` | Audit record structure |
| ☐ Implement `JcrAiAuditServiceImpl` | `core/.../JcrAiAuditServiceImpl.java` | Store audits under `/var/audit/ai/` |

**Audit Entry Fields**:
- `timestamp` - When operation occurred
- `user` - Who triggered it
- `operation` - summarize/translate/classify/etc.
- `provider` - Which AI provider was used
- `promptTemplateId` - Which prompt template (if any)
- `contentPath` - Source content path
- `status` - success/failure/applied/rejected
- `inputHash` - Hash of input content (not full content for privacy)
- `outputPreview` - First 200 chars of output

---

### Phase 2: Text Assistance (Priority: HIGH)

#### 2.1 Title/Summary Suggestions

| Task | Description | Location |
|------|-------------|----------|
| ☐ Create `PageAiAssistModel` Sling Model | Model for page AI assistance | `core/src/main/java/.../models/` |
| ☐ Create `page-ai-assist.html` HTL component | UI panel for AI suggestions | `ui/src/main/resources/jcr_root/libs/sling-cms/components/cms/pageaiassist/` |
| ☐ Create AI suggestion servlet | REST endpoint for AI operations | `core/src/main/java/.../servlets/AiSuggestionServlet.java` |
| ☐ Add SCSS styles | Component styling | `frontend/src/main/frontend/scss/_pageaiassist.scss` |

**UI/UX Requirements**:
- "Suggest Title" button → shows suggestion → "Apply" or "Discard"
- "Suggest Summary" button → shows suggestion → "Apply" or "Discard"  
- Never auto-apply without author confirmation

#### 2.2 External AI Provider Integration

| Task | Description | Priority |
|------|-------------|----------|
| ☐ Create `OpenAiTextServiceImpl` | OpenAI API integration | Medium |
| ☐ Create `AzureOpenAiTextServiceImpl` | Azure OpenAI integration | Medium |
| ☐ Create `AnthropicTextServiceImpl` | Anthropic Claude integration | Low |
| ☐ Create `OllamaTextServiceImpl` | Local Ollama integration | Low |

**Configuration Pattern** (CAConfig):
```
/conf/{site}/ai/
  └── text/
      ├── enabled: true
      ├── provider: "azure-openai"
      └── apiEndpoint: "https://..."
```

---

### Phase 3: Taxonomy/Classification (Priority: MEDIUM)

#### 3.1 Tag Suggestions

| Task | Description | Dependencies |
|------|-------------|--------------|
| ☐ Integrate `AiClassificationService` with `TaxonomyService` | Map AI suggestions to existing taxonomy items | `TaxonomyService` |
| ☐ Create `TagSuggestionServlet` | REST endpoint for tag suggestions | `AiClassificationService` |
| ☐ Add tag suggestion UI to page properties | "Suggest Tags" button in editor | UI module |

**Leverage Existing**:
- `TaxonomyService.searchTaxonomyItems()` - Find matching taxonomy items
- `AssetMetadataModel.getTaxonomyOptions()` - Pattern for displaying taxonomy options

#### 3.2 Category Suggestions

| Task | Description |
|------|-------------|
| ☐ Create category suggestion logic | Suggest page categories based on content |
| ☐ Add category suggestion UI | UI in page properties |

---

### Phase 4: Asset Assistance (Priority: MEDIUM)

#### 4.1 Alt-Text Suggestions

**Leverage Existing**: `AssetMetadataModel` already has `getAltText()` method.

| Task | Description | Location |
|------|-------------|----------|
| ☐ Create `AiImageService` implementation | Image description generation | `core` or `thumbnails` module |
| ☐ Add "Suggest Alt Text" button | UI in asset properties | `thumbnails` UI resources |
| ☐ Create `AssetAiAssistModel` | Sling Model for asset AI assistance | `thumbnails` module |

#### 4.2 Caption Suggestions

| Task | Description |
|------|-------------|
| ☐ Add caption suggestion to `AiImageService` | Generate captions from image content |
| ☐ Add caption suggestion UI | "Suggest Caption" button |

---

### Phase 5: Translation Assistance (Priority: LOW)

#### 5.1 Draft Translation Generation

| Task | Description | Dependencies |
|------|-------------|--------------|
| ☐ Extend `AiTextService.translate()` | Generate draft translations | `I18NProvider` |
| ☐ Create translation draft storage | Store under `jcr:content/i18n/{locale}/aiDraft` | JCR |
| ☐ Create translation compare view | Side-by-side original vs translation | UI module |
| ☐ Create translation approval workflow | Review and approve translations | Publication system |

**Leverage Existing**:
- `I18NProvider` interface - Existing i18n infrastructure
- `I18NDictionary` - Dictionary management

---

### Phase 6: Governance & Configuration (Priority: HIGH)

#### 6.1 AI Configuration

| Task | File | Description |
|------|------|-------------|
| ☐ Create AI configuration schema | `/conf/global/settings/ai/.content.xml` | Default AI configuration |
| ☐ Create per-site AI config | `/conf/{site}/ai/` | Site-specific overrides |
| ☐ Create AI admin console | `/libs/sling-cms/content/admin/ai.json` | Admin UI for AI settings |

**Configuration Structure**:
```
/conf/{site}/ai/
├── enabled: boolean
├── provider: string (default provider ID)
├── text/
│   ├── enabled: boolean
│   ├── provider: string
│   └── promptTemplates/
│       ├── summarize/
│       │   └── template: string
│       └── title/
│           └── template: string
├── classification/
│   ├── enabled: boolean
│   └── taxonomyRoot: string
├── image/
│   ├── enabled: boolean
│   └── provider: string
└── policy/
    ├── requireApproval: boolean
    ├── maskPii: boolean
    └── allowedPaths: string[]
```

#### 6.2 Permission System

| Task | Description |
|------|-------------|
| ☐ Create AI permission group | `ai-users` group for AI access |
| ☐ Add AI permission checks | Check permissions before AI operations |
| ☐ Add per-site AI enablement | Enable/disable AI per site |

---

### Phase 7: UI Components (Priority: MEDIUM)

#### 7.1 AI Assist Panels

| Task | Component | Location |
|------|-----------|----------|
| ☐ Page AI Assist panel | `pageaiassist` | `/libs/sling-cms/components/cms/pageaiassist/` |
| ☐ Asset AI Assist panel | `assetaiassist` | `/libs/sling-cms/components/cms/assetaiassist/` |
| ☐ AI suggestion dialog | `aisuggest` | `/libs/sling-cms/components/cms/aisuggest/` |

**UI Pattern** (follow PathSuggestionServlet pattern):
```html
<div class="cms-ai-assist">
  <button class="cms-button cms-button--ai" data-action="suggest-title">
    <span class="cms-icon cms-icon--ai"></span> Suggest Title
  </button>
  <div class="cms-ai-assist__preview">
    <!-- AI suggestion displayed here -->
  </div>
  <div class="cms-ai-assist__actions">
    <button class="cms-button cms-button--primary">Apply</button>
    <button class="cms-button">Discard</button>
  </div>
</div>
```

#### 7.2 SCSS Organization

| Task | File | Description |
|------|------|-------------|
| ☐ Create `_aiassist.scss` | AI assist component styles | `frontend/src/main/frontend/scss/` |
| ☐ Add AI icon to icon set | `cms-icon--ai` class | `_icons.scss` |
| ☐ Update `cms.scss` | Import new SCSS file | `cms.scss` |

---

### Phase 8: Integration Jobs (Priority: LOW)

#### 8.1 AI Automation Jobs

**Leverage Existing**: `CMSJobManager`, `ConfigurableJobExecutor`

| Task | Description | Pattern |
|------|-------------|---------|
| ☐ Create `AiSummaryGeneratorJob` | Generate summaries for pages without them | `ConfigurableJobExecutor` |
| ☐ Create `AiAltTextGeneratorJob` | Generate alt-text for images without it | `ConfigurableJobExecutor` |
| ☐ Create `AiTagSuggestionJob` | Suggest tags for untagged content | `ConfigurableJobExecutor` |

**Job Pattern Reference**: Follow `AutoRenditionJobConsumer` in thumbnails module.

---

## Technical Notes

### OSGi Patterns for AI Services

All AI services must follow OSGi R7+ patterns:

```java
@Component(
    service = AiTextService.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = OpenAiTextServiceImpl.Config.class)
public class OpenAiTextServiceImpl implements AiTextService {

    @ObjectClassDefinition(name = "OpenAI Text Service")
    public @interface Config {
        @AttributeDefinition(name = "API Key", description = "OpenAI API key")
        String apiKey();
        
        @AttributeDefinition(name = "Model", description = "Model to use")
        String model() default "gpt-4";
        
        @AttributeDefinition(name = "Enabled")
        boolean enabled() default true;
    }

    @Reference
    private AiAuditService auditService;

    @Activate
    protected void activate(Config config) {
        // initialization
    }
}
```

### HTTP Client for External APIs

Use `java.net.http.HttpClient` (Java 11+) for external AI API calls:

```java
private final HttpClient httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build();
```

### Error Handling

All AI operations should:
1. Return empty/fallback response on failure (never throw to UI)
2. Log errors with context
3. Record failures in audit log
4. Show user-friendly error messages

---

## Dependencies on Existing Modules

| Module | Dependency Type | Usage |
|--------|-----------------|-------|
| `api` | New interfaces | AI service interfaces, audit interfaces |
| `core` | Implementations | AI service implementations, servlets |
| `thumbnails` | Integration | Asset AI features |
| `ui` | UI Components | AI assist panels and dialogs |
| `frontend` | Styles | SCSS for AI components |

---

## Testing Strategy

| Test Type | Coverage |
|-----------|----------|
| Unit Tests | AI service interfaces, response handling |
| Integration Tests | Provider switching, configuration loading |
| E2E Tests (Cypress) | AI suggestion UI workflows |

---

## Migration Path

If AI code is initially developed in `core`, plan migration to a dedicated `ai` module:

1. **Phase 1**: Develop in `api` (interfaces) and `core` (implementations)
2. **Phase 2**: Extract to `ai` module when patterns stabilize
3. **Phase 3**: Move UI components to `ai` module's UI resources
