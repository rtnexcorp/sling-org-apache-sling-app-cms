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
/* TipTap editor is now initialized in cms.tiptap.js */

rava.bind(".file", {
  callbacks: {
    created() {
      const field = this;
      const close = field.closest("form").querySelector("a.close");

      function setProgress(m, progress) {
        const meter = m;
        meter.innerText = `${Math.round(progress)}%`;
        meter.value = Math.round(progress);
      }
      function uploadFile(meter, action, file) {
        const formData = new FormData();

        formData.append("*", file);
        formData.append("*@TypeHint", "sling:File");
        formData.append("_charset_", "utf-8");

        const xhr = new XMLHttpRequest();
        xhr.upload.addEventListener(
          "loadstart",
          () => {
            setProgress(meter, 0);
          },
          false
        );
        xhr.upload.addEventListener(
          "progress",
          (event) => {
            const percent = (event.loaded / event.total) * 100;
            setProgress(meter, percent);
          },
          false
        );
        xhr.upload.addEventListener(
          "load",
          () => {
            meter.classList.add("is-info");
          },
          false
        );
        xhr.addEventListener(
          "readystatechange",
          (event) => {
            let status;
            let text;
            let readyState;
            try {
              readyState = event.target.readyState;
              text = event.target.responseText;
              status = event.target.status;
            } catch (e) {
              meter.classList.add("is-danger");
            }
            if (readyState === 4) {
              meter.classList.remove("is-info");
              if (status === 200 && text) {
                meter.classList.add("is-success");
              } else {
                meter.classList.add("is-danger");
                window.SlingCMS.logger.warn(
                  "Failed to upload %s, received message %s",
                  file.name,
                  text
                );
              }
            }
          },
          false
        );
        xhr.open("POST", action, true);
        xhr.send(formData);
      }
      function handleFile(scope, file) {
        const it = document.createElement("div");
        const ctr = scope
          .closest(".control")
          .querySelector(".file-item-container");
        let meter = null;
        const template = document.querySelector(".file-item-template");
        if (template) {
          window.SlingCMS.safeSetInnerHTML(it, template.innerHTML);
        }
        meter = it.querySelector(".progress");
        const fileItemName = it.querySelector(".file-item-name");
        if (fileItemName) {
          fileItemName.innerText = file.name;
        }
        ctr.classList.remove("is-hidden");
        ctr.appendChild(it);
        uploadFile(meter, scope.closest("form").action, file);
      }

      field.addEventListener(
        "dragover",
        (event) => {
          event.preventDefault();
        },
        false
      );
      field.addEventListener(
        "dragenter",
        (event) => {
          event.preventDefault();
          field.classList.add("is-primary");
        },
        false
      );
      field.addEventListener(
        "dragleave",
        (event) => {
          event.preventDefault();
          if (!field.contains(event.fromElement)) {
            field.classList.remove("is-primary");
          }
        },
        false
      );
      field.addEventListener(
        "drop",
        (event) => {
          event.preventDefault();
          field.classList.remove("is-primary");
          if (event.dataTransfer.items) {
            const { items } = event.dataTransfer;
            for (let i = 0; i < items.length; i++) {
              // eslint-disable-line no-plusplus
              if (items[i].kind === "file") {
                handleFile(field, items[i].getAsFile());
              }
            }
          } else {
            const { files } = event.dataTransfer;
            for (let i = 0; i < files.length; i++) {
              // eslint-disable-line no-plusplus
              handleFile(field, files[i]);
            }
          }
        },
        false
      );
      field.closest("form").querySelector("button[type=submit]").remove();
      close.innerText = "Done";
      close.addEventListener("click", () => {
        window.Sling.CMS.ui.reloadContext();
      });
      field.querySelector("input").addEventListener("change", (event) => {
        const { files } = event.target;
        for (let i = 0; i < files.length; i++) {
          // eslint-disable-line no-plusplus
          handleFile(field, files[i]);
        }
      });
    },
  },
});

/* Support for updating the namehint when creating a component */
rava.bind(".namehint", {
  callbacks: {
    created() {
      const field = this;
      this.closest(".Form-Ajax")
        .querySelector('select[name="sling:resourceType"]')
        .addEventListener("change", (evt) => {
          const resourceType = evt.target.value.split("/");
          field.value = resourceType[resourceType.length - 1];
        });
    },
  },
});

