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
<c:set var="fileName" value="${not empty suffixRes ? suffixRes.name : 'File'}" />
<c:set var="mimeType" value="${suffixRes.valueMap['jcr:content/jcr:mimeType']}" />
<c:set var="isImage" value="${fn:startsWith(mimeType, 'image/')}" />
<c:set var="isVideo" value="${fn:startsWith(mimeType, 'video/')}" />
<c:set var="isPdf" value="${mimeType == 'application/pdf'}" />
<c:set var="isWord" value="${mimeType == 'application/msword' || mimeType == 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'}" />
<c:set var="isExcel" value="${mimeType == 'application/vnd.ms-excel' || mimeType == 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'}" />
<c:set var="isPowerPoint" value="${mimeType == 'application/vnd.ms-powerpoint' || mimeType == 'application/vnd.openxmlformats-officedocument.presentationml.presentation'}" />
<c:set var="isDocument" value="${isPdf || isWord || isExcel || isPowerPoint}" />
<c:set var="isText" value="${fn:startsWith(mimeType, 'text/') || mimeType == 'application/json' || mimeType == 'application/xml' || mimeType == 'application/javascript'}" />

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

            <c:when test="${isVideo}">
                <div class="box has-background-dark has-text-centered video-preview-container">
                    <video id="previewVideo" 
                           controls 
                           preload="metadata"
                           style="max-width: 100%; max-height: 500px;">
                        <source src="${sling:encode(imagePath,'HTML_ATTR')}" type="${sling:encode(mimeType,'HTML_ATTR')}" />
                        <fmt:message key="Your browser does not support video playback." />
                    </video>
                </div>
                <p class="help has-text-centered"><fmt:message key="Use video controls to play, pause, and seek" /></p>
            </c:when>

            <c:when test="${isPdf}">
                <div class="box has-background-light pdf-preview-container">
                    <iframe 
                        src="${sling:encode(imagePath,'HTML_ATTR')}" 
                        style="width: 100%; height: 500px; border: none;"
                        title="${sling:encode(fileName,'HTML_ATTR')}">
                    </iframe>
                </div>
                <p class="help has-text-centered"><fmt:message key="PDF preview - scroll to navigate pages" /></p>
            </c:when>

            <c:when test="${isWord}">
                <div class="box has-background-light document-preview-container">
                    <div id="docxPreviewLoading" class="has-text-centered py-6">
                        <span class="icon is-large">
                            <span class="jam jam-refresh jam-spin"></span>
                        </span>
                        <p>Loading document...</p>
                    </div>
                    <div id="docxPreviewContent" style="max-height: 500px; overflow: auto; padding: 1rem; display: none;"></div>
                    <div id="docxPreviewError" class="has-text-centered py-6" style="display: none;">
                        <span class="icon is-large has-text-warning">
                            <span class="jam jam-alert" style="font-size: 2rem;"></span>
                        </span>
                        <p class="mt-2">Could not preview document content</p>
                        <p class="help">Use Download button to view the file</p>
                    </div>
                </div>
                <p class="help has-text-centered"><fmt:message key="Word document preview (formatting may vary)" /></p>
                <script src="https://cdn.jsdelivr.net/npm/mammoth@1.6.0/mammoth.browser.min.js"></script>
                <script>
                    (function() {
                        var docxUrl = "<c:out value='${imagePath}' escapeXml='true'/>";
                        fetch(docxUrl)
                            .then(function(response) { return response.arrayBuffer(); })
                            .then(function(arrayBuffer) {
                                return mammoth.convertToHtml({arrayBuffer: arrayBuffer});
                            })
                            .then(function(result) {
                                document.getElementById('docxPreviewLoading').style.display = 'none';
                                document.getElementById('docxPreviewContent').innerHTML = result.value;
                                document.getElementById('docxPreviewContent').style.display = 'block';
                            })
                            .catch(function(err) {
                                console.error('DOCX preview error:', err);
                                document.getElementById('docxPreviewLoading').style.display = 'none';
                                document.getElementById('docxPreviewError').style.display = 'block';
                            });
                    })();
                </script>
            </c:when>

            <c:when test="${isExcel}">
                <div class="box has-background-light document-preview-container">
                    <div id="xlsxPreviewLoading" class="has-text-centered py-6">
                        <span class="icon is-large">
                            <span class="jam jam-refresh jam-spin"></span>
                        </span>
                        <p>Loading spreadsheet...</p>
                    </div>
                    <div id="xlsxPreviewContent" style="max-height: 500px; overflow: auto; display: none;"></div>
                    <div id="xlsxPreviewError" class="has-text-centered py-6" style="display: none;">
                        <span class="icon is-large has-text-warning">
                            <span class="jam jam-alert" style="font-size: 2rem;"></span>
                        </span>
                        <p class="mt-2">Could not preview spreadsheet content</p>
                        <p class="help">Use Download button to view the file</p>
                    </div>
                </div>
                <p class="help has-text-centered"><fmt:message key="Excel spreadsheet preview (first sheet shown)" /></p>
                <script src="https://cdn.jsdelivr.net/npm/xlsx@0.18.5/dist/xlsx.full.min.js"></script>
                <script>
                    (function() {
                        var xlsxUrl = "<c:out value='${imagePath}' escapeXml='true'/>";
                        fetch(xlsxUrl)
                            .then(function(response) { return response.arrayBuffer(); })
                            .then(function(arrayBuffer) {
                                var workbook = XLSX.read(arrayBuffer, {type: 'array'});
                                var firstSheet = workbook.Sheets[workbook.SheetNames[0]];
                                var html = XLSX.utils.sheet_to_html(firstSheet, {editable: false});
                                document.getElementById('xlsxPreviewLoading').style.display = 'none';
                                document.getElementById('xlsxPreviewContent').innerHTML = html;
                                document.getElementById('xlsxPreviewContent').style.display = 'block';
                                // Add Bulma table styling
                                var table = document.querySelector('#xlsxPreviewContent table');
                                if (table) {
                                    table.className = 'table is-bordered is-striped is-narrow is-fullwidth';
                                }
                            })
                            .catch(function(err) {
                                console.error('XLSX preview error:', err);
                                document.getElementById('xlsxPreviewLoading').style.display = 'none';
                                document.getElementById('xlsxPreviewError').style.display = 'block';
                            });
                    })();
                </script>
            </c:when>

            <c:when test="${isPowerPoint}">
                <div class="box has-background-light has-text-centered document-preview-container">
                    <div class="py-6">
                        <span class="icon is-large has-text-warning">
                            <span class="jam jam-presentation" style="font-size: 4rem;"></span>
                        </span>
                        <p class="title is-4 mt-4">${sling:encode(fileName,'HTML')}</p>
                        <p class="subtitle is-6">Microsoft PowerPoint Presentation</p>
                        <div class="tags is-centered mt-3">
                            <span class="tag is-warning is-medium">${sling:encode(mimeType,'HTML')}</span>
                        </div>
                    </div>
                </div>
                <p class="help has-text-centered"><fmt:message key="PowerPoint presentations cannot be previewed directly - use Download to view" /></p>
            </c:when>

            <c:when test="${isText}">
                <div class="box has-background-light text-preview-container">
                    <pre style="max-height: 500px; overflow: auto; white-space: pre-wrap; word-wrap: break-word;"><sling:include path="${imagePath}" resourceType="sling/servlet/default" /></pre>
                </div>
                <p class="help has-text-centered"><fmt:message key="Text file preview" /></p>
            </c:when>

            <c:otherwise>
                <div class="box has-background-light has-text-centered document-preview-container">
                    <div class="py-6">
                        <span class="icon is-large has-text-grey">
                            <span class="jam jam-files" style="font-size: 4rem;"></span>
                        </span>
                        <p class="title is-4 mt-4">${sling:encode(fileName,'HTML')}</p>
                        <p class="subtitle is-6">File Preview Not Available</p>
                        <div class="tags is-centered mt-3">
                            <span class="tag is-dark is-medium">${sling:encode(mimeType,'HTML')}</span>
                        </div>
                    </div>
                </div>
                <p class="help has-text-centered"><fmt:message key="This file type cannot be previewed - use Download to view" /></p>
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
        <c:if test="${isImage || isVideo || isPdf}">
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

            <!-- All Transformations (for images only) -->
            <c:if test="${isImage && not empty transformations}">
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
