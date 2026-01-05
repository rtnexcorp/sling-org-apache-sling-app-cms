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
package org.apache.sling.cms.workflow.internal.parser;

import java.io.InputStream;

import org.apache.sling.cms.workflow.internal.model.ActivityImpl;
import org.apache.sling.cms.workflow.internal.model.ActivityType;
import org.apache.sling.cms.workflow.internal.model.ProcessDefinitionImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for BPMNParser.
 */
class BPMNParserTest {

    private BPMNParser parser;

    @BeforeEach
    void setUp() {
        parser = new BPMNParser();
    }

    @Test
    void testParseContentApproval() throws Exception {
        InputStream bpmnXml = getClass().getResourceAsStream("/workflows/content-approval.bpmn20.xml");
        assertNotNull(bpmnXml, "BPMN file not found");

        ProcessDefinitionImpl processDef = parser.parse("test", bpmnXml);

        assertNotNull(processDef);
        assertEquals("contentApproval", processDef.getKey());
        assertEquals("Content Approval Workflow", processDef.getName());
        assertEquals(1, processDef.getVersion());

        // Verify activities count
        assertTrue(processDef.getActivities().size() >= 7, "Should have at least 7 activities");

        // Verify sequence flows
        assertTrue(processDef.getSequenceFlows().size() >= 7, "Should have at least 7 sequence flows");

        // Verify start activity
        ActivityImpl startActivity = processDef.getStartActivity();
        assertNotNull(startActivity);
        assertEquals(ActivityType.START_EVENT, startActivity.getType());

        // Verify user task exists
        ActivityImpl reviewTask = processDef.getActivities().get("reviewTask");
        assertNotNull(reviewTask);
        assertEquals(ActivityType.USER_TASK, reviewTask.getType());
        assertEquals("content-reviewers", reviewTask.getCandidateGroup());

        // Verify service task exists
        ActivityImpl publishTask = processDef.getActivities().get("publishTask");
        assertNotNull(publishTask);
        assertEquals(ActivityType.SERVICE_TASK, publishTask.getType());
        assertNotNull(publishTask.getDelegateClass());
    }

    @Test
    void testParseActivitiesLinked() throws Exception {
        InputStream bpmnXml = getClass().getResourceAsStream("/workflows/content-approval.bpmn20.xml");
        ProcessDefinitionImpl processDef = parser.parse("test", bpmnXml);

        ActivityImpl startActivity = processDef.getStartActivity();
        assertNotNull(startActivity);

        // Verify start has outgoing flows
        assertTrue(startActivity.getOutgoingFlows().size() > 0, "Start event should have outgoing flows");

        // Verify flows have target references
        startActivity.getOutgoingFlows().forEach(flow -> {
            assertNotNull(flow.getTargetRef(), "Flow should have target reference");
        });
    }
}
