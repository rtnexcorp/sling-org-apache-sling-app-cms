# Search Functionality

Apache Sling CMS provides powerful search capabilities built on Apache Jackrabbit Oak's Lucene indexing. This document covers the search architecture, configuration, and usage.

## Types of Search

The platform provides multiple search capabilities:

### 1. Full-Text Search (Lucene)
**Technology:** Apache Lucene via Oak Lucene indexes

Full-text search allows users to find content by searching within the actual text content of pages, assets, and other resources.

| Feature | Description |
|---------|-------------|
| **Tokenization** | Text is broken into searchable tokens |
| **Stemming** | Words are reduced to root form (e.g., "running" → "run") |
| **Analyzed Properties** | Title, description, and content are analyzed for search |
| **Boosting** | Page titles have 2x boost for relevance |
| **Aggregation** | Child content (jcr:content) is aggregated into parent for search |

**Example Query:**
```sql
SELECT * FROM [sling:Page] WHERE CONTAINS([jcr:content/jcr:title], 'apache sling')
```

### 2. Property Search
**Technology:** JCR-SQL2 with LIKE operator

Property search finds content by matching specific property values without full-text analysis.

| Feature | Description |
|---------|-------------|
| **Exact Match** | Find exact property values |
| **Wildcard** | Use `%` for partial matches |
| **Case-Insensitive** | LOWER() function for case-insensitive search |
| **No Index Required** | Works without Lucene (but slower) |

**Example Query:**
```sql
SELECT * FROM [sling:Page] WHERE LOWER([jcr:content/jcr:title]) LIKE '%demo%'
```

### 3. Node Name Search
**Technology:** JCR-SQL2 with LOCALNAME/NAME functions

Search by the name of content nodes (e.g., page URL names).

**Example Query:**
```sql
SELECT * FROM [sling:Page] WHERE LOCALNAME(s) LIKE '%multifield%'
```

### 4. Path-Restricted Search
**Technology:** JCR-SQL2 with ISDESCENDANTNODE

Limit search to specific content paths (sites, folders).

**Example Query:**
```sql
SELECT * FROM [sling:Page] WHERE CONTAINS([jcr:content/jcr:title], 'demo') 
  AND ISDESCENDANTNODE([/content/reference])
```

### 5. Type-Specific Search
**Technology:** JCR node type filtering

Search for specific content types:

| Node Type | Description |
|-----------|-------------|
| `sling:Page` | CMS pages |
| `sling:File` | Assets/files |
| `nt:hierarchyNode` | All content (pages, files, folders) |
| `sling:Taxonomy` | Taxonomy items |
| `sling:Component` | Components |

### 6. Taxonomy/Tag Search
**Technology:** Property index on `sling:taxonomy`

Find content by assigned taxonomy terms/tags.

**Example Query:**
```sql
SELECT * FROM [sling:Page] WHERE [jcr:content/sling:taxonomy] = '/etc/taxonomy/topics/news'
```

### Search Comparison Matrix

| Search Type | Location | Speed | Index Required | Use Case |
|-------------|----------|-------|----------------|----------|
| Full-Text (CONTAINS) | Server | ⚡ Fast | Yes (Lucene) | Content search, site search |
| Property (LIKE) | Server | 🐢 Slower | No | Exact filters, admin tools |
| Node Name | Server | ⚡ Fast | Yes (analyzed) | URL-based lookups |
| Path-Restricted | Server | ⚡ Fast | Yes | Site-specific search |
| Type-Specific | Server | ⚡ Fast | Yes | Content type filtering |
| Taxonomy | Server | ⚡ Fast | Yes | Tag-based filtering |
| Content Nav Filter | Client | ⚡⚡ Instant | No | Filtering current folder |
| Path Autocomplete | Hybrid | ⚡ Fast | No | Path field suggestions |
| AJAX Search | Hybrid | ⚡ Fast | Yes (Lucene) | Global search with live results |

---

## Client-Side Search & Filtering

In addition to server-side search, the CMS provides several client-side search and filtering capabilities for instant, responsive user experiences.

### 7. Content Navigation Filter (Client-Side)
**Technology:** JavaScript DOM filtering (`cms.nav.js`)

Real-time filtering of content items in the navigation tree without server requests.

| Feature | Description |
|---------|-------------|
| **Text Search** | Filter by item name/title |
| **Type Filter** | Filter by MIME type (images, documents, etc.) |
| **Tag Filter** | Filter by taxonomy tags |
| **Instant Results** | No server round-trip needed |
| **Combined Filters** | Apply multiple filters simultaneously |

**HTML Data Attributes:**
```html
<!-- Search input -->
<input data-asset-search type="text" placeholder="Search...">

<!-- Type filter dropdown -->
<select data-asset-type-filter>
  <option value="">All Types</option>
  <option value="image/">Images</option>
  <option value="application/pdf">PDFs</option>
</select>

<!-- Tag filter dropdown -->
<select data-asset-tag-filter>
  <option value="">All Tags</option>
  <option value="/etc/taxonomy/topics">Topics</option>
</select>
```

