# Apache Sling CMS - Unit Testing Plan

## Overview

This document outlines the unit testing strategy for Apache Sling CMS, covering all modules with JUnit 5, Mockito, and Sling Mocks.

---

## Table of Contents

1. [Testing Stack](#testing-stack)
2. [Module Coverage](#module-coverage)
3. [Test Categories](#test-categories)
4. [Testing Patterns](#testing-patterns)
5. [Test Scenarios by Module](#test-scenarios-by-module)
6. [Mocking Strategies](#mocking-strategies)
7. [Coverage Goals](#coverage-goals)
8. [Best Practices](#best-practices)

---

## Testing Stack

| Component | Technology | Version | Purpose |
|-----------|------------|---------|---------|
| Test Framework | JUnit 5 (Jupiter) | 5.10.x | Test execution |
| Mocking | Mockito | 5.x | Mock objects |
| Sling Mocks | Sling Mock | 3.5.x | Sling API mocking |
| JCR Mocks | JCR Mock | 1.6.x | JCR repository mocking |
| OSGi Mocks | OSGi Mock | 3.4.x | OSGi container mocking |
| Assertions | AssertJ | 3.24.x | Fluent assertions |
| Coverage | JaCoCo | 0.8.x | Code coverage |

---

## Module Coverage

### Current Test Coverage Summary

| Module | Test Files | Coverage Target | Priority |
|--------|------------|-----------------|----------|
| `api` | 5 | 80% | P0 |
| `core` | 35+ | 75% | P0 |
| `distribution` | 0 (NEW) | 70% | P1 |
| `thumbnails` | 20+ | 70% | P1 |
| `reference` | 5 | 60% | P2 |
| `feature` | 2 | 50% | P2 |

---

## Test Categories

### 1. API Module Tests

#### Interfaces & Value Objects

| Test Class | Target Class | Test Cases |
|------------|--------------|------------|
| `PageTest` | `Page` | Interface contract tests |
| `SiteTest` | `Site` | Interface contract tests |
| `ComponentTest` | `Component` | Interface contract tests |
| `PageTemplateTest` | `PageTemplate` | Interface contract tests |
| `ResourceTreeTest` | `ResourceTree` | Tree traversal tests |
| `PublicationEventTest` | `PublicationEvent` | Event creation/properties |
| `PublicationExceptionTest` | `PublicationException` | Exception handling |

### 2. Core Module Tests

#### Sling Models

| Test Class | Target Class | Test Cases |
|------------|--------------|------------|
| `PageImplTest` | `PageImpl` | Page model adaption |
| `SiteImplTest` | `SiteImpl` | Site model adaption |
| `FileImplTest` | `FileImpl` | File model adaption |
| `ComponentImplTest` | `ComponentImpl` | Component model tests |
| `PageContextImplTest` | `PageContextImpl` | Page context resolution |
| `PageTemplateImplTest` | `PageTemplateImpl` | Template resolution |
| `PublishableResourceImplTest` | `PublishableResourceImpl` | Publication state |
| `AuthorizableWrapperImplTest` | `AuthorizableWrapperImpl` | User/group wrapping |

#### Servlets

| Test Class | Target Class | Test Cases |
|------------|--------------|------------|
| `CMSPageServletTest` | `CMSPageServlet` | Page rendering |
| `DownloadFileServletTest` | `DownloadFileServlet` | File download |
| `PreviewFileServletTest` | `PreviewFileServlet` | File preview |
| `PathSuggestionServletTest` | `PathSuggestionServlet` | Autocomplete |
| `CmsDefaultErrorHandlerServletTest` | `CmsDefaultErrorHandlerServlet` | Error handling |

#### Filters

| Test Class | Target Class | Test Cases |
|------------|--------------|------------|
| `CMSSecurityFilterTest` | `CMSSecurityFilter` | Security filtering |
| `CMSSecurityConfigInstanceTest` | `CMSSecurityConfigInstance` | Config handling |
| `EditIncludeFilterTest` | `EditIncludeFilter` | Edit mode filtering |
| `LocaleFilterTest` | `LocaleFilter` | Locale resolution |

#### Operations (Sling POST Operations)

| Test Class | Target Class | Test Cases |
|------------|--------------|------------|
| `CreateUserOperationTest` | `CreateUserOperation` | User creation |
| `CreateGroupOperationTest` | `CreateGroupOperation` | Group creation |
| `ChangePasswordOperationTest` | `ChangePasswordOperation` | Password change |
| `MembersOperationTest` | `MembersOperation` | Group members |
| `MembershipOperationTest` | `MembershipOperation` | User membership |
| `UpdateStatusOperationTest` | `UpdateStatusOperation` | Publication status |
| `CheckoutPostOperationTest` | `CheckoutPostOperation` | Version checkout |
| `BulkReplaceOperationTest` | `BulkReplaceOperation` | Bulk replace |

#### Services

| Test Class | Target Class | Test Cases |
|------------|--------------|------------|
| `FileMetadataExtractorImplTest` | `FileMetadataExtractorImpl` | Metadata extraction |
| `ReadabilityServiceImplTest` | `ReadabilityServiceImpl` | Readability analysis |
| `I18NProviderImplTest` | `I18NProviderImpl` | i18n resolution |
| `I18NDictionaryImplTest` | `I18NDictionaryImpl` | Dictionary lookup |
| `TaxonomyServiceImplTest` | `TaxonomyServiceImpl` | Taxonomy operations |

#### Insights

| Test Class | Target Class | Test Cases |
|------------|--------------|------------|
| `HTMLValidatorInsightProviderTest` | `HTMLValidatorInsightProvider` | HTML validation |
| `PageSpeedInsightProviderTest` | `PageSpeedInsightProvider` | PageSpeed checks |
| `ReadabilityInsightProviderTest` | `ReadabilityInsightProvider` | Readability checks |

### 3. Distribution Module Tests (NEW)

| Test Class | Target Class | Test Cases |
|------------|--------------|------------|
| `ContentDistributionServiceImplTest` | `ContentDistributionServiceImpl` | Distribution operations |
| `HttpDistributionPublicationManagerTest` | `HttpDistributionPublicationManager` | HTTP distribution |
| `StandalonePublicationManagerTest` | `StandalonePublicationManager` | Standalone publish |
| `PublishPostOperationTest` | `PublishPostOperation` | Publish operation |
| `UnpublishPostOperationTest` | `UnpublishPostOperation` | Unpublish operation |
| `BulkPublicationJobTest` | `BulkPublicationJob` | Bulk publish job |
| `ContentImportServletTest` | `ContentImportServlet` | Content import |

### 4. Thumbnails Module Tests

| Test Class | Target Class | Test Cases |
|------------|--------------|------------|
| `TransformerImplTest` | `TransformerImpl` | Image transformation |
| `ThumbnailSupportImplTest` | `ThumbnailSupportImpl` | Thumbnail generation |
| `RenditionSupportImplTest` | `RenditionSupportImpl` | Rendition management |
| `ImageThumbnailProviderTest` | `ImageThumbnailProvider` | Image thumbnails |
| `PdfThumbnailProviderTest` | `PdfThumbnailProvider` | PDF thumbnails |
| `VideoThumbnailProviderTest` | `VideoThumbnailProvider` | Video thumbnails |
| `CropHandlerTest` | `CropHandler` | Crop transformation |
| `ResizeHandlerTest` | `ResizeHandler` | Resize transformation |
| `RotateHandlerTest` | `RotateHandler` | Rotate transformation |
| `FFmpegVideoFrameExtractorTest` | `FFmpegVideoFrameExtractor` | Video frame extraction |

---

## Testing Patterns

### Pattern 1: Sling Model Testing

```java
@ExtendWith(SlingContextExtension.class)
class PageImplTest {
    
    private final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);
    
    @BeforeEach
    void setUp() {
        context.load().json("/content.json", "/content");
        context.addModelsForClasses(PageImpl.class);
    }
    
    @Test
    void testGetTitle() {
        Resource resource = context.resourceResolver().getResource("/content/site/page");
        Page page = resource.adaptTo(Page.class);
        
        assertThat(page).isNotNull();
        assertThat(page.getTitle()).isEqualTo("Test Page");
    }
    
    @Test
    void testGetContentResource() {
        Resource resource = context.resourceResolver().getResource("/content/site/page");
        Page page = resource.adaptTo(Page.class);
        
        assertThat(page.getContentResource()).isNotNull();
        assertThat(page.getContentResource().getPath()).isEqualTo("/content/site/page/jcr:content");
    }
}
```

### Pattern 2: Servlet Testing

```java
@ExtendWith(SlingContextExtension.class)
class DownloadFileServletTest {
    
    private final SlingContext context = new SlingContext();
    private DownloadFileServlet servlet;
    
    @BeforeEach
    void setUp() {
        servlet = new DownloadFileServlet();
        context.load().binaryFile("/test-file.pdf", "/content/files/test.pdf/jcr:content");
    }
    
    @Test
    void testDoGet() throws Exception {
        context.requestPathInfo().setResourcePath("/content/files/test.pdf");
        context.requestPathInfo().setExtension("pdf");
        
        servlet.doGet(context.request(), context.response());
        
        assertThat(context.response().getStatus()).isEqualTo(200);
        assertThat(context.response().getContentType()).isEqualTo("application/pdf");
        assertThat(context.response().getHeader("Content-Disposition"))
            .contains("attachment");
    }
}
```

### Pattern 3: OSGi Service Testing

```java
@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
class FileMetadataExtractorImplTest {
    
    private final SlingContext context = new SlingContext();
    
    @Mock
    private TikaService tikaService;
    
    @InjectMocks
    private FileMetadataExtractorImpl extractor;
    
    @BeforeEach
    void setUp() {
        context.registerService(TikaService.class, tikaService);
        context.registerInjectActivateService(extractor);
    }
    
    @Test
    void testExtractMetadata() throws Exception {
        when(tikaService.detect(any(InputStream.class))).thenReturn("image/jpeg");
        
        Resource resource = context.create().resource("/content/files/image.jpg");
        Map<String, Object> metadata = extractor.extractMetadata(resource);
        
        assertThat(metadata).containsKey("contentType");
        assertThat(metadata.get("contentType")).isEqualTo("image/jpeg");
    }
}
```

### Pattern 4: POST Operation Testing

```java
@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
class CreateUserOperationTest {
    
    private final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);
    
    @Mock
    private SlingHttpServletRequest request;
    
    @Mock
    private PostResponse response;
    
    private CreateUserOperation operation;
    
    @BeforeEach
    void setUp() {
        operation = new CreateUserOperation();
        context.registerInjectActivateService(operation);
    }
    
    @Test
    void testCreateUser() throws Exception {
        when(request.getParameter("userId")).thenReturn("testuser");
        when(request.getParameter("password")).thenReturn("password123");
        when(request.getParameter("email")).thenReturn("test@example.com");
        
        List<Modification> changes = new ArrayList<>();
        operation.run(context.resourceResolver(), request, response, changes);
        
        assertThat(changes).hasSize(1);
        verify(response).setPath(contains("testuser"));
    }
}
```

### Pattern 5: Filter Testing

```java
@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
class CMSSecurityFilterTest {
    
    private final SlingContext context = new SlingContext();
    
    @Mock
    private FilterChain filterChain;
    
    private CMSSecurityFilter filter;
    
    @BeforeEach
    void setUp() {
        filter = new CMSSecurityFilter();
        // Configure filter
    }
    
    @Test
    void testAllowedPath() throws Exception {
        context.request().setPathInfo("/cms/content.html");
        
        filter.doFilter(context.request(), context.response(), filterChain);
        
        verify(filterChain).doFilter(any(), any());
    }
    
    @Test
    void testBlockedPath() throws Exception {
        context.request().setPathInfo("/admin/secret");
        
        filter.doFilter(context.request(), context.response(), filterChain);
        
        assertThat(context.response().getStatus()).isEqualTo(403);
        verify(filterChain, never()).doFilter(any(), any());
    }
}
```

---

## Test Scenarios by Module

### Core Module - Detailed Scenarios

#### PageImpl Tests

| Test ID | Scenario | Expected Result |
|---------|----------|-----------------|
| PAGE-001 | Adapt resource to Page | Non-null Page instance |
| PAGE-002 | Get page title | Returns jcr:title value |
| PAGE-003 | Get page with no title | Returns name as fallback |
| PAGE-004 | Get content resource | Returns jcr:content child |
| PAGE-005 | Get parent page | Returns parent Page |
| PAGE-006 | Get template path | Returns sling:template |
| PAGE-007 | Get keywords | Returns keyword array |
| PAGE-008 | Get publish date | Returns publication date |
| PAGE-009 | Check published status | Returns true/false |
| PAGE-010 | Get page properties | Returns ValueMap |

#### CMSSecurityFilter Tests

| Test ID | Scenario | Expected Result |
|---------|----------|-----------------|
| SEC-001 | Request to /cms with auth | Allow request |
| SEC-002 | Request to /cms without auth | Redirect to login |
| SEC-003 | Request to public path | Allow request |
| SEC-004 | Request to blocked path | Return 403 |
| SEC-005 | Request with valid session | Allow request |
| SEC-006 | Request with expired session | Redirect to login |
| SEC-007 | CORS preflight request | Handle OPTIONS |
| SEC-008 | API request with token | Validate token |

#### FileMetadataExtractor Tests

| Test ID | Scenario | Expected Result |
|---------|----------|-----------------|
| META-001 | Extract JPEG metadata | Returns EXIF data |
| META-002 | Extract PDF metadata | Returns PDF properties |
| META-003 | Extract Office doc metadata | Returns document properties |
| META-004 | Handle corrupt file | Throws appropriate exception |
| META-005 | Handle empty file | Returns minimal metadata |
| META-006 | Extract video metadata | Returns duration, dimensions |

### Distribution Module - Detailed Scenarios

#### ContentDistributionService Tests

| Test ID | Scenario | Expected Result |
|---------|----------|-----------------|
| DIST-001 | Distribute single page | Success response |
| DIST-002 | Distribute with references | Include references |
| DIST-003 | Distribute to offline target | Retry/queue |
| DIST-004 | Handle distribution conflict | Resolve or report |
| DIST-005 | Bulk distribution | Batch processing |
| DIST-006 | Verify distribution status | Return status |

### Thumbnails Module - Detailed Scenarios

#### Transformer Tests

| Test ID | Scenario | Expected Result |
|---------|----------|-----------------|
| TRANS-001 | Apply crop transformation | Cropped image |
| TRANS-002 | Apply resize transformation | Resized image |
| TRANS-003 | Chain multiple transformations | All applied in order |
| TRANS-004 | Transform PDF to image | PNG/JPEG output |
| TRANS-005 | Transform unsupported format | Appropriate error |
| TRANS-006 | Handle large image | Memory efficient |

---

## Mocking Strategies

### 1. Sling Resource Mocking

```java
// Using Sling Mocks
@BeforeEach
void setUp() {
    context.create().resource("/content/site", 
        "jcr:primaryType", "sling:Site",
        "jcr:title", "Test Site");
        
    context.create().resource("/content/site/page",
        "jcr:primaryType", "sling:Page");
        
    context.create().resource("/content/site/page/jcr:content",
        "jcr:primaryType", "nt:unstructured",
        "jcr:title", "Test Page",
        "sling:resourceType", "cms/components/page");
}
```

### 2. JCR Session Mocking

```java
@Mock
private Session session;

@Mock
private Node node;

@BeforeEach
void setUp() throws Exception {
    when(session.getNode(anyString())).thenReturn(node);
    when(node.getProperty("jcr:title")).thenReturn(mockProperty("Test"));
}
```

### 3. OSGi Service Mocking

```java
@BeforeEach
void setUp() {
    // Register mock services
    context.registerService(ResourceResolverFactory.class, 
        mock(ResourceResolverFactory.class));
    context.registerService(JobManager.class, 
        mock(JobManager.class));
    
    // Activate service under test
    context.registerInjectActivateService(serviceUnderTest);
}
```

---

## Coverage Goals

### Module Coverage Targets

| Module | Line Coverage | Branch Coverage | Mutation Score |
|--------|---------------|-----------------|----------------|
| `api` | 80% | 70% | 60% |
| `core` | 75% | 65% | 55% |
| `distribution` | 70% | 60% | 50% |
| `thumbnails` | 70% | 60% | 50% |
| `reference` | 60% | 50% | 40% |

### Exclusions

Files/packages excluded from coverage:
- Generated code (`**/generated/**`)
- Test utilities (`**/helpers/**`, `**/mocks/**`)
- Configuration classes (simple POJOs)
- Exception classes (simple wrappers)

---

## Best Practices

### 1. Test Naming Convention

```java
// Pattern: methodName_scenario_expectedBehavior
@Test
void getTitle_withValidPage_returnsTitle() { }

@Test
void getTitle_withNullContent_returnsNull() { }

@Test
void createUser_withDuplicateId_throwsException() { }
```

### 2. Test Structure (AAA Pattern)

```java
@Test
void testMethod() {
    // Arrange
    Resource resource = context.create().resource("/test");
    
    // Act
    Page page = resource.adaptTo(Page.class);
    
    // Assert
    assertThat(page).isNotNull();
}
```

### 3. Test Data Files

```
src/test/resources/
├── content.json           # Main test content
├── conf.json             # Configuration content
├── users.json            # Test users
├── test-image.jpg        # Binary test files
├── test-document.pdf
└── expected/             # Expected outputs
    ├── transformed-image.png
    └── extracted-metadata.json
```

### 4. Parameterized Tests

```java
@ParameterizedTest
@CsvSource({
    "/content/site/page, Test Page",
    "/content/site/page2, Another Page",
    "/content/site/child/page, Child Page"
})
void testGetTitle(String path, String expectedTitle) {
    Resource resource = context.resourceResolver().getResource(path);
    Page page = resource.adaptTo(Page.class);
    
    assertThat(page.getTitle()).isEqualTo(expectedTitle);
}
```

### 5. Test Isolation

```java
@BeforeEach
void setUp() {
    // Fresh context for each test
    context.build()
        .resource("/content")
        .commit();
}

@AfterEach
void tearDown() {
    // Cleanup if needed
    context.resourceResolver().revert();
}
```

---

## Running Tests

### Maven Commands

```bash
# Run all unit tests
mvn test

# Run tests for specific module
mvn test -pl core

# Run with coverage report
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=PageImplTest

# Run tests matching pattern
mvn test -Dtest="*ImplTest"

# Skip tests
mvn install -DskipTests
```

### IDE Integration

- **IntelliJ IDEA**: Right-click test class/method → Run
- **VS Code**: Use Java Test Runner extension
- **Eclipse**: Right-click → Run As → JUnit Test

---

## CI Integration

### JaCoCo Configuration

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## Appendix: Test File Checklist

### Files Needing Tests (Priority Order)

#### P0 - Critical (Must Have)

- [ ] `ContentDistributionServiceImpl` - NEW
- [ ] `HttpDistributionPublicationManager` - NEW
- [ ] `TaxonomyServiceImpl` - NEW
- [ ] `PageContextImpl` - EXISTS, needs expansion
- [ ] `CMSPageServlet` - Needs creation

#### P1 - Important

- [ ] `ContentImportServlet` - NEW
- [ ] `BulkPublicationJob` - Needs migration
- [ ] `ContentGridModel` - NEW
- [ ] `ContentTableModel` - NEW
- [ ] `I18nContainerModel` - NEW

#### P2 - Nice to Have

- [ ] `PageEditBarModel` - NEW
- [ ] `ModalActionModel` - NEW
- [ ] `TaxonomyOptions` - NEW
- [ ] `TextElement` - NEW
