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
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * A simple HelloWorld Sling Model.
 * <p>
 * This model generates a greeting message using a configurable name property.
 * The message format is: "Hello another text, {name}!"
 * </p>
 */
@Model(adaptables = Resource.class)
public class HelloWorld {

    @ValueMapValue
    @Optional
    @Default(values = "World")
    private String name;

    private String message;

    @PostConstruct
    protected void init() {
        message = "Hello another text, " + name + "!";
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the greeting message.
     * <p>
     * Returns a message in the format: "Hello another text, {name}!"
     * </p>
     *
     * @return the greeting message
     */
    public String getMessage() {
        return message;
    }
}
