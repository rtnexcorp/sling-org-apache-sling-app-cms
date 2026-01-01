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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.thumbnails.AutoRenditionConfig;
import org.apache.sling.thumbnails.ThumbnailSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutoRenditionListenerTest {

    @Mock
    private JobManager jobManager;

    @Mock
    private TransformationServiceUser transformationServiceUser;

    @Mock
    private ThumbnailSupport thumbnailSupport;

    @Mock
    private AutoRenditionConfig autoRenditionConfig;

    @Mock
    private ResourceResolver serviceResolver;

    @Mock
    private Resource resource;

    @Mock
    private ValueMap valueMap;

    private AutoRenditionListener listener;

    @BeforeEach
    void setUp() throws Exception {
        listener = new AutoRenditionListener();

        // Inject mocks using reflection
        setField(listener, "jobManager", jobManager);
        setField(listener, "transformationServiceUser", transformationServiceUser);
        setField(listener, "thumbnailSupport", thumbnailSupport);
        setField(listener, "autoRenditionConfig", autoRenditionConfig);

        when(transformationServiceUser.getTransformationServiceUser()).thenReturn(serviceResolver);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void testDisabledConfig() {
        when(autoRenditionConfig.isEnabled()).thenReturn(false);
        when(autoRenditionConfig.getContentPaths()).thenReturn(new String[] {"/content"});

        listener.activate();

        ResourceChange change = createResourceChange("/content/test.jpg");
        listener.onChange(Collections.singletonList(change));

        verify(jobManager, never()).addJob(anyString(), anyMap());
    }

    @Test
    void testEnabledConfigWithMatchingResource() throws Exception {
        setupEnabledConfig();
        setupSupportedResource("/content/test.jpg", "sling:File", "image/jpeg");

        listener.activate();

        ResourceChange change = createResourceChange("/content/test.jpg");
        listener.onChange(Collections.singletonList(change));

        verify(jobManager).addJob(eq(AutoRenditionJobConsumer.TOPIC), argThat(map -> {
            Map<String, Object> m = (Map<String, Object>) map;
            return "/content/test.jpg".equals(m.get(AutoRenditionJobConsumer.PROPERTY_PATH))
                    && "thumbnail".equals(m.get(AutoRenditionJobConsumer.PROPERTY_TRANSFORMATION));
        }));
    }

    @Test
    void testResourceOutsideConfiguredPath() throws Exception {
        setupEnabledConfig();

        listener.activate();

        ResourceChange change = createResourceChange("/apps/test.jpg");
        listener.onChange(Collections.singletonList(change));

        verify(jobManager, never()).addJob(anyString(), anyMap());
    }

    @Test
    void testUnsupportedResourceType() throws Exception {
        setupEnabledConfig();

        when(serviceResolver.getResource("/content/test.jpg")).thenReturn(resource);
        when(resource.getResourceType()).thenReturn("nt:folder");

        Set<String> supportedTypes = new HashSet<>(Arrays.asList("sling:File", "nt:file"));
        when(thumbnailSupport.getSupportedTypes()).thenReturn(supportedTypes);
        when(thumbnailSupport.getPersistableTypes()).thenReturn(supportedTypes);

        listener.activate();

        ResourceChange change = createResourceChange("/content/test.jpg");
        listener.onChange(Collections.singletonList(change));

        verify(jobManager, never()).addJob(anyString(), anyMap());
    }

    @Test
    void testNonMatchingMimeType() throws Exception {
        setupEnabledConfig();
        setupSupportedResource("/content/test.txt", "sling:File", "text/plain");

        listener.activate();

        ResourceChange change = createResourceChange("/content/test.txt");
        listener.onChange(Collections.singletonList(change));

        verify(jobManager, never()).addJob(anyString(), anyMap());
    }

    @Test
    void testMultipleTransformations() throws Exception {
        when(autoRenditionConfig.isEnabled()).thenReturn(true);
        when(autoRenditionConfig.getContentPaths()).thenReturn(new String[] {"/content"});
        when(autoRenditionConfig.getSupportedMimeTypes()).thenReturn(new String[] {"image/*"});
        when(autoRenditionConfig.getTransformationNames()).thenReturn(new String[] {"thumbnail", "medium", "large"});

        setupSupportedResource("/content/test.jpg", "sling:File", "image/jpeg");

        listener.activate();

        ResourceChange change = createResourceChange("/content/test.jpg");
        listener.onChange(Collections.singletonList(change));

        verify(jobManager, times(3)).addJob(eq(AutoRenditionJobConsumer.TOPIC), anyMap());
    }

    @Test
    void testVideoMimeType() throws Exception {
        when(autoRenditionConfig.isEnabled()).thenReturn(true);
        when(autoRenditionConfig.getContentPaths()).thenReturn(new String[] {"/content"});
        when(autoRenditionConfig.getSupportedMimeTypes()).thenReturn(new String[] {"image/*", "video/*"});
        when(autoRenditionConfig.getTransformationNames()).thenReturn(new String[] {"thumbnail"});

        setupSupportedResource("/content/video.mp4", "sling:File", "video/mp4");

        listener.activate();

        ResourceChange change = createResourceChange("/content/video.mp4");
        listener.onChange(Collections.singletonList(change));

        verify(jobManager).addJob(eq(AutoRenditionJobConsumer.TOPIC), anyMap());
    }

    @Test
    void testWildcardMimeType() throws Exception {
        when(autoRenditionConfig.isEnabled()).thenReturn(true);
        when(autoRenditionConfig.getContentPaths()).thenReturn(new String[] {"/content"});
        when(autoRenditionConfig.getSupportedMimeTypes()).thenReturn(new String[] {"*/*"});
        when(autoRenditionConfig.getTransformationNames()).thenReturn(new String[] {"thumbnail"});

        setupSupportedResource("/content/test.pdf", "sling:File", "application/pdf");

        listener.activate();

        ResourceChange change = createResourceChange("/content/test.pdf");
        listener.onChange(Collections.singletonList(change));

        verify(jobManager).addJob(eq(AutoRenditionJobConsumer.TOPIC), anyMap());
    }

    @Test
    void testMultipleResourceChanges() throws Exception {
        setupEnabledConfig();

        when(serviceResolver.getResource("/content/test1.jpg")).thenReturn(resource);
        Resource resource2 = mock(Resource.class);
        ValueMap valueMap2 = mock(ValueMap.class);
        when(serviceResolver.getResource("/content/test2.png")).thenReturn(resource2);

        when(resource.getResourceType()).thenReturn("sling:File");
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get("jcr:content/jcr:mimeType", String.class)).thenReturn("image/jpeg");

        when(resource2.getResourceType()).thenReturn("sling:File");
        when(resource2.getValueMap()).thenReturn(valueMap2);
        when(valueMap2.get("jcr:content/jcr:mimeType", String.class)).thenReturn("image/png");

        Set<String> supportedTypes = new HashSet<>(Arrays.asList("sling:File"));
        when(thumbnailSupport.getSupportedTypes()).thenReturn(supportedTypes);
        when(thumbnailSupport.getPersistableTypes()).thenReturn(supportedTypes);
        when(thumbnailSupport.getMetaTypePropertyPath("sling:File")).thenReturn("jcr:content/jcr:mimeType");

        listener.activate();

        List<ResourceChange> changes =
                Arrays.asList(createResourceChange("/content/test1.jpg"), createResourceChange("/content/test2.png"));
        listener.onChange(changes);

        verify(jobManager, times(2)).addJob(eq(AutoRenditionJobConsumer.TOPIC), anyMap());
    }

    private void setupEnabledConfig() {
        when(autoRenditionConfig.isEnabled()).thenReturn(true);
        when(autoRenditionConfig.getContentPaths()).thenReturn(new String[] {"/content", "/static"});
        when(autoRenditionConfig.getSupportedMimeTypes()).thenReturn(new String[] {"image/*"});
        when(autoRenditionConfig.getTransformationNames()).thenReturn(new String[] {"thumbnail"});
    }

    private void setupSupportedResource(String path, String resourceType, String mimeType) {
        when(serviceResolver.getResource(path)).thenReturn(resource);
        when(resource.getPath()).thenReturn(path);
        when(resource.getResourceType()).thenReturn(resourceType);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get("jcr:content/jcr:mimeType", String.class)).thenReturn(mimeType);

        Set<String> supportedTypes = new HashSet<>(Arrays.asList(resourceType));
        when(thumbnailSupport.getSupportedTypes()).thenReturn(supportedTypes);
        when(thumbnailSupport.getPersistableTypes()).thenReturn(supportedTypes);
        when(thumbnailSupport.getMetaTypePropertyPath(resourceType)).thenReturn("jcr:content/jcr:mimeType");
    }

    private ResourceChange createResourceChange(String path) {
        ResourceChange change = mock(ResourceChange.class);
        when(change.getPath()).thenReturn(path);
        when(change.getType()).thenReturn(ChangeType.ADDED);
        return change;
    }
}
