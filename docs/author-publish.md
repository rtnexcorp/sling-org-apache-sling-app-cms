# Author and Publish Workflow in Apache Sling CMS

This document explains how content authoring and publishing works in Apache Sling CMS.

## Overview

Apache Sling CMS supports different deployment models that determine how content is published:

| Mode | Description |
|------|-------------|
| **Standalone** | Single instance handles both authoring and rendering. Publishing sets `sling:published=true` |
| **Author** | Dedicated for content authoring. Publishes to renderer via Sling Content Distribution |
| **Renderer** | Dedicated for rendering published content. Receives content from author instance |

## Publication Status

Every page and file has a publication status stored in the `jcr:content` node:

| Property | Type | Description |
|----------|------|-------------|
| `sling:published` | Boolean | `true` if content is published |
| `sling:lastPublication` | Date | Timestamp of last publication action |
| `sling:lastPublicationType` | String | `ADD` (publish) or `DELETE` (unpublish) |

### Content Structure

```
/content/mysite/mypage
└── jcr:content
    ├── sling:published: true
    ├── sling:lastPublication: 2025-12-04T10:30:00
    ├── sling:lastPublicationType: "ADD"
    └── ... (other page properties)
```

## Publishing Content

### Via the UI

1. Navigate to **Sites** in the left navigation
2. Select your site and browse to the content
3. Click the **Publish** action button on a page or file
4. Confirm the publication

![Publish Action](img/publish-action.png)

### Publish Options

- **Publish** - Makes content visible on the public site
- **Unpublish** - Removes content from the public site
- **Bulk Publish** - Publish multiple items at once

### Via Code

```java
import org.apache.sling.cms.PublishableResource;
import org.apache.sling.cms.publication.PublicationManager;
import org.apache.sling.cms.publication.PublicationManagerFactory;

@Reference
private PublicationManagerFactory publicationManagerFactory;

public void publishContent(Resource resource) throws PublicationException {
    PublicationManager pubMgr = publicationManagerFactory.getPublicationManager();
    PublishableResource publishable = resource.adaptTo(PublishableResource.class);
    
    // Publish
    pubMgr.publish(publishable);
    
    // Unpublish
    pubMgr.unpublish(publishable);
}
```

## Checking Publication Status

### In JSP

```jsp
<%@include file="/libs/sling-cms/global.jsp"%>
<sling:adaptTo adaptable="${resource}" adaptTo="org.apache.sling.cms.PublishableResource" var="publishable" />

<c:choose>
    <c:when test="${publishable.published}">
        <span class="status published">Published</span>
    </c:when>
    <c:otherwise>
        <span class="status draft">Draft</span>
    </c:otherwise>
</c:choose>
```

### In Java

```java
PublishableResource publishable = resource.adaptTo(PublishableResource.class);
boolean isPublished = publishable.isPublished();
Calendar lastPubDate = publishable.getLastPublication();
```

## Querying Published Content

Only query published content on the public site:

### JCR-SQL2

```sql
SELECT * FROM [sling:Page] 
WHERE ISDESCENDANTNODE([/content/mysite]) 
  AND [jcr:content/sling:published] = true
ORDER BY [jcr:content/jcr:created] DESC
```

### In JSP

```jsp
<c:set var="query" value="SELECT * FROM [sling:Page] WHERE ISDESCENDANTNODE([${site.path}]) AND [jcr:content/sling:published]=true" />
<c:forEach var="page" items="${sling:findResources(resourceResolver, query, 'JCR-SQL2')}">
    <!-- Render page -->
</c:forEach>
```

## Security Filter

The CMS Security Filter controls access to unpublished content:

- **Author mode**: All content accessible to authenticated users
- **Renderer mode**: Only published content accessible to anonymous users

Configure in OSGi:
```
org.apache.sling.cms.core.internal.filters.CMSSecurityFilter
```

## Deployment Models

### Standalone Mode

```
┌─────────────────────────────┐
│      Sling CMS Instance     │
│  ┌───────────┬───────────┐  │
│  │  Author   │  Render   │  │
│  │  (Admin)  │  (Public) │  │
│  └───────────┴───────────┘  │
│         JCR Repository      │
└─────────────────────────────┘
```

- Single instance for both authoring and rendering
- Publishing sets `sling:published=true`
- Feature: `slingcms-standalone`

### Author-Renderer Mode

```
┌─────────────────┐     Content      ┌─────────────────┐
│  Author Instance│  Distribution    │ Renderer Instance│
│                 │ ───────────────> │                  │
│  - Edit content │                  │  - Serve content │
│  - Publish      │                  │  - Public access │
└─────────────────┘                  └─────────────────┘
```

- Separate instances for authoring and rendering
- Uses Sling Content Distribution for replication
- Features: `slingcms-author` and `slingcms-renderer`

## Publication Events

Listen for publication events:

```java
import org.apache.sling.cms.publication.PublicationEvent;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@Component(service = EventHandler.class, property = {
    "event.topics=" + PublicationEvent.TOPIC_PUBLISH,
    "event.topics=" + PublicationEvent.TOPIC_UNPUBLISH
})
public class PublicationEventHandler implements EventHandler {
    
    @Override
    public void handleEvent(Event event) {
        String path = (String) event.getProperty(PublicationEvent.PN_PATH);
        String type = event.getTopic().contains("publish") ? "published" : "unpublished";
        log.info("Content {} was {}", path, type);
    }
}
```

## Bulk Publication

For publishing multiple items:

1. Navigate to **Publication** > **Bulk** in the admin
2. Select the paths to publish
3. Choose action (Publish/Unpublish)
4. Submit the job

### Via API

```java
@Reference
private JobManager jobManager;

public void bulkPublish(List<String> paths) {
    Map<String, Object> props = new HashMap<>();
    props.put("paths", paths.toArray(new String[0]));
    props.put("action", "publish");
    
    jobManager.addJob("org/apache/sling/cms/publication/bulk", props);
}
```

## Best Practices

1. **Preview before publishing** - Use preview mode to verify content
2. **Version before publishing** - Create versions for rollback capability
3. **Bulk operations** - Use bulk publish for large content updates
4. **Schedule publishing** - Use workflows for scheduled publication
5. **Security** - Ensure security filter is configured for production

## Troubleshooting

### Content not appearing on public site

1. Verify `sling:published=true` is set
2. Check security filter configuration
3. Verify content is under published site path
4. Check dispatcher/cache if using web server

### Publication errors

1. Check permissions - user needs write access
2. Verify resource resolver is not closed
3. Check for locked content
4. Review error logs for details

## Related Documentation

- [Deployment Models](deployment-models.md)
- [Managing Content](managing-content.md)
- [Securing Sling CMS](securing.md)
- [User Generated Content](user-generated-content.md)
