# Content Distribution and Publishing

This document describes the content distribution system in Apache Sling CMS, which enables publishing content from an Author instance to one or more Renderer (Publisher) instances.

## Overview

Apache Sling CMS supports a distributed architecture with separate Author and Renderer instances:

- **Author Instance**: Where content editors create and manage content
- **Renderer Instance**: Where published content is served to end users

The content distribution system synchronizes content from the Author to Renderer instances using HTTP-based distribution.

## Architecture

```
┌─────────────────┐         HTTP POST          ┌─────────────────┐
│                 │  ───────────────────────►  │                 │
│  Author (8082)  │     /bin/cms/distribution  │ Renderer (8083) │
│                 │          /import           │                 │
└─────────────────┘                            └─────────────────┘
        │                                              │
        │                                              │
        ▼                                              ▼
   Content Editing                              Content Serving
   CMS UI Access                                Public Website
```

## Instance Types

### AUTHOR
- Primary instance for content authoring
- Full CMS UI access
- Content is created and edited here
- Publishes content to Renderer instances

### RENDERER
- Receives published content from Author
- Serves content to end users
- Read-only for content (receives via distribution)
- Can have multiple Renderer instances for load balancing

### STANDALONE
- Single instance mode (default)
- No content distribution
- Suitable for development or small deployments

## Configuration

### Run Mode Configuration

The instance type and publication mode are configured via OSGi configuration in the feature model.

#### Author Configuration (`feature/src/main/features/runmodes/author.json`)

```json
{
    "configurations": {
        "org.apache.sling.cms.distribution.impl.PublicationManagerFactoryImpl": {
            "instanceType": "AUTHOR",
            "publicationMode": "CONTENT_DISTRIBUTION",
            "agents": []
        },
        "org.apache.sling.cms.distribution.impl.ContentDistributionServiceImpl": {
            "enabled": true,
            "publisherEndpoints": ["http://localhost:8083"],
            "publisherUsername": "admin",
            "publisherPassword": "admin",
            "connectionTimeout:Integer": 30000,
            "socketTimeout:Integer": 60000,
            "allowedRoots": ["/content", "/conf", "/etc/taxonomy"],
            "useContentPackage": false,
            "asyncDistribution": false
        }
    }
}
```

#### Renderer Configuration (`feature/src/main/features/runmodes/renderer.json`)

```json
{
    "configurations": {
        "org.apache.sling.cms.distribution.impl.PublicationManagerFactoryImpl": {
            "instanceType": "RENDERER",
            "publicationMode": "CONTENT_DISTRIBUTION",
            "agents": []
        }
    }
}
```

#### Standalone Configuration (`feature/src/main/features/runmodes/standalone.json`)

```json
{
    "configurations": {
        "org.apache.sling.cms.distribution.impl.PublicationManagerFactoryImpl": {
            "instanceType": "STANDALONE",
            "publicationMode": "STANDALONE",
            "agents": []
        }
    }
}
```

### Configuration Properties

#### PublicationManagerFactoryImpl

| Property | Type | Description |
|----------|------|-------------|
| `instanceType` | String | Instance type: `AUTHOR`, `RENDERER`, or `STANDALONE` |
| `publicationMode` | String | Publication mode: `CONTENT_DISTRIBUTION` or `STANDALONE` |
| `agents` | String[] | Distribution agent names (optional) |

#### ContentDistributionServiceImpl (Author only)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | Boolean | `true` | Enable/disable distribution service |
| `publisherEndpoints` | String[] | `[]` | URLs of Renderer instances |
| `publisherUsername` | String | `admin` | Username for authentication |
| `publisherPassword` | String | `admin` | Password for authentication |
| `connectionTimeout` | Integer | `30000` | HTTP connection timeout (ms) |
| `socketTimeout` | Integer | `60000` | HTTP socket timeout (ms) |
| `allowedRoots` | String[] | `["/content", "/conf", "/etc/taxonomy"]` | Paths allowed for distribution |
| `useContentPackage` | Boolean | `false` | Use content packages (reserved) |
| `asyncDistribution` | Boolean | `false` | Enable async distribution |

## Deployment

### Building the JARs

Three executable JARs are generated during the build:

```bash
cd /path/to/sling-org-apache-sling-app-cms
mvn clean install -DskipTests
```

Output JARs in `feature/target/`:
- `org.apache.sling.cms-1.1.9-SNAPSHOT.jar` - Standalone
- `org.apache.sling.cms-1.1.9-SNAPSHOT-author.jar` - Author
- `org.apache.sling.cms-1.1.9-SNAPSHOT-renderer.jar` - Renderer

### Starting Instances

#### Using Deployment Scripts

Copy the JARs to the deployment folder:

```bash
cd deployment
cp ../feature/target/org.apache.sling.cms-1.1.9-SNAPSHOT-author.jar author.jar
cp ../feature/target/org.apache.sling.cms-1.1.9-SNAPSHOT-renderer.jar renderer.jar
```

