/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.cms.reference.forms.impl.fields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.reference.forms.FieldHandler;
import org.apache.sling.cms.reference.forms.FormException;
import org.apache.sling.cms.reference.forms.FormUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for multifield form fields. Processes multiple sets of field values
 * and stores them as a list of maps.
 */
@Component(service = FieldHandler.class)
@Designate(ocd = MultifieldHandler.Config.class)
public class MultifieldHandler implements FieldHandler {

    private static final Logger log = LoggerFactory.getLogger(MultifieldHandler.class);
    public static final String DEFAULT_RESOURCE_TYPE = "reference/components/forms/fields/multifield";
    
    // Pattern to match multifield parameter names: fieldName[index]/subFieldName
    private static final Pattern MULTIFIELD_PATTERN = Pattern.compile("^(.+)\\[(\\d+)\\]/(.+)$");

    private Config config;

    @ObjectClassDefinition(name = "%cms.reference.multifield.name", description = "%cms.reference.multifield.description", localization = "OSGI-INF/l10n/bundle")
    public @interface Config {

        @AttributeDefinition(name = "%cms.reference.supportedTypes.name", description = "%cms.reference.supportedTypes.description", defaultValue = {
                DEFAULT_RESOURCE_TYPE })
        String[] supportedTypes() default { DEFAULT_RESOURCE_TYPE };
    }

    @Activate
    public MultifieldHandler(Config config) {
        this.config = config;
    }

    @Override
    public boolean handles(Resource fieldResource) {
        return FormUtils.handles(config.supportedTypes(), fieldResource);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleField(SlingHttpServletRequest request, Resource fieldResource, Map<String, Object> formData)
            throws FormException {
        log.trace("handleField");
        
        String name = FieldHandler.getName(fieldResource);
        int minItems = fieldResource.getValueMap().get("minItems", 0);
        int maxItems = fieldResource.getValueMap().get("maxItems", -1);
        
        // Collect all multifield items from the request
        Map<Integer, Map<String, String>> itemsMap = new HashMap<>();
        
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();
            String[] values = entry.getValue();
            Matcher matcher = MULTIFIELD_PATTERN.matcher(paramName);
            if (matcher.matches()) {
                String fieldName = matcher.group(1);
                if (fieldName.equals(name)) {
                    int index = Integer.parseInt(matcher.group(2));
                    String subFieldName = matcher.group(3);
                    String value = values != null && values.length > 0 ? values[0] : "";
                    
                    itemsMap.computeIfAbsent(index, k -> new HashMap<>())
                            .put(subFieldName, value);
                }
            }
        }
        
        // Convert to a sorted list
        List<Map<String, String>> items = new ArrayList<>();
        itemsMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> items.add(entry.getValue()));
        
        // Validate item count
        if (FieldHandler.isRequired(fieldResource) && items.isEmpty()) {
            throw new FormException("Field " + name + " requires at least one item");
        }
        
        if (minItems > 0 && items.size() < minItems) {
            throw new FormException("Field " + name + " requires at least " + minItems + " item(s)");
        }
        
        if (maxItems > 0 && items.size() > maxItems) {
            throw new FormException("Field " + name + " allows at most " + maxItems + " item(s)");
        }
        
        // Validate each item's required fields if template exists
        Resource templateResource = fieldResource.getChild("template");
        if (templateResource != null) {
            for (int i = 0; i < items.size(); i++) {
                Map<String, String> item = items.get(i);
                for (Resource subField : templateResource.getChildren()) {
                    String subFieldName = subField.getValueMap().get("name", String.class);
                    boolean subFieldRequired = subField.getValueMap().get("required", false);
                    
                    if (subFieldRequired && StringUtils.isBlank(item.get(subFieldName))) {
                        throw new FormException("Item " + (i + 1) + ": " + subFieldName + " is required");
                    }
                }
            }
        }
        
        log.debug("Setting multifield value for: {} with {} items", name, items.size());
        formData.put(name, items);
    }
}
