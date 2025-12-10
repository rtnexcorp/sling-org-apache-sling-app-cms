#!/bin/bash
#
# Sling CMS Hot Deploy Watcher
# Watches all modules and deploys only the changed module
#

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

# Read port from pom.xml or use default
SLING_PORT=$(grep -m1 '<sling.port>' pom.xml | sed 's/.*<sling.port>\([^<]*\)<.*/\1/' || echo "8082")

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔥 Sling CMS Hot Deploy Watcher"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Target: http://localhost:${SLING_PORT}"
echo "📁 Watching: api, core, login, ui, reference, thumbnails"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Press Ctrl+C to stop"
echo ""

# Deploy function
deploy_module() {
    local module=$1
    local file=$2
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📦 Change detected in: $module"
    echo "📄 File: $file"
    echo "� Applying code format..."
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    cd "$PROJECT_DIR"
    
    # Apply spotless formatting first
    mvn spotless:apply -pl "$module" -q 2>/dev/null
    
    echo "🚀 Deploying $module..."
    mvn install -P autoInstallBundle -pl "$module" -DskipTests -Dbnd.baseline.skip=true -q
    
    if [ $? -eq 0 ]; then
        echo "✅ $module deployed successfully!"
    else
        echo "❌ $module deployment failed!"
    fi
    echo ""
}

# Check for fswatch
if ! command -v fswatch &> /dev/null; then
    echo "⚠️  fswatch not installed. Installing via Homebrew..."
    brew install fswatch
fi

# Watch all module src directories
fswatch -0 -r -l 1 \
    --exclude='.*target.*' \
    --exclude='.*node_modules.*' \
    --exclude='.*\.git.*' \
    --exclude='.*\.class$' \
    --include='.*\.java$' \
    --include='.*\.xml$' \
    --include='.*\.json$' \
    --include='.*\.html$' \
    --include='.*\.js$' \
    --include='.*\.css$' \
    api/src core/src login/src ui/src reference/src thumbnails/src 2>/dev/null | while read -d "" event; do
    
    # Extract module name from path
    module=$(echo "$event" | sed -n 's|.*/\([^/]*\)/src/.*|\1|p')
    
    if [ -n "$module" ]; then
        # Get relative file path
        file=$(echo "$event" | sed "s|$PROJECT_DIR/||")
        deploy_module "$module" "$file"
    fi
done
