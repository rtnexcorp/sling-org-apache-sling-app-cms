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
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

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

            // Handle boolean fields specially - unchecked checkboxes don't send a parameter
            if (param == null) {
                if (field.getType() == org.apache.sling.cms.schema.FieldType.BOOLEAN) {
                    // Checkbox was unchecked, set to false
                    Node fieldNode;
                    if (fragmentNode.hasNode(fieldName)) {
                        fieldNode = fragmentNode.getNode(fieldName);
                    } else {
                        fieldNode = fragmentNode.addNode(fieldName, "nt:unstructured");
                        fieldNode.setProperty("fieldType", field.getType().name());
                    }
                    fieldNode.setProperty("value", false);
                    log.debug("Set unchecked boolean field {} to false", fieldName);
                }
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
                setTypedValue(fieldNode, field, fieldValue);
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

    private void setTypedValue(Node fieldNode, SchemaField field, String fieldValue) throws RepositoryException {
        // Ensure we don't keep an old String-typed property around.
        // Removing first forces JCR to store the new value with the correct type.
        if (fieldNode.hasProperty("value")) {
            Property existing = fieldNode.getProperty("value");
            if (existing.getDefinition() == null || existing.getType() == javax.jcr.PropertyType.STRING) {
                existing.remove();
            }
        }

        switch (field.getType()) {
            case BOOLEAN:
                fieldNode.setProperty("value", Boolean.parseBoolean(fieldValue));
                break;
            case INTEGER:
                try {
                    fieldNode.setProperty("value", Long.parseLong(fieldValue));
                } catch (NumberFormatException nfe) {
                    fieldNode.setProperty("value", fieldValue);
                }
                break;
            case DECIMAL:
                try {
                    fieldNode.setProperty("value", new BigDecimal(fieldValue));
                } catch (NumberFormatException nfe) {
                    fieldNode.setProperty("value", fieldValue);
                }
                break;
            case DATE:
                // Expect yyyy-MM-dd (matches HTML date input)
                if (!setCalendarValue(fieldNode, fieldValue, "yyyy-MM-dd")) {
                    // If parsing fails, fall back to String rather than losing user input
                    fieldNode.setProperty("value", fieldValue);
                }
                break;
            case DATETIME:
                // Accept ISO-ish formats commonly produced by date/time widgets
                // (yyyy-MM-dd'T'HH:mm or yyyy-MM-dd'T'HH:mm:ss)
                if (!setCalendarValue(fieldNode, fieldValue, "yyyy-MM-dd'T'HH:mm:ss")
                        && !setCalendarValue(fieldNode, fieldValue, "yyyy-MM-dd'T'HH:mm")) {
                    fieldNode.setProperty("value", fieldValue);
                }
                break;
            default:
                fieldNode.setProperty("value", fieldValue);
                break;
        }

        if (fieldNode.hasProperty("value")) {
            log.debug(
                    "Stored fragment field {} as JCR type {}",
                    field.getName(),
                    javax.jcr.PropertyType.nameFromValue(
                            fieldNode.getProperty("value").getType()));
        }
    }

    private boolean setCalendarValue(Node fieldNode, String value, String pattern) throws RepositoryException {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setLenient(false);
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(value));
            fieldNode.setProperty("value", cal);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}
