# AI Taxonomy Service Validation Guide

This document provides validation steps for the AI Taxonomy and Classification feature.

## Feature Overview

The AI Taxonomy Service (`AiTaxonomyService`) provides:
1. **Tag suggestions** mapped to existing taxonomy in `/etc/taxonomy`
2. **Category suggestions** for content classification
3. **Confidence scoring** to filter low-quality suggestions
4. **Reason explanations** for each suggestion

## Implementation Status

✅ **Complete** - Implementation ready for validation

| Component | Status | Location |
|-----------|--------|----------|
| Interface | ✅ Complete | `ai/src/main/java/org/apache/sling/cms/ai/AiTaxonomyService.java` |
| Implementation | ✅ Complete | `ai/src/main/java/org/apache/sling/cms/ai/internal/AiTaxonomyServiceImpl.java` |
| Result POJO | ✅ Complete | `ai/src/main/java/org/apache/sling/cms/ai/internal/TaxonomyAnalysisResultImpl.java` |
| Suggestion POJO | ✅ Complete | `ai/src/main/java/org/apache/sling/cms/ai/internal/TaxonomySuggestionImpl.java` |

## Architecture

### 1. Tag Suggestion Flow

```
Content (String)
    ↓
AiTaxonomyService.suggestTags()
    ↓
1. Query existing taxonomy from /etc/taxonomy (via TaxonomyService)
2. Build AI prompt with content + available tags
3. Call AiTextService.explain(request)
4. Parse JSON response
5. Filter by minConfidence (default 0.5)
6. Return ordered suggestions (max 5 by default)
    ↓
TaxonomyAnalysisResult (with suggestions list)
```

### 2. Category Suggestion Flow

```
Content (String)
    ↓
AiTaxonomyService.suggestCategories()
    ↓
Same as suggestTags, but:
- Uses specific category taxonomy path
- Default max 3 suggestions
- Higher confidence threshold (0.6)
    ↓
TaxonomyAnalysisResult (with category suggestions)
```

## Key Features

### 1. Taxonomy-Aware Mapping
- Reads existing taxonomy structure from JCR (`/etc/taxonomy`)
- Only suggests tags that exist in the CMS
- Preserves taxonomy hierarchy (parent/child relationships)

**Implementation:**
```java
private List<String> getAvailableTaxonomyTags(ResourceResolver resolver, String taxonomyPath) {
    // Traverses JCR tree recursively
    // Collects all jcr:title values
    // Builds hierarchical paths (e.g., "Technology/Java/Spring Boot")
}
```

### 2. Confidence Scoring
- Each suggestion has a confidence score (0.0 to 1.0)
- Configurable minimum threshold (default 0.5 for tags, 0.6 for categories)
- Suggestions sorted by confidence (highest first)

**Implementation:**
```java
if (confidence >= minConfidence && taxonomyTitle != null) {
    suggestions.add(TaxonomySuggestionImpl.builder()
        .confidence(confidence)
        // ...
        .build());
}
suggestions.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
```

### 3. AI Provider Integration
- Uses `AiTextService.explain()` method for analysis
- Supports all configured AI providers (OpenAI, Azure, Anthropic, Ollama)
- Dynamic service binding (works with multiple providers)
- Graceful fallback if no provider available

**Implementation:**
```java
@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
private volatile List<AiTextService> textServices;

private AiTextService getPreferredTextService() {
    List<AiTextService> services = getAvailableTextServices();
    return services.isEmpty() ? null : services.get(0);
}
```

### 4. JSON Response Parsing
- Uses Jackson ObjectMapper for robust JSON parsing
- Extracts JSON array from AI response text
- Handles malformed responses gracefully

**Expected JSON format:**
```json
[
  {
    "taxonomyPath": "/etc/taxonomy/topics/java",
    "taxonomyTitle": "Java",
    "confidence": 0.95,
    "reason": "Content discusses Java programming concepts"
  },
  {
    "taxonomyPath": "/etc/taxonomy/topics/spring",
    "taxonomyTitle": "Spring Framework",
    "confidence": 0.87,
    "reason": "Multiple references to Spring Boot"
  }
]
```

