#!/bin/bash

# Field Components Testing Script
# Tests the unified select component variants and boolean field

SERVER="http://localhost:8082"
ADMIN_USER="admin:admin"

echo "========================================="
echo "Field Components Testing"
echo "========================================="

# Test 1: Verify select component is deployed
echo ""
echo "Test 1: Verify select component deployment..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$SERVER/libs/sling-cms/components/editor/fields/select/.json")
if [ "$STATUS" = "200" ]; then
  echo "✅ Select component found (HTTP $STATUS)"
else
  echo "❌ Select component not found (HTTP $STATUS)"
fi

# Test 2: Verify boolean component is deployed
echo ""
echo "Test 2: Verify boolean component deployment..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$SERVER/libs/sling-cms/components/editor/fields/boolean/.json")
if [ "$STATUS" = "200" ]; then
  echo "✅ Boolean component found (HTTP $STATUS)"
else
  echo "❌ Boolean component not found (HTTP $STATUS)"
fi

# Test 3: Verify radio component is deployed
echo ""
echo "Test 3: Verify radio component deployment..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$SERVER/libs/sling-cms/components/editor/fields/radio/.json")
if [ "$STATUS" = "200" ]; then
  echo "✅ Radio component found (HTTP $STATUS)"
else
  echo "❌ Radio component not found (HTTP $STATUS)"
fi

# Test 4: Verify checkbox component is deployed
echo ""
echo "Test 4: Verify checkbox component deployment..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$SERVER/libs/sling-cms/components/editor/fields/checkbox/.json")
if [ "$STATUS" = "200" ]; then
  echo "✅ Checkbox component found (HTTP $STATUS)"
else
  echo "❌ Checkbox component not found (HTTP $STATUS)"
fi

# Test 5: Create a test schema with all field variants
echo ""
echo "Test 5: Creating test schema with field variants..."
SCHEMA_JSON=$(cat <<'EOF'
{
  "jcr:primaryType": "nt:unstructured",
  "fieldTypes": {
    "selectDropdown": {
      "fieldType": "SELECT",
      "label": "Select Dropdown",
      "variant": "dropdown",
      "options": ["red=Red", "green=Green", "blue=Blue"],
      "required": true
    },
    "selectRadio": {
      "fieldType": "SELECT",
      "label": "Select Radio",
      "variant": "radio",
      "options": ["yes=Yes", "no=No"],
      "required": true
    },
    "selectCheckbox": {
      "fieldType": "SELECT",
      "label": "Select Checkbox",
      "variant": "checkbox",
      "options": ["opt1=Option 1", "opt2=Option 2", "opt3=Option 3"]
    },
    "selectMulti": {
      "fieldType": "SELECT",
      "label": "Multi Select",
      "variant": "multi",
      "options": ["cat=Cat", "dog=Dog", "bird=Bird"]
    },
    "booleanField": {
      "fieldType": "BOOLEAN",
      "label": "Enable Feature",
      "required": false
    }
  }
}
EOF
)

curl -s -u "$ADMIN_USER" \
  -X POST \
  -H "Content-Type: application/json" \
  "$SERVER/conf/global/site/schemas/test-variants" \
  -d "$SCHEMA_JSON" > /dev/null 2>&1

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -u "$ADMIN_USER" "$SERVER/conf/global/site/schemas/test-variants.json")
if [ "$STATUS" = "200" ]; then
  echo "✅ Test schema created (HTTP $STATUS)"
else
  echo "❌ Failed to create test schema (HTTP $STATUS)"
fi

# Test 6: Check schema was properly created with field variants
echo ""
echo "Test 6: Verifying field variants in schema..."
curl -s -u "$ADMIN_USER" "$SERVER/conf/global/site/schemas/test-variants.json" | grep -q "variant"
if [ $? -eq 0 ]; then
  echo "✅ Field variants found in schema"
else
  echo "⚠️  Could not verify field variants in schema"
fi

# Summary
echo ""
echo "========================================="
echo "Testing Summary:"
echo ""
echo "Components deployed:"
echo "  • select       - Unified component with variants"
echo "  • boolean      - Boolean toggle field"
echo "  • radio        - Radio button wrapper"
echo "  • checkbox     - Checkbox wrapper"
echo ""
echo "Supported variants for SELECT fields:"
echo "  • dropdown  - Standard single-select dropdown"
echo "  • radio     - Radio button group"
echo "  • checkbox  - Checkbox group"
echo "  • multi     - Multi-select dropdown"
echo ""
echo "========================================="