/* Support for repeating form fields */
rava.bind(".repeating", {
  callbacks: {
    created() {
      const ctr = this;
      this.querySelectorAll(".repeating__add").forEach((el) => {
        el.addEventListener("click", (event) => {
          event.stopPropagation();
          event.preventDefault();
          const node = ctr
            .querySelector(".repeating__template > .repeating__item")
            .cloneNode(true);
          ctr.querySelector(".repeating__container").appendChild(node);
        });
      });
    },
  },
});
rava.bind(".repeating__item", {
  events: {
    ":scope .repeating__remove": {
      click(event) {
        event.stopPropagation();
        event.preventDefault();
        this.remove();
      },
    },
  },
});

/* Support for multifield form fields */
rava.bind(".multifield", {
  callbacks: {
    created() {
      const multifield = this;
      const baseName = multifield.dataset.baseName;
      let itemCounter = multifield.querySelectorAll(".multifield__container > .multifield__item").length;

      function updateItemIndices() {
        const items = multifield.querySelectorAll(".multifield__container > .multifield__item");
        items.forEach((item, index) => {
          // Update item title
          const titleEl = item.querySelector(".multifield__item-title");
          if (titleEl) {
            titleEl.textContent = `Item ${index + 1}`;
          }
          // Update field names to include item index
          item.querySelectorAll("input, select, textarea").forEach((field) => {
            const name = field.getAttribute("name");
            if (name && !name.includes("@")) {
              // Extract the field name (last part after /)
              const fieldName = name.split("/").pop();
              field.setAttribute("name", `${baseName}/item_${index}/${fieldName}`);
            }
          });
          item.dataset.itemIndex = index;
        });
      }

      // Add new item
      multifield.querySelector(".multifield__add").addEventListener("click", (event) => {
        window.SlingCMS.logger.debug("Multifield add button clicked");
        event.stopPropagation();
        event.preventDefault();
        
        const template = multifield.querySelector(".multifield__template > .multifield__item");
        if (template) {
          const newItem = template.cloneNode(true);
          // Enable all fields in the cloned item
          newItem.querySelectorAll("[disabled]").forEach((el) => {
            el.removeAttribute("disabled");
          });
          // Update field names with the new index
          newItem.querySelectorAll("input, select, textarea").forEach((field) => {
            const name = field.getAttribute("name");
            if (name) {
              const fieldName = name.split("/").pop() || name;
              field.setAttribute("name", `${baseName}/item_${itemCounter}/${fieldName}`);
            }
          });
          multifield.querySelector(".multifield__container").appendChild(newItem);
          itemCounter++;
          updateItemIndices();
        }
      });

      // Initial index update for existing items
      updateItemIndices();
    },
  },
});

/* Support for multifield item actions */
rava.bind(".multifield__item", {
  events: {
    ":scope .multifield__remove": {
      click(event) {
        window.SlingCMS.logger.debug("Multifield remove button clicked");
        event.stopPropagation();
        event.preventDefault();
        const item = this;
        const multifield = item.closest(".multifield");
        item.remove();
        // Trigger re-indexing
        if (multifield) {
          const evt = new Event("multifield:reindex");
          multifield.dispatchEvent(evt);
        }
      },
    },
    ":scope .multifield__move-up": {
      click(event) {
        window.SlingCMS.logger.debug("Multifield move-up button clicked");
        event.stopPropagation();
        event.preventDefault();
        const item = this;
        const prev = item.previousElementSibling;
        if (prev && prev.classList.contains("multifield__item")) {
          item.parentNode.insertBefore(item, prev);
          const multifield = item.closest(".multifield");
          if (multifield) {
            const evt = new Event("multifield:reindex");
            multifield.dispatchEvent(evt);
          }
        }
      },
    },
    ":scope .multifield__move-down": {
      click(event) {
        window.SlingCMS.logger.debug("Multifield move-down button clicked");
        event.stopPropagation();
        event.preventDefault();
        const item = this;
        const next = item.nextElementSibling;
        if (next && next.classList.contains("multifield__item")) {
          item.parentNode.insertBefore(next, item);
          const multifield = item.closest(".multifield");
          if (multifield) {
            const evt = new Event("multifield:reindex");
            multifield.dispatchEvent(evt);
          }
        }
      },
    },
  },
});

