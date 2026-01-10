# GraphQL Integration Quick Start

## TL;DR

Add GraphQL API support to the personalization module using Apache Sling GraphQL Core.

## Quick Setup (10 minutes)

### 1. Add Dependencies

**personalization/pom.xml:**
```xml
<dependency>
    <groupId>org.apache.sling</groupId>
    <artifactId>org.apache.sling.graphql.core</artifactId>
    <version>0.0.24</version>
    <scope>provided</scope>
</dependency>
```

### 2. Update Feature File

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
  ]
}
```

### 3. Create Schema

**personalization/src/main/resources/SLING-INF/graphql/personalization.graphqls:**
```graphql
type Segment {
  id: ID!
  name: String!
  type: String!
  rules: JSON!
  active: Boolean!
}

type Query {
  segments: [Segment!]!
  evaluatePersonalization(path: String!): PersonalizationResult!
}
```

### 4. Implement Data Fetcher

**Create:** `personalization/src/main/java/org/apache/sling/cms/personalization/graphql/SegmentDataFetcher.java`

```java
@Component(service = SlingDataFetcher.class)
public class SegmentDataFetcher implements SlingDataFetcher<List<Map<String, Object>>> {
    
    @Reference
    private PersonalizationService personalizationService;

    @Override
    public String getName() {
        return "personalization/segments";
    }

    @Override
    public List<Map<String, Object>> get(SlingDataFetchingEnvironment env) {
        // Fetch and return segment data
        return segments;
    }
}
```

### 5. Test Query

```bash
curl -X POST http://localhost:8080/content/mysite.gql/personalization \
  -H "Content-Type: application/json" \
  -d '{"query": "{ segments { id name type } }"}'
```

## Files to Create

```
personalization/
├── pom.xml (UPDATE - add dependencies)
├── src/main/
│   ├── java/.../personalization/graphql/
│   │   ├── SegmentDataFetcher.java (NEW)
│   │   ├── VariantDataFetcher.java (NEW)
│   │   └── EvaluatePersonalizationDataFetcher.java (NEW)
│   └── resources/SLING-INF/graphql/
│       └── personalization.graphqls (NEW - schema)

feature/src/main/features/
└── personalization.json (UPDATE - add GraphQL bundles)
```

## Example Queries

**Get all segments:**
```graphql
{
  segments {
    id
    name
    type
    priority
  }
}
```

**Evaluate personalization:**
```graphql
{
  evaluatePersonalization(path: "/content/mysite/homepage") {
    matchedSegments {
      id
      name
    }
    selectedVariant {
      name
      contentPath
    }
  }
}
```

## Full Documentation

See [graphql-integration-plan.md](./graphql-integration-plan.md) for complete implementation details.

## Decision: REST vs GraphQL

| Aspect | REST | GraphQL |
|--------|------|---------|
| **Complexity** | Low | Medium |
| **Setup Time** | 1-2 hours | 4-6 hours |
| **Flexibility** | Fixed endpoints | Client-defined queries |
| **Best For** | Simple CRUD | Complex queries, multiple clients |

**Recommendation:** Start with REST API for MVP, migrate to GraphQL when needed.
