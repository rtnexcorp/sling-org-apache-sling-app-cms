/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Task Inbox JavaScript Module
 * Handles task actions: approve, reject, claim, unclaim, delegate
 */
(function () {
  "use strict";

  const TASK_ACTION_URL = "/bin/workflow/task/action";

  /**
   * Initialize task inbox functionality
   */
  function initTaskInbox() {
    const inbox = document.querySelector(".cms-workflow-taskinbox");
    if (!inbox) return;

    // Initialize tabs
    initTabs(inbox);

    // Initialize action buttons
    initActionButtons(inbox);

    // Initialize modals
    initModals(inbox);

    // Initialize delegate type toggle
    initDelegateTypeToggle(inbox);
  }

  /**
   * Initialize tab switching
   */
  function initTabs(inbox) {
    const tabItems = inbox.querySelectorAll(".cms-tabs__item");
    const tabPanels = inbox.querySelectorAll(".cms-tabs__panel");

    tabItems.forEach((item) => {
      item.addEventListener("click", (e) => {
        e.preventDefault();

        // Remove active from all tabs
        tabItems.forEach((t) => t.classList.remove("cms-tabs__item--active"));
        tabPanels.forEach((p) => p.classList.remove("cms-tabs__panel--active"));

        // Add active to clicked tab
        item.classList.add("cms-tabs__item--active");

        // Show corresponding panel
        const tabId = item.dataset.tab;
        const panel = inbox.querySelector(`[data-panel="${tabId}"]`);
        if (panel) {
          panel.classList.add("cms-tabs__panel--active");
        }
      });
    });
  }

  /**
   * Initialize action button handlers
   */
  function initActionButtons(inbox) {
    // Approve button
    inbox.querySelectorAll('[data-action="approve-task"]').forEach((btn) => {
      btn.addEventListener("click", () => openApproveModal(btn.dataset.taskId, btn.dataset.taskName));
    });

    // Reject button
    inbox.querySelectorAll('[data-action="reject-task"]').forEach((btn) => {
      btn.addEventListener("click", () => openRejectModal(btn.dataset.taskId, btn.dataset.taskName));
    });

    // Delegate button
    inbox.querySelectorAll('[data-action="delegate-task"]').forEach((btn) => {
      btn.addEventListener("click", () => openDelegateModal(btn.dataset.taskId, btn.dataset.taskName));
    });

    // Claim button
    inbox.querySelectorAll('[data-action="claim-task"]').forEach((btn) => {
      btn.addEventListener("click", () => claimTask(btn.dataset.taskId));
    });

    // Unclaim button
    inbox.querySelectorAll('[data-action="unclaim-task"]').forEach((btn) => {
      btn.addEventListener("click", () => unclaimTask(btn.dataset.taskId));
    });

    // View task button
    inbox.querySelectorAll('[data-action="view-task"]').forEach((btn) => {
      btn.addEventListener("click", () => viewTask(btn.dataset.taskId));
    });

    // Refresh button
    inbox.querySelectorAll('[data-action="refresh-inbox"]').forEach((btn) => {
      btn.addEventListener("click", () => refreshInbox());
    });
  }

  /**
   * Initialize modal handlers
   */
  function initModals(inbox) {
    // Close modal buttons
    inbox.querySelectorAll('[data-action="close-modal"]').forEach((btn) => {
      btn.addEventListener("click", closeAllModals);
    });

    // Submit approve
    inbox.querySelectorAll('[data-action="submit-approve"]').forEach((btn) => {
      btn.addEventListener("click", submitApprove);
    });

    // Submit reject
    inbox.querySelectorAll('[data-action="submit-reject"]').forEach((btn) => {
      btn.addEventListener("click", submitReject);
    });

    // Submit delegate
    inbox.querySelectorAll('[data-action="submit-delegate"]').forEach((btn) => {
      btn.addEventListener("click", submitDelegate);
    });

    // Close modal on Escape key
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") {
        closeAllModals();
      }
    });
  }

  /**
   * Initialize delegate type radio toggle
   */
  function initDelegateTypeToggle(inbox) {
    const radios = inbox.querySelectorAll('input[name="delegateType"]');
    radios.forEach((radio) => {
      radio.addEventListener("change", () => {
        const userField = inbox.querySelector('[data-delegate-field="user"]');
        const groupField = inbox.querySelector('[data-delegate-field="group"]');

        if (radio.value === "user") {
          userField.style.display = "block";
          groupField.style.display = "none";
        } else {
          userField.style.display = "none";
          groupField.style.display = "block";
        }
      });
    });
  }

  /**
   * Open approve modal
   */
  function openApproveModal(taskId, taskName) {
    const modal = document.getElementById("approve-task-modal");
    if (!modal) return;

    modal.querySelector('[name="taskId"]').value = taskId;
    modal.querySelector('[data-field="taskName"]').textContent = taskName;
    modal.querySelector('[name="comment"]').value = "";

    modal.classList.add("cms-modal--open");
  }

  /**
   * Open reject modal
   */
  function openRejectModal(taskId, taskName) {
    const modal = document.getElementById("reject-task-modal");
    if (!modal) return;

    modal.querySelector('[name="taskId"]').value = taskId;
    modal.querySelector('[data-field="taskName"]').textContent = taskName;
    modal.querySelector('[name="comment"]').value = "";

    modal.classList.add("cms-modal--open");
  }

  /**
   * Open delegate modal
   */
  function openDelegateModal(taskId, taskName) {
    const modal = document.getElementById("delegate-task-modal");
    if (!modal) return;

    modal.querySelector('[name="taskId"]').value = taskId;
    modal.querySelector('[data-field="taskName"]').textContent = taskName;
    modal.querySelector('[name="delegateUser"]').value = "";
    modal.querySelector('[name="delegateGroup"]').value = "";

    // Reset to user selection
    modal.querySelector('input[name="delegateType"][value="user"]').checked = true;
    modal.querySelector('[data-delegate-field="user"]').style.display = "block";
    modal.querySelector('[data-delegate-field="group"]').style.display = "none";

    modal.classList.add("cms-modal--open");
  }

  /**
   * Close all modals
   */
  function closeAllModals() {
    document.querySelectorAll(".cms-modal--open").forEach((modal) => {
      modal.classList.remove("cms-modal--open");
    });
  }

  /**
   * Submit approve action
   */
  async function submitApprove() {
    const modal = document.getElementById("approve-task-modal");
    const form = modal.querySelector('[data-form="approve-task"]');
    const taskId = form.querySelector('[name="taskId"]').value;
    const comment = form.querySelector('[name="comment"]').value;

    await performTaskAction(taskId, "approve", { comment });
  }

  /**
   * Submit reject action
   */
  async function submitReject() {
    const modal = document.getElementById("reject-task-modal");
    const form = modal.querySelector('[data-form="reject-task"]');
    const taskId = form.querySelector('[name="taskId"]').value;
    const comment = form.querySelector('[name="comment"]').value;

    if (!comment.trim()) {
      showNotification("Please provide a reason for rejection", "error");
      return;
    }

    await performTaskAction(taskId, "reject", { comment });
  }

  /**
   * Submit delegate action
   */
  async function submitDelegate() {
    const modal = document.getElementById("delegate-task-modal");
    const form = modal.querySelector('[data-form="delegate-task"]');
    const taskId = form.querySelector('[name="taskId"]').value;
    const delegateType = form.querySelector('input[name="delegateType"]:checked').value;

    const params = {};
    if (delegateType === "user") {
      const user = form.querySelector('[name="delegateUser"]').value;
      if (!user.trim()) {
        showNotification("Please enter a username", "error");
        return;
      }
      params.delegateUser = user;
    } else {
      const group = form.querySelector('[name="delegateGroup"]').value;
      if (!group) {
        showNotification("Please select a group", "error");
        return;
      }
      params.delegateGroup = group;
    }

    await performTaskAction(taskId, "delegate", params);
  }

  /**
   * Claim a task
   */
  async function claimTask(taskId) {
    await performTaskAction(taskId, "claim");
  }

  /**
   * Unclaim/release a task
   */
  async function unclaimTask(taskId) {
    if (!confirm("Are you sure you want to release this task?")) {
      return;
    }
    await performTaskAction(taskId, "unclaim");
  }

  /**
   * View task details
   */
  async function viewTask(taskId) {
    const modal = document.getElementById("view-task-modal");
    if (!modal) return;

    const content = modal.querySelector('[data-content="task-details"]');
    content.innerHTML = '<div class="cms-loading"><em class="jam jam-refresh jam-spin"></em> Loading...</div>';

    modal.classList.add("cms-modal--open");

    try {
      const response = await fetch(`${TASK_ACTION_URL}?taskId=${taskId}`);
      const data = await response.json();

      if (data.error) {
        content.innerHTML = `<div class="cms-message cms-message--error">${data.error}</div>`;
        return;
      }

      // Build published content info section if available
      let publishInfoHTML = '';
      if (data.publishInfo) {
        const info = data.publishInfo;

        // Build status badge HTML
        let statusBadgeHTML = '';
        if (info.publishStatus) {
          const statusClass = info.publishStatus === 'SUCCESS' ? 'success' :
                             info.publishStatus === 'PARTIAL' ? 'warning' : 'danger';
          statusBadgeHTML = `<span class="cms-badge cms-badge--${statusClass}">${info.publishStatus}</span>`;
        }

        // Build deep publish badge
        let deepBadgeHTML = '';
        if (info.deepPublish === true) {
          deepBadgeHTML = '<span class="cms-badge cms-badge--info">Recursive</span>';
        }

        publishInfoHTML = `
          <div class="cms-detail-section">
            <h3 class="cms-detail-section__title">Published Content</h3>
            ${info.contentPath ? `
              <div class="cms-detail-row">
                <label>Content Path:</label>
                <code>${info.contentPath}</code>
              </div>
            ` : ''}
            ${info.authorUrl ? `
              <div class="cms-detail-row">
                <label>View on Author:</label>
                <a href="${info.authorUrl}" target="_blank" class="cms-link cms-link--primary">
                  <span class="icon"><em class="jam jam-external-link"></em></span>
                  ${info.authorUrl}
                </a>
              </div>
            ` : ''}
            ${info.publishedUrl ? `
              <div class="cms-detail-row">
                <label>Published URL (Renderer):</label>
                <a href="${info.publishedUrl}" target="_blank" class="cms-link">${info.publishedUrl}</a>
              </div>
            ` : ''}
            ${info.publishCount !== undefined ? `
              <div class="cms-detail-row">
                <label>Resources Published:</label>
                <span class="cms-badge cms-badge--info">${info.publishCount}</span>
              </div>
            ` : ''}
            ${info.failureCount !== undefined && info.failureCount > 0 ? `
              <div class="cms-detail-row">
                <label>Failures:</label>
                <span class="cms-badge cms-badge--danger">${info.failureCount}</span>
              </div>
            ` : ''}
            ${info.publishedTo ? `
              <div class="cms-detail-row">
                <label>Published To:</label>
                <span class="cms-badge cms-badge--secondary">${info.publishedTo}</span>
              </div>
            ` : ''}
            ${statusBadgeHTML || deepBadgeHTML ? `
              <div class="cms-detail-row">
                <label>Status:</label>
                <span>${statusBadgeHTML} ${deepBadgeHTML}</span>
              </div>
            ` : ''}
          </div>
        `;
      }

      content.innerHTML = `
        <div class="cms-task-details">
          <div class="cms-detail-row">
            <label>Task ID:</label>
            <code>${data.id}</code>
          </div>
          <div class="cms-detail-row">
            <label>Name:</label>
            <span>${data.name}</span>
          </div>
          <div class="cms-detail-row">
            <label>Description:</label>
            <span>${data.description || "-"}</span>
          </div>
          <div class="cms-detail-row">
            <label>Assignee:</label>
            <span>${data.assignee || "Unassigned"}</span>
          </div>
          <div class="cms-detail-row">
            <label>Process Instance:</label>
            <code>${data.processInstanceId}</code>
          </div>
          <div class="cms-detail-row">
            <label>Created:</label>
            <span>${data.createTime ? new Date(data.createTime).toLocaleString() : "-"}</span>
          </div>
          <div class="cms-detail-row">
            <label>Priority:</label>
            <span>${data.priority || 50}</span>
          </div>
          <div class="cms-detail-row">
            <label>Candidate Group:</label>
            <span>${data.candidateGroup || "-"}</span>
          </div>
          ${publishInfoHTML}
        </div>
      `;
    } catch (error) {
      content.innerHTML = `<div class="cms-message cms-message--error">Error loading task details: ${error.message}</div>`;
    }
  }

  /**
   * Perform a task action via API
   */
  async function performTaskAction(taskId, action, params = {}) {
    try {
      const formData = new FormData();
      formData.append("taskId", taskId);
      formData.append("action", action);

      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          formData.append(key, value);
        }
      });

      const response = await fetch(TASK_ACTION_URL, {
        method: "POST",
        body: formData,
      });

      const data = await response.json();

      if (data.success) {
        showNotification(data.message || `Task ${action} successful`, "success");
        closeAllModals();
        refreshInbox();
      } else {
        showNotification(data.error || `Failed to ${action} task`, "error");
      }
    } catch (error) {
      showNotification(`Error: ${error.message}`, "error");
    }
  }

  /**
   * Refresh the inbox by reloading the page
   */
  function refreshInbox() {
    window.location.reload();
  }

  /**
   * Show a notification message
   */
  function showNotification(message, type = "info") {
    // Check if Sling CMS notification system exists
    if (window.Sling && window.Sling.CMS && window.Sling.CMS.ui && window.Sling.CMS.ui.alert) {
      window.Sling.CMS.ui.alert(type, message);
      return;
    }

    // Fallback: Create a simple notification
    const existing = document.querySelector(".cms-notification");
    if (existing) existing.remove();

    const notification = document.createElement("div");
    notification.className = `cms-notification cms-notification--${type}`;
    notification.innerHTML = `
      <span class="cms-notification__message">${message}</span>
      <button class="cms-notification__close">&times;</button>
    `;

    document.body.appendChild(notification);

    // Add close handler
    notification.querySelector(".cms-notification__close").addEventListener("click", () => {
      notification.remove();
    });

    // Auto-remove after 5 seconds
    setTimeout(() => {
      if (notification.parentNode) {
        notification.remove();
      }
    }, 5000);
  }

  // Initialize when DOM is ready
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initTaskInbox);
  } else {
    initTaskInbox();
  }
})();
