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
 * Boolean Field Component - Renders a toggle switch for true/false values
 --%>
<c:set var="fieldName_value" value="${sling:encode(not empty fieldName ? fieldName : properties.name,'HTML_ATTR')}" />
<c:set var="fieldId" value="${sling:encode(properties.name,'HTML_ATTR')}" />
<%-- Check if value is true from: loaded value, or default value property --%>
<c:set var="isChecked" value="${value eq 'true' || value == true || (empty value && (properties.value eq 'true' || properties.value == true))}" />

<label class="toggle-switch">
    <input type="checkbox" name="${fieldName_value}" id="${fieldId}" value="true" ${required} ${disabled} ${isChecked ? 'checked="checked"' : ''} />
    <span class="toggle-slider"></span>
    <c:if test="${not empty properties.checkboxLabel}">
        <span class="toggle-text"><sling:encode value="${properties.checkboxLabel}" mode="HTML" /></span>
    </c:if>
</label>