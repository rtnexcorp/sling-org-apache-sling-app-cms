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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.sling.cms.workflow.WorkflowException;
import org.apache.sling.cms.workflow.internal.model.ActivityImpl;
import org.apache.sling.cms.workflow.internal.model.ActivityType;
import org.apache.sling.cms.workflow.internal.model.ProcessDefinitionImpl;
import org.apache.sling.cms.workflow.internal.model.SequenceFlowImpl;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Parser for BPMN 2.0 XML files.
 *
 * <p>Parses standard BPMN 2.0 XML and builds internal process definition model.</p>
 */
@Component(service = BPMNParser.class)
public class BPMNParser {

    private static final Logger log = LoggerFactory.getLogger(BPMNParser.class);

    private static final String BPMN_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String FLOWABLE_NS = "http://flowable.org/bpmn";

    /**
     * Parse BPMN 2.0 XML into internal process definition.
     *
     * @param processKey Process key for ID generation
     * @param bpmnXml BPMN XML input stream
     * @return Parsed process definition
     * @throws WorkflowException if parsing fails
     */
    public ProcessDefinitionImpl parse(String processKey, InputStream bpmnXml) {
        try {
            // Read XML to string for storage
            String xmlContent = IOUtils.toString(bpmnXml, StandardCharsets.UTF_8);

            // Parse XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(IOUtils.toInputStream(xmlContent, StandardCharsets.UTF_8));

            Element definitions = doc.getDocumentElement();
            NodeList processes = definitions.getElementsByTagNameNS(BPMN_NS, "process");

            if (processes.getLength() == 0) {
                throw new WorkflowException("No process found in BPMN XML");
            }

            Element processElement = (Element) processes.item(0);
            String processId = processElement.getAttribute("id");
            String processName = processElement.getAttribute("name");

            log.debug("Parsing BPMN process: id={}, name={}", processId, processName);

            ProcessDefinitionImpl processDef = new ProcessDefinitionImpl();
            processDef.setKey(processId);
            processDef.setName(processName);
            processDef.setId(processKey + ":" + processId + ":1"); // key:id:version
            processDef.setVersion(1);
            processDef.setBpmnXml(xmlContent);

            // Parse documentation
            NodeList docElements = processElement.getElementsByTagNameNS(BPMN_NS, "documentation");
            if (docElements.getLength() > 0) {
                String description = docElements.item(0).getTextContent().trim();
                processDef.setDescription(description);
            }

            // Parse activities (tasks, gateways, events)
            Map<String, ActivityImpl> activities = parseActivities(processElement);
            processDef.setActivities(activities);

            // Parse sequence flows
            Map<String, SequenceFlowImpl> flows = parseSequenceFlows(processElement);
            processDef.setSequenceFlows(flows);

            // Build execution graph
            buildExecutionGraph(processDef, activities, flows);

            log.info(
                    "Successfully parsed BPMN process: key={}, activities={}, flows={}",
                    processId,
                    activities.size(),
                    flows.size());

            return processDef;

        } catch (Exception e) {
            throw new WorkflowException("Failed to parse BPMN XML: " + e.getMessage(), e);
        }
    }

    /**
     * Parse all activities (tasks, events, gateways) from process element.
     */
    private Map<String, ActivityImpl> parseActivities(Element processElement) {
        Map<String, ActivityImpl> activities = new HashMap<>();

        // Parse start events
        parseElements(processElement, "startEvent", activities, ActivityType.START_EVENT);

        // Parse end events
        parseElements(processElement, "endEvent", activities, ActivityType.END_EVENT);

        // Parse user tasks
        parseElements(processElement, "userTask", activities, ActivityType.USER_TASK);

        // Parse service tasks
        parseElements(processElement, "serviceTask", activities, ActivityType.SERVICE_TASK);

        // Parse exclusive gateways
        parseElements(processElement, "exclusiveGateway", activities, ActivityType.EXCLUSIVE_GATEWAY);

        // Parse parallel gateways (limited support)
        parseElements(processElement, "parallelGateway", activities, ActivityType.PARALLEL_GATEWAY);

        // Parse intermediate catch events
        parseElements(processElement, "intermediateCatchEvent", activities, ActivityType.INTERMEDIATE_CATCH_EVENT);

        return activities;
    }

