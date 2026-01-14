# Personalization REST API - Quick Reference

## ✅ Fixed: Servlet Registration

The servlet is now registered at `/bin/personalization` using **Sling selectors** for different operations.

## 📍 Updated Endpoints

### 1. List All Segments
```bash
GET /bin/personalization.segments.json

# With filters
GET /bin/personalization.segments.json?type=device
GET /bin/personalization.segments.json?active=true
GET /bin/personalization.segments.json?type=cookie&active=true
```

**Response:**
```json
{
  "segments": [
    {
      "id": "mobile-users",
      "name": "Mobile Users",
      "description": "Users on mobile devices",
      "type": "device",
      "priority": 10,
      "active": true,
      "path": "/etc/personalization/segments/mobile-users",
      "rules": {
        "deviceType": "mobile"
      }
    }
  ],
  "total": 1
}
```

### 2. Get Specific Segment
```bash
GET /bin/personalization.segment.{segmentId}.json

# Example
GET /bin/personalization.segment.mobile-users.json
```

**Response:**
```json
{
  "id": "mobile-users",
  "name": "Mobile Users",
  "description": "Users on mobile devices",
  "type": "device",
  "priority": 10,
  "active": true,
  "path": "/etc/personalization/segments/mobile-users",
  "rules": {
    "deviceType": "mobile"
  }
}
```

### 3. Evaluate Personalization
```bash
GET /bin/personalization.evaluate.json?path=/content/mysite/homepage
```

**Response:**
```json
{
  "path": "/content/mysite/homepage",
  "matchedSegments": ["mobile-users", "returning-visitors"],
  "selectedVariant": {
    "id": "mobile-variant",
    "name": "Mobile Version",
    "contentPath": "/content/mysite/homepage/variants/mobile-variant",
    "isDefault": false,
    "priority": 10,
    "segments": ["mobile-users"]
  },
  "availableVariants": [
    {
      "id": "mobile-variant",
      "name": "Mobile Version",
      "contentPath": "/content/mysite/homepage/variants/mobile-variant",
      "isDefault": false,
      "priority": 10,
      "segments": ["mobile-users"]
    },
    {
      "id": "default-variant",
      "name": "Default Version",
      "contentPath": "/content/mysite/homepage/variants/default-variant",
      "isDefault": true,
      "priority": 0,
      "segments": []
    }
  ]
}
```

### 4. List Available Evaluator Types
```bash
GET /bin/personalization.evaluators.json
```

**Response:**
```json
{
  "evaluators": [
    {"type": "device"},
    {"type": "cookie"},
    {"type": "userGroup"},
    {"type": "path"}
  ],
  "total": 4
}
```

## 🔧 Testing Commands

```bash
# Test with curl (replace localhost:8082 with your server)
BASE_URL="http://localhost:8082"

# 1. List all segments
curl "$BASE_URL/bin/personalization.segments.json"

# 2. Get specific segment
curl "$BASE_URL/bin/personalization.segment.mobile-users.json"

# 3. Evaluate personalization
curl "$BASE_URL/bin/personalization.evaluate.json?path=/content/mysite/homepage"

# 4. List evaluators
curl "$BASE_URL/bin/personalization.evaluators.json"
```

## 🚨 Common Issues & Solutions

### Issue: 404 Not Found

**Cause:** Servlet not registered or bundle not active

**Solution:**
1. Check bundle status in Web Console:
   ```
   http://localhost:8082/system/console/bundles
   ```
   Search for "personalization" - status should be "Active"

2. Verify servlet registration:
   ```
   http://localhost:8082/system/console/servletresolver
   ```
   Check that `/bin/personalization` is registered

3. Restart the bundle:
   ```bash
   # Via Web Console or
   mvn clean install -P autoInstallBundle -pl personalization -DskipTests -Dbnd.baseline.skip=true
   ```

### Issue: 403 Forbidden

**Cause:** Authentication required

**Solution:**
```bash
# Add authentication
curl -u admin:admin "$BASE_URL/bin/personalization.segments.json"
```

### Issue: Empty Segments List

**Cause:** No segments created yet

**Solution:**
Create test segment via JCR:
```bash
curl -u admin:admin -X POST "$BASE_URL/etc/personalization/segments/test-segment" \
  -d "jcr:primaryType=sling:Folder" \
  -d "name=Test Segment" \
  -d "evaluatorType=device" \
  -d "active=true" \
  -d "priority=10"
```

## 📊 Endpoint Patterns

The servlet uses **Sling selectors** to determine operations:

| Pattern | Operation |
|---------|-----------|
| `/bin/personalization.json` | List all segments (default) |
| `/bin/personalization.segments.json` | List all segments (explicit) |
| `/bin/personalization.segment.{id}.json` | Get specific segment |
| `/bin/personalization.evaluate.json?path={path}` | Evaluate personalization |
| `/bin/personalization.evaluators.json` | List evaluator types |

## 🎯 Why `/bin/personalization` Instead of `/api/personalization`?

In Apache Sling, path-based servlet registration requires special configuration. Using `/bin/*` is a standard Sling pattern because:

1. ✅ `/bin` is pre-configured for servlet access
2. ✅ No need to create repository nodes
3. ✅ Standard Sling pattern for APIs
4. ✅ Automatically available after bundle deployment

**Alternative:** If you prefer `/api/personalization`, you would need to:
- Create `/api/personalization` node in repository
- Configure resource types
- Use resource-type based servlet registration

## 📚 Related Documentation

- [graphql-integration-plan.md](./graphql-integration-plan.md) - Full GraphQL guide
- [graphql-quick-start.md](./graphql-quick-start.md) - Quick GraphQL setup
- [graphql-integration-summary.md](./graphql-integration-summary.md) - Integration overview

## ✅ Deployment Verified

```
[INFO] Installing Bundle org.apache.sling.cms.personalization to http://localhost:8082
[INFO] Bundle installed successfully
[INFO] BUILD SUCCESS
```

**Status:** ✅ REST API is deployed and ready to use!

---

**Last Updated:** January 10, 2026  
**Bundle:** org.apache.sling.cms.personalization-1.1.9-SNAPSHOT  
**Servlet Path:** `/bin/personalization`
