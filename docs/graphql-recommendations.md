# GraphQL Module - Recommendations & Roadmap

> **Document Version:** 1.0  
> **Date:** January 11, 2026  
> **Purpose:** Strategic recommendations for GraphQL module enhancement

## Executive Summary

The GraphQL module is production-ready for basic use but requires enhancements for enterprise deployment. This document outlines prioritized recommendations for improving security, functionality, testing, and documentation.

## Priority Matrix

| Priority | Timeline | Effort | Impact | Risk if Not Done |
|----------|----------|--------|--------|-----------------|
| **P0** | Immediate | Medium | High | Security vulnerabilities |
| **P1** | 1-2 weeks | Medium | High | Production reliability issues |
| **P2** | 1 month | High | Medium | Limited usability |
| **P3** | 3 months | High | High | Long-term maintainability |

---

## P0: Critical Security (Immediate)

### 1. Add Request Authentication & Authorization

**Issue:** `/graphql` endpoint is publicly accessible without authentication

**Risk:** ⚠️ **HIGH** - Data leakage, unauthorized access

**Recommendation:**

```java
// Add to each DataFetcher
@Override
public Object get(SlingDataFetcherEnvironment environment) throws Exception {
    ResourceResolver resolver = environment.getCurrentResource().getResourceResolver();
    
    // Check if user is authenticated
    if (resolver.getUserID().equals("anonymous")) {
        throw new UnauthorizedException("Authentication required");
    }
    
    // Check specific permissions
    Resource queryResource = resolver.getResource("/content/protected");
    if (queryResource == null || !hasReadPermission(resolver, queryResource)) {
        throw new ForbiddenException("Insufficient permissions");
    }
    
    // Continue with data fetching...
}
```

**Configuration via repoinit:**

```json
{
  "repoinit:TEXT|true": [
    "set ACL for everyone",
    "  deny jcr:read on /graphql",
    "end",
    "",
    "set ACL for sling-cms-authors",
    "  allow jcr:read on /graphql",
    "end"
  ]
}
```

**Effort:** 2-4 hours  
**Testing:** Verify with authenticated and anonymous users

---

### 2. Implement Query Complexity Limiting

**Issue:** No protection against complex queries causing DoS

**Risk:** ⚠️ **MEDIUM** - Resource exhaustion, server overload

**Recommendation:**

```java
// Create: graphql/src/main/java/.../internal/MaxQueryDepthInstrumentation.java
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.ExecutionContext;

@Component(service = Instrumentation.class)
public class MaxQueryDepthInstrumentation extends SimpleInstrumentation {
    
    private static final int MAX_DEPTH = 10;
    
    @Override
    public ExecutionContext instrumentExecutionContext(
            ExecutionContext executionContext,
            InstrumentationExecutionParameters parameters) {
        
        int depth = calculateQueryDepth(executionContext.getOperationDefinition());
        if (depth > MAX_DEPTH) {
            throw new QueryDepthExceededException(
                "Query depth " + depth + " exceeds maximum " + MAX_DEPTH
            );
        }
        
        return executionContext;
    }
}
```

**Configuration:**

```java
@ObjectClassDefinition(name = "GraphQL Security Configuration")
public @interface Config {
    @AttributeDefinition(name = "Max Query Depth")
    int maxQueryDepth() default 10;
    
    @AttributeDefinition(name = "Max Query Complexity")
    int maxQueryComplexity() default 1000;
}
```

**Effort:** 4-6 hours  
**Testing:** Test with deeply nested queries

---

### 3. Self-Host GraphiQL Assets (Remove CDN Dependencies)

**Issue:** GraphiQL loads React/GraphiQL from unpkg.com CDN

**Risk:** ⚠️ **MEDIUM** - Supply chain attack, availability issues

**Recommendation:**

```bash
# Download assets locally
cd graphql/src/main/resources/jcr_root/apps/sling-cms/servlet/assets/
wget https://unpkg.com/react@18/umd/react.production.min.js
wget https://unpkg.com/react-dom@18/umd/react-dom.production.min.js
wget https://unpkg.com/graphiql@3/graphiql.min.js
wget https://unpkg.com/graphiql@3/graphiql.min.css
```

**Update html.html:**

