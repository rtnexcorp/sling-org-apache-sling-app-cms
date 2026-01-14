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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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

    private static final String CONTENT_APPROVAL_BPMN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "targetNamespace=\"http://sling.apache.org/workflow\">"
            + "<process id=\"contentApproval\" name=\"Content Approval Workflow\">"
            + "<startEvent id=\"startEvent\" name=\"Start\"/>"
            + "<userTask id=\"reviewTask\" name=\"Review Content\">"
            + "<potentialOwner>"
            + "<resourceAssignmentExpression>"
            + "<formalExpression>content-reviewers</formalExpression>"
            + "</resourceAssignmentExpression>"
            + "</potentialOwner>"
            + "</userTask>"
            + "<serviceTask id=\"publishTask\" name=\"Publish\" "
            + "xmlns:flowable=\"http://flowable.org/bpmn\" flowable:class=\"org.example.PublishDelegate\"/>"
            + "<endEvent id=\"endEvent\" name=\"End\"/>"
            + "<sequenceFlow id=\"flow1\" sourceRef=\"startEvent\" targetRef=\"reviewTask\"/>"
            + "<sequenceFlow id=\"flow2\" sourceRef=\"reviewTask\" targetRef=\"publishTask\"/>"
            + "<sequenceFlow id=\"flow3\" sourceRef=\"publishTask\" targetRef=\"endEvent\"/>"
            + "</process>"
            + "</definitions>";

    private BPMNParser parser;

    @BeforeEach
    void setUp() {
        parser = new BPMNParser();
    }

    @Test
    void testParseContentApproval() {
        InputStream bpmnXml = new ByteArrayInputStream(CONTENT_APPROVAL_BPMN.getBytes(StandardCharsets.UTF_8));

        // processKey is only used to build the internal ID; process key itself comes from XML process id
        ProcessDefinitionImpl processDef = parser.parse("test", bpmnXml);

        assertNotNull(processDef);
        assertEquals("contentApproval", processDef.getKey());
        assertEquals("Content Approval Workflow", processDef.getName());
        assertEquals(1, processDef.getVersion());

        // Current parser stores the raw BPMN XML string on the definition (as we persist it in JCR)
        assertNotNull(processDef.getBpmnXml());
        assertTrue(processDef.getBpmnXml().contains("<process"));
        assertTrue(processDef.getBpmnXml().contains("contentApproval"));

        // Verify activities
        assertTrue(processDef.getActivities().size() >= 4, "Should have at least 4 activities");
        assertTrue(processDef.getSequenceFlows().size() >= 3, "Should have at least 3 sequence flows");

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
        assertEquals("org.example.PublishDelegate", publishTask.getDelegateClass());
    }

    @Test
    void testParseActivitiesLinked() {
        InputStream bpmnXml = new ByteArrayInputStream(CONTENT_APPROVAL_BPMN.getBytes(StandardCharsets.UTF_8));
        ProcessDefinitionImpl processDef = parser.parse("test", bpmnXml);

        ActivityImpl startActivity = processDef.getStartActivity();
        assertNotNull(startActivity);

        // Verify start has outgoing flows
        assertTrue(startActivity.getOutgoingFlows().size() > 0, "Start event should have outgoing flows");

        // Verify flows have target references
        startActivity
                .getOutgoingFlows()
                .forEach(flow -> assertNotNull(flow.getTargetRef(), "Flow should have target reference"));
    }
}
