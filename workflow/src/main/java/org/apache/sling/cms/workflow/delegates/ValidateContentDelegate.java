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
package org.apache.sling.cms.workflow.delegates;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.sling.cms.workflow.delegate.DelegateExecution;
import org.apache.sling.cms.workflow.delegate.JavaDelegate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates content before publishing.
 *
 * <p>Performs comprehensive validation checks including:
 * <ul>
 *   <li>Path format validation</li>
 *   <li>Naming convention checks</li>
 *   <li>Security validation (no path traversal)</li>
 *   <li>Content structure validation</li>
 * </ul>
 * Sets the "isValid" variable to true/false for workflow gateway decisions.</p>
 */
@Component(
        service = JavaDelegate.class,
        property = "delegate.class=org.apache.sling.cms.workflow.delegates.ValidateContentDelegate")
public class ValidateContentDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(ValidateContentDelegate.class);

    // Valid content path patterns
    private static final Pattern VALID_PATH_PATTERN = Pattern.compile("^/content/[a-zA-Z0-9/_-]+$");
    private static final Pattern SECURITY_PATTERN = Pattern.compile("\\.\\.|\\.\\./|/\\./");

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        LOG.info(
                "Validating content for process instance: {}, businessKey: {}",
                execution.getProcessInstanceId(),
                execution.getBusinessKey());

        String contentPath = execution.getBusinessKey();
        if (contentPath == null || contentPath.isEmpty()) {
            contentPath = (String) execution.getVariable("contentPath");
        }

        List<String> validationErrors = new ArrayList<>();
        boolean isValid = validateContent(contentPath, validationErrors);

        String validationMessage = isValid ? "Content validation passed" : String.join("; ", validationErrors);

        // Set variables for workflow
        execution.setVariable("isValid", isValid);
        execution.setVariable("validationMessage", validationMessage);
        execution.setVariable("validatedAt", System.currentTimeMillis());
        execution.setVariable("validatedPath", contentPath);

        if (isValid) {
            LOG.info("Content validated successfully: {}", contentPath);
        } else {
            LOG.warn("Validation failed for {}: {}", contentPath, validationMessage);
        }

        LOG.debug("Validation complete - isValid: {}, message: {}, path: {}", isValid, validationMessage, contentPath);
    }

    /**
     * Performs comprehensive content validation.
     *
     * @param contentPath The content path to validate
     * @param errors List to collect validation errors
     * @return true if validation passes, false otherwise
     */
    private boolean validateContent(String contentPath, List<String> errors) {
        // Check if path is provided
        if (contentPath == null || contentPath.trim().isEmpty()) {
            errors.add("Content path is required");
            return false;
        }

        contentPath = contentPath.trim();

        // 1. Check if path starts with /content/
        if (!contentPath.startsWith("/content/")) {
            errors.add("Content path must start with /content/");
        }

        // 2. Security check - prevent path traversal attacks
        if (SECURITY_PATTERN.matcher(contentPath).find()) {
            errors.add("Content path contains invalid path traversal characters");
        }

        // 3. Validate path format (alphanumeric, hyphens, underscores, slashes only)
        if (!VALID_PATH_PATTERN.matcher(contentPath).matches()) {
            errors.add("Content path contains invalid characters (allowed: a-z, A-Z, 0-9, /, -, _)");
        }

        // 4. Check path depth (minimum depth check)
        String[] pathSegments = contentPath.split("/");
        if (pathSegments.length < 3) { // Should have at least /content/site/...
            errors.add("Content path is too shallow (expected at least /content/site/page)");
        }

        // 5. Check for consecutive slashes
        if (contentPath.contains("//")) {
            errors.add("Content path contains consecutive slashes");
        }

        // 6. Check if path ends with slash
        if (contentPath.endsWith("/") && !contentPath.equals("/content/")) {
            errors.add("Content path should not end with a slash");
        }

        return errors.isEmpty();
    }
}
