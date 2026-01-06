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

import BpmnModeler from 'bpmn-js/lib/Modeler';

class WorkflowDesigner {
    constructor(containerSelector) {
        this.container = document.querySelector(containerSelector);
        if (!this.container) {
            console.error('Workflow designer container not found');
            return;
        }

        this.modeler = null;
        this.currentWorkflowKey = null;
        
        // Set default config if not provided
        this.config = window.WORKFLOW_DESIGNER_CONFIG || {
            baseUrl: '/bin/workflow/designer',
            saveUrl: '/bin/workflow/designer?operation=save',
            loadUrl: '/bin/workflow/designer?operation=load',
            deleteUrl: '/bin/workflow/designer?operation=delete',
            deployUrl: '/bin/workflow/designer?operation=deploy',
            listUrl: '/bin/workflow/designer?operation=list'
        };

        this.init();
    }

    init() {
        // Initialize BPMN modeler
        const canvas = document.getElementById('bpmn-canvas');
        if (!canvas) {
            console.error('BPMN canvas not found');
            return;
        }

        this.modeler = new BpmnModeler({
            container: canvas,
            keyboard: {
                bindTo: window
            }
        });

        // Create new diagram by default
        this.createNewDiagram();

        // Bind event listeners
        this.bindEvents();

        // Listen to selection changes for properties panel
        this.modeler.on('selection.changed', (event) => {
            this.updatePropertiesPanel(event.newSelection);
        });
    }

    createNewDiagram() {
        const bpmnXml = this.getEmptyBpmnTemplate();
        this.modeler.importXML(bpmnXml).then(() => {
            const canvas = this.modeler.get('canvas');
            canvas.zoom('fit-viewport');
            this.showStatus('New workflow diagram created', 'success');
        }).catch(err => {
            this.showStatus('Error creating new diagram: ' + err.message, 'error');
            console.error('Error creating diagram:', err);
        });
    }

    getEmptyBpmnTemplate() {
        const workflowName = document.getElementById('workflow-name')?.value || 'New Workflow';
        const workflowKey = document.getElementById('workflow-key')?.value || 'newWorkflow';

        return `<?xml version="1.0" encoding="UTF-8"?>
<bpmn2:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                   xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                   xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                   id="Definitions_1"
                   targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn2:process id="${workflowKey}" name="${workflowName}" isExecutable="true">
    <bpmn2:startEvent id="StartEvent_1"/>
  </bpmn2:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="${workflowKey}">
      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_1" bpmnElement="StartEvent_1">
        <dc:Bounds x="152" y="102" width="36" height="36"/>
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn2:definitions>`;
    }

