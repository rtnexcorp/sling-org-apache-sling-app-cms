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
package org.apache.sling.cms.personalization.internal.evaluators;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.personalization.SegmentEvaluator;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates segments based on user authentication and group membership.
 * <p>
 * Configuration properties:
 * </p>
 * <ul>
 *   <li><b>requiresLogin</b> (Boolean, default: false) - If true, matches only logged-in users</li>
 *   <li><b>requiresAnonymous</b> (Boolean, default: false) - If true, matches only anonymous users</li>
 *   <li><b>groups</b> (String[], optional) - Group IDs that user must belong to (any match)</li>
 *   <li><b>requireAllGroups</b> (Boolean, default: false) - If true, user must belong to ALL
 *       specified groups (instead of any)</li>
 * </ul>
 * <p>
 * Example segment configurations:
 * </p>
 */
@Component(service = SegmentEvaluator.class)
public class UserGroupSegmentEvaluator implements SegmentEvaluator {

    private static final Logger log = LoggerFactory.getLogger(UserGroupSegmentEvaluator.class);

    public static final String TYPE = "userGroup";
    public static final String PROP_REQUIRES_LOGIN = "requiresLogin";
    public static final String PROP_REQUIRES_ANONYMOUS = "requiresAnonymous";
    public static final String PROP_GROUPS = "groups";
    public static final String PROP_REQUIRE_ALL_GROUPS = "requireAllGroups";

    private static final String ANONYMOUS_USER = "anonymous";

    @Override
    @NotNull
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean evaluate(@NotNull SlingHttpServletRequest request, @NotNull ValueMap rules) {
        ResourceResolver resolver = request.getResourceResolver();
        String userId = resolver.getUserID();

        boolean requiresLogin = rules.get(PROP_REQUIRES_LOGIN, false);
        boolean requiresAnonymous = rules.get(PROP_REQUIRES_ANONYMOUS, false);
        String[] requiredGroups = rules.get(PROP_GROUPS, String[].class);
        boolean requireAllGroups = rules.get(PROP_REQUIRE_ALL_GROUPS, false);

        // Check conflicting configuration
        if (requiresLogin && requiresAnonymous) {
            log.warn("UserGroupSegmentEvaluator: both requiresLogin and requiresAnonymous are true");
            return false;
        }

        boolean isAnonymous = ANONYMOUS_USER.equals(userId);

        // Check anonymous requirement
        if (requiresAnonymous) {
            log.debug("UserGroupSegmentEvaluator: checking anonymous requirement, isAnonymous: {}", isAnonymous);
            return isAnonymous;
        }

        // Check login requirement
        if (requiresLogin && isAnonymous) {
            log.debug("UserGroupSegmentEvaluator: login required but user is anonymous");
            return false;
        }

        // If no group requirements, just check login status
        if (requiredGroups == null || requiredGroups.length == 0) {
            boolean matches = !requiresLogin || !isAnonymous;
            log.debug("UserGroupSegmentEvaluator: no group requirements, matches: {}", matches);
            return matches;
        }

        // Check group membership
        if (isAnonymous) {
            log.debug("UserGroupSegmentEvaluator: user is anonymous, cannot check group membership");
            return false;
        }

        try {
            Set<String> userGroups = getUserGroups(resolver, userId);
            log.debug("UserGroupSegmentEvaluator: user '{}' belongs to groups: {}", userId, userGroups);

            if (requireAllGroups) {
                // User must belong to ALL specified groups
                boolean matches = userGroups.containsAll(Arrays.asList(requiredGroups));
                log.debug("UserGroupSegmentEvaluator: require all groups, matches: {}", matches);
                return matches;
            } else {
                // User must belong to ANY of the specified groups
                for (String group : requiredGroups) {
                    if (userGroups.contains(group)) {
                        log.debug("UserGroupSegmentEvaluator: user belongs to group '{}'", group);
                        return true;
                    }
                }
                log.debug("UserGroupSegmentEvaluator: user does not belong to any required group");
                return false;
            }
        } catch (Exception e) {
            log.error("UserGroupSegmentEvaluator: error checking group membership for user '{}'", userId, e);
            return false;
        }
    }

    /**
     * Gets all groups that the user belongs to (including nested groups).
     */
    private Set<String> getUserGroups(ResourceResolver resolver, String userId) throws RepositoryException {
        Set<String> groups = new HashSet<>();

        Session session = resolver.adaptTo(Session.class);
        if (!(session instanceof JackrabbitSession)) {
            log.warn("UserGroupSegmentEvaluator: session is not a JackrabbitSession");
            return groups;
        }

        JackrabbitSession jackrabbitSession = (JackrabbitSession) session;
        UserManager userManager = jackrabbitSession.getUserManager();

        Authorizable authorizable = userManager.getAuthorizable(userId);
        if (!(authorizable instanceof User)) {
            log.warn("UserGroupSegmentEvaluator: authorizable '{}' is not a user", userId);
            return groups;
        }

        User user = (User) authorizable;
        Iterator<Group> groupIterator = user.memberOf();

        while (groupIterator.hasNext()) {
            Group group = groupIterator.next();
            groups.add(group.getID());
        }

        return groups;
    }
}
