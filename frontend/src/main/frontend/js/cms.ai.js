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
 * AI Suggestion functionality for editor fields
 * Uses document-level event delegation to handle dynamically loaded dialogs
 */

async function handleAiSuggestClick(event) {
  const button = event.target.closest('.cms-ai-suggest-btn');
  if (!button) return;
  
  event.preventDefault();
  event.stopPropagation();

  const field = button.closest('.cms-ai-suggest-field');
  if (!field) {
    console.error('AI Suggest: Could not find parent field element');
    return;
  }
  
  const input = field.querySelector('.cms-ai-suggest-input');
  const aiType = field.dataset.aiType;
  const contentSource = field.dataset.contentSource;

  // Disable button and show loading state
  button.disabled = true;
  button.classList.add('is-loading');
  const textSpan = button.querySelector('span:not(.cms-icon)');
  const originalText = textSpan ? textSpan.textContent : 'AI Suggest';
  if (textSpan) {
    textSpan.textContent = 'Generating...';
  }

  try {
    // Get content from specified source
    let content = '';
    if (contentSource) {
      const sourceField = document.querySelector(`[name="${contentSource}"]`);
      if (sourceField) {
        content = sourceField.value;
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
          if (fieldValue && fieldValue.trim() !== '' && f !== input) {
            contentParts.push(fieldValue);
          }
        });
        content = contentParts.join(' ');
      }
    }

    if (!content || content.trim() === '') {
      throw new Error('No content available for AI suggestion. Please fill in some content first.');
    }

    // Call AI suggestion endpoint
    const formData = new FormData();
    formData.append('type', aiType);
    formData.append('content', content);

    const response = await fetch('/bin/cms/ai/suggest.json', {
      method: 'POST',
      body: formData
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();

    if (result.success && result.suggestion) {
      // Fill the input with suggestion
      input.value = result.suggestion;
      input.dispatchEvent(new Event('change', { bubbles: true }));
      input.dispatchEvent(new Event('input', { bubbles: true }));

      // Show success notification
      if (window.Sling && window.Sling.CMS && window.Sling.CMS.ui) {
        window.Sling.CMS.ui.confirmMessage(
          'AI Suggestion Applied',
          `Generated ${aiType} using ${result.provider || 'AI service'}`
        );
      }
    } else {
      throw new Error(result.error || 'Failed to generate suggestion');
    }
  } catch (error) {
    console.error('AI suggestion error:', error);
    if (window.Sling && window.Sling.CMS && window.Sling.CMS.ui) {
      window.Sling.CMS.ui.confirmMessage(
        'AI Suggestion Failed',
        error.message || 'An error occurred while generating the suggestion'
      );
    } else {
      alert('AI Suggestion Failed: ' + (error.message || 'An error occurred'));
    }
  } finally {
    // Re-enable button and restore text
    button.disabled = false;
    button.classList.remove('is-loading');
    if (textSpan) {
      textSpan.textContent = originalText;
    }
  }
}

// Use document-level event delegation to handle dynamically loaded content
document.addEventListener('click', function(event) {
  if (event.target.closest('.cms-ai-suggest-btn')) {
    handleAiSuggestClick(event);
  }
});

// Also bind via rava for consistency with other CMS components
if (typeof rava !== 'undefined') {
  rava.bind('.cms-ai-suggest-field', {
    events: {
      'click .cms-ai-suggest-btn': handleAiSuggestClick
    }
  });
}
