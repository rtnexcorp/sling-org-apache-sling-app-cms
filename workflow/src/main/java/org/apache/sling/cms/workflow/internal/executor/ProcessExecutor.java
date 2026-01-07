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
package org.apache.sling.cms.workflow.internal.executor;

import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cms.workflow.WorkflowException;
import org.apache.sling.cms.workflow.delegate.JavaDelegate;
import org.apache.sling.cms.workflow.internal.expression.SimpleExpressionEvaluator;
import org.apache.sling.cms.workflow.internal.instance.ProcessInstanceImpl;
import org.apache.sling.cms.workflow.internal.instance.ProcessInstanceManager;
import org.apache.sling.cms.workflow.internal.model.ActivityImpl;
import org.apache.sling.cms.workflow.internal.model.ActivityType;
import org.apache.sling.cms.workflow.internal.model.ProcessDefinitionImpl;
import org.apache.sling.cms.workflow.internal.model.SequenceFlowImpl;
import org.apache.sling.cms.workflow.internal.task.TaskManager;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes BPMN process flows.
 */
@Component(service = ProcessExecutor.class)
public class ProcessExecutor {

    private static final Logger log = LoggerFactory.getLogger(ProcessExecutor.class);

    @Reference
    private ProcessInstanceManager processInstanceManager;

    @Reference
    private TaskManager taskManager;

    @Reference
    private JavaDelegateRegistry javaDelegateRegistry;

    @Reference
    private SimpleExpressionEvaluator expressionEvaluator;

    /**
     * Executes a process from the start event.
     *
     * @param processDefinition Process definition
     * @param processInstance Process instance
     * @param resolver Resource resolver
     * @throws WorkflowException if execution fails
     */
    public void executeProcess(
            @NotNull ProcessDefinitionImpl processDefinition,
            @NotNull ProcessInstanceImpl processInstance,
            @NotNull ResourceResolver resolver)
            throws WorkflowException {

        ActivityImpl startActivity = processDefinition.getStartActivity();
        if (startActivity == null) {
            throw new WorkflowException("No start activity found in process definition");
        }

        log.info("Starting process execution for instance: {}", processInstance.getId());
        executeActivity(processDefinition, processInstance, startActivity, resolver);
    }

    /**
     * Continues process execution from a specific activity.
     *
     * @param processDefinition Process definition
     * @param processInstance Process instance
     * @param activityId Activity ID to continue from
     * @param variables Variables to merge
     * @param resolver Resource resolver
     * @throws WorkflowException if execution fails
     */
    public void continueExecution(
            @NotNull ProcessDefinitionImpl processDefinition,
            @NotNull ProcessInstanceImpl processInstance,
            @NotNull String activityId,
            @NotNull Map<String, Object> variables,
            @NotNull ResourceResolver resolver)
            throws WorkflowException {

        // Merge variables
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            processInstance.setVariable(entry.getKey(), entry.getValue());
        }
        processInstanceManager.updateProcessInstance(processInstance, resolver);

        // Get activity
        ActivityImpl activity = processDefinition.getActivities().get(activityId);
        if (activity == null) {
            throw new WorkflowException("Activity not found: " + activityId);
        }

