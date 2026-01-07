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
package org.apache.sling.cms.workflow.test;

import org.apache.sling.cms.workflow.delegate.DelegateExecution;
import org.apache.sling.cms.workflow.delegate.JavaDelegate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test JavaDelegate for service task execution.
 *
 * This simulates a publish action in the workflow.
 */
@Component(
        service = JavaDelegate.class,
        property = "delegate.class=org.apache.sling.cms.workflow.test.TestPublishDelegate")
public class TestPublishDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(TestPublishDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        LOG.info("Executing TestPublishDelegate for process instance: {}", execution.getProcessInstanceId());

        // Get variables
        String contentPath = (String) execution.getVariable("contentPath");
        String author = (String) execution.getVariable("author");

        LOG.info("Publishing content: {} by author: {}", contentPath, author);

        // Simulate publish action
        try {
            Thread.sleep(100); // Simulate some work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Set result variable
        execution.setVariable("publishedAt", System.currentTimeMillis());
        execution.setVariable("publishStatus", "SUCCESS");

        LOG.info("Content published successfully: {}", contentPath);
    }
}
