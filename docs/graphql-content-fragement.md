Summary
The plan proposes a generic, scalable GraphQL architecture for content fragments:

Key Design Choices:

Generic fetcher approach - One set of fetchers works with any schema type (article, product, future types) via a schemaType parameter
Key-value field structure - Fields returned as [{name, value, type, multiple, values}] array for flexibility
Full query capabilities - Filtering by field values, sorting, both cursor and offset pagination, full-text search

## How to test the GraphQL content fragment queries

### 1) Deploy the updated GraphQL bundle

If you're running the author instance on port **8082**:

```bash
mvn clean install -P autoInstallBundle -pl graphql -DskipTests -Dbnd.baseline.skip=true
```

### 2) Send queries to `/graphql.json`

All examples below use the default author credentials (`admin:admin`) and POST JSON payloads.

> Endpoint
> `http://localhost:8082/graphql.json`

#### A. Fetch a single fragment by id/path

Query name: `fragment`

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ fragment(id: \"/content/fragments/articles/introduction-to-apache-sling\") { id schemaId title fields { name value type } } }"}'
```

#### B. List fragments (supports schemaType/path/filters/sort/pagination)

Query name: `fragments`

Minimal example (first 5):

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ fragments(schemaType: \"article\", limit: 5, offset: 0) { totalCount nodes { id title } pageInfo { hasNextPage endCursor } } }"}'
```

Example with `path` restriction:

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ fragments(schemaType: \"article\", path: \"/content/fragments/articles\", limit: 5) { totalCount nodes { id title } } }"}'
```

Example with sort:

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ fragments(schemaType: \"article\", sortField: \"jcr:title\", sortDirection: DESC, limit: 5) { nodes { id title } } }"}'
```

Example pagination using `offset`:

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

#### C. Full-text search fragments

