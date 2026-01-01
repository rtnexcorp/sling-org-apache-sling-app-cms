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
package org.apache.sling.cms.core.internal.enrichers;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.cms.File;
import org.apache.sling.cms.FileMetadataEnricher;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Metadata enricher that uses Apache Tika to extract metadata from files.
 * This enricher extracts EXIF, IPTC, XMP, and other metadata formats
 * supported by Tika parsers.
 */
@Component(service = FileMetadataEnricher.class)
@Designate(ocd = TikaMetadataEnricher.Config.class)
public class TikaMetadataEnricher implements FileMetadataEnricher {

    private static final Logger log = LoggerFactory.getLogger(TikaMetadataEnricher.class);

    @ObjectClassDefinition(name = "Tika Metadata Enricher Configuration")
    public @interface Config {
        @AttributeDefinition(name = "Enabled", description = "Enable Tika metadata extraction")
        boolean enabled() default true;

        @AttributeDefinition(name = "Priority", description = "Enricher priority (higher runs first)")
        int priority() default 100;
    }

    @Reference
    private ResourceResolverFactory resolverFactory;

    private boolean enabled;
    private int priority;

    @Activate
    public void activate(Config config) {
        this.enabled = config.enabled();
        this.priority = config.priority();
        log.info("TikaMetadataEnricher activated - enabled: {}, priority: {}", enabled, priority);
    }

    @Override
    public String getName() {
        return "tika";
    }

    @Override
    public boolean shouldEnrich(File file) {
        return enabled;
    }

    @Override
    public void enrichMetadata(File file, Map<String, Object> metadata) throws IOException {
        log.debug("Extracting Tika metadata from {}", file.getPath());
        Resource resource = file.getResource();

        try (InputStream is = resource.adaptTo(InputStream.class)) {
            Parser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler();
            Metadata tikaMetadata = new Metadata();
            ParseContext context = new ParseContext();

            try {
                parser.parse(is, handler, tikaMetadata, context);
            } catch (SAXException se) {
                // unfortunately, we can't use instanceof to check as the class is not exported
                if ("WriteLimitReachedException".equals(se.getClass().getSimpleName())) {
                    log.debug("Write limit reached for {}", resource.getPath());
                } else {
                    throw new IOException("SAX parsing error", se);
                }
            } catch (TikaException e) {
                throw new IOException("Tika parsing error", e);
            }

            try (ResourceResolver adminResolver = resolverFactory.getAdministrativeResourceResolver(null)) {
                NamespaceRegistry registry =
                        adminResolver.adaptTo(Session.class).getWorkspace().getNamespaceRegistry();
                for (String name : tikaMetadata.names()) {
                    putMetadata(metadata, name, tikaMetadata, registry);
                }
            } catch (LoginException | RepositoryException e) {
                throw new IOException("Failed to access JCR repository", e);
            }
        }

        log.debug("Extracted {} metadata properties from {}", metadata.size(), file.getPath());
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public String formatKey(String initialKey, NamespaceRegistry registry) throws RepositoryException {
        String namespace = null;
        String key = null;
        if (initialKey.contains(":")) {
            namespace = StringUtils.substringBefore(initialKey, ":");
            key = StringUtils.substringAfter(initialKey, ":");
        } else {
            key = initialKey;
        }
        key = key.replace(" ", "").replace("/", "-");
        if (namespace != null) {
            namespace = namespace.replace(" ", "").replace("/", "-");
            if (!ArrayUtils.contains(registry.getPrefixes(), namespace)) {
                registry.registerNamespace(namespace, "http://sling.apache.org/cms/ns/" + namespace);
            }
            return namespace + ":" + key;
        } else {
            return key;
        }
    }

    private void putMetadata(
            Map<String, Object> properties, String name, Metadata tikaMetadata, NamespaceRegistry registry)
            throws RepositoryException {
        log.trace("Updating property: {}", name);
        String filtered = formatKey(name, registry);
        Property property = Property.get(name);
        if (property != null) {
            if (tikaMetadata.isMultiValued(property)) {
                properties.put(filtered, tikaMetadata.getValues(property));
            } else if (tikaMetadata.getDate(property) != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(tikaMetadata.getDate(property));
                properties.put(filtered, cal);
            } else if (tikaMetadata.getInt(property) != null) {
                properties.put(filtered, tikaMetadata.getInt(property));
            } else {
                properties.put(filtered, tikaMetadata.get(property));
            }
        } else {
            properties.put(filtered, tikaMetadata.get(name));
        }
    }
}
