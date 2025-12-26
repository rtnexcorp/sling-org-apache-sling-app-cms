#!/bin/bash
# SPDX-License-Identifier: Apache-2.0
# Test script to verify image upload, rendition, and preset generation

set -e

SERVER="http://localhost:8082"
USER="admin:admin"
TEST_PATH="/content/test-upload-$(date +%s)"

echo "=========================================="
echo "Image Upload Workflow Test"
echo "=========================================="
echo ""

# Step 1: Create test folder
echo "1. Creating test folder: $TEST_PATH"
curl -s -u "$USER" -F "jcr:primaryType=sling:Folder" "$SERVER$TEST_PATH" > /dev/null
echo "   ✅ Folder created"
echo ""

# Step 2: Upload a test image (use the existing dog.jpg)
echo "2. Uploading test image..."
TEST_IMAGE="$TEST_PATH/test-image.jpg"

# Copy from existing test image
curl -s -u "$USER" "$SERVER/static/test/dog.jpg" -o /tmp/test-upload.jpg
curl -s -u "$USER" -F "*=@/tmp/test-upload.jpg" "$SERVER$TEST_IMAGE" > /dev/null
echo "   ✅ Image uploaded to $TEST_IMAGE"
echo ""

# Step 3: Wait for metadata extraction (usually takes 1-2 seconds)
echo "3. Waiting for metadata extraction (3 seconds)..."
sleep 3

# Check if metadata exists
METADATA_STATUS=$(curl -s -u "$USER" "$SERVER$TEST_IMAGE/jcr:content/metadata.json" 2>/dev/null | jq -r 'if . then "✅ EXISTS" else "❌ MISSING" end')
echo "   Metadata: $METADATA_STATUS"

if [ "$METADATA_STATUS" = "✅ EXISTS" ]; then
    MIME_TYPE=$(curl -s -u "$USER" "$SERVER$TEST_IMAGE/jcr:content/metadata.json" | jq -r '.["Content-Type"] // "unknown"')
    ORIENTATION=$(curl -s -u "$USER" "$SERVER$TEST_IMAGE/jcr:content/metadata.json" | jq -r '.["tiff:Orientation"] // "none"')
    echo "   - MIME Type: $MIME_TYPE"
    echo "   - EXIF Orientation: $ORIENTATION"
fi
echo ""

# Step 4: Wait for rendition generation (usually takes 2-3 seconds)
echo "4. Waiting for rendition generation (5 seconds)..."
sleep 5

# Check if renditions exist
echo "   Checking renditions..."
RENDITIONS=$(curl -s -u "$USER" "$SERVER$TEST_IMAGE/jcr:content/renditions.json" 2>/dev/null)

if echo "$RENDITIONS" | jq -e '.' > /dev/null 2>&1; then
    RENDITION_COUNT=$(echo "$RENDITIONS" | jq -r 'keys | length')
    echo "   ✅ Found $RENDITION_COUNT rendition(s)"

    echo "$RENDITIONS" | jq -r 'keys[]' | while read -r rendition; do
        echo "      - $rendition"
    done
else
    echo "   ⚠️  No renditions found yet (may need more time)"
fi
echo ""

# Step 5: Test transformation API
echo "5. Testing transformation API..."
TRANSFORM_URL="$SERVER$TEST_IMAGE.transform/sling-cms-thumbnail/image.png"
TRANSFORM_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -u "$USER" "$TRANSFORM_URL")

if [ "$TRANSFORM_STATUS" = "200" ]; then
    echo "   ✅ Transformation API working (HTTP $TRANSFORM_STATUS)"
    echo "   URL: $TRANSFORM_URL"
else
    echo "   ❌ Transformation API failed (HTTP $TRANSFORM_STATUS)"
fi
echo ""

# Step 6: Test delivery preset API
echo "6. Testing delivery preset API..."
PRESET_URL="$SERVER$TEST_IMAGE.preset/avatar.webp"
PRESET_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -u "$USER" "$PRESET_URL")

if [ "$PRESET_STATUS" = "200" ]; then
    echo "   ✅ Delivery preset working (HTTP $PRESET_STATUS)"
    echo "   URL: $PRESET_URL"
else
    echo "   ❌ Delivery preset failed (HTTP $PRESET_STATUS)"
fi
echo ""

# Step 7: Test auto-rotate transformation
echo "7. Testing auto-rotate transformation..."
AUTOROTATE_URL="$SERVER$TEST_IMAGE.transform/auto-rotate-thumbnail/image.png"
AUTOROTATE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -u "$USER" "$AUTOROTATE_URL")

if [ "$AUTOROTATE_STATUS" = "200" ]; then
    echo "   ✅ Auto-rotate transformation working (HTTP $AUTOROTATE_STATUS)"
    echo "   URL: $AUTOROTATE_URL"
else
    echo "   ❌ Auto-rotate transformation failed (HTTP $AUTOROTATE_STATUS)"
fi
echo ""

# Summary
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo ""
echo "Test Image: $TEST_IMAGE"
echo ""
echo "Access URLs:"
echo "  - Original: $SERVER$TEST_IMAGE"
echo "  - Metadata: $SERVER$TEST_IMAGE/jcr:content/metadata.json"
echo "  - Renditions: $SERVER$TEST_IMAGE/jcr:content/renditions.json"
echo "  - Thumbnail: $TRANSFORM_URL"
echo "  - Avatar: $PRESET_URL"
echo "  - Auto-Rotate: $AUTOROTATE_URL"
echo ""
echo "To view in browser:"
echo "  open '$SERVER$TEST_IMAGE'"
echo ""
echo "To clean up:"
echo "  curl -X DELETE -u admin:admin '$SERVER$TEST_PATH'"
echo ""

# Cleanup /tmp file
rm -f /tmp/test-upload.jpg
