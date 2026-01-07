/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Workflow Instance Monitor Component
 * Manages viewing workflow instance details and deleting instances
 */

const { logger, fetchWithErrorHandling } = window.SlingCMS || {};
const rava = window.rava;

// Fallback fetch function if fetchWithErrorHandling is not available
const safeFetch = fetchWithErrorHandling || fetch;

rava.bind('.cms-workflow-instances', {
  callbacks: {
    created() {
      const container = this;

      // Helper function: Apply table filters
      const applyFilters = () => {
        const processTypeFilter = container.querySelector('#filter-process-type')?.value || '';
        const statusFilter = container.querySelector('#filter-status')?.value || '';
        
        const rows = container.querySelectorAll('.cms-table tbody tr');
        
        rows.forEach(row => {
          const processDefinition = row.querySelector('td:nth-child(2)')?.textContent || '';
          const status = row.dataset.status || '';
          
          const matchesProcess = !processTypeFilter || processDefinition.includes(processTypeFilter);
          const matchesStatus = !statusFilter || status === statusFilter;
          
          if (matchesProcess && matchesStatus) {
            row.style.display = '';
          } else {
            row.style.display = 'none';
          }
        });

        // Update count
        const visibleRows = Array.from(rows).filter(row => row.style.display !== 'none');
        const countElement = container.querySelector('.cms-pagination__info');
        if (countElement) {
          countElement.textContent = `Showing ${visibleRows.length} instances`;
        }
      };

      // Helper function: Build HTML for instance details
      const buildInstanceDetailsHTML = (instance, variables, tasks) => {
        let html = `
          <div class="cms-workflow-instance-details">
            <table class="cms-table">
              <tbody>
                <tr>
                  <th>Instance ID</th>
                  <td><code>${instance.id || 'N/A'}</code></td>
                </tr>
                <tr>
                  <th>Process Definition</th>
                  <td>${instance.processDefinitionKey || 'N/A'}</td>
                </tr>
                <tr>
                  <th>Business Key</th>
                  <td>${instance.businessKey || '-'}</td>
                </tr>
                <tr>
                  <th>Status</th>
                  <td>
                    <span class="cms-badge ${instance.suspended ? 'cms-badge--warning' : 'cms-badge--success'}">
                      ${instance.suspended ? 'Suspended' : 'Active'}
                    </span>
                  </td>
                </tr>
                <tr>
                  <th>Started</th>
                  <td>${instance.startTime ? new Date(instance.startTime).toLocaleString() : 'N/A'}</td>
                </tr>
                <tr>
                  <th>Current Activity</th>
                  <td>${instance.currentActivityId ? `<span class="cms-badge">${instance.currentActivityId}</span>` : '-'}</td>
                </tr>`;
        
        // Add tasks if available
        if (tasks && tasks.length > 0) {
          html += `
                <tr>
                  <th>Tasks</th>
                  <td>
                    <ul style="margin: 0; padding-left: 1.5rem;">`;
          tasks.forEach(task => {
            html += `<li>${task.name || task.id} - ${task.assignee || 'Unassigned'}</li>`;
          });
          html += `
                    </ul>
                  </td>
                </tr>`;
        }
        
        // Add variables if available
        if (variables && Object.keys(variables).length > 0) {
          html += `
                <tr>
                  <th>Variables</th>
                  <td>
                    <pre style="background: #f5f5f5; padding: 0.5rem; border-radius: 4px; overflow: auto; max-height: 300px;">${JSON.stringify(variables, null, 2)}</pre>
                  </td>
                </tr>`;
        }
        
        html += `
              </tbody>
            </table>
          </div>
        `;
        
        return html;
      };

      // Helper function: Show modal dialog
      const showModal = (title, content) => {
        // Remove existing modal if any
        const existingModal = document.querySelector('.cms-workflow-modal');
        if (existingModal) {
          existingModal.remove();
        }

        // Create modal
        const modal = document.createElement('div');
        modal.className = 'cms-workflow-modal';
        modal.innerHTML = `
          <div class="cms-workflow-modal__overlay"></div>
          <div class="cms-workflow-modal__content">
            <div class="cms-workflow-modal__header">
              <h3 class="cms-workflow-modal__title">${title}</h3>
              <button class="cms-workflow-modal__close" aria-label="Close">&times;</button>
            </div>
            <div class="cms-workflow-modal__body">
              ${content}
            </div>
          </div>
        `;

        document.body.appendChild(modal);

        // Handle close
        const closeBtn = modal.querySelector('.cms-workflow-modal__close');
        const overlay = modal.querySelector('.cms-workflow-modal__overlay');
        
        const closeModal = () => modal.remove();
        
        closeBtn.addEventListener('click', closeModal);
        overlay.addEventListener('click', closeModal);

        // Close on Escape key
        const handleEscape = (e) => {
          if (e.key === 'Escape') {
            closeModal();
            document.removeEventListener('keydown', handleEscape);
          }
        };
        document.addEventListener('keydown', handleEscape);
      };

      // Helper function: View workflow instance details
      const viewInstance = async (instanceId) => {
        try {
          // Show loading state
          showModal('Loading...', '<p>Loading instance details...</p>');

          // Fetch instance details
          const response = await safeFetch(`/bin/workflow/instance?instanceId=${instanceId}`);
          
          if (!response.ok) {
            throw new Error(`Failed to load instance: ${response.statusText}`);
          }

          const data = await response.json();
          
          // Build modal content - data has structure: { instance: {...}, tasks: [...], variables: {...} }
          const content = buildInstanceDetailsHTML(data.instance, data.variables, data.tasks);
          
          // Show modal with instance details
          showModal(`Instance: ${instanceId}`, content);

        } catch (error) {
          logger?.error('Failed to load instance details:', error);
          showModal('Error', `<p class="cms-message cms-message--danger">Failed to load instance details: ${error.message}</p>`);
        }
      };

      // Helper function: Delete workflow instance
      const deleteInstance = async (instanceId, button) => {
        // Confirm deletion
        if (!confirm(`Are you sure you want to delete instance ${instanceId}?`)) {
          return;
        }

        try {
          // Disable button during deletion
          button.disabled = true;
          button.textContent = 'Deleting...';

          // Send delete request - using FormData for POST
          const formData = new FormData();
          formData.append('instanceId', instanceId);
          formData.append('reason', 'Deleted via instance monitor');

          const response = await safeFetch(`/bin/workflow/instance/delete`, {
            method: 'POST',
            body: formData
          });

          if (!response.ok) {
            const errorData = await response.json().catch(() => ({ error: response.statusText }));
            throw new Error(errorData.error || 'Failed to delete instance');
          }

          // Remove the row from table
          const row = button.closest('tr');
          if (row) {
            row.style.transition = 'opacity 0.3s';
            row.style.opacity = '0';
            setTimeout(() => {
              row.remove();
              // Update count
              applyFilters();
            }, 300);
          }

          logger?.info(`Successfully deleted instance: ${instanceId}`);

        } catch (error) {
          logger?.error('Failed to delete instance:', error);
          alert(`Failed to delete instance: ${error.message}`);
          
          // Re-enable button
          button.disabled = false;
          button.textContent = 'Delete';
        }
      };

      // Handle refresh button
      const refreshBtn = container.querySelector('[data-action="refresh"]');
      if (refreshBtn) {
        refreshBtn.addEventListener('click', () => {
          window.location.reload();
        });
      }

      // Handle filter changes
      const filters = container.querySelectorAll('[data-filter]');
      filters.forEach(filter => {
        filter.addEventListener('change', () => {
          applyFilters();
        });
      });

      // Handle delete instance buttons
      const deleteButtons = container.querySelectorAll('[data-action="delete-instance"]');
      deleteButtons.forEach(button => {
        button.addEventListener('click', (e) => {
          e.preventDefault();
          const instanceId = button.dataset.instanceId;
          deleteInstance(instanceId, button);
        });
      });

      // Handle view instance links with modal
      const viewLinks = container.querySelectorAll('a[href^="/bin/workflow/instance"]');
      viewLinks.forEach(link => {
        link.addEventListener('click', (e) => {
          e.preventDefault();
          const instanceId = new URLSearchParams(link.search).get('instanceId');
          viewInstance(instanceId);
        });
      });
    }
  }
});
