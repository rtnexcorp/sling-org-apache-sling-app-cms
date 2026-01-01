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

import { logger } from './logger.js';

/**
 * Handle fetch errors with user-friendly messages
 * @param {Error} error - The error object
 * @param {string} context - Context description for debugging
 * @param {Function} onError - Optional error callback
 */
export function handleFetchError(error, context = 'Request', onError = null) {
  logger.error(`${context} failed:`, error);
  
  let userMessage = 'An error occurred. Please try again.';
  
  if (error.name === 'TypeError' && error.message.includes('fetch')) {
    userMessage = 'Network error. Please check your connection.';
  } else if (error.message) {
    userMessage = error.message;
  }
  
  if (onError) {
    onError(userMessage, error);
  } else if (window.Sling?.CMS?.ui?.confirmMessage) {
    window.Sling.CMS.ui.confirmMessage('Error', userMessage, () => {});
  } else {
    alert(`Error: ${userMessage}`);
  }
}

/**
 * Wrapper for fetch with error handling and timeout
 * @param {string} url - URL to fetch
 * @param {Object} options - Fetch options
 * @param {number} timeout - Request timeout in milliseconds (default: 30000)
 * @returns {Promise<Response>}
 */
export async function fetchWithErrorHandling(url, options = {}, timeout = 30000) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeout);
  
  try {
    const response = await fetch(url, {
      ...options,
      signal: controller.signal
    });
    
    clearTimeout(timeoutId);
    return response;
  } catch (error) {
    clearTimeout(timeoutId);
    
    if (error.name === 'AbortError') {
      const timeoutError = new Error('Request timeout. Please try again.');
      timeoutError.name = 'TimeoutError';
      throw timeoutError;
    }
    
    throw error;
  }
}

/**
 * Handle response errors
 * @param {Response} response - Fetch response
 * @param {string} context - Context description
 * @returns {Response}
 * @throws {Error}
 */
export function handleResponseError(response, context = 'Request') {
  if (!response.ok) {
    const error = new Error(`${context} failed: ${response.status} ${response.statusText}`);
    error.response = response;
    error.status = response.status;
    throw error;
  }
  return response;
}

/**
 * Safe async wrapper that catches and logs errors
 * @param {Function} fn - Async function to wrap
 * @param {string} context - Context description
 * @param {Function} onError - Optional error callback
 * @returns {Function}
 */
export function safeAsync(fn, context = 'Operation', onError = null) {
  return async (...args) => {
    try {
      return await fn(...args);
    } catch (error) {
      handleFetchError(error, context, onError);
      throw error;
    }
  };
}

// Make error handlers available globally
if (typeof window !== 'undefined') {
  if (!window.SlingCMS) {
    window.SlingCMS = {};
  }
  window.SlingCMS.errorHandler = {
    handleFetchError,
    fetchWithErrorHandling,
    handleResponseError,
    safeAsync
  };
}
