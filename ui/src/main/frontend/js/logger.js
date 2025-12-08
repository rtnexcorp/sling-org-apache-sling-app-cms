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
 * Centralized logging utility with debug mode control
 * To enable debug mode: localStorage.setItem('slingcms:debug', 'true')
 * To disable debug mode: localStorage.removeItem('slingcms:debug')
 */

const DEBUG_KEY = 'slingcms:debug';

/**
 * Check if debug mode is enabled
 * @returns {boolean}
 */
function isDebugEnabled() {
  try {
    return localStorage.getItem(DEBUG_KEY) === 'true';
  } catch (e) {
    return false;
  }
}

/**
 * Logger instance with debug mode support
 */
export const logger = {
  /**
   * Log debug messages (only in debug mode)
   * @param {...any} args - Arguments to log
   */
  debug(...args) {
    if (isDebugEnabled()) {
      console.log('[DEBUG]', ...args);
    }
  },

  /**
   * Log info messages (only in debug mode)
   * @param {...any} args - Arguments to log
   */
  info(...args) {
    if (isDebugEnabled()) {
      console.info('[INFO]', ...args);
    }
  },

  /**
   * Log warning messages (always shown)
   * @param {...any} args - Arguments to log
   */
  warn(...args) {
    console.warn('[WARN]', ...args);
  },

  /**
   * Log error messages (always shown)
   * @param {...any} args - Arguments to log
   */
  error(...args) {
    console.error('[ERROR]', ...args);
  },

  /**
   * Enable debug mode
   */
  enableDebug() {
    try {
      localStorage.setItem(DEBUG_KEY, 'true');
      console.log('Debug mode enabled. Reload the page to see debug logs.');
    } catch (e) {
      console.error('Failed to enable debug mode:', e);
    }
  },

  /**
   * Disable debug mode
   */
  disableDebug() {
    try {
      localStorage.removeItem(DEBUG_KEY);
      console.log('Debug mode disabled.');
    } catch (e) {
      console.error('Failed to disable debug mode:', e);
    }
  },

  /**
   * Check if debug is enabled
   * @returns {boolean}
   */
  isDebugEnabled
};

// Make logger available globally for console access
if (typeof window !== 'undefined') {
  if (!window.SlingCMS) {
    window.SlingCMS = {};
  }
  window.SlingCMS.logger = logger;
}
