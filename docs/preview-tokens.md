# SPDX-License-Identifier: Apache-2.0

# Preview Tokens and Draft Content Access

## Overview

The Preview Token system allows authors to generate time-limited, shareable URLs for accessing unpublished (draft) content without requiring authentication. This enables stakeholders, reviewers, and external collaborators to preview content before it's published.

## Features

- **Secure Token Generation**: UUID-based tokens stored in JCR
- **Time-Limited Access**: Configurable expiration (default: 24 hours)
- **No Authentication Required**: Preview links work without login
- **Automatic Security Bypass**: Valid tokens bypass publication checks
- **Easy Sharing**: One-click URL generation and copying

## How It Works

### Architecture

1. **Token Generation**: Author generates a preview token via UI or API
2. **Token Storage**: Token stored in `jcr:content` node with expiration
3. **URL Sharing**: Preview URL contains `?preview=<token>` parameter
4. **Token Validation**: `PreviewTokenFilter` validates token on request
5. **Security Bypass**: `CMSSecurityFilter` allows access for valid tokens

### Components

| Component | Location | Purpose |
|-----------|----------|---------|
| **PreviewTokenManager** | `api/` | Service interface for token operations |
| **PreviewTokenManagerImpl** | `core/` | Token generation and validation |
| **PreviewTokenFilter** | `core/` | Request filter for token detection |
| **CMSSecurityFilter** | `core/` | Modified to allow preview access |
| **GeneratePreviewTokenServlet** | `core/` | JSON API for token generation |
| **Preview Action** | `ui/` | UI component for generating previews |

## Usage

### For Authors (UI)

1. Navigate to a page or file in the CMS
2. Click the **Preview** button in the action bar
3. A modal displays the generated preview URL
4. Click **Copy** to copy the URL to clipboard
5. Click **Open Preview** to test the preview in a new tab
6. Share the URL with reviewers

### For Developers (API)

#### Generate Preview Token

**Java API:**
```java
@Reference
private PreviewTokenManager previewTokenManager;

// Generate token with 24-hour expiration
PreviewToken token = previewTokenManager.generateToken(resource, 86400);
String previewUrl = previewTokenManager.generatePreviewUrl(resource, 86400);
```

**HTTP API:**
```bash
curl -X POST http://localhost:8082/content/mysite/page.generatePreview.json \
     -d "timeout=86400" \
     -u admin:admin
```

**Response:**
```json
{
  "success": true,
  "previewUrl": "http://mysite.com/page.html?preview=a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

#### Validate Preview Token

```java
boolean isValid = previewTokenManager.validateToken(resource, tokenString);
```

#### Revoke Preview Token

```java
previewTokenManager.revokeToken(resource);
```

## Configuration

### Token Expiration

Default expiration is **24 hours (86400 seconds)**. To change:

**UI**: Pass `timeout` parameter to servlet
**API**: Use `timeoutSeconds` parameter

```java
// 1 hour expiration
PreviewToken token = previewTokenManager.generateToken(resource, 3600);

// No expiration (permanent until revoked)
PreviewToken token = previewTokenManager.generateToken(resource, 0);
```

### Token Storage

Preview tokens are stored in the resource's `jcr:content` node:

```
/content/mysite/page
  jcr:primaryType: sling:Page
  jcr:content
    sling:previewToken: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    sling:previewExpiry: 2025-12-25T14:30:00.000Z
    sling:previewCreatedBy: "admin"
```

## Security Considerations

### What Preview Tokens Allow

✅ **Allowed**:
- Access to unpublished pages and files
- View draft content changes
- Navigate through unpublished site structure

❌ **Not Allowed**:
- Editing content
- Creating/deleting resources
- Administrative operations
- Access to CMS UI

### Token Security

- **UUID v4 Format**: Cryptographically random, 128-bit identifier
- **Single Resource**: Each token is tied to a specific resource
- **Time-Limited**: Tokens expire after configured duration
- **Revocable**: Tokens can be revoked at any time
- **No Cascading**: Child resources require separate tokens

### Best Practices

1. **Use Short Expiration**: Default 24 hours is recommended
2. **Revoke After Use**: Revoke tokens after review is complete
3. **Audit Token Creation**: Monitor `sling:previewCreatedBy` property
4. **HTTPS Only**: Use HTTPS for production preview URLs
5. **Limit Scope**: Generate tokens for specific pages, not entire sites

## Request Flow

```
1. User requests: /content/mysite/page.html?preview=<token>
                  ↓
