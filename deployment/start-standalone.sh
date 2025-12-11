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

# Start Sling CMS Standalone Instance on port 8080

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
# Both standalone JAR and unified JAR work for standalone mode
STANDALONE_JAR="$PROJECT_DIR/feature/target/org.apache.sling.cms-1.1.9-SNAPSHOT.jar"
STANDALONE_DIR="$SCRIPT_DIR/standalone"
PORT=8080

# Check if JAR exists (unified JAR is the default for standalone)
if [ -f "$STANDALONE_JAR" ]; then
    JAR_TO_USE="$STANDALONE_JAR"
    JAR_TYPE="Standalone/Unified JAR"
else
    echo "Error: Sling CMS JAR not found at $STANDALONE_JAR"
    echo ""
    echo "Please build the project first:"
    echo "  mvn clean install -P fast -DskipTests            # For standalone only (fastest)"
    echo "  mvn clean install -P unified -DskipTests         # For unified JAR"
    echo "  mvn clean install -DskipTests                    # For all separate JARs"
    exit 1
fi

# Create standalone directory
mkdir -p "$STANDALONE_DIR"
cd "$STANDALONE_DIR"

echo "============================================"
echo "Starting Sling CMS Standalone Instance"
echo "============================================"
echo "Mode: STANDALONE"
echo "Port: $PORT"
echo "Admin URL: http://localhost:$PORT/cms"
echo "Credentials: admin / admin"
echo "JAR: $JAR_TYPE"
echo "============================================"
echo ""
echo "Press Ctrl+C to stop"
echo ""

# Start standalone instance using the assembled JAR
java -Xmx1g \
    -jar "$JAR_TO_USE" \
    -p "$STANDALONE_DIR/launcher" \
    -D org.osgi.service.http.port=$PORT \
    -D sling.run.modes=standalone
