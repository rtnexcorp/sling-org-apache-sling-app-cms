# GraphQL API Reference - Apache Sling CMS Personalization

## Overview

The personalization module provides a GraphQL API at `/bin/graphql` for querying segments, variants, and evaluating personalization.

## GraphQL Endpoint

**Base URL:** `http://localhost:8082/bin/graphql`

**Authentication:** HTTP Basic Auth (admin/admin for local development)

**Content-Type:** `application/json`

## GraphQL Schema

```graphql
type Query {
    segments(filter: SegmentFilter): [Segment!]!
    segment(id: ID!): Segment
    evaluators: [Evaluator!]!
    evaluate(path: String!): EvaluationResult!
}

type Segment {
    id: ID!
    name: String!
    path: String!
    active: Boolean!
    priority: Int!
    evaluatorType: String!
    conditions: [Condition!]!
}

type Condition {
    key: String!
    value: String!
}

type Evaluator {
    type: String!
}

type EvaluationResult {
    path: String!
    matchedSegments: [String!]!
    selectedVariant: Variant
}

type Variant {
    path: String!
    name: String!
    segments: [String!]!
}

input SegmentFilter {
    active: Boolean
    evaluatorType: String
}
```

## Query Examples

### 1. Get All Evaluator Types

**Query:**
```graphql
{
  evaluators {
    type
  }
}
```

**Request:**
```bash
curl -u admin:admin -X POST http://localhost:8082/bin/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"{ evaluators { type } }"}'
```

**Response:**
```json
{
  "data": {
    "evaluators": [
      { "type": "userGroup" },
      { "type": "path" },
      { "type": "device" },
      { "type": "cookie" }
    ]
  }
}
```

### 2. Get All Segments

**Query:**
```graphql
{
  segments {
    id
    name
    active
    priority
    evaluatorType
    conditions {
      key
      value
    }
  }
}
```

**Request:**
```bash
curl -u admin:admin -X POST http://localhost:8082/bin/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ segments { id name active priority evaluatorType conditions { key value } } }"
  }'
```

**Response:**
```json
{
  "data": {
    "segments": [
      {
        "id": "mobile-users",
        "name": "Mobile Users",
        "active": true,
        "priority": 10,
        "evaluatorType": "device",
        "conditions": [
          {
            "key": "deviceType",
            "value": "mobile"
          }
        ]
      }
    ]
  }
}
```

### 3. Get Specific Segment by ID

**Query:**
```graphql
{
  segment(id: "mobile-users") {
    id
    name
    path
    active
    priority
    evaluatorType
    conditions {
      key
      value
    }
  }
}
```

**Request:**
```bash
curl -u admin:admin -X POST http://localhost:8082/bin/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ segment(id: \"mobile-users\") { id name path active priority evaluatorType conditions { key value } } }"
  }'
```

**Response:**
```json
{
  "data": {
    "segment": {
      "id": "mobile-users",
      "name": "Mobile Users",
      "path": "/etc/personalization/segments/mobile-users",
      "active": true,
      "priority": 10,
      "evaluatorType": "device",
      "conditions": [
        {
          "key": "deviceType",
          "value": "mobile"
        }
      ]
    }
  }
}
```

### 4. Filter Segments by Active Status

**Query:**
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
curl -u admin:admin -X POST http://localhost:8082/bin/graphql \
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
curl -u admin:admin -X POST http://localhost:8082/bin/graphql \
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
curl -u admin:admin -X POST http://localhost:8082/bin/graphql \
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
curl -u admin:admin -X POST http://localhost:8082/bin/graphql \
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
curl -u admin:admin -G http://localhost:8082/bin/graphql \
  --data-urlencode 'query={ evaluators { type } }'
```

## Integration with GraphQL Clients

### Apollo Client (JavaScript)

```javascript
import { ApolloClient, InMemoryCache, HttpLink } from '@apollo/client';

const client = new ApolloClient({
  link: new HttpLink({
    uri: 'http://localhost:8082/bin/graphql',
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

Configure them to point to `http://localhost:8082/bin/graphql` with Basic Auth.

## See Also

- [REST API Reference](./personalization-rest-api.md) - Alternative REST API
- [GraphQL Integration Plan](./graphql-integration-plan.md) - Implementation details
- [GraphQL Quick Start](./graphql-quick-start.md) - 10-minute setup guide
