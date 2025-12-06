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
<c:if test="${not empty properties.bookTitle}">
    <article class="book-card">
        <c:if test="${not empty properties.coverImage}">
            <div class="book-cover">
                <img src="${sling:encode(properties.coverImage,'HTML_ATTR')}" 
                     alt="${sling:encode(properties.bookTitle,'HTML_ATTR')}" />
            </div>
        </c:if>
        <div class="book-details">
            <h2 class="book-title">
                <sling:encode value="${properties.bookTitle}" mode="HTML" />
            </h2>
            <c:if test="${not empty properties.subtitle}">
                <h3 class="book-subtitle">
                    <sling:encode value="${properties.subtitle}" mode="HTML" />
                </h3>
            </c:if>
            
            <%-- Authors Section --%>
            <c:set var="authorsResource" value="${sling:getRelativeResource(resource, 'authors')}" />
            <c:if test="${authorsResource != null}">
                <div class="book-authors">
                    <span class="authors-label">By: </span>
                    <c:forEach var="author" items="${sling:listChildren(authorsResource)}" varStatus="status">
                        <c:set var="authorProps" value="${sling:adaptTo(author,'org.apache.sling.api.resource.ValueMap')}" />
                        <span class="author">
                            <span class="author-name">
                                <sling:encode value="${authorProps.authorName}" mode="HTML" />
                            </span>
                            <c:if test="${not empty authorProps.role && authorProps.role != 'author'}">
                                <span class="author-role">(<sling:encode value="${authorProps.role}" mode="HTML" />)</span>
                            </c:if>
                            <c:if test="${!status.last}">, </c:if>
                        </span>
                    </c:forEach>
                </div>
            </c:if>
            
            <div class="book-meta">
                <c:if test="${not empty properties.publishingYear}">
                    <span class="book-year">
                        <strong>Year:</strong> <sling:encode value="${properties.publishingYear}" mode="HTML" />
                    </span>
                </c:if>
                <c:if test="${not empty properties.publisher}">
                    <span class="book-publisher">
                        <strong>Publisher:</strong> <sling:encode value="${properties.publisher}" mode="HTML" />
                    </span>
                </c:if>
                <c:if test="${not empty properties.isbn}">
                    <span class="book-isbn">
                        <strong>ISBN:</strong> <sling:encode value="${properties.isbn}" mode="HTML" />
                    </span>
                </c:if>
            </div>
            
            <c:if test="${not empty properties.description}">
                <div class="book-description">
                    <p><sling:encode value="${properties.description}" mode="HTML" /></p>
                </div>
            </c:if>
            
            <%-- Detailed Author Information --%>
            <c:if test="${authorsResource != null}">
                <div class="book-authors-detail">
                    <h4>About the Authors</h4>
                    <c:forEach var="author" items="${sling:listChildren(authorsResource)}">
                        <c:set var="authorProps" value="${sling:adaptTo(author,'org.apache.sling.api.resource.ValueMap')}" />
                        <div class="author-detail">
                            <h5 class="author-detail-name">
                                <sling:encode value="${authorProps.authorName}" mode="HTML" />
                                <c:if test="${not empty authorProps.role}">
                                    <span class="author-detail-role">
                                        (<sling:encode value="${authorProps.role}" mode="HTML" />)
                                    </span>
                                </c:if>
                            </h5>
                            <c:if test="${not empty authorProps.bio}">
                                <p class="author-detail-bio">
                                    <sling:encode value="${authorProps.bio}" mode="HTML" />
                                </p>
                            </c:if>
                            <div class="author-detail-contact">
                                <c:if test="${not empty authorProps.email}">
                                    <a href="mailto:${sling:encode(authorProps.email,'HTML_ATTR')}" class="author-email">
                                        <sling:encode value="${authorProps.email}" mode="HTML" />
                                    </a>
                                </c:if>
                                <c:if test="${not empty authorProps.website}">
                                    <a href="${sling:encode(authorProps.website,'HTML_ATTR')}" 
                                       target="_blank" 
                                       rel="noopener noreferrer" 
                                       class="author-website">
                                        Website
                                    </a>
                                </c:if>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:if>
        </div>
    </article>
</c:if>
