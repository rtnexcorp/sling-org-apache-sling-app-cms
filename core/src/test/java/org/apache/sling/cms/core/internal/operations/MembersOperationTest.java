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
package org.apache.sling.cms.core.internal.operations;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.cms.core.helpers.SlingCMSTestHelper;
import org.apache.sling.servlets.post.JSONResponse;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(SlingContextExtension.class)
public class MembersOperationTest {

    public SlingContext context = new SlingContext();

    private ArrayList<String> added;
    private ArrayList<String> removed;

    @BeforeEach
    public void init() throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        SlingCMSTestHelper.initAuthContext(context);

        Group group = (Group) SlingCMSTestHelper.AUTH_REGISTRY.get("/home/groups/sling-cms/authors");

        added = new ArrayList<>();
        removed = new ArrayList<>();

        Mockito.when(group.addMember(Mockito.any())).then((ans) -> {
            added.add(ans.getArgument(0, Authorizable.class).getPath());
            return true;
        });

        Mockito.when(group.removeMember(Mockito.any())).then((ans) -> {
            removed.add(ans.getArgument(0, Authorizable.class).getPath());
            return true;
        });
    }

    @Test
    public void testModifyOperation() throws RepositoryException {
        MembersOperation membersOperation = new MembersOperation();
        PostResponse response = new JSONResponse();

        context.currentResource("/home/groups/sling-cms/authors");
        context.request()
                .setParameterMap(
                        Collections.singletonMap(":members", new String[] {"/home/users/test2", "/home/users/test3"}));

        membersOperation.run(context.request(), response, null);

        assertNull(response.getError());

        assertEquals("/home/groups/sling-cms/authors", response.getPath());

        assertEquals(1, added.size());
        assertEquals("/home/users/test2", added.get(0));

        assertEquals(1, removed.size());
        assertEquals("/home/users/test", removed.get(0));
    }

    @Test
    public void testNotGroup() throws RepositoryException {
        MembersOperation membersOperation = new MembersOperation();
        PostResponse response = new JSONResponse();

        context.currentResource("/home/users/test2");
        context.request()
                .setParameterMap(
                        Collections.singletonMap(":members", new String[] {"/home/users/test2", "/home/users/test3"}));

        membersOperation.run(context.request(), response, null);

        assertNotNull(response.getError());
    }

    @Test
    public void testInvalidPath() throws RepositoryException {
        MembersOperation membersOperation = new MembersOperation();
        PostResponse response = new JSONResponse();

        context.currentResource("/home/groups/sling-cms/authors");
        context.request()
                .setParameterMap(
                        Collections.singletonMap(":members", new String[] {"/home/users/test2", "/home/users/test4"}));

        membersOperation.run(context.request(), response, null);

        assertNotNull(response.getError());
    }
}
