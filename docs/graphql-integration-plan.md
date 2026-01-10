# GraphQL Integration for Apache Sling CMS Personalization Module

## Overview

This document outlines the approach to integrate GraphQL API support for the personalization module, enabling clients to query segments, variants, and personalization data via GraphQL.

## Architecture Options

### Option 1: Apache Sling GraphQL Core (Recommended)
Use the official Apache Sling GraphQL Core framework which provides:
- Schema-driven GraphQL implementation
- Integration with Sling Resource Model
- Built-in authentication/authorization
- OSGi-compliant architecture

**Maven Dependencies:**
```xml
<!-- Apache Sling GraphQL Core -->
<dependency>
    <groupId>org.apache.sling</groupId>
    <artifactId>org.apache.sling.graphql.core</artifactId>
    <version>0.0.24</version>
    <scope>provided</scope>
</dependency>

<!-- GraphQL Java -->
<dependency>
    <groupId>com.graphql-java</groupId>
    <artifactId>graphql-java</artifactId>
    <version>21.5</version>
    <scope>provided</scope>
</dependency>
```

### Option 2: Standalone GraphQL Java
Implement GraphQL using graphql-java directly with custom servlets (more control, more work).

## Recommended Approach: Apache Sling GraphQL Core

### 1. Dependencies Setup

**personalization/pom.xml:**
```xml
<!-- Add to dependencies section -->
<dependency>
    <groupId>org.apache.sling</groupId>
    <artifactId>org.apache.sling.graphql.core</artifactId>
    <version>0.0.24</version>
    <scope>provided</scope>
</dependency>

<dependency>
    <groupId>com.graphql-java</groupId>
    <artifactId>graphql-java</artifactId>
    <version>21.5</version>
    <scope>provided</scope>
</dependency>
```

**feature/src/main/features/personalization.json:**
```json
{
  "bundles": [
    {
      "id": "org.apache.sling:org.apache.sling.cms.personalization:${cms-version}",
      "start-order": "20"
    },
    {
      "id": "org.apache.sling:org.apache.sling.graphql.core:0.0.24",
      "start-order": "20"
    },
    {
      "id": "com.graphql-java:graphql-java:21.5",
      "start-order": "15"
    }
  ],
  // ... rest of configuration
}
```

### 2. GraphQL Schema Definition

Create schema at:
**personalization/src/main/resources/SLING-INF/graphql/personalization.graphqls**

