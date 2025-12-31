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
 * Repeating Path Browser Component
 * Allows multiple path selections with visual browser for each item
 */

const rava = window.rava;

rava.bind('.repeating-pathbrowser', {
  callbacks: {
    created() {
      const container = this;
      const template = container.querySelector('.repeating-pathbrowser__template');
      const itemsContainer = container.querySelector('.repeating-pathbrowser__container');
      const addButton = container.querySelector('.repeating-pathbrowser__add');

      // Add button handler
      addButton.addEventListener('click', () => {
        addPathItem();
      });

      // Add a new path browser item
      function addPathItem(initialValue = '') {
        // Clone the template
        const clone = template.querySelector('.repeating-pathbrowser__item').cloneNode(true);
        const removeButton = clone.querySelector('.repeating-pathbrowser__remove');

        // Set initial value if provided
        if (initialValue) {
          const hiddenInput = clone.querySelector('.pathbrowser-input');
          const pathDisplay = clone.querySelector('.pathbrowser-selected__path');
          hiddenInput.value = initialValue;
          pathDisplay.textContent = initialValue;
        }

        // Remove button handler
        removeButton.addEventListener('click', () => {
          clone.remove();
        });

        // Add to container first
        itemsContainer.appendChild(clone);

        // Initialize the pathbrowser for this item AFTER it's in the DOM
        // Pass the container that is now in the DOM
        const pathbrowserContainer = clone.querySelector('.pathbrowser-container');
        initializePathBrowser(pathbrowserContainer);
      }

      // Initialize a single pathbrowser instance
      function initializePathBrowser(pathbrowserContainer) {
        const fieldName = pathbrowserContainer.dataset.fieldName;
        const basePath = pathbrowserContainer.dataset.basePath || '/content';
        const filterType = pathbrowserContainer.dataset.filterType || '';

        const hiddenInput = pathbrowserContainer.querySelector('.pathbrowser-input');
        const selectedDisplay = pathbrowserContainer.querySelector('.pathbrowser-selected__path');
        const toggleButton = pathbrowserContainer.querySelector('.pathbrowser-toggle');
        const panel = pathbrowserContainer.querySelector('.pathbrowser-panel');
        const breadcrumbList = pathbrowserContainer.querySelector('.pathbrowser-breadcrumb__list');
        const itemsContainer = pathbrowserContainer.querySelector('.pathbrowser-items');
        const loadingEl = pathbrowserContainer.querySelector('.pathbrowser-loading');
        const emptyEl = pathbrowserContainer.querySelector('.pathbrowser-empty');
        const cancelButton = pathbrowserContainer.querySelector('.pathbrowser-cancel');
        const selectButton = pathbrowserContainer.querySelector('.pathbrowser-select');

        // Check if all required elements exist
        if (!hiddenInput || !selectedDisplay || !toggleButton || !panel) {
          return;
        }

        let currentPath = basePath;
        let selectedPath = hiddenInput.value || basePath;

        // Toggle panel
        toggleButton.addEventListener('click', (e) => {
          e.preventDefault();
          e.stopPropagation();

          const isHidden = panel.classList.contains('is-hidden');

          if (isHidden) {
            // Close all other panels first
            document.querySelectorAll('.pathbrowser-panel').forEach(p => {
              if (p !== panel) {
                p.classList.add('is-hidden');
                p.style.display = 'none';
              }
            });

            // Open this panel
            panel.classList.remove('is-hidden');
            panel.style.display = 'block';

            // Rotate chevron icon
            const chevron = toggleButton.querySelector('.jam-chevron-down');
            if (chevron) {
              chevron.style.transform = 'rotate(180deg)';
            }

            loadFolder(selectedPath || currentPath);
          } else {
            // Close this panel
            panel.classList.add('is-hidden');
            panel.style.display = 'none';

            // Reset chevron icon
            const chevron = toggleButton.querySelector('.jam-chevron-down');
            if (chevron) {
              chevron.style.transform = 'rotate(0deg)';
            }
          }
        });

        // Cancel button
        cancelButton.addEventListener('click', (e) => {
          e.preventDefault();
          panel.classList.add('is-hidden');
          panel.style.display = 'none';
        });

        // Select button
        selectButton.addEventListener('click', (e) => {
          e.preventDefault();
          if (selectedPath) {
            hiddenInput.value = selectedPath;
            selectedDisplay.textContent = selectedPath;
            panel.classList.add('is-hidden');
            panel.style.display = 'none';
          }
        });

        // Load folder contents
        async function loadFolder(path) {
          currentPath = path;
          updateBreadcrumb(path);
          loadingEl.classList.remove('is-hidden');
          emptyEl.classList.add('is-hidden');
          itemsContainer.innerHTML = '';

          try {
            // Use indexed /bin/cms/paths endpoint for better performance (same as pathbrowser)
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
              throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const paths = await response.json();

            if (!Array.isArray(paths)) {
              throw new Error('Invalid response: expected array of paths');
            }

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

                // Determine if it's a container (folder/page)
                const isContainer = nodeType.includes('Folder') || nodeType.includes('Page') || nodeType === 'nt:folder' || nodeType === 'sling:Site';

                return {
                  name,
                  path: childPath,
                  type: nodeType,
                  isContainer
                };
              } catch (err) {
                return null;
              }
            });

            const results = await Promise.all(detailsPromises);
            const items = results.filter(item => item !== null);

            // Sort: containers first, then alphabetically
            items.sort((a, b) => {
              if (a.isContainer !== b.isContainer) {
                return a.isContainer ? -1 : 1;
              }
              return a.name.localeCompare(b.name);
            });

            loadingEl.classList.add('is-hidden');

            if (items.length === 0) {
              emptyEl.classList.remove('is-hidden');
              return;
            }

            items.forEach(item => {
              const itemEl = createItem(item);
              itemsContainer.appendChild(itemEl);
            });
          } catch (error) {
            loadingEl.classList.add('is-hidden');
            emptyEl.classList.remove('is-hidden');
          }
        }

        // Create item element
        function createItem(item) {
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
            itemsContainer.querySelectorAll('.pathbrowser-item').forEach(el => {
              el.classList.remove('is-selected');
            });
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

          return itemEl;
        }

        // Update breadcrumb
        function updateBreadcrumb(path) {
          breadcrumbList.innerHTML = '';
          const parts = path.split('/').filter(p => p);
          let currentBreadcrumbPath = '';

          parts.forEach((part, index) => {
            currentBreadcrumbPath += '/' + part;
            const li = document.createElement('li');
            if (index === parts.length - 1) {
              li.classList.add('is-active');
            }

            const a = document.createElement('a');
            a.textContent = part;
            a.href = '#';
            const breadcrumbPath = currentBreadcrumbPath;
            a.addEventListener('click', (e) => {
              e.preventDefault();
              loadFolder(breadcrumbPath);
            });

            li.appendChild(a);
            breadcrumbList.appendChild(li);
          });
        }
      }

      // Initialize with one empty item
      addPathItem();
    }
  }
});
