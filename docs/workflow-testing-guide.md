# Workflow System Testing Guide

This guide helps business users test the Apache Sling CMS Workflow System. Follow these step-by-step instructions to verify workflow functionality.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Accessing the Workflow Dashboard](#accessing-the-workflow-dashboard)
3. [Test Scenario 1: Start and Complete a Document Review Workflow](#test-scenario-1-start-and-complete-a-document-review-workflow)
4. [Test Scenario 2: Reject a Workflow Task](#test-scenario-2-reject-a-workflow-task)
5. [Test Scenario 3: Publishing Workflow](#test-scenario-3-publishing-workflow)
6. [Test Scenario 4: Task Inbox Operations](#test-scenario-4-task-inbox-operations)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

Before testing, ensure:

- [ ] Apache Sling CMS is running (default: http://localhost:8082)
- [ ] You have admin credentials (default: admin/admin)
- [ ] Sample content exists at `/content/reference/en/index`
- [ ] Workflow definitions are deployed (documentReview, publishingWorkflow)

---

## Accessing the Workflow Dashboard

1. Open your browser and navigate to: **http://localhost:8082/cms/start.html**
2. Log in with your credentials
3. In the left sidebar, expand **CMS Configuration**
4. Click on **Workflows**
5. You should see the Workflow Dashboard with these tabs:
   - **Overview** - Statistics and quick actions
   - **Definitions** - Available workflow processes
   - **Active Instances** - Running workflows
   - **Task Inbox** - Your assigned tasks

---

## Test Scenario 1: Start and Complete a Document Review Workflow

### Objective
Start a document review workflow and approve the review task to publish content.

### Steps

#### Step 1: Start a New Workflow

1. Go to the **Workflow Dashboard**
2. Click the **Definitions** tab
3. Find **Document Review Workflow** in the list
4. Click the **Start** button (play icon) next to it
5. In the Start Workflow form:
   - **Workflow Process**: Document Review Workflow (should be pre-selected)
   - **Content Path**: Enter `/content/reference/en/index`
   - **Business Key**: Leave empty (will default to content path)
6. Click **Start Workflow**

#### Step 2: Verify Workflow Started

1. Click the **Active Instances** tab
2. You should see a new instance with:
   - **Process**: documentReview
   - **Status**: Active
   - **Current Activity**: Task_Review
3. Note the Instance ID for reference

#### Step 3: Complete the Review Task

1. Click the **Task Inbox** tab
2. Look in either:
   - **Assigned to Me** - if task is assigned to you
   - **Candidate Tasks** - if task is unassigned
3. Find the task named **"Review Document"**
4. Click the **Approve** button (checkmark icon)
5. In the approval dialog:
   - Enter a comment: "Content reviewed and approved for publishing"
   - Click **Confirm Approve**

#### Step 4: Verify Workflow Completed

1. Go to the **Active Instances** tab
2. The workflow should no longer appear (it's completed)
3. Check the **Overview** tab - completed count should increase
4. The workflow reached **End_Published** - content was published!

### Expected Results

| Step | Expected Result |
|------|-----------------|
| Start Workflow | Redirect to instances page with success message |
| Active Instances | New instance shows "Task_Review" as current activity |
| Task Inbox | "Review Document" task appears |
| Approve Task | Success message, task disappears from inbox |
| Completion | Workflow ends at "End_Published" |

---

## Test Scenario 2: Reject a Workflow Task

### Objective
Test the rejection path of the document review workflow.

### Steps

#### Step 1: Start a New Workflow

1. Go to **Definitions** tab
2. Start **Document Review Workflow** with:
   - **Content Path**: `/content/reference/en/index`

#### Step 2: Reject the Review Task

1. Go to **Task Inbox** tab
2. Find the **"Review Document"** task
3. Click the **Reject** button (X icon)
4. In the rejection dialog:
   - Enter a comment: "Content needs revisions - please fix formatting issues"
   - Click **Confirm Reject**

#### Step 3: Verify Rejection

1. The task should disappear from your inbox
2. Check **Active Instances** - workflow should show completed
3. The workflow ended at **End_Rejected** (rejection path)

### Expected Results

| Step | Expected Result |
|------|-----------------|
| Reject Task | Success message displayed |
| Workflow Status | Ends at "End_Rejected" |
| Task Inbox | Task removed from list |

---

## Test Scenario 3: Publishing Workflow

### Objective
Test the multi-step publishing workflow with editor and production approvals.

### Steps

#### Step 1: Start Publishing Workflow

1. Go to **Definitions** tab
2. Find **Publishing Workflow**
3. Click **Start** and enter:
   - **Content Path**: `/content/reference/en/index`
4. Click **Start Workflow**

#### Step 2: Editor Approval

1. Go to **Task Inbox**
2. Find **"Editor Approval"** task
3. Click **Approve**
4. Enter comment: "Editorial review complete"
5. Confirm approval

#### Step 3: Production Approval

1. Return to **Task Inbox**
2. A new task **"Production Approval"** should appear
3. Click **Approve**
4. Enter comment: "Ready for production"
5. Confirm approval

#### Step 4: Verify Completion

1. Check **Active Instances** - workflow should be completed
2. Workflow reached **EndEvent_Published**

### Expected Results

| Step | Expected Result |
|------|-----------------|
| Start | Workflow creates "Editor Approval" task |
| Editor Approve | Creates "Production Approval" task |
| Production Approve | Workflow completes, content published |

---

## Test Scenario 4: Task Inbox Operations

### Objective
Test various task inbox operations including claim, delegate, and view details.

### Steps

#### Step 4.1: Claiming a Task

1. Start a new workflow (any type)
2. Go to **Task Inbox** > **Candidate Tasks** tab
3. Find an unassigned task
4. Click **Claim** button
5. Task should move to **Assigned to Me** tab

#### Step 4.2: Releasing a Task

1. Go to **Assigned to Me** tab
2. Find a claimed task
3. Click **Release** button
4. Task should move back to **Candidate Tasks**

#### Step 4.3: Delegating a Task

1. Find a task in your inbox
2. Click **Delegate** button (arrow icon)
3. In the delegation dialog:
   - Select a user or group to delegate to
   - Enter reason: "Out of office, delegating to team"
4. Click **Confirm Delegate**

#### Step 4.4: Viewing Task Details

1. Find any task in the inbox
2. Click **View Details** button (eye icon)
3. Review task information:
   - Task name and description
   - Process instance details
   - Content path
   - Creation time
4. Close the details modal

### Expected Results

| Operation | Expected Result |
|-----------|-----------------|
| Claim | Task moves to "Assigned to Me" |
| Release | Task moves to "Candidate Tasks" |
| Delegate | Task assigned to selected user/group |
| View Details | Modal shows complete task information |

---

## Troubleshooting

### Common Issues and Solutions

#### Issue: No tasks appear in Task Inbox

**Possible Causes:**
- No active workflows with user tasks
- Tasks assigned to different user/group

**Solution:**
1. Start a new workflow to create tasks
2. Check both "Assigned to Me" and "Candidate Tasks" tabs
3. Verify you're logged in with correct user

#### Issue: "Content not found" error when approving

**Possible Causes:**
- Invalid content path provided when starting workflow

**Solution:**
1. Verify the content path exists in the CMS
2. Use browser to check: `http://localhost:8082/content/reference/en/index.html`
3. Start a new workflow with valid content path

#### Issue: Task approval fails with "delegate class" error

**Possible Causes:**
- Workflow definition not properly deployed

**Solution:**
1. Go to **Definitions** tab
2. Verify workflow definitions are listed
3. Contact administrator to redeploy workflow definitions

#### Issue: Workflow stuck at a task

**Possible Causes:**
- Task not completed properly
- System error during execution

**Solution:**
1. Check error logs (admin only)
2. Try refreshing the page
3. If task shows in inbox, try completing it again

### Getting Help

If you encounter issues not covered here:

1. Check the browser console for JavaScript errors (F12 > Console)
2. Contact your system administrator
3. Provide the following information:
   - Workflow instance ID
   - Task ID (if applicable)
   - Steps to reproduce the issue
   - Any error messages displayed

---

## Quick Reference

### Workflow Dashboard URLs

| Page | URL |
|------|-----|
| Dashboard | http://localhost:8082/cms/workflow/dashboard.html |
| Start Workflow | http://localhost:8082/cms/workflow/start.html |
| Task Inbox | http://localhost:8082/cms/workflow/dashboard.html (Task Inbox tab) |

### Available Workflows

| Workflow | Description | Tasks |
|----------|-------------|-------|
| documentReview | Single-step document review | Review Document |
| publishingWorkflow | Two-step publishing approval | Editor Approval → Production Approval |
| contentApproval | Content approval process | Review Content → Publish Content |

### Task Actions

| Action | Icon | Description |
|--------|------|-------------|
| Approve | ✓ (checkmark) | Approve and complete the task |
| Reject | ✗ (X) | Reject the task |
| Delegate | → (arrow) | Assign task to another user |
| Claim | 👤 (person) | Take ownership of unassigned task |
| Release | ↩ (return) | Release claimed task back to pool |
| View | 👁 (eye) | View task details |

---

## Test Checklist

Use this checklist to track your testing progress:

### Basic Workflow Operations
- [ ] Access Workflow Dashboard
- [ ] View workflow definitions
- [ ] Start a new workflow
- [ ] View active instances
- [ ] View task inbox

### Document Review Workflow
- [ ] Start documentReview workflow
- [ ] Approve review task
- [ ] Verify workflow completion (End_Published)
- [ ] Start new workflow and reject task
- [ ] Verify rejection path (End_Rejected)

### Publishing Workflow
- [ ] Start publishingWorkflow
- [ ] Complete Editor Approval
- [ ] Complete Production Approval
- [ ] Verify final completion

### Task Inbox Operations
- [ ] View tasks in "Assigned to Me"
- [ ] View tasks in "Candidate Tasks"
- [ ] Claim an unassigned task
- [ ] Release a claimed task
- [ ] Delegate a task
- [ ] View task details

### Edge Cases
- [ ] Start workflow with invalid content path (should fail gracefully)
- [ ] Multiple workflows on same content
- [ ] Refresh page during workflow

---

*Document Version: 1.0*  
*Last Updated: January 2026*  
*Apache Sling CMS Workflow System*
