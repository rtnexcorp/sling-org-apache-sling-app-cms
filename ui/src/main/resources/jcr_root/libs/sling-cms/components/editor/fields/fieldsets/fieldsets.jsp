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
<div class="editor-fieldsets">
    <c:forEach var="fieldset" items="${sling:listChildren(resource)}">
        <c:set var="fieldsetProps" value="${sling:adaptTo(fieldset,'org.apache.sling.api.resource.ValueMap')}" />
        <fieldset class="editor-fieldset">
            <c:if test="${not empty fieldsetProps.title}">
                <legend><fmt:message key="${fieldsetProps.title}" /></legend>
            </c:if>
            <sling:include resource="${fieldset}" resourceType="sling-cms/components/general/container" />
        </fieldset>
    </c:forEach>
</div>
