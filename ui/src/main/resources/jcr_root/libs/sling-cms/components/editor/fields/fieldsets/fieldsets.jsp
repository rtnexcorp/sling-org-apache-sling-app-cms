<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%><%-- /*
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

<%-- Container for collapsible fieldsets using Bulma-style approach --%>
<div class="editor-fieldsets">
    <c:forEach var="fieldset" items="${sling:listChildren(resource)}" varStatus="status">
        <c:set var="fieldsetProps" value="${sling:adaptTo(fieldset,'org.apache.sling.api.resource.ValueMap')}" />
        <c:set var="fieldsetId" value="fieldset-${fn:replace(sling:encode(resource.path,'HTML_ATTR'), '/', '-')}-${status.index}" />
        
        <div class="box editor-fieldset" data-fieldset-id="${sling:encode(fieldsetId,'HTML_ATTR')}">
            <%-- Clickable header to toggle content --%>
            <div class="editor-fieldset__header" data-toggle="${sling:encode(fieldsetId,'HTML_ATTR')}">
                <span class="icon is-small editor-fieldset__icon">
                    <i class="jam jam-chevron-down"></i>
                </span>
                <strong class="editor-fieldset__title">
                    <c:if test="${not empty fieldsetProps.title}">
                        <fmt:message key="${fieldsetProps.title}" />
                    </c:if>
                    <c:if test="${empty fieldsetProps.title}">
                        <fmt:message key="Section" /> ${status.index + 1}
                    </c:if>
                </strong>
            </div>
            
            <%-- Collapsible content --%>
            <div class="editor-fieldset__content" id="${sling:encode(fieldsetId,'HTML_ATTR')}">
                <sling:include resource="${fieldset}" resourceType="sling-cms/components/general/container" />
            </div>
        </div>
    </c:forEach>
</div>

<script>
(function() {
    'use strict';
    
    console.log('[FIELDSETS] Script loaded');
    console.log('[FIELDSETS] Document ready state:', document.readyState);
    
    // Initialize fieldsets collapse/expand functionality
    function initFieldsets() {
        console.log('[FIELDSETS] initFieldsets() called');
        
        const STORAGE_PREFIX = 'cms-fieldset-';
        const headers = document.querySelectorAll('.editor-fieldset__header[data-toggle]');
        
        console.log('[FIELDSETS] Found headers:', headers.length);
        
        headers.forEach(function(header, index) {
            console.log('[FIELDSETS] Processing header', index);
            
            const targetId = header.dataset.toggle;
            const content = document.getElementById(targetId);
            const fieldset = header.closest('.editor-fieldset');
            const icon = header.querySelector('.editor-fieldset__icon i');
            
            console.log('[FIELDSETS] Header', index, '- targetId:', targetId, 'content:', !!content, 'fieldset:', !!fieldset, 'icon:', !!icon);
            
            if (!content) {
                console.warn('[FIELDSETS] No content found for header', index);
                return;
            }
            
            // Restore saved state from localStorage
            const savedState = localStorage.getItem(STORAGE_PREFIX + targetId);
            const isExpanded = savedState !== null ? savedState === 'true' : true;
            
            console.log('[FIELDSETS] Header', index, '- savedState:', savedState, 'isExpanded:', isExpanded);
            
            // Set initial state
            if (!isExpanded) {
                content.classList.add('is-hidden');
                fieldset.classList.add('is-collapsed');
                if (icon) {
                    icon.classList.remove('jam-chevron-down');
                    icon.classList.add('jam-chevron-right');
                }
                console.log('[FIELDSETS] Header', index, '- set to collapsed');
            }
            
            // Add click handler
            header.addEventListener('click', function(e) {
                console.log('[FIELDSETS] Click event triggered on header', index);
                e.preventDefault();
                e.stopPropagation();
                
                const isCurrentlyHidden = content.classList.contains('is-hidden');
                console.log('[FIELDSETS] Currently hidden:', isCurrentlyHidden);
                
                // Toggle visibility
                content.classList.toggle('is-hidden');
                fieldset.classList.toggle('is-collapsed');
                
                // Toggle icon
                if (icon) {
                    if (isCurrentlyHidden) {
                        icon.classList.remove('jam-chevron-right');
                        icon.classList.add('jam-chevron-down');
                    } else {
                        icon.classList.remove('jam-chevron-down');
                        icon.classList.add('jam-chevron-right');
                    }
                }
                
                // Save state
                try {
                    localStorage.setItem(STORAGE_PREFIX + targetId, isCurrentlyHidden.toString());
                    console.log('[FIELDSETS] Saved state:', isCurrentlyHidden);
                } catch (err) {
                    console.warn('[FIELDSETS] Failed to save fieldset state:', err);
                }
            });
            
            // Make header keyboard accessible
            header.setAttribute('tabindex', '0');
            header.setAttribute('role', 'button');
            header.setAttribute('aria-expanded', isExpanded.toString());
            header.setAttribute('aria-controls', targetId);
            
            header.addEventListener('keydown', function(e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    console.log('[FIELDSETS] Keyboard event triggered on header', index);
                    e.preventDefault();
                    header.click();
                }
            });
            
            console.log('[FIELDSETS] Header', index, '- initialized successfully');
        });
        
        console.log('[FIELDSETS] initFieldsets() completed');
    }
    
    // Initialize immediately if DOM is ready, otherwise wait
    if (document.readyState === 'loading') {
        console.log('[FIELDSETS] DOM still loading, waiting for DOMContentLoaded');
        document.addEventListener('DOMContentLoaded', function() {
            console.log('[FIELDSETS] DOMContentLoaded event fired');
            initFieldsets();
        });
    } else {
        console.log('[FIELDSETS] DOM already ready, initializing immediately');
        initFieldsets();
    }
    
    // Also initialize when content is dynamically loaded (e.g., in modals)
    if (window.MutationObserver) {
        console.log('[FIELDSETS] Setting up MutationObserver');
        const observer = new MutationObserver(function(mutations) {
            let hasNewNodes = false;
            mutations.forEach(function(mutation) {
                if (mutation.addedNodes.length) {
                    hasNewNodes = true;
                }
            });
            if (hasNewNodes) {
                console.log('[FIELDSETS] New nodes detected, re-initializing');
                initFieldsets();
            }
        });
        
        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
    }
    
    console.log('[FIELDSETS] Script initialization complete');
})();
</script>