**Content Item Attributes:**
```html
<div class="contentnav__item" 
     data-mime-type="image/png"
     data-taxonomy="/etc/taxonomy/topics/news"
     data-is-folder="false">
  <span title="my-image.png">my-image.png</span>
</div>
```

### 8. Path Field Autocomplete
**Technology:** js-autocomplete library + server-side path lookup

Type-ahead suggestions when entering content paths in form fields.

| Feature | Description |
|---------|-------------|
| **Path Suggestions** | Autocomplete as you type paths |
| **Type Filtering** | Filter by content type (pages, files, etc.) |
| **Debounced Requests** | Efficient API calls with 100ms debounce |
| **Keyboard Navigation** | Arrow keys to navigate suggestions |

**HTML Usage:**
```html
<input class="pathfield" 
       type="text" 
       data-base="/content" 
       data-type="sling:Page">
```

**Server Endpoint:** `/bin/cms/paths?path={term}&type={nodeType}`

### 9. Global Search (AJAX)
**Technology:** Form submission with AJAX response loading

The start page search uses client-side AJAX to load results without page refresh.

**HTML Form:**
```html
<form class="get-form" 
      action="${resource.path}.search.html" 
      data-target=".search-results"
      data-load="div">
    <input type="text" name="q" placeholder="Find Content...">
</form>
<div class="search-results"></div>
```

**Behavior:**
1. User types search term
2. Form submits via AJAX (not full page reload)
3. Server returns HTML fragment
4. Results loaded into target container

### 10. Taxonomy Search (Client-Side)
**Technology:** JavaScript filtering of taxonomy tree

Filter taxonomy items in the taxonomy browser.

| Feature | Description |
|---------|-------------|
| **Tree Filtering** | Filter taxonomy tree by term |
| **Case-Insensitive** | Matches regardless of case |
| **Parent Expansion** | Auto-expands matching parents |

### Client-Side vs Server-Side Search

| Aspect | Client-Side | Server-Side |
|--------|-------------|-------------|
| **Speed** | ⚡ Instant | 🔄 Network latency |
| **Data Scope** | Currently loaded items | Entire repository |
| **Scalability** | Limited by page size | Handles large datasets |
| **Offline** | Works after page load | Requires connection |
| **Use Case** | Filtering current view | Finding any content |
| **Technology** | JavaScript | JCR-SQL2 + Lucene |

### Combined Search Flow

For the best user experience, the CMS combines both approaches:

```
┌─────────────────────────────────────────────────────────────────┐
│                    User Search Interaction                       │
└─────────────────────────────────────────────────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  Path Field     │  │  Content Nav    │  │  Global Search  │
│  Autocomplete   │  │  Filter         │  │  (AJAX)         │
│                 │  │                 │  │                 │
│  - Type-ahead   │  │  - Instant      │  │  - Repository-  │
│  - Server API   │  │  - DOM-based    │  │    wide search  │
│  - Path lookup  │  │  - Multi-filter │  │  - Lucene index │
└─────────────────┘  └─────────────────┘  └─────────────────┘
        │                    │                    │
        ▼                    ▼                    ▼
   /bin/cms/paths       Pure JavaScript      SearchResults
   (server endpoint)    (no server call)     Sling Model
```

---

## Overview

The CMS search system consists of:

1. **Oak Lucene Indexes** - Full-text and property indexes for fast content retrieval
2. **SearchResults Model** - Sling Model for executing search queries
3. **Global Search UI** - Start page search component for finding content across the repository
4. **Site Search** - Reference implementation for public-facing site search
5. **Client-Side Filtering** - JavaScript-based instant filtering for loaded content

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Search Request                           │
│                     (q=term or term=xxx)                        │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                     SearchResults Model                          │
│              (org.apache.sling.cms.core.models)                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Parameters:                                              │   │
│  │   - q / term: Search term                                │   │
│  │   - type: Node type (default: sling:Page)                │   │
│  │   - path: Search path restriction                        │   │
│  │   - fulltext: Enable/disable fulltext (default: true)    │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                       JCR-SQL2 Query                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Fulltext Mode:                                           │   │
│  │   CONTAINS(s.[jcr:content/jcr:title], 'term')            │   │
│  │   OR CONTAINS(s.[jcr:content/jcr:description], 'term')   │   │
│  │   OR LOCALNAME(s) LIKE '%term%'                          │   │
│  ├─────────────────────────────────────────────────────────┤   │
│  │ Property Mode (fallback):                                │   │
│  │   LIKE '%term%' on jcr:title, jcr:content/jcr:title      │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Oak Lucene Index                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Indexes:                                                 │   │
│  │   - slingPage: Pages with aggregated jcr:content        │   │
│  │   - slingFile: Files with metadata                      │   │
│  │   - ntHierarchyNode: All hierarchy nodes                │   │
│  │   - slingTaxonomy: Taxonomy items                       │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Oak Lucene Indexes

