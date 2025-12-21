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
<div class="editor-accordions">
    <c:forEach var="accordion" items="${sling:listChildren(resource)}" varStatus="status">
        <c:set var="accordionProps" value="${sling:adaptTo(accordion,'org.apache.sling.api.resource.ValueMap')}" />
        <div class="editor-accordion">
            <div class="editor-accordion__header${status.first ? ' is-open' : ''}" tabindex="0">
                <fmt:message key="${accordionProps.title}" />
            </div>
            <div class="editor-accordion__content${status.first ? ' is-open' : ''}">
                <sling:include resource="${accordion}" resourceType="sling-cms/components/general/container" />
            </div>
        </div>
    </c:forEach>
</div>
