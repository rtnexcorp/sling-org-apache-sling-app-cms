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
 * Content Filter Module
 * Generic filtering system used by multiple filter components:
 * - sitefilter: Filters pages by status, template, modified date
 * - assetfilter: Filters assets by MIME type, modified date, file size
 * - workflowfilter: Filters workflow instances by status, process type
 * - contentfilter: Generic content filtering
 * 
 * All components use data-component="content-filter" attribute
 */
const Sling = window.Sling || {};

Sling.CMS = Sling.CMS || {};

Sling.CMS.ContentFilter = {

  /**
   * Initialize all content filters on the page
   * Called by: sitefilter, assetfilter, workflowfilter, contentfilter
   */
  init: function() {
    document.querySelectorAll('[data-component="content-filter"]').forEach(filter => {
      this.initFilter(filter);
    });
  },

  /**
   * Initialize a single content filter
   * Called by: sitefilter, assetfilter, workflowfilter, contentfilter
   * @param {HTMLElement} filterElement - The filter container element
   */
  initFilter: function(filterElement) {
    // Load dynamic options for filters that need them
    this.loadDynamicOptions(filterElement);

    // Attach change listeners
    this.attachChangeListeners(filterElement);
  },

  /**
   * Load dynamic options for filters that have data-dynamic-options attribute
   * Used by:
   * - sitefilter: Loads template options dynamically
   * - workflowfilter: Loads process-type options dynamically
   * - assetfilter: Not used (static options only)
   * - contentfilter: May load dynamic options based on configuration
   * @param {HTMLElement} filterElement - The filter container element
   */
  loadDynamicOptions: function(filterElement) {
    const dynamicSelects = filterElement.querySelectorAll('[data-dynamic-options="true"]');

    dynamicSelects.forEach(select => {
      const filterType = select.getAttribute('data-filter');

      if (filterType) {
        this.fetchAndPopulateOptions(select, filterType);
      }
    });
  },

  /**
   * Fetch options from server and populate select element
   * Used by:
   * - sitefilter: Fetches template options from /bin/cms/filter-options.json?type=template
   * - workflowfilter: Fetches process-type options from /bin/cms/filter-options.json?type=process-type
   * @param {HTMLSelectElement} selectElement - The select element to populate
   * @param {string} filterType - The filter type (e.g., 'process-type', 'template')
   */
  fetchAndPopulateOptions: function(selectElement, filterType) {
    const apiUrl = `/bin/cms/filter-options.json?type=${encodeURIComponent(filterType)}`;

    fetch(apiUrl)
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        return response.json();
      })
      .then(options => {
        // Clear existing options except the first one (usually "All")
        while (selectElement.options.length > 1) {
          selectElement.remove(1);
        }

        // Add new options
        options.forEach(option => {
          const optionElement = document.createElement('option');
          optionElement.value = option.value;
          optionElement.textContent = option.label;
          selectElement.appendChild(optionElement);
        });

        // Enable the select if it was disabled
        selectElement.disabled = false;
      })
      .catch(error => {
        console.error(`Error loading filter options for ${filterType}:`, error);
        // Keep the select disabled or show error state
      });
  },

  /**
   * Attach change event listeners to all filter selects
   * Called by: sitefilter, assetfilter, workflowfilter, contentfilter
   * @param {HTMLElement} filterElement - The filter container element
   */
  attachChangeListeners: function(filterElement) {
    const selects = filterElement.querySelectorAll('.cms-select__input');

    selects.forEach(select => {
      select.addEventListener('change', (e) => {
        this.handleFilterChange(filterElement, e.target);
      });
    });
  },

  /**
   * Handle filter change event
   * Called by: sitefilter, assetfilter, workflowfilter, contentfilter
   * Dispatches 'cms:filter-change' custom event that can be listened to by other components
   * @param {HTMLElement} filterElement - The filter container element
   * @param {HTMLSelectElement} changedSelect - The select that changed
   */
  handleFilterChange: function(filterElement, changedSelect) {
    const filterAttr = changedSelect.getAttribute('data-filter');
    const filterValue = changedSelect.value;

    // Get all current filter values
    const allFilters = this.getCurrentFilters(filterElement);

    // Dispatch custom event that other components can listen to
    const event = new CustomEvent('cms:filter-change', {
      detail: {
        attribute: filterAttr,
        value: filterValue,
        allFilters: allFilters
      },
      bubbles: true
    });

    filterElement.dispatchEvent(event);

    // Apply filters to items in the page
    this.applyFilters(allFilters);
  },

  /**
   * Get all current filter values
   * Called by: sitefilter, assetfilter, workflowfilter, contentfilter
   * @param {HTMLElement} filterElement - The filter container element
   * @returns {Object} Object with filter attributes as keys and values as values
   */
  getCurrentFilters: function(filterElement) {
    const filters = {};
    const selects = filterElement.querySelectorAll('.cms-select__input');

    selects.forEach(select => {
      const attr = select.getAttribute('data-filter');
      const value = select.value;

      if (attr && value) {
        filters[attr] = value;
      }
    });

    return filters;
  },

  /**
   * Apply filters to filterable items on the page
   * Called by: sitefilter, assetfilter, workflowfilter, contentfilter
   * 
   * Filter behavior by component:
   * - sitefilter: Filters table rows with data-page-status, data-template, data-modified-date
   * - assetfilter: Filters asset items with data-mime-type, data-modified-date, data-file-size
   * - workflowfilter: Filters workflow instances with data-status, data-process-type, data-initiator
   * - contentfilter: Filters generic content items based on configured attributes
   * 
   * @param {Object} filters - Object with filter attributes and values
   */
  applyFilters: function(filters) {
    // Find all filterable items (workflow instances, assets, content items, etc.)
    const filterableItems = document.querySelectorAll('[data-filterable-item]');

    if (filterableItems.length === 0) {
      // Fallback: look for common patterns
      const items = document.querySelectorAll(
        '[data-instance-id], [data-asset-item], [data-content-item], .cms-table tbody tr'
      );

      items.forEach(item => {
        this.filterItem(item, filters);
      });
    } else {
      filterableItems.forEach(item => {
        this.filterItem(item, filters);
      });
    }

    // Update counts if available
    this.updateFilterCount();
  },

  /**
   * Filter a single item based on filter criteria
   * Called by: sitefilter, assetfilter, workflowfilter, contentfilter
   * 
   * Expects items to have data attributes matching filter names:
   * - sitefilter: data-page-status, data-template, data-modified-date
   * - assetfilter: data-mime-type, data-modified-date, data-file-size
   * - workflowfilter: data-status, data-process-type, data-initiator
   * 
   * @param {HTMLElement} item - The item to filter
   * @param {Object} filters - Object with filter attributes and values
   */
  filterItem: function(item, filters) {
    let visible = true;

    // Check each filter
    Object.entries(filters).forEach(([attr, value]) => {
      if (!value) return; // Skip empty filters

      const itemValue = item.getAttribute(`data-${attr}`);

      if (!this.matchesFilter(itemValue, value)) {
        visible = false;
      }
    });

    // Show or hide the item
    if (visible) {
      item.style.display = '';
      item.classList.remove('cms-filtered-out');
    } else {
      item.style.display = 'none';
      item.classList.add('cms-filtered-out');
    }
  },

  /**
   * Check if an item value matches a filter value
   * Called by: sitefilter, assetfilter, workflowfilter, contentfilter
   * 
   * Matching strategies:
   * - Comma-separated values: Used by assetfilter for multiple MIME types (e.g., "image/,video/")
   * - Prefix matching: Used by assetfilter for MIME type categories (e.g., "image/" matches "image/png")
   * - Exact matching: Used by sitefilter (status, template), workflowfilter (status, process-type), assetfilter (size)
   * 
   * @param {string} itemValue - The item's attribute value
   * @param {string} filterValue - The filter value to match
   * @returns {boolean} True if matches
   */
  matchesFilter: function(itemValue, filterValue) {
    if (!itemValue) return false;

    // Handle multiple values separated by commas (for MIME types, etc.)
    if (filterValue.includes(',')) {
      const filterParts = filterValue.split(',');
      return filterParts.some(part => itemValue.startsWith(part));
    }

    // Handle prefix matching (e.g., "image/" matches "image/png")
    if (filterValue.endsWith('/')) {
      return itemValue.startsWith(filterValue);
    }

    // Handle exact matching
    return itemValue === filterValue;
  },

  /**
   * Update visible item count displays
   * Called by: sitefilter, assetfilter, workflowfilter, contentfilter
   * 
   * Updates elements with [data-filter-count] attribute to show number of visible items
   * Used by assetfilter to show "X items" count in the asset browser
   */
  updateFilterCount: function() {
    const countDisplays = document.querySelectorAll('[data-filter-count]');

    countDisplays.forEach(display => {
      const container = display.closest('[data-filterable-container]') || document;
      const items = container.querySelectorAll('[data-filterable-item]:not(.cms-filtered-out)');
      const visibleCount = Array.from(items).filter(item => item.style.display !== 'none').length;

      display.textContent = visibleCount;
    });
  }
};

// Initialize on DOM ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', function() {
    Sling.CMS.ContentFilter.init();
  });
} else {
  Sling.CMS.ContentFilter.init();
}

// Re-initialize when new content is loaded dynamically
document.addEventListener('cms:content-loaded', function() {
  Sling.CMS.ContentFilter.init();
});
