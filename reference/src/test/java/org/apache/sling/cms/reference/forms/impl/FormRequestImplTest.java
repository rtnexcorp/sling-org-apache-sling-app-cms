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
package org.apache.sling.cms.reference.forms.impl;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.reference.forms.FormException;
import org.apache.sling.cms.reference.forms.impl.fields.SelectionHandler;
import org.apache.sling.cms.reference.forms.impl.fields.TextareaHandler;
import org.apache.sling.cms.reference.forms.impl.fields.TextfieldHandler;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SlingContextExtension.class)
public class FormRequestImplTest {

    public SlingContext context = new SlingContext();

    private FormRequestImpl formRequest;

    @BeforeEach
    public void init() throws NoSuchFieldException, SecurityException, FormException {
        SlingContextHelper.initContext(context);
        context.request().setResource(context.resourceResolver().getResource("/form/jcr:content/container/form"));

        Map<String, Object> params = new HashMap<>();
        params.put("requiredtextarea", "Hello World!");
        params.put("singleselect", "Hello World!");
        params.put("anotherkey", "Hello World!");
        params.put("money", "123");
        params.put("patternfield", "123");
        params.put("double", "2.7");
        params.put("integer", "2");
        params.put("file", new ByteArrayInputStream(new byte[0]));
        params.put("datefield", "2019-02-02");

        context.request().setParameterMap(params);

        formRequest = new FormRequestImpl(
                context.request(),
                null,
                Arrays.asList(
                        new SelectionHandler(new SelectionHandler.Config() {

                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return null;
                            }

                            @Override
                            public String[] supportedTypes() {
                                return new String[] {SelectionHandler.DEFAULT_RESOURCE_TYPE};
                            }
                        }),
                        new TextareaHandler(new TextareaHandler.Config() {

                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return null;
                            }

                            @Override
                            public String[] supportedTypes() {
                                return new String[] {TextareaHandler.DEFAULT_RESOURCE_TYPE};
                            }
                        }),
                        new TextfieldHandler(new TextfieldHandler.Config() {

                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return null;
                            }

                            @Override
                            public String[] supportedTypes() {
                                return new String[] {TextfieldHandler.DEFAULT_RESOURCE_TYPE};
                            }
                        })));
        formRequest.initFields();
    }

    @Test
    public void testValueMap() throws FormException {
        ValueMap formData = formRequest.getFormData();
        assertNotNull(formData);
        assertTrue(formData.containsKey("requiredtextarea"));
        assertFalse(formData.containsKey("textarea"));
        assertFalse(formData.containsKey("anotherkey"));
    }

    @Test
    public void testRequest() throws FormException {
        assertNotNull(formRequest.getOriginalRequest());
        assertEquals(context.request(), formRequest.getOriginalRequest());
    }
}
