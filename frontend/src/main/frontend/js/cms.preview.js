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
 * Preview URL generator for unpublished content.
 * Generates shareable preview URLs with time-limited tokens.
 */
(function (ns) {
  'use strict';

  ns.preview = {
    /**
     * Generates a preview URL for the given resource path
     * @param {string} resourcePath - The path to the resource
     * @param {number} timeoutSeconds - Token validity in seconds (default: 86400 = 24 hours)
     * @returns {Promise<string>} The preview URL
     */
    generatePreviewUrl: function (resourcePath, timeoutSeconds = 86400) {
      return new Promise((resolve, reject) => {
        const url = `${resourcePath}.generatePreview.json`;
        const formData = new FormData();
        formData.append('timeout', timeoutSeconds);

        fetch(url, {
          method: 'POST',
          body: formData,
          headers: {
            'Accept': 'application/json'
          }
        })
          .then(response => response.json())
          .then(data => {
            if (data.success) {
              resolve(data.previewUrl);
            } else {
              reject(new Error(data.error || 'Failed to generate preview URL'));
            }
          })
          .catch(error => {
            reject(error);
          });
      });
    },

    /**
     * Initializes preview action buttons
     */
    init: function () {
      const previewButtons = document.querySelectorAll('.cms-preview-action');

      previewButtons.forEach(button => {
        button.addEventListener('click', (event) => {
          event.preventDefault();

          const targetPath = button.dataset.targetPath;
          if (!targetPath) {
            ns.ui.showAlert('Error', 'No target path specified', 'danger');
            return;
          }

          button.disabled = true;
          const originalText = button.textContent;
          button.textContent = 'Generating...';

          ns.preview.generatePreviewUrl(targetPath)
            .then(previewUrl => {
              // Show modal with preview URL
              ns.preview.showPreviewModal(previewUrl);
            })
            .catch(error => {
              ns.ui.showAlert('Error', `Failed to generate preview URL: ${error.message}`, 'danger');
            })
            .finally(() => {
              button.disabled = false;
              button.textContent = originalText;
            });
        });
      });
    },

    /**
     * Shows a modal with the generated preview URL
     * @param {string} previewUrl - The preview URL to display
     */
    showPreviewModal: function (previewUrl) {
      const modal = document.createElement('div');
      modal.className = 'modal is-active';
      modal.innerHTML = `
        <div class="modal-background"></div>
        <div class="modal-card">
          <header class="modal-card-head">
            <p class="modal-card-title">Preview URL Generated</p>
            <button class="delete" aria-label="close"></button>
          </header>
          <section class="modal-card-body">
            <p class="mb-3">Share this URL to allow others to preview unpublished content:</p>
            <div class="field has-addons">
              <div class="control is-expanded">
                <input class="input" type="text" readonly value="${previewUrl}" id="preview-url-input">
              </div>
              <div class="control">
                <button class="button is-info" id="copy-preview-url">
                  <span class="jam jam-files"></span>
                  <span>Copy</span>
                </button>
              </div>
            </div>
            <p class="help mt-2">This preview link will expire in 24 hours.</p>
          </section>
          <footer class="modal-card-foot">
            <a href="${previewUrl}" target="_blank" class="button is-primary">
              <span class="jam jam-external-link"></span>
              <span>Open Preview</span>
            </a>
            <button class="button" id="close-preview-modal">Close</button>
          </footer>
        </div>
      `;

      document.body.appendChild(modal);

      // Copy to clipboard functionality
      const copyButton = modal.querySelector('#copy-preview-url');
      const input = modal.querySelector('#preview-url-input');

      copyButton.addEventListener('click', () => {
        input.select();
        document.execCommand('copy');
        copyButton.innerHTML = '<span class="jam jam-check"></span><span>Copied!</span>';
        setTimeout(() => {
          copyButton.innerHTML = '<span class="jam jam-files"></span><span>Copy</span>';
        }, 2000);
      });

      // Close modal functionality
      const closeButtons = modal.querySelectorAll('.delete, #close-preview-modal, .modal-background');
      closeButtons.forEach(btn => {
        btn.addEventListener('click', () => {
          modal.remove();
        });
      });
    }
  };

  // Initialize on DOM ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => ns.preview.init());
  } else {
    ns.preview.init();
  }

})(window.rava = window.rava || {});
