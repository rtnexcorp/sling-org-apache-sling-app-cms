# SPDX-License-Identifier: Apache-2.0

# Workflow Module - Implementation Status

## ✅ Completed (Phase 1: Foundation)

### Module Structure
- ✅ Maven POM configuration ([workflow/pom.xml](pom.xml))
- ✅ OSGi bundle configuration ([workflow/bnd.bnd](bnd.bnd))
- ✅ Parent POM updated to include workflow module
- ✅ Package structure and versioning
- ✅ Code formatted with Spotless

### BPMN 2.0 API Interfaces

**Core Services:**
- ✅ [ProcessEngine.java](src/main/java/org/apache/sling/cms/workflow/ProcessEngine.java) - Main workflow engine interface
- ✅ [RuntimeService.java](src/main/java/org/apache/sling/cms/workflow/RuntimeService.java) - Process instance management
- ✅ [TaskService.java](src/main/java/org/apache/sling/cms/workflow/TaskService.java) - User task operations
- ✅ [RepositoryService.java](src/main/java/org/apache/sling/cms/workflow/RepositoryService.java) - Process definition deployment
- ✅ [HistoryService.java](src/main/java/org/apache/sling/cms/workflow/HistoryService.java) - Historical data queries

**Model Interfaces:**
- ✅ [ProcessInstance.java](src/main/java/org/apache/sling/cms/workflow/ProcessInstance.java) - Running process representation
- ✅ [ProcessDefinition.java](src/main/java/org/apache/sling/cms/workflow/ProcessDefinition.java) - Deployed process definition
- ✅ [Task.java](src/main/java/org/apache/sling/cms/workflow/Task.java) - User task representation
- ✅ [TaskQuery.java](src/main/java/org/apache/sling/cms/workflow/TaskQuery.java) - Task query builder
- ✅ [Deployment.java](src/main/java/org/apache/sling/cms/workflow/Deployment.java) - Deployment information
- ✅ [HistoricProcessInstance.java](src/main/java/org/apache/sling/cms/workflow/HistoricProcessInstance.java) - Historical process data
- ✅ [HistoricTaskInstance.java](src/main/java/org/apache/sling/cms/workflow/HistoricTaskInstance.java) - Historical task data

**JavaDelegate Pattern:**
- ✅ [JavaDelegate.java](src/main/java/org/apache/sling/cms/workflow/delegate/JavaDelegate.java) - Service task interface
- ✅ [DelegateExecution.java](src/main/java/org/apache/sling/cms/workflow/delegate/DelegateExecution.java) - Execution context

**Exception Handling:**
- ✅ [WorkflowException.java](src/main/java/org/apache/sling/cms/workflow/WorkflowException.java) - Workflow exceptions

### Example BPMN Workflows
- ✅ [content-approval.bpmn20.xml](src/main/resources/workflows/content-approval.bpmn20.xml) - Simple content approval workflow

### Documentation
- ✅ [README.md](README.md) - Module overview and usage examples
- ✅ Package documentation ([package-info.java](src/main/java/org/apache/sling/cms/workflow/package-info.java))

---

## 🚧 TODO (Phase 2: Implementation)

### 1. SimpleBPMN Engine Implementation

Create native Sling BPMN engine implementation:

**Location:** `src/main/java/org/apache/sling/cms/workflow/internal/`

**Classes to implement:**
- [ ] `SimpleBPMNProcessEngine.java` - ProcessEngine implementation
- [ ] `SimpleBPMNRuntimeService.java` - RuntimeService implementation
- [ ] `SimpleBPMNTaskService.java` - TaskService implementation
- [ ] `SimpleBPMNRepositoryService.java` - RepositoryService implementation
- [ ] `SimpleBPMNHistoryService.java` - HistoryService implementation

**Reference:** See [workflow-bpmn-compliant-implementation.md](/docs/workflow-bpmn-compliant-implementation.md) for complete implementation examples.

### 2. BPMN Parser

Parse BPMN 2.0 XML files:

**Location:** `src/main/java/org/apache/sling/cms/workflow/internal/parser/`

