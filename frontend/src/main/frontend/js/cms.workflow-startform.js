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
 * Workflow Start Form Component
 * Manages dynamic variable rows and preset loading for workflow initiation
 */

const rava = window.rava;

rava.bind('[data-component="workflow-start-form"]', {
  callbacks: {
    created() {
      const form = this;
      const container = form.querySelector('#workflow-variables-container');

      if (!container) return;

      const templateRow = container.querySelector('[data-variable-row]');
      if (!templateRow) return;

      // Clone and store the template
      const template = templateRow.cloneNode(true);

      // Handle form submission to transform variables into var_ parameters
      form.addEventListener('submit', (e) => {
        e.preventDefault();

        // Get all variable name/value pairs
        const varNames = form.querySelectorAll('[name="variableName[]"]');
        const varValues = form.querySelectorAll('[name="variableValue[]"]');

        // Create FormData with transformed variables
        const formData = new FormData();

        // Add processKey and businessKey
        const processKey = form.querySelector('[name="processKey"]');
        const businessKey = form.querySelector('[name="businessKey"]');

        if (processKey && processKey.value) {
          formData.append('processKey', processKey.value);
        }

        if (businessKey && businessKey.value) {
          formData.append('businessKey', businessKey.value);
        }

        // Add variables with var_ prefix
        varNames.forEach((nameInput, index) => {
          const name = nameInput.value.trim();
          const value = varValues[index] ? varValues[index].value.trim() : '';

          if (name && value) {
            formData.append('var_' + name, value);
          }
        });

        // Submit form data
        fetch(form.action, {
          method: 'POST',
          body: formData
        })
        .then(response => {
          if (!response.ok) {
            return response.json().then(err => {
              throw new Error(err.error || 'Unknown error');
            });
          }
          return response.text();
        })
        .then(() => {
          // Redirect to instances page
          window.location.href = '/cms/workflow/instances.html';
        })
        .catch(error => {
          alert('Error starting workflow: ' + error.message);
          console.error('Workflow start error:', error);
        });
      });

      // Add variable row button
      const addButton = form.querySelector('[data-action="add-variable"]');
      if (addButton) {
        addButton.addEventListener('click', (e) => {
          e.preventDefault();
          const newRow = template.cloneNode(true);
          newRow.querySelectorAll('input').forEach((input) => {
            input.value = '';
          });
          container.appendChild(newRow);
        });
      }

      // Remove variable row (using event delegation)
      container.addEventListener('click', (e) => {
        const removeBtn = e.target.closest('[data-action="remove-variable"]');
        if (removeBtn) {
          e.preventDefault();
          const rows = container.querySelectorAll('[data-variable-row]');
          if (rows.length > 1) {
            removeBtn.closest('[data-variable-row]').remove();
          } else {
            // Just clear the values if it's the last row
            rows[0].querySelectorAll('input').forEach((input) => {
              input.value = '';
            });
          }
        }
      });

      // Preset buttons
      const presetButtons = form.querySelectorAll('[data-preset]');
      presetButtons.forEach((btn) => {
        btn.addEventListener('click', (e) => {
          e.preventDefault();
          const preset = btn.dataset.preset;
          const presets = {
            'content-approval': [
              { name: 'author', value: '' },
              { name: 'contentPath', value: '' },
              { name: 'approver', value: '' }
            ],
            'review-workflow': [
              { name: 'reviewer', value: '' },
              { name: 'dueDate', value: '' },
              { name: 'priority', value: '50' }
            ]
          };

          if (presets[preset]) {
            container.innerHTML = '';
            presets[preset].forEach((variable) => {
              const row = template.cloneNode(true);
              row.querySelector('[name="variableName[]"]').value = variable.name;
              row.querySelector('[name="variableValue[]"]').value = variable.value;
              container.appendChild(row);
            });
          }
        });
      });
    }
  }
});
