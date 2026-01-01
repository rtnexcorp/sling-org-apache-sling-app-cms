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
package org.apache.sling.thumbnails.internal;

import java.io.InputStream;
import java.util.function.Function;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;

public class ContextHelper {

    public static final void initContext(SlingContext context) {

        context.load().json("/content.json", "/content");
        context.load().json("/conf.json", "/conf");
        context.load().binaryResource("/apache.png", "/content/apache/sling-apache-org/index/apache.png/jcr:content");
        context.load().binaryResource("/sling.pdf", "/content/apache/sling-apache-org/index/sling.pdf/jcr:content");
        context.load().binaryResource("/Sling.docx", "/content/apache/sling-apache-org/index/Sling.docx/jcr:content");
        context.load().binaryResource("/Sling.pptx", "/content/apache/sling-apache-org/index/Sling.pptx/jcr:content");
        context.load().binaryResource("/Sling.ppt", "/content/apache/sling-apache-org/index/Sling.ppt/jcr:content");
        context.load().binaryResource("/Sling.xlsx", "/content/apache/sling-apache-org/index/Sling.xlsx/jcr:content");
        context.load()
                .binaryResource("/editor.min.css", "/content/apache/sling-apache-org/index/editor.min.css/jcr:content");

        // Set MIME types for test resources
        setMimeType(context, "/content/apache/sling-apache-org/index/apache.png/jcr:content", "image/png");
        setMimeType(context, "/content/apache/sling-apache-org/index/sling.pdf/jcr:content", "application/pdf");
        setMimeType(
                context,
                "/content/apache/sling-apache-org/index/Sling.docx/jcr:content",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        setMimeType(
                context,
                "/content/apache/sling-apache-org/index/Sling.pptx/jcr:content",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        setMimeType(
                context,
                "/content/apache/sling-apache-org/index/Sling.ppt/jcr:content",
                "application/vnd.ms-powerpoint");
        setMimeType(
                context,
                "/content/apache/sling-apache-org/index/Sling.xlsx/jcr:content",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        setMimeType(context, "/content/apache/sling-apache-org/index/editor.min.css/jcr:content", "text/css");

        context.registerAdapter(Resource.class, InputStream.class, new Function<Resource, InputStream>() {
            public InputStream apply(Resource input) {
                return input.getValueMap().get("jcr:content/jcr:data", InputStream.class);
            }
        });
    }

    private static void setMimeType(SlingContext context, String path, String mimeType) {
        Resource resource = context.resourceResolver().getResource(path);
        if (resource != null) {
            ModifiableValueMap props = resource.adaptTo(ModifiableValueMap.class);
            if (props != null) {
                props.put("jcr:mimeType", mimeType);
                try {
                    context.resourceResolver().commit();
                } catch (PersistenceException e) {
                    // Ignore in test context
                }
            }
        }
    }
}
