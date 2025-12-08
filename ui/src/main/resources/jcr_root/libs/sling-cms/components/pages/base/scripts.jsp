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
<%-- Load all three JavaScript bundles as ES modules --%>
<c:forEach var="jsFile" items="${branding.js}">
    <script type="module" src="${sling:encode(jsFile, 'HTML_ATTR')}"></script>
</c:forEach>
<%-- Fallback for cms.bundle if js array is not defined --%>
<c:if test="${empty branding.js}">
    <script type="module" src="/static/sling-cms/js/cms.bundle.min.js"></script>
    <script type="module" src="/static/sling-cms/js/editor.bundle.min.js"></script>
    <script type="module" src="/static/sling-cms/js/starter.bundle.min.js"></script>
</c:if>