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
<%-- 
 * Radio Button Field Component - Renders a group of radio buttons
 --%>
<c:set var="fieldName_value" value="${sling:encode(not empty fieldName ? fieldName : properties.name,'HTML_ATTR')}" />
<c:set var="fieldId" value="${sling:encode(properties.name,'HTML_ATTR')}" />

<div class="control">
    <c:forEach var="option" items="${properties.options}">
        <c:set var="label" value="${fn:split(option,'=')[0]}" />
        <c:set var="optVal" value="${fn:split(option,'=')[1]}" />
        <c:set var="checked" value="${value eq optVal ? 'checked=\"checked\"' : ''}" />
        <label class="radio">
            <input type="radio" name="${fieldName_value}" value="${sling:encode(optVal,'HTML_ATTR')}" id="${fieldId}_${fn:replace(optVal,' ','_')}" ${required} ${disabled} ${checked} />
            <fmt:message key="${label}" var="labelMessage" />
            <sling:encode value="${labelMessage}" mode="HTML" />
        </label>
    </c:forEach>
</div>
