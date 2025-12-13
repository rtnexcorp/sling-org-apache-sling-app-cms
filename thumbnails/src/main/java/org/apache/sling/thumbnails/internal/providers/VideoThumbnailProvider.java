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
package org.apache.sling.thumbnails.internal.providers;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.thumbnails.extension.ThumbnailProvider;
import org.apache.sling.thumbnails.extension.VideoExtractionConfig;
import org.apache.sling.thumbnails.extension.VideoFrameExtractor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thumbnail provider for video files.
 *
 * <p>Uses pluggable {@link VideoFrameExtractor} implementations via OSGi SPI.
 * Automatically selects the best available extractor based on priority and
 * capability.
 *
 * <p>Key features:
 * <ul>
 *   <li>Multiple frame sampling for optimal thumbnail selection</li>
 *   <li>Configurable sample positions and analysis options</li>
 *   <li>Graceful fallback when native implementations unavailable</li>
 *   <li>Platform-independent with pure Java fallback (JCodec)</li>
 * </ul>
 */
@Component(service = ThumbnailProvider.class, immediate = true)
@Designate(ocd = VideoThumbnailProvider.Config.class)
public class VideoThumbnailProvider implements ThumbnailProvider {

    private static final Logger LOG = LoggerFactory.getLogger(VideoThumbnailProvider.class);

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "video/mp4",
            "video/quicktime",
            "video/x-m4v",
            "video/webm",
            "video/x-msvideo",
            "video/mpeg",
            "video/x-matroska",
            "video/ogg",
            "video/3gpp");

    private final List<VideoFrameExtractor> extractors = new CopyOnWriteArrayList<>();

    private Config config;

    @ObjectClassDefinition(
            name = "Apache Sling CMS - Video Thumbnail Provider",
            description = "Configuration for video thumbnail generation")
    public @interface Config {

        @AttributeDefinition(
                name = "Sample Positions",
                description = "Percentage positions in video to sample frames (e.g., 10, 25, 50)")
        int[] samplePositions() default {10, 25, 50};

        @AttributeDefinition(
                name = "Use Sharpness Detection",
                description = "Analyze frame sharpness to select best thumbnail")
        boolean useSharpnessDetection() default true;

        @AttributeDefinition(
                name = "Use Face Detection",
                description = "Prefer frames containing faces (requires OpenCV)")
        boolean useFaceDetection() default false;

        @AttributeDefinition(name = "Timeout (ms)", description = "Maximum time to spend extracting frames")
        long timeoutMs() default 30000L;
    }

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        LOG.info(
                "Video Thumbnail Provider activated with {} extractors available",
                getAvailableExtractors().size());
        logAvailableExtractors();
    }

    @Deactivate
    protected void deactivate() {
        LOG.info("Video Thumbnail Provider deactivated");
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindExtractor(VideoFrameExtractor extractor) {
        extractors.add(extractor);
        extractors.sort(
                Comparator.comparingInt(VideoFrameExtractor::getPriority).reversed());
        LOG.info("Registered video frame extractor: {} (priority: {})", extractor.getName(), extractor.getPriority());
    }

    protected void unbindExtractor(VideoFrameExtractor extractor) {
        extractors.remove(extractor);
        LOG.info("Unregistered video frame extractor: {}", extractor.getName());
    }

    @Override
    public boolean applies(Resource resource, String metaType) {
        try {
            MimeType mt = new MimeType(metaType);
            for (String supportedType : SUPPORTED_TYPES) {
                if (mt.match(supportedType)) {
                    return true;
                }
            }
        } catch (MimeTypeParseException e) {
            LOG.debug("Failed to parse mime type: {}", metaType);
        }
        return false;
    }

    @Override
    public InputStream getThumbnail(Resource resource) throws IOException {
        LOG.debug("Generating video thumbnail for resource: {}", resource.getPath());

        VideoFrameExtractor extractor = findBestExtractor();
        if (extractor == null) {
            throw new IOException("No video frame extractor available");
        }

        LOG.debug("Using extractor: {}", extractor.getName());

        File tempFile = null;
        try {
            tempFile = Files.createTempFile("video-thumb-", ".tmp").toFile();

            try (InputStream is = resource.adaptTo(InputStream.class)) {
                if (is == null) {
                    throw new IOException("Cannot read resource: " + resource.getPath());
                }
                Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            VideoExtractionConfig extractionConfig = VideoExtractionConfig.of(
                    config.samplePositions(),
                    config.useFaceDetection(),
                    config.useSharpnessDetection(),
                    config.timeoutMs());

            BufferedImage frame = extractor.extractFrame(tempFile, extractionConfig);
            if (frame == null) {
                throw new IOException("Failed to extract frame from video: " + resource.getPath());
            }

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(frame, "png", os);
            LOG.debug("Successfully generated thumbnail for: {}", resource.getPath());
            return new ByteArrayInputStream(os.toByteArray());

        } finally {
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    LOG.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
                    tempFile.deleteOnExit();
                }
            }
        }
    }

    private VideoFrameExtractor findBestExtractor() {
        for (VideoFrameExtractor extractor : extractors) {
            if (extractor.isAvailable()) {
                return extractor;
            }
        }
        return null;
    }

    private List<VideoFrameExtractor> getAvailableExtractors() {
        return extractors.stream().filter(VideoFrameExtractor::isAvailable).collect(Collectors.toList());
    }

    private void logAvailableExtractors() {
        List<VideoFrameExtractor> available = getAvailableExtractors();
        if (available.isEmpty()) {
            LOG.warn("No video frame extractors available - video thumbnails will not be generated");
        } else {
            LOG.info("Available video frame extractors:");
            for (VideoFrameExtractor ex : available) {
                LOG.info("  - {} (priority: {}, types: {})", ex.getName(), ex.getPriority(), ex.getSupportedTypes());
            }
        }
    }
}
