# SPDX-License-Identifier: Apache-2.0

# BPMN 2.0 Compliant Workflow Implementation

## Overview

This document provides a **BPMN 2.0 compliant** workflow implementation strategy for Apache Sling CMS. The approach allows you to:

1. Start with **native Sling infrastructure** (no external dependencies)
2. Use **BPMN 2.0 concepts and terminology** from day one
3. Define workflows in **BPMN 2.0 XML format**
4. Maintain a **clean migration path** to Flowable/Camunda later
5. Avoid vendor lock-in and rewrites

---

## Table of Contents

1. [BPMN 2.0 Fundamentals](#bpmn-20-fundamentals)
2. [Architecture Strategy](#architecture-strategy)
3. [BPMN Process Definitions](#bpmn-process-definitions)
4. [BPMN-Compliant API Design](#bpmn-compliant-api-design)
5. [Implementation](#implementation)
6. [Migration Path to Flowable](#migration-path-to-flowable)
7. [Code Examples](#code-examples)

---

## BPMN 2.0 Fundamentals

### What is BPMN 2.0?

**Business Process Model and Notation 2.0** is an OMG standard for describing business processes using:
- Visual flow diagrams
- Standardized XML representation
- Execution semantics (workflow engines can execute BPMN)

### Core BPMN Elements

| Element | Description | BPMN Symbol |
|---------|-------------|-------------|
| **Start Event** | Workflow entry point | ○ (circle) |
| **End Event** | Workflow completion | ⊙ (bold circle) |
| **User Task** | Human activity (approval, review) | ▭ (rounded rectangle) |
| **Service Task** | Automated activity (publish, notify) | ⚙ (gear icon) |
| **Exclusive Gateway** | Decision point (if/else) | ◇ (diamond) |
| **Parallel Gateway** | Fork/join parallel paths | ⊕ (diamond with +) |
| **Sequence Flow** | Connecting arrows | → |
| **Timer Event** | Time-based triggers | ⏰ (clock icon) |

### BPMN 2.0 Terminology

Use BPMN terminology in your code to maintain standard compliance:

| BPMN Term | Instead of | Description |
|-----------|------------|-------------|
| **Process Definition** | Workflow template | Blueprint for workflow |
| **Process Instance** | Workflow execution | Running workflow |
| **Process Variables** | Workflow context | Data passed through workflow |
| **Task** | Workflow step | Unit of work |
| **Activity** | Action | Generic work item |
| **Gateway** | Decision point | Branching logic |
| **Execution** | Workflow state | Current position in process |

---

## Architecture Strategy

### Hybrid Approach: BPMN-Compliant Native Implementation

```
┌─────────────────────────────────────────────────────────────┐
│                   BPMN 2.0 Process Definitions              │
│                      (XML files in /conf)                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              BPMN API Layer (your interfaces)                │
│  ProcessEngine, ProcessInstance, TaskService, etc.           │
└─────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
         ┌──────────────────┐  ┌──────────────────┐
         │  Native Sling     │  │   Flowable       │
         │  Implementation   │  │   Implementation │
         │  (Phase 1)        │  │   (Phase 2+)     │
         └──────────────────┘  └──────────────────┘
                    │                   │
                    └─────────┬─────────┘
                              ▼
         ┌─────────────────────────────────────────┐
         │  Sling Infrastructure                    │
         │  (Jobs, Events, PublicationManager)      │
         └─────────────────────────────────────────┘
```

### Key Principles

1. **BPMN XML as Source of Truth** - Define workflows in BPMN 2.0 XML format
2. **BPMN-Compliant APIs** - Use BPMN terminology and concepts
3. **Pluggable Engine** - Abstract engine implementation behind interfaces
4. **No Vendor Lock-in** - Switch engines without changing process definitions

---

## BPMN Process Definitions

### Example: Content Approval Process

**File**: `/conf/slingcms/workflows/content-approval.bpmn20.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://sling.apache.org/cms/workflows"
             id="content-approval-definitions">

  <process id="contentApproval" name="Content Approval Workflow" isExecutable="true">

    <!-- Start Event -->
    <startEvent id="startEvent" name="Content Submitted">
      <documentation>
        Triggered when author submits content for review
      </documentation>
    </startEvent>

    <!-- User Task: Review Content -->
    <userTask id="reviewTask" name="Review Content">
      <documentation>
        Content reviewer evaluates submitted content and decides to approve or reject
      </documentation>
      <potentialOwner>
        <resourceAssignmentExpression>
          <formalExpression>content-reviewers</formalExpression>
        </resourceAssignmentExpression>
      </potentialOwner>
    </userTask>

    <!-- Exclusive Gateway: Decision Point -->
    <exclusiveGateway id="approvalDecision" name="Approved?" />

    <!-- Service Task: Publish Content -->
    <serviceTask id="publishTask" name="Publish Content"
                 flowable:class="org.apache.sling.cms.workflow.delegate.PublishContentDelegate">
      <documentation>
        Automatically publish approved content to renderer instances
      </documentation>
    </serviceTask>

    <!-- Service Task: Notify Author (Approved) -->
    <serviceTask id="notifyApprovedTask" name="Notify Author - Approved"
                 flowable:class="org.apache.sling.cms.workflow.delegate.NotifyAuthorDelegate">
      <extensionElements>
        <flowable:field name="notificationType">
          <flowable:string>APPROVED</flowable:string>
        </flowable:field>
      </extensionElements>
    </serviceTask>

    <!-- Service Task: Notify Author (Rejected) -->
    <serviceTask id="notifyRejectedTask" name="Notify Author - Rejected"
                 flowable:class="org.apache.sling.cms.workflow.delegate.NotifyAuthorDelegate">
      <extensionElements>
        <flowable:field name="notificationType">
          <flowable:string>REJECTED</flowable:string>
        </flowable:field>
      </extensionElements>
    </serviceTask>

    <!-- End Event: Success -->
    <endEvent id="endEventSuccess" name="Published" />

    <!-- End Event: Rejected -->
    <endEvent id="endEventRejected" name="Rejected" />

    <!-- Sequence Flows -->
    <sequenceFlow id="flow1" sourceRef="startEvent" targetRef="reviewTask" />

    <sequenceFlow id="flow2" sourceRef="reviewTask" targetRef="approvalDecision" />

    <sequenceFlow id="flow3" name="Approved" sourceRef="approvalDecision" targetRef="publishTask">
      <conditionExpression xsi:type="tFormalExpression">
        ${approved == true}
      </conditionExpression>
    </sequenceFlow>

    <sequenceFlow id="flow4" name="Rejected" sourceRef="approvalDecision" targetRef="notifyRejectedTask">
      <conditionExpression xsi:type="tFormalExpression">
        ${approved == false}
      </conditionExpression>
    </sequenceFlow>

    <sequenceFlow id="flow5" sourceRef="publishTask" targetRef="notifyApprovedTask" />

    <sequenceFlow id="flow6" sourceRef="notifyApprovedTask" targetRef="endEventSuccess" />

    <sequenceFlow id="flow7" sourceRef="notifyRejectedTask" targetRef="endEventRejected" />

  </process>

</definitions>
```

### Advanced Example: Parallel Review Process

**File**: `/conf/slingcms/workflows/parallel-approval.bpmn20.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://sling.apache.org/cms/workflows"
             id="parallel-approval-definitions">

  <process id="parallelApproval" name="Parallel Approval Workflow" isExecutable="true">

    <startEvent id="start" name="Content Submitted" />

    <!-- Parallel Gateway: Fork -->
    <parallelGateway id="forkReview" name="Split Reviews" />

    <!-- Parallel User Tasks -->
    <userTask id="technicalReview" name="Technical Review"
              flowable:candidateGroups="technical-reviewers" />

    <userTask id="editorialReview" name="Editorial Review"
              flowable:candidateGroups="editorial-reviewers" />

    <userTask id="legalReview" name="Legal Review"
              flowable:candidateGroups="legal-reviewers" />

    <!-- Parallel Gateway: Join -->
    <parallelGateway id="joinReview" name="Merge Reviews" />

    <!-- Exclusive Gateway: All Approved? -->
    <exclusiveGateway id="allApprovedDecision" name="All Approved?" />

    <!-- Service Task: Publish -->
    <serviceTask id="publish" name="Publish Content"
                 flowable:class="org.apache.sling.cms.workflow.delegate.PublishContentDelegate" />

    <!-- Service Task: Return to Author -->
    <serviceTask id="returnToAuthor" name="Return to Author"
                 flowable:class="org.apache.sling.cms.workflow.delegate.ReturnToAuthorDelegate" />

    <endEvent id="endSuccess" name="Published" />
    <endEvent id="endRejected" name="Rejected" />

    <!-- Sequence Flows -->
    <sequenceFlow sourceRef="start" targetRef="forkReview" />

    <sequenceFlow sourceRef="forkReview" targetRef="technicalReview" />
    <sequenceFlow sourceRef="forkReview" targetRef="editorialReview" />
    <sequenceFlow sourceRef="forkReview" targetRef="legalReview" />

    <sequenceFlow sourceRef="technicalReview" targetRef="joinReview" />
    <sequenceFlow sourceRef="editorialReview" targetRef="joinReview" />
    <sequenceFlow sourceRef="legalReview" targetRef="joinReview" />

    <sequenceFlow sourceRef="joinReview" targetRef="allApprovedDecision" />

    <sequenceFlow name="All Approved" sourceRef="allApprovedDecision" targetRef="publish">
      <conditionExpression xsi:type="tFormalExpression">
        ${technicalApproved == true &amp;&amp; editorialApproved == true &amp;&amp; legalApproved == true}
      </conditionExpression>
    </sequenceFlow>

    <sequenceFlow name="Any Rejected" sourceRef="allApprovedDecision" targetRef="returnToAuthor">
      <conditionExpression xsi:type="tFormalExpression">
        ${technicalApproved == false || editorialApproved == false || legalApproved == false}
      </conditionExpression>
    </sequenceFlow>

    <sequenceFlow sourceRef="publish" targetRef="endSuccess" />
    <sequenceFlow sourceRef="returnToAuthor" targetRef="endRejected" />

  </process>

</definitions>
```

### Scheduled Publishing with Timer

**File**: `/conf/slingcms/workflows/scheduled-publish.bpmn20.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://sling.apache.org/cms/workflows"
             id="scheduled-publish-definitions">

  <process id="scheduledPublish" name="Scheduled Publishing" isExecutable="true">

    <startEvent id="start" name="Content Approved" />

    <!-- Intermediate Timer Event -->
    <intermediateCatchEvent id="waitUntilPublishDate" name="Wait Until Publish Date">
      <timerEventDefinition>
        <timeDate>${scheduledPublishDate}</timeDate>
      </timerEventDefinition>
    </intermediateCatchEvent>

    <!-- Service Task: Publish -->
    <serviceTask id="publish" name="Publish Content"
                 flowable:class="org.apache.sling.cms.workflow.delegate.PublishContentDelegate" />

    <!-- Service Task: Notify -->
    <serviceTask id="notify" name="Notify Author"
                 flowable:class="org.apache.sling.cms.workflow.delegate.NotifyAuthorDelegate" />

    <endEvent id="end" name="Published" />

    <!-- Sequence Flows -->
    <sequenceFlow sourceRef="start" targetRef="waitUntilPublishDate" />
    <sequenceFlow sourceRef="waitUntilPublishDate" targetRef="publish" />
    <sequenceFlow sourceRef="publish" targetRef="notify" />
    <sequenceFlow sourceRef="notify" targetRef="end" />

  </process>

</definitions>
```

---

## BPMN-Compliant API Design

### Core Interfaces

Design your API to mirror BPMN 2.0 and Flowable/Camunda APIs:

#### 1. ProcessEngine

**Location**: `api/src/main/java/org/apache/sling/cms/workflow/ProcessEngine.java`

```java
package org.apache.sling.cms.workflow;

import org.apache.sling.api.resource.ResourceResolver;

/**
 * BPMN 2.0 Process Engine interface.
 *
 * This interface abstracts the underlying workflow engine implementation,
 * allowing seamless migration from native Sling to Flowable/Camunda.
 */
public interface ProcessEngine {

    /**
     * Get the runtime service for managing process instances.
     */
    RuntimeService getRuntimeService();

    /**
     * Get the task service for managing user tasks.
     */
    TaskService getTaskService();

    /**
     * Get the repository service for managing process definitions.
     */
    RepositoryService getRepositoryService();

    /**
     * Get the history service for querying completed processes.
     */
    HistoryService getHistoryService();
}
```

#### 2. RuntimeService

**Location**: `api/src/main/java/org/apache/sling/cms/workflow/RuntimeService.java`

```java
package org.apache.sling.cms.workflow;

import java.util.Map;

/**
 * Service for managing runtime process instances.
 *
 * Modeled after Flowable RuntimeService API.
 */
public interface RuntimeService {

    /**
     * Start a new process instance by process definition key.
     *
     * @param processDefinitionKey Process key from BPMN XML (e.g., "contentApproval")
     * @param variables Process variables (initial context)
     * @return Process instance
     */
    ProcessInstance startProcessInstanceByKey(String processDefinitionKey, Map<String, Object> variables);

    /**
     * Start a new process instance with business key.
     *
     * @param processDefinitionKey Process definition key
     * @param businessKey Unique business identifier (e.g., content path)
     * @param variables Process variables
     * @return Process instance
     */
    ProcessInstance startProcessInstanceByKey(String processDefinitionKey, String businessKey,
                                               Map<String, Object> variables);

    /**
     * Delete a running process instance.
     *
     * @param processInstanceId Process instance ID
     * @param deleteReason Reason for deletion
     */
    void deleteProcessInstance(String processInstanceId, String deleteReason);

    /**
     * Get process variables.
     *
     * @param processInstanceId Process instance ID
     * @return Process variables
     */
    Map<String, Object> getVariables(String processInstanceId);

    /**
     * Set process variables.
     *
     * @param processInstanceId Process instance ID
     * @param variables Variables to set
     */
    void setVariables(String processInstanceId, Map<String, Object> variables);
}
```

#### 3. TaskService

**Location**: `api/src/main/java/org/apache/sling/cms/workflow/TaskService.java`

```java
package org.apache.sling.cms.workflow;

import java.util.List;
import java.util.Map;

/**
 * Service for managing user tasks.
 *
 * Modeled after Flowable TaskService API.
 */
public interface TaskService {

    /**
     * Get a task by ID.
     *
     * @param taskId Task ID
     * @return Task instance
     */
    Task getTask(String taskId);

    /**
     * Query tasks assigned to a user.
     *
     * @param userId User ID
     * @return List of tasks
     */
    List<Task> getTasksAssignedTo(String userId);

    /**
     * Query tasks by candidate group.
     *
     * @param groupId Group ID (e.g., "content-reviewers")
     * @return List of tasks
     */
    List<Task> getTasksByCandidateGroup(String groupId);

    /**
     * Complete a task.
     *
     * @param taskId Task ID
     * @param variables Variables to set when completing task
     */
    void complete(String taskId, Map<String, Object> variables);

    /**
     * Claim a task for a user.
     *
     * @param taskId Task ID
     * @param userId User ID
     */
    void claim(String taskId, String userId);

    /**
     * Create a task query builder.
     *
     * @return Task query
     */
    TaskQuery createTaskQuery();
}
```

#### 4. RepositoryService

**Location**: `api/src/main/java/org/apache/sling/cms/workflow/RepositoryService.java`

```java
package org.apache.sling.cms.workflow;

import java.io.InputStream;
import java.util.List;

/**
 * Service for managing process definitions.
 *
 * Modeled after Flowable RepositoryService API.
 */
public interface RepositoryService {

    /**
     * Deploy a process definition from BPMN 2.0 XML.
     *
     * @param name Deployment name
     * @param bpmnXml BPMN XML input stream
     * @return Deployment
     */
    Deployment deploy(String name, InputStream bpmnXml);

    /**
     * Get all deployed process definitions.
     *
     * @return List of process definitions
     */
    List<ProcessDefinition> getProcessDefinitions();

    /**
     * Get process definition by key.
     *
     * @param processDefinitionKey Process definition key
     * @return Process definition
     */
    ProcessDefinition getProcessDefinitionByKey(String processDefinitionKey);

    /**
     * Delete a deployment.
     *
     * @param deploymentId Deployment ID
     * @param cascade Delete related process instances
     */
    void deleteDeployment(String deploymentId, boolean cascade);
}
```

#### 5. HistoryService

**Location**: `api/src/main/java/org/apache/sling/cms/workflow/HistoryService.java`

```java
package org.apache.sling.cms.workflow;

import java.util.List;

/**
 * Service for querying historical process data.
 *
 * Modeled after Flowable HistoryService API.
 */
public interface HistoryService {

    /**
     * Query completed process instances.
     *
     * @param processDefinitionKey Process definition key
     * @return List of historic process instances
     */
    List<HistoricProcessInstance> getCompletedProcessInstances(String processDefinitionKey);

    /**
     * Query completed tasks.
     *
     * @param processInstanceId Process instance ID
     * @return List of historic tasks
     */
    List<HistoricTaskInstance> getCompletedTasks(String processInstanceId);

    /**
     * Get process instance history.
     *
     * @param processInstanceId Process instance ID
     * @return Historic process instance
     */
    HistoricProcessInstance getHistoricProcessInstance(String processInstanceId);
}
```

### BPMN Model Classes

#### ProcessInstance

**Location**: `api/src/main/java/org/apache/sling/cms/workflow/ProcessInstance.java`

```java
package org.apache.sling.cms.workflow;

import java.util.Date;
import java.util.Map;

/**
 * Represents a running process instance.
 */
public interface ProcessInstance {

    /**
     * Unique process instance ID.
     */
    String getId();

    /**
     * Process definition key from BPMN XML.
     */
    String getProcessDefinitionKey();

    /**
     * Business key (e.g., content path).
     */
    String getBusinessKey();

    /**
     * Start time.
     */
    Date getStartTime();

    /**
     * Whether process is ended.
     */
    boolean isEnded();

    /**
     * Current activity ID.
     */
    String getCurrentActivityId();

    /**
     * Process variables.
     */
    Map<String, Object> getVariables();
}
```

#### Task

**Location**: `api/src/main/java/org/apache/sling/cms/workflow/Task.java`

```java
package org.apache.sling.cms.workflow;

import java.util.Date;

/**
 * Represents a user task in a process.
 */
public interface Task {

    /**
     * Unique task ID.
     */
    String getId();

    /**
     * Task name from BPMN XML.
     */
    String getName();

    /**
     * Task description.
     */
    String getDescription();

    /**
     * Assigned user (if claimed).
     */
    String getAssignee();

    /**
     * Candidate group.
     */
    String getCandidateGroup();

    /**
     * Process instance ID.
     */
    String getProcessInstanceId();

    /**
     * Creation time.
     */
    Date getCreateTime();

    /**
     * Due date (if set).
     */
    Date getDueDate();

    /**
     * Priority.
     */
    int getPriority();
}
```

---

## Implementation

### Phase 1: Native Sling BPMN Engine

Implement the BPMN API using Sling infrastructure:

#### SimpleBPMNProcessEngine

**Location**: `core/src/main/java/org/apache/sling/cms/core/internal/workflow/SimpleBPMNProcessEngine.java`

```java
package org.apache.sling.cms.core.internal.workflow;

import org.apache.sling.cms.workflow.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Simple BPMN 2.0 compliant process engine implementation using native Sling.
 *
 * This implementation provides a subset of BPMN features:
 * - Sequential flows
 * - User tasks
 * - Service tasks
 * - Exclusive gateways (simple conditions)
 *
 * For advanced features (parallel gateways, timers, sub-processes),
 * migrate to Flowable engine.
 */
@Component(service = ProcessEngine.class)
public class SimpleBPMNProcessEngine implements ProcessEngine {

    @Reference
    private BPMNParser bpmnParser;

    @Reference
    private ProcessInstanceManager processInstanceManager;

    private RuntimeService runtimeService;
    private TaskService taskService;
    private RepositoryService repositoryService;
    private HistoryService historyService;

    @Activate
    protected void activate() {
        this.runtimeService = new SimpleBPMNRuntimeService(bpmnParser, processInstanceManager);
        this.taskService = new SimpleBPMNTaskService(processInstanceManager);
        this.repositoryService = new SimpleBPMNRepositoryService(bpmnParser);
        this.historyService = new SimpleBPMNHistoryService(processInstanceManager);
    }

    @Override
    public RuntimeService getRuntimeService() {
        return runtimeService;
    }

    @Override
    public TaskService getTaskService() {
        return taskService;
    }

    @Override
    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    @Override
    public HistoryService getHistoryService() {
        return historyService;
    }
}
```

#### BPMN XML Parser

**Location**: `core/src/main/java/org/apache/sling/cms/core/internal/workflow/BPMNParser.java`

```java
package org.apache.sling.cms.core.internal.workflow;

import org.osgi.service.component.annotations.Component;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.InputStream;
import java.util.*;

/**
 * Parser for BPMN 2.0 XML files.
 *
 * Parses standard BPMN 2.0 XML and builds internal process definition.
 */
@Component(service = BPMNParser.class)
public class BPMNParser {

    private static final String BPMN_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL";

    /**
     * Parse BPMN 2.0 XML into internal process definition.
     */
    public ProcessDefinitionImpl parse(String processKey, InputStream bpmnXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(bpmnXml);

        Element definitions = doc.getDocumentElement();
        NodeList processes = definitions.getElementsByTagNameNS(BPMN_NS, "process");

        if (processes.getLength() == 0) {
            throw new IllegalArgumentException("No process found in BPMN XML");
        }

        Element processElement = (Element) processes.item(0);
        String processId = processElement.getAttribute("id");
        String processName = processElement.getAttribute("name");

        ProcessDefinitionImpl processDef = new ProcessDefinitionImpl();
        processDef.setKey(processId);
        processDef.setName(processName);

        // Parse activities (tasks, gateways, events)
        Map<String, ActivityImpl> activities = parseActivities(processElement);
        processDef.setActivities(activities);

        // Parse sequence flows
        Map<String, SequenceFlowImpl> flows = parseSequenceFlows(processElement);
        processDef.setSequenceFlows(flows);

        // Build execution graph
        buildExecutionGraph(processDef, activities, flows);

        return processDef;
    }

    private Map<String, ActivityImpl> parseActivities(Element processElement) {
        Map<String, ActivityImpl> activities = new HashMap<>();

        // Parse start events
        parseElements(processElement, "startEvent", activities, ActivityType.START_EVENT);

        // Parse end events
        parseElements(processElement, "endEvent", activities, ActivityType.END_EVENT);

        // Parse user tasks
        parseElements(processElement, "userTask", activities, ActivityType.USER_TASK);

        // Parse service tasks
        parseElements(processElement, "serviceTask", activities, ActivityType.SERVICE_TASK);

        // Parse exclusive gateways
        parseElements(processElement, "exclusiveGateway", activities, ActivityType.EXCLUSIVE_GATEWAY);

        // Parse parallel gateways (limited support)
        parseElements(processElement, "parallelGateway", activities, ActivityType.PARALLEL_GATEWAY);

        return activities;
    }

    private void parseElements(Element processElement, String tagName,
                               Map<String, ActivityImpl> activities, ActivityType type) {
        NodeList elements = processElement.getElementsByTagNameNS(BPMN_NS, tagName);
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            String id = element.getAttribute("id");
            String name = element.getAttribute("name");

            ActivityImpl activity = new ActivityImpl();
            activity.setId(id);
            activity.setName(name);
            activity.setType(type);

            // Parse task-specific attributes
            if (type == ActivityType.USER_TASK) {
                parseUserTaskAttributes(element, activity);
            } else if (type == ActivityType.SERVICE_TASK) {
                parseServiceTaskAttributes(element, activity);
            }

            activities.put(id, activity);
        }
    }

    private void parseUserTaskAttributes(Element element, ActivityImpl activity) {
        // Parse candidate groups from potentialOwner
        NodeList potentialOwners = element.getElementsByTagNameNS(BPMN_NS, "potentialOwner");
        if (potentialOwners.getLength() > 0) {
            Element potentialOwner = (Element) potentialOwners.item(0);
            NodeList expressions = potentialOwner.getElementsByTagNameNS(BPMN_NS, "formalExpression");
            if (expressions.getLength() > 0) {
                String candidateGroup = expressions.item(0).getTextContent();
                activity.setCandidateGroup(candidateGroup);
            }
        }
    }

    private void parseServiceTaskAttributes(Element element, ActivityImpl activity) {
        // Parse flowable:class attribute
        String delegateClass = element.getAttributeNS("http://flowable.org/bpmn", "class");
        if (delegateClass != null && !delegateClass.isEmpty()) {
            activity.setDelegateClass(delegateClass);
        }

        // Parse extension elements for field injection
        NodeList extensionElements = element.getElementsByTagNameNS(BPMN_NS, "extensionElements");
        if (extensionElements.getLength() > 0) {
            Element extensions = (Element) extensionElements.item(0);
            parseFieldInjections(extensions, activity);
        }
    }

    private void parseFieldInjections(Element extensions, ActivityImpl activity) {
        NodeList fields = extensions.getElementsByTagNameNS("http://flowable.org/bpmn", "field");
        Map<String, String> fieldValues = new HashMap<>();

        for (int i = 0; i < fields.getLength(); i++) {
            Element field = (Element) fields.item(i);
            String fieldName = field.getAttribute("name");
            NodeList stringValues = field.getElementsByTagNameNS("http://flowable.org/bpmn", "string");
            if (stringValues.getLength() > 0) {
                String value = stringValues.item(0).getTextContent();
                fieldValues.put(fieldName, value);
            }
        }

        activity.setFieldValues(fieldValues);
    }

    private Map<String, SequenceFlowImpl> parseSequenceFlows(Element processElement) {
        Map<String, SequenceFlowImpl> flows = new HashMap<>();

        NodeList flowElements = processElement.getElementsByTagNameNS(BPMN_NS, "sequenceFlow");
        for (int i = 0; i < flowElements.getLength(); i++) {
            Element flowElement = (Element) flowElements.item(i);
            String id = flowElement.getAttribute("id");
            String sourceRef = flowElement.getAttribute("sourceRef");
            String targetRef = flowElement.getAttribute("targetRef");
            String name = flowElement.getAttribute("name");

            SequenceFlowImpl flow = new SequenceFlowImpl();
            flow.setId(id);
            flow.setName(name);
            flow.setSourceRef(sourceRef);
            flow.setTargetRef(targetRef);

            // Parse condition expression
            NodeList conditions = flowElement.getElementsByTagNameNS(BPMN_NS, "conditionExpression");
            if (conditions.getLength() > 0) {
                String condition = conditions.item(0).getTextContent().trim();
                flow.setConditionExpression(condition);
            }

            flows.put(id, flow);
        }

        return flows;
    }

    private void buildExecutionGraph(ProcessDefinitionImpl processDef,
                                     Map<String, ActivityImpl> activities,
                                     Map<String, SequenceFlowImpl> flows) {
        // Build outgoing/incoming flows for each activity
        for (SequenceFlowImpl flow : flows.values()) {
            ActivityImpl source = activities.get(flow.getSourceRef());
            ActivityImpl target = activities.get(flow.getTargetRef());

            if (source != null) {
                source.addOutgoingFlow(flow);
            }
            if (target != null) {
                target.addIncomingFlow(flow);
            }
        }

        // Find start event
        for (ActivityImpl activity : activities.values()) {
            if (activity.getType() == ActivityType.START_EVENT) {
                processDef.setStartActivity(activity);
                break;
            }
        }
    }
}
```

#### JavaDelegate Pattern for Service Tasks

**Location**: `api/src/main/java/org/apache/sling/cms/workflow/delegate/JavaDelegate.java`

```java
package org.apache.sling.cms.workflow.delegate;

/**
 * Interface for implementing service task logic.
 *
 * Compatible with Flowable/Camunda JavaDelegate pattern.
 */
public interface JavaDelegate {

    /**
     * Execute service task logic.
     *
     * @param execution Current process execution context
     * @throws Exception if execution fails
     */
    void execute(DelegateExecution execution) throws Exception;
}
```

**Location**: `api/src/main/java/org/apache/sling/cms/workflow/delegate/DelegateExecution.java`

```java
package org.apache.sling.cms.workflow.delegate;

import java.util.Map;

/**
 * Execution context passed to JavaDelegate implementations.
 *
 * Provides access to process variables and execution metadata.
 */
public interface DelegateExecution {

    /**
     * Get process instance ID.
     */
    String getProcessInstanceId();

    /**
     * Get current activity ID.
     */
    String getCurrentActivityId();

    /**
     * Get business key.
     */
    String getBusinessKey();

    /**
     * Get process variable.
     */
    Object getVariable(String variableName);

    /**
     * Set process variable.
     */
    void setVariable(String variableName, Object value);

    /**
     * Get all process variables.
     */
    Map<String, Object> getVariables();

    /**
     * Get injected field value.
     */
    String getFieldValue(String fieldName);
}
```

#### Example JavaDelegate: Publish Content

**Location**: `core/src/main/java/org/apache/sling/cms/core/workflow/delegate/PublishContentDelegate.java`

```java
package org.apache.sling.cms.core.workflow.delegate;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.cms.PublishableResource;
import org.apache.sling.cms.publication.PublicationManager;
import org.apache.sling.cms.publication.PublicationManagerFactory;
import org.apache.sling.cms.workflow.delegate.DelegateExecution;
import org.apache.sling.cms.workflow.delegate.JavaDelegate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * BPMN Service Task delegate for publishing content.
 *
 * Process variables:
 * - contentPath (String): Path to content to publish
 * - deep (Boolean): Deep publish flag (default: true)
 */
@Component(
    service = JavaDelegate.class,
    property = {
        "delegate.class=org.apache.sling.cms.core.workflow.delegate.PublishContentDelegate"
    }
)
public class PublishContentDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PublishContentDelegate.class);

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private PublicationManagerFactory publicationManagerFactory;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String contentPath = (String) execution.getVariable("contentPath");
        Boolean deep = (Boolean) execution.getVariable("deep");

        if (contentPath == null) {
            throw new IllegalArgumentException("contentPath variable is required");
        }

        if (deep == null) {
            deep = true; // Default to deep publish
        }

        log.info("Publishing content via workflow: path={}, deep={}", contentPath, deep);

        Map<String, Object> authInfo = Collections.singletonMap(
            ResourceResolverFactory.SUBSERVICE, "workflow-publisher"
        );

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            Resource resource = resolver.getResource(contentPath);
            if (resource == null) {
                throw new IllegalArgumentException("Content not found: " + contentPath);
            }

            PublishableResource publishable = resource.adaptTo(PublishableResource.class);
            if (publishable == null) {
                throw new IllegalStateException("Resource is not publishable: " + contentPath);
            }

            PublicationManager publicationManager = publicationManagerFactory.getPublicationManager();
            publicationManager.publish(publishable);

            log.info("Content published successfully: {}", contentPath);

            // Set output variable
            execution.setVariable("publishSuccess", true);

        } catch (Exception e) {
            log.error("Failed to publish content: " + contentPath, e);
            execution.setVariable("publishSuccess", false);
            execution.setVariable("publishError", e.getMessage());
            throw e;
        }
    }
}
```

---

## Migration Path to Flowable

### Step 1: BPMN Files Remain Unchanged

Your BPMN 2.0 XML files work as-is with Flowable! No changes needed.

### Step 2: Swap ProcessEngine Implementation

**Before** (Native Sling):
```java
@Reference
private ProcessEngine processEngine; // SimpleBPMNProcessEngine
```

**After** (Flowable):
```java
@Reference
private ProcessEngine processEngine; // FlowableProcessEngineAdapter
```

### Step 3: Flowable Adapter

**Location**: `core/src/main/java/org/apache/sling/cms/core/internal/workflow/FlowableProcessEngineAdapter.java`

```java
package org.apache.sling.cms.core.internal.workflow;

import org.apache.sling.cms.workflow.*;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Adapter that wraps Flowable ProcessEngine and exposes our BPMN API.
 *
 * This allows seamless migration from SimpleBPMNProcessEngine to Flowable
 * without changing any client code.
 */
@Component(
    service = ProcessEngine.class,
    property = {
        "service.ranking:Integer=100" // Higher ranking than SimpleBPMNProcessEngine
    }
)
public class FlowableProcessEngineAdapter implements ProcessEngine {

    private org.flowable.engine.ProcessEngine flowableEngine;
    private RuntimeService runtimeService;
    private TaskService taskService;
    private RepositoryService repositoryService;
    private HistoryService historyService;

    @Activate
    protected void activate() {
        // Initialize Flowable engine
        ProcessEngineConfiguration config = new StandaloneProcessEngineConfiguration()
            .setJdbcUrl("jdbc:h2:mem:flowable")
            .setJdbcUsername("sa")
            .setJdbcPassword("")
            .setJdbcDriver("org.h2.Driver")
            .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        this.flowableEngine = config.buildProcessEngine();

        // Wrap Flowable services with our adapters
        this.runtimeService = new FlowableRuntimeServiceAdapter(flowableEngine.getRuntimeService());
        this.taskService = new FlowableTaskServiceAdapter(flowableEngine.getTaskService());
        this.repositoryService = new FlowableRepositoryServiceAdapter(flowableEngine.getRepositoryService());
        this.historyService = new FlowableHistoryServiceAdapter(flowableEngine.getHistoryService());
    }

    @Override
    public RuntimeService getRuntimeService() {
        return runtimeService;
    }

    @Override
    public TaskService getTaskService() {
        return taskService;
    }

    @Override
    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    @Override
    public HistoryService getHistoryService() {
        return historyService;
    }
}
```

### Step 4: JavaDelegates Work As-Is

Your `PublishContentDelegate` and other delegates work with Flowable without changes!

Flowable will:
1. Detect `flowable:class` attribute in BPMN XML
2. Instantiate your JavaDelegate via OSGi
3. Call `execute(DelegateExecution execution)`

The only difference: `DelegateExecution` is provided by Flowable, but your code doesn't change.

---

## Code Examples

### Example: Starting a Workflow

```java
import org.apache.sling.cms.workflow.*;
import java.util.*;

@Component
public class ContentSubmissionHandler {

    @Reference
    private ProcessEngine processEngine;

    public void submitContentForReview(String contentPath, String authorId) {
        // Prepare process variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("contentPath", contentPath);
        variables.put("authorId", authorId);
        variables.put("submittedAt", new Date());

        // Start workflow using business key (content path)
        ProcessInstance processInstance = processEngine.getRuntimeService()
            .startProcessInstanceByKey("contentApproval", contentPath, variables);

        log.info("Started approval workflow: processInstanceId={}, contentPath={}",
                 processInstance.getId(), contentPath);
    }
}
```

### Example: Completing a Review Task

```java
import org.apache.sling.cms.workflow.*;
import java.util.*;

@Component
public class ReviewHandler {

    @Reference
    private ProcessEngine processEngine;

    public void approveContent(String taskId, String reviewerId, String comment) {
        TaskService taskService = processEngine.getTaskService();

        // Set approval decision variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("reviewerId", reviewerId);
        variables.put("reviewComment", comment);
        variables.put("reviewedAt", new Date());

        // Complete the task (workflow will advance to next step)
        taskService.complete(taskId, variables);

        log.info("Task approved: taskId={}, reviewerId={}", taskId, reviewerId);
    }

    public void rejectContent(String taskId, String reviewerId, String reason) {
        TaskService taskService = processEngine.getTaskService();

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("reviewerId", reviewerId);
        variables.put("reviewComment", reason);
        variables.put("reviewedAt", new Date());

        taskService.complete(taskId, variables);

        log.info("Task rejected: taskId={}, reviewerId={}", taskId, reviewerId);
    }
}
```

### Example: Querying Tasks

```java
import org.apache.sling.cms.workflow.*;
import java.util.*;

@Component
public class WorkflowInboxService {

    @Reference
    private ProcessEngine processEngine;

    /**
     * Get all pending review tasks for a user's groups.
     */
    public List<Task> getPendingReviews(String userId, List<String> userGroups) {
        TaskService taskService = processEngine.getTaskService();
        List<Task> tasks = new ArrayList<>();

        // Query tasks by candidate groups
        for (String group : userGroups) {
            List<Task> groupTasks = taskService.getTasksByCandidateGroup(group);
            tasks.addAll(groupTasks);
        }

        return tasks;
    }

    /**
     * Get task details with content information.
     */
    public TaskDetails getTaskDetails(String taskId) {
        TaskService taskService = processEngine.getTaskService();
        RuntimeService runtimeService = processEngine.getRuntimeService();

        Task task = taskService.getTask(taskId);
        if (task == null) {
            return null;
        }

        // Get process variables
        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());

        TaskDetails details = new TaskDetails();
        details.setTask(task);
        details.setContentPath((String) variables.get("contentPath"));
        details.setAuthorId((String) variables.get("authorId"));
        details.setSubmittedAt((Date) variables.get("submittedAt"));

        return details;
    }
}
```

---

## Summary

### Benefits of BPMN 2.0 Compliance

1. ✅ **Standard-based** - Use industry-standard workflow modeling
2. ✅ **Future-proof** - Migrate to Flowable/Camunda without rewriting
3. ✅ **Visual modeling** - BPMN diagrams are self-documenting
4. ✅ **Tool support** - Use BPMN modelers (Camunda Modeler, Flowable Modeler)
5. ✅ **Portability** - BPMN files work across engines
6. ✅ **Vendor-neutral** - No lock-in to specific implementation

### Limitations of Native Sling Implementation

The SimpleBPMN engine supports:
- ✅ Sequential flows
- ✅ User tasks with candidate groups
- ✅ Service tasks with JavaDelegate
- ✅ Exclusive gateways (simple conditions)
- ✅ Start/end events
- ✅ Process variables

Does NOT support (use Flowable for these):
- ❌ Parallel gateways (concurrent execution)
- ❌ Timer events (scheduled tasks)
- ❌ Sub-processes
- ❌ Event sub-processes
- ❌ Boundary events
- ❌ Call activities
- ❌ Complex expressions (uses simple ${} evaluation)

### Migration Decision Tree

```
Do you need:
├─ Sequential approval workflows?
│  └─ Use SimpleBPMN (Phase 1)
│
├─ Parallel approvals? (multiple reviewers concurrently)
│  └─ Migrate to Flowable
│
├─ Timers/scheduling? (escalation, scheduled publish)
│  └─ Migrate to Flowable
│
├─ Complex routing? (sub-processes, event-driven)
│  └─ Migrate to Flowable
│
└─ Visual workflow designer for business users?
   └─ Migrate to Flowable
```

---

## Next Steps

1. **Define your first BPMN process** - Start with content-approval.bpmn20.xml
2. **Implement SimpleBPMN engine** - Basic sequential workflow support
3. **Create JavaDelegates** - PublishContentDelegate, NotifyAuthorDelegate
4. **Build UI components** - Task inbox, workflow status
5. **Deploy and validate** - Test with real content workflows
6. **Evaluate migration** - Monitor if you need Flowable features
7. **Migrate to Flowable** - Swap ProcessEngine implementation, zero BPMN changes

---

## Related Documentation

- [Workflow Implementation Guide](workflow-implementation.md) - Simple native workflow
- [Content Distribution and Publishing](content-distribution.md)
- [BPMN 2.0 Specification](https://www.omg.org/spec/BPMN/2.0/)
- [Flowable BPMN User Guide](https://www.flowable.com/open-source/docs/bpmn/ch07b-BPMN-Constructs)
- [Camunda BPMN Tutorial](https://camunda.com/bpmn/)
