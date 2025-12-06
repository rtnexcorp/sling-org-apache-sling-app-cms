# Apache Sling CMS Deployment Scripts

This folder contains scripts and configurations for running Apache Sling CMS in different deployment modes.

## Prerequisites

1. Build the project first:
   ```bash
   cd /path/to/sling-org-apache-sling-app-cms
   mvn clean install -DskipTests
   ```

2. The build creates the following Feature Archives (`.far` files):
   - `slingcms_standalone_far.far` - Single instance mode
   - `slingcms_author_far.far` - Author instance for content authoring
   - `slingcms_renderer_far.far` - Renderer/Publisher instance for content delivery

## Deployment Modes

### 1. Standalone Mode
A single instance that handles both authoring and rendering. Best for development and simple deployments.

```bash
./start-standalone.sh
```
- **Port**: 8080
- **Mode**: STANDALONE
- **URL**: http://localhost:8080/cms

### 2. Author-Publisher Mode (Distributed)
For production environments where content authoring is separated from content delivery.

**Start Author Instance:**
```bash
./start-author.sh
```
- **Port**: 8082
- **Mode**: AUTHOR
- **URL**: http://localhost:8082/cms
- Content distribution is configured to push to Publisher

**Start Publisher Instance:**
```bash
./start-publisher.sh
```
- **Port**: 8083
- **Mode**: RENDERER
- **URL**: http://localhost:8083/cms
- Receives content from Author instance

## Content Publishing Flow

In Author-Publisher mode:

1. Create/edit content on Author (http://localhost:8082/cms)
2. Click "Publish" on the content
3. Content is automatically distributed to Publisher
4. View published content on Publisher (http://localhost:8083)

## Manual Content Sync

For testing or troubleshooting, use the sync script:

```bash
./sync-content.sh /content/path/to/sync
```

## Scripts

| Script | Description |
|--------|-------------|
| `start-standalone.sh` | Start standalone instance on port 8080 |
| `start-author.sh` | Start author instance on port 8082 |
| `start-publisher.sh` | Start publisher/renderer instance on port 8083 |
| `sync-content.sh` | Manually sync content from Author to Publisher |

## Directory Structure

After running, each mode creates its own directory:
- `standalone/` - Standalone instance data
- `author/` - Author instance data  
- `publisher/` - Publisher instance data

## Configuration

### Author Configuration
Located in `feature/src/main/features/runmodes/author.json`:
- Instance Type: AUTHOR
- Publication Mode: CONTENT_DISTRIBUTION
- Publisher Endpoint: http://localhost:8083

### Renderer/Publisher Configuration
Located in `feature/src/main/features/runmodes/renderer.json`:
- Instance Type: RENDERER
- Publication Mode: CONTENT_DISTRIBUTION

### Standalone Configuration
Located in `feature/src/main/features/runmodes/standalone.json`:
- Instance Type: STANDALONE
- Publication Mode: STANDALONE

## Troubleshooting

### Feature Archive Not Found
Build the project first:
```bash
mvn clean install -DskipTests
```

### Port Already in Use
Check if another instance is running:
```bash
lsof -i :8080  # or 8082, 8083
```

### View Logs
Logs are in `{instance}/launcher/logs/error.log`

### Check Bundle Status
```bash
curl -u admin:admin http://localhost:8082/system/console/bundles.json
```

### Check Configuration
```bash
curl -u admin:admin http://localhost:8082/system/console/configMgr
```
