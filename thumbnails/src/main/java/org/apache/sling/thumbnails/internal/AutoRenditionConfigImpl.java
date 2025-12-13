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

import org.apache.sling.thumbnails.AutoRenditionConfig;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi configuration implementation for automatic rendition generation.
 */
@Component(service = AutoRenditionConfig.class)
@Designate(ocd = AutoRenditionConfigImpl.Config.class)
public class AutoRenditionConfigImpl implements AutoRenditionConfig {

    @ObjectClassDefinition(
            name = "Apache Sling Thumbnails - Auto Rendition Configuration",
            description = "Configuration for automatic rendition generation when assets are uploaded")
    public @interface Config {

        @AttributeDefinition(name = "Enabled", description = "Enable automatic rendition generation on asset upload")
        boolean enabled() default true;

        @AttributeDefinition(
                name = "Transformation Names",
                description = "Names of transformations to apply automatically (must exist under /conf or /libs/conf)")
        String[] transformationNames() default {"thumbnail"};

        @AttributeDefinition(
                name = "Supported MIME Types",
                description = "MIME type patterns for auto-rendition generation (supports wildcards like 'image/*')")
        String[] supportedMimeTypes() default {"image/*"};

        @AttributeDefinition(
                name = "Content Paths",
                description = "Content paths under which auto-renditions should be generated")
        String[] contentPaths() default {"/content", "/static"};
    }

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    @Override
    public boolean isEnabled() {
        return config.enabled();
    }

    @Override
    @NotNull
    public String[] getTransformationNames() {
        return config.transformationNames() != null ? config.transformationNames() : new String[0];
    }

    @Override
    @NotNull
    public String[] getSupportedMimeTypes() {
        return config.supportedMimeTypes() != null ? config.supportedMimeTypes() : new String[0];
    }

    @Override
    @NotNull
    public String[] getContentPaths() {
        return config.contentPaths() != null ? config.contentPaths() : new String[0];
    }
}
