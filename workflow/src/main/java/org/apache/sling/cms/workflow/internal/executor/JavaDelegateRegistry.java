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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.cms.workflow.delegate.JavaDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry for JavaDelegate implementations.
 * Tracks all OSGi JavaDelegate services dynamically.
 */
@Component(service = JavaDelegateRegistry.class)
public class JavaDelegateRegistry {

    private static final Logger log = LoggerFactory.getLogger(JavaDelegateRegistry.class);

    private final Map<String, JavaDelegate> delegates = new ConcurrentHashMap<>();

    /**
     * Binds a JavaDelegate service dynamically.
     *
     * @param delegate JavaDelegate implementation
     */
    @Reference(
            service = JavaDelegate.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    protected void bindJavaDelegate(JavaDelegate delegate) {
        String className = delegate.getClass().getName();
        delegates.put(className, delegate);
        log.info("Registered JavaDelegate: {}", className);
    }

    /**
     * Unbinds a JavaDelegate service dynamically.
     *
     * @param delegate JavaDelegate implementation
     */
    protected void unbindJavaDelegate(JavaDelegate delegate) {
        String className = delegate.getClass().getName();
        delegates.remove(className);
        log.info("Unregistered JavaDelegate: {}", className);
    }

    /**
     * Retrieves a JavaDelegate by class name.
     *
     * @param className Fully qualified class name
     * @return JavaDelegate or null if not found
     */
    @Nullable
    public JavaDelegate getDelegate(@NotNull String className) {
        return delegates.get(className);
    }
}
