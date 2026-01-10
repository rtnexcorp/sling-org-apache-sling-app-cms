# GraphQL Out-of-the-Box Test Results

**Date**: January 10, 2026  
**Purpose**: Test if Apache Sling GraphQL can be used OOTB without custom implementation

## Test Results Summary

❌ **Apache Sling GraphQL requires additional dependencies that are not currently in Sling CMS**

## What We Tested

1. Checked if GraphQL endpoint exists at `/graphql.json/query.json` - **Requires auth, but only returns JCR data**
2. Checked if GraphQL bundles are installed - **Not installed**
3. Attempted to deploy GraphQL bundles via feature file - **Build fails due to missing dependencies**

## Missing Dependencies

### For `com.graphql-java:graphql-java:21.5`

```
[ERROR] Bundle is importing packages [org.dataloader.stats, org.dataloader, org.reactivestreams] 
        with start order 15 but no bundle is exporting these for that start order.
[ERROR] Bundle is importing package org.slf4j;version=[2.0,3) with start order 15 
        but no bundle is exporting these for that start order in the required version range.
```

**Required bundles**:
- `com.github.ben-manes.caffeine:caffeine` or `org.dataloader:java-dataloader` (for org.dataloader.*)
- `org.reactivestreams:reactive-streams` (for org.reactivestreams.*)
- SLF4J 2.0+ (current Sling CMS uses SLF4J 1.7.x)

### For `org.apache.sling:org.apache.sling.graphql.core:0.0.24`

```
[ERROR] Bundle is importing package org.apache.johnzon.mapper with start order 20 
        but no bundle is exporting these for that start order.
[ERROR] Bundle is importing packages [Package graphql;version=[20.1,21), ...] with start order 20 
        but no bundle is exporting these for that start order in the required version range.
```

**Required bundles**:
- Apache Johnzon (org.apache.johnzon.mapper)
- Correct version alignment with graphql-java

## Dependency Chain Analysis

```
Apache Sling GraphQL Core 0.0.24
├── graphql-java 20.1-21.x
│   ├── java-dataloader
│   ├── reactive-streams
│   └── slf4j 2.0+
└── Apache Johnzon (JSON mapper)
```

## Why This Is Complex

1. **SLF4J Version Conflict**: 
   - Sling CMS uses SLF4J 1.7.x
   - GraphQL Java 21.5 requires SLF4J 2.0+
   - Upgrading SLF4J affects entire application

2. **Additional Dependencies**:
   - Need to add 3-4 additional OSGi bundles
   - Each bundle may have its own transitive dependencies
   - Risk of version conflicts with existing bundles

3. **Feature Analyzer Errors**:
   - Sling Feature Model analyzer enforces strict dependency checking
   - All bundles must have matching import/export packages
   - Start order coordination required

## Recommendations

### Option 1: Simple REST API (✅ RECOMMENDED - Already Working!)

**Status**: ✅ **Already implemented and deployed**

```bash
# Current working endpoints
curl -u admin:admin http://localhost:8082/bin/personalization.segments.json
curl -u admin:admin http://localhost:8082/bin/personalization.evaluators.json
curl -u admin:admin http://localhost:8082/bin/personalization.evaluate.json?path=/content/page
curl -u admin:admin http://localhost:8082/bin/personalization.segment.mobile-users.json
```

**Pros**:
- ✅ Already working
- ✅ No dependency issues
- ✅ Simple to maintain
- ✅ Standard Sling pattern
- ✅ Good for MVP

**Cons**:
- Limited query flexibility compared to GraphQL
- Multiple endpoints for different queries

### Option 2: Add Full GraphQL Dependency Chain (Complex)

Add all missing dependencies to `personalization.json`:

```json
{
  "bundles": [
    {
      "id": "org.apache.sling:org.apache.sling.cms.personalization:${cms-version}",
      "start-order": "20"
    },
    {
      "id": "org.reactivestreams:reactive-streams:1.0.4",
      "start-order": "10"
    },
    {
      "id": "com.github.ben-manes.caffeine:caffeine:3.1.8",
      "start-order": "10"
    },
    {
      "id": "org.apache.geronimo.specs:geronimo-json_1.1_spec:1.5",
      "start-order": "10"
    },
    {
      "id": "org.apache.johnzon:johnzon-mapper:1.2.21",
      "start-order": "15"
    },
    {
      "id": "com.graphql-java:graphql-java:21.5",
      "start-order": "15"
    },
    {
      "id": "org.apache.sling:org.apache.sling.graphql.core:0.0.24",
      "start-order": "20"
    }
  ]
}
```

**Pros**:
- Full GraphQL query capabilities
- Single endpoint for all queries
- Flexible query structure

**Cons**:
- ❌ Complex dependency management
- ❌ Potential SLF4J version conflicts
- ❌ Requires upgrading/testing entire application
- ❌ May break existing functionality
- ❌ Significant testing required
- ❌ Maintenance burden

### Option 3: Wait for Better GraphQL Support in Sling

Wait for Apache Sling to provide better OOTB GraphQL support with compatible dependencies.

**Pros**:
- Officially supported
- Better tested
- Community maintenance

**Cons**:
- Unknown timeline
- May never happen

## Decision

**RECOMMENDATION: Stick with Option 1 (REST API)**

The REST API is:
- ✅ Already implemented and working
- ✅ Follows Sling best practices
- ✅ Zero dependency issues
- ✅ Easy to maintain
- ✅ Good enough for personalization use cases

GraphQL would be nice-to-have but:
- ❌ Too complex for the benefit gained
- ❌ Risk of breaking existing functionality
- ❌ Personalization queries are simple (don't need GraphQL flexibility)

## Testing Evidence

### OOTB Sling Endpoint Test

```bash
$ curl -u admin:admin http://localhost:8082/graphql.json/query.json
{"jcr:primaryType":"nt:unstructured"}
```

**Interpretation**: The `/graphql.json` path exists but just returns JCR node data. No actual GraphQL processing happens because the GraphQL bundles aren't installed.

### Bundle Installation Attempt

```bash
$ mvn clean install -P autoInstallBundle -pl feature
[ERROR] Analyser detected errors on feature 'slingcms-standalone':
  - com.graphql-java:graphql-java:21.5: Missing packages [org.dataloader.stats, org.dataloader, org.reactivestreams]
  - com.graphql-java:graphql-java:21.5: Missing org.slf4j;version=[2.0,3)
  - org.apache.sling.graphql.core:0.0.24: Missing org.apache.johnzon.mapper
[INFO] BUILD FAILURE
```

## Conclusion

**For personalization use cases in Sling CMS, the REST API is the right choice.**

GraphQL would only make sense if:
1. You need complex nested queries with multiple relationships
2. You have multiple client applications with different data needs
3. You're willing to maintain complex dependency chain
4. You have time for extensive testing

None of these apply to the personalization module, so **continue using the REST API at `/bin/personalization.*`**.

## Next Steps

1. ✅ Remove GraphQL bundles from `personalization.json` to fix build
2. ✅ Document REST API (already done in `/docs/personalization-rest-api.md`)
3. ✅ Create test segments to demonstrate functionality
4. Consider GraphQL only if future requirements truly need it

## References

- Working REST API: `/docs/personalization-rest-api.md`
- GraphQL Integration Plan: `/docs/graphql-integration-plan.md` (archive for future reference)
- Apache Sling GraphQL: https://sling.apache.org/documentation/bundles/graphql.html