Start the Renderer first (so it's available when Author starts):

```bash
# Start Renderer on port 8083
./start-publisher.sh

# Wait for Renderer to be ready, then start Author on port 8082
./start-author.sh
```

#### Manual Start

```bash
# Start Renderer
java -Xmx1g -Dorg.osgi.service.http.port=8083 -Dsling.run.modes=renderer -jar renderer.jar &

# Start Author  
java -Xmx1g -Dorg.osgi.service.http.port=8082 -Dsling.run.modes=author -jar author.jar &
```

### Verifying Configuration

Check the OSGi component status:

```bash
# Check Author configuration
curl -u admin:admin "http://localhost:8082/system/console/components/org.apache.sling.cms.distribution.impl.PublicationManagerFactoryImpl.json"

# Check Renderer configuration
curl -u admin:admin "http://localhost:8083/system/console/components/org.apache.sling.cms.distribution.impl.PublicationManagerFactoryImpl.json"
```

Expected output should show:
- Author: `instanceType = AUTHOR`, `publicationMode = CONTENT_DISTRIBUTION`
- Renderer: `instanceType = RENDERER`, `publicationMode = CONTENT_DISTRIBUTION`

## Publishing Content

### Using the CMS UI

1. Navigate to the Author instance: http://localhost:8082/cms/start.html
2. Go to **Sites** → Select your site
3. Navigate to the page you want to publish
4. Click the **Publish** button in the page actions

### Using cURL

```bash
# Publish a page
curl -u admin:admin -X POST \
  -d ":operation=publish" \
  "http://localhost:8082/content/apache/sling-apache-org/my-page.html"

# Unpublish a page
curl -u admin:admin -X POST \
  -d ":operation=unpublish" \
  "http://localhost:8082/content/apache/sling-apache-org/my-page.html"
```

### Programmatic Publishing

```java
import org.apache.sling.cms.publication.PublicationManager;
import org.apache.sling.cms.PublishableResource;

// Get PublicationManager via adaptation
PublicationManager pm = request.adaptTo(PublicationManager.class);

// Publish a resource
Resource resource = resolver.getResource("/content/mysite/mypage");
PublishableResource publishable = resource.adaptTo(PublishableResource.class);
pm.publish(publishable);

// Unpublish a resource
pm.unpublish(publishable);
```

## Distribution Endpoints

### Import Endpoint (Renderer)

The Renderer exposes an endpoint to receive content from the Author:

- **Path**: `/bin/cms/distribution/import`
- **Method**: POST
- **Authentication**: Basic Auth
- **Content-Type**: application/json

Request body:
```json
{
  "path": "/content/mysite/mypage",
  "action": "ADD",
  "deep": true,
  "content": "{...serialized content JSON...}"
}
```

Actions:
- `ADD` - Create or update content
- `DELETE` - Remove content

## Monitoring and Troubleshooting

### Log Files

Check the log files for distribution activity:

```bash
# Author logs
tail -f deployment/author/launcher/logs/error.log | grep -i distribution

# Renderer logs
tail -f deployment/publisher/launcher/logs/error.log | grep -i distribution
```

### Common Log Messages

**Successful publish (Author)**:
```
INFO Publishing via HTTP distribution: /content/mysite/mypage
INFO Distributing content with resolver: /content/mysite/mypage (deep=true)
```

**Successful import (Renderer)**:
```
INFO Processing distribution: action=ADD, path=/content/mysite/mypage, deep=true
```

### Troubleshooting

#### Content not appearing on Renderer

1. Check if the Author's ContentDistributionService is configured with correct endpoints:
   ```bash
   curl -u admin:admin "http://localhost:8082/system/console/configMgr/org.apache.sling.cms.distribution.impl.ContentDistributionServiceImpl.json"
   ```

2. Verify network connectivity from Author to Renderer:
   ```bash
   curl -u admin:admin "http://localhost:8083/system/console/bundles.json"
   ```

3. Check Renderer logs for import errors

#### Publication fails with 409 Conflict

This usually indicates the content path doesn't exist or there's a repository state conflict. Ensure the parent paths exist before publishing.

#### Authentication errors

Verify the `publisherUsername` and `publisherPassword` in the Author configuration match valid credentials on the Renderer instance.

## Security Considerations

### Production Deployment

For production deployments:

1. **Change default credentials**: Update `publisherUsername` and `publisherPassword`
2. **Use HTTPS**: Configure SSL/TLS for the distribution endpoints
3. **Network isolation**: Place Renderer instances behind a firewall, only accessible from Author
4. **Service user**: Create a dedicated service user for distribution with minimal permissions

### Service User Configuration

The distribution module uses a service user for repository access. Ensure proper service user mapping:

```json
{
    "org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended~distribution": {
        "user.mapping": [
            "org.apache.sling.cms.distribution:distribution=sling-distribution"
        ]
    }
}
```

## Multiple Renderer Instances

To distribute to multiple Renderers, add multiple endpoints:

```json
{
    "org.apache.sling.cms.distribution.impl.ContentDistributionServiceImpl": {
        "publisherEndpoints": [
            "http://renderer1:8083",
            "http://renderer2:8083",
            "http://renderer3:8083"
        ]
    }
}
```

Content will be distributed to all configured endpoints.

## API Reference

### PublicationManager Interface

```java
public interface PublicationManager {
    void publish(PublishableResource resource);
    void unpublish(PublishableResource resource);
}
```

### PublicationManagerFactory Interface

```java
public interface PublicationManagerFactory {
    INSTANCE_TYPE getInstanceType();
    PUBLICATION_MODE getPublicationMode();
    PublicationManager getPublicationManager();
}
```

### ContentDistributionService Interface

```java
public interface ContentDistributionService {
    DistributionResult publish(String path);
    DistributionResult publish(String path, boolean deep);
    DistributionResult unpublish(String path);
    DistributionResult distribute(ResourceResolver resolver, String path);
    DistributionResult distribute(ResourceResolver resolver, String path, boolean deep);
    DistributionResult delete(ResourceResolver resolver, String path);
    boolean isAvailable();
    String[] getPublisherEndpoints();
}
```

## See Also

- [Deployment Models](deployment-models.md)
- [Securing Apache Sling CMS](securing.md)
- [Administration Guide](administration.md)
