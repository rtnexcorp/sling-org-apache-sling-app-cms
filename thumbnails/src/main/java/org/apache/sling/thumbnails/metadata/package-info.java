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
 * Asset metadata extraction and enrichment pipeline
 *
 * <p>This package provides an extensible framework for automatic metadata extraction
 * from digital assets (images, videos, PDFs, etc.) with support for custom enrichers.
 *
 * <p>Key features:
 * <ul>
 *   <li>Automatic metadata extraction on asset upload/update</li>
 *   <li>EXIF/IPTC/XMP extraction for images</li>
 *   <li>Video duration/codec/bitrate extraction</li>
 *   <li>PDF metadata extraction</li>
 *   <li>Extensible enricher framework</li>
 *   <li>Storage at jcr:content/metadata/*</li>
 * </ul>
 *
 * @since 1.2.0
 */
@org.osgi.annotation.versioning.Version("1.2.0")
package org.apache.sling.thumbnails.metadata;