The CMS creates several Lucene indexes at repository initialization via `IndexCreator`:

### slingPage Index

Indexes `sling:Page` nodes with aggregated content from `jcr:content` descendants.

**Indexed Properties:**
| Property | Path | Features |
|----------|------|----------|
| `jcrTitle` | `jcr:content/jcr:title` | Analyzed, Ordered, Boost: 2.0 |
| `jcrDescription` | `jcr:content/jcr:description` | Analyzed |
| `nodeName` | `:nodeName` | Analyzed, Ordered |
| `jcrLastModified` | `jcr:content/jcr:lastModified` | Ordered, Date |
| `slingPublished` | `jcr:content/sling:published` | Boolean, NullCheck |
| `slingTaxonomy` | `jcr:content/sling:taxonomy` | Analyzed |
| `slingTemplate` | `jcr:content/sling:template` | Property Index |
| `allProperties` | `jcr:content/.*` | Regex, Analyzed |
| `allContent` | `.` | NodeScopeIndex (fulltext) |

**Aggregation:** Content from `jcr:content/*`, `jcr:content/*/*`, `jcr:content/*/*/*`, `jcr:content/*/*/*/*` is aggregated into the page node for fulltext search.

**Path Restrictions:** `/content`, `/static`

### slingFile Index

Indexes `sling:File` nodes (assets) with metadata.

**Indexed Properties:**
- Same common properties as slingPage
- `metadata` - `jcr:content/metadata/.*` (regex, analyzed)
- `allContent` - NodeScopeIndex for fulltext

**Aggregation:** `jcr:content/*`, `jcr:content/metadata/*`

### ntHierarchyNode Index

General index for all `nt:hierarchyNode` types (pages, files, folders).

**Path Restrictions:** `/content`, `/static`

### slingTaxonomy Index

Indexes taxonomy items under `/etc/taxonomy`.

**Indexed Properties:**
- `jcr:title` - Analyzed, Ordered
- `:nodeName` - Analyzed, Ordered

## SearchResults Model

The `SearchResults` Sling Model provides search functionality for HTL templates.

### Usage in HTL

```html
<sly data-sly-use.searchResults="org.apache.sling.cms.core.models.SearchResults"></sly>
<sly data-sly-list.item="${searchResults.resultList}">
    <a href="${item.path}">${item.valueMap['jcr:content/jcr:title'] || item.name}</a>
</sly>
```

### Request Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `q` | Search term (alternative to `term`) | - |
| `term` | Search term | - |
| `type` | JCR node type to search | `sling:Page` (or `nt:hierarchyNode` when using `q`) |
| `path` | Restrict search to path descendants | - |
| `fulltext` | Enable fulltext search (`true`/`false`) | `true` |

### Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getResults()` | `Iterator<Resource>` | Returns search results as iterator |
| `getResultList()` | `List<Resource>` | Returns up to 20 results as list |

### Example URLs

```bash
# Search for pages containing "apache"
/path/to/resource.search.html?q=apache

# Search for files only
/path/to/resource.search.html?term=logo&type=sling:File

# Search within specific path
/path/to/resource.search.html?q=demo&path=/content/reference

# Disable fulltext (use LIKE queries only)
/path/to/resource.search.html?q=test&fulltext=false
```

## Global Search (Start Page)

The CMS start page includes a global search component for finding content across the repository.

### Location

The search form is embedded in the start content component:
- Template: `/libs/sling-cms/components/cms/startcontent/startcontent.html`
- Results: `/libs/sling-cms/components/cms/startcontent/search.html`

### How It Works

1. User enters search term in the "Find Content" panel
2. Form submits via AJAX to `{resource.path}.search.html?q={term}`
3. Results are loaded into the `.startcontent-search-results` container
4. Each result links to the content editor for that item

### Search Form HTML

```html
<form class="get-form" 
      action="${resource.path}.search.html" 
      data-target=".startcontent-search-results"
      data-load="div">
    <p class="control has-icons-left">
        <input class="input" type="text" name="q" placeholder="Search..." />
        <span class="icon is-left">
            <i class="jam jam-search"></i>
        </span>
    </p>
</form>
<div class="startcontent-search-results"></div>
```

## Site Search (Reference Implementation)

The reference site includes a search page for public-facing search.

### Components

- **Search Model**: `org.apache.sling.cms.reference.models.Search`
- **Search Page**: `/content/reference/en/search`
- **Template**: `/libs/reference/components/pages/search`

### Configuration

