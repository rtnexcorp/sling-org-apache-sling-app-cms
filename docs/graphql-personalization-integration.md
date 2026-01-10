# GraphQL Integration for Personalization Module

This document explains how to integrate Apache Sling GraphQL with the Personalization module, based on the [official Sling GraphQL samples](https://github.com/apache/sling-samples/tree/master/org.apache.sling.graphql.samples.website).

## Overview

The personalization module uses GraphQL for:
- Querying personalized content variants
- Exposing segment definitions
- Providing a flexible API for client-side and server-side personalization

## Architecture

### Dependencies (Already Configured)

The following OSGi bundles are configured in `feature/src/main/features/personalization.json`:

```json
{
  "id": "org.reactivestreams:reactive-streams:1.0.3",
  "id": "com.graphql-java:java-dataloader:3.2.0",
  "id": "com.graphql-java:graphql-java:20.1",
  "id": "org.apache.johnzon:johnzon-mapper:1.2.8",
  "id": "org.apache.sling:org.apache.sling.graphql.core:0.0.20"
}
```

### GraphQL Servlet Configuration

The GraphQL servlet is configured to handle `.graphql.json` requests:

```json
"org.apache.sling.graphql.core.servlet.GraphQLServlet": {
  "sling.servlet.selectors": ["graphql"],
  "sling.servlet.extensions": ["json"],
  "sling.servlet.methods": ["GET", "POST"]
}
```

This enables:
- Server-side queries: `/content/page.graphql.json`
- Client-side queries: `POST /graphql.json`

## Implementation Guide

### 1. Create GraphQL Schema Files

GraphQL schemas are defined using JSP files with `.GQLschema` extension. These are retrieved via Sling's standard request processing.

**Example: `/apps/personalization/segment/GQLschema.jsp`**

```jsp
<%@ include file="/apps/personalization/common/directives.jsp" %>

type Query {
  # Get current segment for the user
  currentSegment: Segment @fetcher(name:"personalization/currentSegment")
  
  # Get all available segments
  segments: [Segment] @fetcher(name:"personalization/allSegments")
  
  # Get personalized content variants
  variants(path: String!): [Variant] @fetcher(name:"personalization/variants")
}

<%@ include file="/apps/personalization/common/schema-types.jsp" %>
```

**Schema Types: `/apps/personalization/common/schema-types.jsp`**

```jsp
# Personalization Segment
type Segment {
  id: String
  name: String
  description: String
  rules: [Rule]
}

# Segment Rule
type Rule {
  type: String
  property: String
  operator: String
  value: String
}

# Content Variant
type Variant {
  path: String
  title: String
  segment: String
  content: String
}
```

**Directives: `/apps/personalization/common/directives.jsp`**

```jsp
# Maps GraphQL fields to Sling DataFetchers
directive @fetcher(
    name: String,
    options: String = "",
    source: String = "resource"
) on FIELD_DEFINITION
```

### 2. Implement SlingDataFetcher Services

Data fetchers provide the actual data for GraphQL queries. They are registered as OSGi services.

**Example: CurrentSegmentFetcher**

```java
package org.apache.sling.cms.personalization.datafetchers;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.graphql.api.SlingDataFetcher;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import java.util.HashMap;
import java.util.Map;

/**
 * DataFetcher for retrieving the current user's segment
 */
@Component(
    service = SlingDataFetcher.class,
    property = {"name=personalization/currentSegment"}
)
public class CurrentSegmentFetcher implements SlingDataFetcher<Object> {
    
    @Reference
    private PersonalizationService personalizationService;
    
    @Override
    public Object get(SlingDataFetcherEnvironment env) throws Exception {
        Resource currentResource = env.getCurrentResource();
        
        // Get segment from personalization service
        Segment segment = personalizationService.getSegment(
            currentResource.getResourceResolver().adaptTo(HttpServletRequest.class)
        );
        
        // Convert to GraphQL-friendly Map
        Map<String, Object> result = new HashMap<>();
        result.put("id", segment.getId());
        result.put("name", segment.getName());
        result.put("description", segment.getDescription());
        result.put("rules", convertRules(segment.getRules()));
        
        return result;
    }
    
    private List<Map<String, Object>> convertRules(List<SegmentRule> rules) {
        // Convert rules to Maps for GraphQL
        // ... implementation
    }
}
```

**Example: VariantsFetcher**

```java
@Component(
    service = SlingDataFetcher.class,
    property = {"name=personalization/variants"}
)
public class VariantsFetcher implements SlingDataFetcher<Object> {
    
    @Reference
    private PersonalizationService personalizationService;
    
    @Override
    public Object get(SlingDataFetcherEnvironment env) throws Exception {
        // Get 'path' argument from GraphQL query
        String path = env.getArgument("path", String.class);
        
        Resource resource = env.getCurrentResource()
            .getResourceResolver()
            .getResource(path);
            
        if (resource == null) {
            return Collections.emptyList();
        }
        
        // Get all variants for this resource
        List<Map<String, Object>> variants = new ArrayList<>();
        for (Resource variantResource : resource.getChildren()) {
            if (isVariant(variantResource)) {
                variants.add(convertVariant(variantResource));
            }
        }
        
        return variants;
    }
    
    private Map<String, Object> convertVariant(Resource variant) {
        Map<String, Object> result = new HashMap<>();
        result.put("path", variant.getPath());
        result.put("title", variant.getValueMap().get("title", String.class));
        result.put("segment", variant.getValueMap().get("segment", String.class));
        // ... more properties
        return result;
    }
}
```

### 3. Server-Side GraphQL Queries

For server-side rendering, create `.gql` scripts for each resource type.

**Example: `/apps/personalization/page/json.gql`**

```graphql
{
  currentSegment {
    id
    name
  }
  variants(path: "${resource.path}") {
    path
    title
    segment
    content
  }
}
```

This query will be executed when accessing `/content/page.json` and the aggregated result can be used in HTL templates.

**HTL Template: `/apps/personalization/page/page.html`**

```html
<sly data-sly-use.model="org.apache.sling.cms.personalization.models.PersonalizedPage">
  <div class="personalized-content">
    <h2>Current Segment: ${model.currentSegment.name}</h2>
    
    <sly data-sly-list.variant="${model.variants}">
      <div data-segment="${variant.segment}">
        ${variant.content @ context='html'}
      </div>
    </sly>
  </div>
</sly>
```

### 4. Client-Side GraphQL Queries

For client-side rendering (SPA), use JavaScript to query the GraphQL endpoint.

**JavaScript Client:**

```javascript
/**
 * GraphQL client for personalization queries
 */
var personalizationQL = {
    query: function(query, variables, callback) {
        fetch('/graphql.json', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
            },
            body: JSON.stringify({
                query: query,
                variables: variables
            })
        })
        .then(r => r.json())
        .then(data => callback(data.data))
        .catch(err => console.error('GraphQL Error:', err));
    }
};

// Usage example
personalizationQL.query(`
  query GetVariants($path: String!) {
    variants(path: $path) {
      path
      title
      segment
      content
    }
  }
`, { path: '/content/mypage' }, function(result) {
    console.log('Variants:', result.variants);
    // Render variants dynamically
});
```

### 5. Utility Classes for Resource Conversion

Create utilities to convert Sling Resources to GraphQL-friendly Maps:

```java
package org.apache.sling.cms.personalization.graphql;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import java.util.HashMap;
import java.util.Map;

public class GraphQLResourceWrapper {
    
    /**
     * Convert a Sling Resource to a Map for GraphQL
     */
    public static Map<String, Object> resourceToMap(Resource resource) {
        Map<String, Object> result = new HashMap<>();
        ValueMap props = resource.getValueMap();
        
        result.put("path", resource.getPath());
        result.put("name", resource.getName());
        result.put("resourceType", resource.getResourceType());
        
        // Add all properties
        props.forEach((key, value) -> {
            if (isSerializable(value)) {
                result.put(key, value);
            }
        });
        
        return result;
    }
    
    private static boolean isSerializable(Object value) {
        return value instanceof String 
            || value instanceof Number 
            || value instanceof Boolean
            || value instanceof String[];
    }
}
```

## Key Patterns from Sling GraphQL Samples

### 1. Schema Discovery

- Schemas are discovered via `.GQLschema` extension
- Use Sling's standard resource resolution
- Allows resource-type specific schemas

### 2. Data Fetcher Registration

```java
@Component(
    service = SlingDataFetcher.class,
    property = {"name=personalization/myFetcher"}
)
public class MyFetcher implements SlingDataFetcher<Object> {
    @Override
    public Object get(SlingDataFetcherEnvironment env) {
        // Implementation
    }
}
```

The `name` property maps to the `@fetcher(name:"...")` directive in schemas.

### 3. Context Resource Access

```java
// Get the current resource being queried
Resource current = env.getCurrentResource();

// Get the parent object from previous fetcher
Object parent = env.getParentObject();
if (parent instanceof Map) {
    String path = (String) ((Map) parent).get("path");
    // Use path to navigate
}
```

### 4. Query Arguments

```java
// Get arguments from GraphQL query
String text = env.getArgument("withText", String.class);
Integer limit = env.getArgument("limit", Integer.class);
```

### 5. Unstructured Content Support

GraphQL supports dynamic/unstructured content using the `Object` scalar:

```graphql
scalar Object

type Query {
  # Returns dynamic structure
  dynamicData: Object @fetcher(name:"myFetcher")
}
```

## Testing GraphQL Queries

### 1. Using GraphiQL Client

Install [GraphiQL](https://www.electronjs.org/apps/graphiql) and configure it to point to:
- URL: `http://localhost:8080/graphql.json`
- Method: POST

### 2. Using curl

```bash
# Simple query
curl -X POST http://localhost:8080/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"{ currentSegment { id name } }"}'

# Query with variables
curl -X POST http://localhost:8080/graphql.json \
  -H "Content-Type: application/json" \
  -d '{"query":"query($path:String!){variants(path:$path){title}}","variables":{"path":"/content/page"}}'
```

### 3. Browser DevTools

Access `.graphql.json` URLs directly:
- `http://localhost:8080/content/page.graphql.json`

## Best Practices

1. **Keep Schemas Resource-Type Specific**: Use Sling's overlay mechanism for customization
2. **Use Data Fetchers for Business Logic**: Don't put logic in schemas
3. **Return Maps for GraphQL**: Always convert Resources/POJOs to Maps
4. **Limit Result Sets**: Always impose reasonable limits on collections
5. **Handle Errors Gracefully**: Catch exceptions in fetchers and return meaningful errors
6. **Cache When Appropriate**: Use Sling's caching mechanisms
7. **Security**: Always check permissions in data fetchers

## References

- [Apache Sling GraphQL Core](https://github.com/apache/sling-org-apache-sling-graphql-core)
- [GraphQL Samples Website](https://github.com/apache/sling-samples/tree/master/org.apache.sling.graphql.samples.website)
- [GraphQL Java Documentation](https://www.graphql-java.com/)
- [Sling GraphQL Documentation](https://sling.apache.org/documentation/bundles/graphql.html)

## Next Steps

1. Create GraphQL schema files in `/apps/personalization/`
2. Implement SlingDataFetcher services for personalization queries
3. Add server-side `.gql` query files for HTL templates
4. Create JavaScript client for client-side queries
5. Test queries using GraphiQL or curl
6. Document your GraphQL API
