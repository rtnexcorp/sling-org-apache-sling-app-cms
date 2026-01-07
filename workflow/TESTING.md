# Workflow Test Servlet Guide

## Overview
The Workflow Test Servlet provides a comprehensive web-based test suite for the Apache Sling CMS Workflow module. It allows you to interactively test all three core services (RepositoryService, RuntimeService, and HistoryService) through your browser.

## Access

**URL**: http://localhost:8082/bin/workflow/test

**Credentials**: admin/admin (default Sling credentials)

## Available Actions

### 1. Full Test (`?action=full` or no parameter)
Runs a complete test suite that exercises all workflow functionality:
- Deploys a test workflow
- Starts a process instance
- Lists active instances
- Manages tasks
- Queries historical data

**Example**: http://localhost:8082/bin/workflow/test

### 2. Deploy Workflow (`?action=deploy`)
Tests workflow deployment using embedded BPMN XML:
- Creates a test process definition
- Includes user task, service task, and gateway
- Stores definition in JCR at `/var/workflow/definitions`

**Example**: http://localhost:8082/bin/workflow/test?action=deploy

### 3. Start Process Instance (`?action=start`)
Tests process instance creation:
- Starts a new instance of testProcess
- Sets initial variables (author, startedFrom)
- Generates unique business key

**Parameters**:
- `processKey` - Process definition key (default: testProcess)
- `businessKey` - Business key for the instance (default: TEST-{timestamp})

**Example**: http://localhost:8082/bin/workflow/test?action=start&businessKey=MY-123

### 4. List Active Instances (`?action=list`)
Displays all active process instances and tasks:
- Shows process instance details (ID, business key, state)
- Lists all active tasks with assignees
- Provides links to complete tasks

**Example**: http://localhost:8082/bin/workflow/test?action=list

### 5. Complete Task (`?action=complete`)
Completes a specific task:
- Sets task variables (approved=true, completedBy)
- Moves process instance to next activity

**Parameters**:
- `taskId` - Required task ID to complete

**Example**: http://localhost:8082/bin/workflow/test?action=complete&taskId=task-123

### 6. View History (`?action=history`)
Displays historical data:
- Completed process instances with duration
- Completed tasks for each process
- Start/end times and performance metrics

**Example**: http://localhost:8082/bin/workflow/test?action=history

## Test Workflow Structure

The test servlet deploys this workflow:

```
Start → Review Task → Approval Gateway → Publish (Service Task) → End (Published)
                                      └→ End (Rejected)
```

**Activities**:
- **start**: Start event
- **task1**: User task (Review Task) - assigned to "reviewers" group
- **gateway1**: Exclusive gateway - checks `${approved == true}`
- **task2**: Service task (Publish) - executes `TestPublishDelegate`
- **end1**: End event (Published path)
- **end2**: End event (Rejected path)

## Test JavaDelegate

The servlet includes a test service task delegate:

**Class**: `org.apache.sling.cms.workflow.test.TestPublishDelegate`

**Functionality**:
- Logs process instance ID and variables
- Simulates publish action (100ms delay)
- Sets result variables: `publishedAt`, `publishStatus`

## Expected Behavior

### Full Test Flow:
1. **Deploy** - Creates testProcess definition
2. **Start Instance** - Creates process instance with variables
3. **List Active** - Shows 1 active instance, 1 active task (Review Task)
4. **Complete Task** - Sets approved=true, moves to gateway
5. **Gateway Evaluation** - Routes to Publish task (approved=true)
6. **Service Task Execution** - TestPublishDelegate executes
7. **Process Completion** - Reaches end event
8. **History** - Shows completed process with 1 completed task

### Validation Points:
✅ Workflow deployed successfully  
✅ Process instance created with unique ID  
✅ Variables stored correctly  
✅ Task created and assignable  
✅ Task completion triggers gateway evaluation  
✅ Service task delegate executes  
✅ Historical data persisted  

## Troubleshooting

### Bundle Not Active
```bash
# Check bundle status
curl -u admin:admin http://localhost:8082/system/console/bundles.json | grep workflow

# Verify bundle is Active (stateRaw: 32)
```

### ResourceResolver Not Set Error
**Error**: `IllegalStateException: ResourceResolver not set in context`

**Solution**: The TaskService requires a ResourceResolver context. The test servlet handles this automatically:
```java
// Set ResourceResolver context before TaskService operations
taskService.setResolverContext(request.getResourceResolver());

try {
    // Use TaskService methods
    TaskQuery query = taskService.createTaskQuery();
    // ...
} finally {
    // Always clear context to prevent memory leaks
    taskService.clearResolverContext();
}
```

**Important**: Always clear the context in a finally block to prevent ThreadLocal memory leaks.

### No Process Definitions Found
```bash
# Check JCR repository
# Navigate to: http://localhost:8082/bin/browser.html
# Look for: /var/workflow/definitions/testProcess
```

### Tasks Not Created
```bash
# Check logs
tail -f deployment/author/sling/logs/error.log | grep -i workflow

# Verify service user permissions
# Check: /var/workflow/* paths are accessible
```

### Service Task Not Executing
```bash
# Verify JavaDelegate is registered
# Web Console: http://localhost:8082/system/console/services
# Search for: TestPublishDelegate
```

## Integration with CMS

The test servlet demonstrates patterns you can use in your CMS components:

```java
// Inject services
@Reference
private RuntimeService runtimeService;

@Reference
private TaskService taskService;

// Start workflow for content approval
Map<String, Object> variables = new HashMap<>();
variables.put("contentPath", "/content/mypage");
variables.put("author", "admin");

ProcessInstance instance = runtimeService.startProcessInstanceByKey(
    "contentApproval",
    "/content/mypage", // business key
    variables
);

// IMPORTANT: Set ResourceResolver context before TaskService operations
taskService.setResolverContext(request.getResourceResolver());

try {
    // Query user's tasks
    List<Task> myTasks = taskService.createTaskQuery()
        .taskAssignee(request.getRemoteUser())
        .list();

    // Complete task
    Map<String, Object> result = new HashMap<>();
    result.put("approved", true);
    taskService.complete(taskId, result);
} finally {
    // Always clear context to prevent memory leaks
    taskService.clearResolverContext();
}
```

## Next Steps

After validating the test servlet:

1. **Build CMS UI Components**:
   - Workflow inbox component
   - Process instance monitor
   - Workflow start form

2. **Create Real Workflows**:
   - Content approval workflow
   - Publishing workflow
   - Asset review workflow

3. **Add REST API**:
   - RESTful endpoints for external systems
   - JSON responses for AJAX calls
   - Webhook support

## Files Created

```
workflow/src/main/java/org/apache/sling/cms/workflow/test/
├── WorkflowTestServlet.java      # Main test servlet
├── TestPublishDelegate.java      # Service task delegate
└── package-info.java              # Package versioning
```

## Status

✅ **Task 1 Complete**: Create a test servlet/component to exercise the workflow API

The test servlet is fully functional and deployed to:
**http://localhost:8082/bin/workflow/test**
