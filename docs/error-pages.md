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
[Apache Sling](https://sling.apache.org) > [Sling CMS](https://github.com/apache/sling-org-apache-sling-app-cms) > [Administration](administration.md) > Error Pages

# Error Pages

## Overview

Sling CMS provides a comprehensive, hierarchical error handling system that allows site administrators to customize error pages at both the site level and system level. The error handling mechanism ensures users receive appropriate feedback when errors occur, while preventing exposure of sensitive system information.

## Current Implementation Status

### ✅ Implemented Features

#### 1. Hierarchical Error Page Resolution
When a user encounters an error on a Sling CMS site, the `CmsDefaultErrorHandlerServlet` attempts to provide a relevant error page by performing the following steps in order:

 - Check for a `sling:Page` at */content/[SITE_GROUP]/[SITE_NAME]/errors/[STATUS_CODE]*
 - Check for a `sling:Page` at */content/[SITE_GROUP]/[SITE_NAME]/errors/default*
 - Check for a `sling:Page` at */static/sling-cms/errorhandling/[STATUS_CODE]*
 - Check for a `sling:Page` at */static/sling-cms/errorhandling/default*

This allows you to customize the response to an error while not providing too much information to your users.

#### 2. System-Level Default Error Pages
Sling CMS provides pre-configured error pages for common HTTP status codes:
- **401** - Unauthorized
- **403** - Forbidden  
- **404** - Not Found
- **default** - Catch-all for any unhandled error codes

**Location**: `/static/sling-cms/errorhandling/`

#### 3. Intelligent Error Code Detection
The servlet includes smart error code detection logic:
- **404 Not Found**: Validates whether the resource truly doesn't exist or if it's an authorization issue
  - Returns **401 Unauthorized** if user is anonymous and resource exists but is unpublished
  - Returns **403 Forbidden** if user is authenticated but lacks permissions
  - Returns **404** only if resource genuinely doesn't exist for any user
- Uses a service user (`sling-cms-error`) with read-all permissions to validate resource existence

#### 4. Secure Error Rendering
- **No exception stack traces** exposed to end users
- **Separate error page component** (`sling-cms/components/pages/error`) for rendering
- **HTML-only output** - Forces GET method and `.html` extension regardless of original request
- **Content-Type**: Automatically sets to `text/html`
- **Status code preservation**: Maintains original HTTP status code in response

#### 5. Service User Configuration
A dedicated service user `sling-cms-error` is configured with:
- **Permissions**: `sling-readall` (read-only access to all content)
- **Purpose**: Validate resource existence without exposing implementation details
- **Configuration**: Managed via Sling Feature Model in `feature/src/main/features/cms/cms.json`

#### 6. Site-Specific Error Pages
Site administrators can create custom error pages at the site level:

#### 6. Site-Specific Error Pages
Site administrators can create custom error pages at the site level:

**Example structure**:
```
/content
    /sites
        /mysite
            /errors
                /403        (Forbidden - custom branded page)
                /500        (Server error - custom branded page)
                /default    (All other errors)
```

**Behavior**:
- User encountering status code **500** → sees `/content/sites/mysite/errors/500`
- User encountering status code **403** → sees `/content/sites/mysite/errors/403`  
- User encountering any other error → sees `/content/sites/mysite/errors/default`
- If site-specific page not found → falls back to system error pages

**Best Practice**: Always specify at least a `default` error page for every site to prevent Sling CMS system error pages from being displayed.

### 🔧 Implementation Details

#### Servlet Configuration
- **Servlet Path**: `sling/servlet/errorhandler/default`
- **Service Ranking**: `1` (higher than Sling's default error handler)
- **Class**: `org.apache.sling.cms.core.internal.servlets.CmsDefaultErrorHandlerServlet`

#### Error Page Components
- **Component**: `sling-cms/components/pages/error`
- **Template**: `/libs/sling-cms/components/pages/error/error.jsp`
- **Rendering**: Uses standard Sling component inclusion mechanism

#### Testing
- **Test Class**: `CmsDefaultErrorHandlerServletTest`
- **Test Coverage**:
  - Default error page fallback
  - Specific error code handling (403, 404, 500)
  - Site-level error page priority
  - Fallback to system error pages
  - Service user permissions

## Future Enhancements

### 🚀 Planned Features

#### 1. Additional System Error Pages
**Status**: Proposed  
**Priority**: Medium

Add system-level error pages for additional common HTTP status codes:
- **400** - Bad Request
- **405** - Method Not Allowed
- **408** - Request Timeout
- **409** - Conflict
- **410** - Gone
- **429** - Too Many Requests
- **500** - Internal Server Error (currently uses `default`)
- **502** - Bad Gateway
- **503** - Service Unavailable
- **504** - Gateway Timeout

**Implementation**: Add JSON files to `/frontend/src/main/resources/jcr_root/static/sling-cms/errorhandling/`

#### 2. Error Page Templates
**Status**: Proposed  
**Priority**: Low

Provide reusable error page templates that sites can extend:
- Modern, responsive design
- Internationalization (i18n) support
- Search integration (suggest similar pages)
- Breadcrumb navigation back to home
- Contact information/support links

#### 3. Error Analytics Integration
**Status**: Proposed  
**Priority**: Low

Track error occurrences for monitoring and debugging:
- Log structured error data (timestamp, URL, status code, user agent)
- Integration with Content Insights
- Dashboard for error frequency analysis
- Alert administrators for critical errors (500, 503)

#### 4. Custom Error Messages via Configuration
**Status**: Proposed  
**Priority**: Low

Allow error message customization without creating full pages:
- OSGi configuration for simple message overrides
- Support for multiple languages
- Useful for microservices/API deployments

#### 5. Error Page Preview in Author UI
**Status**: Proposed  
**Priority**: Low

Add CMS authoring UI to:
- Preview error pages before publishing
- Test error page hierarchy
- Validate error page content
- Check for missing error pages per site

#### 6. Graceful Degradation
**Status**: Proposed  
**Priority**: Medium

Improve fallback behavior when error handling itself fails:
- Inline minimal HTML error page if component rendering fails
- Log errors during error handling without exposing to users
- Circuit breaker pattern for repeated error handler failures

### 🔒 Security Considerations

#### Current Security Measures
✅ **No stack trace exposure** - Exception details logged but not rendered  
✅ **Service user isolation** - Read-only permissions for error handler  
✅ **Authorization awareness** - Differentiates 401/403/404 based on actual permissions  
✅ **Content-Type enforcement** - Prevents MIME type confusion attacks

#### Future Security Enhancements
- **Rate limiting**: Prevent error page abuse for reconnaissance
- **Audit logging**: Track suspicious error patterns (repeated 403s, etc.)
- **CAPTCHA integration**: Optional challenge for repeated error requests
- **Error page caching**: Reduce load from malicious repeated requests

## Apache Web Server Integration

### Sling CMS Server Down Error Pages

### Sling CMS Server Down Error Pages

**Context**: When the Sling CMS application server becomes unavailable, users should not be left without feedback.

There are various reasons that a Sling CMS application server could become unavailable, and you do not want to leave users unsure what is going on. 

To alleviate this issue you should provide error pages for HTTP Status codes 502-504. These pages cannot be derived from Sling CMS as they will be requested only when Sling CMS is unavailable or not responding as expected. 

Instead, you can configure these pages to be ignored by mod_proxy and served directly from the Apache Web Server.

**Configuration Steps**:

1. **Exclude error page directory from proxying**:
    
    ```apache
    ProxyPass /ERROR !
    ```
    
    This instructs mod_proxy to ignore the directory `/ERROR` and serve it from Apache directly.
    
2. **Configure error document mappings**:

    ```apache
    # Configure Error Documents if Down
    ErrorDocument 502 /ERROR/502.html
    ErrorDocument 503 /ERROR/503.html
    ErrorDocument 504 /ERROR/504.html
    Alias /ERROR /var/www/vhosts/site
    ```
    
3. **Create static HTML error pages** under `/var/www/vhosts/site`

**Best Practices**:
- Error pages should be **static HTML** with no external dependencies
- Inline all CSS, images, and assets (no external requests)
- Keep file size minimal for fast loading
- You don't necessarily need separate pages for each status code (can reuse)
- Test error pages by temporarily stopping Sling CMS

**Example Error Page Structure**:
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Service Temporarily Unavailable</title>
    <style>
        body { font-family: sans-serif; text-align: center; padding: 50px; }
        h1 { color: #333; }
        p { color: #666; }
    </style>
</head>
<body>
    <h1>We'll be right back!</h1>
    <p>We're currently performing scheduled maintenance.</p>
    <p>Please check back in a few minutes.</p>
</body>
</html>
```

## Testing Error Pages

### Testing Site-Level Error Pages
1. Create error pages at `/content/[site]/errors/[code]`
2. Publish the error pages
3. Trigger errors by:
   - Accessing non-existent URLs (404)
   - Accessing restricted content (401/403)
   - Simulating server errors (500)
4. Verify custom error pages display correctly
5. Test fallback to `default` error page

### Testing System-Level Error Pages
1. Remove site-level error pages temporarily
2. Trigger various error conditions
3. Verify system error pages at `/static/sling-cms/errorhandling/` are used
4. Check that `default` page is used for unhandled codes

### Testing Authorization Logic
1. **As anonymous user**: Access unpublished content → expect 401
2. **As authenticated user without permission**: Access restricted content → expect 403  
3. **Any user**: Access non-existent resource → expect 404
4. Verify service user correctly determines resource existence

### Testing Apache Web Server Error Pages
1. Stop Sling CMS instance completely
2. Access site through Apache web server
3. Verify 502/503/504 static error pages display
4. Check that error pages load without Sling CMS running
5. Restart Sling CMS and verify normal operation resumes

## Troubleshooting

### Common Issues

#### Error Pages Not Displaying
**Symptom**: Generic Sling error pages instead of custom pages  
**Causes**:
- Site error pages not published
- Incorrect path structure (should be `/content/[site]/errors/`)
- Component rendering errors in error page itself
- Service user permissions issue

**Solution**:
1. Check error handler servlet logs: `org.apache.sling.cms.core.internal.servlets.CmsDefaultErrorHandlerServlet`
2. Verify error pages are published (`cq:lastReplicationAction=Activate`)
3. Test error page rendering directly via URL
4. Verify service user mapping in OSGi console

#### Stack Traces Visible to Users
**Symptom**: Java exception details exposed  
**Causes**:
- Sling default error handler has higher priority
- Error handler servlet not active
- Development mode enabled

**Solution**:
1. Check servlet ranking (should be `1`)
2. Verify servlet is active in OSGi console
3. Disable Sling's default error handler in production
4. Check for conflicting error handlers

#### 404 vs 401 vs 403 Confusion
**Symptom**: Wrong status codes returned  
**Causes**:
- Service user permissions incorrect
- Resource doesn't have published status
- Authorization logic misconfigured

**Solution**:
1. Verify `sling-cms-error` service user has `sling-readall` permissions
2. Check resource publication status
3. Review authorization logs
4. Test with different user types (anonymous, authenticated)

#### Apache Error Pages Not Working
**Symptom**: Proxy errors show Apache default pages  
**Causes**:
- `ProxyPass` directive not excluding `/ERROR`
- `Alias` directive missing or incorrect
- Static HTML files not readable by Apache
- File permissions issue

**Solution**:
1. Verify Apache configuration syntax: `apachectl configtest`
2. Check file ownership: `ls -la /var/www/vhosts/site/ERROR/`
3. Test error pages directly: `curl http://localhost/ERROR/502.html`
4. Review Apache error logs: `/var/log/apache2/error.log`

## Reference

### Related Documentation
- [Administration Guide](administration.md)
- [Error Pages (this document)](error-pages.md)
- [CMS Security](../core/src/main/java/org/apache/sling/cms/core/internal/filters/CMSSecurityFilter.java)

### Source Code
- **Servlet**: `core/src/main/java/org/apache/sling/cms/core/internal/servlets/CmsDefaultErrorHandlerServlet.java`
- **Tests**: `core/src/test/java/org/apache/sling/cms/core/internal/servlets/CmsDefaultErrorHandlerServletTest.java`
- **System Error Pages**: `frontend/src/main/resources/jcr_root/static/sling-cms/errorhandling/`
- **Error Component**: `ui/src/main/resources/jcr_root/libs/sling-cms/components/pages/error/`

### API Documentation
- `CmsDefaultErrorHandlerServlet`: Handles all error page resolution and rendering
- Service User: `sling-cms-error` with `sling-readall` permissions
- Constants:
  - `DEFAULT_ERROR_PAGE = "default"`
  - `SERVICE_USER_NAME = "sling-cms-error"`
  - `SITE_ERRORS_SUBPATH = "errors/"`
  - `SLING_CMS_ERROR_PATH = "/static/sling-cms/errorhandling/"`

---

## Summary

### Current State (✅ Production Ready)
- ✅ Hierarchical error page resolution (site → system → default)
- ✅ Intelligent 401/403/404 detection based on authorization
- ✅ Secure error rendering (no stack trace exposure)
- ✅ Service user with read-only permissions
- ✅ System-level default error pages (401, 403, 404, default)
- ✅ Apache Web Server integration for server-down scenarios
- ✅ Comprehensive test coverage

### Future Roadmap (🚀 Proposed)
- 🚀 Additional system error pages (400, 405, 429, 500, 502-504)
- 🚀 Error page templates with modern design
- 🚀 Error analytics and monitoring dashboard
- 🚀 Configuration-based error message customization
- 🚀 Author UI for error page preview and validation
- 🚀 Enhanced security (rate limiting, audit logging)
- 🚀 Graceful degradation for error handler failures

The error handling system is production-ready with robust features. Future enhancements focus on improving UX, monitoring, and security.