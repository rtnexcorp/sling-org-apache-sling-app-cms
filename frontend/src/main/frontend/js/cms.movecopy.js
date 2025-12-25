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
 * Move/Copy Operations Module
 *
 * Handles move and copy operations for pages and assets including:
 * - Form-based move/copy with destination path validation
 * - Drag-and-drop support for moving/copying items to folders
 * - Sling POST servlet compliance (ensures :dest ends with /)
 */

window.Sling = window.Sling || {};
window.Sling.CMS = window.Sling.CMS || {};
window.Sling.CMS.MoveCopy = {

  /**
   * Validates and normalizes destination path for move/copy operations
   * Ensures path ends with '/' for Sling POST servlet compliance
   *
   * @param {string} destPath - The destination path
   * @returns {string} Normalized destination path with trailing slash
   */
  normalizeDestinationPath(destPath) {
    if (!destPath) {
      return destPath;
    }

    const trimmed = destPath.trim();
    if (trimmed && !trimmed.endsWith('/')) {
      return trimmed + '/';
    }
    return trimmed;
  },

  /**
   * Validates move/copy form and fixes destination path
   * Called before form submission to ensure Sling POST servlet requirements
   *
   * @param {HTMLFormElement} form - The form element
   * @param {FormData} formData - The form data to be submitted
   * @returns {boolean} True if validation passed and fixes applied
   */
  validateAndFixForm(form, formData) {
    const operationInput = form.querySelector('input[name=":operation"], select[name=":operation"]');
    const destInput = form.querySelector('input[name=":dest"]');

    if (!operationInput || !destInput) {
      return true; // Not a move/copy form
    }

    const operation = operationInput.value;
    if (operation !== 'move' && operation !== 'copy') {
      return true; // Not a move/copy operation
    }

    // Normalize destination path
    const normalizedDest = this.normalizeDestinationPath(destInput.value);
    if (normalizedDest !== destInput.value) {
      destInput.value = normalizedDest;
      formData.set(':dest', normalizedDest);
    }

    return true;
  },

  /**
   * Performs a move or copy operation via Sling POST servlet
   *
   * @param {string} sourcePath - Source resource path
   * @param {string} destPath - Destination parent path (will be normalized)
   * @param {string} operation - Either 'move' or 'copy'
   * @param {boolean} updateReferences - Whether to update references
   * @returns {Promise<Response>} The fetch response
   */
  async performOperation(sourcePath, destPath, operation, updateReferences = false) {
    const normalizedDest = this.normalizeDestinationPath(destPath);

    const formData = new FormData();
    formData.set(':operation', operation);
    formData.set(':dest', normalizedDest);
    if (updateReferences) {
      formData.set(':updateReferences', 'true');
    }

    const response = await fetch(sourcePath, {
      method: 'POST',
      body: formData,
      cache: 'no-cache',
      headers: {
        Accept: 'application/json',
      },
    });

    return response;
  },

  /**
   * Drag-and-drop support - Future implementation
   * This structure provides hooks for adding drag-and-drop functionality
   */
  dragDrop: {
    /**
     * Makes an element draggable for move/copy operations
     *
     * @param {HTMLElement} element - The element to make draggable
     * @param {string} resourcePath - The resource path this element represents
     */
    makeDraggable(element, resourcePath) {
      element.setAttribute('draggable', 'true');
      element.dataset.resourcePath = resourcePath;

      element.addEventListener('dragstart', (e) => {
        e.dataTransfer.effectAllowed = 'copyMove';
        e.dataTransfer.setData('text/plain', resourcePath);
        e.dataTransfer.setData('application/x-sling-cms-path', resourcePath);
        element.classList.add('is-dragging');
      });

      element.addEventListener('dragend', () => {
        element.classList.remove('is-dragging');
      });
    },

    /**
     * Makes an element a drop target for move/copy operations
     *
     * @param {HTMLElement} element - The drop target element
     * @param {string} targetPath - The target folder path
     * @param {Function} onDrop - Callback when item is dropped (sourcePath, targetPath, operation)
     */
    makeDropTarget(element, targetPath, onDrop) {
      element.addEventListener('dragover', (e) => {
        e.preventDefault();
        e.dataTransfer.dropEffect = e.ctrlKey || e.metaKey ? 'copy' : 'move';
        element.classList.add('is-drop-target');
      });

      element.addEventListener('dragleave', () => {
        element.classList.remove('is-drop-target');
      });

      element.addEventListener('drop', async (e) => {
        e.preventDefault();
        element.classList.remove('is-drop-target');

        const sourcePath = e.dataTransfer.getData('application/x-sling-cms-path')
                        || e.dataTransfer.getData('text/plain');

        if (!sourcePath) {
          return;
        }

        // Determine operation based on modifier keys
        const operation = (e.ctrlKey || e.metaKey) ? 'copy' : 'move';

        if (onDrop) {
          await onDrop(sourcePath, targetPath, operation);
        }
      });
    },

    /**
     * Initializes drag-and-drop for content table rows
     * Can be called to enable drag-and-drop in content browsers
     */
    initializeContentTable() {
      // Future implementation: Initialize drag-and-drop for content tables
      // This would make rows draggable and folders/pages drop targets
      window.SlingCMS.logger.info('Drag-and-drop initialization placeholder - to be implemented');
    },
  },
};

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
  module.exports = window.Sling.CMS.MoveCopy;
}