## Validation Test Cases

### Test 1: Basic Tag Suggestion

**Setup:**
```java
// Create test taxonomy in /etc/taxonomy/topics
- Java
- Python
- JavaScript
- Spring Boot (under Java)
- Django (under Python)
```

**Test Code:**
```java
String content = "This article explains how to build REST APIs using Spring Boot and Java.";
ResourceResolver resolver = // get resolver
String taxonomyPath = "/etc/taxonomy/topics";

TaxonomyAnalysisResult result = taxonomyService.suggestTags(
    content, 
    resolver, 
    taxonomyPath,
    5,    // max suggestions
    0.5   // min confidence
);

List<TaxonomySuggestion> suggestions = result.getSuggestions();
```

**Expected Result:**
- 2-3 suggestions returned
- "Java" with high confidence (>0.8)
- "Spring Boot" with high confidence (>0.8)
- Suggestions ordered by confidence
- Each has taxonomyPath, taxonomyTitle, confidence, reason

**Validation:**
```java
assertFalse(suggestions.isEmpty());
assertTrue(suggestions.get(0).getConfidence() >= 0.5);
assertNotNull(suggestions.get(0).getTaxonomyPath());
assertNotNull(suggestions.get(0).getTaxonomyTitle());
assertNotNull(suggestions.get(0).getReason());
```

---

### Test 2: Category Suggestion

**Setup:**
```java
// Create category taxonomy in /etc/taxonomy/categories
- Products
  - Software
  - Hardware
- Services
  - Consulting
  - Support
```

**Test Code:**
```java
String content = "We offer software development and consulting services for enterprise clients.";
ResourceResolver resolver = // get resolver
String categoryPath = "/etc/taxonomy/categories";

TaxonomyAnalysisResult result = taxonomyService.suggestCategories(
    content, 
    resolver, 
    categoryPath
    // Uses defaults: max 3, confidence 0.6
);
```

**Expected Result:**
- 1-2 category suggestions
- "Services" or "Consulting" with high confidence
- Higher confidence threshold (0.6) applied
- Max 3 suggestions

---

### Test 3: Confidence Filtering

**Test Code:**
```java
String content = "Brief mention of Java.";

// Test with low threshold
TaxonomyAnalysisResult lowThreshold = taxonomyService.suggestTags(
    content, resolver, taxonomyPath, 5, 0.3
);

// Test with high threshold
TaxonomyAnalysisResult highThreshold = taxonomyService.suggestTags(
    content, resolver, taxonomyPath, 5, 0.8
);
```

**Expected Result:**
- `lowThreshold` has more suggestions (includes medium confidence)
- `highThreshold` has fewer suggestions (only high confidence)
- All suggestions in each result meet their respective thresholds

---

### Test 4: Empty Content Handling

**Test Code:**
```java
TaxonomyAnalysisResult emptyContent = taxonomyService.suggestTags(
    "", resolver, taxonomyPath
);

TaxonomyAnalysisResult nullContent = taxonomyService.suggestTags(
    null, resolver, taxonomyPath
);
```

**Expected Result:**
- Both return empty results (no exceptions)
- `getSuggestions()` returns empty list
- Graceful degradation

---

### Test 5: No Taxonomy Available

**Test Code:**
```java
String invalidPath = "/etc/taxonomy/nonexistent";

TaxonomyAnalysisResult result = taxonomyService.suggestTags(
    content, resolver, invalidPath
);
```

**Expected Result:**
- Returns empty result (no exceptions)
- Logs appropriate warning
- No AI call made if no taxonomy found

---

### Test 6: Multiple Providers

**Test Code:**
```java
// With OpenAI enabled
TaxonomyAnalysisResult openaiResult = taxonomyService.suggestTags(
    content, resolver, taxonomyPath
);

// Disable OpenAI, enable Ollama
TaxonomyAnalysisResult ollamaResult = taxonomyService.suggestTags(
    content, resolver, taxonomyPath
);
```

