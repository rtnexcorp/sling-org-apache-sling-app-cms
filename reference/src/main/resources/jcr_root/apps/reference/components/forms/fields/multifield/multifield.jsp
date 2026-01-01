<%-- /*
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
 */ --%>
<%@include file="/libs/sling-cms/global.jsp"%>
<c:set var="fieldName" value="${properties.name}" />
<c:set var="minItems" value="${not empty properties.minItems ? properties.minItems : 0}" />
<c:set var="maxItems" value="${not empty properties.maxItems ? properties.maxItems : -1}" />

<div class="${sling:encode(formConfig.fieldGroupClass,'HTML_ATTR')} ${sling:encode(properties.addClasses,'HTML_ATTR')} multifield-form-field" 
     data-field-name="${sling:encode(fieldName,'HTML_ATTR')}"
     data-min-items="${sling:encode(minItems,'HTML_ATTR')}"
     data-max-items="${sling:encode(maxItems,'HTML_ATTR')}">
    
    <c:if test="${not empty properties.label}">
        <label class="multifield-label">
            <sling:encode value="${properties.label}" mode="HTML" />
            <c:if test="${properties.required}">
                <span class="${sling:encode(formConfig.fieldRequiredClass,'HTML_ATTR')}">*</span>
            </c:if>
        </label>
    </c:if>
    
    <c:if test="${not empty properties.description}">
        <p class="multifield-description"><sling:encode value="${properties.description}" mode="HTML" /></p>
    </c:if>
    
    <%-- Template for new items --%>
    <c:set var="templateResource" value="${sling:getRelativeResource(resource, 'template')}" />
    <fieldset disabled="disabled" class="multifield-template" style="display:none;">
        <div class="multifield-item">
            <div class="multifield-item-header">
                <span class="multifield-item-title">Item</span>
                <div class="multifield-item-actions">
                    <button type="button" class="multifield-move-up" title="Move Up">&uarr;</button>
                    <button type="button" class="multifield-move-down" title="Move Down">&darr;</button>
                    <button type="button" class="multifield-remove" title="Remove">&times;</button>
                </div>
            </div>
            <div class="multifield-item-content">
                <c:if test="${templateResource != null}">
                    <c:forEach var="field" items="${sling:listChildren(templateResource)}">
                        <sling:include resource="${field}" />
                    </c:forEach>
                </c:if>
            </div>
        </div>
    </fieldset>
    
    <%-- Container for items --%>
    <div class="multifield-items">
        <%-- Load existing data if available --%>
        <c:if test="${not empty formData[fieldName]}">
            <c:set var="existingItems" value="${formData[fieldName]}" />
            <c:forEach var="itemData" items="${existingItems}" varStatus="itemStatus">
                <div class="multifield-item" data-index="${itemStatus.index}">
                    <div class="multifield-item-header">
                        <span class="multifield-item-title">Item ${itemStatus.index + 1}</span>
                        <div class="multifield-item-actions">
                            <button type="button" class="multifield-move-up" title="Move Up">&uarr;</button>
                            <button type="button" class="multifield-move-down" title="Move Down">&darr;</button>
                            <button type="button" class="multifield-remove" title="Remove">&times;</button>
                        </div>
                    </div>
                    <div class="multifield-item-content">
                        <c:if test="${templateResource != null}">
                            <c:set var="multifieldItemData" value="${itemData}" scope="request" />
                            <c:set var="multifieldItemIndex" value="${itemStatus.index}" scope="request" />
                            <c:set var="multifieldFieldName" value="${fieldName}" scope="request" />
                            <c:forEach var="field" items="${sling:listChildren(templateResource)}">
                                <sling:include resource="${field}" />
                            </c:forEach>
                            <c:remove var="multifieldItemData" scope="request" />
                            <c:remove var="multifieldItemIndex" scope="request" />
                            <c:remove var="multifieldFieldName" scope="request" />
                        </c:if>
                    </div>
                </div>
            </c:forEach>
        </c:if>
    </div>
    
    <%-- Add button --%>
    <button type="button" class="multifield-add ${sling:encode(formConfig.buttonClass,'HTML_ATTR')}">
        + Add Item
    </button>
</div>

