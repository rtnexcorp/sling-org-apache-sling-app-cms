# Apache Sling GraphQL Integration

## Overview

Apache Sling has **official GraphQL support** through the `org.apache.sling.graphql` modules. This is different from a custom GraphQL implementation - we should use Sling's native GraphQL capabilities.

## Why Use Sling GraphQL?

1. **Native Integration**: Built specifically for Sling/JCR content
2. **Resource-Based**: GraphQL schemas live in JCR as resources
3. **HTL Templates**: Can use HTL for field resolvers
4. **Authentication**: Inherits Sling authentication
5. **Community Maintained**: Part of Apache Sling project

## Required Bundles

Based on Apache Sling Starter (version 12+), the following bundles are needed:

```json
{
  "bundles": [
    {
      "id": "org.apache.sling:org.apache.sling.graphql.core:0.0.30",
      "start-order": "20"
    }
  ]
}
```

**Note**: `org.apache.sling.graphql.core` includes:
- GraphQL Java library (embedded)
- GraphQL servlet at `/graphql.json`
- Schema resolution from JCR
- HTL-based field resolvers

## How Sling GraphQL Works

### 1. Schema Definition

GraphQL schemas are stored in JCR as `.gql` files:

```
/apps/myapp/schemas/
  ├── personalization.gql       # GraphQL schema file
  └── personalization.json      # Sling resource (nt:file)
```

Example schema (`personalization.gql`):
```graphql
type Query {
  segments: [Segment]
  segment(id: ID!): Segment
}

type Segment {
  id: ID!
  name: String!
  active: Boolean!
  priority: Int
  evaluatorType: String
}
```

### 2. GraphQL Endpoint

Once `org.apache.sling.graphql.core` is installed, GraphQL is available at:

```
http://localhost:8082/<resource-path>.graphql.json
```

Example:
```bash
# Query segments at /etc/personalization
curl -X POST http://localhost:8082/etc/personalization.graphql.json \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{"query": "{ segments { id name active } }"}'
```

### 3. Field Resolvers

Sling GraphQL can resolve fields using:

**A. HTL Scripts** (recommended for Sling CMS):
```
/apps/myapp/graphql/
  ├── Segment.name.html          # HTL template for Segment.name field
  └── Query.segments.html        # HTL template for segments query
```

Example `Segment.name.html`:
```html
${resource.name @ context='text'}
```

**B. Java Fetchers** (for complex logic):
```java
@Component(service = SlingDataFetcher.class)
public class SegmentsFetcher implements SlingDataFetcher<Object> {
    @Override
    public Object get(SlingDataFetcherEnvironment env) {
        // Fetch segments from JCR
    }
}
```

### 4. Schema Discovery

Sling GraphQL discovers schemas via:

1. **Resource Type**: Resources with `sling:resourceType=sling/graphql/schema`
2. **File Extension**: Files ending in `.gql`
3. **Path Convention**: `/apps/<app>/graphql/**/*.gql`

## Integration Steps for Sling CMS

### Step 1: Add GraphQL Bundle to Feature

Update `feature/src/main/features/personalization.json`:

```json
{
  "bundles": [
    {
      "id": "org.apache.sling:org.apache.sling.cms.personalization:${cms-version}",
      "start-order": "20"
    },
    {
      "id": "org.apache.sling:org.apache.sling.graphql.core:0.0.30",
      "start-order": "20"
    }
  ]
}
```

### Step 2: Create GraphQL Schema in JCR

Via repoinit in `personalization.json`:

```
create path (sling:Folder) /apps/sling-cms/graphql
create path (nt:file) /apps/sling-cms/graphql/personalization.gql

set properties on /apps/sling-cms/graphql/personalization.gql
  set sling:resourceType{String} to sling/graphql/schema
end
```

### Step 3: Add Schema Content

Create schema file in UI module:
```
ui/src/main/resources/jcr_root/apps/sling-cms/graphql/
  └── personalization.gql
```

### Step 4: Create HTL Field Resolvers

```
ui/src/main/resources/jcr_root/apps/sling-cms/graphql/
  ├── personalization.gql
  ├── Query.segments.html
  └── Segment.name.html
```

## Example: Complete Setup

### personalization.gql
```graphql
type Query {
  segments: [Segment]
  segment(id: ID!): Segment
  evaluators: [Evaluator]
}

type Segment {
  id: ID!
  name: String!
  description: String
  active: Boolean!
  priority: Int!
  evaluatorType: String!
  conditions: JSON
}

type Evaluator {
  type: String!
  description: String
}

scalar JSON
```

### Query.segments.html
```html
<sly data-sly-use.segments="org.apache.sling.cms.personalization.models.SegmentsList">
  [
    <sly data-sly-list.segment="${segments.items}">
      {
        "id": "${segment.id}",
        "name": "${segment.name}",
        "active": ${segment.active},
        "priority": ${segment.priority},
        "evaluatorType": "${segment.evaluatorType}"
      }<sly data-sly-test="${!segmentList.last}">,</sly>
    </sly>
  ]
</sly>
```

## Testing

Once deployed, test with:

```bash
# GraphiQL interface (if enabled)
open http://localhost:8082/etc/personalization.graphiql.html

# Query via curl
curl -X POST http://localhost:8082/etc/personalization.graphql.json \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "query": "{ segments { id name active priority } }"
  }'
```

## Advantages Over Custom Implementation

| Feature | Custom GraphQL | Sling GraphQL |
|---------|---------------|---------------|
| Setup Complexity | High (schema stitching, execution) | Low (add bundle) |
| JCR Integration | Manual | Native |
| Schema Storage | Code | JCR (editable) |
| Field Resolvers | Java only | HTL + Java |
| Authentication | Manual | Automatic |
| Resource Filtering | Manual | Built-in |
| Maintenance | Custom code | Sling project |

## Next Steps

1. Add `org.apache.sling.graphql.core` bundle to `personalization.json`
2. Create schema in `/apps/sling-cms/graphql/personalization.gql`
3. Create HTL field resolvers
4. Test with GraphQL queries

## References

- [Apache Sling GraphQL Core](https://github.com/apache/sling-org-apache-sling-graphql-core)
- [Sling GraphQL Samples](https://github.com/apache/sling-samples/tree/master/org.apache.sling.graphql.samples.website)
- [GraphQL Schema Language](https://graphql.org/learn/schema/)
