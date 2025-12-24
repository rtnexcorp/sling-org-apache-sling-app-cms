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
<div class="handler-details">
    <span class="badge badge-success mr-2">Resize</span>
    <dl class="row mb-0 mt-2">
        <c:if test="${not empty properties.width}">
            <dt class="col-sm-4">Width</dt>
            <dd class="col-sm-8"><strong><sling:encode value="${properties.width}" mode="HTML" />px</strong></dd>
        </c:if>
        <c:if test="${not empty properties.height}">
            <dt class="col-sm-4">Height</dt>
            <dd class="col-sm-8"><strong><sling:encode value="${properties.height}" mode="HTML" />px</strong></dd>
        </c:if>
        <dt class="col-sm-4">Keep Aspect Ratio</dt>
        <dd class="col-sm-8">
            <c:choose>
                <c:when test="${properties.keepAspectRatio == true}">
                    <span class="badge badge-info">Yes</span>
                </c:when>
                <c:otherwise>
                    <span class="badge badge-secondary">No</span>
                </c:otherwise>
            </c:choose>
        </dd>
    </dl>
</div>