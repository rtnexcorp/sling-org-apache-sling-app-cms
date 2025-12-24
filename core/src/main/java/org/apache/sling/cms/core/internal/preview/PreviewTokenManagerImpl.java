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
package org.apache.sling.cms.core.internal.preview;

import java.util.Calendar;
import java.util.UUID;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.CMSConstants;
import org.apache.sling.cms.CMSUtils;
import org.apache.sling.cms.preview.PreviewToken;
import org.apache.sling.cms.preview.PreviewTokenManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of PreviewTokenManager service.
 */
@Component(service = PreviewTokenManager.class)
public class PreviewTokenManagerImpl implements PreviewTokenManager {

    private static final Logger log = LoggerFactory.getLogger(PreviewTokenManagerImpl.class);

    @Override
    @NotNull
    public PreviewToken generateToken(@NotNull Resource resource, int timeoutSeconds) throws PersistenceException {
        log.debug("Generating preview token for resource: {}", resource.getPath());

        Resource contentResource = getContentResource(resource);
        if (contentResource == null) {
            throw new PersistenceException("Unable to find jcr:content for resource: " + resource.getPath());
        }

        ModifiableValueMap properties = contentResource.adaptTo(ModifiableValueMap.class);
        if (properties == null) {
            throw new PersistenceException("Unable to get modifiable properties for: " + contentResource.getPath());
        }

        String token = UUID.randomUUID().toString();
        Calendar expiry = null;

        if (timeoutSeconds > 0) {
            expiry = Calendar.getInstance();
            expiry.add(Calendar.SECOND, timeoutSeconds);
            properties.put(CMSConstants.PN_PREVIEW_EXPIRY, expiry);
        } else {
            properties.remove(CMSConstants.PN_PREVIEW_EXPIRY);
        }

        properties.put(CMSConstants.PN_PREVIEW_TOKEN, token);

        String userId = resource.getResourceResolver().getUserID();
        properties.put(CMSConstants.PN_PREVIEW_CREATED_BY, userId);

        resource.getResourceResolver().commit();

        log.info("Preview token generated for {} by user {}, expires: {}", resource.getPath(), userId, expiry);

        return new PreviewTokenImpl(token, expiry, userId);
    }

    @Override
    @Nullable
    public PreviewToken getToken(@NotNull Resource resource) {
        Resource contentResource = getContentResource(resource);
        if (contentResource == null) {
            return null;
        }

        ValueMap properties = contentResource.getValueMap();
        String token = properties.get(CMSConstants.PN_PREVIEW_TOKEN, String.class);

        if (token == null || token.isEmpty()) {
            return null;
        }

        Calendar expiry = properties.get(CMSConstants.PN_PREVIEW_EXPIRY, Calendar.class);
        String createdBy = properties.get(CMSConstants.PN_PREVIEW_CREATED_BY, String.class);

        return new PreviewTokenImpl(token, expiry, createdBy);
    }

    @Override
    public boolean validateToken(@NotNull Resource resource, @NotNull String token) {
        PreviewToken previewToken = getToken(resource);

        if (previewToken == null) {
            log.debug("No preview token found for resource: {}", resource.getPath());
            return false;
        }

        if (!previewToken.getToken().equals(token)) {
            log.debug("Preview token mismatch for resource: {}", resource.getPath());
            return false;
        }

        if (previewToken.isExpired()) {
            log.debug("Preview token expired for resource: {}", resource.getPath());
            return false;
        }

        log.debug("Preview token validated for resource: {}", resource.getPath());
        return true;
    }

    @Override
    public void revokeToken(@NotNull Resource resource) throws PersistenceException {
        log.debug("Revoking preview token for resource: {}", resource.getPath());

        Resource contentResource = getContentResource(resource);
        if (contentResource == null) {
            return;
        }

        ModifiableValueMap properties = contentResource.adaptTo(ModifiableValueMap.class);
        if (properties == null) {
            throw new PersistenceException("Unable to get modifiable properties for: " + contentResource.getPath());
        }

        properties.remove(CMSConstants.PN_PREVIEW_TOKEN);
        properties.remove(CMSConstants.PN_PREVIEW_EXPIRY);
        properties.remove(CMSConstants.PN_PREVIEW_CREATED_BY);

        resource.getResourceResolver().commit();

        log.info("Preview token revoked for {}", resource.getPath());
    }

    @Override
    @NotNull
    public String generatePreviewUrl(@NotNull Resource resource, int timeoutSeconds) throws PersistenceException {
        PreviewToken token = getToken(resource);

        if (token == null || token.isExpired()) {
            token = generateToken(resource, timeoutSeconds);
        }

        // Use the resource path directly for a short, relative URL
        // This works on the current instance (author) where the preview is being generated
        String resourcePath = resource.getPath();

        // For pages, use .html extension
        String previewUrl = resourcePath + ".html?" + CMSConstants.PARAM_PREVIEW_TOKEN + "=" + token.getToken();

        log.debug("Generated preview URL: {}", previewUrl);
        return previewUrl;
    }

    /**
     * Gets the jcr:content child resource for a publishable resource.
     *
     * @param resource the resource
     * @return the jcr:content resource, or null if not found
     */
    @Nullable
    private Resource getContentResource(@NotNull Resource resource) {
        Resource publishableParent = CMSUtils.findPublishableParent(resource);
        if (publishableParent == null) {
            return null;
        }
        return publishableParent.getChild(JcrConstants.JCR_CONTENT);
    }
}
