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

---

## 🚀 Auto-Deploy (Hot Reload for Development)

The project is configured with a **file watcher** that automatically detects changes and deploys only the changed module to your running Sling instance.

### Quick Start - Watch Mode (Recommended)

Use the smart watcher script that monitors all modules and deploys only the changed one:

```bash
# Start the file watcher (requires fswatch on macOS)
./deployment/watch.sh
```

The watcher will:
- Monitor all module `src` directories (api, core, login, ui, reference, thumbnails)
- Detect file changes (.java, .xml, .json, .html, .js, .css)
- Auto-apply Spotless formatting
- Deploy only the changed module to http://localhost:8082

### Alternative: Maven Watcher

You can also use the Maven fizzed-watcher plugin:

```bash
# Watch all modules and deploy to default instance (port 8082)
mvn fizzed-watcher:run -P autoInstallBundle

# Watch all modules and deploy to author instance (port 8082)
mvn fizzed-watcher:run -P autoInstallBundle,author

# Watch all modules and deploy to renderer instance (port 8083)
mvn fizzed-watcher:run -P autoInstallBundle,renderer
```

### One-Time Deploy Commands

Deploy all modules once without watching:

```bash
# Deploy all modules to default instance (port 8082)
mvn clean install -P autoInstallBundle -pl api,core,login,ui,reference,thumbnails -DskipTests -Dbnd.baseline.skip=true

# Deploy all modules to author (port 8082)
mvn clean install -P autoInstallBundle,author -pl api,core,login,ui,reference,thumbnails -DskipTests -Dbnd.baseline.skip=true

# Deploy all modules to renderer (port 8083)  
mvn clean install -P autoInstallBundle,renderer -pl api,core,login,ui,reference,thumbnails -DskipTests -Dbnd.baseline.skip=true
```

Deploy a single module:

```bash
# Deploy only the core module
mvn clean install -P autoInstallBundle -pl core -DskipTests -Dbnd.baseline.skip=true

# Deploy only the UI module to author
mvn clean install -P autoInstallBundle,author -pl ui -DskipTests -Dbnd.baseline.skip=true
```

### Configuration Properties

Configure deployment in parent `pom.xml` or via command line:

| Property | Default | Description |
|----------|---------|-------------|
| `sling.host` | `localhost` | Sling server hostname |
| `sling.port` | `8082` | Sling server port |
| `sling.username` | `admin` | Admin username |
| `sling.password` | `admin` | Admin password |
| `bnd.baseline.skip` | `false` | Skip OSGi baseline check (set to `true` for dev) |

Override via command line:
```bash
mvn clean install -P autoInstallBundle -Dsling.port=8083 -pl core -DskipTests -Dbnd.baseline.skip=true
```

### Available Profiles

| Profile | Port | Description |
|---------|------|-------------|
| `autoInstallBundle` | 8082 | Deploy to default/author instance |
| `author` | 8082 | Deploy to author instance (same as default) |
| `renderer` | 8083 | Deploy to renderer instance |
| `standalone` | 8080 | Deploy to standalone instance |

Combine profiles:
```bash
mvn clean install -P autoInstallBundle,author -pl ui -DskipTests -Dbnd.baseline.skip=true
```

### VS Code Tasks

The project includes VS Code tasks for quick deployment. Press `Ctrl+Shift+B` (or `Cmd+Shift+B` on macOS) and select:

- **Auto Deploy - Core** - Deploy core module
- **Auto Deploy - API** - Deploy api module
- **Auto Deploy - UI** - Deploy ui module
- **Auto Deploy - Login** - Deploy login module
- **Auto Deploy - Reference** - Deploy reference module
- **Auto Deploy - Thumbnails** - Deploy thumbnails module
- **Deploy All Modules** - Deploy all modules at once

---

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
| `watch.sh` | Smart file watcher - auto-deploys changed modules |
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