<script>
(function() {
    document.querySelectorAll('.multifield-form-field').forEach(function(multifield) {
        if (multifield.dataset.initialized) return;
        multifield.dataset.initialized = 'true';
        
        var fieldName = multifield.dataset.fieldName;
        var minItems = parseInt(multifield.dataset.minItems) || 0;
        var maxItems = parseInt(multifield.dataset.maxItems) || -1;
        var itemsContainer = multifield.querySelector('.multifield-items');
        var template = multifield.querySelector('.multifield-template .multifield-item');
        var addBtn = multifield.querySelector('.multifield-add');
        var itemCounter = itemsContainer.querySelectorAll('.multifield-item').length;
        
        function updateIndices() {
            var items = itemsContainer.querySelectorAll('.multifield-item');
            items.forEach(function(item, idx) {
                item.dataset.index = idx;
                item.querySelector('.multifield-item-title').textContent = 'Item ' + (idx + 1);
                item.querySelectorAll('input, select, textarea').forEach(function(input) {
                    var name = input.getAttribute('name');
                    if (name && name.indexOf('@') === -1) {
                        var baseName = name.replace(/\[\d+\]/, '').replace(fieldName + '/', '');
                        input.setAttribute('name', fieldName + '[' + idx + ']/' + baseName);
                    }
                });
            });
            
            // Update add button visibility based on maxItems
            if (maxItems > 0 && items.length >= maxItems) {
                addBtn.style.display = 'none';
            } else {
                addBtn.style.display = '';
            }
        }
        
        function addItem() {
            var items = itemsContainer.querySelectorAll('.multifield-item');
            if (maxItems > 0 && items.length >= maxItems) return;
            
            var newItem = template.cloneNode(true);
            newItem.querySelectorAll('[disabled]').forEach(function(el) {
                el.removeAttribute('disabled');
            });
            itemsContainer.appendChild(newItem);
            itemCounter++;
            updateIndices();
            bindItemEvents(newItem);
        }
        
        function removeItem(item) {
            var items = itemsContainer.querySelectorAll('.multifield-item');
            if (minItems > 0 && items.length <= minItems) {
                alert('Minimum ' + minItems + ' item(s) required');
                return;
            }
            item.remove();
            updateIndices();
        }
        
        function moveUp(item) {
            var prev = item.previousElementSibling;
            if (prev && prev.classList.contains('multifield-item')) {
                itemsContainer.insertBefore(item, prev);
                updateIndices();
            }
        }
        
        function moveDown(item) {
            var next = item.nextElementSibling;
            if (next && next.classList.contains('multifield-item')) {
                itemsContainer.insertBefore(next, item);
                updateIndices();
            }
        }
        
        function bindItemEvents(item) {
            item.querySelector('.multifield-remove').addEventListener('click', function(e) {
                e.preventDefault();
                removeItem(item);
            });
            item.querySelector('.multifield-move-up').addEventListener('click', function(e) {
                e.preventDefault();
                moveUp(item);
            });
            item.querySelector('.multifield-move-down').addEventListener('click', function(e) {
                e.preventDefault();
                moveDown(item);
            });
        }
        
        // Bind events to existing items
        itemsContainer.querySelectorAll('.multifield-item').forEach(bindItemEvents);
        
        // Bind add button
        addBtn.addEventListener('click', function(e) {
            e.preventDefault();
            addItem();
        });
        
        // Initial index update
        updateIndices();
    });
})();
</script>

<style>
.multifield-form-field {
    margin-bottom: 1.5rem;
}
.multifield-label {
    display: block;
    font-weight: bold;
    margin-bottom: 0.5rem;
}
.multifield-description {
    color: #666;
    font-size: 0.875rem;
    margin-bottom: 0.75rem;
}
.multifield-item {
    border: 1px solid #ddd;
    border-radius: 4px;
    margin-bottom: 0.75rem;
    background: #fafafa;
}
.multifield-item-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.5rem 0.75rem;
    background: #eee;
    border-bottom: 1px solid #ddd;
}
.multifield-item-title {
    font-weight: 600;
}
.multifield-item-actions button {
    background: none;
    border: 1px solid #ccc;
    padding: 0.25rem 0.5rem;
    margin-left: 0.25rem;
    cursor: pointer;
    border-radius: 3px;
}
.multifield-item-actions button:hover {
    background: #ddd;
}
.multifield-item-actions .multifield-remove:hover {
    background: #f44;
    color: white;
    border-color: #f44;
}
.multifield-item-content {
    padding: 0.75rem;
}
.multifield-add {
    margin-top: 0.5rem;
}
</style>
