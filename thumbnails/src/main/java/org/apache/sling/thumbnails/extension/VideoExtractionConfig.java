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

/**
 * Configuration for video frame extraction.
 * Provides parameters that control how frames are selected from video files.
 */
public interface VideoExtractionConfig {

    /**
     * Default sample positions (percentage of video duration).
     */
    int[] DEFAULT_SAMPLE_POSITIONS = {10, 25, 50};

    /**
     * Default timeout in milliseconds.
     */
    long DEFAULT_TIMEOUT_MS = 30000L;

    /**
     * Get sample positions as percentage of video duration (0-100).
     * The extractor will sample frames at these positions and select the best one.
     *
     * @return array of percentage positions to sample
     */
    int[] getSamplePositions();

    /**
     * Whether to use face detection for frame selection.
     * When enabled, frames containing faces are preferred.
     * This feature may require native libraries (OpenCV).
     *
     * @return true to enable face detection
     */
    boolean useFaceDetection();

    /**
     * Whether to use sharpness detection for frame selection.
     * When enabled, sharper frames are preferred over blurry ones.
     *
     * @return true to enable sharpness detection
     */
    boolean useSharpnessDetection();

    /**
     * Maximum time to spend on extraction in milliseconds.
     * Extraction will be aborted if this timeout is exceeded.
     *
     * @return timeout in milliseconds
     */
    long getTimeoutMs();

    /**
     * Create a default configuration instance.
     *
     * @return default configuration
     */
    static VideoExtractionConfig defaultConfig() {
        return new VideoExtractionConfig() {
            @Override
            public int[] getSamplePositions() {
                return DEFAULT_SAMPLE_POSITIONS;
            }

            @Override
            public boolean useFaceDetection() {
                return false;
            }

            @Override
            public boolean useSharpnessDetection() {
                return true;
            }

            @Override
            public long getTimeoutMs() {
                return DEFAULT_TIMEOUT_MS;
            }
        };
    }

    /**
     * Create a configuration with custom sample positions.
     *
     * @param samplePositions array of percentage positions
     * @param useFaceDetection enable face detection
     * @param useSharpnessDetection enable sharpness detection
     * @param timeoutMs timeout in milliseconds
     * @return configured instance
     */
    static VideoExtractionConfig of(
            int[] samplePositions, boolean useFaceDetection, boolean useSharpnessDetection, long timeoutMs) {
        return new VideoExtractionConfig() {
            @Override
            public int[] getSamplePositions() {
                return samplePositions != null && samplePositions.length > 0
                        ? samplePositions
                        : DEFAULT_SAMPLE_POSITIONS;
            }

            @Override
            public boolean useFaceDetection() {
                return useFaceDetection;
            }

            @Override
            public boolean useSharpnessDetection() {
                return useSharpnessDetection;
            }

            @Override
            public long getTimeoutMs() {
                return timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
            }
        };
    }
}