    /**
     * Parse elements of a specific type from BPMN XML.
     */
    private void parseElements(
            Element processElement, String tagName, Map<String, ActivityImpl> activities, ActivityType type) {
        NodeList elements = processElement.getElementsByTagNameNS(BPMN_NS, tagName);

        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            String id = element.getAttribute("id");
            String name = element.getAttribute("name");

            ActivityImpl activity = new ActivityImpl();
            activity.setId(id);
            activity.setName(name);
            activity.setType(type);

            // Parse documentation
            NodeList docElements = element.getElementsByTagNameNS(BPMN_NS, "documentation");
            if (docElements.getLength() > 0) {
                String doc = docElements.item(0).getTextContent().trim();
                activity.setDocumentation(doc);
            }

            // Parse task-specific attributes
            if (type == ActivityType.USER_TASK) {
                parseUserTaskAttributes(element, activity);
            } else if (type == ActivityType.SERVICE_TASK) {
                parseServiceTaskAttributes(element, activity);
            }

            activities.put(id, activity);
            log.debug("Parsed activity: id={}, name={}, type={}", id, name, type);
        }
    }

    /**
     * Parse user task specific attributes (candidate groups, assignee).
     */
    private void parseUserTaskAttributes(Element element, ActivityImpl activity) {
        // Parse candidate groups from potentialOwner
        NodeList potentialOwners = element.getElementsByTagNameNS(BPMN_NS, "potentialOwner");
        if (potentialOwners.getLength() > 0) {
            Element potentialOwner = (Element) potentialOwners.item(0);
            NodeList expressions = potentialOwner.getElementsByTagNameNS(BPMN_NS, "resourceAssignmentExpression");
            if (expressions.getLength() > 0) {
                Element expression = (Element) expressions.item(0);
                NodeList formalExpressions = expression.getElementsByTagNameNS(BPMN_NS, "formalExpression");
                if (formalExpressions.getLength() > 0) {
                    String candidateGroup =
                            formalExpressions.item(0).getTextContent().trim();
                    activity.setCandidateGroup(candidateGroup);
                    log.debug("  User task candidate group: {}", candidateGroup);
                }
            }
        }
    }

    /**
     * Parse service task specific attributes (delegate class, field injection).
     */
    private void parseServiceTaskAttributes(Element element, ActivityImpl activity) {
        // Parse flowable:class attribute
        String delegateClass = element.getAttributeNS(FLOWABLE_NS, "class");
        if (delegateClass != null && !delegateClass.isEmpty()) {
            activity.setDelegateClass(delegateClass);
            log.debug("  Service task delegate class: {}", delegateClass);
        }

        // Parse extension elements for field injection
        NodeList extensionElements = element.getElementsByTagNameNS(BPMN_NS, "extensionElements");
        if (extensionElements.getLength() > 0) {
            Element extensions = (Element) extensionElements.item(0);
            parseFieldInjections(extensions, activity);
        }
    }

    /**
     * Parse field injection values from extension elements.
     */
    private void parseFieldInjections(Element extensions, ActivityImpl activity) {
        NodeList fields = extensions.getElementsByTagNameNS(FLOWABLE_NS, "field");
        Map<String, String> fieldValues = new HashMap<>();

        for (int i = 0; i < fields.getLength(); i++) {
            Element field = (Element) fields.item(i);
            String fieldName = field.getAttribute("name");

            // Parse string value
            NodeList stringValues = field.getElementsByTagNameNS(FLOWABLE_NS, "string");
            if (stringValues.getLength() > 0) {
                String value = stringValues.item(0).getTextContent().trim();
                fieldValues.put(fieldName, value);
                log.debug("  Field injection: {}={}", fieldName, value);
            }
        }

        activity.setFieldValues(fieldValues);
    }

    /**
     * Parse sequence flows (connecting arrows between activities).
     */
    private Map<String, SequenceFlowImpl> parseSequenceFlows(Element processElement) {
        Map<String, SequenceFlowImpl> flows = new HashMap<>();

        NodeList flowElements = processElement.getElementsByTagNameNS(BPMN_NS, "sequenceFlow");
        for (int i = 0; i < flowElements.getLength(); i++) {
            Element flowElement = (Element) flowElements.item(i);
            String id = flowElement.getAttribute("id");
            String sourceRef = flowElement.getAttribute("sourceRef");
            String targetRef = flowElement.getAttribute("targetRef");
            String name = flowElement.getAttribute("name");

            SequenceFlowImpl flow = new SequenceFlowImpl();
            flow.setId(id);
            flow.setName(name);
            flow.setSourceRef(sourceRef);
            flow.setTargetRef(targetRef);

            // Parse condition expression
            NodeList conditions = flowElement.getElementsByTagNameNS(BPMN_NS, "conditionExpression");
            if (conditions.getLength() > 0) {
                String condition = conditions.item(0).getTextContent().trim();
                flow.setConditionExpression(condition);
                log.debug(
                        "Parsed sequence flow: id={}, source={}, target={}, condition={}",
                        id,
                        sourceRef,
                        targetRef,
                        condition);
            } else {
                log.debug("Parsed sequence flow: id={}, source={}, target={}", id, sourceRef, targetRef);
            }

            flows.put(id, flow);
        }

        return flows;
    }

    /**
     * Build execution graph by linking activities with sequence flows.
     */
    private void buildExecutionGraph(
            ProcessDefinitionImpl processDef,
            Map<String, ActivityImpl> activities,
            Map<String, SequenceFlowImpl> flows) {
        // Build outgoing/incoming flows for each activity
        for (SequenceFlowImpl flow : flows.values()) {
            ActivityImpl source = activities.get(flow.getSourceRef());
            ActivityImpl target = activities.get(flow.getTargetRef());

            if (source == null) {
                log.warn("Sequence flow {} references unknown source activity: {}", flow.getId(), flow.getSourceRef());
                continue;
            }
            if (target == null) {
                log.warn("Sequence flow {} references unknown target activity: {}", flow.getId(), flow.getTargetRef());
                continue;
            }

            source.addOutgoingFlow(flow);
            target.addIncomingFlow(flow);
        }

        // Find start event
        for (ActivityImpl activity : activities.values()) {
            if (activity.getType() == ActivityType.START_EVENT) {
                processDef.setStartActivity(activity);
                log.debug("Set start activity: {}", activity.getId());
                break;
            }
        }

        if (processDef.getStartActivity() == null) {
            throw new WorkflowException("No start event found in process definition");
        }
    }
}
