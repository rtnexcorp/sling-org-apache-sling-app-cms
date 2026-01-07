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
package org.apache.sling.cms.workflow.delegate;

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Interface for implementing service task logic.
 *
 * <p>Compatible with Flowable/Camunda JavaDelegate pattern.</p>
 *
 * <p>Service task implementations should be registered as OSGi services
 * with a property "delegate.class" matching the fully qualified class name
 * used in BPMN XML.</p>
 *
 * <p>Example:</p>
 * <pre>
 * &#64;Component(
 *     service = JavaDelegate.class,
 *     property = {
 *         "delegate.class=org.apache.sling.cms.workflow.delegate.PublishContentDelegate"
 *     }
 * )
 * public class PublishContentDelegate implements JavaDelegate {
 *
 *     &#64;Override
 *     public void execute(DelegateExecution execution) throws Exception {
 *         String contentPath = (String) execution.getVariable("contentPath");
 *         // Publish content logic
 *     }
 * }
 * </pre>
 */
@ConsumerType
@FunctionalInterface
public interface JavaDelegate {

    /**
     * Execute service task logic.
     *
     * @param execution Current process execution context
     * @throws Exception if execution fails
     */
    void execute(@NotNull DelegateExecution execution) throws Exception;
}
