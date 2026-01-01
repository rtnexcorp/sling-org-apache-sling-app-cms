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
package org.apache.sling.cms.core.internal.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.schema.ContentSchema;
import org.apache.sling.cms.schema.FieldType;
import org.apache.sling.cms.schema.SchemaField;
import org.apache.sling.cms.schema.ValidationResult;
import org.jetbrains.annotations.NotNull;

/**
 * Validator for content against schemas.
 */
class SchemaValidator {

    private final ContentSchema schema;

    SchemaValidator(ContentSchema schema) {
        this.schema = schema;
    }

    ValidationResult validate(Resource content) {
        ValidationResultImpl result = new ValidationResultImpl();
        ValueMap properties = content.getValueMap();

        for (SchemaField field : schema.getFields()) {
            validateField(field, properties, result);
        }

        return result;
    }

    private void validateField(SchemaField field, ValueMap properties, ValidationResultImpl result) {
        String fieldName = field.getName();
        Object value = properties.get(fieldName);

        // Required validation
        if (field.isRequired() && isEmpty(value)) {
            result.addError(new ValidationErrorImpl(fieldName, "Field is required", "required", null));
            return;
        }

        // Skip further validation if field is empty and not required
        if (isEmpty(value)) {
            return;
        }

        // Type-specific validation
        validateByType(field, value, result);

        // Custom validation rules
        validateCustomRules(field, value, result);
    }

    private void validateByType(SchemaField field, Object value, ValidationResultImpl result) {
        FieldType type = field.getType();
        String fieldName = field.getName();

        switch (type) {
            case INTEGER:
                if (!isInteger(value)) {
                    result.addError(new ValidationErrorImpl(
                            fieldName,
                            "Value must be an integer",
                            "type",
                            "Expected integer, got: " + value.getClass().getSimpleName()));
                }
                break;

            case DECIMAL:
                if (!isDecimal(value)) {
                    result.addError(new ValidationErrorImpl(
                            fieldName,
                            "Value must be a decimal number",
                            "type",
                            "Expected decimal, got: " + value.getClass().getSimpleName()));
                }
                break;

            case BOOLEAN:
                if (!isBoolean(value)) {
                    result.addError(new ValidationErrorImpl(
                            fieldName,
                            "Value must be a boolean",
                            "type",
                            "Expected boolean, got: " + value.getClass().getSimpleName()));
                }
                break;

            default:
                // String types don't need special type validation
                break;
        }
    }

    private void validateCustomRules(SchemaField field, Object value, ValidationResultImpl result) {
        Map<String, Object> validationRules = field.getValidation();
        String fieldName = field.getName();

        // Min length validation
        if (validationRules.containsKey("minLength")) {
            Integer minLength = getInteger(validationRules.get("minLength"));
            if (minLength != null && value instanceof String) {
                String strValue = (String) value;
                if (strValue.length() < minLength) {
                    result.addError(new ValidationErrorImpl(
                            fieldName,
                            String.format("Minimum length is %d characters", minLength),
                            "minLength",
                            String.format("Current length: %d", strValue.length())));
                }
            }
        }

        // Max length validation
        if (validationRules.containsKey("maxLength")) {
            Integer maxLength = getInteger(validationRules.get("maxLength"));
            if (maxLength != null && value instanceof String) {
                String strValue = (String) value;
                if (strValue.length() > maxLength) {
                    result.addError(new ValidationErrorImpl(
                            fieldName,
                            String.format("Maximum length is %d characters", maxLength),
                            "maxLength",
                            String.format("Current length: %d", strValue.length())));
                }
            }
        }

        // Min value validation
        if (validationRules.containsKey("min")) {
            Number min = getNumber(validationRules.get("min"));
            Number numValue = getNumber(value);
            if (min != null && numValue != null) {
                if (numValue.doubleValue() < min.doubleValue()) {
                    result.addError(new ValidationErrorImpl(
                            fieldName,
                            String.format("Minimum value is %s", min),
                            "min",
                            String.format("Current value: %s", numValue)));
                }
            }
        }

        // Max value validation
        if (validationRules.containsKey("max")) {
            Number max = getNumber(validationRules.get("max"));
            Number numValue = getNumber(value);
            if (max != null && numValue != null) {
                if (numValue.doubleValue() > max.doubleValue()) {
                    result.addError(new ValidationErrorImpl(
                            fieldName,
                            String.format("Maximum value is %s", max),
                            "max",
                            String.format("Current value: %s", numValue)));
                }
            }
        }

        // Pattern validation (regex)
        if (validationRules.containsKey("pattern")) {
            String pattern = String.valueOf(validationRules.get("pattern"));
            if (value instanceof String) {
                String strValue = (String) value;
                if (!strValue.matches(pattern)) {
                    result.addError(new ValidationErrorImpl(
                            fieldName,
                            "Value does not match required pattern",
                            "pattern",
                            String.format("Pattern: %s", pattern)));
                }
            }
        }
    }

    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return StringUtils.isBlank((String) value);
        }
        if (value instanceof Object[]) {
            return ((Object[]) value).length == 0;
        }
        return false;
    }

    private boolean isInteger(Object value) {
        return value instanceof Integer || value instanceof Long;
    }

    private boolean isDecimal(Object value) {
        return value instanceof Number;
    }

    private boolean isBoolean(Object value) {
        return value instanceof Boolean;
    }

    private Integer getInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Number getNumber(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Implementation of ValidationResult.
     */
    static class ValidationResultImpl implements ValidationResult {

        private final List<ValidationError> errors = new ArrayList<>();
        private final List<ValidationError> warnings = new ArrayList<>();

        void addError(ValidationError error) {
            errors.add(error);
        }

        void addWarning(ValidationError warning) {
            warnings.add(warning);
        }

        @Override
        public boolean isValid() {
            return errors.isEmpty();
        }

        @Override
        @NotNull
        public List<ValidationError> getErrors() {
            return new ArrayList<>(errors);
        }

        @Override
        @NotNull
        public List<ValidationError> getWarnings() {
            return new ArrayList<>(warnings);
        }

        @Override
        public int getErrorCount() {
            return errors.size();
        }

        @Override
        public int getWarningCount() {
            return warnings.size();
        }
    }

    /**
     * Implementation of ValidationError.
     */
    static class ValidationErrorImpl implements ValidationResult.ValidationError {

        private final String fieldName;
        private final String message;
        private final String rule;
        private final String context;

        ValidationErrorImpl(String fieldName, String message, String rule, String context) {
            this.fieldName = fieldName;
            this.message = message;
            this.rule = rule;
            this.context = context;
        }

        @Override
        @NotNull
        public String getFieldName() {
            return fieldName;
        }

        @Override
        @NotNull
        public String getMessage() {
            return message;
        }

        @Override
        @NotNull
        public String getRule() {
            return rule;
        }

        @Override
        public String getContext() {
            return context;
        }
    }
}
