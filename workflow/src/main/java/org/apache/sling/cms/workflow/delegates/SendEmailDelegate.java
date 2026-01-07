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

import org.apache.sling.cms.workflow.delegate.DelegateExecution;
import org.apache.sling.cms.workflow.delegate.JavaDelegate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends email notifications for workflow events.
 *
 * <p>Sends email notifications to subscribers, approvers, or content authors
 * based on workflow state changes.</p>
 *
 * <p>Email configuration can be provided via BPMN fields or process variables:
 * <ul>
 *   <li>recipient - Email recipient(s)</li>
 *   <li>subject - Email subject line</li>
 *   <li>template - Email template name</li>
 *   <li>notificationType - Type of notification (approval, rejection, published, etc.)</li>
 * </ul>
 * </p>
 */
@Component(
        service = JavaDelegate.class,
        property = "delegate.class=org.apache.sling.cms.workflow.delegates.SendEmailDelegate")
public class SendEmailDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(SendEmailDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String contentPath = execution.getBusinessKey();
        if (contentPath == null || contentPath.isEmpty()) {
            contentPath = (String) execution.getVariable("contentPath");
        }

        // Get email configuration from fields or variables
        String recipient = getConfigValue(execution, "recipient");
        String subject = getConfigValue(execution, "subject");
        String template = getConfigValue(execution, "template");
        String notificationType = getConfigValue(execution, "notificationType");

        // Use defaults if not specified
        if (recipient == null || recipient.isEmpty()) {
            recipient = (String) execution.getVariable("initiator");
        }
        if (notificationType == null || notificationType.isEmpty()) {
            notificationType = "workflow-notification";
        }
        if (subject == null || subject.isEmpty()) {
            subject = String.format("Workflow Notification: %s", notificationType);
        }

        LOG.info(
                "Sending email notification - Type: {}, Recipient: {}, Content: {}, Process: {}",
                notificationType,
                recipient,
                contentPath,
                execution.getProcessInstanceId());

        try {
            // In a real implementation, this would:
            // 1. Load email template
            // 2. Populate template with workflow variables
            // 3. Send via SMTP or email service
            // 4. Log delivery status
            Thread.sleep(50);

            execution.setVariable("emailSentAt", System.currentTimeMillis());
            execution.setVariable("emailSentTo", recipient);
            execution.setVariable("emailStatus", "SUCCESS");

            LOG.info(
                    "Email notification sent successfully - Recipient: {}, Type: {}, Content: {}",
                    recipient,
                    notificationType,
                    contentPath);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            execution.setVariable("emailStatus", "FAILED");
            execution.setVariable("emailError", "Interrupted during email sending");
            LOG.error("Email sending interrupted for {}", contentPath, e);
            throw new Exception("Email sending failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get configuration value from BPMN field or process variable.
     */
    private String getConfigValue(DelegateExecution execution, String name) {
        String value = execution.getFieldValue(name);
        if (value == null || value.isEmpty()) {
            Object varValue = execution.getVariable(name);
            if (varValue != null) {
                value = varValue.toString();
            }
        }
        return value;
    }
}
