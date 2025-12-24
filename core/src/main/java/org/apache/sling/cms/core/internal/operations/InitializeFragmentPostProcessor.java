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
package org.apache.sling.cms.core.internal.operations;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.schema.ContentSchema;
import org.apache.sling.cms.schema.FieldType;
import org.apache.sling.cms.schema.SchemaField;
import org.apache.sling.cms.schema.SchemaManager;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Post-processor that initializes content fragments with schema structure.
 * When a fragment is created with a schemaId, this processor creates child nodes
 * for each field defined in the schema.
 */
@Component(
        service = {SlingPostProcessor.class},
        property = {"service.ranking:Integer=100"})
public class InitializeFragmentPostProcessor implements SlingPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(InitializeFragmentPostProcessor.class);
    private static final String FRAGMENT_RESOURCE_TYPE = "sling-cms/components/cms/fragment";

    @Reference
    private SchemaManager schemaManager;

    @Override
    public void process(SlingHttpServletRequest request, java.util.List<Modification> changes) throws Exception {

        // Check if this is a fragment creation request
        String resourceType = request.getParameter("sling:resourceType");
        if (!FRAGMENT_RESOURCE_TYPE.equals(resourceType)) {
            return;
        }

        // Get the schemaId parameter
        String schemaId = request.getParameter("schemaId");
        if (schemaId == null || schemaId.isEmpty()) {
            log.debug("No schemaId provided, skipping fragment initialization");
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();

        // Find the created resource from modifications
        Resource createdResource = null;
        for (Modification mod : changes) {
            if (mod.getType() == ModificationType.CREATE) {
                createdResource = resolver.getResource(mod.getSource());
                if (createdResource != null && FRAGMENT_RESOURCE_TYPE.equals(createdResource.getResourceType())) {
                    break;
                }
            }
        }

        if (createdResource == null) {
            log.debug("Could not find created fragment resource");
            return;
        }

        // Get the schema
        ContentSchema schema = schemaManager.getSchema(createdResource, schemaId);
        if (schema == null) {
            log.warn("Schema not found: {}", schemaId);
            return;
        }

        // Initialize fragment structure based on schema
        initializeFragmentStructure(createdResource, schema);

        log.info("Initialized fragment {} with schema {}", createdResource.getPath(), schemaId);
    }

    /**
     * Creates child nodes for each field in the schema
     */
    private void initializeFragmentStructure(Resource fragmentResource, ContentSchema schema)
            throws RepositoryException {

        Node fragmentNode = fragmentResource.adaptTo(Node.class);
        if (fragmentNode == null) {
            log.error("Could not adapt fragment resource to Node: {}", fragmentResource.getPath());
            return;
        }

        Session session = fragmentNode.getSession();

        // Create a child node for each schema field
        for (SchemaField field : schema.getFields()) {
            String fieldName = field.getName();

            // Skip if field node already exists
            if (fragmentNode.hasNode(fieldName)) {
                continue;
            }

            // Create field node
            Node fieldNode = fragmentNode.addNode(fieldName, "nt:unstructured");

            // Set field properties based on type
            FieldType fieldType = field.getType();
            initializeFieldNode(fieldNode, field, fieldType);

            log.debug("Created field node: {} of type {}", fieldName, fieldType);
        }

        session.save();
    }

    /**
     * Initialize field node with default values based on field type
     */
    private void initializeFieldNode(Node fieldNode, SchemaField field, FieldType fieldType)
            throws RepositoryException {

        // Set field type as property for reference
        fieldNode.setProperty("fieldType", fieldType.name());

        // Initialize with default value if specified
        Object defaultValue = field.getDefaultValue();
        if (defaultValue != null) {
            if (defaultValue instanceof String) {
                fieldNode.setProperty("value", (String) defaultValue);
            } else if (defaultValue instanceof Boolean) {
                fieldNode.setProperty("value", (Boolean) defaultValue);
            } else if (defaultValue instanceof Long) {
                fieldNode.setProperty("value", (Long) defaultValue);
            } else if (defaultValue instanceof Double) {
                fieldNode.setProperty("value", (Double) defaultValue);
            }
        }

        // For array/repeating fields, create a container structure
        if (field.isMultiple()) {
            // Arrays will store values as multi-value properties or child nodes
            fieldNode.setProperty("multiple", true);
        }
    }
}
