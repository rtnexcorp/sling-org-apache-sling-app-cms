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
  
  // Default configuration allows common HTML elements
  const defaultConfig = {
    ALLOWED_TAGS: [
      'div', 'span', 'p', 'a', 'button', 'input', 'label', 'form',
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'ul', 'ol', 'li', 'table', 'tr', 'td', 'th', 'thead', 'tbody',
      'br', 'hr', 'img', 'strong', 'em', 'b', 'i', 'u',
      'select', 'option', 'textarea', 'fieldset', 'legend',
      'section', 'article', 'header', 'footer', 'nav', 'aside',
      'time', 'code', 'pre', 'blockquote'
    ],
    ALLOWED_ATTR: [
      'class', 'id', 'href', 'title', 'alt', 'src', 'type', 'name', 'value',
      'placeholder', 'required', 'disabled', 'readonly', 'checked', 'selected',
      'data-*', 'aria-*', 'role', 'tabindex', 'style', 'target', 'rel',
      'width', 'height', 'colspan', 'rowspan'
    ],
    ALLOW_DATA_ATTR: true,
    ALLOW_ARIA_ATTR: true,
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

// Make it available globally for backward compatibility
window.SlingCMS = window.SlingCMS || {};
window.SlingCMS.sanitizeHTML = sanitizeHTML;
window.SlingCMS.safeSetInnerHTML = safeSetInnerHTML;
