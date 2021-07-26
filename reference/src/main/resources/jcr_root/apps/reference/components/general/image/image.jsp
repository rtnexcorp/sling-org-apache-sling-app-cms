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
<c:if test="${not empty properties.src}">
    <c:if test="${not empty properties.transformation}">
        <c:choose>
            <c:when test="${properties.transformationFormat == 'default'}">
                <c:set var="transform" value=".transform/${properties.transformation}" />
            </c:when>
            <c:otherwise>
                <c:set var="transform" value=".transform/${properties.transformation}.${properties.transformationFormat}" />
            </c:otherwise>
        </c:choose>
    </c:if>
    <img src="${properties.src}${transform}" alt="${properties.alt}" class="${properties.imageClass}" />
</c:if>