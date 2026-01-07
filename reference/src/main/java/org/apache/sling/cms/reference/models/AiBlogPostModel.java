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
package org.apache.sling.cms.reference.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Sling Model for the AI-Enhanced Blog Post component.
 * Provides structured access to blog post properties including AI-generated fields.
 *
 * <h2>Usage in HTL:</h2>
 * <pre>
 * &lt;sly data-sly-use.blog="org.apache.sling.cms.reference.models.AiBlogPostModel"&gt;
 *     &lt;h2&gt;${blog.title}&lt;/h2&gt;
 *     &lt;p&gt;${blog.summary}&lt;/p&gt;
 *
 *     &lt;!-- Display tag titles --&gt;
 *     &lt;sly data-sly-list.tag="${blog.tagList}"&gt;
 *         &lt;span class="tag"&gt;${tag}&lt;/span&gt;
 *     &lt;/sly&gt;
 *
 *     &lt;!-- Or if you need the paths --&gt;
 *     &lt;sly data-sly-list.tagPath="${blog.tagPaths}"&gt;
 *         &lt;sly data-sly-resource="${tagPath}" /&gt;
 *     &lt;/sly&gt;
 * &lt;/sly&gt;
 * </pre>
 *
 * <p>Note: Tags are stored as an array of taxonomy resource paths (e.g., "/etc/taxonomy/reference/technology/java/spring-boot").
 * Use {@link #getTagList()} to get resolved tag titles (e.g., "Spring Boot"), or {@link #getTagPaths()} for raw paths.</p>
 */
@Model(adaptables = Resource.class)
public class AiBlogPostModel {

    @SlingObject
    private ResourceResolver resourceResolver;

    @ValueMapValue
    @Default(values = "")
    private String title;

    @ValueMapValue
    @Default(values = "")
    private String summary;

    @ValueMapValue
    @Default(values = "")
    private String author;

    @ValueMapValue
    @Default(values = "")
    private String publishDate;

    @ValueMapValue
    @Default(values = "")
    private String content;

    @ValueMapValue
    @Default(values = "")
    private String metaDescription;

    @ValueMapValue
    @Default(values = {})
    private String[] tags;

    /**
     * Gets the blog post title.
     *
     * @return the title, or empty string if not set
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the blog post summary.
     *
     * @return the summary, or empty string if not set
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Gets the blog post author.
     *
     * @return the author name, or empty string if not set
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Gets the publish date.
     *
     * @return the publish date, or empty string if not set
     */
    public String getPublishDate() {
        return publishDate;
    }

    /**
     * Gets the blog post content (rich text).
     *
     * @return the content HTML, or empty string if not set
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the meta description for SEO.
     *
     * @return the meta description, or empty string if not set
     */
    public String getMetaDescription() {
        return metaDescription;
    }

    /**
     * Gets the raw tags array (taxonomy paths).
     *
     * @return the tags array, or empty array if not set
     */
    public String[] getTags() {
        return tags != null ? tags : new String[0];
    }

    /**
     * Gets the tags as a list of tag paths.
     * This returns the taxonomy resource paths that can be resolved to get tag details.
     *
     * @return list of taxonomy paths, or empty list if no tags
     */
    public List<String> getTagPaths() {
        if (tags == null || tags.length == 0) {
            return Collections.emptyList();
        }

        List<String> tagList = new ArrayList<>();
        for (String tag : tags) {
            if (StringUtils.isNotBlank(tag)) {
                tagList.add(tag);
            }
        }
        return tagList;
    }

    /**
     * Gets the tags as a list of tag titles (display names).
     * Resolves each taxonomy path to get the jcr:title property.
     *
     * @return list of tag titles, or empty list if no tags
     */
    public List<String> getTagList() {
        if (tags == null || tags.length == 0) {
            return Collections.emptyList();
        }

        List<String> tagTitles = new ArrayList<>();
        for (String tagPath : tags) {
            if (StringUtils.isNotBlank(tagPath)) {
                Resource tagResource = resourceResolver.getResource(tagPath);
                if (tagResource != null) {
                    String title = tagResource.getValueMap().get("jcr:title", tagResource.getName());
                    tagTitles.add(title);
                } else {
                    // Fallback: use the path if resource not found
                    tagTitles.add(tagPath);
                }
            }
        }
        return tagTitles;
    }

    /**
     * Checks if the blog post has a summary.
     *
     * @return true if summary is not blank
     */
    public boolean hasSummary() {
        return StringUtils.isNotBlank(summary);
    }

    /**
     * Checks if the blog post has tags.
     *
     * @return true if tags array is not empty
     */
    public boolean hasTags() {
        return tags != null && tags.length > 0;
    }

    /**
     * Checks if the blog post has a meta description.
     *
     * @return true if meta description is not blank
     */
    public boolean hasMetaDescription() {
        return StringUtils.isNotBlank(metaDescription);
    }
}
