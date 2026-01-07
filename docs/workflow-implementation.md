# SPDX-License-Identifier: Apache-2.0

# Workflow Implementation Guide

## Overview

This document provides recommendations and implementation strategies for adding workflow capabilities to Apache Sling CMS. Workflows enable content approval processes, scheduled publishing, and automated content lifecycle management.

Apache Sling CMS already has foundational workflow infrastructure through its job management and publication systems. This guide explores options for extending these capabilities with formal approval workflows.

---

## Table of Contents

1. [Current Workflow Infrastructure](#current-workflow-infrastructure)
2. [Workflow Engine Options](#workflow-engine-options)
3. [Recommended Approach](#recommended-approach)
4. [Implementation Strategy](#implementation-strategy)
5. [Architecture](#architecture)
6. [API Design](#api-design)
7. [Configuration](#configuration)
8. [UI Components](#ui-components)
9. [Future Enhancements](#future-enhancements)

---

## Current Workflow Infrastructure

Apache Sling CMS provides several foundational workflow capabilities:

### 1. Job Management System

**Location**: [api/src/main/java/org/apache/sling/cms/CMSJobManager.java](../api/src/main/java/org/apache/sling/cms/CMSJobManager.java)

The `CMSJobManager` provides:
- Job queue management via Apache Sling Event Jobs
- User-triggered background jobs via `ConfigurableJobExecutor`
- Job tracking and history
- Cluster-safe job execution

**Example**: [BulkPublicationJob](../distribution/src/main/java/org/apache/sling/cms/distribution/impl/BulkPublicationJob.java) demonstrates bulk content operations.

### 2. Publication System

**Location**: `distribution/` module

The publication system provides:
- Content distribution from Author to Publisher instances
- Publication state tracking (`PublishableResource`)
- Publication events (`PublicationEvent`)
- Post operations for publish/unpublish actions

See [Content Distribution](content-distribution.md) for details.

### 3. User Generated Content Approval

**Location**: [core/src/main/java/org/apache/sling/cms/core/usergenerated/](../core/src/main/java/org/apache/sling/cms/core/usergenerated/)

Simple approval workflow for UGC:
- `ApproveUGCOperation` - Move or publish approved content
- Content types: comments, forum posts, blog posts, contact forms

### 4. Version Control

**Location**: [core/src/main/java/org/apache/sling/cms/core/internal/operations/](../core/src/main/java/org/apache/sling/cms/core/internal/operations/)

JCR versioning support:
- `CheckpointOperation` - Create version snapshots
- `CheckoutPostOperation` - Enable editing of versioned content

### What's Missing

The following workflow features are NOT currently implemented:

- ❌ Multi-step approval workflows
- ❌ Workflow state machine (draft → review → approved → published)
- ❌ Task assignment and review queues
- ❌ Scheduled publication dates
- ❌ Workflow history and audit trails
- ❌ Conditional routing and branching
- ❌ Escalation and SLA monitoring

---

## Workflow Engine Options

### Option A: Native Sling Job/Event Infrastructure (RECOMMENDED)

**Description**: Extend existing Sling Job and Event infrastructure for simple approval workflows.

#### Pros
✅ **Zero external dependencies** - Uses existing Sling/OSGi infrastructure
✅ **OSGi-native** - Perfect integration with Sling lifecycle
✅ **Lightweight** - Minimal overhead
✅ **Proven patterns** - `BulkPublicationJob` demonstrates approach
✅ **Cluster-safe** - Sling Event Jobs handle distributed execution
✅ **Fast implementation** - Can deliver in 1-2 weeks
✅ **Simple to understand** - Standard OSGi service patterns

#### Cons
❌ **Not BPMN 2.0 compliant** - Custom workflow definitions
❌ **No visual designer** - Workflows defined in code/JSON
❌ **Limited advanced features** - Need custom implementation for parallel gateways, timers
❌ **Custom UI required** - Build workflow monitoring UI from scratch
❌ **Scalability concerns** - Complex workflows become hard to manage

#### Best For
- Simple approval workflows (submit → review → approve/reject → publish)
- Scheduled publishing
- Content automation triggers (from [new-age-cms-content-automation.md](new-age-cms-content-automation.md))
- Policy validation gates
- Small to medium workflow complexity

#### OSGi Compatibility
✅ **Excellent** - Native Sling/OSGi infrastructure

---

### Option B: Flowable Workflow Engine (RECOMMENDED FOR COMPLEX WORKFLOWS)

**Description**: Integrate [Flowable](https://flowable.com/open-source) BPMN 2.0 workflow engine.

#### Pros
✅ **BPMN 2.0 standard** - Industry standard workflow modeling
✅ **Strong OSGi support** - Native OSGi bundles with proper manifests
✅ **Apache 2.0 license** - Fully compatible with Apache Sling CMS
✅ **Active development** - Version 7.2.0 released in 2025
✅ **Visual process designer** - Drag-and-drop workflow creation
✅ **Advanced features** - Parallel gateways, timers, escalation, boundary events
✅ **Production proven** - Used in ForgeRock IDM (OSGi environment)
✅ **Lightweight** - ~5-10 MB dependency footprint
✅ **Excellent documentation** - Extensive guides and examples

#### Cons
❌ **External dependency** - Adds ~5-10 OSGi bundles
❌ **Learning curve** - Team needs BPMN 2.0 knowledge
❌ **Database required** - Flowable needs DB for process state (can use existing Jackrabbit DB)
❌ **Increased complexity** - More moving parts to maintain
❌ **Integration work** - Need to bridge Flowable with Sling APIs
❌ **No native Sling examples** - No existing Sling+Flowable integration to reference

#### Best For
- Multi-step approval processes with parallel reviews
- Complex content lifecycle management
- Cross-department workflows
- Long-running processes with timers and escalation
- Enterprise-grade workflow requirements

#### OSGi Compatibility
✅ **Excellent** - Flowable artifacts include OSGi manifest metadata

**Maven Coordinates**:
```xml
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-engine</artifactId>
    <version>7.2.0</version>
</dependency>
```

---

### Option C: Apache KIE (jBPM) - Alternative

**Description**: Integrate [Apache KIE](https://kie.apache.org/) (formerly Red Hat jBPM, now Apache incubating project).

#### Pros
✅ **Apache Foundation project** - Aligns with Sling CMS governance
✅ **Apache 2.0 license** - License compatible
✅ **OSGi support** - Via Apache Aries Blueprint
✅ **BPMN 2.0 + DMN** - Workflow + decision management
✅ **Mature engine** - Decades of development

#### Cons
❌ **OSGi documentation limited** - JBoss Fuse support ended (Feb 2025)
❌ **Incubating status** - Still transitioning to Apache Foundation
❌ **Heavier dependency** - Larger footprint than Flowable
❌ **Complex setup** - Requires Aries Blueprint configuration
❌ **Remote APIs limited** - OSGi support incomplete for remote clients

#### Best For
- Organizations with existing jBPM expertise
- Preference for Apache Foundation projects
- Need for decision management (Drools integration)

#### OSGi Compatibility
⭐⭐⭐⭐ **Good** - OSGi support via Aries Blueprint, but limited recent documentation

---

### Options NOT Recommended

| Engine | License | OSGi Support | Status | Why Not Recommended |
|--------|---------|--------------|--------|---------------------|
| **Camunda 7 CE** | Apache 2.0 | Yes (via extension) | **End of Life Oct 2025** | Already deprecated |
| **Camunda 8** | Camunda License 1.0 | Unknown | Active | **License incompatible with Apache projects** |
| **Operaton** | Apache 2.0 | Likely | New fork | Too new, unproven |
| **Activiti** | Apache 2.0 | Limited | Maintenance mode | Stale ecosystem, Spring version conflicts |
| **Apache Airflow** | Apache 2.0 | None | Active | Python-based, wrong technology stack |

---

## Recommended Approach

### Strategy: Start Simple, Evolve Later

**Phase 1: Native Sling Workflow (Weeks 1-3)** ← **START HERE**

Build simple approval workflow using existing Sling infrastructure:
- Extend `ConfigurableJobExecutor` pattern
- Add workflow state properties to content
- Create HTL components for workflow actions
- Implement post operations for workflow transitions

**Phase 2: Evaluate Complexity (Month 2-3)**

Monitor workflow requirements:
- Track requests for advanced features
- Identify limitations of simple approach
- Decision point: Do you need parallel approvals, timers, escalation?

**Phase 3: Integrate Flowable if Needed (Month 3-4)**

If complex workflows are required:
- Add Flowable bundles to Sling Feature Model
- Migrate simple workflows to BPMN definitions
- Build workflow designer integration
- Provide admin UI for process monitoring

### Why This Approach?

1. **Delivers value quickly** - Simple workflow in weeks, not months
2. **Minimizes risk** - Start with zero dependencies
3. **Validates requirements** - Real usage informs BPMN decision
4. **Preserves options** - Can always add Flowable later
5. **Follows Apache way** - Start small, grow organically

---

## Implementation Strategy

### Phase 1: Simple Approval Workflow (Native Sling)

#### 1.1 Workflow States

Define workflow states as JCR properties on content resources:

| State | Description | Transitions |
|-------|-------------|-------------|
| `DRAFT` | Initial state, editable by author | → PENDING_REVIEW |
| `PENDING_REVIEW` | Awaiting reviewer approval | → APPROVED, REJECTED |
| `APPROVED` | Approved, ready to publish | → PUBLISHED |
| `REJECTED` | Rejected, returned to author | → DRAFT |
| `PUBLISHED` | Published to renderer instances | → (end state) |

**JCR Properties**:
```
/content/mysite/mypage
  - workflowState (String) = "PENDING_REVIEW"
  - submittedAt (Date)
  - submittedBy (String)
  - reviewedAt (Date)
  - reviewedBy (String)
  - reviewComment (String)
  - scheduledPublishDate (Date) // optional
```

#### 1.2 Workflow Service

Create `WorkflowService` to manage state transitions:

**API Interface** (`api/src/main/java/org/apache/sling/cms/workflow/WorkflowService.java`):
```java
package org.apache.sling.cms.workflow;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Service for managing content workflow lifecycle.
 */
public interface WorkflowService {

    /**
     * Submit content for review.
     * Transitions: DRAFT → PENDING_REVIEW
     */
    void submitForReview(ResourceResolver resolver, String path, String comment);

    /**
     * Approve content and optionally publish.
     * Transitions: PENDING_REVIEW → APPROVED → PUBLISHED
     */
    void approve(ResourceResolver resolver, String path, String comment, boolean publish);

    /**
     * Reject content and return to author.
     * Transitions: PENDING_REVIEW → REJECTED → DRAFT
     */
    void reject(ResourceResolver resolver, String path, String comment);

    /**
     * Get current workflow state.
     */
    WorkflowState getWorkflowState(Resource resource);

    /**
     * Check if user can perform workflow action.
     */
    boolean canPerformAction(ResourceResolver resolver, String path, WorkflowAction action);
}
```

**Enum** (`api/src/main/java/org/apache/sling/cms/workflow/WorkflowState.java`):
```java
package org.apache.sling.cms.workflow;

public enum WorkflowState {
    DRAFT("Draft"),
    PENDING_REVIEW("Pending Review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    PUBLISHED("Published");

    private final String displayName;

    WorkflowState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

**Implementation** (`core/src/main/java/org/apache/sling/cms/core/internal/workflow/WorkflowServiceImpl.java`):
```java
package org.apache.sling.cms.core.internal.workflow;

import org.apache.sling.api.resource.*;
import org.apache.sling.cms.CMSJobManager;
import org.apache.sling.cms.publication.PublicationManager;
import org.apache.sling.cms.workflow.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

@Component(service = WorkflowService.class)
public class WorkflowServiceImpl implements WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowServiceImpl.class);

    @Reference
    private CMSJobManager jobManager;

    @Reference
    private PublicationManager publicationManager;

    @Override
    public void submitForReview(ResourceResolver resolver, String path, String comment) {
        try {
            Resource resource = resolver.getResource(path);
            if (resource == null) {
                throw new IllegalArgumentException("Resource not found: " + path);
            }

            ModifiableValueMap props = resource.adaptTo(ModifiableValueMap.class);
            if (props == null) {
                throw new IllegalStateException("Cannot modify resource: " + path);
            }

            // Update workflow state
            props.put("workflowState", WorkflowState.PENDING_REVIEW.name());
            props.put("submittedAt", Calendar.getInstance());
            props.put("submittedBy", resolver.getUserID());
            if (comment != null) {
                props.put("submissionComment", comment);
            }

            resolver.commit();

            log.info("Content submitted for review: {} by {}", path, resolver.getUserID());

            // TODO: Trigger notification to reviewers

        } catch (Exception e) {
            log.error("Failed to submit content for review: " + path, e);
            throw new RuntimeException("Failed to submit for review", e);
        }
    }

    @Override
    public void approve(ResourceResolver resolver, String path, String comment, boolean publish) {
        try {
            Resource resource = resolver.getResource(path);
            if (resource == null) {
                throw new IllegalArgumentException("Resource not found: " + path);
            }

            ModifiableValueMap props = resource.adaptTo(ModifiableValueMap.class);
            if (props == null) {
                throw new IllegalStateException("Cannot modify resource: " + path);
            }

            // Update workflow state
            props.put("workflowState", WorkflowState.APPROVED.name());
            props.put("approvedAt", Calendar.getInstance());
            props.put("approvedBy", resolver.getUserID());
            if (comment != null) {
                props.put("approvalComment", comment);
            }

            resolver.commit();

            log.info("Content approved: {} by {}", path, resolver.getUserID());

            // Auto-publish if requested
            if (publish) {
                publicationManager.publish(resource.adaptTo(PublishableResource.class));
                props.put("workflowState", WorkflowState.PUBLISHED.name());
                resolver.commit();
            }

            // TODO: Trigger notification to author

        } catch (Exception e) {
            log.error("Failed to approve content: " + path, e);
            throw new RuntimeException("Failed to approve", e);
        }
    }

    @Override
    public void reject(ResourceResolver resolver, String path, String comment) {
        try {
            Resource resource = resolver.getResource(path);
            if (resource == null) {
                throw new IllegalArgumentException("Resource not found: " + path);
            }

            ModifiableValueMap props = resource.adaptTo(ModifiableValueMap.class);
            if (props == null) {
                throw new IllegalStateException("Cannot modify resource: " + path);
            }

            // Update workflow state
            props.put("workflowState", WorkflowState.REJECTED.name());
            props.put("rejectedAt", Calendar.getInstance());
            props.put("rejectedBy", resolver.getUserID());
            if (comment != null) {
                props.put("rejectionComment", comment);
            }

            resolver.commit();

            log.info("Content rejected: {} by {}", path, resolver.getUserID());

            // Reset to draft after rejection
            props.put("workflowState", WorkflowState.DRAFT.name());
            resolver.commit();

            // TODO: Trigger notification to author

        } catch (Exception e) {
            log.error("Failed to reject content: " + path, e);
            throw new RuntimeException("Failed to reject", e);
        }
    }

    @Override
    public WorkflowState getWorkflowState(Resource resource) {
        ValueMap props = resource.getValueMap();
        String state = props.get("workflowState", String.class);

        if (state == null) {
            return WorkflowState.DRAFT; // Default state
        }

        try {
            return WorkflowState.valueOf(state);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid workflow state: {}", state);
            return WorkflowState.DRAFT;
        }
    }

    @Override
    public boolean canPerformAction(ResourceResolver resolver, String path, WorkflowAction action) {
        // TODO: Implement permission checks based on:
        // - User groups (authors vs reviewers)
        // - Current workflow state
        // - Resource ACLs
        return true;
    }
}
```

#### 1.3 Post Operations

Create Sling POST operations for workflow actions:

**Submit for Review** (`core/src/main/java/org/apache/sling/cms/core/internal/operations/SubmitForReviewOperation.java`):
```java
package org.apache.sling.cms.core.internal.operations;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.cms.workflow.WorkflowService;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostOperation;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;

@Component(
    service = SlingPostOperation.class,
    property = {
        SlingPostOperation.PROP_OPERATION_NAME + "=submitForReview"
    }
)
public class SubmitForReviewOperation implements SlingPostOperation {

    @Reference
    private WorkflowService workflowService;

    @Override
    public void run(SlingHttpServletRequest request, List<Modification> changes) {
        String path = request.getResource().getPath();
        String comment = request.getParameter("comment");

        workflowService.submitForReview(request.getResourceResolver(), path, comment);
        changes.add(Modification.onModified(path));
    }
}
```

**Approve** (`core/src/main/java/org/apache/sling/cms/core/internal/operations/ApproveContentOperation.java`):
```java
@Component(
    service = SlingPostOperation.class,
    property = {
        SlingPostOperation.PROP_OPERATION_NAME + "=approve"
    }
)
public class ApproveContentOperation implements SlingPostOperation {

    @Reference
    private WorkflowService workflowService;

    @Override
    public void run(SlingHttpServletRequest request, List<Modification> changes) {
        String path = request.getResource().getPath();
        String comment = request.getParameter("comment");
        boolean publish = Boolean.parseBoolean(request.getParameter("publish"));

        workflowService.approve(request.getResourceResolver(), path, comment, publish);
        changes.add(Modification.onModified(path));
    }
}
```

**Reject** (`core/src/main/java/org/apache/sling/cms/core/internal/operations/RejectContentOperation.java`):
```java
@Component(
    service = SlingPostOperation.class,
    property = {
        SlingPostOperation.PROP_OPERATION_NAME + "=reject"
    }
)
public class RejectContentOperation implements SlingPostOperation {

    @Reference
    private WorkflowService workflowService;

    @Override
    public void run(SlingHttpServletRequest request, List<Modification> changes) {
        String path = request.getResource().getPath();
        String comment = request.getParameter("comment");

        workflowService.reject(request.getResourceResolver(), path, comment);
        changes.add(Modification.onModified(path));
    }
}
```

#### 1.4 Sling Models

Create Sling Models for rendering workflow state in UI:

**WorkflowStatus Model** (`core/src/main/java/org/apache/sling/cms/core/models/WorkflowStatus.java`):
```java
package org.apache.sling.cms.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.workflow.WorkflowService;
import org.apache.sling.cms.workflow.WorkflowState;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.annotation.PostConstruct;
import java.util.Calendar;

@Model(adaptables = Resource.class)
public class WorkflowStatus {

    @Self
    private Resource resource;

    @OSGiService
    private WorkflowService workflowService;

    private WorkflowState state;

    @PostConstruct
    protected void init() {
        this.state = workflowService.getWorkflowState(resource);
    }

    public String getState() {
        return state.name();
    }

    public String getDisplayName() {
        return state.getDisplayName();
    }

    public String getSubmittedBy() {
        return resource.getValueMap().get("submittedBy", String.class);
    }

    public Calendar getSubmittedAt() {
        return resource.getValueMap().get("submittedAt", Calendar.class);
    }

    public String getApprovedBy() {
        return resource.getValueMap().get("approvedBy", String.class);
    }

    public Calendar getApprovedAt() {
        return resource.getValueMap().get("approvedAt", Calendar.class);
    }

    public boolean isPendingReview() {
        return state == WorkflowState.PENDING_REVIEW;
    }

    public boolean isDraft() {
        return state == WorkflowState.DRAFT;
    }

    public boolean isApproved() {
        return state == WorkflowState.APPROVED;
    }
}
```

#### 1.5 Scheduled Publishing

Add scheduled publication support via Sling Scheduled Jobs:

**ScheduledPublishJob** (`core/src/main/java/org/apache/sling/cms/core/internal/workflow/ScheduledPublishJob.java`):
```java
package org.apache.sling.cms.core.internal.workflow;

import org.apache.sling.api.resource.*;
import org.apache.sling.cms.publication.PublicationManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Scheduled job to publish content with future publish dates.
 */
@Component(
    service = Runnable.class,
    property = {
        "scheduler.expression=0 * * * * ?", // Run every minute
        "scheduler.concurrent=false"
    }
)
public class ScheduledPublishJob implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPublishJob.class);

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private PublicationManager publicationManager;

    @Override
    public void run() {
        Map<String, Object> authInfo = Collections.singletonMap(
            ResourceResolverFactory.SUBSERVICE, "workflow-scheduler"
        );

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {

            // Query for approved content with scheduledPublishDate <= now
            String query = "SELECT * FROM [nt:base] WHERE " +
                "[workflowState] = 'APPROVED' AND " +
                "[scheduledPublishDate] IS NOT NULL AND " +
                "[scheduledPublishDate] <= CAST('" + getCurrentTimestamp() + "' AS DATE)";

            Iterator<Resource> results = resolver.findResources(query, "JCR-SQL2");

            while (results.hasNext()) {
                Resource resource = results.next();
                try {
                    log.info("Publishing scheduled content: {}", resource.getPath());
                    publicationManager.publish(resource.adaptTo(PublishableResource.class));

                    // Update workflow state
                    ModifiableValueMap props = resource.adaptTo(ModifiableValueMap.class);
                    props.put("workflowState", "PUBLISHED");
                    props.remove("scheduledPublishDate");
                    resolver.commit();

                } catch (Exception e) {
                    log.error("Failed to publish scheduled content: " + resource.getPath(), e);
                }
            }

        } catch (Exception e) {
            log.error("Scheduled publish job failed", e);
        }
    }

    private String getCurrentTimestamp() {
        Calendar cal = Calendar.getInstance();
        return String.format("%tFT%<tT.%<tLZ", cal);
    }
}
```

**Service User Mapping** (required for scheduled job):

Add to `feature/src/main/features/workflow.json`:
```json
{
    "org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended~workflow": {
        "user.mapping": [
            "org.apache.sling.cms.core:workflow-scheduler=sling-cms-workflow"
        ]
    }
}
```

---

## Architecture

### Module Structure

Create a new `workflow` module in the project:

```
workflow/
├── pom.xml
├── bnd.bnd
├── src/main/java/org/apache/sling/cms/workflow/
│   ├── api/
│   │   ├── WorkflowService.java
│   │   ├── WorkflowState.java
│   │   ├── WorkflowAction.java
│   │   └── package-info.java
│   └── internal/
│       ├── WorkflowServiceImpl.java
│       ├── ScheduledPublishJob.java
│       ├── SubmitForReviewOperation.java
│       ├── ApproveContentOperation.java
│       └── RejectContentOperation.java
└── src/main/resources/
    └── jcr_root/
        └── libs/sling-cms/
            └── components/workflow/
                ├── status/
                │   ├── status.html
                │   └── .content.xml
                ├── actions/
                │   ├── submit.html
                │   ├── approve.html
                │   ├── reject.html
                │   └── .content.xml
                └── inbox/
                    ├── inbox.html
                    └── .content.xml
```

**OR** extend existing `core` module if you prefer not to create a new module.

### Integration Points

The workflow system integrates with existing CMS infrastructure:

1. **PublicationManager** - Trigger publication after approval
2. **CMSJobManager** - Execute workflow tasks as jobs
3. **PublishableResource** - Add workflow state to publishable content
4. **Sling Event Jobs** - Background processing for notifications, scheduled publish
5. **JCR Versioning** - Create checkpoints at workflow milestones

### Data Model

Workflow state is stored as JCR properties on content resources:

```
/content/mysite/articles/my-article
  jcr:primaryType: sling:Page
  jcr:content/
    jcr:primaryType: nt:unstructured
    jcr:title: "My Article"
    workflowState: "PENDING_REVIEW"           // Current state
    submittedAt: 2026-01-04T10:30:00.000Z    // Submission timestamp
    submittedBy: "john.doe"                   // Submitter user ID
    submissionComment: "Ready for review"     // Author's comment
    reviewedAt: 2026-01-04T14:15:00.000Z     // Review timestamp
    reviewedBy: "jane.smith"                  // Reviewer user ID
    approvalComment: "Approved with changes"  // Reviewer's comment
    scheduledPublishDate: 2026-01-05T09:00:00.000Z  // Optional scheduled date
```

---

## Configuration

### Workflow Configuration

Store workflow configuration under site context-aware config:

```
/conf/mysite/
  settings/
    workflow/
      sling:configRef: /conf/mysite/settings/workflow
      enabled: true
      requireApproval: true
      reviewerGroups: ["content-reviewers", "editors"]
      allowScheduledPublish: true
      notifyOnSubmit: true
      notifyOnApproval: true
```

**OSGi Configuration** (`core/src/main/java/org/apache/sling/cms/core/internal/workflow/WorkflowConfig.java`):
```java
package org.apache.sling.cms.core.internal.workflow;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "Apache Sling CMS - Workflow Configuration",
    description = "Configuration for content approval workflows"
)
public @interface WorkflowConfig {

    @AttributeDefinition(
        name = "Enabled",
        description = "Enable workflow for content approval"
    )
    boolean enabled() default false;

    @AttributeDefinition(
        name = "Reviewer Groups",
        description = "User groups authorized to approve content"
    )
    String[] reviewerGroups() default {"content-reviewers"};

    @AttributeDefinition(
        name = "Allow Scheduled Publish",
        description = "Allow authors to schedule future publish dates"
    )
    boolean allowScheduledPublish() default true;

    @AttributeDefinition(
        name = "Notify on Submit",
        description = "Send notification when content is submitted for review"
    )
    boolean notifyOnSubmit() default true;

    @AttributeDefinition(
        name = "Notify on Approval",
        description = "Send notification when content is approved or rejected"
    )
    boolean notifyOnApproval() default true;
}
```

---

## UI Components

### 1. Workflow Status Component

**Location**: `ui/src/main/resources/jcr_root/libs/sling-cms/components/workflow/status/status.html`

```html
<sly data-sly-use.workflow="org.apache.sling.cms.core.models.WorkflowStatus">
    <div class="cms-workflow-status cms-workflow-status--${workflow.state @ context='attribute'}">
        <span class="cms-workflow-status__label">
            Status:
        </span>
        <span class="cms-workflow-status__state">
            ${workflow.displayName}
        </span>

        <sly data-sly-test="${workflow.pendingReview}">
            <div class="cms-workflow-status__details">
                <p>Submitted by: ${workflow.submittedBy}</p>
                <p>Submitted at: ${workflow.submittedAt @ format='yyyy-MM-dd HH:mm'}</p>
            </div>
        </sly>

        <sly data-sly-test="${workflow.approved}">
            <div class="cms-workflow-status__details">
                <p>Approved by: ${workflow.approvedBy}</p>
                <p>Approved at: ${workflow.approvedAt @ format='yyyy-MM-dd HH:mm'}</p>
            </div>
        </sly>
    </div>
</sly>
```

### 2. Workflow Actions Component

**Location**: `ui/src/main/resources/jcr_root/libs/sling-cms/components/workflow/actions/actions.html`

```html
<sly data-sly-use.workflow="org.apache.sling.cms.core.models.WorkflowStatus">
    <div class="cms-workflow-actions">

        <!-- Submit for Review (visible in DRAFT state) -->
        <sly data-sly-test="${workflow.draft}">
            <button class="cms-button cms-button--primary"
                    data-action="submitForReview"
                    data-path="${resource.path @ context='attribute'}">
                Submit for Review
            </button>
        </sly>

        <!-- Approve/Reject (visible in PENDING_REVIEW state) -->
        <sly data-sly-test="${workflow.pendingReview}">
            <button class="cms-button cms-button--success"
                    data-action="approve"
                    data-path="${resource.path @ context='attribute'}">
                Approve
            </button>

            <button class="cms-button cms-button--danger"
                    data-action="reject"
                    data-path="${resource.path @ context='attribute'}">
                Reject
            </button>
        </sly>

        <!-- Publish (visible in APPROVED state) -->
        <sly data-sly-test="${workflow.approved}">
            <button class="cms-button cms-button--primary"
                    data-action="publish"
                    data-path="${resource.path @ context='attribute'}">
                Publish Now
            </button>

            <button class="cms-button cms-button--secondary"
                    data-action="schedulePublish"
                    data-path="${resource.path @ context='attribute'}">
                Schedule Publish
            </button>
        </sly>
    </div>
</sly>
```

### 3. Review Queue / Inbox Component

**Location**: `ui/src/main/resources/jcr_root/libs/sling-cms/components/workflow/inbox/inbox.html`

```html
<div class="cms-workflow-inbox">
    <h2>Content Awaiting Review</h2>

    <sly data-sly-use.query="${'org.apache.sling.cms.core.models.QueryBuilder' @
                              query='SELECT * FROM [nt:base] WHERE [workflowState] = \'PENDING_REVIEW\'',
                              limit=50}">

        <table class="cms-table">
            <thead>
                <tr>
                    <th>Title</th>
                    <th>Path</th>
                    <th>Submitted By</th>
                    <th>Submitted At</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <sly data-sly-list.item="${query.results}">
                    <tr>
                        <td>${item.valueMap['jcr:title']}</td>
                        <td>${item.path}</td>
                        <td>${item.valueMap.submittedBy}</td>
                        <td>${item.valueMap.submittedAt @ format='yyyy-MM-dd HH:mm'}</td>
                        <td>
                            <a href="/cms/page/edit.html${item.path}" class="cms-button cms-button--small">
                                Review
                            </a>
                        </td>
                    </tr>
                </sly>
            </tbody>
        </table>
    </sly>
</div>
```

### 4. Frontend Styles

**Location**: `frontend/src/main/frontend/scss/_workflow.scss`

```scss
// Workflow Status Component
.cms-workflow-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  border-radius: 4px;

  &__label {
    font-weight: 600;
    font-size: 0.875rem;
  }

  &__state {
    padding: 0.25rem 0.75rem;
    border-radius: 12px;
    font-size: 0.875rem;
    font-weight: 500;
  }

  // State-specific styling
  &--DRAFT {
    background-color: #f5f5f5;
    .cms-workflow-status__state {
      background-color: #9e9e9e;
      color: white;
    }
  }

  &--PENDING_REVIEW {
    background-color: #fff3cd;
    .cms-workflow-status__state {
      background-color: #ffc107;
      color: #000;
    }
  }

  &--APPROVED {
    background-color: #d1ecf1;
    .cms-workflow-status__state {
      background-color: #17a2b8;
      color: white;
    }
  }

  &--REJECTED {
    background-color: #f8d7da;
    .cms-workflow-status__state {
      background-color: #dc3545;
      color: white;
    }
  }

  &--PUBLISHED {
    background-color: #d4edda;
    .cms-workflow-status__state {
      background-color: #28a745;
      color: white;
    }
  }

  &__details {
    margin-top: 0.5rem;
    font-size: 0.875rem;
    color: #666;
  }
}

// Workflow Actions
.cms-workflow-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
}

// Workflow Inbox
.cms-workflow-inbox {
  padding: 1rem;

  .cms-table {
    width: 100%;
    border-collapse: collapse;
    margin-top: 1rem;

    th, td {
      padding: 0.75rem;
      text-align: left;
      border-bottom: 1px solid #ddd;
    }

    th {
      background-color: #f5f5f5;
      font-weight: 600;
    }

    tr:hover {
      background-color: #f9f9f9;
    }
  }
}
```

Import in `frontend/src/main/frontend/scss/cms.scss`:
```scss
@import 'workflow';
```

### 5. JavaScript Workflow Actions

**Location**: `frontend/src/main/frontend/js/workflow-actions.js`

```javascript
/**
 * Workflow action handlers
 */
(function() {
    'use strict';

    // Submit for Review
    document.addEventListener('click', function(e) {
        if (e.target.dataset.action === 'submitForReview') {
            const path = e.target.dataset.path;
            const comment = prompt('Add a comment (optional):');

            submitWorkflowAction(path, 'submitForReview', { comment });
        }
    });

    // Approve
    document.addEventListener('click', function(e) {
        if (e.target.dataset.action === 'approve') {
            const path = e.target.dataset.path;
            const comment = prompt('Add approval comment (optional):');
            const publish = confirm('Publish immediately after approval?');

            submitWorkflowAction(path, 'approve', { comment, publish });
        }
    });

    // Reject
    document.addEventListener('click', function(e) {
        if (e.target.dataset.action === 'reject') {
            const path = e.target.dataset.path;
            const comment = prompt('Please provide a reason for rejection:');

            if (!comment) {
                alert('A reason is required for rejection.');
                return;
            }

            submitWorkflowAction(path, 'reject', { comment });
        }
    });

    // Submit workflow action via POST
    function submitWorkflowAction(path, operation, params) {
        const formData = new FormData();
        formData.append(':operation', operation);

        Object.keys(params).forEach(key => {
            if (params[key] !== null && params[key] !== undefined) {
                formData.append(key, params[key]);
            }
        });

        fetch(path, {
            method: 'POST',
            body: formData,
            credentials: 'same-origin'
        })
        .then(response => {
            if (response.ok) {
                alert('Action completed successfully');
                window.location.reload();
            } else {
                throw new Error('Action failed: ' + response.statusText);
            }
        })
        .catch(error => {
            alert('Error: ' + error.message);
        });
    }

})();
```

---

## Future Enhancements

### Phase 2: Advanced Workflow Features (if needed)

Once simple workflow is deployed and proven valuable, consider:

1. **Parallel Approvals** - Multiple reviewers must approve
2. **Conditional Routing** - Different approval paths based on content type
3. **Escalation** - Auto-escalate if not reviewed within SLA
4. **Notifications** - Email/Slack notifications for workflow events
5. **Workflow Templates** - Define reusable workflow patterns
6. **Analytics** - Track workflow bottlenecks and efficiency

### Phase 3: Flowable Integration (if complexity requires)

If workflow requirements exceed simple state machine capabilities:

1. **Add Flowable Bundles** to Sling Feature Model:
   ```json
   {
     "bundles": [
       {
         "id": "org.flowable:flowable-engine:7.2.0",
         "start-order": 20
       },
       {
         "id": "org.flowable:flowable-engine-common:7.2.0"
       },
       {
         "id": "org.flowable:flowable-bpmn-model:7.2.0"
       }
     ]
   }
   ```

2. **Create ProcessEngine OSGi Service** wrapper
3. **Define BPMN Workflows** in `/conf/{site}/workflows/*.bpmn20.xml`
4. **Build Workflow Designer UI** for visual process creation
5. **Migrate simple workflows** to BPMN definitions

**Example BPMN Process** (Content Approval):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
  <process id="contentApproval" name="Content Approval Workflow">

    <startEvent id="start" name="Content Submitted"/>

    <userTask id="review" name="Review Content"
              flowable:candidateGroups="content-reviewers"/>

    <exclusiveGateway id="decision" name="Approved?"/>

    <serviceTask id="publish" name="Publish Content"
                 flowable:class="org.apache.sling.cms.workflow.flowable.PublishDelegate"/>

    <serviceTask id="notify" name="Notify Author"
                 flowable:class="org.apache.sling.cms.workflow.flowable.NotifyDelegate"/>

    <endEvent id="end" name="Workflow Complete"/>

    <sequenceFlow sourceRef="start" targetRef="review"/>
    <sequenceFlow sourceRef="review" targetRef="decision"/>
    <sequenceFlow sourceRef="decision" targetRef="publish">
      <conditionExpression>${approved == true}</conditionExpression>
    </sequenceFlow>
    <sequenceFlow sourceRef="decision" targetRef="notify">
      <conditionExpression>${approved == false}</conditionExpression>
    </sequenceFlow>
    <sequenceFlow sourceRef="publish" targetRef="end"/>
    <sequenceFlow sourceRef="notify" targetRef="end"/>

  </process>
</definitions>
```

---

## Summary

### Recommended Path

1. **Start Simple** - Implement Phase 1 native Sling workflow (1-3 weeks)
2. **Validate Requirements** - Deploy and gather user feedback (1-2 months)
3. **Evaluate Complexity** - Determine if BPMN engine is needed
4. **Integrate Flowable** - Only if advanced features required (3-4 weeks)

### Key Benefits

- ✅ **Quick time to value** - Simple workflow in weeks
- ✅ **Zero initial dependencies** - Uses existing Sling infrastructure
- ✅ **Extensible** - Can migrate to Flowable when needed
- ✅ **OSGi-compliant** - Follows Apache Sling best practices
- ✅ **Apache 2.0 compatible** - All recommended engines are Apache-licensed

### Decision Matrix

| Requirement | Native Sling | + Flowable |
|-------------|-------------|------------|
| Simple approval (submit → review → publish) | ✅ Perfect | ⚠️ Overkill |
| Multi-step approvals (parallel reviewers) | ⚠️ Custom code | ✅ Native support |
| Scheduled publishing | ✅ Built-in | ✅ Built-in |
| Conditional routing | ⚠️ Custom code | ✅ BPMN gateways |
| Timers and escalation | ⚠️ Custom schedulers | ✅ BPMN timers |
| Visual workflow designer | ❌ No | ✅ Yes |
| BPMN 2.0 compliance | ❌ No | ✅ Yes |

---

## Related Documentation

- [Content Distribution and Publishing](content-distribution.md)
- [Content Automation](new-age-cms-content-automation.md)
- [User Generated Content](user-generated-content.md)
- [Apache Sling Eventing and Job Handling](https://sling.apache.org/documentation/bundles/apache-sling-eventing-and-job-handling.html)
- [Flowable Documentation](https://www.flowable.com/open-source/docs)
- [Apache KIE Documentation](https://kie.apache.org/docs)

---

## Questions or Contributions

For questions about workflow implementation:
- GitHub Issues: https://github.com/apache/sling-org-apache-sling-app-cms/issues
- Apache Sling JIRA: https://issues.apache.org/jira (Component: "App CMS")
- Mailing List: dev@sling.apache.org
