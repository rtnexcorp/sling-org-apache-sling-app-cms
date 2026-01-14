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
package org.apache.sling.cms.core.internal.index;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.cms.AuthorizableWrapper;
import org.apache.sling.cms.i18n.I18NDictionary;
import org.apache.sling.cms.i18n.I18NProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Admin-only servlet to trigger Oak index reindex by setting {@code reindex=true}.
 */
@Component(
        service = Servlet.class,
        property = {"sling.servlet.paths=/bin/cms/oak/reindex", "sling.servlet.methods=" + HttpConstants.METHOD_POST})
public class ReindexIndexServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final String PARAM_INDEX_PATH = "indexPath";

    @Reference
    private transient I18NProvider i18nProvider;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        I18NDictionary i18n = i18nProvider.getDictionary(request);

        AuthorizableWrapper user = request.getResourceResolver().adaptTo(AuthorizableWrapper.class);
        if (user == null || !user.isAdministrator()) {
            response.setStatus(SlingHttpServletResponse.SC_FORBIDDEN);
            response.getWriter()
                    .write("{\"title\":\"" + i18n.get("Forbidden") + "\",\"message\":\""
                            + i18n.get("Administrator access required") + "\"}");
            return;
        }

        String indexPath = request.getParameter(PARAM_INDEX_PATH);
        if (indexPath == null || indexPath.isBlank()) {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, i18n.get("Bad Request"));
            return;
        }

        if (!indexPath.startsWith("/oak:index/")) {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, i18n.get("Bad Request"));
            return;
        }

        Session session = request.getResourceResolver().adaptTo(Session.class);
        if (session == null) {
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, i18n.get("Failed"));
            return;
        }

        try {
            if (!session.nodeExists(indexPath)) {
                response.sendError(SlingHttpServletResponse.SC_NOT_FOUND, i18n.get("Not Found"));
                return;
            }

            // Ensure the (admin) session actually has permission to modify the index definition.
            // This avoids confusing 403s from downstream JCR permission checks.
            assertCanWriteIndex(session, indexPath);

            Node indexNode = session.getNode(indexPath);
            indexNode.setProperty("reindex", true);
            session.save();

            String title = i18n.get("Reindex triggered");
            response.getWriter().write("{\"title\":\"" + title + "\",\"path\":\"" + indexPath + "\"}");
        } catch (AccessDeniedException e) {
            response.setStatus(SlingHttpServletResponse.SC_FORBIDDEN);
            response.getWriter()
                    .write("{\"title\":\"" + i18n.get("Forbidden") + "\",\"message\":\""
                            + i18n.get("Not permitted to modify index definition") + "\",\"path\":\"" + indexPath
                            + "\"}");
        } catch (Exception e) {
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private static void assertCanWriteIndex(Session session, String indexPath) throws RepositoryException {
        AccessControlManager acm = session.getAccessControlManager();
        Privilege write = acm.privilegeFromName(Privilege.JCR_WRITE);
        Privilege modifyProps = acm.privilegeFromName(Privilege.JCR_MODIFY_PROPERTIES);
        if (!acm.hasPrivileges(indexPath, new Privilege[] {write, modifyProps})) {
            throw new AccessDeniedException("Missing JCR privileges for " + indexPath);
        }
    }
}