```graphql
# Personalization GraphQL Schema

"""
A personalization segment that can be evaluated against a request
"""
type Segment {
  """Segment identifier"""
  id: ID!
  
  """Display name of the segment"""
  name: String!
  
  """Description of what this segment represents"""
  description: String
  
  """Evaluator type (device, cookie, userGroup, path, etc.)"""
  type: String!
  
  """Segment evaluation rules as JSON"""
  rules: JSON!
  
  """Segment priority (higher = more important)"""
  priority: Int
  
  """Whether this segment is active"""
  active: Boolean!
  
  """Resource path in repository"""
  path: String!
}

"""
A content variant for personalized content
"""
type Variant {
  """Variant identifier"""
  id: ID!
  
  """Display name of the variant"""
  name: String!
  
  """Segment IDs that should see this variant"""
  segments: [String!]!
  
  """Content resource path"""
  contentPath: String!
  
  """Whether this is the default/fallback variant"""
  isDefault: Boolean!
  
  """Variant priority"""
  priority: Int
  
  """Resource path in repository"""
  path: String!
}

"""
Personalization evaluation result
"""
type PersonalizationResult {
  """Matched segments for current request"""
  matchedSegments: [Segment!]!
  
  """Selected variant based on segment matching"""
  selectedVariant: Variant
  
  """All available variants"""
  availableVariants: [Variant!]!
}

"""
Custom JSON scalar type for flexible data structures
"""
scalar JSON

"""
Query operations for personalization
"""
type Query {
  """Get all available segments"""
  segments(
    """Filter by segment type"""
    type: String
    
    """Filter by active status"""
    active: Boolean
  ): [Segment!]!
  
  """Get a specific segment by ID"""
  segment(id: ID!): Segment
  
  """Get all variants for a content path"""
  variants(
    """Content path to get variants for"""
    path: String!
  ): [Variant!]!
  
  """Get a specific variant by ID"""
  variant(id: ID!): Variant
  
  """Evaluate personalization for current request"""
  evaluatePersonalization(
    """Content path to evaluate"""
    path: String!
    
    """Cookie values for evaluation (key-value pairs)"""
    cookies: JSON
    
    """Request headers for evaluation"""
    headers: JSON
  ): PersonalizationResult!
  
  """Get available segment evaluator types"""
  availableEvaluatorTypes: [String!]!
}

"""
Mutation operations for personalization (admin only)
"""
type Mutation {
  """Create a new segment"""
  createSegment(
    """Segment name"""
    name: String!
    
    """Evaluator type"""
    type: String!
    
    """Evaluation rules"""
    rules: JSON!
    
    """Segment description"""
    description: String
    
    """Segment priority"""
    priority: Int
  ): Segment!
  
  """Update an existing segment"""
  updateSegment(
    """Segment ID to update"""
    id: ID!
    
    """New segment data"""
    name: String
    rules: JSON
    description: String
    priority: Int
    active: Boolean
  ): Segment!
  
  """Delete a segment"""
  deleteSegment(id: ID!): Boolean!
  
  """Create a new variant"""
  createVariant(
    """Parent content path"""
    contentPath: String!
    
    """Variant name"""
    name: String!
    
    """Segment IDs"""
    segments: [String!]!
    
    """Whether this is the default variant"""
    isDefault: Boolean
  ): Variant!
}
```

### 3. Data Fetchers Implementation

Create data fetchers in:
**personalization/src/main/java/org/apache/sling/cms/personalization/graphql/**

**SegmentDataFetcher.java:**
```java
package org.apache.sling.cms.personalization.graphql;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.personalization.PersonalizationService;
import org.apache.sling.graphql.api.SlingDataFetcher;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GraphQL data fetcher for segments query
 */
@Component(service = SlingDataFetcher.class)
public class SegmentDataFetcher implements SlingDataFetcher<List<Map<String, Object>>> {

    private static final Logger log = LoggerFactory.getLogger(SegmentDataFetcher.class);

    @Reference
    private PersonalizationService personalizationService;

    @Override
    @NotNull
    public String getName() {
        return "personalization/segments";
    }

    @Override
    public List<Map<String, Object>> get(@NotNull SlingDataFetchingEnvironment env) throws Exception {
        ResourceResolver resolver = env.getCurrentResource().getResourceResolver();
        String type = env.getArgument("type");
        Boolean active = env.getArgument("active");

        // Query segments from repository
        String query = buildSegmentQuery(type, active);
        
        List<Map<String, Object>> segments = new ArrayList<>();
        
        resolver.findResources(query, "JCR-SQL2").forEachRemaining(resource -> {
            segments.add(resourceToSegmentMap(resource));
        });

        log.debug("Found {} segments", segments.size());
        return segments;
    }

    private String buildSegmentQuery(String type, Boolean active) {
        StringBuilder query = new StringBuilder(
            "SELECT * FROM [sling:Folder] WHERE ISDESCENDANTNODE('/etc/personalization/segments')"
        );
        
        if (type != null) {
            query.append(" AND [evaluatorType]='").append(type).append("'");
        }
        
        if (active != null) {
            query.append(" AND [active]=").append(active);
        }
        
        query.append(" ORDER BY [priority] DESC, [name]");
        return query.toString();
    }

