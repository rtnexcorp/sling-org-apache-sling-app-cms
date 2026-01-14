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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserGroupSegmentEvaluatorTest {

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private JackrabbitSession jackrabbitSession;

    @Mock
    private UserManager userManager;

    @Mock
    private User user;

    private UserGroupSegmentEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new UserGroupSegmentEvaluator();
        lenient().when(request.getResourceResolver()).thenReturn(resourceResolver);
    }

    @Test
    void testGetType() {
        assertEquals("userGroup", evaluator.getType());
    }

    @Test
    void testEvaluate_RequiresAnonymous_WithAnonymousUser() {
        when(resourceResolver.getUserID()).thenReturn("anonymous");

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("requiresAnonymous", true);
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_RequiresAnonymous_WithLoggedInUser() {
        when(resourceResolver.getUserID()).thenReturn("admin");

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("requiresAnonymous", true);
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_RequiresLogin_WithLoggedInUser() {
        when(resourceResolver.getUserID()).thenReturn("admin");

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("requiresLogin", true);
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_RequiresLogin_WithAnonymousUser() {
        when(resourceResolver.getUserID()).thenReturn("anonymous");

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("requiresLogin", true);
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_ConflictingRequirements_ReturnsFalse() {
        when(resourceResolver.getUserID()).thenReturn("admin");

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("requiresLogin", true);
        rulesMap.put("requiresAnonymous", true);
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_NoRequirements_MatchesAnyUser() {
        when(resourceResolver.getUserID()).thenReturn("admin");

        Map<String, Object> rulesMap = new HashMap<>();
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_NoRequirements_MatchesAnonymousUser() {
        when(resourceResolver.getUserID()).thenReturn("anonymous");

        Map<String, Object> rulesMap = new HashMap<>();
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_GroupMembership_AnonymousUser_ReturnsFalse() {
        when(resourceResolver.getUserID()).thenReturn("anonymous");

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("groups", new String[] {"administrators"});
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_GroupMembership_UserInGroup() throws RepositoryException {
        when(resourceResolver.getUserID()).thenReturn("admin");
        when(resourceResolver.adaptTo(Session.class)).thenReturn(jackrabbitSession);
        when(jackrabbitSession.getUserManager()).thenReturn(userManager);
        when(userManager.getAuthorizable("admin")).thenReturn(user);

        Group adminGroup = mockGroup("administrators");
        @SuppressWarnings("unchecked")
        Iterator<Group> groupIterator = mock(Iterator.class);
        when(groupIterator.hasNext()).thenReturn(true, false);
        when(groupIterator.next()).thenReturn(adminGroup);
        when(user.memberOf()).thenReturn(groupIterator);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("groups", new String[] {"administrators"});
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_GroupMembership_UserNotInGroup() throws RepositoryException {
        when(resourceResolver.getUserID()).thenReturn("testuser");
        when(resourceResolver.adaptTo(Session.class)).thenReturn(jackrabbitSession);
        when(jackrabbitSession.getUserManager()).thenReturn(userManager);
        when(userManager.getAuthorizable("testuser")).thenReturn(user);

        Group contentAuthorsGroup = mockGroup("content-authors");
        @SuppressWarnings("unchecked")
        Iterator<Group> groupIterator = mock(Iterator.class);
        when(groupIterator.hasNext()).thenReturn(true, false);
        when(groupIterator.next()).thenReturn(contentAuthorsGroup);
        when(user.memberOf()).thenReturn(groupIterator);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("groups", new String[] {"administrators"});
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_GroupMembership_RequireAllGroups_UserInAllGroups() throws RepositoryException {
        when(resourceResolver.getUserID()).thenReturn("admin");
        when(resourceResolver.adaptTo(Session.class)).thenReturn(jackrabbitSession);
        when(jackrabbitSession.getUserManager()).thenReturn(userManager);
        when(userManager.getAuthorizable("admin")).thenReturn(user);

        Group adminGroup = mockGroup("administrators");
        Group editorsGroup = mockGroup("editors");
        @SuppressWarnings("unchecked")
        Iterator<Group> groupIterator = mock(Iterator.class);
        when(groupIterator.hasNext()).thenReturn(true, true, false);
        when(groupIterator.next()).thenReturn(adminGroup, editorsGroup);
        when(user.memberOf()).thenReturn(groupIterator);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("groups", new String[] {"administrators", "editors"});
        rulesMap.put("requireAllGroups", true);
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_GroupMembership_RequireAllGroups_UserNotInAllGroups() throws RepositoryException {
        when(resourceResolver.getUserID()).thenReturn("admin");
        when(resourceResolver.adaptTo(Session.class)).thenReturn(jackrabbitSession);
        when(jackrabbitSession.getUserManager()).thenReturn(userManager);
        when(userManager.getAuthorizable("admin")).thenReturn(user);

        Group adminGroup = mockGroup("administrators");
        @SuppressWarnings("unchecked")
        Iterator<Group> groupIterator = mock(Iterator.class);
        when(groupIterator.hasNext()).thenReturn(true, false);
        when(groupIterator.next()).thenReturn(adminGroup);
        when(user.memberOf()).thenReturn(groupIterator);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("groups", new String[] {"administrators", "editors"});
        rulesMap.put("requireAllGroups", true);
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_GroupMembership_RequireAnyGroup_UserInOneGroup() throws RepositoryException {
        when(resourceResolver.getUserID()).thenReturn("admin");
        when(resourceResolver.adaptTo(Session.class)).thenReturn(jackrabbitSession);
        when(jackrabbitSession.getUserManager()).thenReturn(userManager);
        when(userManager.getAuthorizable("admin")).thenReturn(user);

        Group adminGroup = mockGroup("administrators");
        @SuppressWarnings("unchecked")
        Iterator<Group> groupIterator = mock(Iterator.class);
        when(groupIterator.hasNext()).thenReturn(true, false);
        when(groupIterator.next()).thenReturn(adminGroup);
        when(user.memberOf()).thenReturn(groupIterator);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("groups", new String[] {"administrators", "editors", "publishers"});
        rulesMap.put("requireAllGroups", false);
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_NonJackrabbitSession_ReturnsFalse() {
        Session regularSession = mock(Session.class);
        when(resourceResolver.getUserID()).thenReturn("admin");
        when(resourceResolver.adaptTo(Session.class)).thenReturn(regularSession);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("groups", new String[] {"administrators"});
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_NullSession_ReturnsFalse() {
        when(resourceResolver.getUserID()).thenReturn("admin");
        when(resourceResolver.adaptTo(Session.class)).thenReturn(null);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("groups", new String[] {"administrators"});
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_RepositoryException_ReturnsFalse() throws RepositoryException {
        when(resourceResolver.getUserID()).thenReturn("admin");
        when(resourceResolver.adaptTo(Session.class)).thenReturn(jackrabbitSession);
        when(jackrabbitSession.getUserManager()).thenThrow(new RepositoryException("Test exception"));

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("groups", new String[] {"administrators"});
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_UserNotFound_ReturnsFalse() throws RepositoryException {
        when(resourceResolver.getUserID()).thenReturn("unknownuser");
        when(resourceResolver.adaptTo(Session.class)).thenReturn(jackrabbitSession);
        when(jackrabbitSession.getUserManager()).thenReturn(userManager);
        when(userManager.getAuthorizable("unknownuser")).thenReturn(null);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("groups", new String[] {"administrators"});
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_EmptyGroupsArray_LoggedInUser_ReturnsTrue() {
        when(resourceResolver.getUserID()).thenReturn("admin");

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("groups", new String[] {});
        rulesMap.put("requiresLogin", true);
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertTrue(evaluator.evaluate(request, rules));
    }

    @Test
    void testEvaluate_MultipleGroups_UserInNoGroups() throws RepositoryException {
        when(resourceResolver.getUserID()).thenReturn("testuser");
        when(resourceResolver.adaptTo(Session.class)).thenReturn(jackrabbitSession);
        when(jackrabbitSession.getUserManager()).thenReturn(userManager);
        when(userManager.getAuthorizable("testuser")).thenReturn(user);

        @SuppressWarnings("unchecked")
        Iterator<Group> emptyGroupIterator = mock(Iterator.class);
        when(emptyGroupIterator.hasNext()).thenReturn(false);
        when(user.memberOf()).thenReturn(emptyGroupIterator);

        Map<String, Object> rulesMap = new HashMap<>();
        rulesMap.put("groups", new String[] {"administrators", "editors"});
        ValueMap rules = new ValueMapDecorator(rulesMap);

        assertFalse(evaluator.evaluate(request, rules));
    }

    private Group mockGroup(String groupId) throws RepositoryException {
        Group group = mock(Group.class);
        when(group.getID()).thenReturn(groupId);
        return group;
    }
}
