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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.reference.beans.Author;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Sling Model for the Book component.
 * Demonstrates multifield handling with nested author data.
 * 
 * <h2>Usage in HTL:</h2>
 * <pre>
 * &lt;sly data-sly-use.book="org.apache.sling.cms.reference.models.BookModel"&gt;
 *     &lt;h2&gt;${book.bookTitle}&lt;/h2&gt;
 *     &lt;sly data-sly-list.author="${book.authors}"&gt;
 *         &lt;p&gt;${author.authorName}&lt;/p&gt;
 *     &lt;/sly&gt;
 * &lt;/sly&gt;
 * </pre>
 */
@Model(adaptables = Resource.class)
public class BookModel {

    @Self
    private Resource resource;

    @ValueMapValue
    @Default(values = "")
    private String bookTitle;

    @ValueMapValue
    @Default(values = "")
    private String subtitle;

    @ValueMapValue
    @Default(values = "")
    private String publishingYear;

    @ValueMapValue
    @Default(values = "")
    private String isbn;

    @ValueMapValue
    @Default(values = "")
    private String publisher;

    @ValueMapValue
    @Default(values = "")
    private String coverImage;

    @ValueMapValue
    @Default(values = "")
    private String description;

    private List<Author> authors;

    /**
     * Get the list of authors from the multifield.
     * This is lazily initialized on first access.
     * @return list of authors
     */
    public List<Author> getAuthors() {
        if (authors == null) {
            authors = new ArrayList<>();
            Resource authorsResource = resource.getChild("authors");
            if (authorsResource != null) {
                for (Resource authorResource : authorsResource.getChildren()) {
                    authors.add(new Author(authorResource));
                }
            }
        }
        return Collections.unmodifiableList(authors);
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getPublishingYear() {
        return publishingYear;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHasAuthors() {
        return !getAuthors().isEmpty();
    }

    public boolean isHasTitle() {
        return StringUtils.isNotBlank(bookTitle);
    }

    public boolean isHasCoverImage() {
        return StringUtils.isNotBlank(coverImage);
    }

    public boolean isHasSubtitle() {
        return StringUtils.isNotBlank(subtitle);
    }

    public boolean isHasDescription() {
        return StringUtils.isNotBlank(description);
    }

    public boolean isHasPublishingYear() {
        return StringUtils.isNotBlank(publishingYear);
    }

    public boolean isHasPublisher() {
        return StringUtils.isNotBlank(publisher);
    }

    public boolean isHasIsbn() {
        return StringUtils.isNotBlank(isbn);
    }

    /**
     * Get authors formatted as a comma-separated string for display in byline.
     * @return formatted authors string
     */
    public String getAuthorsDisplayText() {
        List<Author> authorList = getAuthors();
        if (authorList.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < authorList.size(); i++) {
            Author author = authorList.get(i);
            sb.append(author.getAuthorName());
            if (author.isHasRole() && !"author".equals(author.getRole())) {
                sb.append(" (").append(author.getRole()).append(")");
            }
            if (i < authorList.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