**Expected Result:**
- Both produce suggestions
- Different providers may give slightly different results
- Service automatically uses first available enabled provider

---

### Test 7: Max Suggestions Limit

**Test Code:**
```java
String richContent = "Article about Java, Spring, Hibernate, Maven, Gradle, JUnit, Docker, Kubernetes, and microservices.";

TaxonomyAnalysisResult result = taxonomyService.suggestTags(
    richContent, resolver, taxonomyPath, 3, 0.5
);
```

**Expected Result:**
- Exactly 3 suggestions returned (even if AI suggests more)
- Top 3 by confidence score
- Limit enforced by service

---

## Manual Testing via Groovy Console

### 1. Deploy the Service

```bash
cd /Users/phoolchandra/projects/sling-org-apache-sling-app-cms
mvn clean install -P autoInstallBundle -pl ai,feature -DskipTests
```

### 2. Verify Service is Active

Open: `http://localhost:8082/system/console/components`
Search for: `AiTaxonomyServiceImpl`
Status should be: **Active**

### 3. Create Test Taxonomy

Navigate to: `http://localhost:8082/cms/taxonomy/edit.html/etc/taxonomy`

Create structure:
```
/etc/taxonomy/
  topics/
    java (jcr:title="Java")
    python (jcr:title="Python")
    javascript (jcr:title="JavaScript")
```

### 4. Run Groovy Script

Open: `http://localhost:8082/system/console/groovyconsole`

```groovy
import org.apache.sling.cms.ai.AiTaxonomyService
import org.apache.sling.cms.ai.AiTaxonomyService.TaxonomyAnalysisResult
import org.apache.sling.cms.ai.AiTaxonomyService.TaxonomySuggestion

def taxonomyService = getService(AiTaxonomyService.class)

if (taxonomyService == null) {
    println "❌ AiTaxonomyService not found!"
    return
}

println "✅ Service found: ${taxonomyService.getId()}"
println "Enabled: ${taxonomyService.isEnabled()}"
println ""

def content = """
This comprehensive tutorial teaches you how to build modern web applications 
using Java and the Spring Boot framework. We'll cover REST APIs, database 
integration with Hibernate, and deployment with Docker.
"""

def taxonomyPath = "/etc/taxonomy/topics"
def resolver = resourceResolver

println "Analyzing content..."
println "Taxonomy path: ${taxonomyPath}"
println ""

TaxonomyAnalysisResult result = taxonomyService.suggestTags(
    content, 
    resolver, 
    taxonomyPath,
    5,    // max 5 tags
    0.5   // min confidence 0.5
)

println "Results:"
println "--------"
println "Total suggestions: ${result.suggestions.size()}"
println "Max requested: ${result.maxSuggestions}"
println "Min confidence: ${result.minConfidence}"
println ""

result.suggestions.each { suggestion ->
    println "Tag: ${suggestion.taxonomyTitle}"
    println "  Path: ${suggestion.taxonomyPath}"
    println "  Confidence: ${String.format('%.2f', suggestion.confidence)}"
    println "  Reason: ${suggestion.reason}"
    println ""
}

// Test categories
println "\n--- Testing Category Suggestions ---\n"

def categoryContent = "We provide enterprise software consulting and cloud migration services."

TaxonomyAnalysisResult categoryResult = taxonomyService.suggestCategories(
    categoryContent, 
    resolver, 
    "/etc/taxonomy/categories"
)

categoryResult.suggestions.each { suggestion ->
    println "Category: ${suggestion.taxonomyTitle}"
    println "  Confidence: ${String.format('%.2f', suggestion.confidence)}"
    println ""
}

println "✅ Validation complete!"
```

