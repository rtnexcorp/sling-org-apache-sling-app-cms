# Author/Renderer Quick Start Guide

This guide helps you quickly set up an Author-Renderer deployment of Apache Sling CMS.

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- At least 4GB RAM available

## Quick Setup

### 1. Build the Project

```bash
cd /path/to/sling-org-apache-sling-app-cms
mvn clean install -DskipTests
```

### 2. Set Up Deployment Directory

```bash
mkdir -p deployment
cd deployment

# Copy the JARs
cp ../feature/target/org.apache.sling.cms-1.1.9-SNAPSHOT-author.jar author.jar
cp ../feature/target/org.apache.sling.cms-1.1.9-SNAPSHOT-renderer.jar renderer.jar
```

### 3. Create Start Scripts

**start-author.sh**:
```bash
#!/bin/bash
mkdir -p author
cd author
java -Xmx1g -Dorg.osgi.service.http.port=8082 -jar ../author.jar &
echo "Author starting on port 8082..."
```

**start-publisher.sh** (for Renderer):
```bash
#!/bin/bash
mkdir -p publisher
cd publisher
java -Xmx1g -Dorg.osgi.service.http.port=8083 -jar ../renderer.jar &
echo "Renderer starting on port 8083..."
```

Make them executable:
```bash
chmod +x start-author.sh start-publisher.sh
```

### 4. Start the Instances

**Important**: Start the Renderer first, then the Author.

```bash
# Terminal 1: Start Renderer
./start-publisher.sh

# Wait 60 seconds for Renderer to be ready

# Terminal 2: Start Author
./start-author.sh
```

### 5. Verify Setup

Wait for both instances to start (about 60 seconds each), then verify:

```bash
# Check Renderer (should return bundle count)
curl -s -u admin:admin "http://localhost:8083/system/console/bundles.json" | grep -o '"state":"Active"' | wc -l

# Check Author (should return bundle count)
curl -s -u admin:admin "http://localhost:8082/system/console/bundles.json" | grep -o '"state":"Active"' | wc -l
```

Both should return approximately 276 active bundles.

## Access Points

| Instance | URL | Purpose |
|----------|-----|---------|
| Author CMS UI | http://localhost:8082/cms/start.html | Content management |
| Author Console | http://localhost:8082/system/console | OSGi administration |
| Renderer Console | http://localhost:8083/system/console | OSGi administration |
| Published Content | http://localhost:8083/content/... | Public website |

**Default credentials**: admin / admin

## Test Publishing

### Via cURL

```bash
# Publish a page
curl -u admin:admin -X POST \
  -d ":operation=publish" \
  "http://localhost:8082/content/apache/sling-apache-org/index.html"

# Verify on Renderer
curl -u admin:admin \
  "http://localhost:8083/content/apache/sling-apache-org/index.json"
```

### Via CMS UI

1. Open http://localhost:8082/cms/start.html
2. Login with admin/admin
3. Navigate to Sites → Apache → sling-apache-org
4. Select a page and click "Publish"

## Stop Instances

```bash
# Find and stop processes
pkill -f "author.jar"
pkill -f "renderer.jar"
```

## Clean Restart

To start fresh (removes all data):

```bash
cd deployment
rm -rf author publisher
./start-publisher.sh
# Wait 60 seconds
./start-author.sh
```

## Configuration Summary

### Author (Port 8082)
- Instance Type: `AUTHOR`
- Publication Mode: `CONTENT_DISTRIBUTION`
- Publishes to: `http://localhost:8083`

### Renderer (Port 8083)
- Instance Type: `RENDERER`
- Publication Mode: `CONTENT_DISTRIBUTION`
- Receives content from Author

## Troubleshooting

### Instances not starting
- Check if ports 8082/8083 are available
- Ensure sufficient memory (1GB per instance)
- Check Java version: `java -version`

### Publishing fails
- Verify Renderer is running before Author
- Check network connectivity between instances
- Review logs: `tail -f author/launcher/logs/error.log`

### Content not appearing on Renderer
- Ensure you clicked "Publish" (not just save)
- Check Renderer logs for import errors
- Verify the content path is in allowed roots

## Next Steps

- Read the full [Content Distribution Guide](content-distribution.md)
- Configure [custom sites](configure-site.md)
- Set up [production security](securing.md)
