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
package org.apache.sling.cms.core.models;

import javax.inject.Inject;

import java.util.ResourceBundle;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Sling Model for the TextElement component.
 * Supports i18n text translation and dynamic HTML element rendering.
 *
 * This model replaces the JSP-based textelement.jsp with HTL support.
 */
@ProviderType
@Model(adaptables = {Resource.class, SlingHttpServletRequest.class})
public class TextElement {

    @ValueMapValue
    @Default(values = "")
    private String text;

    @ValueMapValue
    @Default(booleanValues = false)
    private boolean i18n;

    @ValueMapValue
    @Default(values = "p")
    private String level;

    @ValueMapValue(optional = true)
    private String href;

    @Inject
    @Optional
    private SlingHttpServletRequest request;

    /**
     * Gets the text content, optionally translated via i18n.
     *
     * @return the text content (translated if i18n is enabled)
     */
    public String getText() {
        if (i18n && text != null && !text.isEmpty() && request != null) {
            ResourceBundle bundle = request.getResourceBundle(request.getLocale());
            if (bundle != null && bundle.containsKey(text)) {
                return bundle.getString(text);
            }
        }
        return text != null ? text : "";
    }

    /**
     * Gets the HTML element level (e.g., h1, h2, p, span).
     *
     * @return the HTML element tag name
     */
    public String getLevel() {
        return level != null ? level : "p";
    }

    /**
     * Gets the optional href link.
     *
     * @return the href URL or null if not set
     */
    public String getHref() {
        return href;
    }

    /**
     * Checks if the text should be translated.
     *
     * @return true if i18n is enabled
     */
    public boolean isI18n() {
        return i18n;
    }
}
