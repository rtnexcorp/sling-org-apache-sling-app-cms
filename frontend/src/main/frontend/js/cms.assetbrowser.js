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
 * Asset Browser Module
 * Handles filtering, searching, and view toggling for the asset grid
 * 
 * Integration with Filter System:
 * - Works in conjunction with cms.contentfilter.js for the assetfilter component
 * - cms.contentfilter.js: Handles the filter dropdown UI (MIME type, date filters)
 * - cms.assetbrowser.js: Handles asset-specific filtering, search, and view modes
 * 
 * Used by: assetfilter component in asset browser views
 */
const Sling = window.Sling || {};

Sling.CMS = Sling.CMS || {};

Sling.CMS.AssetBrowser = {
  
  /**
   * Initialize all asset browsers on the page
   * Called when: Asset browser page loads or content is dynamically loaded
   * Used by: assetfilter component
   */
  init: function() {
    document.querySelectorAll('[data-component="asset-browser"]').forEach(browser => {
      this.initBrowser(browser);
    });
  },

  /**
   * Initialize a single asset browser
   * Sets up event listeners for:
   * - Search input: Real-time search with debouncing
   * - Type filter: MIME type filtering (image/, video/, document/, etc.)
   * - Tag filter: Taxonomy-based filtering
   * - View toggle: Grid/list view switching
   * 
   * Used by: assetfilter component
   * @param {HTMLElement} browser - The browser container element
   */
  initBrowser: function(browser) {
    const searchInput = browser.querySelector('[data-asset-search]');
    const typeFilter = browser.querySelector('[data-asset-type-filter]');
    const tagFilter = browser.querySelector('[data-asset-tag-filter]');
    const viewButtons = browser.querySelectorAll('.asset-view-btn');
    const assetGrid = browser.querySelector('.asset-grid');
    const countDisplay = browser.querySelector('[data-asset-count]');
    const emptyState = browser.querySelector('.asset-grid__empty');
    
    // Initialize count from metadata
    const metadata = browser.querySelector('[data-asset-metadata]');
    if (metadata && countDisplay) {
      try {
        const data = JSON.parse(metadata.textContent);
        countDisplay.textContent = data.count || 0;
      } catch (e) {
        // Ignore parsing errors
      }
    }

    // Search functionality
    // Used by: assetfilter - text search input with 300ms debounce
    if (searchInput) {
      let debounceTimer;
      searchInput.addEventListener('input', (e) => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
          this.filterAssets(browser);
        }, 300);
      });
    }

    // Type filter
    // Used by: assetfilter - MIME type dropdown (image/, video/, document/, etc.)
    if (typeFilter) {
      typeFilter.addEventListener('change', () => {
        this.filterAssets(browser);
      });
    }

    // Tag filter
    // Used by: assetfilter - Taxonomy/tag dropdown for categorization
    if (tagFilter) {
      tagFilter.addEventListener('change', () => {
        this.filterAssets(browser);
      });
    }

    // View toggle
    // Used by: assetfilter - switches between grid/list view for assets
    // Persists preference in localStorage
    viewButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        const view = btn.dataset.view;
        
        // Update button states
        viewButtons.forEach(b => b.classList.remove('is-selected'));
        btn.classList.add('is-selected');
        
        // Update grid view
        if (assetGrid) {
          assetGrid.dataset.view = view;
        }
        
        // Store preference
        localStorage.setItem('sling-cms-asset-view', view);
      });
    });

    // Restore view preference
    const savedView = localStorage.getItem('sling-cms-asset-view');
    if (savedView && assetGrid) {
      assetGrid.dataset.view = savedView;
      viewButtons.forEach(btn => {
        btn.classList.toggle('is-selected', btn.dataset.view === savedView);
      });
    }
  },

  /**
   * Filter assets based on search and type filter
   * @param {HTMLElement} browser - The browser container element
   */
  filterAssets: function(browser) {
    const searchInput = browser.querySelector('[data-asset-search]');
    const typeFilter = browser.querySelector('[data-asset-type-filter]');
    const tagFilter = browser.querySelector('[data-asset-tag-filter]');
    const items = browser.querySelectorAll('.asset-item');
    const countDisplay = browser.querySelector('[data-asset-count]');
    const emptyState = browser.querySelector('.asset-grid__empty');
    const gridItems = browser.querySelector('.asset-grid__items');
    
    const searchTerm = searchInput ? searchInput.value.toLowerCase().trim() : '';
    const typeFilters = typeFilter ? typeFilter.value.split(',').filter(t => t) : [];
    const tagFilterValue = tagFilter ? tagFilter.value : '';
    
    let visibleCount = 0;
    
    items.forEach(item => {
      const name = item.dataset.name || '';
      const mimeType = item.dataset.mimeType || '';
      const isFolder = item.dataset.isFolder === 'true';
      const taxonomy = item.dataset.taxonomy || '';
      
      // Search match
      const matchesSearch = !searchTerm || name.includes(searchTerm);
      
      // Type filter match
      let matchesType = true;
      if (typeFilters.length > 0 && !isFolder) {
        matchesType = typeFilters.some(filter => mimeType.startsWith(filter));
      }
      
      // Tag filter match
      let matchesTag = true;
      if (tagFilterValue && !isFolder) {
        const taxonomyPaths = taxonomy.split(',').filter(t => t.trim());
        matchesTag = taxonomyPaths.some(path => path === tagFilterValue || path.startsWith(tagFilterValue + '/'));
      }
      
      // Show/hide item
      const isVisible = matchesSearch && matchesType && matchesTag;
      item.style.display = isVisible ? '' : 'none';
      
      if (isVisible) {
        visibleCount++;
      }
    });
    
    // Update count
    if (countDisplay) {
      countDisplay.textContent = visibleCount;
    }
    
    // Show/hide empty state
    if (emptyState && gridItems) {
      emptyState.style.display = visibleCount === 0 ? 'block' : 'none';
      gridItems.style.display = visibleCount === 0 ? 'none' : '';
    }
  }
};

// Initialize on DOM ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    Sling.CMS.AssetBrowser.init();
  });
} else {
  Sling.CMS.AssetBrowser.init();
}

// Re-initialize on AJAX content load
document.addEventListener('slingcms:reload', () => {
  Sling.CMS.AssetBrowser.init();
});

export default Sling.CMS.AssetBrowser;
