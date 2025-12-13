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
package org.apache.sling.thumbnails;

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Configuration interface for automatic rendition generation.
 * Implementations provide access to auto-rendition settings.
 */
@ProviderType
public interface AutoRenditionConfig {

    /**
     * Returns whether automatic rendition generation is enabled.
     *
     * @return true if auto-renditions are enabled
     */
    boolean isEnabled();

    /**
     * Returns the transformation names to apply automatically when assets are uploaded.
     *
     * @return array of transformation names
     */
    @NotNull
    String[] getTransformationNames();

    /**
     * Returns the MIME type patterns for which auto-renditions should be generated.
     * Supports wildcards (e.g., "image/*", "video/*").
     *
     * @return array of MIME type patterns
     */
    @NotNull
    String[] getSupportedMimeTypes();

    /**
     * Returns the content paths under which auto-renditions should be generated.
     *
     * @return array of content paths
     */
    @NotNull
    String[] getContentPaths();
}