```html
<link rel="stylesheet" href="/apps/sling-cms/servlet/assets/graphiql.min.css" />
<script src="/apps/sling-cms/servlet/assets/react.production.min.js"></script>
<script src="/apps/sling-cms/servlet/assets/react-dom.production.min.js"></script>
<script src="/apps/sling-cms/servlet/assets/graphiql.min.js"></script>
```

**Effort:** 2-3 hours  
**Testing:** Verify GraphiQL loads without internet connection

---

## P1: Production Reliability (1-2 Weeks)

### 4. Add Comprehensive Unit Tests

**Issue:** No unit tests exist for any component

**Risk:** ⚠️ **HIGH** - Regressions undetected, low confidence in changes

**Recommendation:**

Create test suite:

```
graphql/src/test/java/org/apache/sling/cms/graphql/internal/
├── fetchers/
│   ├── HelloDataFetcherTest.java
│   ├── ServerInfoDataFetcherTest.java
│   └── BaseDataFetcherTest.java (abstract test class)
├── schema/
│   ├── SlingCmsSchemaProviderTest.java
│   └── SchemaValidationTest.java
└── security/
    └── AuthorizationTest.java
```

**Example Test:**

```java
package org.apache.sling.cms.graphql.internal.fetchers;

import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServerInfoDataFetcherTest {

    @Mock
    private SlingDataFetcherEnvironment environment;

    private ServerInfoDataFetcher fetcher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fetcher = new ServerInfoDataFetcher();
    }

    @Test
    void testServerInfoReturnsAllFields() throws Exception {
        Map<String, Object> result = fetcher.get(environment);
        
        assertNotNull(result);
        assertTrue(result.containsKey("version"));
        assertTrue(result.containsKey("timestamp"));
        assertTrue(result.containsKey("environment"));
        assertTrue(result.containsKey("graphqlVersion"));
    }
    
    @Test
    void testTimestampIsValidISO8601() throws Exception {
        Map<String, Object> result = fetcher.get(environment);
        String timestamp = (String) result.get("timestamp");
        
        // Verify ISO 8601 format
        assertNotNull(timestamp);
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"));
    }
    
    @Test
    void testVersionMatchesProjectVersion() throws Exception {
        Map<String, Object> result = fetcher.get(environment);
        String version = (String) result.get("version");
        
        // Should match project version from pom.xml
        assertNotNull(version);
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+.*"));
    }
}
```

**Coverage Goal:** >80% line coverage

**Effort:** 1 week  
**Testing:** Run `mvn test -pl graphql`

---

### 5. Add Error Handling & Logging

**Issue:** Data fetchers don't handle exceptions properly

**Risk:** ⚠️ **MEDIUM** - Poor user experience, difficult debugging

**Recommendation:**

Create base fetcher with error handling:

```java
package org.apache.sling.cms.graphql.internal.fetchers;

import org.apache.sling.graphql.api.SlingDataFetcher;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseDataFetcher<T> implements SlingDataFetcher<T> {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public final T get(SlingDataFetcherEnvironment environment) throws Exception {
        try {
            log.debug("Fetching data for: {}", getClass().getSimpleName());
            T result = fetchData(environment);
            log.debug("Successfully fetched data");
            return result;
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid arguments: {}", e.getMessage());
            throw e;
            
        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found: {}", e.getMessage());
            throw e;
            
        } catch (Exception e) {
            log.error("Error fetching data", e);
            throw new DataFetchingException("Internal error: " + e.getMessage(), e);
        }
    }
    
    protected abstract T fetchData(SlingDataFetcherEnvironment environment) throws Exception;
}
```

**Update existing fetchers:**

```java
@Component(
    service = SlingDataFetcher.class,
    property = {"name=slingcms/serverInfo"})
public class ServerInfoDataFetcher extends BaseDataFetcher<Map<String, Object>> {
    
    @Override
    protected Map<String, Object> fetchData(SlingDataFetcherEnvironment environment) {
        // Implementation without try-catch (handled by base class)
        return createServerInfo();
    }
}
```

**Effort:** 4-6 hours  
**Testing:** Verify error messages in logs

---

### 6. Externalize Configuration (OSGi Config)

**Issue:** Version strings and environment hardcoded in fetchers

**Risk:** ⚠️ **LOW** - Maintenance overhead, inflexibility