**Classes to implement:**
- [ ] `BPMNParser.java` - Parse BPMN 2.0 XML
- [ ] `ProcessDefinitionImpl.java` - Internal process definition
- [ ] `ActivityImpl.java` - BPMN activity representation
- [ ] `SequenceFlowImpl.java` - BPMN sequence flow
- [ ] `ActivityType.java` - Enum for activity types

### 3. Process Instance Manager

Manage process state in JCR:

**Location:** `src/main/java/org/apache/sling/cms/workflow/internal/instance/`

**Classes to implement:**
- [ ] `ProcessInstanceManager.java` - Manage process instances
- [ ] `ProcessInstanceImpl.java` - ProcessInstance implementation
- [ ] `ProcessExecutor.java` - Execute process flows
- [ ] `ProcessVariablesManager.java` - Manage process variables

**JCR Structure:**
```
/var/workflow/
  ├── definitions/
  │   └── {processKey}/
  │       └── {version}/
  │           ├── bpmn.xml
  │           └── metadata
  ├── instances/
  │   └── {processInstanceId}/
  │       ├── metadata (processDefinitionKey, businessKey, startTime)
  │       ├── variables/
  │       └── tasks/
  └── history/
      └── {processInstanceId}/
```

### 4. Task Manager

Manage user tasks:

**Location:** `src/main/java/org/apache/sling/cms/workflow/internal/task/`

**Classes to implement:**
- [ ] `TaskManager.java` - Manage tasks
- [ ] `TaskImpl.java` - Task implementation
- [ ] `TaskQueryImpl.java` - TaskQuery implementation

### 5. JavaDelegate Implementations

Implement concrete service tasks:

**Location:** `src/main/java/org/apache/sling/cms/workflow/internal/delegate/`

**Classes to implement:**
- [ ] `PublishContentDelegate.java` - Publish content on approval
- [ ] `NotifyAuthorDelegate.java` - Send notification to author
- [ ] `ValidateContentDelegate.java` - Validate content policies
- [ ] `CreateVersionDelegate.java` - Create JCR version checkpoint

**Example:**
```java
@Component(
    service = JavaDelegate.class,
    property = {
        "delegate.class=org.apache.sling.cms.workflow.internal.delegate.PublishContentDelegate"
    }
)
public class PublishContentDelegate implements JavaDelegate {

    @Reference
    private PublicationManagerFactory publicationManagerFactory;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String contentPath = (String) execution.getVariable("contentPath");
        PublicationManager pm = publicationManagerFactory.getPublicationManager();
        pm.publish(contentPath);
        execution.setVariable("publishSuccess", true);
    }
}
```

### 6. Post Operations

Create Sling POST operations for workflow actions:

**Location:** `src/main/java/org/apache/sling/cms/workflow/internal/operations/`

**Classes to implement:**
- [ ] `StartWorkflowOperation.java` - Start workflow (operation name: "startWorkflow")
- [ ] `CompleteTaskOperation.java` - Complete task (operation name: "completeTask")
- [ ] `ClaimTaskOperation.java` - Claim task (operation name: "claimTask")

### 7. Sling Models

Create models for UI components:

**Location:** `src/main/java/org/apache/sling/cms/workflow/models/`

**Classes to implement:**
- [ ] `WorkflowStatusModel.java` - Display workflow state
- [ ] `WorkflowInboxModel.java` - Task inbox for reviewers
- [ ] `ProcessInstanceModel.java` - Process instance details
- [ ] `TaskDetailsModel.java` - Task details with content info

### 8. HTL UI Components

Create HTL templates:

**Location:** `src/main/resources/jcr_root/libs/sling-cms/components/workflow/`

**Components to create:**
- [ ] `status/status.html` - Workflow status badge
- [ ] `actions/actions.html` - Workflow action buttons
- [ ] `inbox/inbox.html` - Task inbox list
- [ ] `history/history.html` - Process history
- [ ] `details/details.html` - Process instance details

### 9. SCSS Styles

Create styles for workflow components:

**Location:** `frontend/src/main/frontend/scss/_workflow.scss`

**Styles needed:**
- [ ] `.cms-workflow-status` - Status badge styling
- [ ] `.cms-workflow-actions` - Action button styling
- [ ] `.cms-workflow-inbox` - Inbox table styling
- [ ] `.cms-workflow-history` - History timeline styling

