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
 * CMS Search Module (Consolidated)
 * 
 * Handles all search functionality:
 * - Global search (navbar)
 * - Advanced search form (start page)
 * - Filter toggle and taxonomy quick search
 */

const rava = window.rava;

// =============================================================================
// Global Search Component (Navbar)
// =============================================================================
rava.bind('[data-component="global-search"]', {
    callbacks: {
        created() {
            const container = this;
            const searchInput = container.querySelector('input[name="search"]');
            const searchBtn = container.querySelector('[data-global-search-btn]');
            const searchPath = container.dataset.searchPath || '';
            
            function performGlobalSearch() {
                const term = searchInput ? searchInput.value.trim() : '';
                if (!term) return;
                
                const searchUrl = new URL('/cms/shared/search.html', window.location.origin);
                searchUrl.searchParams.set('q', term);
                if (searchPath) {
                    searchUrl.searchParams.set('path', searchPath);
                }
                
                if (window.SlingCMS && window.SlingCMS.ui && window.SlingCMS.ui.confirmModal) {
                    window.SlingCMS.ui.confirmModal(searchUrl.toString(), `Search: ${term}`);
                } else {
                    window.location.href = searchUrl.toString();
                }
            }
            
            function handleKeyPress(event) {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    performGlobalSearch();
                }
            }
            
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

// =============================================================================
// Advanced Search Form (Start Page)
// =============================================================================

// Toggle advanced search filters visibility
rava.bind('.advanced-search-toggle', {
    events: {
        click(event) {
            event.preventDefault();
            const form = this.closest('form');
            const filters = form.querySelector('.advanced-search-filters');
            const icon = this.querySelector('i.jam');
            
            if (filters.style.display === 'none') {
                filters.style.display = 'block';
                icon.classList.remove('jam-filter');
                icon.classList.add('jam-close');
                this.classList.remove('is-light');
                this.classList.add('is-info');
            } else {
                filters.style.display = 'none';
                icon.classList.remove('jam-close');
                icon.classList.add('jam-filter');
                this.classList.remove('is-info');
                this.classList.add('is-light');
            }
        }
    }
});

// Quick taxonomy search from tag badges
rava.bind('.taxonomy-quick-search', {
    events: {
        click(event) {
            event.preventDefault();
            const taxonomyPath = this.dataset.taxonomy;
            const form = document.querySelector('.advanced-search-form');
            
            if (form) {
                // Set the taxonomy select value
                const taxonomySelect = form.querySelector('select[name="taxonomy"]');
                if (taxonomySelect) {
                    taxonomySelect.value = taxonomyPath;
                }
                
                // Show advanced filters
                const filters = form.querySelector('.advanced-search-filters');
                if (filters && filters.style.display === 'none') {
                    filters.style.display = 'block';
                    const toggle = form.querySelector('.advanced-search-toggle');
                    if (toggle) {
                        const icon = toggle.querySelector('i.jam');
                        icon.classList.remove('jam-filter');
                        icon.classList.add('jam-close');
                        toggle.classList.remove('is-light');
                        toggle.classList.add('is-info');
                    }
                }
                
                // Submit the form
                form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
            }
        }
    }
});

// Clear filters button (can be added to form if needed)
rava.bind('.advanced-search-clear', {
    events: {
        click(event) {
            event.preventDefault();
            const form = this.closest('form');
            
            if (form) {
                // Clear all inputs
                form.querySelectorAll('input[type="text"], input[type="date"]').forEach(input => {
                    input.value = '';
                });
                
                // Reset selects to first option
                form.querySelectorAll('select').forEach(select => {
                    select.selectedIndex = 0;
                });
                
                // Submit to show initial state
                form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
            }
        }
    }
});

// Handle taxonomy badge clicks in search results (filter by same tag)
rava.bind('.taxonomy-badge', {
    events: {
        click(event) {
            // Don't prevent default - let the parent link work
            // But if we want to filter by tag instead, uncomment below:
            /*
            event.preventDefault();
            event.stopPropagation();
            const taxonomyPath = this.title;
            const form = document.querySelector('.advanced-search-form');
            if (form && taxonomyPath) {
                const taxonomySelect = form.querySelector('select[name="taxonomy"]');
                if (taxonomySelect) {
                    taxonomySelect.value = taxonomyPath;
                    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
                }
            }
            */
        }
    }
});
