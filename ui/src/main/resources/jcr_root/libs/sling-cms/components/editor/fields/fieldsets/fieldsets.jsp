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

<%-- Container for collapsible fieldsets --%>
<div class="editor-fieldsets">
    
    <%-- Render each fieldset as a collapsible section --%>
    <c:forEach var="fieldset" items="${sling:listChildren(resource)}" varStatus="status">
        <c:set var="fieldsetProps" value="${sling:adaptTo(fieldset,'org.apache.sling.api.resource.ValueMap')}" />
        <c:set var="fieldsetId" value="fieldset-${status.index}" />
        
        <fieldset class="editor-fieldset" data-fieldset-id="${fieldsetId}">
            <%-- Fieldset legend with toggle button --%>
            <legend class="editor-fieldset__legend">
                <button type="button" 
                        class="editor-fieldset__toggle" 
                        aria-expanded="true" 
                        aria-controls="${fieldsetId}">
                    <span class="editor-fieldset__toggle-icon">▼</span>
                    <span class="editor-fieldset__toggle-title">
                        <c:if test="${not empty fieldsetProps.title}">
                            <fmt:message key="${fieldsetProps.title}" />
                        </c:if>
                        <c:if test="${empty fieldsetProps.title}">
                            <fmt:message key="Section" /> ${status.index + 1}
                        </c:if>
                    </span>
                </button>
            </legend>
            
            <%-- Fieldset content (collapsible) --%>
            <div class="editor-fieldset__content" id="${fieldsetId}" role="region">
                <sling:include resource="${fieldset}" resourceType="sling-cms/components/general/container" />
            </div>
        </fieldset>
    </c:forEach>
    
</div>

