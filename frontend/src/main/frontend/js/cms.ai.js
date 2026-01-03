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
 * Supports text, textarea, richtext, and select fields
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

  // Determine field type
  const isRichtext = field.classList.contains('cms-ai-suggest-field--richtext');
  const isSelect = field.classList.contains('cms-ai-suggest-field--select');

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
      // Fill the input with suggestion based on field type
      if (isRichtext) {
        // For richtext, check if ProseMirror editor exists
        const prosemirror = field.querySelector('.ProseMirror');
        if (prosemirror && prosemirror.pmView) {
          // Update ProseMirror editor if available
          const { state, dispatch } = prosemirror.pmView;
          const transaction = state.tr.insertText(result.suggestion, 0, state.doc.content.size);
          dispatch(transaction);
        } else {
          // Fallback to textarea
          input.value = result.suggestion;
        }
      } else if (isSelect) {
        // For select, find matching option or set first option
        const option = Array.from(input.options).find(opt =>
          opt.value.toLowerCase() === result.suggestion.toLowerCase() ||
          opt.text.toLowerCase().includes(result.suggestion.toLowerCase())
        );
        if (option) {
          input.value = option.value;
        }
      } else {
        // For text and textarea fields
        input.value = result.suggestion;
      }

      // Trigger change events
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

/**
 * AI Image Suggestion functionality for image alt-text, captions, and descriptions
 * Handles suggestions for images in the asset metadata editor
 */

async function handleAiImageSuggestClick(event) {
  const button = event.target.closest('.ai-suggest-button');
  if (!button) return;

  event.preventDefault();
  event.stopPropagation();

  const aiType = button.dataset.aiType;
  const targetInputId = button.dataset.aiTarget;
  const imagePath = button.dataset.imagePath;

  if (!targetInputId || !imagePath) {
    console.error('AI Image Suggest: Missing target input or image path');
    return;
  }

  const targetInput = document.getElementById(targetInputId);
  if (!targetInput) {
    console.error('AI Image Suggest: Target input not found:', targetInputId);
    return;
  }

  // Disable button and show loading state
  button.disabled = true;
  button.classList.add('is-loading');
  const textSpan = button.querySelector('.ai-button-text');
  const originalText = textSpan ? textSpan.textContent : 'AI';
  if (textSpan) {
    textSpan.textContent = 'Loading...';
  }

  try {
    // Call AI image suggestion endpoint
    const formData = new FormData();
    formData.append('type', aiType);
    formData.append('imagePath', imagePath);

    const response = await fetch('/bin/cms/ai/suggest-image.json', {
      method: 'POST',
      body: formData
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();

    if (result.success && result.suggestion) {
      // Fill the target input with the suggestion
      targetInput.value = result.suggestion;

      // Trigger change events
      targetInput.dispatchEvent(new Event('change', { bubbles: true }));
      targetInput.dispatchEvent(new Event('input', { bubbles: true }));

      // Show success notification
      if (window.Sling && window.Sling.CMS && window.Sling.CMS.ui) {
        window.Sling.CMS.ui.confirmMessage(
          'AI Suggestion Applied',
          `Generated ${aiType} using ${result.provider || 'AI service'}`
        );
      }
    } else {
      // Handle skipped/disabled AI service
      if (result.skipped) {
        throw new Error('AI image service is not enabled or configured. Please configure an AI provider.');
      }
      throw new Error(result.error || 'Failed to generate suggestion');
    }
  } catch (error) {
    console.error('AI image suggestion error:', error);
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

// Handle AI image suggestion clicks via document-level event delegation
document.addEventListener('click', function(event) {
  if (event.target.closest('.ai-suggest-button')) {
    handleAiImageSuggestClick(event);
  }
});

// Also bind via rava for consistency with other CMS components
if (typeof rava !== 'undefined') {
  rava.bind('.ai-suggest-button', {
    events: {
      'click': handleAiImageSuggestClick
    }
  });
}
