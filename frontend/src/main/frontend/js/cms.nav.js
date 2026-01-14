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

const rava = window.rava;

rava.bind('.navbar-burger', {
  events: {
    click() {
      const target = document.querySelector(this.dataset.target);
      target.classList.toggle('is-active');
      this.classList.toggle('is-active');
    },
  },
});

rava.bind('.layout-switch select', {
  events: {
    change() {
      window.location = this.value;
    },
  },
});

rava.bind('.contentnav', {
  callbacks: {
    created() {
      const urlParams = new URLSearchParams(window.location.search);
      const resourceParam = urlParams.get('resource');
      const searchParam = urlParams.get('search');
      const cnav = this;
      // Support both contentnav-search and asset filter bar search inputs
      const search = document.querySelector('.contentnav-search input[name=search]') 
        || document.querySelector('[data-asset-search]');
      // Support both content-filter and asset-filter-bar type filters
      const typeFilter = document.querySelector('[data-content-type-filter]') 
        || document.querySelector('[data-asset-type-filter]');
      // Support both content-filter and asset-filter-bar tag filters
      const tagFilter = document.querySelector('[data-content-tag-filter]') 
        || document.querySelector('[data-asset-tag-filter]');
      
      function attrContains(ctx, attr) {
        let matches = false;
        const value = search ? search.value.toLowerCase() : '';
        ctx.querySelectorAll(`*[${attr}]`).forEach((it) => {
          if (it.getAttribute(attr).indexOf(value) !== -1) {
            matches = true;
          }
        });
        return matches;
      }
      
      function filter(event) {
        if (event) {
          event.stopPropagation();
          event.preventDefault();
        }
        const searchValue = search ? search.value.toLowerCase() : '';
        const typeValue = typeFilter ? typeFilter.value : '';
        const tagValue = tagFilter ? tagFilter.value : '';
        const typeFilters = typeValue ? typeValue.split(',').filter(t => t) : [];
        
        cnav.querySelectorAll('.contentnav__item').forEach((item) => {
          // Get item attributes
          const mimeType = item.dataset.mimeType || '';
          const isFolder = item.dataset.isFolder === 'true' || item.dataset.type === 'folder';
          const taxonomy = item.dataset.taxonomy || '';
          
          // Search match
          const matchesSearch = !searchValue || 
            item.innerText.toLowerCase().indexOf(searchValue) !== -1 ||
            attrContains(item, 'title') ||
            attrContains(item, 'data-value');
          
          // Type filter match
          let matchesType = true;
          if (typeFilters.length > 0) {
            if (typeFilters.includes('folder')) {
              matchesType = isFolder;
            } else if (!isFolder) {
              matchesType = typeFilters.some(filter => mimeType.startsWith(filter));
            }
          }
          
          // Tag filter match
          let matchesTag = true;
          if (tagValue) {
            if (isFolder) {
              // Folders always match tag filter (they contain items that might have tags)
              matchesTag = true;
            } else {
              // For files, check if taxonomy matches the selected tag
              const taxonomyPaths = taxonomy.split(',').filter(t => t.trim());
              matchesTag = taxonomyPaths.length > 0 && 
                taxonomyPaths.some(path => path === tagValue || path.startsWith(tagValue + '/'));
            }
          }
          
          // Show/hide item
          if (matchesSearch && matchesType && matchesTag) {
            item.classList.remove('is-hidden');
          } else {
            item.classList.add('is-hidden');
          }
        });
      }
      
      if (search) {
        search.addEventListener('keyup', filter);
        search.addEventListener('change', filter);
      }
      
      if (typeFilter) {
        typeFilter.addEventListener('change', filter);
      }
      
      if (tagFilter) {
        tagFilter.addEventListener('change', filter);
      }
      
      if (resourceParam) {
        cnav.querySelectorAll('.contentnav__item').forEach((item) => {
          if (item.querySelector(`*[data-value="${resourceParam}"]`)) {
            item.classList.remove('is-hidden');
            item.click();
          } else {
            item.classList.add('is-hidden');
          }
        });
        if (search) {
          search.value = resourceParam;
        }
      } else if (searchParam) {
        if (search) {
          search.value = searchParam;
        }
        filter();
      }
    },
  },
});
rava.bind('.contentnav .contentnav__item', {
  events: {
    click() {
      this.closest('.contentnav').querySelectorAll('.contentnav__item.is-selected').forEach((tr) => {
        tr.classList.remove('is-selected');
      });
      this.classList.add('is-selected');
      
      // Add "more" button if there are more than 3 action buttons
      // Works for both grid view (.cell-actions) and table view (td.is-vhidden)
      const cellActions = this.querySelector('.cell-actions') || this.querySelector('td.is-vhidden') || this.querySelector('td.cms-table__cell--visible-on-hover');
      
      if (cellActions) {
        const buttons = cellActions.querySelectorAll('.button:not(.cell-actions-more)');
        const existingMoreButton = cellActions.querySelector('.cell-actions-more');
        
        // Remove existing more button if present
        if (existingMoreButton) {
          existingMoreButton.remove();
        }
        
        // Add more button if there are more than 3 buttons
        if (buttons.length > 3) {
          const moreButton = document.createElement('button');
          moreButton.className = 'button cell-actions-more';
          moreButton.title = 'Show all actions';
          moreButton.setAttribute('type', 'button');
          moreButton.innerHTML = '<span class="jam jam-menu"><span class="is-sr-only">More actions</span></span>';
          
          moreButton.addEventListener('click', (e) => {
            e.stopPropagation();
            e.preventDefault();
            cellActions.classList.toggle('is-expanded');
            const icon = moreButton.querySelector('.jam');
            if (cellActions.classList.contains('is-expanded')) {
              icon.classList.remove('jam-menu');
              icon.classList.add('jam-close');
              moreButton.title = 'Show less';
            } else {
              icon.classList.remove('jam-close');
              icon.classList.add('jam-menu');
              moreButton.title = 'Show all actions';
            }
          });
          
          cellActions.appendChild(moreButton);
        }
      }
    },
    dblclick() {
      if (this.querySelector('.item-link')) {
        window.location = this.querySelector('.item-link').href;
      }
    },
  },
});
