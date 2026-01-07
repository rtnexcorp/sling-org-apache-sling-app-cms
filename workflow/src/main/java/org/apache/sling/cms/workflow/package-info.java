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

/**
 * BPMN 2.0 compliant workflow API for Apache Sling CMS.
 *
 * <p>This package provides a workflow engine API compatible with BPMN 2.0 standard,
 * allowing seamless migration from native Sling implementation to Flowable/Camunda
 * engines without code changes.</p>
 *
 * <p>Core interfaces:</p>
 * <ul>
 *   <li>{@link org.apache.sling.cms.workflow.ProcessEngine} - Main workflow engine</li>
 *   <li>{@link org.apache.sling.cms.workflow.RuntimeService} - Process instance management</li>
 *   <li>{@link org.apache.sling.cms.workflow.TaskService} - User task management</li>
 *   <li>{@link org.apache.sling.cms.workflow.RepositoryService} - Process definition management</li>
 * </ul>
 *
 * @version 1.0.0
 */
@Version("1.0.0")
package org.apache.sling.cms.workflow;

import org.osgi.annotation.versioning.Version;
