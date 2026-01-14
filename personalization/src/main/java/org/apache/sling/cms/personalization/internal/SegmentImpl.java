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
package org.apache.sling.cms.personalization.internal;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.personalization.Segment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Internal implementation of {@link Segment} backed by a JCR resource.
 */
public class SegmentImpl implements Segment {

    private final String id;
    private final String title;
    private final String description;
    private final int priority;
    private final String evaluatorType;

    /**
     * Creates a segment from a resource.
     *
     * @param resource the segment configuration resource
     */
    public SegmentImpl(@NotNull Resource resource) {
        ValueMap properties = resource.getValueMap();

        this.id = resource.getName();
        this.title = properties.get("title", this.id);
        this.description = properties.get("description", String.class);
        this.priority = properties.get("priority", 0);
        this.evaluatorType = properties.get("evaluator", "");
    }

    @Override
    @NotNull
    public String getId() {
        return id;
    }

    @Override
    @NotNull
    public String getTitle() {
        return title;
    }

    @Override
    @Nullable
    public String getDescription() {
        return description;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    @NotNull
    public String getEvaluatorType() {
        return evaluatorType;
    }

    @Override
    public String toString() {
        return "Segment{" + "id='"
                + id + '\'' + ", title='"
                + title + '\'' + ", priority="
                + priority + ", evaluatorType='"
                + evaluatorType + '\'' + '}';
    }
}