    bindEvents() {
        // New workflow button
        document.getElementById('new-workflow-btn')?.addEventListener('click', () => {
            if (confirm('Create a new workflow? Unsaved changes will be lost.')) {
                this.currentWorkflowKey = null;
                document.getElementById('workflow-name').value = '';
                document.getElementById('workflow-key').value = '';
                this.createNewDiagram();
            }
        });

        // Save workflow button
        document.getElementById('save-workflow-btn')?.addEventListener('click', () => {
            this.saveWorkflow();
        });

        // Load workflow button
        document.getElementById('load-workflow-btn')?.addEventListener('click', () => {
            this.showLoadDialog();
        });

        // Deploy workflow button
        document.getElementById('deploy-workflow-btn')?.addEventListener('click', () => {
            this.deployWorkflow();
        });

        // Load dialog close button
        document.querySelector('#load-workflow-dialog .cms-modal__close')?.addEventListener('click', () => {
            this.hideLoadDialog();
        });

        // Load dialog overlay
        document.querySelector('#load-workflow-dialog .cms-modal__overlay')?.addEventListener('click', () => {
            this.hideLoadDialog();
        });

        // Load workflow actions
        document.querySelectorAll('.load-workflow-action').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const workflowKey = e.target.getAttribute('data-workflow-key');
                this.loadWorkflow(workflowKey);
            });
        });

        // Delete workflow actions
        document.querySelectorAll('.delete-workflow-action').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const workflowKey = e.target.getAttribute('data-workflow-key');
                if (confirm('Delete this workflow design? This cannot be undone.')) {
                    this.deleteWorkflow(workflowKey);
                }
            });
        });
    }

    async saveWorkflow() {
        const workflowName = document.getElementById('workflow-name')?.value;
        const workflowKey = document.getElementById('workflow-key')?.value;

        if (!workflowName || !workflowKey) {
            this.showStatus('Please enter workflow name and key', 'error');
            return;
        }

        try {
            const { xml } = await this.modeler.saveXML({ format: true });

            const response = await fetch(this.config.saveUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({
                    name: workflowName,
                    key: workflowKey,
                    bpmn: xml
                })
            });

            const data = await response.json();
            
            if (response.ok && data.success) {
                this.currentWorkflowKey = workflowKey;
                this.showStatus(data.message || 'Workflow saved successfully to /etc/workflow/designs', 'success');
            } else {
                this.showStatus(data.message || 'Failed to save workflow', 'error');
            }
        } catch (err) {
            this.showStatus('Error saving workflow: ' + err.message, 'error');
            console.error('Save error:', err);
        }
    }

    async loadWorkflow(workflowKey) {
        try {
            const response = await fetch(`${this.config.loadUrl}&key=${encodeURIComponent(workflowKey)}`);
            const data = await response.json();

            if (response.ok && data.success) {
                // Set workflow metadata
                document.getElementById('workflow-name').value = data.name || '';
                document.getElementById('workflow-key').value = data.key || '';
                this.currentWorkflowKey = workflowKey;

                // Import BPMN XML
                await this.modeler.importXML(data.bpmn);
                const canvas = this.modeler.get('canvas');
                canvas.zoom('fit-viewport');

                this.hideLoadDialog();
                this.showStatus(data.message || 'Workflow loaded successfully from /etc/workflow/designs', 'success');
            } else {
                this.showStatus(data.message || 'Failed to load workflow', 'error');
            }
        } catch (err) {
            this.showStatus('Error loading workflow: ' + err.message, 'error');
            console.error('Load error:', err);
        }
    }

    async deleteWorkflow(workflowKey) {
        try {
            const response = await fetch(this.config.deleteUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({
                    key: workflowKey
                })
            });

            const data = await response.json();

            if (response.ok && data.success) {
                this.showStatus(data.message || 'Workflow deleted successfully from /etc/workflow/designs', 'success');
                // Refresh the load dialog
                window.location.reload();
            } else {
                this.showStatus(data.message || 'Failed to delete workflow', 'error');
            }
        } catch (err) {
            this.showStatus('Error deleting workflow: ' + err.message, 'error');
            console.error('Delete error:', err);
        }
    }

    async deployWorkflow() {
        const workflowName = document.getElementById('workflow-name')?.value;
        const workflowKey = document.getElementById('workflow-key')?.value;

        if (!workflowName || !workflowKey) {
            this.showStatus('Please enter workflow name and key', 'error');
            return;
        }

        try {
            const { xml } = await this.modeler.saveXML({ format: true });

            const response = await fetch(this.config.deployUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({
                    name: workflowName,
                    key: workflowKey,
                    bpmn: xml
                })
            });

            const data = await response.json();

            if (response.ok && data.success) {
                this.showStatus(data.message || 'Workflow deployed successfully to /etc/workflow/definitions', 'success');
            } else {
                this.showStatus(data.message || 'Failed to deploy workflow', 'error');
            }
        } catch (err) {
            this.showStatus('Error deploying workflow: ' + err.message, 'error');
            console.error('Deploy error:', err);
        }
    }

    updatePropertiesPanel(selection) {
        const propertiesContent = document.getElementById('properties-content');
        if (!propertiesContent) return;

        if (selection.length === 0) {
            propertiesContent.innerHTML = '<p class="cms-message cms-message--info">Select an element to view its properties</p>';
            return;
        }

        const element = selection[0];
        const businessObject = element.businessObject;

        let html = '<div class="cms-properties-grid">';
        html += `<div class="cms-form-group">
                    <label class="cms-form-label">Type:</label>
                    <span class="cms-form-value">${businessObject.$type}</span>
                 </div>`;

        if (businessObject.id) {
            html += `<div class="cms-form-group">
                        <label class="cms-form-label">ID:</label>
                        <span class="cms-form-value">${businessObject.id}</span>
                     </div>`;
        }

        if (businessObject.name) {
            html += `<div class="cms-form-group">
                        <label class="cms-form-label">Name:</label>
                        <span class="cms-form-value">${businessObject.name}</span>
                     </div>`;
        }

        html += '</div>';
        propertiesContent.innerHTML = html;
    }

    showLoadDialog() {
        const dialog = document.getElementById('load-workflow-dialog');
        if (dialog) {
            dialog.style.display = 'block';
        }
    }

    hideLoadDialog() {
        const dialog = document.getElementById('load-workflow-dialog');
        if (dialog) {
            dialog.style.display = 'none';
        }
    }

    showStatus(message, type = 'info') {
        const statusDiv = document.getElementById('status-message');
        if (!statusDiv) return;

        statusDiv.className = `cms-message cms-message--${type}`;
        statusDiv.textContent = message;
        statusDiv.style.display = 'block';

        setTimeout(() => {
            statusDiv.style.display = 'none';
        }, 5000);
    }
}

// Initialize on DOM ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        if (document.querySelector('.cms-workflow-designer')) {
            new WorkflowDesigner('.cms-workflow-designer');
        }
    });
} else {
    if (document.querySelector('.cms-workflow-designer')) {
        new WorkflowDesigner('.cms-workflow-designer');
    }
}

export default WorkflowDesigner;
