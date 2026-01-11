# GraphQL API Reference - Apache Sling CMS

> **Module:** `org.apache.sling.cms.graphql`  
> **Version:** 1.1.9-SNAPSHOT  
> **GraphQL Core:** Apache Sling GraphQL Core 0.0.24  
> **Last Updated:** January 11, 2026

## Overview

The GraphQL module provides a GraphQL API for querying Apache Sling CMS content. This reference documents the current production schema and query capabilities.

## Endpoint Information

| Property | Value |
|----------|-------|
| **Base URL** | `http://localhost:8082/graphql.json` |
| **GraphiQL UI** | `http://localhost:8082/graphql` (HTML) |
| **Methods** | `GET`, `POST` |
| **Content-Type** | `application/json` |
| **Authentication** | Required for protected queries (configurable) |

## Current Schema (v1.0)

```graphql
type Query {
  # Returns a welcome message for API discovery
  # Use this to verify the GraphQL API is working
  hello: String @fetcher(name: "slingcms/hello")
  
  # Returns server information and metadata
  # Useful for health checks and version detection
  serverInfo: ServerInfo @fetcher(name: "slingcms/serverInfo")
}

# Server metadata and version information
type ServerInfo {
  # CMS version (e.g., "1.1.9-SNAPSHOT")
  version: String
  
  # Current server timestamp in ISO 8601 format
  # Example: "2026-01-11T10:30:45-08:00"
  timestamp: String
  
  # Environment name (e.g., "development", "production")
  environment: String
  
  # GraphQL Core library version (e.g., "0.0.24")
  graphqlVersion: String
}
```

## Query Examples

### 1. Hello Query (Health Check) - Anonymous Access

**Purpose:** Verify GraphQL API is accessible and operational
**Authentication:** Not required (anonymous access allowed)

**Query:**
```graphql
{
  hello
}
```

**cURL Request (no authentication needed):**
```bash
curl -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query": "{ hello }"}'
```

**Response:**
```json
{
  "data": {
    "hello": "Apache Sling CMS GraphQL API - Version 1.1.9 | Ready to serve content queries | Use this endpoint to access CMS content, pages, assets, and metadata through GraphQL queries."
  }
}
```

**Status Codes:**
- `200 OK` - API is functional
- `404 Not Found` - GraphQL module not deployed
- `500 Internal Server Error` - Configuration issue

---

### 2. Server Info Query - Authenticated Access

**Purpose:** Get server metadata, version, and environment information
**Authentication:** Required (HTTP 401 returned if not authenticated)

**Query:**
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

**cURL Request (with authentication):**
```bash
# Authenticated request with basic auth
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query": "{ serverInfo { version timestamp environment graphqlVersion } }"}'
```

**cURL Request (without authentication - will fail):**
```bash
# Unauthenticated request - returns 401 Unauthorized
curl -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query": "{ serverInfo { version } }"}'
```

**Response (authenticated):**
```json
{
  "data": {
    "serverInfo": {
      "version": "1.1.9-SNAPSHOT",
      "timestamp": "2026-01-11T18:45:30.123Z",
      "environment": "development",
      "graphqlVersion": "0.0.24"
    }
  }
}
```

**Response (unauthenticated - 401):**
```json
{
  "errors": [
    {
      "message": "Authentication required"
    }
  ]
}
```

**Field Details:**

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `version` | String | CMS version from pom.xml | `"1.1.9-SNAPSHOT"` |
| `timestamp` | String | Current server time (ISO 8601) | `"2026-01-11T18:45:30Z"` |
| `environment` | String | Runtime environment | `"development"` |
| `graphqlVersion` | String | Sling GraphQL Core version | `"0.0.24"` |

---

### 3. Partial Field Selection

**Purpose:** Request only specific fields to reduce response size

**Query:**
```graphql
{
  serverInfo {
    version
    timestamp
  }
}
```

**Response:**
```json
{
  "data": {
    "serverInfo": {
      "version": "1.1.9-SNAPSHOT",
      "timestamp": "2026-01-11T18:45:30.123Z"
    }
  }
}
```

---

### 4. Multiple Queries in One Request

**Purpose:** Fetch multiple data points in a single HTTP request

**Query:**
```graphql
{
  hello
  serverInfo {
    version
    environment
  }
}
```

