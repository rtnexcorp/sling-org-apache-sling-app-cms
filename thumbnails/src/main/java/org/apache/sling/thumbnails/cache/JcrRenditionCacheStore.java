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
package org.apache.sling.thumbnails.cache;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JCR-based implementation of RenditionCacheStore.
 *
 * <p>Stores renditions at: {asset}/jcr:content/renditions/{cacheKey}
 */
@Component(service = RenditionCacheStore.class)
public class JcrRenditionCacheStore implements RenditionCacheStore {

    private static final Logger log = LoggerFactory.getLogger(JcrRenditionCacheStore.class);
    private static final String RENDITIONS_PATH = "jcr:content/renditions";
    private static final String NT_FILE = "nt:file";
    private static final String NT_RESOURCE = "nt:resource";
    private static final String JCR_DATA = "jcr:data";
    private static final String JCR_MIMETYPE = "jcr:mimeType";
    private static final String JCR_LASTMODIFIED = "jcr:lastModified";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public Optional<InputStream> get(String cacheKey) {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-thumbnails");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            String[] parts = parseCacheKey(cacheKey);
            if (parts == null) {
                return Optional.empty();
            }

            String assetPath = parts[0];
            String renditionName = parts[1];
            String renditionPath = assetPath + "/" + RENDITIONS_PATH + "/" + renditionName;

            Resource renditionResource = resolver.getResource(renditionPath + "/jcr:content");
            if (renditionResource != null) {
                Node node = renditionResource.adaptTo(Node.class);
                if (node != null && node.hasProperty(JCR_DATA)) {
                    Binary binary = node.getProperty(JCR_DATA).getBinary();
                    byte[] data = IOUtils.toByteArray(binary.getStream());
                    binary.dispose();
                    return Optional.of(new ByteArrayInputStream(data));
                }
            }
        } catch (LoginException | RepositoryException | IOException e) {
            log.error("Error retrieving cached rendition: {}", cacheKey, e);
        }

