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
package org.apache.sling.thumbnails.internal.delivery;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.thumbnails.ThumbnailSupport;
import org.apache.sling.thumbnails.delivery.DeliveryFormatResolver;
import org.apache.sling.thumbnails.delivery.DeliveryPreset;
import org.apache.sling.thumbnails.delivery.DeliveryPresetManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to deliver optimized images using delivery presets.
 *
 * <p>URL format: /content/path/to/asset.jpg.deliver/preset-name.webp</p>
 *
 * <p>This servlet works by:</p>
 * <ol>
 *   <li>Looking up the delivery preset by name</li>
 *   <li>Resolving the best format based on Accept header (if format not specified)</li>
 *   <li>Forwarding to the transform servlet with the appropriate transformation</li>
 * </ol>
 *
 * <p>The delivery servlet delegates to the existing transform infrastructure,
 * providing a higher-level abstraction for front-end developers.</p>
 */
@Component(immediate = true)
@Designate(ocd = DeliveryServlet.Config.class)
public class DeliveryServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(DeliveryServlet.class);
    private static final long serialVersionUID = 1L;

    private static final String TRANSFORM_EXTENSION = "transform";

    @ObjectClassDefinition(
            name = "Apache Sling CMS - Delivery Servlet",
            description = "Servlet for delivering optimized images using delivery presets")
    public @interface Config {
        @AttributeDefinition(name = "Enabled", description = "Enable the delivery servlet")
        boolean enabled() default true;

        @AttributeDefinition(name = "Cache Control", description = "Cache-Control header value for delivered assets")
        String cacheControl() default "public, max-age=86400";

        @AttributeDefinition(name = "Error Resource Path", description = "Path to the error resource for fallback")
        String errorResourcePath() default "/static/sling-cms/thumbnails/file.png";
    }

    @Reference
    private DeliveryPresetManager presetManager;

    @Reference
    private DeliveryFormatResolver formatResolver;

    @Reference
    private ThumbnailSupport thumbnailSupport;

    private boolean enabled;
    private String cacheControl;
    private String errorResourcePath;

    private transient ServiceRegistration<Servlet> servletRegistration;

    @Activate
    protected void activate(Config config, BundleContext context) {
        this.enabled = config.enabled();
        this.cacheControl = config.cacheControl();
        this.errorResourcePath = config.errorResourcePath();

        if (enabled) {
            registerServlet(context);
        }
    }

    @Deactivate
    protected void deactivate() {
        if (servletRegistration != null) {
            servletRegistration.unregister();
            servletRegistration = null;
            log.info("Delivery servlet unregistered");
        }
    }

    private void registerServlet(BundleContext context) {
        log.info("Registering delivery servlet...");

        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("sling.servlet.methods", new String[] {"GET"});
        properties.put("sling.servlet.extensions", "deliver");
        properties.put("sling.servlet.resourceTypes", thumbnailSupport.getSupportedTypes());

        servletRegistration = context.registerService(Servlet.class, this, properties);
        log.info("Delivery servlet registered");
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        log.trace("doGet - delivery request");

        Resource asset = request.getResource();
        String suffix = request.getRequestPathInfo().getSuffix();

        if (StringUtils.isBlank(suffix)) {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Missing preset name in URL");
            return;
        }

        // Parse suffix: /preset-name.format
        String presetName = StringUtils.substringBeforeLast(suffix, ".");
        String requestedFormat = StringUtils.substringAfterLast(suffix, ".");

        // Remove leading slash from preset name
        if (presetName.startsWith("/")) {
            presetName = presetName.substring(1);
        }

        log.debug("Delivery request: asset={}, preset={}, format={}", asset.getPath(), presetName, requestedFormat);

        try {
            // Look up the delivery preset
            DeliveryPreset preset = presetManager.getPreset(asset, presetName);
            if (preset == null) {
                log.warn("Delivery preset not found: {}", presetName);
                response.sendError(SlingHttpServletResponse.SC_NOT_FOUND, "Delivery preset not found: " + presetName);
                return;
            }

            if (!preset.isEnabled()) {
                log.warn("Delivery preset is disabled: {}", presetName);
                response.sendError(SlingHttpServletResponse.SC_NOT_FOUND, "Delivery preset is disabled: " + presetName);
                return;
            }

            // Resolve the actual format to use
            String resolvedFormat;
            if (StringUtils.isNotBlank(requestedFormat)) {
                // Use explicitly requested format
                resolvedFormat = requestedFormat;
            } else {
                // Negotiate format based on Accept header
                resolvedFormat = formatResolver.resolveFormat(request, preset, asset);
            }

            // Get the transformation name to use
            String transformationName = preset.getTransformationName();
            if (StringUtils.isBlank(transformationName)) {
                // Use preset name as transformation name (convention)
                transformationName = presetName;
            }

            // Forward to transform servlet
            forwardToTransform(request, response, transformationName, resolvedFormat);

        } catch (Exception e) {
            log.error("Error processing delivery request", e);
            handleError(request, response, e);
        }
    }

    /**
     * Forward the request to the transform servlet.
     */
    private void forwardToTransform(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            String transformationName,
            String format)
            throws ServletException, IOException {
        // Set cache control headers
        if (StringUtils.isNotBlank(cacheControl)) {
            response.setHeader("Cache-Control", cacheControl);
        }

        // Build new suffix for transform servlet: /transformation-name.format
        String newSuffix = "/" + transformationName + "." + format;

        RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setReplaceExtension(TRANSFORM_EXTENSION);
        options.setReplaceSuffix(newSuffix);

        log.debug("Forwarding to transform: suffix={}", newSuffix);

        RequestDispatcher dispatcher = request.getRequestDispatcher(request.getResource(), options);

        if (dispatcher != null) {
            dispatcher.forward(request, response);
        } else {
            log.error("Could not get dispatcher for transform");
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not process delivery request");
        }
    }

    /**
     * Handle error by forwarding to error resource.
     */
    private void handleError(SlingHttpServletRequest request, SlingHttpServletResponse response, Exception e)
            throws IOException, ServletException {
        response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        // Check for recursion
        String resourcePath =
                request.getResource() != null ? request.getResource().getPath() : null;
        if (resourcePath != null
                && (resourcePath.equals(errorResourcePath) || resourcePath.startsWith(errorResourcePath + "/"))) {
            log.warn("Error occurred while processing error resource, returning empty response");
            return;
        }

        RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setReplaceSuffix("/delivery-error.jpg");
        options.setReplaceSelectors(TRANSFORM_EXTENSION);

        RequestDispatcher dispatcher = request.getRequestDispatcher(errorResourcePath, options);
        if (dispatcher != null) {
            dispatcher.forward(request, response);
        }
    }
}
