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
<c:set var="asset" value="${slingRequest.requestPathInfo.suffixResource}" />
<c:set var="assetContent" value="${sling:getRelativeResource(asset, 'jcr:content')}" />
<c:set var="mimeType" value="${assetContent.valueMap['jcr:mimeType']}" />
<c:set var="fileSize" value="${assetContent.valueMap['jcr:data'].length}" />

<fmt:message key="Asset Metadata" var="pageTitle" />
<fmt:message key="Save" var="saveBtn" />
<fmt:message key="Cancel" var="cancelBtn" />

<sling:adaptTo adaptable="${asset}" adaptTo="org.apache.sling.thumbnails.ThumbnailSupport" var="thumbnailSupport" />

<div class="asset-metadata-editor">
    <div class="columns">
        <%-- Preview Column --%>
        <div class="column is-5">
            <div class="asset-metadata-editor__preview">
                <figure class="image">
                    <c:choose>
                        <c:when test="${fn:startsWith(mimeType, 'image/')}">
                            <img src="${sling:encode(asset.path,'HTML_ATTR')}.transform/sling-cms-thumbnail.png" alt="${sling:encode(asset.name,'HTML_ATTR')}">
                        </c:when>
                        <c:when test="${fn:startsWith(mimeType, 'video/')}">
                            <img src="${sling:encode(asset.path,'HTML_ATTR')}.transform/sling-cms-thumbnail.png" alt="${sling:encode(asset.name,'HTML_ATTR')}">
                            <span class="asset-badge asset-badge--video asset-badge--large">
                                <i class="jam jam-video-camera"></i> Video
                            </span>
                        </c:when>
                        <c:when test="${mimeType == 'application/pdf'}">
                            <img src="${sling:encode(asset.path,'HTML_ATTR')}.transform/sling-cms-thumbnail.png" alt="${sling:encode(asset.name,'HTML_ATTR')}">
                        </c:when>
                        <c:otherwise>
                            <img src="/cms/file/preview.html${sling:encode(branding.gridIconsBase,'HTML_ATTR')}/file.png" alt="${sling:encode(asset.name,'HTML_ATTR')}">
                        </c:otherwise>
                    </c:choose>
                </figure>
                
                <div class="asset-metadata-editor__info mt-4">
                    <table class="table is-narrow is-fullwidth">
                        <tbody>
                            <tr>
                                <th><fmt:message key="File Name" /></th>
                                <td>${sling:encode(asset.name,'HTML')}</td>
                            </tr>
                            <tr>
                                <th><fmt:message key="MIME Type" /></th>
                                <td>${sling:encode(mimeType,'HTML')}</td>
                            </tr>
                            <tr>
                                <th><fmt:message key="Size" /></th>
                                <td>
                                    <c:choose>
                                        <c:when test="${fileSize >= 1048576}">
                                            <fmt:formatNumber value="${fileSize / 1048576}" maxFractionDigits="2" /> MB
                                        </c:when>
                                        <c:when test="${fileSize >= 1024}">
                                            <fmt:formatNumber value="${fileSize / 1024}" maxFractionDigits="1" /> KB
                                        </c:when>
                                        <c:otherwise>
                                            ${fileSize} bytes
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                            <c:catch var="ex">
                                <fmt:formatDate type="both" dateStyle="medium" timeStyle="short" value="${assetContent.valueMap['jcr:lastModified'].time}" var="lastMod" />
                                <tr>
                                    <th><fmt:message key="Last Modified" /></th>
                                    <td>${lastMod}</td>
                                </tr>
                            </c:catch>
                            <c:catch var="ex2">
                                <fmt:formatDate type="both" dateStyle="medium" timeStyle="short" value="${assetContent.valueMap['jcr:created'].time}" var="created" />
                                <tr>
                                    <th><fmt:message key="Created" /></th>
                                    <td>${created}</td>
                                </tr>
                            </c:catch>
                            <c:if test="${fn:startsWith(mimeType, 'image/')}">
                                <c:if test="${not empty assetContent.valueMap['tiff:ImageWidth']}">
                                    <tr>
                                        <th><fmt:message key="Dimensions" /></th>
                                        <td>${assetContent.valueMap['tiff:ImageWidth']} x ${assetContent.valueMap['tiff:ImageLength']} px</td>
                                    </tr>
                                </c:if>
                            </c:if>
                        </tbody>
                    </table>
                </div>
                
                <%-- Renditions --%>
                <c:set var="renditions" value="${sling:getRelativeResource(assetContent, 'renditions')}" />
                <c:if test="${not empty renditions}">
                    <div class="asset-metadata-editor__renditions mt-4">
                        <h4 class="title is-6"><fmt:message key="Renditions" /></h4>
                        <div class="tags">
                            <c:forEach var="rendition" items="${sling:listChildren(renditions)}">
                                <a href="${sling:encode(rendition.path,'HTML_ATTR')}" class="tag is-link" target="_blank">
                                    ${sling:encode(rendition.name,'HTML')}
                                </a>
                            </c:forEach>
                        </div>
                    </div>
                </c:if>
            </div>
        </div>
        
        <%-- Form Column --%>
        <div class="column is-7">
            <form method="post" action="${sling:encode(assetContent.path,'HTML_ATTR')}" class="Form-Ajax" data-add-date="true">
                <input type="hidden" name="_charset_" value="UTF-8" />
                
                <%-- Basic Metadata Tab --%>
                <div class="box">
                    <h4 class="title is-5"><fmt:message key="Basic Information" /></h4>
                    
                    <div class="field">
                        <label class="label"><fmt:message key="Title" /></label>
                        <div class="control">
                            <input class="input" type="text" name="jcr:title" 
                                   value="${sling:encode(assetContent.valueMap['jcr:title'],'HTML_ATTR')}"
                                   placeholder="Enter asset title">
                        </div>
                    </div>
                    
                    <div class="field">
                        <label class="label"><fmt:message key="Description" /></label>
                        <div class="control">
                            <textarea class="textarea" name="jcr:description" rows="3"
                                      placeholder="Enter asset description">${sling:encode(assetContent.valueMap['jcr:description'],'HTML')}</textarea>
                        </div>
                    </div>
                    
                    <div class="field">
                        <label class="label"><fmt:message key="Alt Text" /></label>
                        <div class="control">
                            <input class="input" type="text" name="alt" 
                                   value="${sling:encode(assetContent.valueMap['alt'],'HTML_ATTR')}"
                                   placeholder="Alternative text for accessibility">
                        </div>
                        <p class="help"><fmt:message key="Used for accessibility and SEO" /></p>
                    </div>
                </div>
                
                <%-- Copyright & Attribution --%>
                <div class="box">
                    <h4 class="title is-5"><fmt:message key="Copyright & Attribution" /></h4>
                    
                    <div class="field">
                        <label class="label"><fmt:message key="Copyright" /></label>
                        <div class="control">
                            <input class="input" type="text" name="dc:rights" 
                                   value="${sling:encode(assetContent.valueMap['dc:rights'],'HTML_ATTR')}"
                                   placeholder="© 2024 Company Name">
                        </div>
                    </div>
                    
                    <div class="field">
                        <label class="label"><fmt:message key="Creator / Author" /></label>
                        <div class="control">
                            <input class="input" type="text" name="dc:creator" 
                                   value="${sling:encode(assetContent.valueMap['dc:creator'],'HTML_ATTR')}"
                                   placeholder="Photographer or creator name">
                        </div>
                    </div>
                    
                    <div class="field">
                        <label class="label"><fmt:message key="Source / Credit" /></label>
                        <div class="control">
                            <input class="input" type="text" name="dc:source" 
                                   value="${sling:encode(assetContent.valueMap['dc:source'],'HTML_ATTR')}"
                                   placeholder="Source attribution">
                        </div>
                    </div>
                </div>
                
                <%-- Tags (if taxonomy support exists) --%>
                <div class="box">
                    <h4 class="title is-5"><fmt:message key="Tags & Categories" /></h4>
                    
                    <div class="field">
                        <label class="label"><fmt:message key="Keywords" /></label>
                        <div class="control">
                            <input class="input" type="text" name="keywords" 
                                   value="${sling:encode(assetContent.valueMap['keywords'],'HTML_ATTR')}"
                                   placeholder="Comma-separated keywords">
                        </div>
                        <p class="help"><fmt:message key="Separate multiple keywords with commas" /></p>
                    </div>
                </div>
                
                <%-- Form Actions --%>
                <div class="field is-grouped">
                    <div class="control">
                        <button class="button is-primary" type="submit">${saveBtn}</button>
                    </div>
                    <div class="control">
                        <a class="button close" href="#">${cancelBtn}</a>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>