**Response:**
```json
{
  "data": {
    "hello": "Apache Sling CMS GraphQL API - Version 1.1.9 | Ready to serve content queries...",
    "serverInfo": {
      "version": "1.1.9-SNAPSHOT",
      "environment": "development"
    }
  }
}
```

---

### 5. GET Request (URL-Encoded Query)

**Purpose:** Use GET method for caching and browser bookmarks

**cURL Request:**
```bash
curl -G http://localhost:8082/graphql.json \
  --data-urlencode "query={ serverInfo { version } }"
```

**Browser URL:**
```
http://localhost:8082/graphql.json?query=%7B%20serverInfo%20%7B%20version%20%7D%20%7D
```

**Response:** (same as POST)

---

## Error Responses

### Syntax Error

**Query:**
```graphql
{
  hello(
}
```

**Response:**
```json
{
  "errors": [
    {
      "message": "Invalid Syntax : offending token '}'",
      "locations": [
        {
          "line": 2,
          "column": 3
        }
      ],
      "extensions": {
        "classification": "InvalidSyntax"
      }
    }
  ]
}
```

### Unknown Field

**Query:**
```graphql
{
  unknownField
}
```

**Response:**
```json
{
  "errors": [
    {
      "message": "Validation error of type FieldUndefined: Field 'unknownField' in type 'Query' is undefined",
      "locations": [
        {
          "line": 2,
          "column": 3
        }
      ],
      "extensions": {
        "classification": "ValidationError"
      }
    }
  ]
}
```

### Data Fetcher Exception

**Scenario:** Data fetcher throws an exception

**Response:**
```json
{
  "errors": [
    {
      "message": "Exception while fetching data (/serverInfo) : NullPointerException",
      "path": ["serverInfo"],
      "locations": [
        {
          "line": 2,
          "column": 3
        }
      ],
      "extensions": {
        "classification": "DataFetchingException"
      }
    }
  ],
  "data": {
    "serverInfo": null
  }
}
```

## Authentication & Authorization

The GraphQL API uses a configurable security system with per-query authentication requirements.

### Security Architecture

The security is implemented through:

1. **GraphQLSecurityFilter** - Servlet filter that intercepts GraphQL requests
2. **GraphQLSecurityService** - OSGi service providing authentication/authorization checks
3. **Per-query validation** - Data fetchers can enforce query-specific security

### Query Security Levels

| Query | Authentication | Description |
|-------|----------------|-------------|
| `hello` | **Not required** | Anonymous access allowed (health check) |
| `serverInfo` | **Required** | Must be authenticated |

### OSGi Configuration

#### GraphQL Security Service Configuration

Configure via OSGi console or `.cfg.json` file:

**Service PID:** `org.apache.sling.cms.graphql.internal.security.GraphQLSecurityService`

| Property | Default | Description |
|----------|---------|-------------|
| `enableAuthentication` | `true` | Require authentication for GraphQL queries |
| `allowedAnonymousQueries` | `["hello"]` | Queries that don't require authentication |
| `requiredGroups` | `[]` | User groups with API access (empty = all authenticated users) |

**Example Configuration:**
```json
{
  "enableAuthentication": true,
  "allowedAnonymousQueries": ["hello", "healthCheck"],
  "requiredGroups": ["graphql-users", "administrators"]
}
```

#### GraphQL Security Filter Configuration

**Service PID:** `org.apache.sling.cms.graphql.internal.filters.GraphQLSecurityFilter`

| Property | Default | Description |
|----------|---------|-------------|
| `enabled` | `true` | Enable the security filter |
| `graphqlPaths` | `["/graphql"]` | URL patterns to protect |

### Authentication Examples

#### Authenticated Request (Basic Auth)

```bash
# Using basic authentication
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query": "{ serverInfo { version timestamp } }"}'
```

#### Authenticated Request (Session Cookie)

```bash
# First, login to get session cookie
curl -c cookies.txt -X POST http://localhost:8080/system/sling/form/login \
  -d "j_username=admin&j_password=admin"

# Then use session cookie for GraphQL requests
curl -b cookies.txt -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query": "{ serverInfo { version } }"}'
```

#### Anonymous Request (Allowed Queries Only)

```bash
# No authentication needed for 'hello' query
curl -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query": "{ hello }"}'
```

### Error Responses

#### 401 Unauthorized

Returned when authentication is required but not provided:

```json
{
  "errors": [
    {
      "message": "Authentication required"
    }
  ]
}
```

