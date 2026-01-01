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
package org.apache.sling.cms.distribution.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for the Content Distribution Service.
 */
@ObjectClassDefinition(
        name = "Apache Sling CMS - Content Distribution Configuration",
        description = "Configuration for content distribution from Author to Publisher")
public @interface ContentDistributionConfig {

    @AttributeDefinition(name = "Enabled", description = "Enable content distribution")
    boolean enabled() default true;

    @AttributeDefinition(
            name = "Publisher Endpoints",
            description = "URLs of publisher instances (e.g., http://localhost:8083)")
    String[] publisherEndpoints() default {};

    @AttributeDefinition(name = "Publisher Username", description = "Username for authentication to publisher")
    String publisherUsername() default "admin";

    @AttributeDefinition(name = "Publisher Password", description = "Password for authentication to publisher")
    String publisherPassword() default "admin";

    @AttributeDefinition(name = "Connection Timeout", description = "Connection timeout in milliseconds")
    int connectionTimeout() default 30000;

    @AttributeDefinition(name = "Socket Timeout", description = "Socket timeout in milliseconds")
    int socketTimeout() default 60000;

    @AttributeDefinition(name = "Allowed Roots", description = "Content roots that can be distributed")
    String[] allowedRoots() default {"/content", "/conf", "/etc/taxonomy"};

    @AttributeDefinition(
            name = "Use Content Package",
            description = "Use FileVault content package for distribution (recommended for deep trees)")
    boolean useContentPackage() default true;

    @AttributeDefinition(name = "Async Distribution", description = "Perform distribution asynchronously")
    boolean asyncDistribution() default true;
}
