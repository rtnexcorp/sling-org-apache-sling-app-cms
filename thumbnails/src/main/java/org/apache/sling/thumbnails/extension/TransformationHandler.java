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
package org.apache.sling.thumbnails.extension;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.sling.cms.transformation.TransformationHandlerConfig;
import org.osgi.annotation.versioning.ConsumerType;

/*
 * Transformation handlers handle the transformation of files. Each transformation handler
 * implements a transformation command using the specifed configuration.
 */
@ConsumerType
public interface TransformationHandler {

    /**
     * Get the resource type associated with this handler
     *
     * @return the handler resource type
     */
    String getResourceType();

    /**
     * Handles the transformation of the file using the command values from the
     * suffix segment.
     *
     * @param inputStream  the inputstream from which to read the file to transform
     *                     from
     * @param outputStream the outputstream to write the transformed file to
     * @param config       the configuration values for the transformation
     * @throws IOException an exception occurs transforming the file
     */
    void handle(InputStream inputStream, OutputStream outputStream, TransformationHandlerConfig config)
            throws IOException;

    /**
     * Handles the transformation of the file using the command values from the
     * suffix segment, with access to source file metadata.
     * <p>
     * This method allows handlers to use metadata extracted from the source file
     * (e.g., EXIF orientation, GPS coordinates, copyright information) to make
     * intelligent transformation decisions.
     * </p>
     * <p>
     * Default implementation delegates to the non-metadata-aware method for
     * backward compatibility.
     * </p>
     *
     * @param inputStream    the inputstream from which to read the file to transform
     * @param outputStream   the outputstream to write the transformed file to
     * @param config         the configuration values for the transformation
     * @param sourceMetadata metadata extracted from the source file (may be empty)
     * @throws IOException an exception occurs transforming the file
     * @since 1.1.0
     */
    default void handle(
            InputStream inputStream,
            OutputStream outputStream,
            TransformationHandlerConfig config,
            Map<String, Object> sourceMetadata)
            throws IOException {
        // Default: delegate to existing method for backward compatibility
        handle(inputStream, outputStream, config);
    }

    /**
     * Indicates whether this handler uses source file metadata.
     * <p>
     * Handlers that return {@code true} will receive metadata via the
     * {@link #handle(InputStream, OutputStream, TransformationHandlerConfig, Map)}
     * method. This allows the transformation system to optimize metadata loading.
     * </p>
     *
     * @return {@code true} if this handler uses metadata, {@code false} otherwise
     * @since 1.1.0
     */
    default boolean usesMetadata() {
        return false;
    }
}
