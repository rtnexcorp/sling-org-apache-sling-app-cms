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
package org.apache.sling.thumbnails.internal.providers.video;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sling.thumbnails.extension.VideoExtractionConfig;
import org.apache.sling.thumbnails.extension.VideoFrameExtractor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Video frame extractor using FFmpeg command-line tool.
 *
 * <p>This implementation uses the system-installed FFmpeg to extract frames
 * from video files. FFmpeg must be installed and available in the system PATH.
 *
 * <p>Supports all video formats that FFmpeg supports, including:
 * <ul>
 *   <li>MP4, MOV, M4V (H.264/H.265)</li>
 *   <li>WebM (VP8/VP9)</li>
 *   <li>AVI, MKV, WMV, FLV</li>
 *   <li>And many more</li>
 * </ul>
 */
@Component(
        service = VideoFrameExtractor.class,
        configurationPolicy = ConfigurationPolicy.OPTIONAL,
        property = {"service.ranking:Integer=200"})
@Designate(ocd = FFmpegVideoFrameExtractor.Config.class)
public class FFmpegVideoFrameExtractor implements VideoFrameExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(FFmpegVideoFrameExtractor.class);

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

    private static final Pattern DURATION_PATTERN =
            Pattern.compile("Duration:\\s*(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{2})");

    private static final long DEFAULT_TIMEOUT_SECONDS = 30;

    @ObjectClassDefinition(
            name = "Apache Sling Thumbnails - FFmpeg Video Frame Extractor",
            description = "Configuration for FFmpeg-based video frame extraction")
    public @interface Config {
        @AttributeDefinition(
                name = "FFmpeg Path",
                description = "Path to FFmpeg executable. Leave empty to use system PATH.")
        String ffmpeg_path() default "";

        @AttributeDefinition(
                name = "FFprobe Path",
                description = "Path to FFprobe executable. Leave empty to use system PATH.")
        String ffprobe_path() default "";

        @AttributeDefinition(
                name = "Timeout (seconds)",
                description = "Maximum time to wait for FFmpeg to complete extraction")
        long timeout_seconds() default DEFAULT_TIMEOUT_SECONDS;

        @AttributeDefinition(name = "Enabled", description = "Enable or disable FFmpeg video frame extractor")
        boolean enabled() default true;
    }

    private String ffmpegPath = "ffmpeg";
    private String ffprobePath = "ffprobe";
    private long timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
    private boolean enabled = true;
    private boolean available = false;

    @Activate
    protected void activate(Config config) {
        if (config != null) {
            this.enabled = config.enabled();
            this.timeoutSeconds = config.timeout_seconds();

            String configFfmpegPath = config.ffmpeg_path();
            if (configFfmpegPath != null && !configFfmpegPath.isEmpty()) {
                this.ffmpegPath = configFfmpegPath;
            }

            String configFfprobePath = config.ffprobe_path();
            if (configFfprobePath != null && !configFfprobePath.isEmpty()) {
                this.ffprobePath = configFfprobePath;
            }
        }

        if (enabled) {
            this.available = checkFFmpegAvailable();
            if (available) {
                LOG.info("FFmpeg video frame extractor activated (path: {})", ffmpegPath);
            } else {
                LOG.warn("FFmpeg not available. Video thumbnail generation will be disabled.");
            }
        } else {
            this.available = false;
            LOG.info("FFmpeg video frame extractor is disabled by configuration");
        }
    }

    private boolean checkFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            if (completed && process.exitValue() == 0) {
                LOG.debug("FFmpeg is available");
                return true;
            }
        } catch (Exception e) {
            LOG.debug("FFmpeg check failed: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        return available && enabled;
    }

    @Override
    public int getPriority() {
        return 200; // Higher priority than pure Java implementations
    }

    @Override
    public String getName() {
        return "FFmpeg";
    }

    @Override
    public Set<String> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public BufferedImage extractFrame(File videoFile, VideoExtractionConfig config) throws IOException {
        if (!isAvailable()) {
            throw new IOException("FFmpeg is not available");
        }

        if (videoFile == null || !videoFile.exists()) {
            LOG.warn("Video file does not exist: {}", videoFile);
            return null;
        }

        // Get video duration
        double durationSec = getVideoDuration(videoFile);
        if (durationSec <= 0) {
            LOG.warn("Could not determine video duration for: {}", videoFile.getName());
            durationSec = 10.0; // Assume 10 seconds if unknown
        }

        int[] samplePositions = config.getSamplePositions();
        if (samplePositions == null || samplePositions.length == 0) {
            samplePositions = VideoExtractionConfig.DEFAULT_SAMPLE_POSITIONS;
        }

        BufferedImage bestFrame = null;
        double bestScore = -1;

        for (int percent : samplePositions) {
            double targetSec = durationSec * percent / 100.0;

            try {
                BufferedImage frame = extractFrameAtTime(videoFile, targetSec);
                if (frame == null) {
                    continue;
                }

                // Calculate frame score
                double score = config.useSharpnessDetection() ? calculateSharpness(frame) : 1.0;

                if (score > bestScore) {
                    bestScore = score;
                    bestFrame = frame;
                }

                LOG.debug("Sampled frame at {}% ({}s), score: {}", percent, targetSec, score);

            } catch (Exception e) {
                LOG.debug("Failed to extract frame at {}%: {}", percent, e.getMessage());
            }
        }

        if (bestFrame == null) {
            // Try to get first frame as fallback
            LOG.debug("No frame selected, trying first frame as fallback");
            bestFrame = extractFrameAtTime(videoFile, 0.1);
        }

        return bestFrame;
    }

    /**
     * Get video duration using FFprobe.
     */
    private double getVideoDuration(File videoFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffprobePath,
                    "-i",
                    videoFile.getAbsolutePath(),
                    "-show_entries",
                    "format=duration",
                    "-v",
                    "quiet",
                    "-of",
                    "csv=p=0");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            if (completed && process.exitValue() == 0) {
                String result = output.toString().trim();
                if (!result.isEmpty()) {
                    return Double.parseDouble(result);
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to get duration via ffprobe, trying ffmpeg: {}", e.getMessage());
        }

        // Fallback: parse duration from ffmpeg output
        return getVideoDurationFromFFmpeg(videoFile);
    }

    /**
     * Get video duration by parsing FFmpeg output.
     */
    private double getVideoDurationFromFFmpeg(File videoFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-i", videoFile.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            process.waitFor(10, TimeUnit.SECONDS);

            // Parse duration from output
            Matcher matcher = DURATION_PATTERN.matcher(output.toString());
            if (matcher.find()) {
                int hours = Integer.parseInt(matcher.group(1));
                int minutes = Integer.parseInt(matcher.group(2));
                int seconds = Integer.parseInt(matcher.group(3));
                int centiseconds = Integer.parseInt(matcher.group(4));
                return hours * 3600 + minutes * 60 + seconds + centiseconds / 100.0;
            }
        } catch (Exception e) {
            LOG.debug("Failed to parse duration from ffmpeg: {}", e.getMessage());
        }
        return -1;
    }

    /**
     * Extract a single frame at the specified time.
     */
    private BufferedImage extractFrameAtTime(File videoFile, double timeSec) throws IOException {
        File tempFile = null;
        try {
            tempFile = Files.createTempFile("sling-thumbnail-", ".png").toFile();

            // FFmpeg command to extract a single frame
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-ss",
                    String.format("%.3f", timeSec),
                    "-i",
                    videoFile.getAbsolutePath(),
                    "-vframes",
                    "1",
                    "-f",
                    "image2",
                    "-y",
                    tempFile.getAbsolutePath());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Consume output to prevent blocking
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                    // Consume output
                }
            }

            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new IOException("FFmpeg timed out");
            }

            if (process.exitValue() != 0) {
                throw new IOException("FFmpeg failed with exit code: " + process.exitValue());
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                return ImageIO.read(tempFile);
            }

            return null;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while extracting frame", e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    tempFile.deleteOnExit();
                }
            }
        }
    }

    /**
     * Calculate image sharpness using variance of pixel intensity.
     * Higher variance generally indicates sharper images.
     *
     * @param image the image to analyze
     * @return sharpness score (higher = sharper)
     */
    private double calculateSharpness(BufferedImage image) {
        if (image == null) {
            return 0;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // Sample pixels for performance (every 4th pixel)
        int step = 4;
        long sum = 0;
        long sumSquared = 0;
        int count = 0;

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int rgb = image.getRGB(x, y);
                // Convert to grayscale
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (r + g + b) / 3;

                sum += gray;
                sumSquared += (long) gray * gray;
                count++;
            }
        }

        if (count == 0) {
            return 0;
        }

        double mean = (double) sum / count;
        double variance = ((double) sumSquared / count) - (mean * mean);

        return variance;
    }
}
