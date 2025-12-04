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
<c:set var="baseName" value="${properties.name}" />
<c:set var="itemTemplate" value="${sling:getRelativeResource(resource, 'template')}" />
<c:set var="minItems" value="${properties.minItems}" />
<c:set var="maxItems" value="${properties.maxItems}" />

<%-- Get the edited resource from request suffix --%>
<c:if test="${slingRequest.requestPathInfo.suffix != null}">
    <sling:getResource path="${slingRequest.requestPathInfo.suffix}" var="suffixResource" />
</c:if>

<div class="multifield" data-base-name="${sling:encode(baseName,'HTML_ATTR')}" 
     data-min-items="${sling:encode(minItems,'HTML_ATTR')}" 
     data-max-items="${sling:encode(maxItems,'HTML_ATTR')}">
    
    <%-- Template for new items (hidden and disabled) --%>
    <fieldset disabled="disabled" class="multifield__template is-hidden">
        <div class="multifield__item card mb-3">
            <header class="card-header multifield__item-header">
                <p class="card-header-title">
                    <span class="multifield__item-title"><fmt:message key="Item" /></span>
                </p>
                <div class="card-header-icon">
                    <button type="button" class="multifield__move-up button is-small" title="<fmt:message key='Move Up' />">
                        <span class="jam jam-chevron-up"></span>
                    </button>
                    <button type="button" class="multifield__move-down button is-small" title="<fmt:message key='Move Down' />">
                        <span class="jam jam-chevron-down"></span>
                    </button>
                    <button type="button" class="multifield__remove button is-small is-danger" title="<fmt:message key='Remove' />">
                        <span class="jam jam-trash"></span>
                    </button>
                </div>
            </header>
            <div class="card-content multifield__item-content">
                <c:if test="${itemTemplate != null}">
                    <c:forEach var="field" items="${sling:listChildren(itemTemplate)}">
                        <sling:include resource="${field}" />
                    </c:forEach>
                </c:if>
            </div>
        </div>
    </fieldset>
    
    <%-- Container for existing and new items --%>
    <div class="multifield__container">
        <%-- Render existing items from the edited resource --%>
        <c:if test="${suffixResource != null}">
            <c:set var="itemsResource" value="${sling:getRelativeResource(suffixResource, baseName)}" />
            <c:if test="${itemsResource != null}">
                <c:forEach var="item" items="${sling:listChildren(itemsResource)}" varStatus="status">
                    <div class="multifield__item card mb-3" data-item-name="${sling:encode(item.name,'HTML_ATTR')}">
                        <header class="card-header multifield__item-header">
                            <p class="card-header-title">
                                <span class="multifield__item-title"><fmt:message key="Item" /> ${status.index + 1}</span>
                            </p>
                            <div class="card-header-icon">
                                <button type="button" class="multifield__move-up button is-small" title="<fmt:message key='Move Up' />">
                                    <span class="jam jam-chevron-up"></span>
                                </button>
                                <button type="button" class="multifield__move-down button is-small" title="<fmt:message key='Move Down' />">
                                    <span class="jam jam-chevron-down"></span>
                                </button>
                                <button type="button" class="multifield__remove button is-small is-danger" title="<fmt:message key='Remove' />">
                                    <span class="jam jam-trash"></span>
                                </button>
                            </div>
                        </header>
                        <div class="card-content multifield__item-content">
                            <c:if test="${itemTemplate != null}">
                                <%-- Store the current item's path for field value resolution --%>
                                <c:set var="multifieldItemResource" value="${item}" scope="request" />
                                <c:set var="multifieldBaseName" value="${baseName}" scope="request" />
                                <c:set var="multifieldItemName" value="${item.name}" scope="request" />
                                <c:forEach var="field" items="${sling:listChildren(itemTemplate)}">
                                    <sling:include resource="${field}" />
                                </c:forEach>
                                <c:remove var="multifieldItemResource" scope="request" />
                            </c:if>
                        </div>
                    </div>
                </c:forEach>
            </c:if>
        </c:if>
    </div>
    
    <%-- Add button --%>
    <div class="multifield__actions mt-3">
        <button type="button" class="multifield__add button is-primary">
            <span class="jam jam-plus"></span>
            <span class="ml-2"><fmt:message key="Add Item" /></span>
        </button>
    </div>
    
    <%-- Hidden field for Sling POST servlet to handle deletion of the entire container --%>
    <input type="hidden" name="${sling:encode(baseName,'HTML_ATTR')}@Delete" value="delete" />
</div>
