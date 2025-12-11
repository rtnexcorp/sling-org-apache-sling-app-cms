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
 * TipTap Rich Text Editor Integration for Apache Sling CMS
 * Replaces the legacy wysihtml editor with modern TipTap editor
 */

/* global rava, SlingCMS */

(function() {
  'use strict';

  // Store editor instances for cleanup
  const editorInstances = new Map();

  /**
   * Command mapping from legacy wysihtml commands to TipTap commands
   */
  const COMMAND_MAP = {
    // Basic formatting
    'bold': (editor) => editor.chain().focus().toggleBold().run(),
    'italic': (editor) => editor.chain().focus().toggleItalic().run(),
    'underline': (editor) => editor.chain().focus().toggleUnderline().run(),
    
    // Alignment
    'alignLeftStyle': (editor) => editor.chain().focus().setTextAlign('left').run(),
    'alignCenterStyle': (editor) => editor.chain().focus().setTextAlign('center').run(),
    'alignRightStyle': (editor) => editor.chain().focus().setTextAlign('right').run(),
    
    // Lists
    'insertUnorderedList': (editor) => editor.chain().focus().toggleBulletList().run(),
    'insertOrderedList': (editor) => editor.chain().focus().toggleOrderedList().run(),
    
    // Indentation (for lists)
    'indentList': (editor) => {
      if (editor.can().sinkListItem('listItem')) {
        editor.chain().focus().sinkListItem('listItem').run();
      }
    },
    'outdentList': (editor) => {
      if (editor.can().liftListItem('listItem')) {
        editor.chain().focus().liftListItem('listItem').run();
      }
    },
    
    // Links
    'createLink': (editor, href, target) => {
      if (href) {
        editor.chain().focus().extendMarkRange('link').setLink({ 
          href: href,
          target: target || null 
        }).run();
      }
    },
    'removeLink': (editor) => editor.chain().focus().unsetLink().run(),
    
    // Images
    'insertImage': (editor, src, alt) => {
      if (src) {
        editor.chain().focus().setImage({ src: src, alt: alt || '' }).run();
      }
    },
    
    // History
    'undo': (editor) => editor.chain().focus().undo().run(),
    'redo': (editor) => editor.chain().focus().redo().run(),
    
    // Format block
    'formatBlock': (editor, value) => {
      switch(value) {
        case 'p':
          editor.chain().focus().setParagraph().run();
          break;
        case 'h1':
          editor.chain().focus().toggleHeading({ level: 1 }).run();
          break;
        case 'h2':
          editor.chain().focus().toggleHeading({ level: 2 }).run();
          break;
        case 'h3':
          editor.chain().focus().toggleHeading({ level: 3 }).run();
          break;
        case 'h4':
          editor.chain().focus().toggleHeading({ level: 4 }).run();
          break;
        case 'h5':
          editor.chain().focus().toggleHeading({ level: 5 }).run();
          break;
        case 'h6':
          editor.chain().focus().toggleHeading({ level: 6 }).run();
          break;
        case 'pre':
          editor.chain().focus().toggleCodeBlock().run();
          break;
        case 'blockquote':
          editor.chain().focus().toggleBlockquote().run();
          break;
      }
    }
  };

  /**
   * Check if a command/format is active
   */
  function isActive(editor, command, value) {
    switch(command) {
      case 'bold':
        return editor.isActive('bold');
      case 'italic':
        return editor.isActive('italic');
      case 'underline':
        return editor.isActive('underline');
      case 'alignLeftStyle':
        return editor.isActive({ textAlign: 'left' });
      case 'alignCenterStyle':
        return editor.isActive({ textAlign: 'center' });
      case 'alignRightStyle':
        return editor.isActive({ textAlign: 'right' });
      case 'insertUnorderedList':
        return editor.isActive('bulletList');
      case 'insertOrderedList':
        return editor.isActive('orderedList');
      case 'createLink':
        return editor.isActive('link');
      case 'formatBlock':
        if (value === 'p') return editor.isActive('paragraph');
        if (value === 'pre') return editor.isActive('codeBlock');
        if (value === 'blockquote') return editor.isActive('blockquote');
        if (value && value.match(/^h[1-6]$/)) {
          const level = parseInt(value.charAt(1));
          return editor.isActive('heading', { level: level });
        }
        return false;
      default:
        return false;
    }
  }

  /**
   * Update toolbar button states
   * Supports both data-tiptap-* and data-wysihtml-* attributes for backward compatibility
   */
  function updateToolbarState(rteContainer, editor) {
    // Update command buttons (support both tiptap and wysihtml attributes)
    rteContainer.querySelectorAll('[data-tiptap-command], [data-wysihtml-command]').forEach(btn => {
      const command = btn.getAttribute('data-tiptap-command') || btn.getAttribute('data-wysihtml-command');
      const value = btn.getAttribute('data-tiptap-command-value') || btn.getAttribute('data-wysihtml-command-value');
      const active = isActive(editor, command, value);
      btn.classList.toggle('is-active', active);
      btn.classList.toggle('is-selected', active);
    });
  }

  /**
   * Initialize TipTap editor
   */
  function initTiptapEditor(rteContainer) {
    window.SlingCMS.logger.debug('[TipTap] Initializing editor for container:', rteContainer);
    const textarea = rteContainer.querySelector('.rte-editor');
    const toolbar = rteContainer.querySelector('.rte-toolbar');
    
    if (!textarea) {
      window.SlingCMS.logger.error('[TipTap] No textarea found with class .rte-editor');
      return null;
    }
    
    if (!window.TiptapEditor) {
      window.SlingCMS.logger.error('[TipTap] window.TiptapEditor not found - bundle not loaded');
      return null;
    }
    
    window.SlingCMS.logger.debug('[TipTap] Found textarea and TiptapEditor, creating editor...');

    // Create editor element
    const editorElement = document.createElement('div');
    editorElement.className = 'tiptap-editor-content';
    
    // Insert editor element after toolbar
    if (toolbar && toolbar.nextSibling) {
      toolbar.parentNode.insertBefore(editorElement, toolbar.nextSibling);
    } else {
      rteContainer.appendChild(editorElement);
    }

    // Get initial content from textarea
    const initialContent = textarea.value || '';

    // Create TipTap editor instance
    const editor = window.TiptapEditor.create({
      element: editorElement,
      content: initialContent,
      onUpdate: ({ editor }) => {
        // Sync content back to textarea
        textarea.value = editor.getHTML();
        // Trigger change event
        textarea.dispatchEvent(new Event('change', { bubbles: true }));
      },
      onSelectionUpdate: ({ editor }) => {
        updateToolbarState(rteContainer, editor);
      },
      onTransaction: ({ editor }) => {
        updateToolbarState(rteContainer, editor);
      }
    });

    // Hide original textarea
    textarea.style.display = 'none';

    // Store instance
    editorInstances.set(rteContainer, editor);

    // Setup toolbar button handlers
    setupToolbarHandlers(rteContainer, editor);

    // Setup dialog handlers
    setupDialogHandlers(rteContainer, editor);

    // Setup source view toggle
    setupSourceViewToggle(rteContainer, editor, textarea, editorElement);

    return editor;
  }

  /**
   * Setup toolbar button click handlers
   * Supports both data-tiptap-* and data-wysihtml-* attributes for backward compatibility
   */
  function setupToolbarHandlers(rteContainer, editor) {
    const buttons = rteContainer.querySelectorAll('[data-tiptap-command], [data-wysihtml-command]');
    window.SlingCMS.logger.debug('[TipTap] Setting up', buttons.length, 'toolbar buttons');
    
    buttons.forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        
        const command = btn.getAttribute('data-tiptap-command') || btn.getAttribute('data-wysihtml-command');
        const value = btn.getAttribute('data-tiptap-command-value') || btn.getAttribute('data-wysihtml-command-value');
        
        window.SlingCMS.logger.debug('[TipTap] Button clicked:', command, value);
        
        // Handle special dialog commands
        if (command === 'createLink') {
          showDialog(rteContainer, 'createLink');
          return;
        }
        if (command === 'insertImage') {
          showDialog(rteContainer, 'insertImage');
          return;
        }
        
        // Execute command
        const commandFn = COMMAND_MAP[command];
        if (commandFn) {
          commandFn(editor, value);
        }
        
        // Update toolbar state
        updateToolbarState(rteContainer, editor);
      });
    });
  }

  /**
   * Show a dialog
   * Supports both data-tiptap-dialog and data-wysihtml-dialog attributes
   */
  function showDialog(rteContainer, dialogName) {
    const dialog = rteContainer.querySelector(`[data-tiptap-dialog="${dialogName}"], [data-wysihtml-dialog="${dialogName}"]`);
    if (dialog) {
      dialog.style.display = 'block';
      const firstInput = dialog.querySelector('input');
      if (firstInput) {
        firstInput.focus();
      }
    }
  }

  /**
   * Hide a dialog
   * Supports both data-tiptap-dialog and data-wysihtml-dialog attributes
   */
  function hideDialog(rteContainer, dialogName) {
    const dialog = rteContainer.querySelector(`[data-tiptap-dialog="${dialogName}"], [data-wysihtml-dialog="${dialogName}"]`);
    if (dialog) {
      dialog.style.display = 'none';
      // Clear input values
      dialog.querySelectorAll('input').forEach(input => {
        input.value = '';
      });
      dialog.querySelectorAll('select').forEach(select => {
        select.selectedIndex = 0;
      });
    }
  }

  /**
   * Setup dialog handlers for links and images
   * Supports both data-tiptap-* and data-wysihtml-* attributes for backward compatibility
   */
  function setupDialogHandlers(rteContainer, editor) {
    // Create Link Dialog (support both tiptap and wysihtml attributes)
    const linkDialog = rteContainer.querySelector('[data-tiptap-dialog="createLink"], [data-wysihtml-dialog="createLink"]');
    if (linkDialog) {
      const saveBtn = linkDialog.querySelector('[data-tiptap-dialog-action="save"], [data-wysihtml-dialog-action="save"]');
      const cancelBtn = linkDialog.querySelector('[data-tiptap-dialog-action="cancel"], [data-wysihtml-dialog-action="cancel"]');
      const hrefInput = linkDialog.querySelector('[data-tiptap-dialog-field="href"], [data-wysihtml-dialog-field="href"]');
      const targetSelect = linkDialog.querySelector('[data-tiptap-dialog-field="target"], [data-wysihtml-dialog-field="target"]');
      
      if (saveBtn) {
        saveBtn.addEventListener('click', (e) => {
          e.preventDefault();
          const href = hrefInput ? hrefInput.value : '';
          const target = targetSelect ? targetSelect.value : '';
          
          if (href) {
            COMMAND_MAP.createLink(editor, href, target);
          }
          hideDialog(rteContainer, 'createLink');
        });
      }
      
      if (cancelBtn) {
        cancelBtn.addEventListener('click', (e) => {
          e.preventDefault();
          hideDialog(rteContainer, 'createLink');
        });
      }
    }

    // Insert Image Dialog (support both tiptap and wysihtml attributes)
    const imageDialog = rteContainer.querySelector('[data-tiptap-dialog="insertImage"], [data-wysihtml-dialog="insertImage"]');
    if (imageDialog) {
      const saveBtn = imageDialog.querySelector('[data-tiptap-dialog-action="save"], [data-wysihtml-dialog-action="save"]');
      const cancelBtn = imageDialog.querySelector('[data-tiptap-dialog-action="cancel"], [data-wysihtml-dialog-action="cancel"]');
      const srcInput = imageDialog.querySelector('[data-tiptap-dialog-field="src"], [data-wysihtml-dialog-field="src"]');
      const altInput = imageDialog.querySelector('[data-tiptap-dialog-field="alt"], [data-wysihtml-dialog-field="alt"]');
      
      if (saveBtn) {
        saveBtn.addEventListener('click', (e) => {
          e.preventDefault();
          const src = srcInput ? srcInput.value : '';
          const alt = altInput ? altInput.value : '';
          
          if (src) {
            COMMAND_MAP.insertImage(editor, src, alt);
          }
          hideDialog(rteContainer, 'insertImage');
        });
      }
      
      if (cancelBtn) {
        cancelBtn.addEventListener('click', (e) => {
          e.preventDefault();
          hideDialog(rteContainer, 'insertImage');
        });
      }
    }
  }

  /**
   * Setup source view toggle
   * Supports both data-tiptap-action and data-wysihtml-action attributes
   */
  function setupSourceViewToggle(rteContainer, editor, textarea, editorElement) {
    const sourceBtn = rteContainer.querySelector('[data-tiptap-action="change_view"], [data-wysihtml-action="change_view"]');
    let isSourceView = false;
    
    if (sourceBtn) {
      sourceBtn.addEventListener('click', (e) => {
        e.preventDefault();
        isSourceView = !isSourceView;
        
        if (isSourceView) {
          // Switch to source view
          textarea.value = editor.getHTML();
          textarea.style.display = 'block';
          editorElement.style.display = 'none';
          sourceBtn.classList.add('is-active');
        } else {
          // Switch to WYSIWYG view
          editor.commands.setContent(textarea.value);
          textarea.style.display = 'none';
          editorElement.style.display = 'block';
          sourceBtn.classList.remove('is-active');
        }
      });
    }
  }

  /**
   * Destroy editor instance
   */
  function destroyEditor(rteContainer) {
    const editor = editorInstances.get(rteContainer);
    if (editor) {
      editor.destroy();
      editorInstances.delete(rteContainer);
    }
  }

  // Bind to .rte elements using rava
  window.SlingCMS.logger.debug('[TipTap] Binding to .rte elements with rava');
  
  // Check if rava is available
  if (typeof rava === 'undefined') {
    window.SlingCMS.logger.error('[TipTap] rava is not defined! Cannot bind RTE elements.');
    return;
  }
  
  rava.bind('.rte', {
    callbacks: {
      created() {
        window.SlingCMS.logger.debug('[TipTap] Rava callback: created');
        initTiptapEditor(this);
      },
      removed() {
        window.SlingCMS.logger.debug('[TipTap] Rava callback: removed');
        destroyEditor(this);
      }
    }
  });
  
  // Also initialize any .rte elements that already exist in the DOM
  document.querySelectorAll('.rte').forEach(rteContainer => {
    if (!editorInstances.has(rteContainer)) {
      window.SlingCMS.logger.debug('[TipTap] Initializing existing .rte element');
      initTiptapEditor(rteContainer);
    }
  });

  // Export for potential external use
  window.SlingCMS = window.SlingCMS || {};
  window.SlingCMS.RTE = {
    init: initTiptapEditor,
    destroy: destroyEditor,
    getEditor: (container) => editorInstances.get(container),
    COMMAND_MAP: COMMAND_MAP
  };

})();