**Expected Console Output:**
```
✅ Service found: ai-taxonomy
Enabled: true

Analyzing content...
Taxonomy path: /etc/taxonomy/topics

Results:
--------
Total suggestions: 3
Max requested: 5
Min confidence: 0.5

Tag: Java
  Path: /etc/taxonomy/topics/java
  Confidence: 0.95
  Reason: Content extensively discusses Java programming

Tag: Spring Boot
  Path: /etc/taxonomy/topics/spring-boot
  Confidence: 0.92
  Reason: Multiple mentions of Spring Boot framework

Tag: Docker
  Path: /etc/taxonomy/topics/docker
  Confidence: 0.78
  Reason: Deployment discussion includes Docker

--- Testing Category Suggestions ---

Category: Services
  Confidence: 0.88

Category: Consulting
  Confidence: 0.82

✅ Validation complete!
```

---

## Integration Checklist

- [ ] Service deploys successfully to Sling instance
- [ ] Service shows as "Active" in Felix console
- [ ] At least one AiTextService provider is enabled (OpenAI, Azure, Anthropic, or Ollama)
- [ ] Taxonomy structure exists in `/etc/taxonomy`
- [ ] Tag suggestions return non-empty results for relevant content
- [ ] Category suggestions work with specific taxonomy paths
- [ ] Confidence filtering works correctly
- [ ] Suggestions are ordered by confidence (highest first)
- [ ] Each suggestion has all required fields (path, title, confidence, reason)
- [ ] Empty/null content handled gracefully
- [ ] Service works with different AI providers
- [ ] Max suggestions limit is enforced
- [ ] Min confidence threshold is respected

---

## Known Limitations

1. **Requires AI Provider:** Service needs at least one enabled `AiTextService`. Returns empty results if none available.

2. **Taxonomy Must Exist:** Only suggests tags that exist in the taxonomy structure. If taxonomy is empty, returns no suggestions.

3. **AI Response Format:** Depends on AI provider returning parseable JSON. Malformed responses are logged and return empty results.

4. **Service User Required:** Needs `sling-cms-ai` service user with read access to taxonomy paths. Configured via Feature Model.

5. **Performance:** Each call makes an external AI API request. Consider caching results for identical content.

---

## Troubleshooting

### No Suggestions Returned

**Check:**
1. Is service enabled? → `taxonomyService.isEnabled()` should return `true`
2. Is AI provider configured? → Check `/system/console/configMgr` for OpenAI/Azure/Anthropic
3. Does taxonomy exist? → Verify `/etc/taxonomy` has content
4. Is content too short? → Try with longer, more descriptive content
5. Is confidence threshold too high? → Try with `minConfidence = 0.3`

### Service Not Found

**Check:**
1. Bundle deployed? → Look for "Apache Sling CMS AI Support" in `/system/console/bundles`
2. Service active? → Check `/system/console/components` for `AiTaxonomyServiceImpl`
3. Dependencies resolved? → Verify `TaxonomyService` and `AiTextService` are available

### JSON Parse Errors

**Check:**
1. AI provider logs → `/system/console/slinglog` for parse errors
2. AI response format → May need to adjust prompt if provider returns unexpected format
3. Jackson library → Verify com.fasterxml.jackson is available

---

## Success Criteria

✅ **Phase 2 Feature is Complete when:**

1. Service deploys and activates successfully
2. Tag suggestions return relevant results for test content
3. Category suggestions work with specific taxonomy paths
4. Confidence filtering behaves correctly
5. All validation test cases pass
6. Manual Groovy Console test produces expected output
7. Integration checklist items are verified
8. Service works with at least 2 different AI providers
9. Documentation is complete and accurate
10. No compilation or runtime errors

---

## Next Steps After Validation

1. **UI Integration** - Create editor field component for tag suggestions
2. **Caching** - Add result caching to reduce API calls
3. **Batch Processing** - Support bulk taxonomy analysis via jobs
4. **Analytics** - Track suggestion acceptance rates
5. **Fine-tuning** - Adjust prompts based on real-world usage

---

**Document Status:** Ready for Validation Testing
**Last Updated:** January 3, 2026
**Maintainer:** Apache Sling CMS AI Team
