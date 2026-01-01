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
package org.apache.sling.cms.distribution;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Result of a distribution operation.
 */
public class DistributionResult {

    public enum Status {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILURE,
        NOT_AVAILABLE
    }

    private final Status status;
    private final String message;
    private final String path;
    private final Map<String, EndpointResult> endpointResults;

    public DistributionResult(Status status, String message, String path) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.endpointResults = new HashMap<>();
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public void addEndpointResult(String endpoint, boolean success, String message) {
        endpointResults.put(endpoint, new EndpointResult(endpoint, success, message));
    }

    public Map<String, EndpointResult> getEndpointResults() {
        return Collections.unmodifiableMap(endpointResults);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Result for a specific endpoint.
     */
    public static class EndpointResult {
        private final String endpoint;
        private final boolean success;
        private final String message;

        public EndpointResult(String endpoint, boolean success, String message) {
            this.endpoint = endpoint;
            this.success = success;
            this.message = message;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    @Override
    public String toString() {
        return "DistributionResult{" + "status="
                + status + ", message='"
                + message + '\'' + ", path='"
                + path + '\'' + ", endpointResults="
                + endpointResults.size() + '}';
    }
}
