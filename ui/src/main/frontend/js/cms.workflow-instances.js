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
 * Workflow Instance Monitor JavaScript
 * Handles view details and delete actions
 */
window.Sling = window.Sling || {};
window.Sling.CMS = window.Sling.CMS || {};
window.Sling.CMS.workflow = window.Sling.CMS.workflow || {};

window.Sling.CMS.workflow.instanceMonitor = {
  
  /**
   * Initialize the instance monitor
   */
  init() {
    this.attachEventListeners();
  },

  /**
   * Attach event listeners to action buttons
   */
  attachEventListeners() {
    const container = document.querySelector('.cms-workflow-instances');
    if (!container) return;

    // View button click - show modal with instance details
    container.addEventListener('click', async (e) => {
      const viewButton = e.target.closest('.cms-button:not([data-action])');
      if (viewButton && viewButton.textContent.trim().includes('View')) {
        e.preventDefault();
        const href = viewButton.getAttribute('href');
        const instanceId = new URL(href, window.location.origin).searchParams.get('instanceId');
        if (instanceId) {
          await this.showInstanceDetails(instanceId);
        }
      }
    });

    // Delete button click
    container.addEventListener('click', async (e) => {
      const deleteButton = e.target.closest('[data-action="delete-instance"]');
      if (deleteButton) {
        e.preventDefault();
        const instanceId = deleteButton.dataset.instanceId;
        if (instanceId) {
          await this.deleteInstance(instanceId);
        }
      }
    });

    // Refresh button
    const refreshButton = container.querySelector('[data-action="refresh"]');
    if (refreshButton) {
      refreshButton.addEventListener('click', () => {
        window.location.reload();
      });
    }
  },

  /**
   * Show instance details in a modal
   */
  async showInstanceDetails(instanceId) {
    try {
      const response = await fetch(`/bin/workflow/instance?instanceId=${encodeURIComponent(instanceId)}`);
      
      if (!response.ok) {
        throw new Error(`Failed to fetch instance details: ${response.statusText}`);
      }

      const data = await response.json();
      this.displayDetailsModal(data);
      
    } catch (error) {
      console.error('Error fetching instance details:', error);
      alert(`Failed to load instance details: ${error.message}`);
    }
  },

  /**
   * Display instance details in a modal
   */
  displayDetailsModal(data) {
    const { instance, tasks, variables } = data;
    
    const modalHtml = `
      <div class="modal is-active">
        <div class="modal-background"></div>
        <div class="modal-card">
          <header class="modal-card-head">
            <p class="modal-card-title">Workflow Instance Details</p>
            <button class="delete close-modal" aria-label="close"></button>
          </header>
          <section class="modal-card-body">
            <h3 class="title is-5">Instance Information</h3>
            <dl class="cms-details-list">
              <dt>Instance ID:</dt>
              <dd><code>${this.escapeHtml(instance.id)}</code></dd>
              
              <dt>Process Definition:</dt>
              <dd>${this.escapeHtml(instance.processDefinitionKey)}</dd>
              
              <dt>Business Key:</dt>
              <dd>${instance.businessKey ? this.escapeHtml(instance.businessKey) : '-'}</dd>
              
              <dt>Started:</dt>
              <dd>${new Date(instance.startTime).toLocaleString()}</dd>
              
              <dt>Status:</dt>
              <dd>
                <span class="cms-badge ${instance.suspended ? 'cms-badge--warning' : 'cms-badge--success'}">
                  ${instance.suspended ? 'Suspended' : 'Active'}
                </span>
              </dd>
              
              <dt>Current Activity:</dt>
              <dd>${instance.currentActivityId ? `<span class="cms-badge">${this.escapeHtml(instance.currentActivityId)}</span>` : '-'}</dd>
            </dl>

            ${tasks && tasks.length > 0 ? `
              <h3 class="title is-5" style="margin-top: 1.5rem;">Tasks</h3>
              <table class="cms-table">
                <thead>
                  <tr>
                    <th>Task ID</th>
                    <th>Name</th>
                    <th>Assignee</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  ${tasks.map(task => `
                    <tr>
                      <td><code>${this.escapeHtml(task.id)}</code></td>
                      <td>${this.escapeHtml(task.name || '-')}</td>
                      <td>${task.assignee ? this.escapeHtml(task.assignee) : '-'}</td>
                      <td>${new Date(task.createTime).toLocaleString()}</td>
                    </tr>
                  `).join('')}
                </tbody>
              </table>
            ` : '<p style="margin-top: 1rem;">No active tasks</p>'}

            ${variables && Object.keys(variables).length > 0 ? `
              <h3 class="title is-5" style="margin-top: 1.5rem;">Process Variables</h3>
              <table class="cms-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Value</th>
                  </tr>
                </thead>
                <tbody>
                  ${Object.entries(variables).map(([key, value]) => `
                    <tr>
                      <td><strong>${this.escapeHtml(key)}</strong></td>
                      <td><code>${this.escapeHtml(JSON.stringify(value))}</code></td>
                    </tr>
                  `).join('')}
                </tbody>
              </table>
            ` : '<p style="margin-top: 1rem;">No variables</p>'}
          </section>
          <footer class="modal-card-foot">
            <button class="cms-button close-modal">Close</button>
          </footer>
        </div>
      </div>
    `;

    // Remove existing modal if any
    const existingModal = document.querySelector('.modal.is-active');
    if (existingModal) {
      existingModal.remove();
    }

    // Add new modal
    document.body.insertAdjacentHTML('beforeend', modalHtml);

    // Add close handlers
    const modal = document.querySelector('.modal.is-active');
    const closeButtons = modal.querySelectorAll('.close-modal');
    const modalBackground = modal.querySelector('.modal-background');
    
    closeButtons.forEach(btn => {
      btn.addEventListener('click', () => modal.remove());
    });
    
    if (modalBackground) {
      modalBackground.addEventListener('click', () => modal.remove());
    }

    // ESC key to close
    const escHandler = (e) => {
      if (e.key === 'Escape') {
        modal.remove();
        document.removeEventListener('keydown', escHandler);
      }
    };
    document.addEventListener('keydown', escHandler);
  },

  /**
   * Delete a workflow instance
   */
  async deleteInstance(instanceId) {
    const confirmed = confirm(`Are you sure you want to delete workflow instance: ${instanceId}?`);
    if (!confirmed) return;

    try {
      const formData = new FormData();
      formData.append('instanceId', instanceId);
      formData.append('reason', 'Deleted by user from instance monitor');

      const response = await fetch('/bin/workflow/instance/delete', {
        method: 'POST',
        body: formData
      });

      const result = await response.json();

      if (result.success) {
        // Remove the row from the table
        const row = document.querySelector(`tr[data-instance-id="${instanceId}"]`);
        if (row) {
          row.style.transition = 'opacity 0.3s';
          row.style.opacity = '0';
          setTimeout(() => {
            row.remove();
            // Check if table is now empty
            const tbody = document.querySelector('.cms-table tbody');
            if (tbody && tbody.children.length === 0) {
              window.location.reload();
            }
          }, 300);
        }
        
        alert('Workflow instance deleted successfully');
      } else {
        alert(`Failed to delete instance: ${result.message}`);
      }
      
    } catch (error) {
      console.error('Error deleting instance:', error);
      alert(`Failed to delete instance: ${error.message}`);
    }
  },

  /**
   * Escape HTML to prevent XSS
   */
  escapeHtml(text) {
    if (text === null || text === undefined) return '';
    const map = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    };
    return String(text).replace(/[&<>"']/g, m => map[m]);
  }
};

// Initialize when DOM is ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    window.Sling.CMS.workflow.instanceMonitor.init();
  });
} else {
  window.Sling.CMS.workflow.instanceMonitor.init();
}
