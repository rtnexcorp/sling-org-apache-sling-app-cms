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
<%@include file="/libs/sling-cms/global.jsp" %>
<%-- 
    Page Context Demo Component
    
    This component demonstrates how to check instance type and page mode
    using the PageContext interface in JSP.
    
    The 'slingPageContext' variable is automatically available from global.jsp
--%>

<div class="pagecontext-demo">
    <h3>Page Context Demo (JSP)</h3>
    
    <table class="table is-bordered is-striped">
        <thead>
            <tr>
                <th>Property</th>
                <th>Value</th>
            </tr>
        </thead>
        <tbody>
            <%-- Page Mode --%>
            <tr>
                <td>Page Mode</td>
                <td>${slingPageContext.pageMode}</td>
            </tr>
            <tr>
                <td>Is Edit Mode</td>
                <td>
                    <c:choose>
                        <c:when test="${slingPageContext.editMode}">
                            <span class="tag is-warning">Yes - Edit Mode</span>
                        </c:when>
                        <c:otherwise>
                            <span class="tag is-info">No - Preview Mode</span>
                        </c:otherwise>
                    </c:choose>
                </td>
            </tr>
            <tr>
                <td>Is Preview Mode</td>
                <td>
                    <c:choose>
                        <c:when test="${slingPageContext.previewMode}">
                            <span class="tag is-info">Yes</span>
                        </c:when>
                        <c:otherwise>
                            <span class="tag is-light">No</span>
                        </c:otherwise>
                    </c:choose>
                </td>
            </tr>
            
            <%-- Instance Type --%>
            <tr>
                <td>Instance Type</td>
                <td>
                    <c:choose>
                        <c:when test="${slingPageContext.author}">
                            <span class="tag is-primary">AUTHOR</span>
                        </c:when>
                        <c:when test="${slingPageContext.renderer}">
                            <span class="tag is-success">RENDERER</span>
                        </c:when>
                        <c:otherwise>
                            <span class="tag is-dark">STANDALONE</span>
                        </c:otherwise>
                    </c:choose>
                </td>
            </tr>
            <tr>
                <td>Is Author</td>
                <td>
                    <c:choose>
                        <c:when test="${slingPageContext.author}"><span class="tag is-success">true</span></c:when>
                        <c:otherwise><span class="tag is-light">false</span></c:otherwise>
                    </c:choose>
                </td>
            </tr>
            <tr>
                <td>Is Renderer</td>
                <td>
                    <c:choose>
                        <c:when test="${slingPageContext.renderer}"><span class="tag is-success">true</span></c:when>
                        <c:otherwise><span class="tag is-light">false</span></c:otherwise>
                    </c:choose>
                </td>
            </tr>
            <tr>
                <td>Is Standalone</td>
                <td>
                    <c:choose>
                        <c:when test="${slingPageContext.standalone}"><span class="tag is-success">true</span></c:when>
                        <c:otherwise><span class="tag is-light">false</span></c:otherwise>
                    </c:choose>
                </td>
            </tr>
            
            <%-- Computed Properties --%>
            <tr>
                <td>Is Authoring Enabled</td>
                <td>
                    <c:choose>
                        <c:when test="${slingPageContext.authoringEnabled}"><span class="tag is-success">true</span></c:when>
                        <c:otherwise><span class="tag is-light">false</span></c:otherwise>
                    </c:choose>
                </td>
            </tr>
            <tr>
                <td>Is Publish Mode</td>
                <td>
                    <c:choose>
                        <c:when test="${slingPageContext.publishMode}"><span class="tag is-success">true</span></c:when>
                        <c:otherwise><span class="tag is-light">false</span></c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </tbody>
    </table>
    
    <%-- Conditional content based on mode --%>
    <div class="mt-4">
        <h4>Conditional Content:</h4>
        
        <c:if test="${slingPageContext.editMode}">
            <div class="notification is-warning">
                <strong>Edit Mode Only:</strong> This content is only visible in edit mode.
                You can use this to show editing instructions or placeholders.
            </div>
        </c:if>
        
        <c:if test="${slingPageContext.previewMode}">
            <div class="notification is-info">
                <strong>Preview Mode Only:</strong> This content is only visible in preview mode.
                Use this for content that should not appear during editing.
            </div>
        </c:if>
        
        <c:if test="${slingPageContext.author}">
            <div class="notification is-primary">
                <strong>Author Instance:</strong> Running on the Author instance.
            </div>
        </c:if>
        
        <c:if test="${slingPageContext.renderer}">
            <div class="notification is-success">
                <strong>Renderer Instance:</strong> Running on the Renderer/Publish instance.
            </div>
        </c:if>
        
        <c:if test="${slingPageContext.standalone}">
            <div class="notification is-dark">
                <strong>Standalone Instance:</strong> Running on a Standalone instance.
            </div>
        </c:if>
    </div>
</div>