/* Listen for reindex events on multifield */
rava.bind(".multifield", {
  events: {
    "multifield:reindex"() {
      const multifield = this;
      const baseName = multifield.dataset.baseName;
      const items = multifield.querySelectorAll(".multifield__container > .multifield__item");
      items.forEach((item, index) => {
        const titleEl = item.querySelector(".multifield__item-title");
        if (titleEl) {
          titleEl.textContent = `Item ${index + 1}`;
        }
        item.querySelectorAll("input, select, textarea").forEach((field) => {
          const name = field.getAttribute("name");
          if (name && !name.includes("@")) {
            const fieldName = name.split("/").pop();
            field.setAttribute("name", `${baseName}/item_${index}/${fieldName}`);
          }
        });
        item.dataset.itemIndex = index;
      });
    },
  },
});

rava.bind('.field[data-events]:not([data-events=""])', {
  callbacks: {
    async created() {
      try {
        const events = this.dataset.events.split(",").filter((e) => e !== "");
        const res = await window.SlingCMS.errorHandler.fetchWithErrorHandling(
          `${this.dataset.path}/events.json`,
          {
            cache: "no-cache",
            headers: {
              Accept: "application/json",
            },
          }
        );
        window.SlingCMS.errorHandler.handleResponseError(res, 'Load field events');
        const handlers = await res.json();
        for (var event of events) {
          this.querySelectorAll("input,select,textarea").forEach((el) => {
            if (event === "load") {
              Function(handlers[event])();
            } else {
              el.addEventListener(event, Function(handlers[event]));
            }
          });
        }
      } catch (error) {
        window.SlingCMS.errorHandler.handleFetchError(error, 'Load field events');
      }
    },
  },
});

// Tabs field component handler
rava.bind(".editor-tabs", {
  callbacks: {
    created() {
      const tabsContainer = this;
      const tabsId = tabsContainer.dataset.tabsId;
      const tabItems = tabsContainer.querySelectorAll(".tabs ul li");
      const tabContents = tabsContainer.querySelectorAll(".tab-content");

      tabItems.forEach((tabItem) => {
        tabItem.addEventListener("click", (e) => {
          e.preventDefault();
          const targetId = tabItem.dataset.tabTarget;

          // Remove active class from all tabs
          tabItems.forEach((item) => item.classList.remove("is-active"));
          // Add active class to clicked tab
          tabItem.classList.add("is-active");

          // Hide all tab contents
          tabContents.forEach((content) => content.classList.add("is-hidden"));
          // Show target tab content
          const targetContent = document.getElementById(targetId);
          if (targetContent) {
            targetContent.classList.remove("is-hidden");
          }
        });
      });
    },
  },
});

// Fieldsets field component handler - provides collapsible fieldsets in editor dialogs
const FIELDSET_STORAGE_PREFIX = 'editor-fieldset-';

rava.bind('.editor-fieldset__toggle', {
  callbacks: {
    created() {
      const button = this;
      const fieldset = button.closest('.editor-fieldset');
      
      if (!fieldset) return;

      // Restore saved state from localStorage
      const fieldsetId = fieldset.getAttribute('data-fieldset-id');
      if (fieldsetId) {
        try {
          const savedState = localStorage.getItem(FIELDSET_STORAGE_PREFIX + fieldsetId);
          if (savedState !== null) {
            const isExpanded = savedState === 'true';
            const content = fieldset.querySelector('.editor-fieldset__content');

            if (content) {
              button.setAttribute('aria-expanded', isExpanded);
              content.setAttribute('aria-hidden', !isExpanded);
              fieldset.classList.toggle('is-collapsed', !isExpanded);
            }
          }
        } catch (err) {
          window.SlingCMS.logger.warn('Failed to restore fieldset state:', err);
        }
      }
    },
  },
  events: {
    click(e) {
      e.preventDefault();

      const button = this;
      const fieldset = button.closest('.editor-fieldset');
      if (!fieldset) return;

      const content = fieldset.querySelector('.editor-fieldset__content');
      if (!content) return;

      const isExpanded = button.getAttribute('aria-expanded') === 'true';
      const newState = !isExpanded;

      // Update ARIA attributes
      button.setAttribute('aria-expanded', newState);
      content.setAttribute('aria-hidden', !newState);

      // Update visual state
      fieldset.classList.toggle('is-collapsed', !newState);

      // Persist state in localStorage
      const fieldsetId = fieldset.getAttribute('data-fieldset-id');
      if (fieldsetId) {
        try {
          localStorage.setItem(FIELDSET_STORAGE_PREFIX + fieldsetId, newState);
        } catch (err) {
          window.SlingCMS.logger.warn('Failed to persist fieldset state:', err);
        }
      }
    },
  },
});
