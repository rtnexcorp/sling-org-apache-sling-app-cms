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
/**
 * Personalization API for Apache Sling CMS.
 * <p>
 * This package provides the core interfaces for implementing personalization
 * features in Apache Sling CMS, including:
 * </p>
 * <ul>
 *   <li>Segment evaluation and management</li>
 *   <li>Pluggable segment evaluators</li>
 *   <li>Component variant resolution</li>
 *   <li>A/B testing and experimentation</li>
 * </ul>
 * <p>
 * The personalization framework follows Apache Sling best practices:
 * </p>
 * <ul>
 *   <li>Configuration stored in Context-Aware Configuration under {@code /conf/}</li>
 *   <li>Pluggable evaluators registered as OSGi services</li>
 *   <li>Resource-based variant storage in content hierarchy</li>
 *   <li>Request-scoped caching for performance</li>
 * </ul>
 *
 * @version 1.0.0
 */
@org.osgi.annotation.versioning.Version("1.0.0")
package org.apache.sling.cms.personalization;
