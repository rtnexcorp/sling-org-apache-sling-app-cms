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

<c:set var="imagePath" value="${slingRequest.requestPathInfo.suffix}" />
<c:set var="suffixRes" value="${slingRequest.requestPathInfo.suffixResource}" />
<c:set var="fileName" value="${not empty suffixRes ? suffixRes.name : 'Image'}" />
<c:set var="mimeType" value="${suffixRes.valueMap['jcr:content/jcr:mimeType']}" />
<c:set var="isImage" value="${fn:startsWith(mimeType, 'image/')}" />

<sling:adaptTo adaptable="${slingRequest}" adaptTo="org.apache.sling.thumbnails.RenderedResource" var="rendered" />

<!-- Load transformations -->
<sling:findResources 
    query="SELECT * FROM [nt:unstructured] 
           WHERE ISDESCENDANTNODE([/conf/global/settings/thumbnails]) 
           AND [sling:resourceType]='sling/thumbnails/transformation'"
    language="JCR-SQL2"
    var="transformations" />

<div class="image-preview-modal">
<div class="columns">
    <!-- LEFT: IMAGE PREVIEW -->
    <div class="column is-three-quarters">
        <h4 class="title is-5"><fmt:message key="Preview" /></h4>

        <c:choose>
            <c:when test="${isImage}">
                <div class="box has-background-dark has-text-centered image-preview-container">
                    <!-- Main preview element that JS will update -->
                    <img id="previewImage"
                         loading="lazy"
                         src="${sling:encode(imagePath,'HTML_ATTR')}"
                         alt="${sling:encode(fileName,'HTML_ATTR')}"
                         class="preview-image" />
                </div>

                <p class="help has-text-centered"><fmt:message key="Click image links to preview without refresh" /></p>
            </c:when>

            <c:otherwise>
                <div class="notification is-warning">
                    <p><strong>Cannot preview:</strong> ${sling:encode(fileName,'HTML')}</p>
                    <p>This file is not an image: ${sling:encode(mimeType,'HTML')}</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- RIGHT: INFO + LINKS -->
    <div class="column is-one-quarter">

        <!-- Info -->
        <h4 class="title is-5">Info</h4>
        <table class="table is-fullwidth is-narrow">
            <tbody>
                <tr>
                    <th>File Name</th>
                    <td>${sling:encode(fileName,'HTML')}</td>
                </tr>
                <tr>
                    <th>MIME Type</th>
                    <td><span class="tag">${sling:encode(mimeType,'HTML')}</span></td>
                </tr>
            </tbody>
        </table>

        <!-- Renditions -->
        <c:if test="${isImage}">
            <h4 class="title is-6 mt-4">Renditions</h4>
            <div class="buttons are-small">

                <!-- Original -->
                <a href="${sling:encode(imagePath,'HTML_ATTR')}"
                   class="button is-info preview-link"
                   data-image="${sling:encode(imagePath,'HTML_ATTR')}">
                    <span class="icon"><span class="jam jam-picture"></span></span>
                    <span>Original</span>
                </a>

                <!-- Auto renditions -->
                <c:if test="${not empty rendered.supportedRenditions}">
                    <c:forEach var="rendition" items="${rendered.supportedRenditions}">
                        <a href="${sling:encode(imagePath,'HTML_ATTR')}.transform/${sling:encode(rendition,'HTML_ATTR')}.png"
                           class="button is-link is-outlined preview-link"
                           data-image="${sling:encode(imagePath,'HTML_ATTR')}.transform/${sling:encode(rendition,'HTML_ATTR')}.png">
                            <span class="icon"><span class="jam jam-picture"></span></span>
                            <span>${sling:encode(rendition,'HTML')}</span>
                        </a>
                    </c:forEach>
                </c:if>
            </div>

            <!-- All Transformations -->
            <c:if test="${not empty transformations}">
                <h4 class="title is-6 mt-4">All Transformations</h4>
                <div class="buttons are-small">
                    <c:forEach var="transformation" items="${transformations}">
                        <a href="${sling:encode(imagePath,'HTML_ATTR')}.transform/${sling:encode(transformation.valueMap.name,'HTML_ATTR')}.png"
                           class="button is-primary is-outlined preview-link"
                           data-image="${sling:encode(imagePath,'HTML_ATTR')}.transform/${sling:encode(transformation.valueMap.name,'HTML_ATTR')}.png">
                            <span class="icon"><span class="jam jam-picture"></span></span>
                            <span>${sling:encode(transformation.valueMap.name,'HTML')}</span>
                        </a>
                    </c:forEach>
                </div>
            </c:if>
        </c:if>

        <!-- Download & Close -->
        <div class="field is-grouped mt-4">
            <p class="control">
                <a href="${sling:encode(imagePath,'HTML_ATTR')}"
                   download="${sling:encode(fileName,'HTML_ATTR')}"
                   class="button is-success">
                    <span class="icon"><span class="jam jam-download"></span></span>
                    <span>Download</span>
                </a>
            </p>
            <p class="control">
                <button type="button" class="button close"><fmt:message key="Close" /></button>
            </p>
        </div>
    </div>
</div>
</div>
