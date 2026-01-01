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
package org.apache.sling.cms.preview;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service for managing preview tokens for unpublished content.
 * Preview tokens enable temporary access to draft content without authentication.
 */
@ProviderType
public interface PreviewTokenManager {

    /**
     * Generates a new preview token for the given resource.
     * The token is stored in the resource's jcr:content node.
     *
     * @param resource the resource to create a preview token for
     * @param timeoutSeconds the token validity duration in seconds (0 for no expiration)
     * @return the generated preview token
     * @throws PersistenceException if unable to store the token
     */
    @NotNull
    PreviewToken generateToken(@NotNull Resource resource, int timeoutSeconds) throws PersistenceException;

    /**
     * Retrieves the preview token for a resource, if it exists.
     *
     * @param resource the resource to get the preview token for
     * @return the preview token, or null if no token exists
     */
    @Nullable
    PreviewToken getToken(@NotNull Resource resource);

    /**
     * Validates a preview token for the given resource.
     *
     * @param resource the resource to validate the token for
     * @param token the token string to validate
     * @return true if the token is valid and not expired, false otherwise
     */
    boolean validateToken(@NotNull Resource resource, @NotNull String token);

    /**
     * Revokes (removes) the preview token for a resource.
     *
     * @param resource the resource whose token should be revoked
     * @throws PersistenceException if unable to remove the token
     */
    void revokeToken(@NotNull Resource resource) throws PersistenceException;

    /**
     * Generates a preview URL for the given resource with a valid token.
     * If no token exists, a new one is generated.
     *
     * @param resource the resource to generate a preview URL for
     * @param timeoutSeconds the token validity duration in seconds (0 for no expiration)
     * @return the full preview URL including the token parameter
     * @throws PersistenceException if unable to generate or store the token
     */
    @NotNull
    String generatePreviewUrl(@NotNull Resource resource, int timeoutSeconds) throws PersistenceException;
}
