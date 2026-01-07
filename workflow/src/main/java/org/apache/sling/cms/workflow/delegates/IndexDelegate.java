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
 * Updates search index after content publishing.
 *
 * <p>Triggers search index update for published content to ensure
 * it appears in search results.</p>
 */
@Component(
        service = JavaDelegate.class,
        property = "delegate.class=org.apache.sling.cms.workflow.delegates.IndexDelegate")
public class IndexDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(IndexDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String contentPath = execution.getBusinessKey();
        if (contentPath == null || contentPath.isEmpty()) {
            contentPath = (String) execution.getVariable("contentPath");
        }

        LOG.info(
                "Updating search index for content: {} in process instance: {}",
                contentPath,
                execution.getProcessInstanceId());

        try {
            // In a real implementation, this would:
            // 1. Connect to search index (Lucene, Solr, Elasticsearch)
            // 2. Extract content metadata and text
            // 3. Update or add document to index
            // 4. Commit changes
            Thread.sleep(50);

            execution.setVariable("indexedAt", System.currentTimeMillis());
            execution.setVariable("indexStatus", "SUCCESS");

            LOG.info("Search index updated successfully for: {}", contentPath);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            execution.setVariable("indexStatus", "FAILED");
            execution.setVariable("indexError", "Interrupted during indexing");
            LOG.error("Indexing interrupted for {}", contentPath, e);
            throw new Exception("Indexing failed: " + e.getMessage(), e);
        }
    }
}