        // Continue from next activities
        for (SequenceFlowImpl flow : activity.getOutgoingFlows()) {
            String targetRef = flow.getTargetRef();
            ActivityImpl nextActivity = processDefinition.getActivities().get(targetRef);
            if (nextActivity != null) {
                executeActivity(processDefinition, processInstance, nextActivity, resolver);
            }
        }
    }

    private void executeActivity(
            @NotNull ProcessDefinitionImpl processDefinition,
            @NotNull ProcessInstanceImpl processInstance,
            @NotNull ActivityImpl activity,
            @NotNull ResourceResolver resolver)
            throws WorkflowException {

        log.debug("Executing activity: {} (type: {})", activity.getId(), activity.getType());

        processInstance.setCurrentActivityId(activity.getId());
        processInstanceManager.updateProcessInstance(processInstance, resolver);

        ActivityType type = activity.getType();

        switch (type) {
            case START_EVENT:
                handleStartEvent(processDefinition, processInstance, activity, resolver);
                break;

            case END_EVENT:
                handleEndEvent(processInstance, resolver);
                break;

            case USER_TASK:
                handleUserTask(processInstance, activity, resolver);
                break;

            case SERVICE_TASK:
                handleServiceTask(processDefinition, processInstance, activity, resolver);
                break;

            case EXCLUSIVE_GATEWAY:
                handleExclusiveGateway(processDefinition, processInstance, activity, resolver);
                break;

            case PARALLEL_GATEWAY:
                handleParallelGateway(processDefinition, processInstance, activity, resolver);
                break;

            default:
                log.warn("Unsupported activity type: {}", type);
                break;
        }
    }

    private void handleStartEvent(
            @NotNull ProcessDefinitionImpl processDefinition,
            @NotNull ProcessInstanceImpl processInstance,
            @NotNull ActivityImpl activity,
            @NotNull ResourceResolver resolver)
            throws WorkflowException {
        // Start event just moves to next activities
        for (SequenceFlowImpl flow : activity.getOutgoingFlows()) {
            String targetRef = flow.getTargetRef();
            ActivityImpl nextActivity = processDefinition.getActivities().get(targetRef);
            if (nextActivity != null) {
                executeActivity(processDefinition, processInstance, nextActivity, resolver);
            }
        }
    }

    private void handleEndEvent(@NotNull ProcessInstanceImpl processInstance, @NotNull ResourceResolver resolver)
            throws WorkflowException {
        // Mark process as ended
        processInstance.setEnded(true);
        processInstanceManager.updateProcessInstance(processInstance, resolver);
        log.info("Process instance {} ended", processInstance.getId());
    }

    private void handleUserTask(
            @NotNull ProcessInstanceImpl processInstance,
            @NotNull ActivityImpl activity,
            @NotNull ResourceResolver resolver)
            throws WorkflowException {
        // Create a task and wait for user completion
        String candidateGroup = activity.getCandidateGroup();
        taskManager.createTask(activity.getName(), processInstance.getId(), activity.getId(), candidateGroup, resolver);
        log.info("Created user task for activity: {}", activity.getId());
        // Execution stops here until task is completed
    }

    private void handleServiceTask(
            @NotNull ProcessDefinitionImpl processDefinition,
            @NotNull ProcessInstanceImpl processInstance,
            @NotNull ActivityImpl activity,
            @NotNull ResourceResolver resolver)
            throws WorkflowException {
        // Execute JavaDelegate
        String delegateClass = activity.getDelegateClass();
        if (delegateClass == null) {
            throw new WorkflowException("No delegate class specified for service task: " + activity.getId());
        }

        JavaDelegate delegate = javaDelegateRegistry.getDelegate(delegateClass);
        if (delegate == null) {
            throw new WorkflowException("JavaDelegate not found: " + delegateClass);
        }

        // Create execution context
        DelegateExecutionImpl execution = new DelegateExecutionImpl(processInstance, activity.getId());

        // Set ResourceResolver for delegates to access content
        execution.setResourceResolver(resolver);

        // Apply field injections
        Map<String, String> fields = activity.getFieldValues();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            execution.setFieldInjection(entry.getKey(), entry.getValue());
        }

        // Execute delegate
        try {
            delegate.execute(execution);
            log.info("Executed service task: {}", activity.getId());
        } catch (Exception e) {
            throw new WorkflowException("Failed to execute service task: " + activity.getId(), e);
        }

        processInstanceManager.updateProcessInstance(processInstance, resolver);

        // Continue to next activities
        for (SequenceFlowImpl flow : activity.getOutgoingFlows()) {
            String targetRef = flow.getTargetRef();
            ActivityImpl nextActivity = processDefinition.getActivities().get(targetRef);
            if (nextActivity != null) {
                executeActivity(processDefinition, processInstance, nextActivity, resolver);
            }
        }
    }

    private void handleExclusiveGateway(
            @NotNull ProcessDefinitionImpl processDefinition,
            @NotNull ProcessInstanceImpl processInstance,
            @NotNull ActivityImpl activity,
            @NotNull ResourceResolver resolver)
            throws WorkflowException {
        // Evaluate conditions and take first matching path
        for (SequenceFlowImpl flow : activity.getOutgoingFlows()) {
            String condition = flow.getConditionExpression();
            if (condition == null || expressionEvaluator.evaluate(condition, processInstance.getVariables())) {
                String targetRef = flow.getTargetRef();
                ActivityImpl nextActivity = processDefinition.getActivities().get(targetRef);
                if (nextActivity != null) {
                    executeActivity(processDefinition, processInstance, nextActivity, resolver);
                    return; // Take only first matching path
                }
            }
        }

        throw new WorkflowException("No outgoing flow matched for exclusive gateway: " + activity.getId());
    }

    private void handleParallelGateway(
            @NotNull ProcessDefinitionImpl processDefinition,
            @NotNull ProcessInstanceImpl processInstance,
            @NotNull ActivityImpl activity,
            @NotNull ResourceResolver resolver)
            throws WorkflowException {
        // Execute all outgoing flows in parallel (sequentially for now)
        for (SequenceFlowImpl flow : activity.getOutgoingFlows()) {
            String targetRef = flow.getTargetRef();
            ActivityImpl nextActivity = processDefinition.getActivities().get(targetRef);
            if (nextActivity != null) {
                executeActivity(processDefinition, processInstance, nextActivity, resolver);
            }
        }
    }
}
