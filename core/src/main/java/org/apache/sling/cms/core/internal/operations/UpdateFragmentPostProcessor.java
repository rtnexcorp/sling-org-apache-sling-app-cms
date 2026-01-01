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
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.schema.ContentSchema;
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
 * Post-processor that updates content fragment field values in child nodes.
 * When a fragment is modified, this processor saves field values to the
 * appropriate child field nodes instead of directly on the fragment resource.
 */
@Component(
        service = {SlingPostProcessor.class},
        property = {"service.ranking:Integer=100"})
public class UpdateFragmentPostProcessor implements SlingPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(UpdateFragmentPostProcessor.class);
    private static final String FRAGMENT_RESOURCE_TYPE = "sling-cms/components/cms/fragment";

    @Reference
    private SchemaManager schemaManager;

    @Override
    public void process(SlingHttpServletRequest request, java.util.List<Modification> changes) throws Exception {

        ResourceResolver resolver = request.getResourceResolver();

        // Find modified fragment resources
        for (Modification mod : changes) {
            if (mod.getType() == ModificationType.MODIFY) {
                Resource modifiedResource = resolver.getResource(mod.getSource());

                if (modifiedResource != null && FRAGMENT_RESOURCE_TYPE.equals(modifiedResource.getResourceType())) {

                    String schemaId = modifiedResource.getValueMap().get("schemaId", String.class);
                    if (schemaId != null) {
                        updateFragmentFields(request, modifiedResource, schemaId);
                    }
                }
            }
        }
    }

    /**
     * Updates field values in child field nodes based on request parameters
     */
    private void updateFragmentFields(SlingHttpServletRequest request, Resource fragmentResource, String schemaId)
            throws RepositoryException {

        // Get the schema
        ContentSchema schema = schemaManager.getSchema(fragmentResource, schemaId);
        if (schema == null) {
            log.debug("Schema not found: {}", schemaId);
            return;
        }

        Node fragmentNode = fragmentResource.adaptTo(Node.class);
        if (fragmentNode == null) {
            log.error("Could not adapt fragment resource to Node: {}", fragmentResource.getPath());
            return;
        }

        Session session = fragmentNode.getSession();

        // Process each field from the schema
        for (SchemaField field : schema.getFields()) {
            String fieldName = field.getName();

            // Get the value from request parameters
            RequestParameter param = request.getRequestParameter(fieldName);
            if (param == null) {
                continue; // Field not in this request
            }

            String fieldValue = param.getString();

            // Get or create field node
            Node fieldNode;
            if (fragmentNode.hasNode(fieldName)) {
                fieldNode = fragmentNode.getNode(fieldName);
            } else {
                fieldNode = fragmentNode.addNode(fieldName, "nt:unstructured");
                fieldNode.setProperty("fieldType", field.getType().name());
            }

            // Update the value property
            if (fieldValue != null && !fieldValue.isEmpty()) {
                fieldNode.setProperty("value", fieldValue);
                log.debug("Updated field {} with value: {}", fieldName, fieldValue);
            } else {
                // Remove value property if empty
                if (fieldNode.hasProperty("value")) {
                    fieldNode.getProperty("value").remove();
                }
            }
        }

        session.save();
        log.info("Updated fragment fields: {}", fragmentResource.getPath());
    }
}
