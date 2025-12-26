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
package org.apache.sling.thumbnails.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.rendition.RenditionSupport;
import org.apache.sling.cms.transformation.OutputFileFormat;
import org.apache.sling.cms.transformation.Transformation;
import org.apache.sling.cms.transformation.Transformer;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer.JobResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutoRenditionJobConsumerTest {

    @Mock
    private TransformationServiceUser transformationServiceUser;

    @Mock
    private TransformationCache transformationCache;

    @Mock
    private Transformer transformer;

    @Mock
    private RenditionSupport renditionSupport;

    @Mock
    private ResourceResolver serviceResolver;

    @Mock
    private Resource resource;

    @Mock
    private Job job;

    @Mock
    private Transformation transformation;

    private AutoRenditionJobConsumer consumer;

    @BeforeEach
    void setUp() throws Exception {
        consumer = new AutoRenditionJobConsumer();

        // Inject mocks using reflection
        setField(consumer, "transformationServiceUser", transformationServiceUser);
        setField(consumer, "transformationCache", transformationCache);
        setField(consumer, "transformer", transformer);
        setField(consumer, "renditionSupport", renditionSupport);

        when(transformationServiceUser.getTransformationServiceUser()).thenReturn(serviceResolver);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void testMissingPathProperty() {
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_PATH, String.class))
                .thenReturn(null);
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_TRANSFORMATION, String.class))
                .thenReturn("thumbnail");

        JobResult result = consumer.process(job);

        assertEquals(JobResult.CANCEL, result);
    }

    @Test
    void testMissingTransformationProperty() {
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_PATH, String.class))
                .thenReturn("/content/test.jpg");
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_TRANSFORMATION, String.class))
                .thenReturn(null);

        JobResult result = consumer.process(job);

        assertEquals(JobResult.CANCEL, result);
    }

    @Test
    void testResourceNotFound() throws Exception {
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_PATH, String.class))
                .thenReturn("/content/test.jpg");
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_TRANSFORMATION, String.class))
                .thenReturn("thumbnail");
        when(serviceResolver.getResource("/content/test.jpg")).thenReturn(null);

        JobResult result = consumer.process(job);

        assertEquals(JobResult.CANCEL, result);
    }

    @Test
    void testResourceDoesNotSupportRenditions() throws Exception {
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_PATH, String.class))
                .thenReturn("/content/test.jpg");
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_TRANSFORMATION, String.class))
                .thenReturn("thumbnail");
        when(serviceResolver.getResource("/content/test.jpg")).thenReturn(resource);
        when(renditionSupport.supportsRenditions(resource)).thenReturn(false);

        JobResult result = consumer.process(job);

        assertEquals(JobResult.OK, result);
        verify(transformer, never()).transform(any(), any(), any(), any());
    }

    @Test
    void testTransformationNotFound() throws Exception {
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_PATH, String.class))
                .thenReturn("/content/test.jpg");
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_TRANSFORMATION, String.class))
                .thenReturn("thumbnail");
        when(serviceResolver.getResource("/content/test.jpg")).thenReturn(resource);
        when(renditionSupport.supportsRenditions(resource)).thenReturn(true);
        when(transformationCache.getTransformation(serviceResolver, "/thumbnail"))
                .thenReturn(Optional.empty());

        JobResult result = consumer.process(job);

        assertEquals(JobResult.CANCEL, result);
    }

    @Test
    void testRenditionAlreadyExists() throws Exception {
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_PATH, String.class))
                .thenReturn("/content/test.jpg");
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_TRANSFORMATION, String.class))
                .thenReturn("thumbnail");
        when(serviceResolver.getResource("/content/test.jpg")).thenReturn(resource);
        when(renditionSupport.supportsRenditions(resource)).thenReturn(true);
        when(transformationCache.getTransformation(serviceResolver, "/thumbnail"))
                .thenReturn(Optional.of(transformation));
        when(renditionSupport.renditionExists(resource, "/thumbnail.png")).thenReturn(true);

        JobResult result = consumer.process(job);

        assertEquals(JobResult.OK, result);
        verify(transformer, never()).transform(any(), any(), any(), any());
    }

    @Test
    void testSuccessfulRenditionGeneration() throws Exception {
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_PATH, String.class))
                .thenReturn("/content/test.jpg");
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_TRANSFORMATION, String.class))
                .thenReturn("thumbnail");
        when(serviceResolver.getResource("/content/test.jpg")).thenReturn(resource);
        when(renditionSupport.supportsRenditions(resource)).thenReturn(true);
        when(transformationCache.getTransformation(serviceResolver, "/thumbnail"))
                .thenReturn(Optional.of(transformation));
        when(renditionSupport.renditionExists(resource, "/thumbnail.png")).thenReturn(false);

        JobResult result = consumer.process(job);

        assertEquals(JobResult.OK, result);
        verify(transformer)
                .transform(eq(resource), eq(transformation), eq(OutputFileFormat.PNG), any(OutputStream.class));
        verify(renditionSupport).setRendition(eq(resource), eq("/thumbnail.png"), any(ByteArrayInputStream.class));
    }

    @Test
    void testTransformationFailure() throws Exception {
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_PATH, String.class))
                .thenReturn("/content/test.jpg");
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_TRANSFORMATION, String.class))
                .thenReturn("thumbnail");
        when(serviceResolver.getResource("/content/test.jpg")).thenReturn(resource);
        when(renditionSupport.supportsRenditions(resource)).thenReturn(true);
        when(transformationCache.getTransformation(serviceResolver, "/thumbnail"))
                .thenReturn(Optional.of(transformation));
        when(renditionSupport.renditionExists(resource, "/thumbnail.png")).thenReturn(false);
        doThrow(new IOException("Transform failed")).when(transformer).transform(any(), any(), any(), any());

        JobResult result = consumer.process(job);

        assertEquals(JobResult.FAILED, result);
    }

    @Test
    void testPersistenceFailure() throws Exception {
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_PATH, String.class))
                .thenReturn("/content/test.jpg");
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_TRANSFORMATION, String.class))
                .thenReturn("thumbnail");
        when(serviceResolver.getResource("/content/test.jpg")).thenReturn(resource);
        when(renditionSupport.supportsRenditions(resource)).thenReturn(true);
        when(transformationCache.getTransformation(serviceResolver, "/thumbnail"))
                .thenReturn(Optional.of(transformation));
        when(renditionSupport.renditionExists(resource, "/thumbnail.png")).thenReturn(false);
        doThrow(new PersistenceException("Save failed")).when(renditionSupport).setRendition(any(), any(), any());

        JobResult result = consumer.process(job);

        assertEquals(JobResult.FAILED, result);
    }

    @Test
    void testLoginException() throws Exception {
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_PATH, String.class))
                .thenReturn("/content/test.jpg");
        when(job.getProperty(AutoRenditionJobConsumer.PROPERTY_TRANSFORMATION, String.class))
                .thenReturn("thumbnail");
        when(transformationServiceUser.getTransformationServiceUser())
                .thenThrow(new org.apache.sling.api.resource.LoginException("Login failed"));

        JobResult result = consumer.process(job);

        assertEquals(JobResult.FAILED, result);
    }
}
