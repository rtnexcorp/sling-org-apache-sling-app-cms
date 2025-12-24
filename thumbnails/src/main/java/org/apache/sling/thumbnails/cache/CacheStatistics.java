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
package org.apache.sling.thumbnails.cache;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics for rendition cache operations.
 *
 * <p>Tracks cache hits, misses, size, and hit ratio.
 */
public class CacheStatistics {

    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong entries = new AtomicLong(0);
    private final AtomicLong sizeBytes = new AtomicLong(0);

    /**
     * Record a cache hit.
     */
    public void recordHit() {
        hits.incrementAndGet();
    }

    /**
     * Record a cache miss.
     */
    public void recordMiss() {
        misses.incrementAndGet();
    }

    /**
     * Record a new cache entry.
     *
     * @param bytes size of the entry in bytes
     */
    public void recordEntry(long bytes) {
        entries.incrementAndGet();
        sizeBytes.addAndGet(bytes);
    }

    /**
     * Record removal of a cache entry.
     *
     * @param bytes size of the entry in bytes
     */
    public void recordRemoval(long bytes) {
        entries.decrementAndGet();
        sizeBytes.addAndGet(-bytes);
    }

    /**
     * Get total cache hits.
     *
     * @return number of hits
     */
    public long getHits() {
        return hits.get();
    }

    /**
     * Get total cache misses.
     *
     * @return number of misses
     */
    public long getMisses() {
        return misses.get();
    }

    /**
     * Get number of cache entries.
     *
     * @return number of entries
     */
    public long getEntries() {
        return entries.get();
    }

    /**
     * Get total cache size in bytes.
     *
     * @return size in bytes
     */
    public long getSizeBytes() {
        return sizeBytes.get();
    }

    /**
     * Get total cache size in megabytes.
     *
     * @return size in MB
     */
    public long getSizeMB() {
        return sizeBytes.get() / (1024 * 1024);
    }

    /**
     * Get cache hit ratio.
     *
     * @return hit ratio (0.0 to 1.0)
     */
    public double getHitRatio() {
        long totalRequests = hits.get() + misses.get();
        if (totalRequests == 0) {
            return 0.0;
        }
        return (double) hits.get() / totalRequests;
    }

    /**
     * Reset all statistics.
     */
    public void reset() {
        hits.set(0);
        misses.set(0);
        entries.set(0);
        sizeBytes.set(0);
    }

    @Override
    public String toString() {
        return String.format(
                "CacheStatistics[hits=%d, misses=%d, entries=%d, sizeMB=%d, hitRatio=%.2f%%]",
                getHits(), getMisses(), getEntries(), getSizeMB(), getHitRatio() * 100);
    }
}
