/*
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
 */
package org.apache.sling.cms;

import org.apache.sling.cms.publication.INSTANCE_TYPE;

/**
 * Provides context information about the current page request including
 * instance type (Author/Renderer/Standalone) and page mode (Edit/Preview).
 *
 * <p>This interface can be used in Sling Models, JSP, and HTL to check
 * the current execution context.</p>
 *
 * <h3>Usage in Sling Model:</h3>
 * <pre>
 * &#64;Model(adaptables = SlingHttpServletRequest.class)
 * public class MyComponent {
 *     &#64;Inject
 *     private PageContext pageContext;
 *
 *     public boolean isAuthorMode() {
 *         return pageContext.isAuthor();
 *     }
 * }
 * </pre>
 *
 * <h3>Usage in HTL:</h3>
 * <pre>
 * &lt;sly data-sly-use.ctx="org.apache.sling.cms.PageContext"&gt;
 *     &lt;sly data-sly-test="${ctx.editMode}"&gt;Edit Mode&lt;/sly&gt;
 *     &lt;sly data-sly-test="${ctx.author}"&gt;Author Instance&lt;/sly&gt;
 * &lt;/sly&gt;
 * </pre>
 *
 * <h3>Usage in JSP:</h3>
 * <pre>
 * &lt;sling:adaptTo adaptable="${slingRequest}" adaptTo="org.apache.sling.cms.PageContext" var="ctx"/&gt;
 * &lt;c:if test="${ctx.editMode}"&gt;Edit Mode&lt;/c:if&gt;
 * </pre>
 */
public interface PageContext {

    /**
     * Page mode enumeration
     */
    enum PageMode {
        /** Page is being edited in the CMS editor */
        EDIT,
        /** Page is being previewed (no edit controls) */
        PREVIEW
    }

    /**
     * Get the current page mode
     * @return PageMode.EDIT if in edit mode, PageMode.PREVIEW otherwise
     */
    PageMode getPageMode();

    /**
     * Check if the page is in edit mode
     * @return true if edit mode is enabled
     */
    boolean isEditMode();

    /**
     * Check if the page is in preview mode (not edit mode)
     * @return true if in preview mode
     */
    boolean isPreviewMode();

    /**
     * Get the current instance type
     * @return the instance type (AUTHOR, RENDERER, or STANDALONE)
     */
    INSTANCE_TYPE getInstanceType();

    /**
     * Check if running on an Author instance
     * @return true if instance type is AUTHOR
     */
    boolean isAuthor();

    /**
     * Check if running on a Renderer instance
     * @return true if instance type is RENDERER
     */
    boolean isRenderer();

    /**
     * Check if running on a Standalone instance
     * @return true if instance type is STANDALONE
     */
    boolean isStandalone();

    /**
     * Check if the page should show authoring controls.
     * This is true when in edit mode on an Author or Standalone instance.
     * @return true if authoring controls should be shown
     */
    boolean isAuthoringEnabled();

    /**
     * Check if the page is being served to end users.
     * This is true when on a Renderer instance or in preview mode.
     * @return true if serving to end users
     */
    boolean isPublishMode();
}
