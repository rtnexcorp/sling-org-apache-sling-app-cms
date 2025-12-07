/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.cms.reference.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.cms.PageContext;
import org.apache.sling.cms.publication.INSTANCE_TYPE;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

/**
 * Example Sling Model demonstrating how to use PageContext to check
 * instance type and page mode.
 * 
 * <h2>Usage in HTL:</h2>
 * <pre>
 * &lt;sly data-sly-use.demo="org.apache.sling.cms.reference.models.PageContextDemoModel"&gt;
 *     &lt;p&gt;Edit Mode: ${demo.editMode}&lt;/p&gt;
 *     &lt;p&gt;Instance: ${demo.instanceTypeName}&lt;/p&gt;
 *     &lt;sly data-sly-test="${demo.shouldShowEditControls}"&gt;
 *         &lt;!-- Show edit button --&gt;
 *     &lt;/sly&gt;
 * &lt;/sly&gt;
 * </pre>
 * 
 * <h2>Usage in JSP:</h2>
 * <pre>
 * &lt;sling:adaptTo adaptable="${slingRequest}" 
 *                adaptTo="org.apache.sling.cms.reference.models.PageContextDemoModel" 
 *                var="demo"/&gt;
 * &lt;c:if test="${demo.editMode}"&gt;Edit Mode&lt;/c:if&gt;
 * </pre>
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class PageContextDemoModel {

    @Self
    private PageContext pageContext;

    /**
     * Check if in edit mode
     * @return true if edit mode
     */
    public boolean isEditMode() {
        return pageContext != null && pageContext.isEditMode();
    }

    /**
     * Check if in preview mode
     * @return true if preview mode
     */
    public boolean isPreviewMode() {
        return pageContext != null && pageContext.isPreviewMode();
    }

    /**
     * Get the instance type
     * @return INSTANCE_TYPE enum value
     */
    public INSTANCE_TYPE getInstanceType() {
        return pageContext != null ? pageContext.getInstanceType() : INSTANCE_TYPE.STANDALONE;
    }

    /**
     * Get the instance type as a string for display
     * @return instance type name
     */
    public String getInstanceTypeName() {
        return getInstanceType().name();
    }

    /**
     * Check if on Author instance
     * @return true if Author
     */
    public boolean isAuthor() {
        return pageContext != null && pageContext.isAuthor();
    }

    /**
     * Check if on Renderer instance
     * @return true if Renderer
     */
    public boolean isRenderer() {
        return pageContext != null && pageContext.isRenderer();
    }

    /**
     * Check if on Standalone instance
     * @return true if Standalone
     */
    public boolean isStandalone() {
        return pageContext != null && pageContext.isStandalone();
    }

    /**
     * Determines if edit controls should be shown.
     * Edit controls are shown when:
     * - In edit mode AND
     * - On Author or Standalone instance
     * 
     * @return true if edit controls should be displayed
     */
    public boolean isShouldShowEditControls() {
        return pageContext != null && pageContext.isAuthoringEnabled();
    }

    /**
     * Determines if content is being served to end users.
     * This is true when on Renderer instance or in preview mode.
     * 
     * @return true if serving to end users
     */
    public boolean isServingEndUsers() {
        return pageContext != null && pageContext.isPublishMode();
    }

    /**
     * Get a summary string describing current context
     * @return context summary
     */
    public String getContextSummary() {
        if (pageContext == null) {
            return "PageContext not available";
        }
        return String.format("Instance: %s, Mode: %s", 
            pageContext.getInstanceType().name(),
            pageContext.getPageMode().name());
    }

    /**
     * Get CSS class based on instance type
     * @return CSS class name
     */
    public String getInstanceCssClass() {
        if (pageContext == null) {
            return "is-dark";
        }
        switch (pageContext.getInstanceType()) {
            case AUTHOR:
                return "is-primary";
            case RENDERER:
                return "is-success";
            default:
                return "is-dark";
        }
    }

    /**
     * Get CSS class based on page mode
     * @return CSS class name
     */
    public String getModeCssClass() {
        if (pageContext != null && pageContext.isEditMode()) {
            return "is-warning";
        }
        return "is-info";
    }
}
