<!-- Licensed to the Apache Software Foundation (ASF) under one or more contributor
	license agreements. See the NOTICE file distributed with this work for additional
	information regarding copyright ownership. The ASF licenses this file to
	you under the Apache License, Version 2.0 (the "License"); you may not use
	this file except in compliance with the License. You may obtain a copy of
	the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required
	by applicable law or agreed to in writing, software distributed under the
	License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
	OF ANY KIND, either express or implied. See the License for the specific
	language governing permissions and limitations under the License. -->
[Apache Sling](https://sling.apache.org) > [Sling CMS](https://github.com/apache/sling-org-apache-sling-app-cms) > [Developers](developers.md) > Oak Indexing

# Oak Indexing

Apache Sling CMS uses Apache Jackrabbit Oak as its underlying JCR repository. Oak provides a powerful indexing mechanism that enables efficient querying of content. This document explains how indexing works in Sling CMS and how to create custom indexes.

## Overview

Oak supports several types of indexes:

| Index Type | Description | Use Case |
|------------|-------------|----------|
| **Lucene** | Full-text and property indexing using Apache Lucene | Complex queries, full-text search, sorting |
| **Property** | Simple property-based indexing | Equality checks on single properties |
| **Counter** | Tracks node counts | Aggregate counting |

Sling CMS primarily uses **Lucene indexes** for optimal query performance.

## Index Location

All Oak indexes are stored under `/oak:index` in the JCR repository. You can view existing indexes using the Node Browser at `/bin/browser.html` or via the System Console.

## Sling CMS Built-in Indexes

Sling CMS defines the following indexes in `IndexCreator.java`:

| Index Name | Node Type | Included Paths | Purpose |
|------------|-----------|----------------|---------|
| `ntFolder` | `nt:folder` | All paths | Folder content indexing |
| `ntHierarchyNode` | `nt:hierarchyNode` | `/content`, `/static` | Hierarchy node queries |
| `slingComponent` | `sling:Component` | `/apps`, `/libs` | Component lookups |
| `slingeventJob` | `slingevent:Job` | `/var/eventing` | Job queue queries |
| `slingFile` | `sling:File` | `/content`, `/static` | File/asset queries |
| `slingPage` | `sling:Page` | `/content`, `/static` | Page content queries |
| `slingTaxonomy` | `sling:Taxonomy` | `/etc/taxonomy` | Taxonomy lookups |
| `slingFragments` | `nt:unstructured` | `/content/fragments` | Content fragment queries |

## Creating Custom Indexes

### Using RepositoryInitializer

Sling CMS creates indexes programmatically using Oak's `RepositoryInitializer` interface. This ensures indexes are created at repository startup.

```java
import org.apache.jackrabbit.oak.plugins.index.IndexConstants;
import org.apache.jackrabbit.oak.plugins.index.lucene.util.IndexDefinitionBuilder;
import org.apache.jackrabbit.oak.plugins.index.lucene.util.IndexDefinitionBuilder.IndexRule;
import org.apache.jackrabbit.oak.spi.lifecycle.RepositoryInitializer;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.osgi.service.component.annotations.Component;

@Component(service = RepositoryInitializer.class)
public class MyIndexCreator implements RepositoryInitializer {

    @Override
    public void initialize(NodeBuilder builder) {
        NodeBuilder indexRoot = builder.child(IndexConstants.INDEX_DEFINITIONS_NAME);
        createMyCustomIndex(indexRoot);
    }

    private void createMyCustomIndex(NodeBuilder indexRoot) {
        // Create index node
        NodeBuilder index = indexRoot.child("myCustomIndex");
        index.setProperty("jcr:primaryType", IndexConstants.INDEX_DEFINITIONS_NODE_TYPE, Type.NAME);

        // Configure index using IndexDefinitionBuilder
        IndexDefinitionBuilder builder = new IndexDefinitionBuilder(index, true);
        builder.async(IndexConstants.ASYNC_PROPERTY_NAME, IndexConstants.INDEXING_MODE_NRT);
        builder.evaluatePathRestrictions();
        builder.includedPaths("/content/myapp");
        builder.tags("myapp", "myapp-custom");

        // Define index rules
        IndexRule indexRule = builder.indexRule("nt:unstructured");
        indexRule.indexNodeName();
        indexRule.property("myProperty", "myProperty", false).propertyIndex();
    }
}
```

### IndexDefinitionBuilder API

The `IndexDefinitionBuilder` provides a fluent API for creating index definitions:

#### Builder Configuration Methods

| Method | Description | Example |
|--------|-------------|---------|
| `async(name, mode)` | Set async indexing mode | `builder.async("async", "nrt")` |
| `evaluatePathRestrictions()` | Enable path restriction evaluation | `builder.evaluatePathRestrictions()` |
| `includedPaths(paths...)` | Restrict index to specific paths | `builder.includedPaths("/content", "/static")` |
| `excludedPaths(paths...)` | Exclude paths from indexing | `builder.excludedPaths("/content/temp")` |
| `tags(tags...)` | Add tags for index identification | `builder.tags("slingcms", "custom")` |
| `supersedes(indexPath)` | Mark this index as replacing another | `builder.supersedes("/oak:index/oldIndex")` |
| `aggregateRule(nodeType, patterns...)` | Define aggregation rules | `builder.aggregateRule("sling:Page", "jcr:content/*")` |

#### IndexRule Methods

| Method | Description | Example |
|--------|-------------|---------|
| `indexRule(nodeType)` | Create rule for node type | `builder.indexRule("sling:Page")` |
| `indexNodeName()` | Index the node name | `indexRule.indexNodeName()` |
| `property(name, propertyPath, isRegex)` | Define property to index | `indexRule.property("title", "jcr:title", false)` |

#### PropertyRule Methods

| Method | Description | Example |
|--------|-------------|---------|
| `propertyIndex()` | Enable property index | `.propertyIndex()` |
| `analyzed()` | Enable full-text analysis | `.analyzed()` |
| `ordered()` | Enable sorting on this property | `.ordered()` |
| `type(typeName)` | Set property type | `.type("Date")` |
| `nullCheckEnabled()` | Enable null value checking | `.nullCheckEnabled()` |
| `notNullCheckEnabled()` | Enable not-null checking | `.notNullCheckEnabled()` |
| `nodeScopeIndex()` | Include in full-text node scope | `.nodeScopeIndex()` |
| `boost(value)` | Set relevance boost factor | `.boost(2)` |

## Index Definition Best Practices

### 1. Use Specific Node Types

Always use the most specific node type possible. Avoid using broad types like `nt:base` as they can affect query performance globally.

```java
// Good - specific node type
IndexRule indexRule = builder.indexRule("sling:Page");

// Bad - too broad, affects all nodes
IndexRule indexRule = builder.indexRule(JcrConstants.NT_BASE);
```

### 2. Restrict Index Paths

Always use `includedPaths()` to limit the scope of your index:

```java
// Good - restricted to specific path
builder.includedPaths("/content/fragments");

// Bad - indexes entire repository
// (no includedPaths call)
```

### 3. Use Async Indexing

Always configure async indexing for production:

```java
builder.async(IndexConstants.ASYNC_PROPERTY_NAME, IndexConstants.INDEXING_MODE_NRT);
```

The `nrt` (near-real-time) mode provides a good balance between freshness and performance.

### 4. Tag Your Indexes

Use tags to identify and group related indexes:

```java
builder.tags("slingcms", "slingcms-myfeature");
```

### 5. Avoid Interfering with Other Systems

When using common node types like `nt:unstructured`, ensure your index doesn't interfere with other subsystems (like Composum) by:

- Using restrictive `includedPaths()`
- Not using `valuePattern()` which can affect query planning
- Testing thoroughly before deployment

## Example: Content Fragment Index

Here's a complete example of creating an index for content fragments:

```java
private void ensureFragmentsIndex(NodeBuilder indexRoot) {
    log.info("ensureFragmentsIndex");
    NodeBuilder index = ensureNode(indexRoot, "slingFragments",
        IndexConstants.INDEX_DEFINITIONS_NODE_TYPE);

    IndexDefinitionBuilder builder = new IndexDefinitionBuilder(index, true);

    // Async indexing with near-real-time mode
    builder.async(IndexConstants.ASYNC_PROPERTY_NAME, IndexConstants.INDEXING_MODE_NRT);

    // Evaluate path restrictions in queries
    builder.evaluatePathRestrictions();

    // Only index content under /content/fragments
    builder.includedPaths("/content/fragments");

    // Tag for identification
    builder.tags("slingcms", "slingcms-fragments");

    // Index nt:unstructured nodes (content fragments use this type)
    IndexRule indexRule = builder.indexRule(JcrConstants.NT_UNSTRUCTURED);
    indexRule.indexNodeName();

    // Index sling:resourceType for filtering
    indexRule.property("slingResourceType", "sling:resourceType", false)
        .propertyIndex();

    // Full-text searchable fields
    indexRule.property("title", "title", false).analyzed().propertyIndex();
    indexRule.property("summary", "summary", false).analyzed().propertyIndex();
    indexRule.property("body", "body", false).analyzed().propertyIndex();

    // Sortable title field
    indexRule.property("jcrTitle", "jcr:title", false)
        .analyzed()
        .propertyIndex()
        .ordered();

    // Enable full-text node scope queries
    indexRule.property("allContent", ".", false).nodeScopeIndex();
}
```

## Querying Indexed Content

### JCR-SQL2 Queries

```sql
-- Find all pages with title containing 'welcome'
SELECT * FROM [sling:Page] AS page
WHERE ISDESCENDANTNODE(page, '/content')
AND CONTAINS(page.*, 'welcome')

-- Find content fragments by resource type
SELECT * FROM [nt:unstructured] AS fragment
WHERE ISDESCENDANTNODE(fragment, '/content/fragments')
AND [sling:resourceType] = 'sling-cms/components/cms/fragment'

-- Find pages modified after a specific date
SELECT * FROM [sling:Page] AS page
WHERE ISDESCENDANTNODE(page, '/content')
AND [jcr:content/jcr:lastModified] > CAST('2024-01-01T00:00:00.000Z' AS DATE)
ORDER BY [jcr:content/jcr:lastModified] DESC
```

### XPath Queries

```xpath
//element(*, sling:Page)[jcr:contains(., 'welcome')]
/jcr:root/content//element(*, sling:File)[@jcr:content/jcr:mimeType = 'image/png']
```

## Index Management

### Viewing Indexes

1. **Node Browser**: Navigate to `/oak:index` in the Node Browser
2. **System Console**: Access `/system/console/jmx` and look for Oak indexing MBeans
3. **Query Debugger**: Use `/cms/admin/querydebug.html` to analyze query performance

### Reindexing

To trigger a reindex, set the `reindex` property to `true` on the index node:

```bash
curl -u admin:admin -X POST \
  "http://localhost:8080/oak:index/slingPage" \
  -d "reindex=true"
```

**Warning**: Reindexing can be resource-intensive. Perform during maintenance windows.

### Deleting an Index

```bash
curl -u admin:admin -X DELETE \
  "http://localhost:8080/oak:index/myCustomIndex"
```

## Troubleshooting

### Common Issues

#### 1. Index Not Being Used

Check if your query matches the index configuration:
- Verify `includedPaths` covers the query path
- Ensure the node type in `indexRule` matches the query
- Check that indexed properties match query predicates

#### 2. Slow Queries

- Enable `evaluatePathRestrictions()` if using path constraints
- Add `ordered()` to properties used in ORDER BY
- Consider adding more specific indexes

#### 3. Index Interfering with Other Components

If your index causes issues with other systems (like Composum):
- Use more restrictive `includedPaths()`
- Avoid broad node types like `nt:base` or `nt:unstructured` without path restrictions
- Remove `valuePattern()` if present
- Restart the Sling instance after changes

### Checking Index Status

View index status via JMX or the System Console. Look for:
- `reindexCount`: Number of times reindexed
- `async`: Current async indexing status
- `indexedNodes`: Number of indexed nodes

## Additional Resources

- [Apache Jackrabbit Oak Documentation](https://jackrabbit.apache.org/oak/docs/query/lucene.html)
- [Oak Query and Indexing](https://jackrabbit.apache.org/oak/docs/query/query.html)
- [IndexCreator.java](../core/src/main/java/org/apache/sling/cms/core/internal/IndexCreator.java) - Sling CMS index definitions
