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
package org.apache.sling.thumbnails;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Model representing a transformation, a series of handlers
 *
 * @deprecated Use {@link org.apache.sling.cms.transformation.Transformation} instead.
 *             This interface will be removed in version 2.0.0.
 */
@Deprecated
@ProviderType
public interface Transformation extends org.apache.sling.cms.transformation.Transformation {

    @Override
    @Deprecated
    List<org.apache.sling.cms.transformation.TransformationHandlerConfig> getHandlers();

    @Override
    @Deprecated
    String getName();

    @Override
    @Deprecated
    String getPath();
}
