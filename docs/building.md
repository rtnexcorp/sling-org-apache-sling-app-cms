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
[Apache Sling](https://sling.apache.org) > [Sling CMS](https://github.com/apache/sling-org-apache-sling-app-cms) > [Developers](developers.md) > Building Sling CMS

# Building Sling CMS

## Prerequisites

- **Java 21** or higher
- **Apache Maven 3.6+**
- **Git**
- **Node.js 14+** (for frontend development)

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/apache/sling-org-apache-sling-app-cms.git
cd sling-org-apache-sling-app-cms
```

### 2. Build the Project

```bash
# Full build (includes tests)
mvn clean install

# Skip tests (faster)
mvn clean install -DskipTests
```

The build artifacts will be located at:
- **Standalone JAR:** `feature/target/org.apache.sling.cms-{VERSION}.jar`
- **Author JAR:** `feature/target/org.apache.sling.cms-{VERSION}-author.jar`
- **Renderer JAR:** `feature/target/org.apache.sling.cms-{VERSION}-renderer.jar`

## Running Sling CMS

### Standalone Instance (Development)

For local development with a single instance:

```bash
./deployment/start-standalone.sh
```

Access at: [http://localhost:8080/cms](http://localhost:8080/cms)

### Author Instance (Content Authoring)

For author/publisher setup:

```bash
./deployment/start-author.sh
```

Access at: [http://localhost:8082/cms](http://localhost:8082/cms)

### Renderer Instance (Publisher)

```bash
# Start on port 8083 (requires author instance for content distribution)
java -jar feature/target/org.apache.sling.cms-{VERSION}-renderer.jar
```

Access at: [http://localhost:8083](http://localhost:8083)

## Login Credentials

**Default credentials:** `admin` / `admin`

## Development Workflow

### Hot Deploy Single Module

For faster development iteration, deploy individual modules:

```bash
# Deploy core module
mvn clean install -P autoInstallBundle -pl core -DskipTests -Dbnd.baseline.skip=true

# Deploy UI module
mvn clean install -P autoInstallBundle -pl ui -DskipTests

# Deploy multiple modules
mvn clean install -P autoInstallBundle -pl api,core,ui -DskipTests
```

### Code Formatting

**REQUIRED before committing:**

```bash
# Apply code formatting to all modules
mvn spotless:apply

# Check formatting without changes
mvn spotless:check
```

### Frontend Development

If modifying JavaScript or SCSS:

```bash
cd frontend/src/main/frontend
npm install
npm run build

# Deploy frontend module
cd ../../..
mvn clean install -P autoInstallBundle -pl frontend -DskipTests
```

## Available Build Profiles

| Profile | Port | Description |
|---------|------|-------------|
| `autoInstallBundle` | 8082 | Default/author instance |
| `standalone` | 8080 | Standalone instance |
| `renderer` | 8083 | Publisher instance |

## Project Structure

- **`api/`** - Public API interfaces
- **`core/`** - Core business logic, Sling Models, services
- **`ui/`** - HTL templates, components, content
- **`frontend/`** - JavaScript/SCSS assets
- **`feature/`** - Sling Feature Model configurations
- **`distribution/`** - Content distribution & workflow
- **`thumbnails/`** - Digital asset management
- **`reference/`** - Reference implementation
- **`login/`** - Authentication module

## Recent Improvements

### Publication System (2025-12-31)

- ✅ **JSP to HTL Migration** - All publication components converted to HTL
- ✅ **Sling Distribution Agent** - Native Sling Content Distribution configured
- ✅ **Binary References** - Efficient binary handling without base64 encoding
- ✅ **Queue Management** - Priority queues with retry mechanism

See:
- [Distribution Agent Configuration](distribution-agent-configuration.md)
- [Sling Distribution Migration Plan](sling-distribution-migration-plan.md)
- [JSP to HTL Migration](jsp-to-htl-publication-migration.md)

## Troubleshooting

### Build Fails with "Spotless Check Failed"

Run `mvn spotless:apply` to auto-format code.

### Port Already in Use

Change the port in startup scripts or kill the process:

```bash
# macOS/Linux
lsof -ti:8080 | xargs kill -9

# Or use different port
java -Dorg.osgi.service.http.port=9090 -jar feature/target/org.apache.sling.cms-*.jar
```

### Module Not Updated After Deploy

Clear Felix bundle cache:

```bash
# Stop instance, then:
rm -rf launcher/felix-cache
./deployment/start-author.sh
```

## Additional Resources

- [Developer Documentation](developers.md)
- [Administration Guide](administration.md)
- [Publication System Analysis](publication-system-analysis.md)
