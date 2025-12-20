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
package org.apache.sling.cms.core.internal.listeners;

import java.util.Collections;
import java.util.List;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.cms.CMSConstants;
import org.apache.sling.cms.core.internal.jobs.FileMetadataExtractorConsumer;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Resource Change Listener which extracts the metadata from sling:Files when
 * they are uploaded.
 *
 * @deprecated This listener has been replaced by
 *             {@link org.apache.sling.thumbnails.internal.metadata.AssetMetadataExtractionListener}
 *             in the thumbnails module. This listener is disabled by default and will be removed
 *             in a future version. The new metadata pipeline provides enhanced features including
 *             EXIF/IPTC/XMP extraction, video metadata, and PDF metadata extraction.
 */
@Deprecated
@Component(
        service = {
            FileMetadataExtractorListener.class,
            ResourceChangeListener.class,
            ExternalResourceChangeListener.class
        },
        configurationPolicy = ConfigurationPolicy.OPTIONAL,
        property = {
            ResourceChangeListener.CHANGES + "=ADDED",
            ResourceChangeListener.PATHS + "=/content",
            ResourceChangeListener.PATHS + "=/static"
        },
        immediate = true)
@Designate(ocd = FileMetadataExtractorListener.Config.class)
public class FileMetadataExtractorListener implements ResourceChangeListener, ExternalResourceChangeListener {

    @ObjectClassDefinition(
            name = "Apache Sling CMS - File Metadata Extractor Listener (Deprecated)",
            description =
                    "DEPRECATED: This listener is replaced by the new metadata pipeline in the thumbnails module. "
                            + "Disable this listener to use the new enhanced metadata extraction.")
    public @interface Config {
        @AttributeDefinition(
                name = "Enabled",
                description = "Enable or disable this deprecated listener. "
                        + "Set to false to use the new metadata pipeline in the thumbnails module (recommended).")
        boolean enabled() default false;
    }

    private static final Logger log = LoggerFactory.getLogger(FileMetadataExtractorListener.class);

    @Reference
    private JobManager jobManager;

    @Reference
    private ResourceResolverFactory factory;

    private boolean enabled = false;

    @Activate
    protected void activate(Config config) {
        this.enabled = config.enabled();
        if (enabled) {
            log.warn("FileMetadataExtractorListener is DEPRECATED and enabled. "
                    + "Please migrate to the new metadata pipeline in the thumbnails module.");
        } else {
            log.info("FileMetadataExtractorListener is disabled (using new metadata pipeline in thumbnails module)");
        }
    }

    @Override
    public void onChange(List<ResourceChange> changes) {
        if (!enabled) {
            log.trace("FileMetadataExtractorListener is disabled, skipping");
            return;
        }
        try (ResourceResolver serviceResolver = factory.getServiceResourceResolver(
                Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, "sling-cms-metadata"))) {
            changes.stream()
                    .map(rc -> serviceResolver.getResource(rc.getPath()))
                    .filter(r -> CMSConstants.NT_FILE.equals(r.getResourceType()))
                    .forEach(r -> {
                        log.debug("Queueing resource {}", r);
                        jobManager.addJob(
                                FileMetadataExtractorConsumer.TOPIC,
                                Collections.singletonMap(SlingConstants.PROPERTY_PATH, r.getPath()));
                    });
        } catch (LoginException e) {
            log.error("Exception getting service user", e);
        }
    }
}
