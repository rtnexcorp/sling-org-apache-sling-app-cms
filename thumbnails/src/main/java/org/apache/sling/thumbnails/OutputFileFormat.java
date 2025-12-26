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

import org.apache.sling.api.SlingHttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Enumeration of the valid output formats for the thumbnail generator.
 *
 * @deprecated Use {@link org.apache.sling.cms.transformation.OutputFileFormat} instead.
 *             This enum will be removed in version 2.0.0.
 */
@Deprecated
@ProviderType
public enum OutputFileFormat {
    /** @deprecated Use {@link org.apache.sling.cms.transformation.OutputFileFormat#GIF} */
    @Deprecated
    GIF("image/gif"),

    /** @deprecated Use {@link org.apache.sling.cms.transformation.OutputFileFormat#JPEG} */
    @Deprecated
    JPEG("image/jpeg"),

    /** @deprecated Use {@link org.apache.sling.cms.transformation.OutputFileFormat#PNG} */
    @Deprecated
    PNG("image/png"),

    /** @deprecated Use {@link org.apache.sling.cms.transformation.OutputFileFormat#WEBM} */
    @Deprecated
    WEBM("video/webm");

    /**
     * Loads the output format requested in the specified request suffix.
     *
     * @param request the current request from which to get the suffix
     * @return the format for the suffix
     * @deprecated Use {@link org.apache.sling.cms.transformation.OutputFileFormat#forRequest(SlingHttpServletRequest)}
     */
    @Deprecated
    public static OutputFileFormat forRequest(SlingHttpServletRequest request) {
        org.apache.sling.cms.transformation.OutputFileFormat newFormat =
                org.apache.sling.cms.transformation.OutputFileFormat.forRequest(request);
        return OutputFileFormat.valueOf(newFormat.name());
    }

    /**
     * Loads the output format
     *
     * @param format the requested format
     * @return the format requested
     * @deprecated Use {@link org.apache.sling.cms.transformation.OutputFileFormat#forValue(String)}
     */
    @Deprecated
    public static OutputFileFormat forValue(@NotNull String format) {
        org.apache.sling.cms.transformation.OutputFileFormat newFormat =
                org.apache.sling.cms.transformation.OutputFileFormat.forValue(format);
        return OutputFileFormat.valueOf(newFormat.name());
    }

    private String mimeType;

    private OutputFileFormat(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @deprecated Use {@link org.apache.sling.cms.transformation.OutputFileFormat#getMimeType()}
     */
    @Deprecated
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Convert to new API enum
     * @return the new API OutputFileFormat
     */
    public org.apache.sling.cms.transformation.OutputFileFormat toNewAPI() {
        return org.apache.sling.cms.transformation.OutputFileFormat.valueOf(this.name());
    }
}