#### 403 Forbidden

Returned when user is authenticated but lacks required group membership:

```json
{
  "errors": [
    {
      "message": "Insufficient permissions to access GraphQL API"
    }
  ]
}
```

### Group-Based Access Control

To restrict API access to specific groups:

1. Configure `requiredGroups` in GraphQL Security Service
2. Add users to one of the specified groups
3. Only users in those groups can access protected queries

**Example:** Restrict to `graphql-api-users` group:
```json
{
  "enableAuthentication": true,
  "requiredGroups": ["graphql-api-users"]
}
```

## GraphiQL Interface

### Access

Open in browser: `http://localhost:8082/graphql`

### Features

- 🔍 **Autocomplete** - Press `Ctrl+Space` for suggestions
- 📖 **Docs Explorer** - View schema documentation
- 💾 **History** - Access previous queries
- ✨ **Prettify** - Format queries automatically
- ▶️ **Execute** - Run queries with `Ctrl+Enter`

### Sample Session

1. Open GraphiQL: `http://localhost:8082/graphql`
2. Type in the editor:
   ```graphql
   {
     serverInfo {
       version
     }
   }
   ```
3. Click "Execute Query" (or press `Ctrl+Enter`)
4. View response in right panel

## Client Examples

### JavaScript (Fetch API)

```javascript
async function queryGraphQL(query) {
  const response = await fetch('http://localhost:8082/graphql.json', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ query })
  });
  
  const data = await response.json();
  return data;
}

// Usage
const result = await queryGraphQL('{ serverInfo { version } }');
console.log(result.data.serverInfo.version);
```

### JavaScript (Axios)

```javascript
const axios = require('axios');

async function queryGraphQL(query) {
  const response = await axios.post('http://localhost:8082/graphql.json', {
    query: query
  });
  
  return response.data;
}

// Usage
const result = await queryGraphQL('{ hello }');
console.log(result.data.hello);
```

### Java (OkHttp)

```java
import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GraphQLClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    
    public Map<String, Object> query(String query) throws IOException {
        Map<String, String> body = Map.of("query", query);
        String json = mapper.writeValueAsString(body);
        
        Request request = new Request.Builder()
            .url("http://localhost:8082/graphql.json")
            .post(RequestBody.create(json, JSON))
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            return mapper.readValue(response.body().string(), Map.class);
        }
    }
}

// Usage
GraphQLClient client = new GraphQLClient();
Map<String, Object> result = client.query("{ serverInfo { version } }");
```

### Python (Requests)

```python
import requests

def query_graphql(query):
    response = requests.post(
        'http://localhost:8082/graphql.json',
        json={'query': query}
    )
    return response.json()

# Usage
result = query_graphql('{ serverInfo { version environment } }')
print(result['data']['serverInfo']['version'])
```

## Schema Introspection

### Get Full Schema

**Query:**
```graphql
{
  __schema {
    types {
      name
      kind
      description
    }
  }
}
```

### Get Query Type

**Query:**
```graphql
{
  __schema {
    queryType {
      name
      fields {
        name
        type {
          name
          kind
        }
      }
    }
  }
}
```

### Get Type Details

**Query:**
```graphql
{
  __type(name: "ServerInfo") {
    name
    kind
    fields {
      name
      type {
        name
        kind
      }
    }
  }
}
```

## Performance Considerations

### Response Times (Expected)

| Query | Size | Time (ms) | Notes |
|-------|------|-----------|-------|
| `{ hello }` | ~100 bytes | < 50ms | Constant string |
| `{ serverInfo {...} }` | ~200 bytes | < 100ms | Timestamp generation |

### Optimization Tips

1. **Request only needed fields** - GraphQL allows field selection
2. **Use GET for cacheable queries** - Browser/CDN caching
3. **Batch queries** - Multiple queries in one request
4. **Monitor query complexity** - Avoid deep nesting (when available)

## Limitations

### Current Version (1.0) Limitations

1. **No Mutations** - Read-only API (queries only)
2. **No Subscriptions** - Real-time updates not supported
3. **No CMS Data** - Only sample queries (hello, serverInfo)
4. **No Pagination** - List queries return all results
5. **No Filtering** - No built-in filter arguments
6. **No DataLoader** - No N+1 query optimization

### Planned Features (Future Versions)

