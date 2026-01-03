# Apache Sling CMS AI Support

AI integration services for Apache Sling CMS - vendor-neutral, governed, and composable AI assistance.

## Overview

This module provides AI capabilities for the Sling CMS, designed with the following principles:

- **Vendor-neutral**: Switch AI providers without changing templates or components
- **Author-first**: AI suggests; humans approve
- **Auditable**: Every AI-assisted change is traceable
- **Safe by default**: Protect PII, enforce permissions, avoid leaking content
- **Composable**: Small services that integrate with Sling Models, CAConfig, and jobs

## Features

### Text Services (`AiTextService`)
- Content summarization (short/long)
- Title suggestions
- Meta description generation
- Tone rewriting (formal, informal, concise, etc.)
- Translation assistance
- Change summaries for workflows

### Classification Services (`AiClassificationService`)
- Tag suggestions mapped to existing taxonomy
- Category suggestions
- Content classification
- Keyword extraction

### Image Services (`AiImageService`)
- Alt-text generation for accessibility
- Caption generation
- Image description

### Audit Services (`AiAuditService`)
- Comprehensive audit logging
- Operation tracking by user, content, and time
- Compliance and governance support

## Included Implementations

### Rules-Based Fallback
The module includes a `RulesBasedTextService` that provides basic functionality without requiring an external AI API:
- Extracts first N sentences for summaries
- Uses first sentence for title suggestions
- Character-based truncation for meta descriptions

This ensures the CMS works even when AI providers are disabled or unavailable.

## Configuration

AI services can be configured per-site under `/conf/{site}/ai/`:

```
/conf/{site}/ai/
├── enabled: boolean
├── provider: string (default provider ID)
├── text/
│   ├── enabled: boolean
│   └── provider: string
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

## Adding External AI Providers

To add support for external AI providers (OpenAI, Azure OpenAI, Anthropic, etc.):

1. Create a new implementation of the appropriate service interface
2. Use OSGi R7+ `@Component` annotation with `configurationPolicy = ConfigurationPolicy.REQUIRE`
3. Use `@Designate` for configuration
4. Inject `AiAuditService` for logging
5. Set `service.ranking` higher than the rules-based fallback (0)

Example:
```java
@Component(
    service = AiTextService.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = OpenAiTextService.Config.class)
public class OpenAiTextService implements AiTextService {
    // Implementation
}
```

## Usage

### In Sling Models

```java
@Model(adaptables = Resource.class)
public class PageAiAssistModel {

    @OSGiService
    private AiTextService textService;

    @OSGiService
    private AiAuditService auditService;

    public AiResponse suggestTitle() {
        AiRequest request = AiRequest.builder()
            .content(getPageContent())
            .contentPath(resource.getPath())
            .userId(getCurrentUserId())
            .build();

        return textService.suggestTitle(request);
    }
}
```

### In Servlets

```java
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "sling-cms/components/cms/page",
    selectors = "ai-suggest",
    extensions = "json"
)
public class AiSuggestionServlet extends SlingAllMethodsServlet {

    @Reference
    private AiTextService textService;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        // Handle AI suggestion request
    }
}
```

## Documentation

For detailed design and roadmap, see:
- [AI Integration Design](/docs/new-age-cms-ai.md)
- [Content Automation](/docs/new-age-cms-content-automation.md)

## Building

```bash
# Build this module
mvn clean install -pl ai

# Deploy to running instance
mvn clean install -P autoInstallBundle -pl ai -DskipTests -Dbnd.baseline.skip=true
```

## License

Licensed under the Apache License, Version 2.0.