        return Optional.empty();
    }

    @Override
    public void put(String cacheKey, InputStream data, String assetPath) {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-thumbnails");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            String renditionsPath = assetPath + "/" + RENDITIONS_PATH;
            Resource renditionsFolder = resolver.getResource(renditionsPath);

            if (renditionsFolder == null) {
                log.warn("Renditions folder not found: {}", renditionsPath);
                return;
            }

            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                log.error("Could not get JCR session");
                return;
            }

            Node renditionsNode = renditionsFolder.adaptTo(Node.class);
            if (renditionsNode == null) {
                log.error("Could not adapt renditions resource to Node");
                return;
            }

            // Extract rendition name from cache key
            String renditionName = extractRenditionName(cacheKey);

            // Create or update rendition node
            Node renditionNode;
            if (renditionsNode.hasNode(renditionName)) {
                renditionNode = renditionsNode.getNode(renditionName);
            } else {
                renditionNode = renditionsNode.addNode(renditionName, NT_FILE);
            }

            // Create or update jcr:content node
            Node contentNode;
            if (renditionNode.hasNode("jcr:content")) {
                contentNode = renditionNode.getNode("jcr:content");
            } else {
                contentNode = renditionNode.addNode("jcr:content", NT_RESOURCE);
            }

            // Store binary data
            Binary binary = session.getValueFactory().createBinary(data);
            contentNode.setProperty(JCR_DATA, binary);
            contentNode.setProperty(JCR_MIMETYPE, "image/jpeg"); // TODO: Determine from format
            contentNode.setProperty(JCR_LASTMODIFIED, Calendar.getInstance());

            session.save();
            binary.dispose();

            log.debug("Stored rendition in cache: {}", cacheKey);
        } catch (LoginException | RepositoryException e) {
            log.error("Error storing rendition in cache: {}", cacheKey, e);
        }
    }

    @Override
    public void invalidate(String assetPath) {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-thumbnails");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            String renditionsPath = assetPath + "/" + RENDITIONS_PATH;
            Resource renditionsFolder = resolver.getResource(renditionsPath);

            if (renditionsFolder != null) {
                resolver.delete(renditionsFolder);
                resolver.commit();
                log.info("Invalidated renditions for asset: {}", assetPath);
            }
        } catch (LoginException | PersistenceException e) {
            log.error("Error invalidating renditions for asset: {}", assetPath, e);
        }
    }

    @Override
    public void purgeAll() {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-thumbnails");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                log.error("Could not get JCR session");
                return;
            }

            QueryManager queryManager = session.getWorkspace().getQueryManager();
            String queryString = "SELECT * FROM [nt:folder] WHERE NAME() = 'renditions'";
            Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            QueryResult result = query.execute();

            int count = 0;
            Iterator<Node> nodes = result.getNodes();
            while (nodes.hasNext()) {
                Node renditionsNode = nodes.next();
                renditionsNode.remove();
                count++;
            }

            session.save();
            log.info("Purged {} rendition folders from cache", count);
        } catch (LoginException | RepositoryException e) {
            log.error("Error purging all renditions", e);
        }
    }

    @Override
    public void removeExpired(int maxAgeHours) {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-thumbnails");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                log.error("Could not get JCR session");
                return;
            }

            Calendar cutoffDate = Calendar.getInstance();
            cutoffDate.add(Calendar.HOUR, -maxAgeHours);

            QueryManager queryManager = session.getWorkspace().getQueryManager();
            String queryString = String.format(
                    "SELECT * FROM [nt:resource] WHERE [jcr:lastModified] < CAST('%tFT%<tT.%<tL%<tz' AS DATE)",
                    cutoffDate);
            Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            QueryResult result = query.execute();

            int count = 0;
            List<Node> toRemove = new ArrayList<>();
            Iterator<Node> nodes = result.getNodes();
            while (nodes.hasNext()) {
                Node contentNode = nodes.next();
                if (contentNode.getPath().contains("/renditions/")) {
                    toRemove.add(contentNode.getParent()); // Remove parent nt:file node
                }
            }

            for (Node node : toRemove) {
                node.remove();
                count++;
            }

            session.save();
            log.info("Removed {} expired renditions from cache", count);
        } catch (LoginException | RepositoryException e) {
            log.error("Error removing expired renditions", e);
        }
    }

    @Override
    public void enforceSizeLimit(int maxSizeMB) {
        // TODO: Implement size limit enforcement
        log.warn("Size limit enforcement not yet implemented");
    }

    @Override
    public long getEntryCount() {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "sling-cms-thumbnails");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                return 0;
            }

            QueryManager queryManager = session.getWorkspace().getQueryManager();
            String queryString = "SELECT * FROM [nt:file] AS file WHERE ISCHILDNODE(file, [/content])";
            Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            QueryResult result = query.execute();

            long count = 0;
            Iterator<Node> nodes = result.getNodes();
            while (nodes.hasNext()) {
                nodes.next();
                count++;
            }

            return count;
        } catch (LoginException | RepositoryException e) {
            log.error("Error counting cache entries", e);
            return 0;
        }
    }

    @Override
    public long getSizeBytes() {
        // TODO: Implement size calculation
        log.warn("Size calculation not yet implemented");
        return 0;
    }

    /**
     * Parse cache key to extract asset path.
     *
     * @param cacheKey the cache key
     * @return array of [assetPath, renditionName] or null if invalid
     */
    private String[] parseCacheKey(String cacheKey) {
        // Cache key format: {sanitizedPath}_{transformation}_{format}_{checksum}_{version}
        // We need to reverse the sanitization to get the original path
        // For now, this is a simplified implementation
        // TODO: Implement proper cache key parsing
        return null;
    }

    /**
     * Extract rendition name from cache key.
     *
     * @param cacheKey the cache key
     * @return the rendition name
     */
    private String extractRenditionName(String cacheKey) {
        // Use cache key as rendition name for simplicity
        return cacheKey.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