- Page queries (by path, type, template)
- Asset queries (by path, mimetype, metadata)
- Navigation queries
- Search integration
- Mutations for content updates
- Subscriptions for live updates

## Extending the API

See [graphql-current-implementation.md](./graphql-current-implementation.md) for how to add custom queries and types.

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| `404 Not Found` | GraphQL module not deployed | Deploy `graphql` module |
| `500 Server Error` | Data fetcher exception | Check logs at `logs/error.log` |
| `Empty response` | Wrong endpoint | Use `/graphql.json` not `/graphql` |
| `CORS error` | Cross-origin request blocked | Configure CORS in servlet |

### Debug Mode

Enable debug logging:

```
org.apache.sling.cms.graphql=DEBUG
org.apache.sling.graphql.core=DEBUG
```

Add to `sling/logs/org.apache.sling.commons.log.LogManager.factory.config-graphql.cfg.json`

## Related Documentation

- [graphql-module-review.md](./graphql-module-review.md) - Architecture review
- [graphql-current-implementation.md](./graphql-current-implementation.md) - Implementation guide
- [Apache Sling GraphQL Core Docs](https://github.com/apache/sling-org-apache-sling-graphql-core)

---

**Document Version:** 2.0  
**Last Updated:** January 11, 2026  
**Status:** ✅ Reflects Current Implementation
```graphql
{
  segments(filter: { active: true }) {
    id
    name
    active
  }
}
```

**Request:**
```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ segments(filter: { active: true }) { id name active } }"
  }'
```

### 5. Filter Segments by Evaluator Type

**Query:**
```graphql
{
  segments(filter: { evaluatorType: "device" }) {
    id
    name
    evaluatorType
  }
}
```

**Request:**
```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ segments(filter: { evaluatorType: \"device\" }) { id name evaluatorType } }"
  }'
```

### 6. Evaluate Personalization for Content Path

**Query:**
```graphql
{
  evaluate(path: "/content/my-site/home") {
    path
    matchedSegments
    selectedVariant {
      path
      name
      segments
    }
  }
}
```

**Request:**
```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ evaluate(path: \"/content/my-site/home\") { path matchedSegments selectedVariant { path name segments } } }"
  }'
```

**Response:**
```json
{
  "data": {
    "evaluate": {
      "path": "/content/my-site/home",
      "matchedSegments": ["mobile-users", "returning-visitors"],
      "selectedVariant": {
        "path": "/content/my-site/home/variants/mobile",
        "name": "mobile",
        "segments": ["mobile-users"]
      }
    }
  }
}
```

## GraphQL Introspection

You can introspect the GraphQL schema using the special `__schema` query:

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ __schema { types { name } } }"
  }'
```

## Error Handling

GraphQL returns errors in the response:

```json
{
  "data": null,
  "errors": [
    {
      "message": "Resource not found: /content/invalid-path",
      "locations": [{"line": 1, "column": 3}],
      "path": ["evaluate"]
    }
  ]
}
```

## GET Requests (Optional)

For simple queries, you can use GET requests with the query as a URL parameter:

```bash
curl -u admin:admin -G http://localhost:8082/graphql.json \
  --data-urlencode 'query={ evaluators { type } }'
```

## Integration with GraphQL Clients

### Apollo Client (JavaScript)

```javascript
import { ApolloClient, InMemoryCache, HttpLink } from '@apollo/client';

const client = new ApolloClient({
  link: new HttpLink({
    uri: 'http://localhost:8082/graphql.json',
    headers: {
      authorization: 'Basic ' + btoa('admin:admin')
    }
  }),
  cache: new InMemoryCache()
});
```

### GraphQL Code Generator

Use GraphQL Code Generator to generate TypeScript types from the schema:

```bash
npx graphql-codegen init
```

## Testing with GraphQL Playground

You can use GraphQL Playground or GraphiQL for interactive testing. Add these as browser bookmarks:

- **GraphiQL:** https://github.com/graphql/graphiql
- **GraphQL Playground:** https://github.com/graphql/graphql-playground

Configure them to point to `http://localhost:8082/graphql.json` with Basic Auth.

## See Also

- [REST API Reference](./personalization-rest-api.md) - Alternative REST API
- [GraphQL Integration Plan](./graphql-integration-plan.md) - Implementation details
- [GraphQL Quick Start](./graphql-quick-start.md) - 10-minute setup guide
