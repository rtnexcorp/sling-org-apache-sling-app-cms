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
<c:set var="asset" value="${resource}" />
<c:choose>
    <c:when test="${not empty asset.valueMap['jcr:content/jcr:title']}">
        <c:set var="assetTitle" value="${asset.valueMap['jcr:content/jcr:title']}" />
    </c:when>
    <c:otherwise>
        <c:set var="assetTitle" value="${asset.name}" />
    </c:otherwise>
</c:choose>
<c:set var="mimeType" value="${asset.valueMap['jcr:content/jcr:mimeType']}" />
<c:set var="fileSize" value="${asset.valueMap['jcr:content/jcr:data'].length}" />

<div class="asset-card card is-linked" title="${sling:encode(assetTitle,'HTML_ATTR')}" data-value="${sling:encode(asset.path,'HTML_ATTR')}" data-mime-type="${sling:encode(mimeType,'HTML_ATTR')}">
    <div class="card-image">
        <figure class="image is-5by4">
            <c:choose>
                <c:when test="${fn:startsWith(mimeType, 'image/')}">
                    <img src="/cms/file/preview.html${sling:encode(asset.path,'HTML_ATTR')}.transform/auto-rotate-thumbnail.png" loading="lazy" alt="${sling:encode(assetTitle, 'HTML_ATTR')}" class="asset-thumbnail" onerror="this.src='/cms/file/preview.html${sling:encode(asset.path,'HTML_ATTR')}.transform/sling-cms-thumbnail.png'">
                </c:when>
                <c:when test="${fn:startsWith(mimeType, 'video/')}">
                    <img src="/cms/file/preview.html${sling:encode(asset.path,'HTML_ATTR')}.transform/sling-cms-thumbnail.png" loading="lazy" alt="${sling:encode(assetTitle, 'HTML_ATTR')}" class="asset-thumbnail">
                    <span class="asset-badge asset-badge--video">
                        <i class="jam jam-video-camera"></i>
                    </span>
                </c:when>
                <c:when test="${mimeType == 'application/pdf'}">
                    <img src="/cms/file/preview.html${sling:encode(asset.path,'HTML_ATTR')}.transform/sling-cms-thumbnail.png" loading="lazy" alt="${sling:encode(assetTitle, 'HTML_ATTR')}" class="asset-thumbnail">
                    <span class="asset-badge asset-badge--pdf">PDF</span>
                </c:when>
                <c:otherwise>
                    <img src="/cms/file/preview.html${sling:encode(branding.gridIconsBase,'HTML_ATTR')}/file.png" loading="lazy" alt="${sling:encode(assetTitle, 'HTML_ATTR')}" class="asset-thumbnail">
                </c:otherwise>
            </c:choose>
        </figure>
        <div class="asset-card__overlay is-vhidden">
            <div class="asset-card__actions">
                <c:if test="${not empty actionConfigs}">
                    <c:forEach var="actionConfig" items="${actionConfigs}">
                        <c:set var="actionConfig" value="${actionConfig}" scope="request" />
                        <sling:include path="${sling:encode(asset.path,'HTML_ATTR')}" resourceType="${actionConfig.resourceType}" />
                    </c:forEach>
                </c:if>
            </div>
        </div>
    </div>
    <div class="card-content asset-card__info">
        <p class="asset-card__title" title="${sling:encode(assetTitle,'HTML_ATTR')}">
            ${sling:encode(assetTitle,'HTML')}
        </p>
        <div class="asset-card__meta">
            <c:if test="${not empty mimeType}">
                <span class="tag is-light is-small">${sling:encode(fn:substringAfter(mimeType, '/'),'HTML')}</span>
            </c:if>
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
        <c:catch var="ex">
            <fmt:formatDate type="date" dateStyle="medium" value="${asset.valueMap['jcr:content/jcr:lastModified'].time}" var="lastMod" />
            <small class="asset-card__date">${lastMod}</small>
        </c:catch>
    </div>
</div>
