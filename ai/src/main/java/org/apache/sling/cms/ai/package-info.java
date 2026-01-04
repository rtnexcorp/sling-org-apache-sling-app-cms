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
 * AI integration services for Apache Sling CMS.
 * <p>
 * This package provides vendor-neutral AI service interfaces that can be
 * implemented by different AI providers (OpenAI, Azure OpenAI, Anthropic,
 * local models, etc.) or rule-based fallback implementations.
 * </p>
 * <p>
 * Key interfaces:
 * </p>
 * <ul>
 *   <li>{@link org.apache.sling.cms.ai.AiService} - Base marker interface for all AI services</li>
 *   <li>{@link org.apache.sling.cms.ai.AiTextService} - Text generation, summarization, translation</li>
 *   <li>{@link org.apache.sling.cms.ai.AiTaxonomyService} - Taxonomy suggestions and content classification</li>
 *   <li>{@link org.apache.sling.cms.ai.AiImageService} - Image alt-text and caption generation</li>
 * </ul>
 *
 * @version 1.0.0
 */
@org.osgi.annotation.versioning.Version("1.0.0")
package org.apache.sling.cms.ai;