**Recommendation:**

```java
// Create: graphql/src/main/java/.../internal/GraphQLConfigService.java
@Component(
    service = GraphQLConfigService.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = GraphQLConfigService.Config.class)
public class GraphQLConfigService {
    
    @ObjectClassDefinition(name = "GraphQL Configuration")
    public @interface Config {
        @AttributeDefinition(name = "Environment")
        String environment() default "development";
        
        @AttributeDefinition(name = "Enable GraphiQL")
        boolean enableGraphiQL() default true;
        
        @AttributeDefinition(name = "Max Query Depth")
        int maxQueryDepth() default 10;
    }
    
    private Config config;
    
    @Activate
    protected void activate(Config config) {
        this.config = config;
    }
    
    public String getEnvironment() {
        return config.environment();
    }
    
    public boolean isGraphiQLEnabled() {
        return config.enableGraphiQL();
    }
    
    public int getMaxQueryDepth() {
        return config.maxQueryDepth();
    }
}
```

**Update ServerInfoDataFetcher:**

```java
@Component(
    service = SlingDataFetcher.class,
    property = {"name=slingcms/serverInfo"})
public class ServerInfoDataFetcher extends BaseDataFetcher<Map<String, Object>> {
    
    @Reference
    private GraphQLConfigService config;
    
    @Override
    protected Map<String, Object> fetchData(SlingDataFetcherEnvironment environment) {
        Map<String, Object> info = new HashMap<>();
        info.put("version", getProjectVersion());  // From MANIFEST.MF
        info.put("timestamp", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        info.put("environment", config.getEnvironment());  // From config
        info.put("graphqlVersion", "0.0.24");
        return info;
    }
}
```

**Effort:** 3-4 hours  
**Testing:** Verify config via Web Console

---

## P2: Feature Enhancement (1 Month)

### 7. Implement CMS-Specific Queries

**Issue:** Only sample queries exist, no real CMS data access

**Risk:** ⚠️ **HIGH** - Module not useful for real applications

**Recommendation:**

**Phase 1: Page Queries**

```graphql
type Query {
  page(path: String!): Page
  pages(parent: String!, depth: Int): [Page!]!
}

type Page {
  path: String!
  name: String!
  title: String
  description: String
  template: String
  lastModified: String
  author: String
  properties: JSON
  children: [Page!]!
}
```

**Implementation:**

```java
@Component(
    service = SlingDataFetcher.class,
    property = {"name=slingcms/page"})
public class PageDataFetcher extends BaseDataFetcher<Map<String, Object>> {
    
    @Reference
    private PageManager pageManager;
    
    @Override
    protected Map<String, Object> fetchData(SlingDataFetcherEnvironment environment) {
        String path = environment.getArgument("path", String.class);
        ResourceResolver resolver = environment.getCurrentResource().getResourceResolver();
        
        Page page = pageManager.getPage(resolver, path);
        if (page == null) {
            throw new ResourceNotFoundException("Page not found: " + path);
        }
        
        return convertPage(page);
    }
    
    private Map<String, Object> convertPage(Page page) {
        Map<String, Object> result = new HashMap<>();
        result.put("path", page.getPath());
        result.put("name", page.getName());
        result.put("title", page.getTitle());
        result.put("description", page.getDescription());
        result.put("template", page.getTemplate());
        result.put("lastModified", page.getLastModifiedDate());
        result.put("author", page.getLastModifiedBy());
        return result;
    }
}
```

**Phase 2: Asset Queries**

```graphql
type Query {
  asset(path: String!): Asset
  assets(folder: String!, mimeType: String): [Asset!]!
}

type Asset {
  path: String!
  name: String!
  mimeType: String!
  size: Int!
  width: Int
  height: Int
  metadata: JSON
  renditions: [Rendition!]!
}

type Rendition {
  name: String!
  url: String!
  width: Int
  height: Int
}
```

**Phase 3: Navigation & Search**

```graphql
type Query {
  navigation(root: String!): [NavigationItem!]!
  search(query: String!, limit: Int): SearchResult!
}

type NavigationItem {
  title: String!
  url: String!
  children: [NavigationItem!]!
}

type SearchResult {
  total: Int!
  items: [SearchResultItem!]!
}

type SearchResultItem {
  path: String!
  title: String!
  excerpt: String
  type: String!
}
```

