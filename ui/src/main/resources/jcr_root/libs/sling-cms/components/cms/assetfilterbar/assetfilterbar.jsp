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
<fmt:message key="Search assets..." var="searchPlaceholder" />
<fmt:message key="All Types" var="allTypes" />
<fmt:message key="Images" var="images" />
<fmt:message key="Videos" var="videos" />
<fmt:message key="Documents" var="documents" />
<fmt:message key="Grid View" var="gridView" />
<fmt:message key="List View" var="listView" />

<div class="asset-filter-bar" data-component="asset-filter-bar">
    <div class="asset-filter-bar__search">
        <div class="control has-icons-left">
            <input class="input" type="text" placeholder="${sling:encode(searchPlaceholder,'HTML_ATTR')}" 
                   data-asset-search aria-label="${sling:encode(searchPlaceholder,'HTML_ATTR')}">
            <span class="icon is-left">
                <i class="jam jam-search"></i>
            </span>
        </div>
    </div>
    
    <div class="asset-filter-bar__filters">
        <div class="select">
            <select data-asset-type-filter aria-label="Filter by type">
                <option value="">${sling:encode(allTypes,'HTML')}</option>
                <option value="image/">${sling:encode(images,'HTML')}</option>
                <option value="video/">${sling:encode(videos,'HTML')}</option>
                <option value="application/pdf,application/msword,application/vnd">${sling:encode(documents,'HTML')}</option>
            </select>
        </div>
    </div>
    
    <div class="asset-filter-bar__view-toggle">
        <div class="buttons has-addons">
            <button class="button is-small asset-view-btn is-selected" data-view="grid" title="${sling:encode(gridView,'HTML_ATTR')}">
                <span class="icon">
                    <i class="jam jam-grid"></i>
                </span>
            </button>
            <button class="button is-small asset-view-btn" data-view="list" title="${sling:encode(listView,'HTML_ATTR')}">
                <span class="icon">
                    <i class="jam jam-unordered-list"></i>
                </span>
            </button>
        </div>
    </div>
    
    <div class="asset-filter-bar__count">
        <span data-asset-count>0</span> <fmt:message key="items" />
    </div>
</div>