2. PreviewTokenFilter detects preview parameter
                  ↓
3. PreviewTokenManager validates token against resource
                  ↓
4. If valid: Set request attribute ATTR_PREVIEW_ENABLED=true
                  ↓
5. CMSSecurityFilter checks ATTR_PREVIEW_ENABLED
                  ↓
6. If true: Allow access to unpublished content
                  ↓
7. Content rendered normally
```

## Integration with Existing Features

### Publication Workflow

Preview tokens work independently of publication state:
- **Unpublished Content**: Accessible with valid token
- **Published Content**: Accessible with or without token
- **Mixed**: Site can have both public and preview-only pages

### Version Management

Preview tokens show the **current draft version**, not historical versions. For version preview, use the version restoration feature.

### Multi-Site

Each site can have independent preview tokens. Tokens are resource-specific, not site-wide.

## Troubleshooting

### Token Not Working

**Symptoms**: 401 Unauthorized when accessing preview URL

**Causes**:
1. Token expired
2. Token revoked
3. Wrong resource path
4. Token not stored in jcr:content

**Solutions**:
```bash
# Check token in JCR
curl http://localhost:8082/content/mysite/page.json -u admin:admin

# Regenerate token
curl -X POST http://localhost:8082/content/mysite/page.generatePreview.json \
     -u admin:admin
```

### Token Not Persisting

**Symptoms**: Token generation succeeds but doesn't persist

**Causes**:
1. No jcr:content node
2. Insufficient permissions
3. ResourceResolver not committed

**Solutions**:
- Ensure resource is a sling:Page or sling:File
- Check user permissions on jcr:content node
- Verify ResourceResolver.commit() is called

### Preview Mode Not Enabling

**Symptoms**: Token validates but content still shows 401

**Causes**:
1. PreviewTokenFilter not registered
2. CMSSecurityFilter order incorrect
3. Request attribute not set

**Solutions**:
```bash
# Check OSGi filter registration
http://localhost:8082/system/console/components

# Verify PreviewTokenFilter service.ranking=10000
# Verify CMSSecurityFilter checks ATTR_PREVIEW_ENABLED
```

## API Reference

### PreviewTokenManager

```java
public interface PreviewTokenManager {

    PreviewToken generateToken(Resource resource, int timeoutSeconds)
        throws PersistenceException;

    PreviewToken getToken(Resource resource);

    boolean validateToken(Resource resource, String token);

    void revokeToken(Resource resource)
        throws PersistenceException;

    String generatePreviewUrl(Resource resource, int timeoutSeconds)
        throws PersistenceException;
}
```

### PreviewToken

```java
public interface PreviewToken {
    String getToken();
    Calendar getExpiry();
    String getCreatedBy();
    boolean isValid();
    boolean isExpired();
}
```

### Constants

```java
// Request attribute for preview mode
CMSConstants.ATTR_PREVIEW_ENABLED = "cmsPreviewEnabled"

// Query parameter name
CMSConstants.PARAM_PREVIEW_TOKEN = "preview"

// JCR properties
CMSConstants.PN_PREVIEW_TOKEN = "sling:previewToken"
CMSConstants.PN_PREVIEW_EXPIRY = "sling:previewExpiry"
CMSConstants.PN_PREVIEW_CREATED_BY = "sling:previewCreatedBy"
```

## Future Enhancements

Potential improvements for future releases:

1. **Token Analytics**: Track preview link usage and views
2. **Email Integration**: Send preview URLs via email
3. **Bulk Generation**: Generate tokens for multiple pages at once
4. **QR Codes**: Generate QR codes for mobile preview
5. **Password Protection**: Add optional password to tokens
6. **Preview Expiry Notifications**: Alert when tokens are about to expire
7. **Token Templates**: Predefined expiration configurations

## Related Documentation

- [Page Editing](page-editing.md)
- [Publishing](publishing.md)
- [Security](security.md)
- [Version Management](versioning.md)