    private Map<String, Object> resourceToSegmentMap(Resource resource) {
        Map<String, Object> segment = new HashMap<>();
        segment.put("id", resource.getName());
        segment.put("name", resource.getValueMap().get("name", String.class));
        segment.put("description", resource.getValueMap().get("description", String.class));
        segment.put("type", resource.getValueMap().get("evaluatorType", String.class));
        segment.put("priority", resource.getValueMap().get("priority", 0));
        segment.put("active", resource.getValueMap().get("active", true));
        segment.put("path", resource.getPath());
        
        // Convert rules to map
        Map<String, Object> rules = new HashMap<>();
        Resource rulesResource = resource.getChild("rules");
        if (rulesResource != null) {
            rulesResource.getValueMap().forEach((key, value) -> {
                if (!key.startsWith("jcr:")) {
                    rules.put(key, value);
                }
            });
        }
        segment.put("rules", rules);
        
        return segment;
    }
}
```

**EvaluatePersonalizationDataFetcher.java:**
```java
package org.apache.sling.cms.personalization.graphql;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.personalization.PersonalizationService;
import org.apache.sling.graphql.api.SlingDataFetcher;
import org.apache.sling.graphql.api.SlingDataFetchingEnvironment;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GraphQL data fetcher for personalization evaluation
 */
@Component(service = SlingDataFetcher.class)
public class EvaluatePersonalizationDataFetcher implements SlingDataFetcher<Map<String, Object>> {

    @Reference
    private PersonalizationService personalizationService;

    @Override
    @NotNull
    public String getName() {
        return "personalization/evaluatePersonalization";
    }

    @Override
    public Map<String, Object> get(@NotNull SlingDataFetchingEnvironment env) throws Exception {
        SlingHttpServletRequest request = env.getRequest();
        String path = env.getArgument("path");
        
        Resource resource = env.getCurrentResource().getResourceResolver().getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }

        // Evaluate segments
        List<String> matchedSegments = personalizationService.evaluateSegments(request);
        
        // Get selected variant
        Resource selectedVariant = personalizationService.selectVariant(resource, request);

        Map<String, Object> result = new HashMap<>();
        result.put("matchedSegments", matchedSegments.stream()
            .map(segmentId -> Map.of("id", segmentId))
            .collect(Collectors.toList()));
        
        if (selectedVariant != null) {
            result.put("selectedVariant", resourceToVariantMap(selectedVariant));
        }
        
        // Get all available variants
        List<Map<String, Object>> variants = personalizationService.getVariants(resource)
            .stream()
            .map(this::resourceToVariantMap)
            .collect(Collectors.toList());
        result.put("availableVariants", variants);

        return result;
    }

    private Map<String, Object> resourceToVariantMap(Resource resource) {
        Map<String, Object> variant = new HashMap<>();
        variant.put("id", resource.getName());
        variant.put("name", resource.getValueMap().get("name", String.class));
        variant.put("contentPath", resource.getPath());
        variant.put("isDefault", resource.getValueMap().get("isDefault", false));
        variant.put("priority", resource.getValueMap().get("priority", 0));
        variant.put("path", resource.getPath());
        
        String[] segments = resource.getValueMap().get("segments", String[].class);
        variant.put("segments", segments != null ? List.of(segments) : List.of());
        
        return variant;
    }
}
```

### 4. GraphQL Endpoint Configuration

The Sling GraphQL Core automatically exposes endpoints at:
- `POST /content/{path}.gql/{schema}` - GraphQL query endpoint
- `GET /content/{path}.gql/{schema}` - GraphQL query (for GET with query param)

Configure the schema path in repository:
**Create resource structure:**
```
/conf/global/settings/graphql/
└── personalization/
    ├── jcr:primaryType: sling:Folder
    └── schema
        ├── jcr:primaryType: nt:file
        └── jcr:content
            ├── jcr:primaryType: nt:resource
            ├── jcr:mimeType: text/plain
            └── jcr:data: <personalization.graphqls content>
