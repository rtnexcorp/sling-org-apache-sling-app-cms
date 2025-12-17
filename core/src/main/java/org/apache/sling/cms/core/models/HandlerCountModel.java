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
package org.apache.sling.cms.core.models;

import javax.annotation.PostConstruct;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Model for displaying handler count in transformation list
 */
@Model(adaptables = Resource.class)
public class HandlerCountModel {

    @SlingObject
    private Resource resource;

    private int handlerCount;
    private boolean hasHandlers;

    @PostConstruct
    protected void init() {
        Resource handlersNode = resource.getChild("handlers");
        if (handlersNode != null) {
            handlerCount = 0;
            for (Resource handler : handlersNode.getChildren()) {
                handlerCount++;
            }
            hasHandlers = handlerCount > 0;
        } else {
            hasHandlers = false;
            handlerCount = 0;
        }
    }

    public int getHandlerCount() {
        return handlerCount;
    }

    public boolean isHasHandlers() {
        return hasHandlers;
    }

    public String getHandlerText() {
        return handlerCount + " handler" + (handlerCount != 1 ? "s" : "");
    }
}