**Effort:** 2-3 weeks  
**Testing:** Integration tests for each query type

---

### 8. Implement DataLoader for N+1 Query Optimization

**Issue:** DataLoader dependency present but not used

**Risk:** ⚠️ **MEDIUM** - Poor performance for complex queries

**Recommendation:**

```java
// Create: graphql/src/main/java/.../internal/dataloaders/PageDataLoader.java
@Component(service = DataLoader.class)
public class PageDataLoader implements DataLoader<String, Page> {
    
    @Reference
    private PageManager pageManager;
    
    public CompletableFuture<Page> load(String path) {
        return CompletableFuture.supplyAsync(() -> 
            pageManager.getPage(path)
        );
    }
    
    public CompletableFuture<List<Page>> loadMany(List<String> paths) {
        return CompletableFuture.supplyAsync(() -> {
            // Batch load pages efficiently
            return paths.stream()
                .map(pageManager::getPage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        });
    }
}
```

**Usage in fetcher:**

```java
@Override
protected List<Map<String, Object>> fetchData(SlingDataFetcherEnvironment environment) {
    DataLoader<String, Page> pageLoader = environment.getDataLoader("pageLoader");
    
    List<String> pagePaths = getPagePaths();
    
    // Load all pages in one batch
    CompletableFuture<List<Page>> futurePages = pageLoader.loadMany(pagePaths);
    List<Page> pages = futurePages.join();
    
    return pages.stream()
        .map(this::convertPage)
        .collect(Collectors.toList());
}
```

**Effort:** 1 week  
**Testing:** Performance benchmark with/without DataLoader

---

### 9. Add Caching Layer

**Issue:** No caching, every query hits JCR

**Risk:** ⚠️ **MEDIUM** - Poor performance under load

**Recommendation:**

```java
@Component(service = GraphQLCacheService.class)
public class GraphQLCacheService {
    
    private final Cache<String, Object> cache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
    
    public <T> T getOrFetch(String key, Supplier<T> fetcher) {
        return (T) cache.get(key, k -> fetcher.get());
    }
    
    public void invalidate(String key) {
        cache.invalidate(key);
    }
    
    public void invalidateAll() {
        cache.invalidateAll();
    }
}
```

**Usage:**

```java
@Reference
private GraphQLCacheService cacheService;

@Override
protected Map<String, Object> fetchData(SlingDataFetcherEnvironment environment) {
    String path = environment.getArgument("path", String.class);
    String cacheKey = "page:" + path;
    
    return cacheService.getOrFetch(cacheKey, () -> {
        Page page = pageManager.getPage(path);
        return convertPage(page);
    });
}
```

**Effort:** 3-4 days  
**Testing:** Benchmark cache hit/miss performance

---

## P3: Long-Term Improvements (3 Months)

### 10. Add Integration Tests (Cypress)

**Issue:** No end-to-end tests for GraphQL API

**Risk:** ⚠️ **MEDIUM** - Integration issues undetected

**Recommendation:**

```javascript
// it/cypress/e2e/graphql/graphql-api.cy.js
describe('GraphQL API', () => {
  
  it('should return hello message', () => {
    cy.request({
      method: 'POST',
      url: '/graphql.json',
      body: {
        query: '{ hello }'
      }
    }).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body.data.hello).to.include('Apache Sling CMS');
    });
  });
  
  it('should return server info', () => {
    cy.request({
      method: 'POST',
      url: '/graphql.json',
      body: {
        query: '{ serverInfo { version environment } }'
      }
    }).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body.data.serverInfo).to.have.property('version');
      expect(response.body.data.serverInfo).to.have.property('environment');
    });
  });
  
  it('should require authentication for protected queries', () => {
    cy.request({
      method: 'POST',
      url: '/graphql.json',
      failOnStatusCode: false,
      body: {
        query: '{ pages { path } }'
      }
    }).then((response) => {
      expect(response.status).to.eq(401);
    });
  });
  
  it('GraphiQL interface should load', () => {
    cy.visit('/graphql');
    cy.get('#graphiql').should('be.visible');
    cy.contains('GraphiQL').should('be.visible');
  });
});
```

**Effort:** 1 week  
**Testing:** Run in CI/CD pipeline

