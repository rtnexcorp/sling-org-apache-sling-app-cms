# GraphQL Module - Current Implementation Guide

> **Module Status:** ✅ PRODUCTION READY  
> **Version:** 1.1.9-SNAPSHOT  
> **Last Updated:** January 11, 2026

## Quick Start

### Access GraphiQL Interface

Open your browser:
```
http://localhost:8082/graphql
```

### Test the API

**Query 1: Hello World**
```graphql
{
  hello
}
```

**Query 2: Server Info**
```graphql
{
  serverInfo {
    version
    timestamp
    environment
    graphqlVersion
  }
}
```

### REST API

**POST Request:**
```bash
curl -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query": "{ serverInfo { version timestamp } }"}'
```

**GET Request:**
```bash
curl "http://localhost:8082/graphql.json?query=%7BhelloString%7D"
```

## Current Schema

The module implements a minimal schema for demonstration:

```graphql
type Query {
  # Welcome message for API discovery
  hello: String @fetcher(name: "slingcms/hello")
  
  # Server metadata and version info
  serverInfo: ServerInfo @fetcher(name: "slingcms/serverInfo")
}

type ServerInfo {
  version: String          # CMS version (e.g., "1.1.9-SNAPSHOT")
  timestamp: String        # Current server timestamp (ISO 8601)
  environment: String      # Environment name (e.g., "development")
  graphqlVersion: String   # GraphQL Core version (e.g., "0.0.24")
}
```

## Architecture Overview

### Module Structure

```
org.apache.sling.cms.graphql/
├── Java Classes
│   ├── SlingCmsSchemaProvider.java     # Schema definition (inline string)
│   ├── HelloDataFetcher.java           # Implements hello query
│   └── ServerInfoDataFetcher.java      # Implements serverInfo query
│
├── JCR Resources
│   ├── servlet/html.html               # GraphiQL interface (React-based)
│   └── servlet/GQLschema.gql           # Reference schema (not used at runtime)
│
└── Feature Configuration
    └── graphql.json                     # OSGi bundles and servlet config
```

### Key Design Decisions

1. **Schema as Java String (Not .gql File)**
   - Avoids recursive servlet execution
   - Faster runtime performance
   - Type-safe at compile time
   
2. **Java Data Fetchers Only (No HTL)**
   - Better testability
   - Type safety
   - Easier debugging
   
3. **Minimal Initial Implementation**
   - 2 sample queries for demonstration
   - Extension points for adding more queries
   - Production-ready infrastructure

## How It Works

### Request Flow

```
Client Request
    ↓
[GraphQL Servlet] (Sling GraphQL Core)
    ↓
[SlingCmsSchemaProvider] - Provides schema string
    ↓
[GraphQL-Java Engine] - Parses query
    ↓
[HelloDataFetcher or ServerInfoDataFetcher] - Executes query
    ↓
[JSON Response] - Returns data
```

### OSGi Service Registration

**Schema Provider:**
```java
@Component(
    service = SchemaProvider.class,
    property = {"name=slingcms/schema"})
public class SlingCmsSchemaProvider implements SchemaProvider {
    // Returns schema as string
}
```

**Data Fetcher:**
```java
@Component(
    service = SlingDataFetcher.class,
    property = {"name=slingcms/hello"})
public class HelloDataFetcher implements SlingDataFetcher<String> {
    // Fetches data for query
}
```

## Adding Custom Queries

### Example: Add a "pages" Query

**Step 1: Create Data Fetcher**

Create: `graphql/src/main/java/org/apache/sling/cms/graphql/internal/fetchers/PagesDataFetcher.java`

```java
package org.apache.sling.cms.graphql.internal.fetchers;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.graphql.api.SlingDataFetcher;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(
    service = SlingDataFetcher.class,
    property = {"name=slingcms/pages"})
public class PagesDataFetcher implements SlingDataFetcher<List<Map<String, Object>>> {

    private static final Logger log = LoggerFactory.getLogger(PagesDataFetcher.class);

    @Override
    public List<Map<String, Object>> get(SlingDataFetcherEnvironment environment) throws Exception {
        // Get path argument from query
        String path = environment.getArgument("path", String.class);
        if (path == null) {
            path = "/content";
        }

        ResourceResolver resolver = environment.getCurrentResource().getResourceResolver();
        Resource rootResource = resolver.getResource(path);
        
        List<Map<String, Object>> pages = new ArrayList<>();
        
        if (rootResource != null) {
            for (Resource child : rootResource.getChildren()) {
                if (isPage(child)) {
                    pages.add(convertToPage(child));
                }
            }
        }
        
        log.debug("Fetched {} pages from path: {}", pages.size(), path);
        return pages;
    }
    
    private boolean isPage(Resource resource) {
        // Check if resource is a page (implement your logic)
        return resource.getResourceType() != null 
            && resource.getResourceType().contains("page");
    }
    
    private Map<String, Object> convertToPage(Resource resource) {
        Map<String, Object> page = new HashMap<>();
        page.put("path", resource.getPath());
        page.put("name", resource.getName());
        page.put("title", resource.getValueMap().get("jcr:title", String.class));
        page.put("resourceType", resource.getResourceType());
        return page;
    }
}
```