Query name: `fragmentSearch`

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ fragmentSearch(searchTerm: \"apache sling\", schemaType: \"article\", limit: 5, offset: 0) { totalCount nodes { id title } } }"}'
```

Notes:
- If `searchTerm` is blank, the fetcher returns an empty connection (no auth check is performed).

#### D. List available content schemas

Query name: `schemas`

```bash
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ schemas { id title description } }"}'
```

## Available Content Fragments

### Article Schema (schemaId: "article")
**Location**: `/content/fragments/articles/`

Available articles (12 total):
- `welcome-to-sling-cms.json` - Welcome to Sling CMS (original)
- `introduction-to-apache-sling.json` - Introduction to Apache Sling
- `osgi-fundamentals.json` - OSGi Fundamentals for Content Management
- `jcr-repository-basics.json` - JCR Repository Basics
- `sling-resource-resolution.json` - Sling Resource Resolution
- `osgi-services-in-sling.json` - OSGi Services in Sling Applications
- `jcr-node-types-content-modeling.json` - JCR Node Types and Content Modeling
- `sling-models-component-development.json` - Sling Models and Component Development
- `osgi-configuration-management.json` - OSGi Configuration Management
- `jcr-queries-and-search.json` - JCR Queries and Search
- `sling-scripting-and-htl.json` - Sling Scripting and HTL
- `hello.json` - Sample hello article

### Product Schema (schemaId: "product")
**Location**: `/content/fragments/products/`

Available products (8 total):
- `sling-cms-starter-tshirt.json` - Sling CMS Starter T-Shirt ($19.99)
- `apache-sling-developer-guide.json` - Apache Sling Developer Guide ($49.99)
- `osgi-fundamentals-training.json` - OSGi Fundamentals Training Course ($299.99)
- `jcr-content-repository-guide.json` - JCR Content Repository Guide ($39.99)
- `sling-cms-developer-hoodie.json` - Sling CMS Developer Hoodie ($45.99)
- `apache-sling-conference-ticket.json` - Apache Sling Developer Conference Ticket ($199.99)
- `sling-cms-consulting-package.json` - Sling CMS Consulting Package ($2500.00)
- `sling-cms-premium-support.json` - Sling CMS Premium Support Subscription ($999.99)

Files to Create:

4 DTOs - ContentFragmentDTO, FieldValueDTO, FragmentConnectionDTO, PageInfoDTO
1 Query Service - ContentFragmentQueryService (handles JCR queries, field mapping, pagination)
4 Data Fetchers - Single fragment, list with filters, search, schema introspection
GraphQL Schema updates - New types, queries, inputs, and enums
4 Test classes - Comprehensive unit tests
GraphQL Queries Provided:

contentFragment(id) - Get single fragment by path
contentFragments(schemaType, filters, sort, pagination) - List with filtering
searchFragments(query, schemaType, pagination) - Full-text search
contentSchemas - List available schemas
The plan includes verification steps with curl commands and test instructions.

Claude’s Plan
GraphQL Content Fragment Fetchers - Implementation Plan
Overview
Implement a generic, scalable GraphQL API for content fragments that works with any schema type (article, product, and future schemas) with full query capabilities including filtering, sorting, pagination, and search.

Design Decisions
Generic fetcher: One set of fetchers works with all schema types via schemaType parameter
Key-value fields: Fields returned as [{name, value, type, multiple, values}] array
Full query features: Filtering by field, sorting, cursor/offset pagination, full-text search
Files to Create
1. DTOs (Data Transfer Objects)
Location: graphql/src/main/java/org/apache/sling/cms/graphql/internal/dto/

File	Purpose
ContentFragmentDTO.java	Fragment with id, name, schemaId, title, timestamps, fields array
FieldValueDTO.java	Field value: name, value, type, multiple, values[]
FragmentConnectionDTO.java	Paginated response: nodes, pageInfo, totalCount
PageInfoDTO.java	Pagination: hasNextPage, hasPreviousPage, cursors
2. Query Service
Location: graphql/src/main/java/org/apache/sling/cms/graphql/internal/services/

File	Purpose
ContentFragmentQueryService.java	JCR queries, field mapping, pagination logic
Key methods:

getFragmentById(resolver, fragmentId) → single fragment
listFragments(resolver, schemaType, pathFilter, filters, sort, pagination) → paginated list
searchFragments(resolver, searchTerm, schemaType, pagination) → full-text search
3. Data Fetchers
Location: graphql/src/main/java/org/apache/sling/cms/graphql/internal/fetchers/

File	GraphQL Query	Purpose
ContentFragmentDataFetcher.java	contentFragment(id: String!)	Get single fragment by path
ContentFragmentsDataFetcher.java	contentFragments(schemaType!, filters, sort, pagination)	List with filtering
SearchFragmentsDataFetcher.java	searchFragments(query!, schemaType, pagination)	Full-text search
SchemasDataFetcher.java	contentSchemas	List available schemas
4. GraphQL Schema Updates
File: graphql/src/main/resources/jcr_root/apps/sling-cms/servlet/GQLschema.gql

Add:


# Queries
contentFragment(id: String!): ContentFragment
contentFragments(schemaType: String!, pathFilter: String, filters: [FragmentFilterInput!],
                 sortField: String, sortDirection: SortDirection, limit: Int, offset: Int, after: String): FragmentConnection!
searchFragments(query: String!, schemaType: String, limit: Int, offset: Int): FragmentConnection!
contentSchemas: [ContentSchema!]!

# Types
type ContentFragment { id, name, schemaId, title, created, modified, createdBy, modifiedBy, fields }
type FieldValue { name, value, type, multiple, values }
type FragmentConnection { nodes, pageInfo, totalCount }
type PageInfo { hasNextPage, hasPreviousPage, startCursor, endCursor }
type ContentSchema { id, title, description, version, enabled, fields }
type SchemaFieldDef { name, label, description, type, required, multiple, order, options }

# Input/Enum
input FragmentFilterInput { fieldName, operator, value }
enum SortDirection { ASC, DESC }
enum FilterOperator { EQ, NE, CONTAINS, GT, GTE, LT, LTE, IS_NULL, IS_NOT_NULL }
5. Unit Tests
Location: graphql/src/test/java/org/apache/sling/cms/graphql/internal/

File	Tests
fetchers/ContentFragmentDataFetcherTest.java	Single fragment fetcher
fetchers/ContentFragmentsDataFetcherTest.java	List with filters, sort, pagination
fetchers/SearchFragmentsDataFetcherTest.java	Full-text search
services/ContentFragmentQueryServiceTest.java	Query building, field serialization, pagination
Implementation Details
Field Type Serialization
FieldType	GraphQL Value
STRING, TEXT, RICHTEXT	As-is
BOOLEAN	"true"/"false"
INTEGER, DECIMAL	String representation
DATE	ISO-8601 (YYYY-MM-DD)
DATETIME	ISO-8601 with time
REFERENCE, ASSET	JCR path
TAG (multiple)	values[] array
SELECT, RADIO	Selected value
CHECKBOX	values[] array
JCR Query Pattern

SELECT * FROM [nt:unstructured] AS f
WHERE ISDESCENDANTNODE(f, '/content/fragments')
  AND f.[sling:resourceType] = 'sling-cms/components/cms/fragment'
  AND f.[schemaId] = 'article'
  AND CONTAINS(f.*, 'search term')  -- for search
ORDER BY f.[publishDate] DESC
Pagination
Offset: limit: 10, offset: 20 for page navigation
Cursor: after: "Y3Vyc29yOjIw" (Base64-encoded offset) for infinite scroll
Max limit: 100 results per request
Security
All queries validate authentication via GraphQLSecurityService
Each fragment path validated via validateReadAccess(resolver, path)
Inputs escaped via Text.escapeIllegalXpathSearchChars()
Example Queries
Get Single Fragment

```graphql
{
  fragment(id: "/content/fragments/articles/introduction-to-apache-sling") {
    id
    schemaId
    title
    fields { name value type }
  }
}
```

Get Single Product

```graphql
{
  fragment(id: "/content/fragments/products/apache-sling-developer-guide") {
    id
    schemaId
    title
    fields { name value type }
  }
}
```

List with Filtering

{
  fragments(
    schemaType: "article"
    filters: [{fieldName: "category", operator: EQ, value: "technology"}]
    sortField: "publishDate"
    sortDirection: DESC
    limit: 10
  ) {
    totalCount
    nodes { id title fields { name value } }
    pageInfo { hasNextPage endCursor }
  }
}
Full-Text Search

{
  fragmentSearch(searchTerm: "apache sling", schemaType: "article", limit: 10, offset: 0) {
    totalCount
    nodes { id title }
  }
}
Implementation Sequence
Create DTOs (ContentFragmentDTO, FieldValueDTO, etc.)
Create ContentFragmentQueryService with query logic
Implement ContentFragmentDataFetcher (single fragment)
Update GQLschema.gql with types
Implement ContentFragmentsDataFetcher (list + filter + sort)
Add pagination (offset + cursor)
Implement SearchFragmentsDataFetcher
Implement SchemasDataFetcher
Write unit tests
Run mvn spotless:apply and verify build
Verification
Build & Deploy

mvn clean install -P autoInstallBundle -pl graphql -DskipTests -Dbnd.baseline.skip=true
Test Queries (curl)

# Single article fragment
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query": "{ contentFragment(id: \"/content/fragments/articles/introduction-to-apache-sling\") { id title schemaId fields { name value type } } }"}'

# Single product fragment
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query": "{ contentFragment(id: \"/content/fragments/products/apache-sling-developer-guide\") { id title schemaId fields { name value type } } }"}'

# List articles
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query": "{ contentFragments(schemaType: \"article\", limit: 5) { totalCount nodes { id title } } }"}'

# List products
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query": "{ contentFragments(schemaType: \"product\", limit: 5) { totalCount nodes { id title } } }"}'

# Search articles
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query": "{ searchFragments(query: \"sling\", schemaType: \"article\", limit: 5) { totalCount nodes { id title } } }"}'

# Search products
curl -u admin:admin -X POST http://localhost:8082/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query": "{ searchFragments(query: \"training\", schemaType: \"product\", limit: 5) { totalCount nodes { id title } } }"}'
Run Unit Tests

cd graphql && mvn test

## Current Status

✅ **Content Fragments Created**: 12 articles and 8 products are available for testing GraphQL queries once implemented
⏳ **Implementation Status**: Plan approved, ready for development

Update Todos

✅ Create sample content fragments (articles and products) for testing
⏳ Create DTOs (ContentFragmentDTO, FieldValueDTO, FragmentConnectionDTO, PageInfoDTO)
⏳ Create ContentFragmentQueryService with JCR query logic
⏳ Implement ContentFragmentDataFetcher (single fragment)
⏳ Update GQLschema.gql with new types, queries, inputs, enums
⏳ Implement ContentFragmentsDataFetcher (list with filters, sort, pagination)
⏳ Implement SearchFragmentsDataFetcher (full-text search)
⏳ Implement SchemasDataFetcher (schema introspection)
⏳ Write unit tests for all fetchers and service
⏳ Run mvn spotless:apply and verify build