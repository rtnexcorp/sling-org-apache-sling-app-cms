# Apache Sling CMS Docker Support

Docker configuration for running Apache Sling CMS in containers.

## Prerequisites

- [Docker](https://docs.docker.com/install/) (20.10+)
- [Docker Compose](https://docs.docker.com/compose/install/) (v2+)
- Built project artifacts in local Maven repository (`~/.m2`)

## Build Project First

Before running Docker, build the project:

```bash
cd /path/to/sling-org-apache-sling-app-cms
mvn clean install -DskipTests
```

---

## Option 1: Standalone Mode (Single Instance)

Best for development and testing.

### Build & Run

```bash
cd docker

# Build standalone image
docker build -t slingcms-standalone -f cms/Dockerfile --build-arg RUNMODE=standalone cms/

# Run standalone
docker run -d --name slingcms \
  -p 8080:8080 \
  -v ~/.m2:/root/.m2:ro \
  -v sling-data:/opt/slingcms/sling \
  slingcms-standalone
```

### Access

- **CMS**: http://localhost:8080
- **Login**: admin / admin

### Stop & Cleanup

```bash
docker stop slingcms && docker rm slingcms
docker volume rm sling-data  # Optional: remove data
```

---

## Option 2: Author + Renderer Mode (Production-like)

Best for testing content distribution and production scenarios.

### Build & Run with Docker Compose

```bash
cd docker

# Build all images
docker-compose build

# Start all services (author, renderer, webcache)
docker-compose up -d

# View logs
docker-compose logs -f
```

### Access

| Service | URL | Purpose |
|---------|-----|---------|
| Author | http://localhost:8080 | Content authoring |
| Renderer | http://localhost:8090 | Content rendering |
| Webcache | http://localhost:80 | Cached public site |

**Login**: admin / admin

### Host File Setup (for webcache)

Add to `/etc/hosts`:
```
127.0.0.1 cms.sling.apache.local
127.0.0.1 sling2.apache.local
```

Then access:
- **CMS UI**: http://cms.sling.apache.local
- **Public Site**: http://sling2.apache.local

### Stop & Cleanup

```bash
docker-compose down

# Remove volumes (deletes all data)
docker-compose down -v
```

---

## Build Options

| Argument | Default | Description |
|----------|---------|-------------|
| `RUNMODE` | standalone | `standalone`, `author`, or `renderer` |
| `CMS_VERSION` | 1.1.9-SNAPSHOT | CMS version to deploy |

### Example: Build Specific Version

```bash
docker build -t slingcms-author \
  --build-arg RUNMODE=author \
  --build-arg CMS_VERSION=1.1.8 \
  -f cms/Dockerfile cms/
```

---

## Volumes

| Volume | Purpose |
|--------|---------|
| `sling-author` | Author instance repository |
| `sling-renderer` | Renderer instance repository |
| `sling-data` | Standalone instance repository |

---

## Troubleshooting

### Check Logs
```bash
# Standalone
docker logs slingcms

# Docker Compose
docker-compose logs author
docker-compose logs renderer
```

### Rebuild Without Cache
```bash
docker-compose build --no-cache
```

### Health Check
```bash
curl http://localhost:8080/system/health
```
