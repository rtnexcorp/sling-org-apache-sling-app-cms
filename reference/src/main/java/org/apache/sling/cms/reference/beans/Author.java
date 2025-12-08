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
package org.apache.sling.cms.reference.beans;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

/**
 * Bean class representing an author from a multifield.
 * Used by BookModel and other components that need author information.
 */
public class Author {
    private final String authorName;
    private final String email;
    private final String role;
    private final String bio;
    private final String website;

    public Author(Resource resource) {
        ValueMap props = resource.getValueMap();
        this.authorName = props.get("authorName", "");
        this.email = props.get("email", "");
        this.role = props.get("role", "author");
        this.bio = props.get("bio", "");
        this.website = props.get("website", "");
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getBio() {
        return bio;
    }

    public String getWebsite() {
        return website;
    }

    public boolean isHasEmail() {
        return StringUtils.isNotBlank(email);
    }

    public boolean isHasRole() {
        return StringUtils.isNotBlank(role);
    }

    public boolean isHasBio() {
        return StringUtils.isNotBlank(bio);
    }

    public boolean isHasWebsite() {
        return StringUtils.isNotBlank(website);
    }

    /**
     * Check if role should be displayed (not 'author' which is default)
     * @return true if role should be shown
     */
    public boolean isShowRole() {
        return isHasRole() && !"author".equals(role);
    }

    public String getMailtoLink() {
        return "mailto:" + email;
    }
}
