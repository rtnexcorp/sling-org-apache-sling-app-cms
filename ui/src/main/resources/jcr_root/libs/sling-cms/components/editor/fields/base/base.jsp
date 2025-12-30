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
<%-- Check for editResourcePath attribute first (for Edit Properties modal) --%>
<c:set var="editPath" value="${requestScope.editResourcePath}" />
<c:if test="${empty editPath && slingRequest.requestPathInfo.suffix != null}">
    <c:set var="editPath" value="${slingRequest.requestPathInfo.suffix}" />
</c:if>
<c:if test="${not empty editPath}">
    <sling:getResource path="${editPath}" var="editedResource" />
    <c:set var="editProperties" value="${sling:adaptTo(editedResource,'org.apache.sling.api.resource.ValueMap')}" scope="request"/>
</c:if>
<%-- Handle multifield item context - get properties from the multifield item resource --%>
<c:choose>
    <c:when test="${not empty multifieldItemResource}">
        <c:set var="itemProperties" value="${sling:adaptTo(multifieldItemResource,'org.apache.sling.api.resource.ValueMap')}" scope="request" />
    </c:when>
    <c:otherwise>
        <c:set var="itemProperties" value="${editProperties}" scope="request" />
    </c:otherwise>
</c:choose>
<c:choose>
    <c:when test="${properties.required}">
        <c:set var="required" value="required='required'" scope="request" />
    </c:when>
    <c:otherwise>
        <c:set var="required" value="" scope="request" />
    </c:otherwise>
</c:choose>
<c:choose>
    <c:when test="${properties.disabled}">
        <c:set var="disabled" value="disabled='disabled'" scope="request" />
    </c:when>
    <c:otherwise>
        <c:set var="disabled" value="" scope="request" />
    </c:otherwise>
</c:choose>
<c:choose>
    <c:when test="${properties.skipload}">
        <c:set var="value" value="" scope="request" />
    </c:when>
    <c:when test="${empty itemProperties[properties.name] && properties.defaultValue}">
        <c:set var="value" value="${properties.defaultValue}" scope="request" />
    </c:when>
    <c:otherwise>
        <c:set var="value" value="${itemProperties[properties.name]}" scope="request" />
    </c:otherwise>
</c:choose>
<%-- Set the field name - prefix with multifield path if in multifield context --%>
<c:choose>
    <c:when test="${not empty multifieldBaseName && not empty multifieldItemName}">
        <c:set var="fieldName" value="${multifieldBaseName}/${multifieldItemName}/${properties.name}" scope="request" />
    </c:when>
    <c:otherwise>
        <c:set var="fieldName" value="${properties.name}" scope="request" />
    </c:otherwise>
</c:choose>
<c:forEach var="event" items="${sling:getRelativeResource(resource,'./events').valueMap}">
    <c:if test="${!fn:contains(event.key,':')}">
        <c:set var="events" value="${events},${event.key}" />
    </c:if>
</c:forEach>
<%-- Build toggle-value class and attributes if configured --%>
<c:set var="toggleClass" value="" />
<c:set var="toggleAttrs" value="" />
<c:if test="${not empty properties.toggleSource && not empty properties.toggleValue}">
    <c:set var="toggleClass" value="is-hidden toggle-value" />
    <c:set var="toggleAttrs" value="data-toggle-source=\"${sling:encode(properties.toggleSource,'HTML_ATTR')}\" data-toggle-value=\"${sling:encode(properties.toggleValue,'HTML_ATTR')}\"" />
</c:if>
<div class="field ${toggleClass}" data-events="${events}" data-path="${sling:encode(resource.path,'HTML_ATTR')}" ${toggleAttrs}>
    <c:if test="${not empty properties.label}">
        <label class="label" for="${sling:encode(properties.name,'HTML_ATTR')}">
            <fmt:message key="${properties.label}" var="label" />
            <sling:encode value="${label}" mode="HTML" />
            <c:if test="${properties.required}"><span class="has-text-danger">*</span></c:if>
        </label>
    </c:if>
    <div class="control">
    <sling:call script="field.jsp" />

    <c:if test="${not empty properties.description}">
        <p class="help">
            <fmt:message key="${properties.description}" var="description" />
            <sling:encode value="${description}" mode="HTML" />
        </p>
    </c:if>
    </div>
</div>