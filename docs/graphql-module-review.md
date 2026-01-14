# GraphQL Module - Architecture Review & Findings

> **Document Version:** 1.2  
> **Review Date:** January 14, 2026

## Executive Summary

✅ **Status: PRODUCTION READY**

The GraphQL module (`org.apache.sling.cms.graphql`) provides a working GraphQL API for Apache Sling CMS, built on Apache Sling GraphQL Core **0.0.24** with a custom schema provider and Java data fetchers.

### Key Findings

| Category | Status | Notes |
|----------|--------|-------|
| **Architecture** | ✅ Good | OSGi R7 compliant, follows Sling patterns |
| **Schema Design** | ✅ Good | Diagnostics + content fragment API + schema listing |
| **Security** | ✅ Present | Request filter + per-query checks + complexity limits |
| **Testing** | ✅ Present | Unit tests exist under `graphql/src/test/java` |
| **Docs vs Code** | ⚠️ Needs Attention | `GQLschema.gql` declares `now`, but no `slingcms/now` fetcher is present in `src/main/java` |

## Module architecture

### Schema source of truth

The schema SDL is stored at:

- `graphql/src/main/resources/jcr_root/apps/sling-cms/servlet/GQLschema.gql`

At runtime it is available under:

- `/apps/sling-cms/servlet/GQLschema.gql`

### Schema provider

**File:** `graphql/src/main/java/org/apache/sling/cms/graphql/internal/schema/SlingCmsSchemaProvider.java`

The schema provider returns the SDL as a **String**, but loads it from the bundled `.gql` resource (test fallback supported). This keeps the schema readable while avoiding recursive script execution.

### Data fetchers

Current fetchers implemented in `graphql/src/main/java/.../internal/fetchers/` include:

- `slingcms/hello` (`HelloDataFetcher`)
- `slingcms/serverInfo` (`ServerInfoDataFetcher`)
- `slingcms/ping` (`PingDataFetcher`)
- `slingcms/fragment` (`ContentFragmentDataFetcher`)
- `slingcms/fragments` (`ContentFragmentsDataFetcher`)
- `slingcms/fragmentSearch` (`SearchFragmentsDataFetcher`)
- `slingcms/schemas` (`SchemasDataFetcher`)

**Note:** The schema currently also declares:

- `now: String @fetcher(name: "slingcms/now")`

…but there is **no corresponding fetcher** registered in `graphql/src/main/java` (`name=slingcms/now` not found). This should be corrected by either:

- adding `NowDataFetcher` with `property = {"name=slingcms/now"}`, or
- removing `now` from the schema.

### Security

Security is implemented via:

- `GraphQLSecurityFilter` (request interception)
- `GraphQLSecurityService` (authentication + optional group enforcement)
- `QueryComplexityAnalyzer` (depth/complexity limits)

By default, `hello` is allowed anonymously; other queries require authentication unless configured otherwise.

# GraphQL Module - Recommendations & Roadmap

> **Document Version:** 1.1

## Executive summary

The GraphQL module is production-ready and already includes:

- Authentication/authorization via `GraphQLSecurityFilter` + `GraphQLSecurityService`
- Query depth/complexity limiting via `QueryComplexityAnalyzer`
- Unit tests under `graphql/src/test/java`

This document focuses on **next improvements** based on the current implementation.

## Priority matrix (updated)

| Priority | Timeline | Effort | Impact | Focus |
|---|---:|---:|---:|---|
| **P0** | Immediate | Medium | High | Hardening + accuracy |
| **P1** | 1-2 weeks | Medium | Medium | Developer experience |
| **P2** | 1 month | High | Medium | Performance + scale |
| **P3** | 3 months | High | High | Expanded CMS coverage |

## P0: Hardening (Immediate)

### 1) Remove hardcoded values in `serverInfo`

**Current state:** `serverInfo` returns hardcoded values for `version`, `environment`, and `graphqlVersion`.

**Recommendation:**

- Read CMS version from bundle metadata / Maven filtered resource
- Read environment from OSGi config
- Read GraphQL Core version from bundle/package metadata

### 2) Align feature/model versions in docs

**Current state:** schema and docs refer to GraphQL Core version `0.0.24` and module `1.1.9-SNAPSHOT`.

**Recommendation:** ensure docs are generated/updated from actual deployed versions (or avoid pinning patch versions unless necessary).

## P1: Developer experience (1-2 weeks)

### 3) Add additional “example queries” coverage

Expand examples for:

- `filters` operators (`CONTAINS`, numeric compare, null operators)
- pagination using `cursor` (if/when supported by the backing service)
- schema fields (`Schema.fields`) examples

### 4) Consider self-hosting GraphiQL assets

**Current state:** GraphiQL UI (`html.html`) loads assets from a CDN.

**Recommendation:** self-host to avoid supply-chain dependency and allow offline environments.

## P2: Performance and scale (1 month)

### 5) Add caching / query optimization for fragment listing

- Consider caching schema lookup and/or fragment query results for common use cases
- Review `ContentFragmentQueryService` for query planning and index usage

### 6) Add observability

- Add structured log markers for query name + duration
- Optionally integrate with existing Sling/OSGi metrics mechanisms

## P3: Expanded CMS GraphQL coverage (3 months)

### 7) Extend schema beyond fragments

Potential additions:

- Pages and navigation
- Assets metadata
- Site definitions

