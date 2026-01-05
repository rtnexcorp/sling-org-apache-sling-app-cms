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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.cms.workflow.delegate.DelegateExecution;
import org.apache.sling.cms.workflow.internal.instance.ProcessInstanceImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of DelegateExecution for JavaDelegate invocation.
 */
public class DelegateExecutionImpl implements DelegateExecution {

    private final ProcessInstanceImpl processInstance;
    private final String currentActivityId;
    private final Map<String, Object> fieldInjections;

    public DelegateExecutionImpl(@NotNull ProcessInstanceImpl processInstance, @NotNull String currentActivityId) {
        this.processInstance = processInstance;
        this.currentActivityId = currentActivityId;
        this.fieldInjections = new HashMap<>();
    }

    @Override
    @NotNull
    public String getProcessInstanceId() {
        return processInstance.getId();
    }

    @Override
    @NotNull
    public String getCurrentActivityId() {
        return currentActivityId;
    }

    @Override
    @Nullable
    public Object getVariable(@NotNull String name) {
        return processInstance.getVariable(name);
    }

    @Override
    public void setVariable(@NotNull String name, @Nullable Object value) {
        processInstance.setVariable(name, value);
    }

    @Override
    @NotNull
    public Map<String, Object> getVariables() {
        return processInstance.getVariables();
    }

    @Override
    @NotNull
    public String getProcessDefinitionKey() {
        return processInstance.getProcessDefinitionKey();
    }

    @Override
    @Nullable
    public String getBusinessKey() {
        return processInstance.getBusinessKey();
    }

    @Override
    @Nullable
    public <T> T getVariable(@NotNull String variableName, @NotNull Class<T> variableClass) {
        Object value = getVariable(variableName);
        if (value != null && variableClass.isInstance(value)) {
            return variableClass.cast(value);
        }
        return null;
    }

    @Override
    public void setVariables(@NotNull Map<String, Object> variables) {
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            setVariable(entry.getKey(), entry.getValue());
        }
    }

    @Override
    @Nullable
    public String getFieldValue(@NotNull String fieldName) {
        Object value = fieldInjections.get(fieldName);
        return value != null ? value.toString() : null;
    }

    @Override
    public boolean hasVariable(@NotNull String variableName) {
        return processInstance.getVariable(variableName) != null;
    }

    @Override
    public void removeVariable(@NotNull String variableName) {
        setVariable(variableName, null);
    }

    /**
     * Sets a field injection value for JavaDelegate.
     *
     * @param name Field name
     * @param value Field value
     */
    public void setFieldInjection(@NotNull String name, @Nullable Object value) {
        if (value != null) {
            fieldInjections.put(name, value);
        }
    }

    /**
     * Gets field injection value.
     *
     * @param name Field name
     * @return Field value
     */
    @Nullable
    public Object getFieldInjection(@NotNull String name) {
        return fieldInjections.get(name);
    }
}