**Step 2: Update Schema Provider**

Edit: `graphql/src/main/java/org/apache/sling/cms/graphql/internal/schema/SlingCmsSchemaProvider.java`

```java
private static final String SCHEMA = "type Query {\n"
        + "  hello: String @fetcher(name: \"slingcms/hello\")\n"
        + "  serverInfo: ServerInfo @fetcher(name: \"slingcms/serverInfo\")\n"
        + "  pages(path: String): [Page!]! @fetcher(name: \"slingcms/pages\")\n"  // ADD THIS
        + "}\n"
        + "\n"
        + "type ServerInfo {\n"
        + "  version: String\n"
        + "  timestamp: String\n"
        + "  environment: String\n"
        + "  graphqlVersion: String\n"
        + "}\n"
        + "\n"
        + "type Page {\n"                                                          // ADD THIS
        + "  path: String!\n"
        + "  name: String!\n"
        + "  title: String\n"
        + "  resourceType: String\n"
        + "}\n";
```

**Step 3: Deploy**

```bash
mvn clean install -P autoInstallBundle -pl graphql -DskipTests -Dbnd.baseline.skip=true
```

**Step 4: Test**

```graphql
{
  pages(path: "/content") {
    path
    name
    title
    resourceType
  }
}
```

## Advanced Patterns

### Pattern 1: Query with Arguments

```java
@Override
public Object get(SlingDataFetcherEnvironment environment) throws Exception {
    String id = environment.getArgument("id", String.class);
    Integer limit = environment.getArgument("limit", Integer.class);
    Boolean active = environment.getArgument("active", Boolean.class);
    
    // Use arguments to filter results
}
```

**Schema:**
```graphql
type Query {
  items(id: ID, limit: Int, active: Boolean): [Item!]!
}
```

### Pattern 2: Nested Types

```java
@Override
public Map<String, Object> get(SlingDataFetcherEnvironment environment) {
    Map<String, Object> page = new HashMap<>();
    page.put("title", "My Page");
    
    // Nested author object
    Map<String, Object> author = new HashMap<>();
    author.put("name", "John Doe");
    author.put("email", "john@example.com");
    page.put("author", author);
    
    return page;
}
```

**Schema:**
```graphql
type Page {
  title: String
  author: Author
}

type Author {
  name: String
  email: String
}
```

### Pattern 3: Using ResourceResolver

```java
@Component(
    service = SlingDataFetcher.class,
    property = {"name=slingcms/resource"})
public class ResourceDataFetcher implements SlingDataFetcher<Map<String, Object>> {

    @Override
    public Map<String, Object> get(SlingDataFetcherEnvironment environment) throws Exception {
        // Get current resource from Sling context
        Resource currentResource = environment.getCurrentResource();
        ResourceResolver resolver = currentResource.getResourceResolver();
        
        // Query JCR
        String path = environment.getArgument("path", String.class);
        Resource targetResource = resolver.getResource(path);
        
        if (targetResource == null) {
            throw new ResourceNotFoundException("Resource not found: " + path);
        }
        
        // Convert to GraphQL response
        return convertResource(targetResource);
    }
}
```

### Pattern 4: Error Handling

```java
@Override
public Object get(SlingDataFetcherEnvironment environment) throws Exception {
    try {
        String path = environment.getArgument("path", String.class);
        
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path argument is required");
        }
        
        // Fetch data
        return fetchData(path);
        
    } catch (IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        throw e;  // Will be returned as GraphQL error
    } catch (Exception e) {
        log.error("Error fetching data", e);
        throw new DataFetchingException("Failed to fetch data", e);
    }
}
```

