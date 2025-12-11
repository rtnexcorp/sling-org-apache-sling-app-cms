#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Start Sling CMS Publisher (Renderer) Instance on port 8083

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
RENDERER_JAR="$PROJECT_DIR/feature/target/org.apache.sling.cms-1.1.9-SNAPSHOT-renderer.jar"
UNIFIED_JAR="$PROJECT_DIR/feature/target/org.apache.sling.cms-1.1.9-SNAPSHOT.jar"
PUBLISHER_DIR="$SCRIPT_DIR/publisher"
PORT=8083
USE_UNIFIED=false

# Check if renderer JAR exists, fallback to unified JAR
if [ -f "$RENDERER_JAR" ]; then
    JAR_TO_USE="$RENDERER_JAR"
    JAR_TYPE="Renderer JAR"
elif [ -f "$UNIFIED_JAR" ]; then
    JAR_TO_USE="$UNIFIED_JAR"
    JAR_TYPE="Unified JAR (with -P renderer)"
    USE_UNIFIED=true
else
    echo "Error: No suitable JAR found!"
    echo "Looked for:"
    echo "  1. Renderer JAR: $RENDERER_JAR"
    echo "  2. Unified JAR: $UNIFIED_JAR"
    echo ""
    echo "Please build the project first:"
    echo "  mvn clean install -DskipTests                    # For separate JARs"
    echo "  mvn clean install -P unified -DskipTests         # For unified JAR"
    exit 1
fi

# Create publisher directory if it doesn't exist
mkdir -p "$PUBLISHER_DIR"

echo "============================================"
echo "Starting Sling CMS Publisher (Renderer) Instance"
echo "============================================"
echo "Mode: RENDERER"
echo "Port: $PORT"
echo "Admin URL: http://localhost:$PORT/cms"
echo "Credentials: admin / admin"
echo "JAR: $JAR_TYPE"
echo "============================================"
echo ""
echo "Press Ctrl+C to stop"
echo ""

cd "$PUBLISHER_DIR"

# Start the renderer instance
if [ "$USE_UNIFIED" = true ]; then
    java -Xmx1g \
        -Dorg.osgi.service.http.port=$PORT \
        -jar "$JAR_TO_USE" \
        -P renderer \
        -p "$PUBLISHER_DIR/launcher"
else
    java -Xmx1g \
        -Dsling.run.modes=renderer \
        -Dorg.osgi.service.http.port=$PORT \
        -jar "$JAR_TO_USE"
fi
