# SPDX-License-Identifier: Apache-2.0

# Apache Sling CMS Publication System Analysis

**Document Version:** 1.0
**Date:** 2025-12-31
**Author:** System Analysis
**Project Version:** 1.1.9-SNAPSHOT

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current State Architecture](#current-state-architecture)
3. [Component Inventory](#component-inventory)
4. [Feature Analysis](#feature-analysis)
5. [Technical Debt & Issues](#technical-debt--issues)
6. [Future State Recommendations](#future-state-recommendations)
7. [Migration Path](#migration-path)
8. [Appendix](#appendix)

---

## Executive Summary

The Apache Sling CMS publication system provides a complete content distribution workflow supporting both **standalone** and **distributed** (author-publish) deployment models. The system enables content authors to publish pages, assets, and other resources from an author instance to one or more publisher instances.

### Key Capabilities

- ✅ **Dual Publication Modes**: Standalone (single instance) and Content Distribution (author-publish)
- ✅ **Single & Bulk Publication**: Publish individual resources or entire content trees
- ✅ **HTTP-based Distribution**: JSON-serialized content over HTTP with Basic Auth
- ✅ **Publication Metadata**: Tracks publication status, date, user, and type
- ✅ **Event System**: OSGi events for publication lifecycle hooks
- ✅ **Job Framework**: Asynchronous bulk publication with progress tracking
- ✅ **UI Dashboard**: Centralized publication management at `/cms/publication/home.html`

### Critical Findings

**Strengths:**
- Clean separation of concerns (API, implementation, UI)
- OSGi R7-compliant service architecture
- Extensible via OSGi event system
- Support for both shallow and deep publication

**Weaknesses:**
- ⚠️ Mixed JSP/HTL templates (JSP deprecation path incomplete)
- ⚠️ HTTP distribution has no retry/queue mechanism for failed distributions
- ⚠️ Basic Auth credentials stored in OSGi config (no secrets management)
- ⚠️ No conflict resolution for concurrent publication
- ⚠️ Limited observability (no metrics, minimal logging)
- ⚠️ Binary handling inefficient (base64 encoding in JSON)

---

## Current State Architecture

### 1. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Author Instance                          │
│                                                                 │
│  ┌──────────────┐      ┌──────────────────────────────────┐   │
│  │   UI Layer   │      │     Publication Dashboard         │   │
│  │              │      │   /cms/publication/home.html      │   │
│  │  Components  │◄─────┤                                   │   │
│  │   - Home     │      │  - Agents                         │   │
│  │   - Status   │      │  - Bulk Publication               │   │
│  │   - Agent    │      │  - Exporters/Importers            │   │
│  └──────┬───────┘      └────────────────┬──────────────────┘   │
│         │                               │                      │
│  ┌──────▼───────────────────────────────▼──────────────────┐   │
│  │              Sling Post Servlets                        │   │
│  │  - PublishPostOperation (:operation=publish)            │   │
│  │  - UnpublishPostOperation (:operation=unpublish)        │   │
│  └──────┬──────────────────────────────────────────────────┘   │
│         │                                                      │
│  ┌──────▼──────────────────────────────────────────────────┐   │
│  │         PublicationManagerFactory                       │   │
│  │  (Adapts ResourceResolver → PublicationManager)         │   │
│  └──────┬──────────────────────────────────────────────────┘   │
│         │                                                      │
│    ┌────┴─────┐                                               │
│    │          │                                               │
│  ┌─▼─────────────────┐      ┌──────────────────────────┐     │
│  │  Standalone       │      │  HttpDistribution        │     │
│  │  PublicationMgr   │      │  PublicationManager      │     │
│  │                   │      │                          │     │
│  │  - Updates JCR    │      │  - Calls Content         │     │
│  │    metadata       │      │    Distribution Service  │     │
│  │  - Fires events   │      │  - HTTP POST to          │     │
│  └───────────────────┘      │    publisher             │     │
│                             └──────────┬────────────────┘     │
│                                        │                      │
│                             ┌──────────▼────────────────┐     │
│                             │ ContentDistribution       │     │
│                             │ Service                   │     │
│                             │  - Serializes JCR → JSON  │     │
│                             │  - HTTP POST with auth    │     │
│                             │  - Configurable depth     │     │
│                             └──────────┬────────────────┘     │
│                                        │                      │
└────────────────────────────────────────┼──────────────────────┘
                                         │ HTTP POST
                                         │ /bin/cms/distribution/import
                                         │
┌────────────────────────────────────────▼──────────────────────┐
│                       Publisher Instance                      │
│                                                                │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │          ContentImportServlet                           │  │
│  │          /bin/cms/distribution/import                   │  │
│  │                                                         │  │
│  │  - Receives JSON payload                               │  │
│  │  - Parses action (ADD/DELETE)                          │  │
│  │  - Creates/updates/deletes resources                   │  │
│  │  - Uses 'distribution' service user                    │  │
│  └─────────────────────────┬───────────────────────────────┘  │
│                            │                                  │
│  ┌─────────────────────────▼───────────────────────────────┐  │
│  │              JCR Repository (Oak)                       │  │
│  │              /content tree                              │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### 2. Deployment Modes

#### Mode 1: Standalone

```
┌──────────────────────────┐
│   Single Instance        │
│                          │
│  - Author UI enabled     │
│  - Publication updates   │
│    JCR metadata only     │
│  - No distribution       │
│                          │
│  Port: 8080             │
└──────────────────────────┘
```

**Use Case:** Development, small sites, demo environments

**Configuration:**
```
instance.type = STANDALONE
publication.mode = STANDALONE
```

#### Mode 2: Content Distribution (Author-Publish)

```
┌──────────────────────┐         ┌──────────────────────┐
│   Author Instance    │         │  Publisher Instance  │
│                      │         │                      │
│  - Authoring UI      │  HTTP   │  - Public website    │
│  - Content creation  ├────────►│  - Read-only content │
│  - Distribution      │  JSON   │  - Import servlet    │
│                      │         │                      │
│  Port: 8082          │         │  Port: 8083          │
└──────────────────────┘         └──────────────────────┘
```

**Use Case:** Production deployments, scalable architecture

**Configuration (Author):**
```
instance.type = AUTHOR
publication.mode = CONTENT_DISTRIBUTION
distribution.endpoints = ["http://publisher1:8083", "http://publisher2:8083"]
distribution.username = admin
distribution.password = admin
```

**Configuration (Publisher):**
```
instance.type = RENDERER
publication.mode = CONTENT_DISTRIBUTION
```

### 3. Module Organization

| Module | Path | Responsibility |
|--------|------|----------------|
| **api** | `/api/src/main/java/.../publication/` | Publication interfaces, enums, exceptions |
| **core** | `/core/src/main/java/.../internal/models/` | PublishableResourceImpl Sling Model |
| **distribution** | `/distribution/src/main/java/.../distribution/` | All publication services, servlets, jobs |
| **ui** | `/ui/src/main/resources/jcr_root/libs/sling-cms/` | HTL/JSP components, content pages |

---

## Component Inventory

### 1. Java APIs (api module)

#### Interfaces

**PublicationManager** (`org.apache.sling.cms.publication.PublicationManager`)
```java
public interface PublicationManager {
    void publish(PublishableResource resource) throws PublicationException;
    void unpublish(PublishableResource resource) throws PublicationException;
    PUBLICATION_MODE getPublicationMode();
}
```

**PublicationManagerFactory** (`org.apache.sling.cms.publication.PublicationManagerFactory`)
```java
public interface PublicationManagerFactory {
    INSTANCE_TYPE getInstanceType();
    PUBLICATION_MODE getPublicationMode();
    PublicationManager getPublicationManager();
}
```

**PublishableResource** (`org.apache.sling.cms.PublishableResource`)
```java
public interface PublishableResource {
    // Status
    boolean isPublished();

    // Metadata
    Calendar getLastPublicationDate();
    String getLastPublicationBy();
    PublicationType getLastPublicationType();

    // Paths
    String getPath();
    String getPublishedPath();
    String getPublishedUrl();

    // Resource access
    Resource getResource();
    ValueMap getValueMap();
}
```

#### Enums

**PUBLICATION_MODE**
- `STANDALONE` - Single instance, metadata updates only
- `CONTENT_DISTRIBUTION` - Author-publish with HTTP distribution

**INSTANCE_TYPE**
- `STANDALONE` - Combined author/publisher
- `AUTHOR` - Content authoring instance
- `RENDERER` - Publishing/delivery instance

**PublicationType**
- `ADD` - Content published/updated
- `DELETE` - Content unpublished/removed
- `NONE` - No publication action

#### Exceptions

**PublicationException** - Thrown when publication operations fail

### 2. Core Services (distribution module)

#### PublicationManagerFactoryImpl

**Location:** `/distribution/src/main/java/org/apache/sling/cms/distribution/impl/PublicationManagerFactoryImpl.java`

**Role:** Central factory for obtaining PublicationManager instances

**Features:**
- OSGi Service + AdapterFactory
- Adaptable from: `ResourceResolver`, `SlingHttpServletRequest`
- Returns appropriate manager based on publication mode
- Configurable via OSGi config

**Configuration Class:**
```java
@ObjectClassDefinition(name = "Publication Configuration")
public @interface PublicationConfig {

    @AttributeDefinition(name = "Instance Type")
    INSTANCE_TYPE instance_type() default INSTANCE_TYPE.STANDALONE;

    @AttributeDefinition(name = "Publication Mode")
    PUBLICATION_MODE publication_mode() default PUBLICATION_MODE.STANDALONE;

    @AttributeDefinition(name = "Distribution Agents")
    String[] distribution_agents() default {};
}
```

#### StandalonePublicationManager

**Location:** `/distribution/src/main/java/org/apache/sling/cms/distribution/impl/StandalonePublicationManager.java`

**Role:** Handles publication in standalone mode (no distribution)

**Operations:**

**Publish:**
1. Sets `sling:published = true`
2. Sets `sling:lastPublication = <current date>`
3. Sets `sling:lastPublicationType = "ADD"`
4. Sets `sling:lastPublicationBy = <current user>`
5. Fires `org/apache/sling/cms/publication/PUBLISH` OSGi event

**Unpublish:**
1. Sets `sling:published = false`
2. Updates timestamp and user
3. Sets `sling:lastPublicationType = "DELETE"`
4. Fires `org/apache/sling/cms/publication/UNPUBLISH` OSGi event

#### HttpDistributionPublicationManager

**Location:** `/distribution/src/main/java/org/apache/sling/cms/distribution/impl/HttpDistributionPublicationManager.java`

**Role:** Extends StandalonePublicationManager with HTTP distribution

**Additional Operations:**
1. Calls parent (updates local metadata, fires events)
2. Invokes `ContentDistributionService.publish()` or `unpublish()`
3. Distributes content to configured publisher endpoints

#### ContentDistributionService

**Location:** `/distribution/src/main/java/org/apache/sling/cms/distribution/ContentDistributionService.java`

**Implementation:** `ContentDistributionServiceImpl`

**Key Methods:**

```java
public interface ContentDistributionService {

    // Publish operations
    void publish(String path) throws DistributionException;
    void publish(String path, boolean deep) throws DistributionException;
    void distribute(ResourceResolver resolver, String path, boolean deep)
        throws DistributionException;

    // Unpublish operation
    void unpublish(String path) throws DistributionException;
    void delete(ResourceResolver resolver, String path)
        throws DistributionException;
}
```

**Configuration:**

```java
@ObjectClassDefinition(name = "Content Distribution Configuration")
public @interface ContentDistributionConfig {

    @AttributeDefinition(name = "Enabled")
    boolean enabled() default false;

    @AttributeDefinition(name = "Publisher Endpoints")
    String[] publisher_endpoints() default {};

    @AttributeDefinition(name = "Username")
    String username() default "admin";

    @AttributeDefinition(name = "Password", type = AttributeType.PASSWORD)
    String password() default "admin";

    @AttributeDefinition(name = "Connection Timeout (ms)")
    int connection_timeout() default 10000;

    @AttributeDefinition(name = "Socket Timeout (ms)")
    int socket_timeout() default 30000;

    @AttributeDefinition(name = "Allowed Content Roots")
    String[] allowed_content_roots() default {"/content"};

    @AttributeDefinition(name = "Use Content Package")
    boolean use_content_package() default false;

    @AttributeDefinition(name = "Async Distribution")
    boolean async_distribution() default false;
}
```

**Export Logic (JSON Serialization):**

The service exports JCR content to JSON format:

```json
{
  "path": "/content/mysite/page",
  "action": "ADD",
  "deep": true,
  "content": {
    "jcr:primaryType": "sling:Page",
    "jcr:created": "2025-01-15T10:30:00.000Z",
    "jcr:content": {
      "jcr:primaryType": "nt:unstructured",
      "sling:resourceType": "sling-cms/components/pages/page",
      "jcr:title": "My Page",
      "children": [
        {
          "name": "child-resource",
          "properties": {...}
        }
      ]
    }
  }
}
```

**Property Handling:**
- **String, Long, Boolean, Double**: Direct serialization
- **Date/Calendar**: ISO 8601 format
- **Binary**: Base64 encoded (⚠️ inefficient for large files)
- **Multi-value properties**: JSON arrays
- **Reference properties**: Preserved as strings

**Distribution Flow:**
1. Serialize resource tree to JSON
2. Create HTTP POST request to each endpoint
3. Set Basic Auth header
4. POST to `/bin/cms/distribution/import`
5. Handle response (success/failure)
6. Log errors, throw DistributionException on failure

#### ContentImportServlet

**Location:** `/distribution/src/main/java/org/apache/sling/cms/distribution/impl/ContentImportServlet.java`

**Endpoint:** `/bin/cms/distribution/import`

**Methods:** POST only

**Authentication:** Basic Auth (configured on publisher)

**Service User:** `distribution` (requires service user mapping)

**Request Format:**

```json
{
  "path": "/content/mysite/page",
  "action": "ADD" | "DELETE",
  "deep": true,
  "content": { ... }
}
```

**Import Logic (ADD action):**
1. Parse JSON payload
2. Get or create parent path
3. Get or create resource at target path
4. Update properties from payload
5. Recursively process children if deep=true
6. Commit changes
7. Return success/failure JSON

**Import Logic (DELETE action):**
1. Parse JSON payload
2. Get resource at path
3. Delete resource and descendants
4. Commit changes
5. Return success/failure

**Security:**
- Requires authentication
- Uses dedicated service user
- Validates allowed content roots (should - currently not implemented)
- No CSRF protection (servlet-to-servlet communication)

#### BulkPublicationJob

**Location:** `/distribution/src/main/java/org/apache/sling/cms/distribution/impl/BulkPublicationJob.java`

**Job Topic:** `cmsjob/org/apache/sling/cms/publication/Bulk`

**Job Parameters:**

```java
{
  "paths": String or String[],      // Content paths to publish
  "type": "ADD" | "DELETE",          // Publication type
  "deep": true | false               // Include descendants
}
```

**Processing Logic:**

1. Parse job properties
2. For each path:
   - Get resource
   - If deep=true, stream descendants using `ResourceTree.stream()`
   - Filter by predicates:
     - `IsPublishableResourceType` (sling:Page, sling:File)
     - `IsPublishableResourceContainer` (folders, sites, pages)
3. For each publishable resource:
   - Adapt to PublishableResource
   - Call publish() or unpublish()
   - Update job progress context
4. Log results
5. Return JobExecutionResult (success/partial/failure)

**Resource Filtering:**

```java
// Publishable types
- sling:Page
- sling:File
- sling:OrderedFolder (if deep)
- sling:Folder (if deep)
- sling-cms/components/general/site (if deep)
```

**Progress Tracking:**
- Uses JobExecutionContext
- Logs each processed resource
- Reports total count, success count, failure count

#### Post Operations

**PublishPostOperation**
- **Operation:** `:operation=publish`
- **Adapts** resource to PublishableResource
- **Calls** publish()
- **Returns** success/error JSON

**UnpublishPostOperation**
- **Operation:** `:operation=unpublish`
- **Adapts** resource to PublishableResource
- **Calls** unpublish()
- **Returns** success/error JSON

### 3. Sling Models (core module)

#### PublishableResourceImpl

**Location:** `/core/src/main/java/org/apache/sling/cms/core/internal/models/PublishableResourceImpl.java`

**Adaptable:** `Resource`

**Adapter:** `PublishableResource`

**Data Source:** JCR properties on `jcr:content` node

**Properties Read:**

| Property | Type | Description |
|----------|------|-------------|
| `sling:published` | Boolean | Publication status |
| `sling:lastPublication` | Calendar | Last publication date |
| `sling:lastPublicationType` | String | ADD/DELETE/NONE |
| `sling:lastPublicationBy` | String | User who published |
| `published` | Boolean | Legacy property (deprecated) |

**Computed Values:**

**publishedPath:**
- For pages: `/content/site/page` → `/content/site/page`
- Relative to site root

**publishedUrl:**
- Looks up parent Site
- Constructs URL: `{site.url}{publishedPath}.html`
- Falls back to path if no site

**Legacy Support:**
- Checks both `sling:published` and `published` properties
- Maintains backward compatibility with older content

### 4. UI Components

#### Publication Home Component

**Location:** `/libs/sling-cms/components/publication/home/home.html`

**Content:** `/libs/sling-cms/content/publication/home.json`

**URL:** `/cms/publication/home.html`

**Sling Model:** `org.apache.sling.cms.core.models.PublicationHome`

**Features:**
- Displays publication mode label
- Navigation tiles:
  - **Agents** → `/cms/publication/agents.html`
  - **Bulk Publication** → `/cms/jobs/list.html`
  - **Exporters** → `/cms/publication/exporters.html`
  - **Importers** → `/cms/publication/importers.html`

**Template:**
```html
<sly data-sly-use.model="org.apache.sling.cms.core.models.PublicationHome">
  <div class="publication-home">
    <div class="publication-mode">
      <strong>Publication Mode:</strong> ${model.publicationMode}
    </div>
    <div class="tiles">
      <!-- Navigation tiles -->
    </div>
  </div>
</sly>
```

#### Publication Status Component

**Location:** `/libs/sling-cms/components/publication/status/status.jsp` ⚠️ **JSP - Should migrate to HTL**

**Usage:** Displays publication metadata for a resource

**Rendered Data:**
- Published: Yes/No badge
- Last Publication: Date and time
- Last Published By: Username
- Publication Type: ADD/DELETE icon

**Example Output:**
```
┌─────────────────────────────┐
│ Published: ✓ Yes            │
│ Last Publication: 2025-12-31│
│ Last Published By: admin    │
│ Type: ADD                   │
└─────────────────────────────┘
```

#### Publication Agent Component

**Location:** `/libs/sling-cms/components/publication/agent/agent.jsp` ⚠️ **JSP - Should migrate to HTL**

**Usage:** Displays distribution agent configuration and status

**Features:**
- Agent name and title
- Configuration details
- Queue status per endpoint
- Queue item count
- Agent state (idle/processing/blocked)
- Agent logs (iframe)
- Test button to validate agent

**Data Source:** Sling Distribution API (if using Sling Distribution)

#### Exporter/Importer Components

**Exporter:** `/libs/sling-cms/components/publication/exporter/exporter.jsp` ⚠️ **JSP**

**Importer:** `/libs/sling-cms/components/publication/importer/importer.jsp` ⚠️ **JSP**

**Purpose:** Display configuration for exporters and importers

**Note:** These may be related to Sling Distribution framework (not actively used by HTTP distribution)

#### Publish/Unpublish Modals

**Publish Modal:** `/libs/sling-cms/content/shared/publish.json`

**Unpublish Modal:** `/libs/sling-cms/content/shared/unpublish.json`

**Features:**
- Confirmation dialog
- Deep publish option (checkbox)
- Submit triggers POST with `:operation=publish` or `unpublish`

### 5. Bulk Publication UI

**Configuration:** `/libs/sling-cms/content/publication/bulk.json`

**Form Fields:**

| Field | Type | Configuration |
|-------|------|---------------|
| **Paths** | PathBrowser | Base: `/content`, Types: `sling:Page, sling:Folder, sling:OrderedFolder`, Required |
| **Type** | Select | Options: Publish (ADD), Unpublish (DELETE) |
| **Is Deep** | Select | Options: Yes (true), No (false) |
| **Topic** | Hidden | Value: `cmsjob/org/apache/sling/cms/publication/Bulk` |

**Submission:**
- Creates Sling Job with configured topic
- Job picked up by BulkPublicationJob executor
- Progress viewable in Jobs list

**Job List:** `/cms/jobs/list.html`
- Shows all jobs (publication and others)
- Status: queued, active, succeeded, failed
- Job details: start time, end time, duration, result

---

## Feature Analysis

### 1. Export Mechanism

**Format:** JSON-serialized JCR content

**Depth Control:**
- `deep=false`: Single resource only
- `deep=true`: Resource + all descendants

**Content Structure:**

```json
{
  "path": "/content/site/page",
  "action": "ADD",
  "deep": true,
  "content": {
    "jcr:primaryType": "sling:Page",
    "properties": {
      "jcr:created": "2025-01-15T10:30:00.000Z",
      "jcr:title": "My Page"
    },
    "children": [
      {
        "name": "jcr:content",
        "jcr:primaryType": "nt:unstructured",
        "properties": {...},
        "children": [...]
      }
    ]
  }
}
```

**Property Type Handling:**

| JCR Type | JSON Representation | Notes |
|----------|---------------------|-------|
| String | `"value"` | Direct |
| Long | `123` | Number |
| Double | `123.45` | Number |
| Boolean | `true/false` | Boolean |
| Date/Calendar | `"2025-12-31T10:30:00.000Z"` | ISO 8601 |
| Binary | `"base64EncodedString"` | ⚠️ Inefficient for large binaries |
| Reference | `"/path/to/node"` | String |
| Multi-value | `["val1", "val2"]` | Array |

**Limitations:**

❌ **Binary Handling:**
- Base64 encoding bloats payload (33% overhead)
- No streaming support
- Memory intensive for large assets
- Should use separate asset synchronization

❌ **No Chunking:**
- Large trees sent as single request
- Risk of timeout or OOM
- No progress tracking during transfer

❌ **No Versioning:**
- Overwrites target content
- No conflict detection
- No merge capability

### 2. Import Mechanism

**Endpoint:** `/bin/cms/distribution/import` (POST only)

**Authentication:** Basic Auth

**Service User:** `distribution` (requires mapping in `org.apache.sling.serviceusermapping`)

**Actions:**

**ADD Action:**
1. Parse JSON payload
2. Create parent paths if needed (`ResourceUtil.getOrCreateResource`)
3. Get or create resource at target path
4. Update properties from payload
5. Recursively process children
6. Commit transaction
7. Return success JSON

**DELETE Action:**
1. Parse JSON payload
2. Get resource at path
3. Delete resource (cascades to children)
4. Commit transaction
5. Return success JSON

**Error Handling:**
- Catches exceptions
- Returns JSON error response
- Logs errors
- Does NOT rollback partial imports (⚠️ potential inconsistency)

**Security Considerations:**

✅ **Good:**
- Requires authentication
- Uses service user (not admin)
- POST only (no GET)

⚠️ **Missing:**
- No CSRF protection (acceptable for servlet-to-servlet)
- No validation of allowed content roots
- No rate limiting
- Credentials in OSGi config (plaintext)

### 3. Distribution Agents

**Configuration Location:** `/libs/sling/distribution/settings/agents`

**Agent Types:**
- Forward agents (author → publisher)
- Reverse agents (publisher → author, e.g., user-generated content)
- Sync agents (bidirectional)

**Queue Management:**
- Each endpoint has dedicated queue
- Queue states: idle, running, blocked, stopped
- Item count tracking
- Manual queue operations (pause, resume, clear)

**UI Features:**
- Agent configuration display
- Queue status per endpoint
- Agent logs viewer (iframe)
- Test functionality

**Integration:**
- Currently: Sling Distribution framework (if enabled)
- HTTP Distribution: Custom implementation (ContentDistributionService)

**Note:** The agent UI components reference Sling Distribution API, but HTTP distribution uses a custom, simpler approach. This creates some UI/backend mismatch.

### 4. Bulk Publication

**User Workflow:**

1. Navigate to `/cms/publication/home.html`
2. Click "Bulk Publication" tile
3. Redirected to `/cms/publication/bulk.html` (form)
4. Select content paths via PathBrowser
5. Choose publication type (Publish/Unpublish)
6. Choose deep option (Yes/No)
7. Submit form
8. Job created and queued
9. View progress at `/cms/jobs/list.html`

**Backend Workflow:**

1. Form POST creates Sling Job
2. Job topic: `cmsjob/org/apache/sling/cms/publication/Bulk`
3. BulkPublicationJob executor picks up job
4. For each path:
   - Stream resources if deep=true
   - Filter by predicates
   - Publish/unpublish each
   - Update progress
5. Job completes with result (success/partial/failure)

**Features:**

✅ **Strengths:**
- Asynchronous processing (doesn't block UI)
- Progress tracking
- Handles large content trees
- Filters by resource type
- Logs each operation

⚠️ **Limitations:**
- No scheduling (immediate execution only)
- No retry on partial failure
- Limited configurability (no custom predicates)
- No preview mode (dry run)

**Performance Considerations:**

- Streams resources to avoid loading entire tree in memory
- Publishes sequentially (not parallel)
- Each publish is separate HTTP request if HTTP distribution enabled
- Large bulk operations can take significant time

**Suggested Improvements:**
- Parallel publishing (configurable thread pool)
- Batch HTTP requests (multiple resources per request)
- Dry run mode (preview what will be published)
- Scheduling support (publish at specific time)
- Retry logic with exponential backoff

---

## Technical Debt & Issues

### 1. JSP Components (CRITICAL)

**Issue:** Several publication components still use JSP instead of HTL

**Affected Components:**
- `/libs/sling-cms/components/publication/status/status.jsp`
- `/libs/sling-cms/components/publication/agent/agent.jsp`
- `/libs/sling-cms/components/publication/exporter/exporter.jsp`
- `/libs/sling-cms/components/publication/importer/importer.jsp`

**Impact:**
- Inconsistent with project standards (HTL mandated for new components)
- Harder to maintain
- Less secure (scriptlet risk)
- Poor separation of concerns

**Recommendation:**
- **Priority: HIGH**
- Migrate to HTL following `/docs/jsp-to-htl-migration.md`
- Create Sling Models for business logic
- Use semantic CSS classes

### 2. Binary Distribution (DESIGN FLAW)

**Issue:** Binary content (images, PDFs, videos) distributed via base64-encoded JSON

**Problems:**
- 33% size overhead (base64 encoding)
- Memory intensive (entire binary loaded into memory)
- Timeout risk (large binaries in HTTP request)
- Inefficient (no streaming, no compression)

**Current Flow:**
```
Binary (10 MB) → Base64 (13.3 MB) → JSON → HTTP POST → Base64 Decode → Binary
```

**Recommendation:**
- **Priority: HIGH**
- Implement separate asset synchronization:
  - Option A: Direct binary transfer (multipart HTTP)
  - Option B: S3/blob storage sync (author and publisher use shared storage)
  - Option C: Sling Content Distribution with package format
- Exclude binaries from JSON serialization
- Use reference-based approach (sync assets separately, reference by path)

### 3. No Retry/Queue for HTTP Distribution (RELIABILITY)

**Issue:** Failed HTTP distributions are not retried or queued

**Current Behavior:**
- HTTP POST fails → Exception thrown → Publication metadata updated anyway
- No persistent queue
- No retry mechanism
- User sees "published" but content not on publisher

**Impact:**
- Data inconsistency between author and publisher
- Network glitches cause silent failures
- No way to recover without manual republishing

**Recommendation:**
- **Priority: HIGH**
- Implement persistent queue (Sling Jobs or custom)
- Retry logic with exponential backoff
- Failed item tracking and monitoring
- UI to view/retry failed distributions

**Suggested Architecture:**

```
PublicationManager
  ↓
Update local metadata
  ↓
Enqueue distribution job → [Persistent Queue]
  ↓
DistributionJobExecutor
  ↓
Attempt HTTP POST
  ↓ (if fail)
Retry with backoff
  ↓ (if all retries fail)
Mark as failed, alert admin
```

### 4. Credentials Management (SECURITY)

**Issue:** Publisher credentials stored in OSGi config (plaintext or obscured)

**Current:**
```java
@AttributeDefinition(name = "Password", type = AttributeType.PASSWORD)
String password() default "admin";
```

**Problems:**
- `AttributeType.PASSWORD` provides minimal obfuscation (not encryption)
- Credentials visible in OSGi config dump
- No secrets rotation support
- No integration with secrets management systems

**Recommendation:**
- **Priority: MEDIUM**
- Integrate with secrets management:
  - Apache Sling Commons Crypto (encryption)
  - Environment variables (for containerized deployments)
  - External secrets managers (HashiCorp Vault, AWS Secrets Manager)
- Support certificate-based authentication (mTLS)
- Token-based auth instead of Basic Auth

### 5. No Conflict Resolution (DATA INTEGRITY)

**Issue:** Concurrent publications can cause race conditions

**Scenario:**
1. User A publishes `/content/site/page` (version 1)
2. User B edits and publishes `/content/site/page` (version 2)
3. Both HTTP POSTs in flight
4. Publisher receives out-of-order
5. Result: Data inconsistency

**Current Mitigation:** None (last-write-wins)

**Recommendation:**
- **Priority: MEDIUM**
- Implement optimistic locking (version numbers, last-modified timestamps)
- Detect conflicts and reject stale updates
- UI notification for conflicts
- Manual conflict resolution workflow

### 6. Limited Observability (OPERATIONS)

**Issue:** Insufficient monitoring and metrics

**Missing:**
- Publication success/failure metrics
- HTTP distribution latency metrics
- Queue depth monitoring
- Error rate tracking
- Publication audit trail (beyond JCR metadata)

**Recommendation:**
- **Priority: MEDIUM**
- Emit OSGi metrics (via Dropwizard Metrics or Micrometer)
- Structured logging (JSON logs with correlation IDs)
- Health check endpoints (publication system health)
- Integration with monitoring tools (Prometheus, Grafana)

### 7. Agent UI Mismatch (TECHNICAL DEBT)

**Issue:** Agent UI components reference Sling Distribution API, but HTTP distribution doesn't use it

**Current:**
- Agent JSP components expect Sling Distribution services
- HTTP distribution uses custom ContentDistributionService
- UI shows agents/exporters/importers that may not exist

**Impact:**
- Confusing UX (dead links, empty pages)
- Misleading information
- Inconsistent terminology

**Recommendation:**
- **Priority: LOW**
- Clarify distribution strategy:
  - Option A: Fully migrate to Sling Distribution framework
  - Option B: Remove agent UI, simplify to HTTP-only
  - Option C: Adapt UI to show HTTP distribution status
- Update documentation to reflect actual architecture

### 8. No Content Validation (QUALITY)

**Issue:** Published content not validated before distribution

**Current:**
- No schema validation
- No broken link checking
- No required field validation
- No component policy enforcement

**Impact:**
- Invalid content on publisher
- Broken pages in production
- Poor content quality

**Recommendation:**
- **Priority: LOW**
- Pre-publication validation hooks
- Component validation rules
- Required metadata checks
- Broken link detection
- Preview mode (validate before publish)

---

## Future State Recommendations

### Vision Statement

**Transform the publication system into a robust, enterprise-grade content distribution platform with:**

1. **Reliability:** Guaranteed content delivery with retries, monitoring, and auditing
2. **Performance:** Efficient binary handling, parallel distribution, and bulk operations
3. **Security:** Secrets management, mTLS, and comprehensive audit trails
4. **Usability:** Modern HTL UI, conflict resolution, and scheduling
5. **Observability:** Metrics, health checks, and operational dashboards

### Recommended Architecture (Target State)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Author Instance                          │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  Publication UI (HTL)                     │  │
│  │  - Modern component library                              │  │
│  │  - Real-time status updates (WebSocket)                  │  │
│  │  - Conflict resolution workflow                          │  │
│  │  - Scheduling & preview                                  │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                         │
│  ┌────────────────────▼─────────────────────────────────────┐  │
│  │         Publication Orchestrator Service                 │  │
│  │  - Validation pipeline                                   │  │
│  │  - Version control & conflict detection                  │  │
│  │  - Workflow routing (approve, review, publish)           │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                         │
│         ┌─────────────┴──────────────┐                          │
│         │                            │                          │
│  ┌──────▼─────────┐      ┌──────────▼──────────┐               │
│  │  Metadata Sync │      │  Content/Asset Sync │               │
│  │                │      │                     │               │
│  │  - JCR props   │      │  - Separate binary  │               │
│  │  - Versioning  │      │    transfer         │               │
│  │  - Refs        │      │  - Streaming        │               │
│  └──────┬─────────┘      │  - Compression      │               │
│         │                │  - Checksums        │               │
│         │                └──────────┬──────────┘               │
│         │                           │                          │
│  ┌──────▼───────────────────────────▼──────────────────────┐  │
│  │          Persistent Distribution Queue                   │  │
│  │  - Retry logic (exponential backoff)                     │  │
│  │  - Priority-based processing                             │  │
│  │  - Batch optimization                                    │  │
│  │  - Failed item tracking                                  │  │
│  └────────────────────────┬─────────────────────────────────┘  │
│                           │                                    │
│  ┌────────────────────────▼─────────────────────────────────┐  │
│  │         Distribution Transport Layer                     │  │
│  │  - mTLS authentication                                   │  │
│  │  - Compression (gzip)                                    │  │
│  │  - Multiplexing (HTTP/2)                                 │  │
│  └────────────────────────┬─────────────────────────────────┘  │
│                           │                                    │
│  ┌────────────────────────▼─────────────────────────────────┐  │
│  │         Metrics & Monitoring                             │  │
│  │  - Publication rate, latency, errors                     │  │
│  │  - Queue depth, retry count                              │  │
│  │  - Health checks                                         │  │
│  └──────────────────────────────────────────────────────────┘  │
│                           │ HTTPS (mTLS)                       │
└───────────────────────────┼────────────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────────────┐
│                       Publisher Instance(s)                    │
│                                                                │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │          Content Import Service                         │  │
│  │  - Schema validation                                    │  │
│  │  - Transaction management (rollback on error)           │  │
│  │  - Version tracking                                     │  │
│  │  - Event emission (for cache invalidation)             │  │
│  └─────────────────────────┬───────────────────────────────┘  │
│                            │                                  │
│  ┌─────────────────────────▼───────────────────────────────┐  │
│  │         JCR Repository (Oak)                            │  │
│  │         + Binary Store (S3/Azure Blob/FileSystem)       │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### Feature Roadmap

#### Phase 1: Foundation & Stability (Q1)

**Goal:** Address critical technical debt, improve reliability

1. **Migrate JSP to HTL**
   - Convert all publication components to HTL
   - Create Sling Models for business logic
   - Implement semantic CSS with framework abstraction
   - **Effort:** 2-3 weeks
   - **Priority:** HIGH

2. **Implement Persistent Queue**
   - Use Sling Jobs for distribution queue
   - Retry logic with exponential backoff (3 retries: 5s, 25s, 125s)
   - Failed item tracking
   - **Effort:** 1-2 weeks
   - **Priority:** HIGH

3. **Separate Binary Distribution**
   - Exclude binaries from JSON serialization
   - Implement direct binary transfer (multipart HTTP)
   - Add checksums for integrity verification
   - **Effort:** 2-3 weeks
   - **Priority:** HIGH

4. **Enhanced Logging & Metrics**
   - Structured JSON logging with correlation IDs
   - OSGi metrics (success rate, latency, queue depth)
   - Health check endpoint
   - **Effort:** 1 week
   - **Priority:** MEDIUM

#### Phase 2: Security & Compliance (Q2)

**Goal:** Harden security, meet enterprise requirements

1. **Secrets Management**
   - Integrate Sling Commons Crypto
   - Support environment variable credentials
   - External secrets manager integration (Vault, AWS Secrets Manager)
   - **Effort:** 1-2 weeks
   - **Priority:** MEDIUM

2. **mTLS Support**
   - Certificate-based authentication
   - Publisher certificate validation
   - **Effort:** 1 week
   - **Priority:** MEDIUM

3. **Audit Trail**
   - Comprehensive publication audit log
   - Who, what, when, where tracking
   - Retention policy support
   - **Effort:** 1 week
   - **Priority:** MEDIUM

4. **Content Validation**
   - Pre-publication validation hooks
   - Required metadata checks
   - Component policy enforcement
   - **Effort:** 2 weeks
   - **Priority:** LOW

#### Phase 3: Advanced Features (Q3)

**Goal:** Improve usability, enable advanced workflows

1. **Conflict Resolution**
   - Optimistic locking with version numbers
   - Conflict detection and UI notification
   - Manual merge workflow
   - **Effort:** 2-3 weeks
   - **Priority:** MEDIUM

2. **Scheduled Publication**
   - Time-based publication (publish at future date/time)
   - Recurring schedules (weekly content updates)
   - Timezone support
   - **Effort:** 1-2 weeks
   - **Priority:** LOW

3. **Preview & Dry Run**
   - Preview what will be published (before commit)
   - Dry run mode (simulate without changes)
   - Impact analysis (dependencies, references)
   - **Effort:** 2 weeks
   - **Priority:** LOW

4. **Batch Optimization**
   - Parallel publishing (configurable thread pool)
   - Batch HTTP requests (multiple resources per request)
   - Smart batching (group related resources)
   - **Effort:** 1-2 weeks
   - **Priority:** MEDIUM

#### Phase 4: Enterprise Scale (Q4)

**Goal:** Support large-scale deployments

1. **Multi-Publisher Support**
   - Fan-out to multiple publishers
   - Publisher groups (staging, production, regional)
   - Selective publishing (target specific publishers)
   - **Effort:** 2 weeks
   - **Priority:** LOW

2. **CDN Integration**
   - Cache invalidation on publish/unpublish
   - Fastly, Cloudflare, Akamai integration
   - Purge API support
   - **Effort:** 2-3 weeks
   - **Priority:** LOW

3. **Real-Time Status Updates**
   - WebSocket-based UI updates
   - Live queue monitoring
   - Publication progress bar
   - **Effort:** 1-2 weeks
   - **Priority:** LOW

4. **Advanced Analytics**
   - Publication frequency dashboard
   - Content freshness reports
   - Performance trending
   - **Effort:** 1 week
   - **Priority:** LOW

### API Evolution

#### Backward Compatibility

**Maintain existing APIs** in `org.apache.sling.cms.publication`:
- PublicationManager
- PublicationManagerFactory
- PublishableResource

**Deprecate and replace** with new APIs:

```java
// New API (proposed)
package org.apache.sling.cms.publication.v2;

public interface PublicationService {

    // Synchronous publish (waits for completion)
    PublicationResult publish(PublicationRequest request)
        throws PublicationException;

    // Asynchronous publish (returns immediately)
    CompletableFuture<PublicationResult> publishAsync(PublicationRequest request);

    // Batch publish (optimized for multiple resources)
    List<PublicationResult> publishBatch(List<PublicationRequest> requests)
        throws PublicationException;

    // Schedule publish for future time
    ScheduledPublication schedulePublish(PublicationRequest request, Instant publishTime);

    // Preview (dry run)
    PublicationPreview preview(PublicationRequest request);
}

public class PublicationRequest {
    private String path;
    private boolean deep;
    private PublicationMode mode; // PUBLISH, UNPUBLISH, ACTIVATE, DEACTIVATE
    private Set<String> targetPublishers; // null = all
    private Map<String, Object> metadata;
    private ValidationLevel validationLevel; // NONE, BASIC, STRICT
}

public class PublicationResult {
    private boolean success;
    private String path;
    private Instant timestamp;
    private String correlationId;
    private List<PublisherResult> publisherResults;
    private List<ValidationError> validationErrors;
}

public class PublisherResult {
    private String publisherId;
    private boolean success;
    private String errorMessage;
    private long latencyMs;
}
```

#### Migration Strategy

1. **Phase 1:** Introduce new APIs alongside existing ones
2. **Phase 2:** Implement adapters (old API → new API)
3. **Phase 3:** Deprecate old APIs (mark with @Deprecated, update docs)
4. **Phase 4:** Remove old APIs (major version bump: 2.0.0)

**Timeline:** 12-18 months (gives adopters time to migrate)

### Configuration Evolution

#### Current Config (OSGi)

```
org.apache.sling.cms.distribution.impl.PublicationConfig
org.apache.sling.cms.distribution.impl.ContentDistributionConfig
```

#### Proposed Config (Enhanced)

```java
@ObjectClassDefinition(name = "Publication Service Configuration v2")
public @interface PublicationServiceConfig {

    // Mode
    @AttributeDefinition(name = "Publication Mode")
    PublicationMode mode() default PublicationMode.HTTP_DISTRIBUTION;

    // Retry
    @AttributeDefinition(name = "Max Retries")
    int max_retries() default 3;

    @AttributeDefinition(name = "Retry Backoff (ms)")
    long retry_backoff_ms() default 5000;

    // Performance
    @AttributeDefinition(name = "Parallel Threads")
    int parallel_threads() default 4;

    @AttributeDefinition(name = "Batch Size")
    int batch_size() default 10;

    // Security
    @AttributeDefinition(name = "Use mTLS")
    boolean use_mtls() default false;

    @AttributeDefinition(name = "Secrets Provider")
    String secrets_provider() default "osgi"; // osgi, vault, aws, env

    // Validation
    @AttributeDefinition(name = "Enable Pre-Publish Validation")
    boolean enable_validation() default true;

    @AttributeDefinition(name = "Validation Level")
    ValidationLevel validation_level() default ValidationLevel.BASIC;

    // Monitoring
    @AttributeDefinition(name = "Enable Metrics")
    boolean enable_metrics() default true;

    @AttributeDefinition(name = "Metrics Provider")
    String metrics_provider() default "dropwizard"; // dropwizard, micrometer
}

@ObjectClassDefinition(name = "Publisher Endpoint Configuration")
public @interface PublisherEndpointConfig {

    @AttributeDefinition(name = "Publisher ID")
    String id();

    @AttributeDefinition(name = "Endpoint URL")
    String url();

    @AttributeDefinition(name = "Group")
    String group() default "default"; // staging, production, regional

    @AttributeDefinition(name = "Enabled")
    boolean enabled() default true;

    @AttributeDefinition(name = "Auth Type")
    AuthType auth_type() default AuthType.BASIC; // BASIC, MTLS, TOKEN

    @AttributeDefinition(name = "Credentials Reference")
    String credentials_ref(); // Reference to secret

    @AttributeDefinition(name = "Priority")
    int priority() default 0; // Higher priority processed first

    @AttributeDefinition(name = "Timeout (ms)")
    int timeout_ms() default 30000;
}
```

**Factory Configuration:** Use OSGi factory config for multiple publishers

```
# Author instance - publisher configs
org.apache.sling.cms.publication.PublisherEndpoint-staging.cfg.json
org.apache.sling.cms.publication.PublisherEndpoint-production.cfg.json
org.apache.sling.cms.publication.PublisherEndpoint-regional-eu.cfg.json
```

---

## Migration Path

### Step-by-Step Implementation Plan

#### Step 1: JSP to HTL Migration (2-3 weeks)

**Tasks:**

1. **Create Sling Models**
   - `PublicationStatusModel` - For status component
   - `PublicationAgentModel` - For agent component
   - `PublicationExporterModel` - For exporter component
   - `PublicationImporterModel` - For importer component

2. **Convert Templates**
   - `status.jsp` → `status.html`
   - `agent.jsp` → `agent.html`
   - `exporter.jsp` → `exporter.html`
   - `importer.jsp` → `importer.html`

3. **Create SCSS Files**
   - `_publication-status.scss`
   - `_publication-agent.scss`
   - Import in `cms.scss`

4. **Testing**
   - Verify UI rendering
   - Test all interactive elements
   - Validate data binding

**Acceptance Criteria:**
- All publication components use HTL
- No JSP files in publication components
- Semantic CSS classes (no direct Bulma)
- Unit tests for Sling Models

#### Step 2: Persistent Queue Implementation (1-2 weeks)

**Tasks:**

1. **Create DistributionJob**
   - Job topic: `cmsjob/org/apache/sling/cms/distribution/HTTP`
   - Job properties: path, action, deep, targetPublishers
   - Job executor: `HttpDistributionJobExecutor`

2. **Update HttpDistributionPublicationManager**
   - Instead of immediate HTTP POST, create Sling Job
   - Update local metadata immediately (optimistic)
   - Return success (job queued)

3. **Implement HttpDistributionJobExecutor**
   - Execute HTTP distribution
   - Retry logic (3 attempts, exponential backoff)
   - On success: update job result
   - On failure: retry or mark failed

4. **Create Failed Distribution UI**
   - List failed distributions at `/cms/publication/failed.html`
   - Retry button (requeue job)
   - Delete button (dismiss)

**Acceptance Criteria:**
- HTTP distributions queued as Sling Jobs
- Automatic retries on failure (3 attempts)
- Failed distributions tracked and visible
- Manual retry capability

#### Step 3: Binary Distribution Optimization (2-3 weeks)

**Tasks:**

1. **Create BinaryDistributionService**
   - Detect binary properties during export
   - Skip binary in JSON, add reference
   - Separate HTTP request per binary (multipart upload)

2. **Update ContentImportServlet**
   - Accept multipart requests
   - Handle binary uploads separately
   - Store binaries in JCR

3. **Add Checksum Verification**
   - Calculate SHA-256 of binary on author
   - Include in metadata
   - Verify on publisher after import

4. **Testing**
   - Test with large binaries (100MB+)
   - Verify integrity (checksums match)
   - Performance benchmarking

**Acceptance Criteria:**
- Binaries not base64-encoded in JSON
- Separate binary transfer (streaming)
- Checksum verification
- Performance improvement (measure before/after)

#### Step 4: Metrics & Monitoring (1 week)

**Tasks:**

1. **Add Metrics**
   - Counter: publications_total (labels: action, result)
   - Histogram: publication_duration_ms
   - Gauge: queue_depth
   - Counter: retry_count

2. **Structured Logging**
   - JSON log format
   - Correlation ID per publication
   - Include: user, path, action, result, latency

3. **Health Check**
   - Endpoint: `/system/health/publication`
   - Checks:
     - Queue depth < threshold
     - Failed distributions < threshold
     - Last successful publication < X minutes ago

**Acceptance Criteria:**
- Metrics exposed (JMX or HTTP endpoint)
- JSON logs with correlation IDs
- Health check endpoint functional

#### Step 5: Secrets Management (1-2 weeks)

**Tasks:**

1. **Integrate Sling Commons Crypto**
   - Encrypt publisher passwords in OSGi config
   - Decrypt at runtime

2. **Environment Variable Support**
   - Read credentials from env vars (for containers)
   - Format: `PUBLISHER_${ID}_USERNAME`, `PUBLISHER_${ID}_PASSWORD`

3. **External Secrets Provider (Optional)**
   - Abstract interface: `SecretsProvider`
   - Implementations: Vault, AWS Secrets Manager
   - Factory to select provider

**Acceptance Criteria:**
- Passwords encrypted in OSGi config
- Support for env var credentials
- No plaintext credentials in config dumps

#### Step 6: Conflict Resolution (2-3 weeks)

**Tasks:**

1. **Add Version Tracking**
   - Property: `sling:version` (long, auto-increment)
   - Update on every publish

2. **Export Version in JSON**
   - Include `expectedVersion` in payload

3. **Validate Version on Import**
   - Compare `expectedVersion` with current publisher version
   - If mismatch → reject, return conflict error

4. **Conflict UI**
   - Show conflict notification
   - Options: Force publish, Cancel, Merge (future)

**Acceptance Criteria:**
- Version numbers tracked
- Conflicts detected and rejected
- UI notification of conflicts

---

## Appendix

### A. Key File Locations

#### Java APIs
- `/api/src/main/java/org/apache/sling/cms/publication/`
  - PublicationManager.java
  - PublicationManagerFactory.java
- `/api/src/main/java/org/apache/sling/cms/PublishableResource.java`

#### Core Services
- `/distribution/src/main/java/org/apache/sling/cms/distribution/`
  - ContentDistributionService.java
  - impl/ContentDistributionServiceImpl.java
  - impl/PublicationManagerFactoryImpl.java
  - impl/StandalonePublicationManager.java
  - impl/HttpDistributionPublicationManager.java
  - impl/BulkPublicationJob.java
  - impl/ContentImportServlet.java
  - impl/PublishPostOperation.java
  - impl/UnpublishPostOperation.java

#### Sling Models
- `/core/src/main/java/org/apache/sling/cms/core/internal/models/PublishableResourceImpl.java`

#### UI Components
- `/ui/src/main/resources/jcr_root/libs/sling-cms/components/publication/`
  - home/home.html
  - status/status.jsp ⚠️
  - agent/agent.jsp ⚠️
  - exporter/exporter.jsp ⚠️
  - importer/importer.jsp ⚠️

#### Content Pages
- `/ui/src/main/resources/jcr_root/libs/sling-cms/content/publication/`
  - home.json
  - agents.json
  - bulk.json
  - exporters.json
  - importers.json

### B. Configuration Reference

#### OSGi Configurations

**Publication Config**
```
PID: org.apache.sling.cms.distribution.impl.PublicationConfig

Properties:
- instance.type: STANDALONE | AUTHOR | RENDERER
- publication.mode: STANDALONE | CONTENT_DISTRIBUTION
- distribution.agents: String[] (legacy, may not be used)
```

**Content Distribution Config**
```
PID: org.apache.sling.cms.distribution.impl.ContentDistributionConfig

Properties:
- enabled: boolean
- publisher.endpoints: String[] (URLs)
- username: String
- password: String (obfuscated)
- connection.timeout: int (ms)
- socket.timeout: int (ms)
- allowed.content.roots: String[] (default: /content)
- use.content.package: boolean (future)
- async.distribution: boolean (future)
```

#### Service User Mapping

**Required for ContentImportServlet:**

```
# Service user mapping
org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended-distribution.cfg.json
{
  "user.mapping": [
    "org.apache.sling.cms.distribution:distribution=sling-cms-distribution"
  ]
}

# Create service user
curl -u admin:admin -F"createUser=sling-cms-distribution" \
  -F"authorizableId=sling-cms-distribution" \
  http://localhost:8083/libs/sling/servlet/post/system/userManager/user.create.html

# Grant permissions (example)
# sling-cms-distribution user needs:
# - jcr:write on /content
# - jcr:read on /
```

### C. REST API Examples

#### Publish a Page

**Request:**
```http
POST /content/mysite/page HTTP/1.1
Host: localhost:8082
Authorization: Basic YWRtaW46YWRtaW4=
Content-Type: application/x-www-form-urlencoded

:operation=publish&deep=true
```

**Response:**
```json
{
  "success": true,
  "message": "Published successfully",
  "path": "/content/mysite/page"
}
```

#### Unpublish a Page

**Request:**
```http
POST /content/mysite/page HTTP/1.1
Host: localhost:8082
Authorization: Basic YWRtaW46YWRtaW4=
Content-Type: application/x-www-form-urlencoded

:operation=unpublish
```

#### Bulk Publication Job

**Create Job:**
```http
POST /cms/publication/bulk HTTP/1.1
Host: localhost:8082
Content-Type: application/x-www-form-urlencoded

paths=/content/mysite/page1&paths=/content/mysite/page2&type=ADD&deep=true
```

**Job Properties:**
```json
{
  "topic": "cmsjob/org/apache/sling/cms/publication/Bulk",
  "properties": {
    "paths": ["/content/mysite/page1", "/content/mysite/page2"],
    "type": "ADD",
    "deep": true
  }
}
```

### D. Event Topics

**Publication Events** (OSGi EventAdmin)

**Publish Event:**
```
Topic: org/apache/sling/cms/publication/PUBLISH

Properties:
- path: String (resource path)
- publicationType: String (ADD)
- deep: Boolean
- user: String (who published)
- timestamp: Long
```

**Unpublish Event:**
```
Topic: org/apache/sling/cms/publication/UNPUBLISH

Properties:
- path: String (resource path)
- publicationType: String (DELETE)
- user: String (who unpublished)
- timestamp: Long
```

**Example Listener:**
```java
@Component(service = EventHandler.class, immediate = true)
@Property(name = EventConstants.EVENT_TOPIC, value = {
    "org/apache/sling/cms/publication/PUBLISH",
    "org/apache/sling/cms/publication/UNPUBLISH"
})
public class PublicationListener implements EventHandler {

    @Override
    public void handleEvent(Event event) {
        String path = (String) event.getProperty("path");
        String type = (String) event.getProperty("publicationType");

        // Custom logic (e.g., invalidate cache, send notification)
        logger.info("Publication event: {} - {}", path, type);
    }
}
```

### E. Glossary

| Term | Definition |
|------|------------|
| **Author Instance** | Sling instance where content is created and edited |
| **Publisher Instance** | Sling instance that serves published content to end users (also called "Renderer") |
| **Publication** | Process of making content available on publisher instance(s) |
| **Distribution** | Technical mechanism for transferring content from author to publisher |
| **Deep Publish** | Publishing a resource and all its descendants |
| **Shallow Publish** | Publishing only a single resource (no descendants) |
| **Bulk Publication** | Publishing multiple resources in a single operation |
| **Distribution Agent** | Configuration that defines how content is distributed (Sling Distribution concept) |
| **Export** | Serializing JCR content for distribution |
| **Import** | Deserializing and storing distributed content in JCR |
| **Publication Metadata** | JCR properties tracking publication status (sling:published, sling:lastPublication, etc.) |

### F. Related Documentation

- [Apache Sling Distribution](https://sling.apache.org/documentation/bundles/content-distribution.html)
- [Sling Jobs](https://sling.apache.org/documentation/bundles/apache-sling-eventing-and-job-handling.html)
- [JSP to HTL Migration Guide](jsp-to-htl-migration.md)
- [OSGi R7 Annotations](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html)

---

**End of Document**
