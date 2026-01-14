# GraphQL API Reference - Apache Sling CMS

> **Module:** `org.apache.sling.cms.graphql`

## Overview

The GraphQL module exposes a Sling GraphQL Core endpoint backed by Sling CMS-specific schema providers and Java data fetchers.

## Endpoints

| Endpoint | Purpose |
|---|---|
| `http://localhost:8082/graphql.json` | GraphQL JSON endpoint (GET/POST) |
| `http://localhost:8082/graphql` | GraphiQL UI (HTML) |

## Current schema

> Source of truth: `graphql/src/main/resources/jcr_root/apps/sling-cms/servlet/GQLschema.gql`

```graphql
type Query {
  hello: String
  serverInfo: ServerInfo
  ping: String
  now: String

  fragment(id: String!): ContentFragment
  fragments(
    schemaType: String
    path: String
    filters: [ContentFragmentFilterInput!]
    sortField: String
    sortDirection: SortDirection
    limit: Int
    offset: Int
    cursor: String
  ): ContentFragmentConnection

  fragmentSearch(searchTerm: String!, schemaType: String, limit: Int, offset: Int): ContentFragmentConnection
  schemas: [Schema]
}
```

## Query examples

### 1) `hello` (health check)

```graphql
{ hello }
```

```bash
curl -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ hello }"}'
```

### 2) `serverInfo`

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

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ serverInfo { version timestamp environment graphqlVersion } }"}'
```

### 3) `ping` (demo)

```graphql
{ ping }
```

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ ping }"}'
```

### 4) `now` (server time)

```graphql
{ now }
```

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ now }"}'
```

### 5) Content fragments

See: `docs/graphql-content-fragement.md`

## Authentication & authorization

Security is implemented via:

1. `GraphQLSecurityFilter` (request-level filter)
2. `GraphQLSecurityService` (per-query access checks)
3. `QueryComplexityAnalyzer` (depth/complexity limits)

### Anonymous access

By default, anonymous access is allowed only for queries listed in:

- OSGi PID: `org.apache.sling.cms.graphql.internal.security.GraphQLSecurityService`
- Property: `allowedAnonymousQueries`

Default includes:

- `hello`

All other queries require authentication unless configured otherwise.

### Group-based access control

Use `requiredGroups` to restrict access to specific groups (empty = any authenticated user).

### Query complexity limiting

OSGi PID: `org.apache.sling.cms.graphql.internal.security.QueryComplexityAnalyzer`

- `maxComplexity` (default `100`)
- `maxDepth` (default `10`)

When exceeded, the endpoint returns a 400 with an error message describing the limit violation.

# Summary

The GraphQL module provides queries for **Content Fragments** and **Content Schemas**.

This document focuses on:

- Fetching a single fragment (`fragment`)
- Listing fragments with filtering/sorting/pagination (`fragments`)
- Full-text search (`fragmentSearch`)
- Listing enabled schemas (`schemas`)

## How to test

### 1) Deploy the GraphQL bundle

If you're running the author instance on port **8082**:

```bash
mvn clean install -P autoInstallBundle -pl graphql -DskipTests -Dbnd.baseline.skip=true
```

### 2) Send queries to `/graphql.json`

All examples below use the default author credentials (`admin:admin`) and POST JSON payloads.

**Endpoint**

`http://localhost:8082/graphql.json`

## Queries

### A) Fetch a single fragment by id/path

Query name: `fragment`

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ fragment(id: \"/content/fragments/articles/introduction-to-apache-sling\") { id schemaId title created modified fields { name type multiple value values } } }"}'
```

### B) List fragments (schemaType/path/filters/sort/pagination)

Query name: `fragments`

#### Minimal example

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ fragments(schemaType: \"article\", limit: 5, offset: 0) { totalCount nodes { id title } pageInfo { hasNextPage endCursor } } }"}'
```

#### Restrict by path

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ fragments(schemaType: \"article\", path: \"/content/fragments/articles\", limit: 5) { totalCount nodes { id title } } }"}'
```

#### Sort

> `sortField` is a JCR property name (examples: `jcr:title`, `jcr:created`, `jcr:lastModified`).

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ fragments(schemaType: \"article\", sortField: \"jcr:title\", sortDirection: DESC, limit: 5) { nodes { id title } } }"}'
```

#### Pagination using `offset`

```bash
# page 1
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ fragments(schemaType: \"article\", limit: 5, offset: 0) { nodes { id title } pageInfo { hasNextPage endCursor } } }"}'

# page 2
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ fragments(schemaType: \"article\", limit: 5, offset: 5) { nodes { id title } pageInfo { hasNextPage endCursor } } }"}'
```

#### Filtering

Filtering uses `ContentFragmentFilterInput`:

- `fieldName`: fragment field name
- `operator`: one of `EQ`, `NE`, `CONTAINS`, `GT`, `GTE`, `LT`, `LTE`, `IS_NULL`, `IS_NOT_NULL`
- `value`: string value (not used for null operators)

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ fragments(schemaType: \"article\", filters: [{ fieldName: \"category\", operator: EQ, value: \"technology\" }], limit: 10) { totalCount nodes { id title } } }"}'
```

### C) Full-text search fragments

Query name: `fragmentSearch`

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ fragmentSearch(searchTerm: \"apache sling\", schemaType: \"article\", limit: 5, offset: 0) { totalCount nodes { id title } } }"}'
```

**Notes**

- If `searchTerm` is blank, the fetcher returns an empty connection and skips auth checks.

### D) List available content schemas

Query name: `schemas`

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ schemas { id title description enabled fields { name title type multiple required } } }"}'
```

## Schema reference

> Source of truth: `graphql/src/main/resources/jcr_root/apps/sling-cms/servlet/GQLschema.gql`

```graphql
type Query {
  fragment(id: String!): ContentFragment

  fragments(
    schemaType: String
    path: String
    filters: [ContentFragmentFilterInput!]
    sortField: String
    sortDirection: SortDirection
    limit: Int
    offset: Int
    cursor: String
  ): ContentFragmentConnection

  fragmentSearch(
    searchTerm: String!
    schemaType: String
    limit: Int
    offset: Int
  ): ContentFragmentConnection

  schemas: [Schema]
}

type ContentFragment {
  id: String
  name: String
  schemaId: String
  title: String
  created: String
  modified: String
  createdBy: String
  modifiedBy: String
  fields: [FieldValue]
}

type FieldValue {
  name: String
  type: String
  multiple: Boolean
  value: String
  values: [String]
}

type ContentFragmentConnection {
  nodes: [ContentFragment]
  pageInfo: PageInfo
  totalCount: Int
}

type PageInfo {
  hasNextPage: Boolean
  hasPreviousPage: Boolean
  startCursor: String
  endCursor: String
}

input ContentFragmentFilterInput {
  fieldName: String!
  operator: FilterOperator!
  value: String
}

enum FilterOperator {
  EQ
  NE
  CONTAINS
  GT
  GTE
  LT
  LTE
  IS_NULL
  IS_NOT_NULL
}

enum SortDirection {
  ASC
  DESC
}

type Schema {
  id: String
  title: String
  description: String
  enabled: Boolean
  fields: [SchemaField]
}

type SchemaField {
  name: String
  title: String
  description: String
  type: String
  multiple: Boolean
  required: Boolean
}
```

