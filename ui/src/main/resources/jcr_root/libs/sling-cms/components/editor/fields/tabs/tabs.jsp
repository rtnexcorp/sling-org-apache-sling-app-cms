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
<c:set var="tabsId" value="tabs-${fn:replace(resource.path, '/', '-')}" />
<div class="editor-tabs field" data-tabs-id="${sling:encode(tabsId,'HTML_ATTR')}">
    <div class="tabs is-boxed">
        <ul>
            <c:forEach var="tab" items="${sling:listChildren(resource)}" varStatus="status">
                <c:set var="tabProps" value="${sling:adaptTo(tab,'org.apache.sling.api.resource.ValueMap')}" />
                <fmt:message key="${tabProps.title}" var="tabTitle" />
                <li class="${status.first ? 'is-active' : ''}" data-tab-target="${sling:encode(tabsId,'HTML_ATTR')}-${status.index}">
                    <a href="#"><sling:encode value="${tabTitle}" mode="HTML" /></a>
                </li>
            </c:forEach>
        </ul>
    </div>
    <div class="tab-contents">
        <c:forEach var="tab" items="${sling:listChildren(resource)}" varStatus="status">
            <div class="tab-content ${status.first ? '' : 'is-hidden'}" id="${sling:encode(tabsId,'HTML_ATTR')}-${status.index}">
                <sling:include resource="${tab}" resourceType="sling-cms/components/general/container" />
            </div>
        </c:forEach>
    </div>
</div>