---

### 11. Add Performance Monitoring

**Issue:** No metrics or monitoring

**Risk:** ⚠️ **LOW** - Can't detect performance degradation

**Recommendation:**

```java
@Component(service = GraphQLMetricsService.class)
public class GraphQLMetricsService {
    
    private final Counter queryCounter = Counter.build()
        .name("graphql_queries_total")
        .help("Total GraphQL queries executed")
        .labelNames("fetcher", "status")
        .register();
    
    private final Histogram queryDuration = Histogram.build()
        .name("graphql_query_duration_seconds")
        .help("GraphQL query execution time")
        .labelNames("fetcher")
        .register();
    
    public void recordQuery(String fetcherName, long durationMs, boolean success) {
        queryCounter.labels(fetcherName, success ? "success" : "error").inc();
        queryDuration.labels(fetcherName).observe(durationMs / 1000.0);
    }
}
```

**Effort:** 3-4 days  
**Testing:** Verify metrics in Prometheus/Grafana

---

### 12. Add Rate Limiting

**Issue:** No protection against request flooding

**Risk:** ⚠️ **MEDIUM** - DoS vulnerability

**Recommendation:**

```java
@Component(
    service = Filter.class,
    property = {
        "sling.filter.scope=REQUEST",
        "sling.filter.pattern=/graphql.*"
    })
public class GraphQLRateLimitFilter implements Filter {
    
    private final RateLimiter rateLimiter = RateLimiter.create(100.0); // 100 req/sec
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        if (!rateLimiter.tryAcquire()) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(429); // Too Many Requests
            httpResponse.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
            return;
        }
        
        chain.doFilter(request, response);
    }
}
```

**Effort:** 2-3 days  
**Testing:** Load test to verify rate limiting

---

## Implementation Roadmap

### Sprint 1 (Week 1-2): Security & Reliability

- ✅ P0.1: Add authentication/authorization
- ✅ P0.2: Implement query complexity limiting
- ✅ P0.3: Self-host GraphiQL assets
- ✅ P1.4: Add unit tests (basic coverage)

**Deliverable:** Secure, tested GraphQL module ready for production

---

### Sprint 2 (Week 3-4): Production Hardening

- ✅ P1.5: Add error handling & logging
- ✅ P1.6: Externalize configuration
- ✅ P1.4: Complete unit tests (>80% coverage)

**Deliverable:** Production-grade module with proper observability

---

### Sprint 3 (Week 5-8): Feature Development

- ✅ P2.7: Implement page queries
- ✅ P2.7: Implement asset queries
- ✅ P2.8: Add DataLoader support
- ✅ P2.9: Add caching layer

**Deliverable:** Functional CMS GraphQL API with performance optimization

---

### Sprint 4 (Week 9-12): Long-Term Quality

- ✅ P3.10: Add integration tests
- ✅ P3.11: Add performance monitoring
- ✅ P3.12: Add rate limiting
- ✅ Final documentation updates

**Deliverable:** Enterprise-ready GraphQL module with monitoring and protection

---

## Success Metrics

### Security

- ✅ All endpoints require authentication
- ✅ Query complexity limiting active (max depth: 10)
- ✅ Rate limiting: 100 req/sec per IP
- ✅ No external CDN dependencies

### Reliability

- ✅ Unit test coverage: >80%
- ✅ Integration test coverage: >50%
- ✅ Zero known critical bugs
- ✅ Error handling in all fetchers

### Performance

- ✅ p50 query latency: <100ms
- ✅ p95 query latency: <500ms
- ✅ Cache hit rate: >70%
- ✅ DataLoader reduces N+1 queries by 90%

### Functionality

- ✅ Page query support
- ✅ Asset query support
- ✅ Navigation query support
- ✅ Search integration

---

## Conclusion

The GraphQL module has a solid foundation but needs systematic enhancement to be enterprise-ready. Following this roadmap will result in a secure, performant, and feature-complete GraphQL API for Apache Sling CMS.

**Next Steps:**
1. Review and approve recommendations
2. Allocate development resources
3. Start with Sprint 1 (security & reliability)
4. Track progress against success metrics

---

**Document Version:** 1.0  
**Last Updated:** January 11, 2026  
**Status:** ✅ Ready for Implementation
