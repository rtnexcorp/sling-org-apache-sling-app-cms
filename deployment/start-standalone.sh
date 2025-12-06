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
FEATURE_FAR="$PROJECT_DIR/feature/target/org.apache.sling.cms.feature-1.1.9-SNAPSHOT-slingcms_standalone_far.far"
FEATURE_LAUNCHER="$HOME/.m2/repository/org/apache/sling/org.apache.sling.feature.launcher/1.3.2/org.apache.sling.feature.launcher-1.3.2.jar"
STANDALONE_DIR="$SCRIPT_DIR/standalone"
PORT=8080

# Check if feature archive exists
if [ ! -f "$FEATURE_FAR" ]; then
    echo "Error: Standalone Feature Archive not found at $FEATURE_FAR"
    echo "Please build the project first: mvn clean install -DskipTests"
    exit 1
fi

# Check if feature launcher exists
if [ ! -f "$FEATURE_LAUNCHER" ]; then
    echo "Error: Feature Launcher not found at $FEATURE_LAUNCHER"
    echo "Downloading feature launcher..."
    mvn dependency:get -Dartifact=org.apache.sling:org.apache.sling.feature.launcher:1.3.2
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
echo "Feature Archive: $FEATURE_FAR"
echo "============================================"
echo ""
echo "Press Ctrl+C to stop"
echo ""

# Start standalone instance using feature launcher
java -Xmx512m \
    -jar "$FEATURE_LAUNCHER" \
    -f "$FEATURE_FAR" \
    -p "$STANDALONE_DIR/launcher" \
    -D org.osgi.service.http.port=$PORT \
    -D sling.run.modes=standalone
