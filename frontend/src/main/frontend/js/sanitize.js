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

import DOMPurify from 'dompurify';

/**
 * Sanitize HTML to prevent XSS attacks
 * @param {string} dirty - The HTML string to sanitize
 * @param {Object} config - Optional DOMPurify configuration
 * @returns {string} - Sanitized HTML
 */
export function sanitizeHTML(dirty, config = {}) {
  if (!dirty || typeof dirty !== 'string') {
    return '';
  }
  
  // For trusted internal CMS content, use permissive settings
  // This allows all standard HTML elements needed for dialogs and forms
  const defaultConfig = {
    ADD_TAGS: ['iframe', 'sly', 'template'],
    ADD_ATTR: ['target', 'data-*', 'aria-*', 'for', 'action', 'method', 'enctype', 'autocomplete', 'maxlength', 'minlength', 'pattern', 'min', 'max', 'step', 'multiple', 'accept', 'cols', 'rows', 'wrap', 'size', 'list', 'form', 'formaction', 'formenctype', 'formmethod', 'formnovalidate', 'formtarget', 'dirname', 'spellcheck', 'contenteditable', 'draggable', 'dropzone', 'hidden', 'lang', 'translate', 'accesskey', 'autofocus', 'nonce', 'slot', 'part', 'exportparts', 'inputmode', 'is', 'itemid', 'itemprop', 'itemref', 'itemscope', 'itemtype', 'enterkeyhint', 'loading', 'decoding', 'fetchpriority', 'referrerpolicy', 'sandbox', 'allow', 'allowfullscreen', 'frameborder', 'scrolling', 'srcdoc', 'xmlns', 'xml:lang', 'xml:base', 'onload', 'onerror'],
    ALLOW_DATA_ATTR: true,
    ALLOW_ARIA_ATTR: true,
    WHOLE_DOCUMENT: false,
    RETURN_DOM: false,
    RETURN_DOM_FRAGMENT: false,
    ...config
  };
  
  return DOMPurify.sanitize(dirty, defaultConfig);
}

/**
 * Safely set innerHTML with sanitization
 * @param {HTMLElement} element - The element to set innerHTML on
 * @param {string} html - The HTML string to set
 * @param {Object} config - Optional DOMPurify configuration
 */
export function safeSetInnerHTML(element, html, config = {}) {
  if (!element || !(element instanceof HTMLElement)) {
    console.error('safeSetInnerHTML: Invalid element provided');
    return;
  }
  
  element.innerHTML = sanitizeHTML(html, config);
}

/**
 * Set innerHTML without sanitization - USE ONLY FOR TRUSTED INTERNAL CONTENT
 * This should only be used for content loaded from the CMS server itself
 * @param {HTMLElement} element - The element to set innerHTML on
 * @param {string} html - The trusted HTML string to set
 */
export function trustedSetInnerHTML(element, html) {
  if (!element || !(element instanceof HTMLElement)) {
    console.error('trustedSetInnerHTML: Invalid element provided');
    return;
  }
  
  element.innerHTML = html;
}

// Make it available globally for backward compatibility
window.SlingCMS = window.SlingCMS || {};
window.SlingCMS.sanitizeHTML = sanitizeHTML;
window.SlingCMS.safeSetInnerHTML = safeSetInnerHTML;
window.SlingCMS.trustedSetInnerHTML = trustedSetInnerHTML;
