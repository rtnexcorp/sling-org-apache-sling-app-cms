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

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.cms.core.helpers.SlingCMSTestHelper;
import org.apache.sling.cms.core.internal.CommonUtils;
import org.apache.sling.servlets.post.JSONResponse;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.SlingPostProcessor;
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
public class CreateUserOperationTest {

    public SlingContext context = new SlingContext();

    private User user;

    @BeforeEach
    public void init() throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        SlingCMSTestHelper.initAuthContext(context);

        user = null;
        UserManager userManager = CommonUtils.getUserManager(context.resourceResolver());
        Mockito.when(userManager.createUser(
                        Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.anyString()))
                .thenAnswer((ans) -> {
                    User user = Mockito.mock(User.class);
                    Mockito.when(user.getPath()).thenReturn("/home/users/tests");
                    this.user = user;
                    return user;
                });
    }

    @Test
    public void testCreate() throws RepositoryException {
        CreateUserOperation createUserOperation = new CreateUserOperation();
        PostResponse response = new JSONResponse();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SlingPostConstants.RP_NODE_NAME, "tests");
        parameters.put(CreateUserOperation.PN_PASSWORD, "test5");
        context.currentResource("/home/users");
        context.request().setParameterMap(parameters);

        createUserOperation.run(
                context.request(), response, new SlingPostProcessor[] {Mockito.mock(SlingPostProcessor.class)});

        assertNull(response.getError());

        assertNotNull(user);
        assertEquals("/home/users/tests", user.getPath());
    }

    @Test
    public void testExisting() throws RepositoryException {
        CreateUserOperation createUserOperation = new CreateUserOperation();
        PostResponse response = new JSONResponse();

        context.currentResource("/home/users");
        context.request().setParameterMap(Collections.singletonMap(SlingPostConstants.RP_NODE_NAME, "test5"));

        UserManager userManager = CommonUtils.getUserManager(context.resourceResolver());
        Mockito.when(userManager.getAuthorizable(Mockito.any(Principal.class))).thenReturn(Mockito.mock(Group.class));

        createUserOperation.run(context.request(), response, null);

        assertNotNull(response.getError());
    }
}