```

### 5. Example GraphQL Queries

**Query all segments:**
```graphql
query {
  segments(active: true) {
    id
    name
    type
    description
    priority
    rules
  }
}
```

**Query specific segment:**
```graphql
query {
  segment(id: "mobile-users") {
    id
    name
    type
    rules
    priority
  }
}
```

**Evaluate personalization:**
```graphql
query {
  evaluatePersonalization(path: "/content/mysite/homepage") {
    matchedSegments {
      id
      name
      type
    }
    selectedVariant {
      id
      name
      contentPath
      isDefault
    }
    availableVariants {
      id
      name
      segments
    }
  }
}
```

**Get variants for content:**
```graphql
query {
  variants(path: "/content/mysite/homepage") {
    id
    name
    segments
    isDefault
    priority
  }
}
```

**Create segment (admin only):**
```graphql
mutation {
  createSegment(
    name: "Premium Users"
    type: "cookie"
    rules: {
      cookieName: "subscription"
      cookieValue: "premium"
    }
    priority: 10
  ) {
    id
    name
    type
  }
}
```

### 6. Security Configuration

**Add ACLs to personalization.json repoinit:**
```json
{
  "repoinit:TEXT|true": [
    "# ... existing repoinit ...",
    "",
    "# GraphQL access control",
    "set ACL for anonymous",
    "  # Allow read-only GraphQL queries",
    "  allow jcr:read on /conf/global/settings/graphql/personalization",
    "end",
    "",
    "# Restrict mutations to content-authors group",
    "set ACL for content-authors",
    "  allow jcr:all on /etc/personalization",
    "  allow jcr:all on /conf/global/settings/graphql/personalization",
    "end"
  ]
}
```

### 7. Testing

**Integration Test Example:**
```java
@Test
public void testSegmentsQuery() throws Exception {
    String query = "{ segments { id name type } }";
    
    JsonNode result = executeGraphQLQuery("/content/mysite.gql/personalization", query);
    
    assertNotNull(result.get("data").get("segments"));
    assertTrue(result.get("data").get("segments").size() > 0);
}
```

### 8. Client-Side Integration

**JavaScript/React Example:**
```javascript
async function fetchPersonalization(contentPath) {
  const query = `
    query {
      evaluatePersonalization(path: "${contentPath}") {
        matchedSegments {
          id
          name
        }
        selectedVariant {
          id
          name
          contentPath
        }
      }
    }
  `;
  
  const response = await fetch('/content/mysite.gql/personalization', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ query })
  });
  
  const result = await response.json();
  return result.data.evaluatePersonalization;
}
```

## Implementation Steps

1. ✅ Add GraphQL dependencies to personalization/pom.xml
2. ✅ Update feature/personalization.json with GraphQL bundles
3. ✅ Create GraphQL schema file
4. ✅ Implement data fetchers for queries
5. ✅ Implement data fetchers for mutations (optional)
6. ✅ Configure security and ACLs
7. ✅ Deploy schema to repository
8. ✅ Write integration tests
9. ✅ Document API endpoints
10. ✅ Create client examples

## Benefits

- **Standard API**: GraphQL provides a strongly-typed, self-documenting API
- **Flexible Queries**: Clients request exactly the data they need
- **Real-time**: Support for subscriptions (future enhancement)
- **OSGi-compliant**: Uses Apache Sling GraphQL Core framework
- **Secure**: Integrates with Sling authentication/authorization
- **Performant**: Efficient data fetching with batching support

## Alternative: REST API

If GraphQL is too heavy, consider a simpler REST API:

**PersonalizationServlet.java:**
```java
@Component(service = Servlet.class, property = {
    "sling.servlet.resourceTypes=sling-cms/personalization/api",
    "sling.servlet.methods=GET",
    "sling.servlet.extensions=json"
})
public class PersonalizationServlet extends SlingSafeMethodsServlet {
    
    @Reference
    private PersonalizationService personalizationService;
    
    @Override
    protected void doGet(SlingHttpServletRequest request, 
                        SlingHttpServletResponse response) throws IOException {
        // Return JSON with segments and variants
    }
}
```

Endpoints:
- `GET /api/personalization/segments.json` - List all segments
- `GET /api/personalization/segments/{id}.json` - Get specific segment
- `GET /api/personalization/evaluate.json?path=/content/page` - Evaluate personalization

## Recommendation

**Start with REST API** for simplicity, then migrate to GraphQL if needed for:
- Complex queries with nested data
- Multiple frontend clients with different data needs
- Real-time updates via subscriptions
