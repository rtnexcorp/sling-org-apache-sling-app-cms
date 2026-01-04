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
package org.apache.sling.cms.workflow.internal.model;

/**
 * BPMN activity types supported by SimpleBPMN engine.
 */
public enum ActivityType {
    /** Start event - workflow entry point */
    START_EVENT,

    /** End event - workflow completion */
    END_EVENT,

    /** User task - requires human interaction */
    USER_TASK,

    /** Service task - automated task executed by JavaDelegate */
    SERVICE_TASK,

    /** Exclusive gateway - decision point (if/else) */
    EXCLUSIVE_GATEWAY,

    /** Parallel gateway - fork/join parallel execution (limited support) */
    PARALLEL_GATEWAY,

    /** Intermediate catch event - waiting state */
    INTERMEDIATE_CATCH_EVENT,

    /** Unknown/unsupported activity type */
    UNKNOWN
}
