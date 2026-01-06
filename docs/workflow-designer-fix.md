# Workflow Designer Fix - January 6, 2026

## Issue
The workflow designer was causing errors when trying to save workflows:
```
java.lang.UnsupportedOperationException: create 'undefined' at /mnt/overlay/sling-cms/content/config
```

## Root Cause
The JavaScript configuration object `window.WORKFLOW_DESIGNER_CONFIG` was not properly initialized, causing the workflow designer to use `undefined` URLs for all operations.

## Solution

### 1. Fixed JavaScript Configuration
**File**: `/frontend/src/main/frontend/js/cms.workflow-designer.js`

Added default configuration in the constructor:
```javascript
this.config = window.WORKFLOW_DESIGNER_CONFIG || {
    baseUrl: '/bin/workflow/designer',
    saveUrl: '/bin/workflow/designer?operation=save',
    loadUrl: '/bin/workflow/designer?operation=load',
    deleteUrl: '/bin/workflow/designer?operation=delete',
    deployUrl: '/bin/workflow/designer?operation=deploy',
    listUrl: '/bin/workflow/designer?operation=list'
};
```

### 2. Updated All Fetch Calls to Handle JSON Responses
All AJAX operations now properly handle JSON responses from the servlet:

- **Save**: Saves workflow design to `/etc/workflow/designs`
- **Load**: Loads workflow design from `/etc/workflow/designs`
- **Delete**: Removes workflow design from `/etc/workflow/designs`
- **Deploy**: Deploys workflow definition to `/etc/workflow/definitions` (execution-ready)

### 3. Storage Architecture

Following Sling/JCR best practices:

#### Persistent Configuration (`/etc`)
- `/etc/workflow/designs` - BPMN workflow designs (editable in designer)
- `/etc/workflow/definitions` - Deployed workflow definitions (execution-ready)

#### Temporary Runtime Data (`/var`)
- `/var/workflow/instances` - Active workflow instances (can be cleaned)
- `/var/workflow/tasks` - Active user tasks (can be cleaned)
- `/var/workflow/history` - Completed workflow history (can be cleaned)

## Changes Summary

### Files Modified

1. **feature/src/main/features/workflow.json**
   - Created `/etc/workflow/definitions` path
   - Created `/etc/workflow/designs` path
   - Set proper ACLs for authors, administrators, and service user
   - Moved definitions from `/var` to `/etc`

2. **workflow/src/main/java/.../BPMNRepositoryService.java**
   - Changed `DEFINITIONS_PATH` from `/var/workflow/definitions` to `/etc/workflow/definitions`

3. **workflow/src/main/java/.../WorkflowDesignerServlet.java**
   - Fixed servlet registration (single path with operation parameter)
   - Added CSRF protection
   - Added proper permission error handling
   - All responses now return JSON format
   - Added `listWorkflows` operation

4. **frontend/src/main/frontend/js/cms.workflow-designer.js**
   - Added default configuration URLs
   - Updated all fetch calls to handle JSON responses
   - Added proper error handling with user-friendly messages
   - Added location info in success messages

## Testing

All JUnit tests passing (6/6):
```
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

## API Endpoints

### New Unified Endpoint
```
Base URL: /bin/workflow/designer

Operations:
- GET  ?operation=load&key={workflowKey}   - Load workflow design
- GET  ?operation=list                      - List all workflow designs
- POST ?operation=save                      - Save workflow design
- POST ?operation=delete                    - Delete workflow design
- POST ?operation=deploy                    - Deploy to workflow engine
```

### Response Format
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { /* operation-specific data */ }
}
```

## Security Features

1. **Authentication**: Requires logged-in user (SlingAuthenticator)
2. **Authorization**: JCR permissions on `/etc/workflow` paths
3. **CSRF Protection**: Validates Referer header on POST requests
4. **Permission Handling**: User-friendly error messages for access denied

## Permissions

- **authors**: Can read/write workflow designs
- **administrators**: Full access to all workflow paths
- **sling-cms-workflow**: Service user with full access
- **everyone**: Read-only access to deployed definitions

## Next Steps

1. Deploy the changes
2. Test workflow designer UI
3. Verify permissions work correctly for different user roles
4. Test workflow deployment and execution