### 10. Additional BPMN Workflows

**Location:** `src/main/resources/workflows/`

**Workflows to create:**
- [ ] `parallel-approval.bpmn20.xml` - Multi-reviewer parallel approval
- [ ] `scheduled-publish.bpmn20.xml` - Scheduled publishing with timers
- [ ] `multi-step-review.bpmn20.xml` - Sequential multi-step approval

### 11. OSGi Configuration

**Service User Configuration:**

Add to feature model:
```json
{
    "org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended~workflow": {
        "user.mapping": [
            "org.apache.sling.cms.workflow:workflow-executor=sling-cms-workflow"
        ]
    }
}
```

**Workflow Configuration:**
```json
{
    "org.apache.sling.cms.workflow.internal.config.WorkflowConfig": {
        "enabled": true,
        "reviewerGroups": ["content-reviewers"],
        "allowScheduledPublish": true,
        "notifyOnSubmit": true,
        "notifyOnApproval": true
    }
}
```

### 12. Unit Tests

**Location:** `src/test/java/org/apache/sling/cms/workflow/`

**Tests to create:**
- [ ] `ProcessEngineTest.java`
- [ ] `RuntimeServiceTest.java`
- [ ] `TaskServiceTest.java`
- [ ] `BPMNParserTest.java`
- [ ] `PublishContentDelegateTest.java`

---

## 🎯 Build & Test

```bash
# Build module
cd workflow
mvn clean install

# Format code
mvn spotless:apply

# Run tests
mvn test

# Deploy to Sling
mvn clean install -P autoInstallBundle
```

---

## 📚 Next Steps

1. **Start with SimpleBPMN implementation**:
   - Implement `BPMNParser` to parse content-approval.bpmn20.xml
   - Implement `SimpleBPMNProcessEngine` and services
   - Store process state in JCR under `/var/workflow/`

2. **Create JavaDelegate examples**:
   - `PublishContentDelegate` - integrate with existing PublicationManager
   - `NotifyAuthorDelegate` - log notifications (email integration later)

3. **Build UI components**:
   - Workflow status badge
   - Task inbox page
   - Workflow action buttons

4. **Integration testing**:
   - Start content-approval workflow via POST
   - Complete review task
   - Verify content is published

5. **Documentation**:
   - API usage examples
   - BPMN modeling guide
   - Migration guide to Flowable

---

## 📖 Reference Documentation

- [Workflow Implementation Guide](/docs/workflow-implementation.md)
- [BPMN 2.0 Compliant Implementation](/docs/workflow-bpmn-compliant-implementation.md)
- [Content Distribution](/docs/content-distribution.md)
- [BPMN 2.0 Specification](https://www.omg.org/spec/BPMN/2.0/)
- [Flowable User Guide](https://www.flowable.com/open-source/docs)

---

## 🏗️ Architecture Summary

### API Layer (✅ COMPLETE)
```
ProcessEngine (interface)
    ├── RuntimeService (interface)
    ├── TaskService (interface)
    ├── RepositoryService (interface)
    └── HistoryService (interface)
```

### Implementation Layer (🚧 TODO)
```
SimpleBPMNProcessEngine (OSGi service)
    ├── BPMNParser (parse XML)
    ├── ProcessInstanceManager (JCR storage)
    ├── TaskManager (task lifecycle)
    └── Delegates (service task logic)
```

### Integration Layer (🚧 TODO)
```
Post Operations → ProcessEngine API
Sling Models → ProcessEngine API
HTL Components → Sling Models
```

---

## 🔄 Migration Path to Flowable

When SimpleBPMN limitations are reached:

1. Add Flowable dependencies to `pom.xml`
2. Create `FlowableProcessEngineAdapter` implementing `ProcessEngine`
3. Register with higher OSGi service ranking
4. **Zero changes to:**
   - BPMN XML files ✅
   - JavaDelegate implementations ✅
   - Client code using ProcessEngine API ✅

---

**Status:** Foundation complete, implementation in progress.

**Last Updated:** 2026-01-04
