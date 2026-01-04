# SPDX-License-Identifier: Apache-2.0

# Apache Sling CMS - Workflow Module

BPMN 2.0 compliant workflow engine for Apache Sling CMS.

## Overview

This module provides a workflow engine that is compatible with BPMN 2.0 standard, allowing you to:

- Define workflows in BPMN 2.0 XML format
- Start with a lightweight native Sling implementation
- Seamlessly migrate to Flowable or Camunda later without code changes
- Use industry-standard workflow modeling tools (Camunda Modeler)

## Architecture

```
BPMN 2.0 XML Files (Standard)
         ↓
   Workflow API Layer
         ↓
    ┌─────────┴─────────┐
    ↓                   ↓
Native Sling      Flowable Engine
(SimpleBPMN)      (Future)
```

## Module Structure

```
workflow/
├── pom.xml                              # Maven configuration
├── bnd.bnd                              # OSGi bundle configuration
├── README.md                            # This file
└── src/main/java/org/apache/sling/cms/workflow/
    ├── ProcessEngine.java               # Main engine interface
    ├── RuntimeService.java              # Process instance management
    ├── TaskService.java                 # User task management
    ├── RepositoryService.java           # Process definition management
    ├── HistoryService.java              # Historical data queries
    ├── ProcessInstance.java             # Running process model
    ├── Task.java                        # User task model
    ├── WorkflowException.java           # Exception handling
    └── delegate/
        ├── JavaDelegate.java            # Service task interface
        └── DelegateExecution.java       # Execution context
```

## Current Status

### ✅ Completed

- Module structure and Maven configuration
- OSGi bundle configuration (bnd.bnd)
- BPMN 2.0 API interfaces:
  - `ProcessEngine` - Main workflow engine
  - `RuntimeService` - Process instance management
  - `TaskService` - User task operations
  - `RepositoryService` - Process definition deployment
  - `HistoryService` - Historical data queries
- Model interfaces:
  - `ProcessInstance` - Running process representation
  - `Task` - User task representation
- JavaDelegate pattern for service tasks:
  - `JavaDelegate` - Service task implementation interface
  - `DelegateExecution` - Execution context
- Exception handling (`WorkflowException`)

### 🚧 TODO (Next Steps)

1. **Implementation Classes** (see `/docs/workflow-bpmn-compliant-implementation.md`):
   - `SimpleBPMNProcessEngine` - Native Sling engine implementation
   - `BPMNParser` - Parse BPMN 2.0 XML files
   - `ProcessInstanceManager` - Manage process state in JCR
   - `TaskManager` - Manage user tasks
   - Service implementations for RuntimeService, TaskService, etc.

2. **Example JavaDelegates**:
   - `PublishContentDelegate` - Publish content on approval
   - `NotifyAuthorDelegate` - Send notifications
   - `ValidateContentDelegate` - Validate content policies

3. **Sling Models** for UI:
   - `WorkflowStatus` - Display workflow state
   - `WorkflowInbox` - Task inbox for reviewers
   - `ProcessInstanceModel` - Process instance details

4. **HTL Components**:
   - `workflow/status` - Workflow status badge
   - `workflow/actions` - Workflow action buttons
   - `workflow/inbox` - Task inbox list
   - `workflow/history` - Process history

5. **Example BPMN Workflows**:
   - `content-approval.bpmn20.xml` - Simple approval workflow
   - `parallel-approval.bpmn20.xml` - Multi-reviewer workflow
   - `scheduled-publish.bpmn20.xml` - Scheduled publishing

6. **Integration with CMS**:
   - Post operations for workflow actions
   - Service user configuration
   - Feature model updates

## API Usage Examples

### Starting a Workflow

```java
@Reference
private ProcessEngine processEngine;

public void submitContentForReview(String contentPath, String authorId) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("contentPath", contentPath);
    variables.put("authorId", authorId);

    ProcessInstance instance = processEngine.getRuntimeService()
        .startProcessInstanceByKey("contentApproval", contentPath, variables);
}
```

### Completing a Task

```java
public void approveContent(String taskId, String reviewComment) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("approved", true);
    variables.put("reviewComment", reviewComment);

    processEngine.getTaskService()
        .complete(taskId, variables);
}
```

### Implementing a JavaDelegate

```java
@Component(
    service = JavaDelegate.class,
    property = {
        "delegate.class=org.apache.sling.cms.workflow.delegate.PublishContentDelegate"
    }
)
public class PublishContentDelegate implements JavaDelegate {

    @Reference
    private PublicationManager publicationManager;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String contentPath = (String) execution.getVariable("contentPath");
        publicationManager.publish(contentPath);
        execution.setVariable("publishSuccess", true);
    }
}
```

## BPMN 2.0 Workflow Example

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             targetNamespace="http://sling.apache.org/cms/workflows">

  <process id="contentApproval" name="Content Approval" isExecutable="true">

    <startEvent id="start" name="Content Submitted"/>

    <userTask id="reviewTask" name="Review Content"
              flowable:candidateGroups="content-reviewers"/>

    <exclusiveGateway id="decision" name="Approved?"/>

    <serviceTask id="publish" name="Publish Content"
                 flowable:class="org.apache.sling.cms.workflow.delegate.PublishContentDelegate"/>

    <endEvent id="end" name="Complete"/>

    <sequenceFlow sourceRef="start" targetRef="reviewTask"/>
    <sequenceFlow sourceRef="reviewTask" targetRef="decision"/>
    <sequenceFlow sourceRef="decision" targetRef="publish">
      <conditionExpression>${approved == true}</conditionExpression>
    </sequenceFlow>
    <sequenceFlow sourceRef="publish" targetRef="end"/>

  </process>
</definitions>
```

## Building

```bash
# Build workflow module
cd workflow
mvn clean install

# Build and deploy to running Sling instance
mvn clean install -P autoInstallBundle
```

## Testing

```bash
# Run unit tests
mvn test

# Run with test coverage
mvn clean verify
```

## Migration to Flowable

When you need advanced BPMN features (parallel gateways, timers, sub-processes):

1. Add Flowable dependencies to `pom.xml`
2. Create `FlowableProcessEngineAdapter` implementing `ProcessEngine`
3. Deploy with higher OSGi service ranking
4. **No changes to BPMN files or client code needed!**

See `/docs/workflow-bpmn-compliant-implementation.md` for complete migration guide.

## Documentation

- [Workflow Implementation Guide](/docs/workflow-implementation.md) - Simple native workflow
- [BPMN 2.0 Compliant Implementation](/docs/workflow-bpmn-compliant-implementation.md) - Full BPMN guide
- [Content Distribution](/docs/content-distribution.md) - Publication system integration

## License

Apache License 2.0 - See LICENSE file for details.
