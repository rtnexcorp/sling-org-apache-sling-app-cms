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
package org.apache.sling.thumbnails.internal.metadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.thumbnails.metadata.MetadataExtractor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Metadata extractor for video files using FFprobe.
 *
 * <p>Extracts video metadata including:
 * <ul>
 *   <li>Duration (seconds)</li>
 *   <li>Video codec</li>
 *   <li>Audio codec</li>
 *   <li>Bitrate</li>
 *   <li>Frame rate (FPS)</li>
 *   <li>Resolution (width x height)</li>
 *   <li>Container format</li>
 * </ul>
 *
 * <p>Requires FFprobe to be installed and available in the system PATH.
 *
 * @since 1.2.0
 */
@Component(
        service = MetadataExtractor.class,
        configurationPolicy = ConfigurationPolicy.OPTIONAL,
        property = {"service.ranking:Integer=100"})
@Designate(ocd = VideoMetadataExtractor.Config.class)
public class VideoMetadataExtractor implements MetadataExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(VideoMetadataExtractor.class);

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "video/mp4",
            "video/quicktime",
            "video/x-m4v",
            "video/webm",
            "video/x-msvideo",
            "video/x-ms-wmv",
            "video/x-flv",
            "video/x-matroska",
            "video/mpeg",
            "video/3gpp",
            "video/3gpp2",
            "video/ogg");

    private static final long DEFAULT_TIMEOUT_SECONDS = 10;

    @ObjectClassDefinition(
            name = "Apache Sling Thumbnails - Video Metadata Extractor",
            description = "Configuration for FFprobe-based video metadata extraction")
    public @interface Config {
        @AttributeDefinition(
                name = "FFprobe Path",
                description = "Path to FFprobe executable. Leave empty to use system PATH.")
        String ffprobe_path() default "";

        @AttributeDefinition(name = "Timeout (seconds)", description = "Maximum time to wait for FFprobe to complete")
        long timeout_seconds() default DEFAULT_TIMEOUT_SECONDS;

        @AttributeDefinition(name = "Enabled", description = "Enable or disable video metadata extractor")
        boolean enabled() default true;
    }

    private String ffprobePath = "ffprobe";
    private long timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
    private boolean enabled = true;
    private boolean available = false;

    @Activate
    protected void activate(Config config) {
        if (config != null) {
            this.enabled = config.enabled();
            this.timeoutSeconds = config.timeout_seconds();

            String configPath = config.ffprobe_path();
            if (configPath != null && !configPath.isEmpty()) {
                this.ffprobePath = configPath;
            }
        }

        if (enabled) {
            this.available = checkFFprobeAvailable();
            if (available) {
                LOG.info("Video metadata extractor activated (ffprobe: {})", ffprobePath);
            } else {
                LOG.warn("FFprobe not available. Video metadata extraction will be disabled.");
            }
        } else {
            this.available = false;
            LOG.info("Video metadata extractor is disabled by configuration");
        }
    }

    private boolean checkFFprobeAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffprobePath, "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            if (completed && process.exitValue() == 0) {
                LOG.debug("FFprobe is available");
                return true;
            }
        } catch (Exception e) {
            LOG.debug("FFprobe check failed: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public Set<String> getSupportedMimeTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public int getPriority() {
        return available ? 100 : 0;
    }

    @Override
    public String getName() {
        return "Video Metadata Extractor (FFprobe)";
    }

    @Override
    public Map<String, Object> extractMetadata(InputStream inputStream, String mimeType, String filename)
            throws IOException {
        Map<String, Object> metadata = new HashMap<>();

        if (!available) {
            LOG.debug("FFprobe not available, skipping video metadata extraction");
            return metadata;
        }

        File tempFile = null;
        try {
            // FFprobe requires a file, so write stream to temp file
            tempFile = Files.createTempFile("sling-video-metadata-", getFileExtension(filename))
                    .toFile();
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            extractFFprobeMetadata(tempFile, metadata);

        } catch (Exception e) {
            LOG.error("Error extracting video metadata", e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    tempFile.deleteOnExit();
                }
            }
        }

        return metadata;
    }

    /**
     * Extract metadata using FFprobe with JSON output.
     */
    private void extractFFprobeMetadata(File videoFile, Map<String, Object> metadata) throws IOException {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffprobePath,
                    "-v",
                    "quiet",
                    "-print_format",
                    "json",
                    "-show_format",
                    "-show_streams",
                    videoFile.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new IOException("FFprobe timed out");
            }

            if (process.exitValue() != 0) {
                LOG.warn("FFprobe exited with code: {}", process.exitValue());
                return;
            }

            parseFFprobeOutput(output.toString(), metadata);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while extracting video metadata", e);
        }
    }

    /**
     * Parse FFprobe JSON output and extract metadata.
     */
    private void parseFFprobeOutput(String jsonOutput, Map<String, Object> metadata) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonOutput);

            // Extract format information
            if (root.has("format")) {
                JsonNode format = root.get("format");

                if (format.has("duration")) {
                    double duration = format.get("duration").asDouble();
                    metadata.put("video:duration", (long) duration);
                    metadata.put("video:durationFormatted", formatDuration(duration));
                }

                if (format.has("bit_rate")) {
                    long bitrate = format.get("bit_rate").asLong();
                    metadata.put("video:bitrate", bitrate);
                    metadata.put("video:bitrateFormatted", formatBitrate(bitrate));
                }

                if (format.has("format_name")) {
                    metadata.put("video:format", format.get("format_name").asText());
                }

                if (format.has("format_long_name")) {
                    metadata.put(
                            "video:formatLongName",
                            format.get("format_long_name").asText());
                }

                if (format.has("size")) {
                    metadata.put("video:fileSize", format.get("size").asLong());
                }
            }

            // Extract stream information
            if (root.has("streams")) {
                JsonNode streams = root.get("streams");
                extractStreamMetadata(streams, metadata);
            }

        } catch (Exception e) {
            LOG.error("Error parsing FFprobe output", e);
        }
    }

    /**
     * Extract metadata from video and audio streams.
     */
    private void extractStreamMetadata(JsonNode streams, Map<String, Object> metadata) {
        for (JsonNode stream : streams) {
            if (!stream.has("codec_type")) {
                continue;
            }

            String codecType = stream.get("codec_type").asText();

            if ("video".equals(codecType)) {
                extractVideoStreamMetadata(stream, metadata);
            } else if ("audio".equals(codecType)) {
                extractAudioStreamMetadata(stream, metadata);
            }
        }
    }

    /**
     * Extract video stream metadata.
     */
    private void extractVideoStreamMetadata(JsonNode stream, Map<String, Object> metadata) {
        if (stream.has("codec_name")) {
            metadata.put("video:videoCodec", stream.get("codec_name").asText());
        }

        if (stream.has("codec_long_name")) {
            metadata.put(
                    "video:videoCodecLongName", stream.get("codec_long_name").asText());
        }

        if (stream.has("width")) {
            metadata.put("video:width", stream.get("width").asInt());
        }

        if (stream.has("height")) {
            metadata.put("video:height", stream.get("height").asInt());
        }

        if (stream.has("r_frame_rate")) {
            String frameRate = stream.get("r_frame_rate").asText();
            metadata.put("video:frameRate", parseFrameRate(frameRate));
        }

        if (stream.has("bit_rate")) {
            metadata.put("video:videoStreamBitrate", stream.get("bit_rate").asLong());
        }
    }

    /**
     * Extract audio stream metadata.
     */
    private void extractAudioStreamMetadata(JsonNode stream, Map<String, Object> metadata) {
        if (stream.has("codec_name")) {
            metadata.put("video:audioCodec", stream.get("codec_name").asText());
        }

        if (stream.has("codec_long_name")) {
            metadata.put(
                    "video:audioCodecLongName", stream.get("codec_long_name").asText());
        }

        if (stream.has("sample_rate")) {
            metadata.put("video:audioSampleRate", stream.get("sample_rate").asInt());
        }

        if (stream.has("channels")) {
            metadata.put("video:audioChannels", stream.get("channels").asInt());
        }

        if (stream.has("bit_rate")) {
            metadata.put("video:audioStreamBitrate", stream.get("bit_rate").asLong());
        }
    }

    /**
     * Parse frame rate from FFprobe format (e.g., "30000/1001" or "30")
     */
    private double parseFrameRate(String frameRate) {
        try {
            if (frameRate.contains("/")) {
                String[] parts = frameRate.split("/");
                double numerator = Double.parseDouble(parts[0]);
                double denominator = Double.parseDouble(parts[1]);
                return numerator / denominator;
            }
            return Double.parseDouble(frameRate);
        } catch (Exception e) {
            LOG.debug("Could not parse frame rate: {}", frameRate);
            return 0.0;
        }
    }

    /**
     * Format duration in seconds to HH:MM:SS format.
     */
    private String formatDuration(double durationSeconds) {
        long totalSeconds = (long) durationSeconds;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Format bitrate in bps to human-readable format.
     */
    private String formatBitrate(long bitrate) {
        if (bitrate < 1024) {
            return bitrate + " bps";
        } else if (bitrate < 1024 * 1024) {
            return String.format("%.1f Kbps", bitrate / 1024.0);
        } else {
            return String.format("%.1f Mbps", bitrate / (1024.0 * 1024.0));
        }
    }

    /**
     * Get file extension from filename.
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".tmp";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
