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
package org.apache.sling.cms.core.internal;

import javax.script.Bindings;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.cms.Page;
import org.apache.sling.cms.core.helpers.SlingCMSTestHelper;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SlingContextExtension.class)
public class DefaultScriptBindingsValueProviderTest {

    public SlingContext context = new SlingContext();

    @BeforeEach
    public void init() {
        SlingCMSTestHelper.initContext(context);
    }

    @Test
    public void testPageBinding() {
        Bindings bindings = new SlingBindings();
        context.currentResource("/content/apache/sling-apache-org/index");
        bindings.put("resource", context.currentResource());
        bindings.put("request", context.request());

        DefaultScriptBindingsValueProvider provider = new DefaultScriptBindingsValueProvider();
        provider.addBindings(bindings);

        System.out.println(bindings.keySet().toString());
        assertNotNull(bindings.get("page"));
        assertTrue(bindings.get("page") instanceof Page);
        assertNotNull(bindings.get("currentPage"));
        assertEquals(bindings.get("currentPage"), bindings.get("page"));
        assertNotNull(bindings.get("properties"));
        assertTrue(bindings.get("properties") instanceof ValueMap);
        assertNotNull(bindings.get("componentConfiguration"));
        assertTrue(bindings.get("componentConfiguration") instanceof ValueMap);
    }

    @Test
    public void testSiteBinding() {
        Bindings bindings = new SlingBindings();
        context.currentResource("/content/apache/sling-apache-org");
        bindings.put("resource", context.currentResource());
        bindings.put("request", context.request());

        DefaultScriptBindingsValueProvider provider = new DefaultScriptBindingsValueProvider();
        provider.addBindings(bindings);

        System.out.println(bindings.keySet().toString());
        assertNull(bindings.get("page"));
        assertNull(bindings.get("currentPage"));
        assertNotNull(bindings.get("properties"));
        assertTrue(bindings.get("properties") instanceof ValueMap);
        assertNotNull(bindings.get("componentConfiguration"));
        assertTrue(bindings.get("componentConfiguration") instanceof ValueMap);
    }
}
