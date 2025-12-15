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
 * Global Search module
 * Provides repo-wide server-side search functionality
 */
const rava = window.rava;

rava.bind('[data-component="global-search"]', {
  callbacks: {
    created() {
      const container = this;
      const searchInput = container.querySelector('input[name="search"]');
      const searchBtn = container.querySelector('[data-global-search-btn]');
      const searchPath = container.dataset.searchPath || '';
      
      /**
       * Perform repo-wide search by opening the search modal
       */
      function performGlobalSearch() {
        const term = searchInput ? searchInput.value.trim() : '';
        if (!term) {
          return;
        }
        
        // Build search URL with parameters
        const searchUrl = new URL('/cms/shared/search.html', window.location.origin);
        searchUrl.searchParams.set('term', term);
        if (searchPath) {
          searchUrl.searchParams.set('path', searchPath);
        }
        
        // Open search modal with results
        if (window.SlingCMS && window.SlingCMS.ui && window.SlingCMS.ui.confirmModal) {
          window.SlingCMS.ui.confirmModal(searchUrl.toString(), `Search: ${term}`);
        } else {
          // Fallback: navigate to search page
          window.location.href = searchUrl.toString();
        }
      }
      
      /**
       * Handle Enter key for search
       */
      function handleKeyPress(event) {
        if (event.key === 'Enter') {
          event.preventDefault();
          performGlobalSearch();
        }
      }
      
      // Bind events
      if (searchInput) {
        searchInput.addEventListener('keypress', handleKeyPress);
      }
      
      if (searchBtn) {
        searchBtn.addEventListener('click', (e) => {
          e.preventDefault();
          performGlobalSearch();
        });
      }
    }
  }
});
