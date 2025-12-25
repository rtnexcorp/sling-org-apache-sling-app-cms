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
 * Path Browser Component
 *
 * Provides a macOS Finder-like visual folder browser for selecting paths.
 * Features:
 * - Tree/list view of folders
 * - Breadcrumb navigation
 * - Search/filter functionality
 * - Single-click selection
 * - Type filtering (folders, pages, assets)
 */

const rava = window.rava;

rava.bind('.pathbrowser-container', {
  callbacks: {
    created() {
      const container = this;
      const fieldName = container.dataset.fieldName;
      const basePath = container.dataset.basePath || '/content';
      const filterType = container.dataset.filterType || '';
      const initialValue = container.dataset.initialValue || '';

      const hiddenInput = document.querySelector(`input[name="${fieldName}"]`);
      const selectedDisplay = container.querySelector('.pathbrowser-selected__path');
      const toggleButton = container.querySelector('.pathbrowser-toggle');
      const panel = container.querySelector('.pathbrowser-panel');
      const breadcrumbList = container.querySelector('.pathbrowser-breadcrumb__list');
      const itemsContainer = container.querySelector('.pathbrowser-items');
      const loadingEl = container.querySelector('.pathbrowser-loading');
      const emptyEl = container.querySelector('.pathbrowser-empty');
      const searchInput = container.querySelector('.pathbrowser-search__input');
      const cancelButton = container.querySelector('.pathbrowser-cancel');
      const selectButton = container.querySelector('.pathbrowser-select');

      let currentPath = basePath;
      let selectedPath = initialValue || basePath;
      let allItems = [];

      // Toggle panel open/close
      toggleButton.addEventListener('click', () => {
        const isHidden = panel.classList.contains('is-hidden');
        if (isHidden) {
          panel.classList.remove('is-hidden');
          loadFolder(selectedPath || currentPath);
        } else {
          panel.classList.add('is-hidden');
        }
      });

      // Cancel button
      cancelButton.addEventListener('click', () => {
        panel.classList.add('is-hidden');
      });

      // Select button
      selectButton.addEventListener('click', () => {
        if (selectedPath) {
          hiddenInput.value = selectedPath;
          selectedDisplay.textContent = selectedPath;
          panel.classList.add('is-hidden');
        }
      });

      // Search/filter
      let searchTimeout;
      searchInput.addEventListener('input', () => {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
          filterItems(searchInput.value);
        }, 300);
      });

      // Load folder contents
      async function loadFolder(path) {
        currentPath = path;

        // Update breadcrumb
        updateBreadcrumb(path);

        // Show loading
        loadingEl.classList.remove('is-hidden');
        emptyEl.classList.add('is-hidden');
        itemsContainer.innerHTML = '';

        try {
          // Use indexed /bin/cms/paths endpoint for better performance
          // Map filterType to PathSuggestionServlet type keys
          let typeParam = 'all';
          if (filterType) {
            const types = filterType.split(',').map(t => t.trim());
            // Map common types to PathSuggestionServlet keys
            if (types.some(t => t.includes('Folder'))) {
              typeParam = 'folder';
            } else if (types.includes('sling:Page')) {
              typeParam = 'page';
            } else if (types.some(t => t.includes('File') || t.includes('file'))) {
              typeParam = 'file';
            }
          }

          const url = `/bin/cms/paths?path=${encodeURIComponent(path)}&type=${encodeURIComponent(typeParam)}`;
          const response = await fetch(url);

          if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
          }

          const paths = await response.json();
          allItems = [];

          // Fetch details for each path to get node type
          const detailsPromises = paths.map(async (childPath) => {
            try {
              const detailResponse = await fetch(`${childPath}.json`);
              if (!detailResponse.ok) {
                return null;
              }
              const details = await detailResponse.json();
              const name = childPath.substring(childPath.lastIndexOf('/') + 1);
              const nodeType = details['jcr:primaryType'];

              // Additional filtering if specific types were requested
              if (filterType) {
                const types = filterType.split(',').map(t => t.trim());
                if (!types.includes(nodeType)) {
                  return null;
                }
              }

              // Determine if it's a container (folder/page)
              const isContainer = nodeType.includes('Folder') || nodeType.includes('Page') || nodeType === 'nt:folder';

              return {
                name,
                path: childPath,
                type: nodeType,
                isContainer,
              };
            } catch (err) {
              window.SlingCMS.logger.warn('Failed to fetch details for:', childPath, err);
              return null;
            }
          });

          const results = await Promise.all(detailsPromises);
          allItems = results.filter(item => item !== null);

          // Sort: containers first, then alphabetically
          allItems.sort((a, b) => {
            if (a.isContainer !== b.isContainer) {
              return a.isContainer ? -1 : 1;
            }
            return a.name.localeCompare(b.name);
          });

          renderItems(allItems);

        } catch (error) {
          window.SlingCMS.logger.error('Failed to load folder:', error);
          itemsContainer.innerHTML = '<p class="has-text-danger">Failed to load folders</p>';
        } finally {
          loadingEl.classList.add('is-hidden');
        }
      }

      // Render items in the list
      function renderItems(items) {
        itemsContainer.innerHTML = '';

        if (items.length === 0) {
          emptyEl.classList.remove('is-hidden');
          return;
        }

        emptyEl.classList.add('is-hidden');

        items.forEach(item => {
          const itemEl = document.createElement('div');
          itemEl.className = 'pathbrowser-item';
          itemEl.dataset.path = item.path;
          itemEl.dataset.isContainer = item.isContainer;

          const icon = item.isContainer ? 'jam-folder' : 'jam-document';
          const typeLabel = item.type.split(':')[1] || item.type;

          itemEl.innerHTML = `
            <span class="icon is-small">
              <span class="jam ${icon}"></span>
            </span>
            <span class="pathbrowser-item__name">${item.name}</span>
            <span class="pathbrowser-item__type">${typeLabel}</span>
          `;

          // Single click to select
          itemEl.addEventListener('click', () => {
            // Remove previous selection
            itemsContainer.querySelectorAll('.pathbrowser-item').forEach(el => {
              el.classList.remove('is-selected');
            });

            // Select this item
            itemEl.classList.add('is-selected');
            selectedPath = item.path;
            selectButton.disabled = false;
          });

          // Double click to navigate (if container)
          if (item.isContainer) {
            itemEl.addEventListener('dblclick', () => {
              loadFolder(item.path);
            });
            itemEl.classList.add('is-container');
          }

          itemsContainer.appendChild(itemEl);
        });
      }

      // Update breadcrumb navigation
      function updateBreadcrumb(path) {
        breadcrumbList.innerHTML = '';

        const parts = path.split('/').filter(p => p);
        let currentPathBuild = '';

        // Add root
        const rootLi = document.createElement('li');
        const rootLink = document.createElement('a');
        rootLink.textContent = 'Root';
        rootLink.href = '#';
        rootLink.addEventListener('click', (e) => {
          e.preventDefault();
          loadFolder('/');
        });
        rootLi.appendChild(rootLink);
        breadcrumbList.appendChild(rootLi);

        // Add each part
        parts.forEach((part, index) => {
          currentPathBuild += '/' + part;
          const partPath = currentPathBuild;

          const li = document.createElement('li');
          if (index === parts.length - 1) {
            li.classList.add('is-active');
            const span = document.createElement('a');
            span.textContent = part;
            span.setAttribute('aria-current', 'page');
            li.appendChild(span);
          } else {
            const link = document.createElement('a');
            link.textContent = part;
            link.href = '#';
            link.addEventListener('click', (e) => {
              e.preventDefault();
              loadFolder(partPath);
            });
            li.appendChild(link);
          }

          breadcrumbList.appendChild(li);
        });
      }

      // Filter items by search term
      function filterItems(searchTerm) {
        if (!searchTerm) {
          renderItems(allItems);
          return;
        }

        const term = searchTerm.toLowerCase();
        const filtered = allItems.filter(item =>
          item.name.toLowerCase().includes(term)
        );

        renderItems(filtered);
      }

      // Initialize if there's an initial value
      if (initialValue) {
        selectedDisplay.textContent = initialValue;
      }
    },
  },
});
