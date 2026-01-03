# AI Module Package Organization

This document describes the package structure and organization of the AI module.

## Package Structure

```
org.apache.sling.cms.ai/
├── [root] - Public API interfaces and core classes
│   ├── AiService.java - Base service interface
│   ├── AiRequest.java - Request model
│   ├── AiResponse.java - Response model
│   ├── AiTextService.java - Text generation/manipulation API
│   ├── AiImageService.java - Image analysis API
│   ├── AiClassificationService.java - Classification API
│   ├── AiTaxonomyService.java - Taxonomy suggestion API
│   └── package-info.java - Package documentation
│
├── audit/ - Audit-specific API
│   ├── AiAuditService.java - Audit service interface
│   ├── AiAuditEntry.java - Audit entry model
│   └── package-info.java - Package documentation
│
├── internal/ - OSGi service implementations and internal POJOs
│   ├── JcrAiAuditServiceImpl.java - JCR-based audit service
│   ├── AiTaxonomyServiceImpl.java - Taxonomy service implementation
│   ├── TaxonomyAnalysisResultImpl.java - Result POJO
│   ├── TaxonomySuggestionImpl.java - Suggestion POJO
│   ├── OpenAiTextService.java - OpenAI provider
│   ├── AzureOpenAiTextService.java - Azure OpenAI provider
│   ├── AnthropicTextService.java - Anthropic provider
│   ├── OllamaTextService.java - Ollama provider
│   └── RulesBasedTextService.java - Fallback provider
│
└── servlets/ - HTTP servlets for AI operations
    └── AiSuggestionServlet.java - Suggestion servlet
```

## Package Conventions

### `org.apache.sling.cms.ai` (Root Package)
**Purpose:** Public API interfaces and core classes

**Contains:**
- Service interfaces (annotated with `@ProviderType`)
- Request/Response models
- Common enums and constants
- Package documentation

**Visibility:** PUBLIC - These classes are exported and form the API contract

**Rules:**
- All interfaces must be annotated with `@ProviderType`
- No implementation details
- Stable API - changes must maintain backward compatibility
- Well-documented with Javadoc

### `org.apache.sling.cms.ai.audit`
**Purpose:** Audit-specific API

**Contains:**
- Audit service interface
- Audit entry models
- Audit-related enums

**Visibility:** PUBLIC - Exported as part of the API

**Rules:**
- Same as root package
- Focused on audit functionality
- Clean separation from other features

### `org.apache.sling.cms.ai.internal`
**Purpose:** OSGi service implementations and internal POJOs

**Contains:**
- OSGi Component implementations (annotated with `@Component`)
- Internal POJO implementations (Builder pattern)
- Provider-specific implementations
- Helper classes

**Visibility:** PRIVATE - Not exported, internal only

**Rules:**
- All OSGi services must be annotated with `@Component(service = XxxService.class)`
- Implementation classes should end with `Impl` suffix
- No public API guarantees - can change freely
- POJOs use Builder pattern for immutability

### `org.apache.sling.cms.ai.servlets`
**Purpose:** HTTP servlets for AI operations

**Contains:**
- Sling servlets (annotated with `@SlingServlet`)
- Request handlers
- Response writers

**Visibility:** PRIVATE - Not exported

**Rules:**
- All servlets must be annotated with `@SlingServlet`
- Use ResourceTypes for binding
- Proper error handling and response codes
- Input validation

## Design Principles

1. **API/Implementation Separation**
   - Public interfaces in root/audit packages
   - Implementations in internal package
   - Clear boundary between API and implementation

2. **OSGi Best Practices**
   - Declarative Services (R7+)
   - Service ranking for multiple providers
   - Dynamic service binding
   - Proper service lifecycle

3. **Immutability**
   - Request/Response objects are immutable
   - Builder pattern for complex objects
   - Thread-safe implementations

4. **Single Responsibility**
   - Each package has a focused purpose
   - Clear separation of concerns
   - Cohesive modules

5. **Dependency Management**
   - Minimal external dependencies
   - Jackson for JSON (already in project)
   - Sling/JCR APIs for core functionality

## File Naming Conventions

- **Interfaces:** `AiXxxService.java`
- **Implementations:** `XxxServiceImpl.java` (in internal package)
- **Models:** `AiXxx.java` (request/response objects)
- **POJOs:** `XxxImpl.java` (builder pattern implementations)
- **Servlets:** `XxxServlet.java`

## Import Organization

Classes in the same package don't need to import each other. This is why `AiTaxonomyServiceImpl` doesn't import `TaxonomyAnalysisResultImpl` and `TaxonomySuggestionImpl` - they're all in the `internal` package.

## Testing Organization

```
test/java/org/apache/sling/cms/ai/
├── internal/ - Implementation tests
│   └── JcrAiAuditServiceImplTest.java
└── [future test packages]
```

Test packages mirror the main source packages.

## Configuration

Service configurations are managed through:
- OSGi Configuration Admin
- Sling Feature Model (`feature/src/main/features/ai.json`)
- Repoinit scripts (`feature/src/main/features/ai-repoinit.txt`)

## Summary

This organization follows Apache Sling CMS conventions:
- ✅ Clear API/implementation separation
- ✅ Proper OSGi packaging
- ✅ Focused, cohesive packages
- ✅ Consistent naming conventions
- ✅ Internal implementation details hidden
- ✅ Public API stable and well-documented