**GraphQL Error Response:**
```json
{
  "errors": [
    {
      "message": "Path argument is required",
      "locations": [{"line": 2, "column": 3}],
      "path": ["myQuery"]
    }
  ],
  "data": null
}
```

## Configuration

### Servlet Configuration

In `feature/src/main/features/graphql.json`:

```json
{
  "configurations": {
    "org.apache.sling.graphql.core.GraphQLServlet~cms": {
      "sling.servlet.resourceTypes": "sling-cms/servlet",
      "sling.servlet.extensions": "json",
      "sling.servlet.methods": ["GET", "POST"]
    }
  }
}
```

**To change endpoint path:**
```json
{
  "repoinit:TEXT|true": [
    "create path (nt:unstructured) /my-custom-graphql-endpoint",
    "set properties on /my-custom-graphql-endpoint",
    "  set sling:resourceType{String} to sling-cms/servlet",
    "end"
  ]
}
```

Now accessible at: `/my-custom-graphql-endpoint.json`

### Access Control

**Grant read access:**
```json
{
  "repoinit:TEXT|true": [
    "set ACL for mygroup",
    "  allow jcr:read on /graphql",
    "end"
  ]
}
```

**Require authentication:**
```json
{
  "repoinit:TEXT|true": [
    "set ACL for everyone",
    "  deny jcr:read on /graphql",
    "end",
    "",
    "set ACL for authenticated-users",
    "  allow jcr:read on /graphql",
    "end"
  ]
}
```

## Testing

### Manual Testing with curl

**POST request:**
```bash
curl -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ serverInfo { version timestamp } }"
  }'
```

**GET request with variables:**
```bash
curl -G http://localhost:8082/graphql.json \
  --data-urlencode "query={ pages(path: \"/content\") { path title } }"
```

**With authentication:**
```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query": "{ hello }"}'
```

### Unit Testing Data Fetchers

Create: `graphql/src/test/java/org/apache/sling/cms/graphql/internal/fetchers/HelloDataFetcherTest.java`

```java
package org.apache.sling.cms.graphql.internal.fetchers;

import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

class HelloDataFetcherTest {

    @Mock
    private SlingDataFetcherEnvironment environment;

    private HelloDataFetcher fetcher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fetcher = new HelloDataFetcher();
    }

    @Test
    void testGetReturnsWelcomeMessage() throws Exception {
        String result = fetcher.get(environment);
        
        assertNotNull(result);
        assertTrue(result.contains("Apache Sling CMS"));
        assertTrue(result.contains("GraphQL API"));
    }
}
```

## Common Issues & Solutions

### Issue 1: Query Returns Null

**Problem:** Data fetcher returns `null` for a field

**Solution:** Check:
1. Fetcher name matches `@fetcher(name: "...")` in schema
2. Fetcher is registered as OSGi service
3. Fetcher returns correct type (String, Map, List)

### Issue 2: Schema Not Found

**Problem:** `No schema provider found for resource`

**Solution:**
- Verify `SlingCmsSchemaProvider` is deployed
- Check OSGi console: `http://localhost:8082/system/console/components`
- Look for `org.apache.sling.cms.graphql.internal.schema.SlingCmsSchemaProvider`

### Issue 3: GraphiQL Not Loading

**Problem:** GraphiQL shows blank page

**Solution:**
1. Check browser console for JavaScript errors
2. Verify `/graphql` resource exists in JCR
3. Check `sling:resourceType=sling-cms/servlet`
4. Clear browser cache

### Issue 4: 500 Error on Query

**Problem:** GraphQL servlet returns HTTP 500

**Solution:**
1. Check logs: `logs/error.log`
2. Look for data fetcher exceptions
3. Verify arguments match schema types
4. Add try-catch to data fetcher

## Next Steps

1. **Add Authentication** - Implement permission checks in data fetchers
2. **Add CMS Queries** - Pages, assets, navigation, search
3. **Add Caching** - Use DataLoader for N+1 query optimization
4. **Add Tests** - Unit tests for all data fetchers
5. **Monitor Performance** - Add metrics and logging

## Related Documentation

- [graphql-module-review.md](./graphql-module-review.md) - Architecture review and findings
- [graphql-api-reference.md](./graphql-api-reference.md) - Complete API reference
- [Apache Sling GraphQL Core](https://github.com/apache/sling-org-apache-sling-graphql-core) - Upstream project

---

**Document Version:** 1.0  
**Last Updated:** January 11, 2026  
**Status:** ✅ Current and Accurate
