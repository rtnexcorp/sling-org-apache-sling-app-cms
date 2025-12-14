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
<c:choose>
    <c:when test="${not empty param.page}">
        <c:set var="paginationPage" value="${param.page}" />
    </c:when>
    <c:otherwise>
        <c:set var="paginationPage" value="0" />
    </c:otherwise>
</c:choose>
<c:set var="PAGE_SIZE" value="${60}" />
 <div class="reload-container table__wrapper scroll-container contentnav" data-path="${sling:encode(resource.path,'HTML_ATTR')}.table.html${sling:encode(slingRequest.requestPathInfo.suffix,'HTML_ATTR')}">
    <table class="table is-fullwidth is-striped sortable">
        <thead>
            <tr>
                <th scope="col">
                    #
                </th>
                <c:forEach var="column" items="${sling:listChildren(sling:getRelativeResource(resource,'columns'))}">
                    <th class="${column.name == 'actions' ? 'is-hidden' : '' }" data-attribute="${sling:encode(column.name,'HTML_ATTR')}" scope="col">
                        <fmt:message key="${column.valueMap.title}" var="title" />
                        <sling:encode value="${title}" mode="HTML" />
                    </th>
                </c:forEach>
            </tr>
        </thead>
        <tbody>
            <c:set var="parentPath" value="${slingRequest.requestPathInfo.suffix}${not empty properties.appendSuffix ? properties.appendSuffix : ''}" />
            <c:set var="count" value="${paginationPage * PAGE_SIZE + 1}" />
            <c:forEach var="child" items="${sling:listChildren(sling:getResource(resourceResolver, parentPath))}" varStatus="status" begin="${paginationPage * PAGE_SIZE}" end="${(paginationPage * PAGE_SIZE + PAGE_SIZE) - 1}">
                <c:set var="type" value="${not empty child.valueMap['jcr:primaryType'] ? child.valueMap['jcr:primaryType'] : fn:replace(child.resourceType,'/','-')}" />
                <sling:getResource var="typeConfig" base="${resource}" path="types/${type}" />
                <c:if test="${typeConfig != null && !fn:contains(child.name,':')}">
                    <%-- Get MIME type for files --%>
                    <c:set var="mimeType" value="${child.valueMap['jcr:content/jcr:mimeType']}" />
                    <c:set var="isFolder" value="${type == 'sling:OrderedFolder' || type == 'sling:Folder' || type == 'nt:folder'}" />
                    
                    <%-- Get taxonomy --%>
                    <c:set var="assetTaxonomy" value="${child.valueMap['jcr:content/sling:taxonomy']}" />
                    <c:set var="taxonomyStr" value="" />
                    <c:if test="${not empty assetTaxonomy}">
                        <c:choose>
                            <c:when test="${fn:contains(assetTaxonomy.class.name, '[')}">
                                <c:forEach var="tax" items="${assetTaxonomy}" varStatus="taxStatus">
                                    <c:set var="taxonomyStr" value="${taxonomyStr}${taxStatus.first ? '' : ','}${tax}" />
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <c:set var="taxonomyStr" value="${assetTaxonomy}" />
                            </c:otherwise>
                        </c:choose>
                    </c:if>
                    
                    <tr class="contentnav__item sortable__row" 
                        data-resource="${sling:encode(child.path,'HTML_ATTR')}" 
                        data-type="${sling:encode(typeConfig.path,'HTML_ATTR')}"
                        data-mime-type="${sling:encode(mimeType,'HTML_ATTR')}"
                        data-is-folder="${isFolder}"
                        data-taxonomy="${sling:encode(taxonomyStr,'HTML_ATTR')}">
                        <td class="Cell-Static" title="# ${status.index + 1}" data-sort-value="<fmt:formatNumber pattern="0000" value="${count}" />">
                            ${count}
                        </td>
                        <c:forEach var="column" items="${sling:listChildren(sling:getRelativeResource(typeConfig,'columns'))}">
                            <c:set var="configPath" value="columns/${column.name}"/>
                            <c:set var="colConfig" value="${sling:getRelativeResource(typeConfig,configPath)}" scope="request" />
                            <c:if test="${colConfig != null}">
                                <sling:include path="${child.path}" resourceType="${colConfig.valueMap['sling:resourceType']}" />
                            </c:if>
                        </c:forEach>
                    </tr>
                    <c:set var="count" value="${count + 1}" />
                </c:if>
            </c:forEach> 
        </tbody>
    </table>
    <nav class="pagination" role="navigation" aria-label="pagination">
        <c:if test="${paginationPage != 0}">
            <a class="pagination-previous" href="?page=${paginationPage - 1}"><fmt:message key="Previous" /></a>
        </c:if>
        <c:if test="${paginationPage * PAGE_SIZE + PAGE_SIZE < fn:length(sling:listChildren(slingRequest.requestPathInfo.suffixResource))}">
            <a class="pagination-next" href="?page=${paginationPage + 1}"><fmt:message key="Next" /></a>
        </c:if>
    </nav>
</div>