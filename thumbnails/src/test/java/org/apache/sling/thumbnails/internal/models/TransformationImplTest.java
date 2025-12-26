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
package org.apache.sling.thumbnails.internal.models;

import javax.jcr.LoginException;

import java.util.Collections;
import java.util.List;

import org.apache.sling.cms.transformation.Transformation;
import org.apache.sling.cms.transformation.TransformationHandlerConfig;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.apache.sling.thumbnails.internal.ContextHelper;
import org.apache.sling.thumbnails.internal.transformers.RotateHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SlingContextExtension.class)
public class TransformationImplTest {

    public SlingContext context = new SlingContext();

    @BeforeEach
    public void init() throws IllegalAccessException, LoginException {
        ContextHelper.initContext(context);
        context.addModelsForPackage("org.apache.sling.thumbnails.internal.models");
    }

    @Test
    public void testModel() {
        Transformation transformation = context.resourceResolver()
                .getResource("/conf/global/files/transformations/sling-cms-thumbnail")
                .adaptTo(Transformation.class);
        assertNotNull(transformation);
        assertEquals("sling-cms-thumbnail", transformation.getName());
        assertEquals("/conf/global/files/transformations/sling-cms-thumbnail", transformation.getPath());

        assertEquals(1, transformation.getHandlers().size());

        assertEquals(
                "sling/thumbnails/transformers/crop",
                transformation.getHandlers().get(0).getHandlerType());
    }

    @Test
    public void testJson() {

        List<TransformationHandlerConfig> config = Collections.singletonList(new TransformationHandlerConfigImpl(
                "sling/thumbnails/transformers/rotate", Collections.singletonMap(RotateHandler.DEGREES, 90)));

        Transformation transformation = new TransformationImpl(config);

        assertNotNull(transformation);
        assertNull(transformation.getName());
        assertNull(transformation.getPath());

        assertEquals(1, transformation.getHandlers().size());
        assertTrue(transformation.getHandlers().get(0) instanceof TransformationHandlerConfig);
        assertEquals(
                "sling/thumbnails/transformers/rotate",
                transformation.getHandlers().get(0).getHandlerType());
    }
}
