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
[Apache Sling](https://sling.apache.org) > [Sling CMS](https://github.com/apache/sling-org-apache-sling-app-cms) > [Administration](administration.md) > Deployment Models

# Deployment Models

Apache Sling CMS provides a number of deployment models for your solution needs. 

## Instance Types

The project creates a number of instance types for different deployment scenarios.

### Standalone

An instance that is not part of a cluster of instances. In a standalone instance, the instance handles both authoring and rendering web content. Publishing content in a standalone instance is done by updating the `sling:published` property.

**Feature:**
```
org.apache.sling:org.apache.sling.cms.feature:slingosgifeature:slingcms-standalone:[VERSION]
```

**JAR File:** `org.apache.sling.cms-[VERSION].jar`

**Installation:**
```bash
# Build the project
mvn clean install -DskipTests

# Create a directory and start
mkdir -p standalone && cd standalone
java -Xmx1g -Dorg.osgi.service.http.port=8080 \
    -jar ../feature/target/org.apache.sling.cms-1.1.9-SNAPSHOT.jar
```

**Access:**
- CMS Admin: http://localhost:8080/cms/start.html
- Credentials: admin / admin

---

### Author

An author instance is used to author the content of the site. It is not responsible for rendering content to end users. Content is published from the author instance to the renderer instance using HTTP-based Content Distribution.

**Feature:**
```
org.apache.sling:org.apache.sling.cms.feature:slingosgifeature:slingcms-author:[VERSION]
```

**JAR File:** `org.apache.sling.cms-[VERSION]-author.jar`

**Installation:**
```bash
# Build the project
mvn clean install -DskipTests

# Create a directory and start on port 8082
mkdir -p author && cd author
java -Xmx1g -Dorg.osgi.service.http.port=8082 \
    -Dsling.run.modes=author \
    -jar ../feature/target/org.apache.sling.cms-1.1.9-SNAPSHOT-author.jar
```

**Access:**
- CMS Admin: http://localhost:8082/cms/start.html
- Credentials: admin / admin

**Configuration:**
The author instance is pre-configured with:
- Instance Type: `AUTHOR`
- Publication Mode: `CONTENT_DISTRIBUTION`
- Publisher Endpoint: `http://localhost:8083` (Renderer)

---

### Renderer

A renderer instance is used to render and serve the published content to end users. Content should not be authored on the renderer; instead, it receives content published from the author instance via Content Distribution.

**Feature:**
```
org.apache.sling:org.apache.sling.cms.feature:slingosgifeature:slingcms-renderer:[VERSION]
```

**JAR File:** `org.apache.sling.cms-[VERSION]-renderer.jar`

**Installation:**
```bash
# Build the project
mvn clean install -DskipTests

# Create a directory and start on port 8083
mkdir -p renderer && cd renderer
java -Xmx1g -Dorg.osgi.service.http.port=8083 \
    -Dsling.run.modes=renderer \
    -jar ../feature/target/org.apache.sling.cms-1.1.9-SNAPSHOT-renderer.jar
```

**Access:**
- Published Content: http://localhost:8083/content/...
- System Console: http://localhost:8083/system/console
- Credentials: admin / admin

**Configuration:**
The renderer instance is pre-configured with:
- Instance Type: `RENDERER`
- Publication Mode: `CONTENT_DISTRIBUTION`

---

## Author-Renderer Setup

For a production-like setup with content authoring separated from content delivery:

### Quick Start

```bash
# 1. Build the project
cd /path/to/sling-org-apache-sling-app-cms
mvn clean install -DskipTests

# 2. Start Renderer FIRST (must be running before Author)
mkdir -p deployment/renderer && cd deployment/renderer
java -Xmx1g -Dorg.osgi.service.http.port=8083 \
    -Dsling.run.modes=renderer \
    -jar ../../feature/target/org.apache.sling.cms-1.1.9-SNAPSHOT-renderer.jar &

# Wait 60 seconds for Renderer to start

# 3. Start Author
cd .. && mkdir -p author && cd author
java -Xmx1g -Dorg.osgi.service.http.port=8082 \
    -Dsling.run.modes=author \
    -jar ../../feature/target/org.apache.sling.cms-1.1.9-SNAPSHOT-author.jar &
```

### Using Start Scripts

The [deployment](../deployment) directory contains ready-to-use start scripts:

```bash
cd deployment

# Start Renderer first
./start-renderer.sh

# Wait 60 seconds, then start Author
./start-author.sh
```

### Publishing Content

Once both instances are running:

1. **Via CMS UI:**
   - Login to Author at http://localhost:8082/cms/start.html
   - Navigate to Sites and select a page
   - Click "Publish" to distribute content to Renderer

2. **Via API:**
   ```bash
   curl -u admin:admin -X POST \
       -d ":operation=publish" \
       "http://localhost:8082/content/apache/sling-apache-org/index"
   ```

3. **Verify on Renderer:**
   ```bash
   curl "http://localhost:8083/content/apache/sling-apache-org/index.html"
   ```

For detailed information about content distribution, see [Content Distribution Guide](content-distribution.md).

---

## Sample Deployments

There are a number of samples to help you understand how to deploy Sling CMS:

### VM Installation

The [vagrant](../vagrant) directory contains a project to start Sling CMS in [standalone](#Standalone) mode in two CentOS 7 VMs one to run the CMS instance and one to run Apache Web Server.

### Docker Compose

The [docker](../docker) directory contains a project to start Sling CMS with an author-renderer pair with a separate container running Apache Web Server.

### Docker End to End Build

The [klcodanr/com.danklco.sample.infra](https://github.com/klcodanr/com.danklco.sample.infra) project contains a project to build a standalone composite node store Sling CMS instance.