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
 * AI Taxonomy Field functionality
 * Handles AI-powered taxonomy suggestions
 * Uses document-level event delegation to handle dynamically loaded dialogs
 */

(function() {
  'use strict';

  /**
   * Handles AI-powered taxonomy suggestions
   */
  async function handleAiTaxonomySuggest(event) {
    const button = event.target.closest('[data-action="suggest-taxonomy"]');
    if (!button) return;

    event.preventDefault();
    event.stopPropagation();

    const field = button.closest('.cms-ai-taxonomy-field');
    if (!field) {
      console.error('AI Taxonomy: Could not find parent field element');
      return;
    }

    const aiType = field.dataset.aiType;
    const taxonomyBase = field.dataset.taxonomyBase;
    const contentSource = field.dataset.contentSource;

    // Disable button and show loading state
    button.disabled = true;
    button.classList.add('is-loading');
    const textSpan = button.querySelector('span:not(.icon)');
    const originalText = textSpan ? textSpan.textContent : 'AI Suggest';
    if (textSpan) {
      textSpan.textContent = 'Analyzing...';
    }

    try {
      // Get content from specified source or form
      let content = '';
      if (contentSource) {
        const sourceField = document.querySelector(`[name="${contentSource}"]`);
        if (sourceField) {
          content = sourceField.value || sourceField.textContent || '';
        }
      }

      // If no content source or content is empty, get all text from form
      if (!content || content.trim() === '') {
        const formWrapper = field.closest('.form-wrapper') || field.closest('form');
        if (formWrapper) {
          const textFields = formWrapper.querySelectorAll('input[type="text"], textarea, .ProseMirror');
          const contentParts = [];
          textFields.forEach(f => {
            const fieldValue = f.value || f.textContent || f.innerHTML;
            if (fieldValue && fieldValue.trim() !== '') {
              contentParts.push(fieldValue);
            }
          });
          content = contentParts.join(' ');
        }
      }

      if (!content || content.trim() === '') {
        throw new Error('No content available for AI taxonomy suggestions. Please fill in some content first.');
      }

      // Call AI taxonomy suggestion endpoint
      const formData = new FormData();
      formData.append('type', aiType);
      formData.append('content', content);
      if (taxonomyBase) {
        formData.append('taxonomyBase', taxonomyBase);
      }

      const response = await fetch('/bin/cms/ai/suggest-taxonomy.json', {
        method: 'POST',
        body: formData
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();

      if (result.success && result.suggestions && Array.isArray(result.suggestions)) {
        const labelfield = field.querySelector('.labelfield');
        const container = labelfield.querySelector('.labelfield__container');
        const template = labelfield.querySelector('.labelfield__template');
        const inputField = labelfield.querySelector('.labelfield__field input');

        if (!template || !container) {
          throw new Error('Labelfield template or container not found');
        }

        // Get existing taxonomy paths
        const existingPaths = new Set();
        container.querySelectorAll('input[type="hidden"]').forEach(input => {
          existingPaths.add(input.value);
        });

        // Add suggested taxonomies
        let addedCount = 0;
        result.suggestions.forEach(suggestion => {
          if (!existingPaths.has(suggestion.path)) {
            const tmp = document.createElement('span');
            window.SlingCMS.safeSetInnerHTML(tmp, template.innerHTML);
            
            // Set values
            tmp.querySelector('input').value = suggestion.path;
            tmp.querySelector('.labelfield__item').title = suggestion.path;
            tmp.querySelector('.labelfield__title').textContent = suggestion.title || suggestion.path;

            // Append to container
            tmp.childNodes.forEach(node => {
              const clone = node.cloneNode(true);
              container.appendChild(clone);
            });

            existingPaths.add(suggestion.path);
            addedCount++;
          }
        });

        // Show success notification
        if (window.Sling && window.Sling.CMS && window.Sling.CMS.ui) {
          window.Sling.CMS.ui.confirmMessage(
            'AI Taxonomy Suggestions Applied',
            `Added ${addedCount} taxonomy suggestion(s) using ${result.provider || 'AI service'}`
          );
        }
      } else {
        throw new Error(result.error || 'Failed to generate taxonomy suggestions');
      }
    } catch (error) {
      console.error('AI taxonomy suggestion error:', error);
      if (window.Sling && window.Sling.CMS && window.Sling.CMS.ui) {
        window.Sling.CMS.ui.confirmMessage(
          'AI Taxonomy Suggestion Failed',
          error.message || 'An error occurred while generating taxonomy suggestions'
        );
      }
    } finally {
      // Re-enable button and restore state
      button.disabled = false;
      button.classList.remove('is-loading');
      if (textSpan) {
        textSpan.textContent = originalText;
      }
    }
  }

  /**
   * Handles caret click to show dropdown
   */
  function handleCaretClick(event) {
    const caret = event.target.closest('.cms-ai-taxonomy-field .icon.is-right');
    if (!caret) return;

    event.preventDefault();
    event.stopPropagation();

    // Find the input field in the same control group
    const control = caret.closest('.control');
    if (control) {
      const input = control.querySelector('input[list]');
      if (input) {
        // Focus the input to trigger datalist dropdown
        input.focus();
        
        // For browsers that support it, trigger the dropdown by simulating a click
        // This helps show the datalist options
        if (input.showPicker) {
          try {
            input.showPicker();
          } catch (e) {
            // Fallback: Some browsers don't support showPicker() on inputs with datalist
            // Just focusing should be enough to show suggestions when user types
          }
        }
        
        // Alternative: Simulate typing to trigger dropdown
        if (!input.showPicker) {
          const originalValue = input.value;
          input.value = '';
          input.dispatchEvent(new Event('input', { bubbles: true }));
          input.value = originalValue;
        }
      }
    }
  }

  // Initialize event listeners using delegation
  document.addEventListener('click', handleAiTaxonomySuggest);
  document.addEventListener('click', handleCaretClick);

  console.log('AI Taxonomy Field initialized');
})();
