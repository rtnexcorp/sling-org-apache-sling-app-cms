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
 * @fileoverview Centralized namespace management for Sling CMS
 * Provides a clean, organized global state management pattern
 */

/**
 * Initialize the SlingCMS namespace with proper structure
 * This prevents naming conflicts and provides a clean API surface
 * 
 * @namespace SlingCMS
 * @global
 */
export function initializeNamespace() {
  if (typeof window === 'undefined') {
    return null;
  }

  // Initialize top-level namespace
  if (!window.SlingCMS) {
    window.SlingCMS = {};
  }

  // Core utilities (set by individual modules)
  if (!window.SlingCMS.logger) {
    window.SlingCMS.logger = null; // Will be set by logger.js
  }
  
  if (!window.SlingCMS.errorHandler) {
    window.SlingCMS.errorHandler = null; // Will be set by error-handler.js
  }
  
  if (!window.SlingCMS.sanitizeHTML) {
    window.SlingCMS.sanitizeHTML = null; // Will be set by sanitize.js
  }
  
  if (!window.SlingCMS.safeSetInnerHTML) {
    window.SlingCMS.safeSetInnerHTML = null; // Will be set by sanitize.js
  }

  // Third-party dependencies
  if (!window.SlingCMS.dependencies) {
    /**
     * Third-party library references
     * @namespace SlingCMS.dependencies
     */
    window.SlingCMS.dependencies = {
      /** @type {Object} rava - Reactive element binding library */
      rava: null,
      /** @type {Function} autoComplete - Autocomplete widget library */
      autoComplete: null
    };
  }

  // Editor namespace (for CMSEditor)
  if (!window.SlingCMS.editor) {
    /**
     * Editor-specific functionality
     * @namespace SlingCMS.editor
     */
    window.SlingCMS.editor = {};
  }

  // Initialize legacy Sling.CMS namespace for backward compatibility
  if (!window.Sling) {
    window.Sling = {};
  }
  
  if (!window.Sling.CMS) {
    /**
     * Legacy Sling.CMS namespace for backward compatibility
     * @namespace Sling.CMS
     * @deprecated Use window.SlingCMS instead
     */
    window.Sling.CMS = {
      ui: {},
      utils: {},
      pathfield: null
    };
  }

  return window.SlingCMS;
}

/**
 * Register a dependency in the SlingCMS namespace
 * 
 * @param {string} name - Name of the dependency
 * @param {*} value - The dependency value
 * @returns {boolean} Success status
 * 
 * @example
 * registerDependency('rava', ravaInstance);
 */
export function registerDependency(name, value) {
  if (!window.SlingCMS?.dependencies) {
    initializeNamespace();
  }
  
  if (window.SlingCMS.dependencies) {
    window.SlingCMS.dependencies[name] = value;
    return true;
  }
  
  return false;
}

/**
 * Get a dependency from the SlingCMS namespace
 * 
 * @param {string} name - Name of the dependency
 * @returns {*} The dependency value or null if not found
 * 
 * @example
 * const rava = getDependency('rava');
 */
export function getDependency(name) {
  return window.SlingCMS?.dependencies?.[name] || null;
}

/**
 * Check if a dependency is registered
 * 
 * @param {string} name - Name of the dependency
 * @returns {boolean} Whether the dependency exists
 * 
 * @example
 * if (hasDependency('rava')) {
 *   // Use rava
 * }
 */
export function hasDependency(name) {
  return !!window.SlingCMS?.dependencies?.[name];
}

/**
 * Register a utility function in the SlingCMS namespace
 * 
 * @param {string} name - Name of the utility
 * @param {*} value - The utility value (function, object, etc.)
 * @returns {boolean} Success status
 * 
 * @example
 * registerUtility('logger', loggerInstance);
 */
export function registerUtility(name, value) {
  if (!window.SlingCMS) {
    initializeNamespace();
  }
  
  window.SlingCMS[name] = value;
  return true;
}

// Auto-initialize on import
initializeNamespace();
