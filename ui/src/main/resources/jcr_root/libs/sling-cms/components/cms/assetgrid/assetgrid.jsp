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
<c:set var="suffixResource" value="${slingRequest.requestPathInfo.suffixResource}" />

<div class="asset-browser" data-component="asset-browser" 
     data-path="${sling:encode(resource.path,'HTML_ATTR')}.assetgrid.html${sling:encode(slingRequest.requestPathInfo.suffix,'HTML_ATTR')}">
    
    <%-- Filter Bar --%>
    <sling:include resourceType="sling-cms/components/cms/assetfilterbar" />
    
    <%-- Asset Grid Container --%>
    <div class="asset-grid reload-container scroll-container contentnav" data-view="grid">
        <div class="asset-grid__items columns is-multiline">
            <c:set var="itemCount" value="0" />
            <c:forEach var="child" items="${sling:listChildren(suffixResource)}" varStatus="status" begin="${paginationPage * PAGE_SIZE}" end="${(paginationPage * PAGE_SIZE + PAGE_SIZE) - 1}">
                <c:set var="showItem" value="${false}" />
                <c:forEach var="type" items="${sling:listChildren(sling:getRelativeResource(resource,'types'))}">
                    <c:if test="${child.valueMap['jcr:primaryType'] == type.name}">
                        <c:set var="showItem" value="${true}" />
                    </c:if>
                </c:forEach>
                
                <c:if test="${showItem}">
                    <c:set var="itemCount" value="${itemCount + 1}" />
                    <c:set var="mimeType" value="${child.valueMap['jcr:content/jcr:mimeType']}" />
                    <c:set var="isFile" value="${child.resourceType == 'sling:File' || child.resourceType == 'nt:file'}" />
                    <c:set var="isFolder" value="${child.resourceType == 'sling:OrderedFolder' || child.resourceType == 'sling:Folder' || child.resourceType == 'nt:folder'}" />
                    
                    <%-- Get title --%>
                    <c:choose>
                        <c:when test="${not empty child.valueMap['jcr:content/jcr:title']}">
                            <c:set var="title" value="${child.valueMap['jcr:content/jcr:title']}" />
                        </c:when>
                        <c:when test="${not empty child.valueMap['jcr:title']}">
                            <c:set var="title" value="${child.valueMap['jcr:title']}" />
                        </c:when>
                        <c:otherwise>
                            <c:set var="title" value="${child.name}" />
                        </c:otherwise>
                    </c:choose>
                    
                    <%-- Get taxonomy --%>
                    <c:set var="assetTaxonomy" value="${child.valueMap['jcr:content/sling:taxonomy']}" />
                    <c:set var="taxonomyStr" value="" />
                    <c:if test="${not empty assetTaxonomy}">
                        <c:choose>
                            <c:when test="${assetTaxonomy.class.array}">
                                <c:forEach var="tax" items="${assetTaxonomy}" varStatus="taxStatus">
                                    <c:set var="taxonomyStr" value="${taxonomyStr}${taxStatus.first ? '' : ','}${tax}" />
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <c:set var="taxonomyStr" value="${assetTaxonomy}" />
                            </c:otherwise>
                        </c:choose>
                    </c:if>
                    
                    <div class="column is-half-tablet is-one-third-widescreen is-one-quarter-fullhd contentnav__item asset-item" 
                         data-name="${sling:encode(fn:toLowerCase(child.name),'HTML_ATTR')}" 
                         data-mime-type="${sling:encode(mimeType,'HTML_ATTR')}"
                         data-is-folder="${isFolder}"
                         data-taxonomy="${sling:encode(taxonomyStr,'HTML_ATTR')}">
                        <div class="card is-linked asset-card" title="${sling:encode(title,'HTML_ATTR')}" data-value="${sling:encode(child.path,'HTML_ATTR')}">
                            <div class="card-image">
                                <figure class="image is-5by4">
                                    <c:choose>
                                        <c:when test="${isFile}">
                                            <img src="/cms/file/preview.html${sling:encode(child.path,'HTML_ATTR')}.transform/sling-cms-thumbnail.png" loading="lazy" alt="${sling:encode(child.name, 'HTML_ATTR')}">
                                            <c:if test="${fn:startsWith(mimeType, 'video/')}">
                                                <span class="asset-badge asset-badge--video">
                                                    <i class="jam jam-video-camera"></i>
                                                </span>
                                            </c:if>
                                            <c:if test="${mimeType == 'application/pdf'}">
                                                <span class="asset-badge asset-badge--pdf">PDF</span>
                                            </c:if>
                                        </c:when>
                                        <c:when test="${isFolder}">
                                            <img src="/cms/file/preview.html${sling:encode(branding.gridIconsBase,'HTML_ATTR')}/folder.png" loading="lazy" alt="${sling:encode(child.name, 'HTML_ATTR')}">
                                        </c:when>
                                        <c:otherwise>
                                            <img src="/cms/file/preview.html${sling:encode(branding.gridIconsBase,'HTML_ATTR')}/file.png" loading="lazy" alt="${sling:encode(child.name, 'HTML_ATTR')}">
                                        </c:otherwise>
                                    </c:choose>
                                </figure>
                                <div class="is-vhidden cell-actions">
                                    <sling:getResource base="${resource}" path="types/${child.valueMap['jcr:primaryType']}/columns/actions" var="colConfig" />
                                    <c:forEach var="ac" items="${sling:listChildren(colConfig)}">
                                        <c:set var="actionConfig" value="${ac}" scope="request" />
                                        <sling:include path="${sling:encode(child.path,'HTML_ATTR')}" resourceType="${actionConfig.resourceType}" />
                                    </c:forEach>
                                </div>
                            </div>
                            <div class="card-content asset-card__info">
                                <p class="asset-card__title" title="${sling:encode(title,'HTML_ATTR')}">
                                    ${sling:encode(title,'HTML')}
                                </p>
                                <c:if test="${isFile}">
                                    <div class="asset-card__meta">
                                        <c:if test="${not empty mimeType}">
                                            <span class="tag is-light is-small">${sling:encode(fn:substringAfter(mimeType, '/'),'HTML')}</span>
                                        </c:if>
                                        <c:set var="fileSize" value="${child.valueMap['jcr:content/jcr:data'].length}" />
                                        <c:if test="${fileSize > 0}">
                                            <span class="asset-card__size">
                                                <c:choose>
                                                    <c:when test="${fileSize >= 1048576}">
                                                        <fmt:formatNumber value="${fileSize / 1048576}" maxFractionDigits="1" /> MB
                                                    </c:when>
                                                    <c:when test="${fileSize >= 1024}">
                                                        <fmt:formatNumber value="${fileSize / 1024}" maxFractionDigits="0" /> KB
                                                    </c:when>
                                                    <c:otherwise>
                                                        ${fileSize} B
                                                    </c:otherwise>
                                                </c:choose>
                                            </span>
                                        </c:if>
                                    </div>
                                </c:if>
                                <c:catch var="ex">
                                    <fmt:formatDate type="date" dateStyle="medium" value="${child.valueMap['jcr:content/jcr:lastModified'].time}" var="lastMod" />
                                    <small class="asset-card__date">${lastMod}</small>
                                </c:catch>
                            </div>
                            <footer class="card-footer">
                                <sling:adaptTo adaptable="${resourceResolver}" adaptTo="org.apache.sling.cms.publication.PublicationManager" var="publicationManager" />
                                <sling:adaptTo adaptable="${child}" adaptTo="org.apache.sling.cms.PublishableResource" var="publishableResource" />
                                <c:if test="${not empty publicationManager}">
                                    <c:set var="published" value="${publishableResource.published}" />
                                    <span class="card-footer-item publication-status publication-status__${published ? 'published' : 'unpublished'}">
                                        <c:choose>
                                            <c:when test="${published}">
                                                <fmt:message key="Published" />
                                            </c:when>
                                            <c:otherwise>
                                                <fmt:message key="Draft" />
                                            </c:otherwise>
                                        </c:choose>
                                    </span>
                                </c:if>
                            </footer>
                        </div>
                    </div>
                </c:if>
            </c:forEach>
        </div>
        
        <%-- Empty state --%>
        <div class="asset-grid__empty" style="display: none;">
            <div class="has-text-centered py-6">
                <span class="icon is-large">
                    <i class="jam jam-folder-open" style="font-size: 3rem;"></i>
                </span>
                <p class="mt-4"><fmt:message key="No assets found" /></p>
            </div>
        </div>
        
        <%-- Pagination --%>
        <c:set var="totalItems" value="${fn:length(sling:listChildren(suffixResource))}" />
        <c:if test="${totalItems > PAGE_SIZE}">
            <nav class="pagination is-centered mt-4" role="navigation" aria-label="pagination">
                <c:if test="${paginationPage > 0}">
                    <a class="pagination-previous" href="?page=${paginationPage - 1}">
                        <fmt:message key="Previous" />
                    </a>
                </c:if>
                <c:if test="${(paginationPage + 1) * PAGE_SIZE < totalItems}">
                    <a class="pagination-next" href="?page=${paginationPage + 1}">
                        <fmt:message key="Next" />
                    </a>
                </c:if>
            </nav>
        </c:if>
    </div>
    
    <%-- Hidden count for JS --%>
    <script type="application/json" data-asset-metadata>
        {"count": ${itemCount}}
    </script>
</div>
