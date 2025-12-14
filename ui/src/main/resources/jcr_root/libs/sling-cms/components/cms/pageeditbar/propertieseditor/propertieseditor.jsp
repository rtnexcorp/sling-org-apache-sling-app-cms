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

<%-- NOTE: Guard against missing suffix resource / jcr:content to avoid NPEs during include. --%>
<c:set var="contentResource" value="${sling:getRelativeResource(slingRequest.requestPathInfo.suffixResource,'jcr:content')}" />

<c:choose>
    <c:when test="${contentResource != null}">
        <sling:adaptTo adaptable="${contentResource}" adaptTo="org.apache.sling.cms.EditableResource" var="editable" />
        <c:if test="${editable != null && not empty editable.editPath}">
            <sling:include path="${editable.editPath}" resourceType="sling-cms/components/editor/slingform" replaceSuffix="${editable.resource.path}" />
        </c:if>
        <c:if test="${editable == null || empty editable.editPath}">
            <div class="notification is-warning is-light">
                <fmt:message key="Unable to open properties editor for this resource." />
            </div>
        </c:if>
    </c:when>
    <c:otherwise>
        <div class="notification is-warning is-light">
            <fmt:message key="Unable to open properties editor: no resource selected." />
        </div>
    </c:otherwise>
</c:choose>