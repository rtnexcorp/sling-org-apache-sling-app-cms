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

/**
 * Default ports for Sling CMS instances
 */
export const PORTS = {
  standalone: 8080,
  author: 8082,
  renderer: 8083
};

/**
 * Get URL for a specific instance type
 * @param {string} instanceType - 'standalone', 'author', or 'renderer'
 * @returns {string} the base URL for the instance
 */
export function getInstanceUrl(instanceType = 'standalone') {
  const port = PORTS[instanceType] || PORTS.standalone;
  return `http://localhost:${port}`;
}

/**
 * Get Author instance URL (port 8082)
 * @returns {string} Author URL
 */
export function getAuthorUrl() {
  return Cypress.env('AUTHOR_URL') || getInstanceUrl('author');
}

/**
 * Get Renderer instance URL (port 8083)
 * @returns {string} Renderer URL
 */
export function getRendererUrl() {
  return Cypress.env('RENDERER_URL') || getInstanceUrl('renderer');
}

/**
 * Sends a post to the specified URL
 * @param {string} url the URL to which to send the post
 * @param {Record<string, string>} body the body of the post to send
 */
export function sendPost(url, body) {
  cy.request({
    method: "POST",
    url,
    form: true,
    body,
    headers: {
      Referer: Cypress.config('baseUrl') || "http://localhost:8080",
    },
  });
}

/**
 * Login directly by posting to the login endpoint
 * @param {string} username - default 'admin'
 * @param {string} password - default 'admin'
 */
export function login(username = 'admin', password = 'admin') {
  sendPost("/j_security_check", {
    j_username: username,
    j_password: password,
  });
}

/**
 * Login to a specific instance (for author-renderer testing)
 * @param {string} instanceUrl - the instance base URL
 * @param {string} username - default 'admin'
 * @param {string} password - default 'admin'
 */
export function loginToInstance(instanceUrl, username = 'admin', password = 'admin') {
  cy.request({
    method: "POST",
    url: `${instanceUrl}/j_security_check`,
    form: true,
    body: {
      j_username: username,
      j_password: password,
    },
    headers: {
      Referer: instanceUrl,
    },
  });
}

/**
 * Check if a page exists on the renderer (published content)
 * @param {string} pagePath - the content path (e.g., '/content/site/page')
 * @param {boolean} shouldExist - whether the page should exist (default true)
 */
export function checkPageOnRenderer(pagePath, shouldExist = true) {
  const rendererUrl = getRendererUrl();
  cy.request({
    url: `${rendererUrl}${pagePath}.html`,
    failOnStatusCode: false
  }).then((response) => {
    if (shouldExist) {
      expect(response.status).to.eq(200);
    } else {
      expect(response.status).to.be.oneOf([404, 401, 403]);
    }
  });
}

/**
 * Verify CMS UI is not accessible on renderer
 */
export function verifyCmsNotOnRenderer() {
  const rendererUrl = getRendererUrl();
  cy.request({
    url: `${rendererUrl}/cms`,
    failOnStatusCode: false
  }).then((response) => {
    expect(response.status).to.be.oneOf([404, 401, 403]);
  });
}
