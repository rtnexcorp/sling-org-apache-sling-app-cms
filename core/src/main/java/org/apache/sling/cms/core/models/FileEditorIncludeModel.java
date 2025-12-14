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

import javax.annotation.PostConstruct;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

/**
 * Sling Model for the fileeditorinclude component.
 * Finds the appropriate editor configuration based on file mime type.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class FileEditorIncludeModel {

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private ConfigurationResourceResolver configResourceResolver;

    private Resource suffixResource;
    private String mimeType;
    private Resource matchingEditor;
    private Resource generalEditor;

    @PostConstruct
    protected void init() {
        suffixResource = request.getRequestPathInfo().getSuffixResource();
        if (suffixResource != null) {
            mimeType = suffixResource.getValueMap().get("jcr:content/jcr:mimeType", String.class);
            findMatchingEditor();
        }
    }

    private void findMatchingEditor() {
        if (suffixResource == null || configResourceResolver == null) {
            return;
        }

        Collection<Resource> editors = configResourceResolver.getResourceCollection(suffixResource, "files", "editors");

        for (Resource editor : editors) {
            Resource content = editor.getChild("jcr:content");
            if (content != null) {
                String[] mimetypes = content.getValueMap().get("mimetypes", String[].class);
                if (mimetypes == null || mimetypes.length == 0) {
                    // General editor (no specific mimetypes)
                    generalEditor = editor;
                } else if (mimeType != null) {
                    Set<String> mimetypeSet = new HashSet<>(Arrays.asList(mimetypes));
                    if (mimetypeSet.contains(mimeType)) {
                        matchingEditor = editor;
                        break; // Found exact match
                    }
                }
            }
        }
    }

    /**
     * @return the suffix path for form action
     */
    public String getSuffix() {
        return request.getRequestPathInfo().getSuffix();
    }

    /**
     * @return the mime type of the suffix resource
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @return true if a matching editor was found
     */
    public boolean hasMatchingEditor() {
        return matchingEditor != null;
    }

    /**
     * @return true if a general editor was found (and no specific match)
     */
    public boolean hasGeneralEditor() {
        return matchingEditor == null && generalEditor != null;
    }

    /**
     * @return true if no editor was found
     */
    public boolean hasNoEditor() {
        return matchingEditor == null && generalEditor == null;
    }

    /**
     * @return the matching editor fields path
     */
    public String getMatchingEditorFieldsPath() {
        return matchingEditor != null ? matchingEditor.getPath() + "/fields" : null;
    }

    /**
     * @return the general editor fields path
     */
    public String getGeneralEditorFieldsPath() {
        return generalEditor != null ? generalEditor.getPath() + "/fields" : null;
    }
}