The reference Search model uses similar query patterns but is designed for site visitors rather than CMS authors.

## Index Management

### Viewing Index Status

Access the Oak index definitions via:
```
http://localhost:8080/oak:index.tidy.1.json
```

### Triggering Reindex

To rebuild an index (e.g., after schema changes):

```bash
curl -u admin:admin -X POST "http://localhost:8080/oak:index/slingPage" \
  -F "reindex=true" \
  -F "reindex@TypeHint=Boolean"
```

### Index Health Check

The CMS includes a health check for Lucene indexes:
- Name: `Jackrabbit Oak - Lucene Index`
- MBean: `org.apache.jackrabbit.oak:name=Lucene Index statistics,type=LuceneIndex`

## Troubleshooting

### "No Results" for Fulltext Search

**Symptom:** Search returns no results even for content that exists.

**Cause:** Fulltext queries require a Lucene index with `nodeScopeIndex=true` or analyzed properties.

**Solution:**
1. Check if the index has fulltext enabled:
   ```bash
   curl -u admin:admin "http://localhost:8080/oak:index/slingPage/indexRules/sling:Page/properties.tidy.2.json"
   ```
2. Ensure `allContent` property has `nodeScopeIndex=true` and `analyzed=true`
3. Trigger reindex if index schema was updated

### "Fulltext query without index" Warning

**Symptom:** Log shows warning about fulltext query without index.

**Cause:** The query uses `CONTAINS(s.*, ...)` but no index supports full node scope indexing.

**Solution:** The search now uses property-specific CONTAINS queries that work with analyzed properties in the existing indexes.

### Slow Search Performance

**Symptom:** Search queries are slow or cause traversal warnings.

**Solutions:**
1. Ensure `evaluatePathRestrictions=true` on indexes
2. Add path restrictions to queries when possible
3. Use specific node types instead of `nt:hierarchyNode` when possible
4. Check index covers the properties being queried

### Index Not Picking Up Changes

**Symptom:** New content doesn't appear in search results.

**Cause:** Async index hasn't processed the changes yet.

**Solution:**
1. Wait for async indexing (indexes use `async` + `nrt` mode)
2. Check async indexer status in JMX console
3. Verify index `reindex` property is `false` (not stuck in reindex)

## Extending Search

### Adding Custom Properties to Index

To index additional properties, extend `IndexCreator` or create a new `RepositoryInitializer`:

```java
@Component(service = RepositoryInitializer.class)
public class CustomIndexCreator implements RepositoryInitializer {
    
    @Override
    public void initialize(NodeBuilder builder) {
        NodeBuilder indexRoot = builder.child("oak:index");
        NodeBuilder slingPage = indexRoot.child("slingPage");
        NodeBuilder properties = slingPage
            .child("indexRules")
            .child("sling:Page")
            .child("properties");
        
        // Add custom property
        NodeBuilder customProp = properties.child("customField");
        customProp.setProperty("jcr:primaryType", "nt:unstructured", Type.NAME);
        customProp.setProperty("name", "jcr:content/customField");
        customProp.setProperty("propertyIndex", true);
        customProp.setProperty("analyzed", true);
    }
}
```

### Creating Custom Search Component

1. Create a Sling Model that adapts from `SlingHttpServletRequest`
2. Build JCR-SQL2 queries using indexed properties
3. Use `ResourceResolver.findResources()` to execute queries
4. Create HTL template to render results

```java
@Model(adaptables = SlingHttpServletRequest.class)
public class CustomSearch {
    
    @Self
    private SlingHttpServletRequest request;
    
    public List<Resource> getResults() {
        String term = request.getParameter("q");
        String query = "SELECT * FROM [sling:Page] WHERE " +
            "CONTAINS([jcr:content/customField], '" + term + "')";
        
        Iterator<Resource> it = request.getResourceResolver()
            .findResources(query, Query.JCR_SQL2);
        
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(it, Spliterator.NONNULL), false)
            .limit(50)
            .collect(Collectors.toList());
    }
}
```

## Best Practices

1. **Use specific node types** - Query `sling:Page` instead of `nt:hierarchyNode` when possible
2. **Add path restrictions** - Use `ISDESCENDANTNODE()` to limit search scope
3. **Leverage analyzed properties** - Use `CONTAINS()` on indexed, analyzed properties
4. **Limit results** - Always limit result count to prevent memory issues
5. **Escape user input** - Use `Text.escapeIllegalXpathSearchChars()` for search terms
6. **Monitor index health** - Check async indexer status and index size regularly

## Related Documentation

- [Digital Asset Management](digital-asset-management.md) - Asset search and filtering
- [Managing Taxonomy](managing-taxonomy.md) - Taxonomy search
- [Technology Stack](technology-stack.md) - Oak Lucene details
- [Tika Upgrade Plan](tika-upgrade-plan.md) - Content extraction for search
