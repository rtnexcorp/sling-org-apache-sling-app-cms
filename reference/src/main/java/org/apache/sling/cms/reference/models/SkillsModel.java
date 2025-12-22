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
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SkillsModel {

    @Self
    private Resource resource;

    @ValueMapValue
    private String authorName;

    @ValueMapValue
    private String jobTitle;

    @ValueMapValue
    private String bio;

    @ValueMapValue
    private String email;

    @ValueMapValue
    private String authorImage;

    private List<SocialLink> socialLinks;
    private List<Skill> skills;

    public boolean isHasAuthor() {
        return StringUtils.isNotBlank(authorName);
    }

    public boolean isHasImage() {
        return StringUtils.isNotBlank(authorImage);
    }

    public boolean isHasJobTitle() {
        return StringUtils.isNotBlank(jobTitle);
    }

    public boolean isHasBio() {
        return StringUtils.isNotBlank(bio);
    }

    public boolean isHasEmail() {
        return StringUtils.isNotBlank(email);
    }

    public boolean isHasSocialLinks() {
        return !getSocialLinks().isEmpty();
    }

    public boolean isHasSkills() {
        return !getSkills().isEmpty();
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getBio() {
        return bio;
    }

    public String getEmail() {
        return email;
    }

    public String getAuthorImage() {
        return authorImage;
    }

    public List<SocialLink> getSocialLinks() {
        if (socialLinks == null) {
            Resource socialLinksRes = resource.getChild("socialLinks");
            List<SocialLink> links = new ArrayList<>();
            if (socialLinksRes != null) {
                for (Resource linkRes : socialLinksRes.getChildren()) {
                    ValueMap vm = linkRes.getValueMap();
                    links.add(new SocialLink(
                            vm.get("platform", String.class),
                            vm.get("url", String.class),
                            vm.get("label", String.class)));
                }
            }
            socialLinks = Collections.unmodifiableList(links);
        }
        return socialLinks;
    }

    public List<Skill> getSkills() {
        if (skills == null) {
            Resource skillsRes = resource.getChild("skills");
            List<Skill> skillList = new ArrayList<>();
            if (skillsRes != null) {
                for (Resource skillRes : skillsRes.getChildren()) {
                    ValueMap vm = skillRes.getValueMap();
                    skillList.add(new Skill(
                            vm.get("name", String.class),
                            vm.get("level", String.class),
                            vm.get("description", String.class)));
                }
            }
            skills = Collections.unmodifiableList(skillList);
        }
        return skills;
    }

    public static class SocialLink {
        private final String platform;
        private final String url;
        private final String label;

        public SocialLink(String platform, String url, String label) {
            this.platform = platform;
            this.url = url;
            this.label = label;
        }

        public String getPlatform() {
            return platform;
        }

        public String getUrl() {
            return url;
        }

        public String getLabel() {
            return label;
        }
    }

    public static class Skill {
        private final String name;
        private final String level;
        private final String description;

        public Skill(String name, String level, String description) {
            this.name = name;
            this.level = level;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getLevel() {
            return level;
        }

        public String getDescription() {
            return description;
        }
    }
}
