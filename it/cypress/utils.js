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
module.exports.PORTS = {
  standalone: 8080,
  author: 8082,
  renderer: 8083
};

module.exports.login = function() {
  cy.visit("/system/sling/form/login");
  cy.get("input[name=j_username]").invoke("attr", "value", "admin");
  cy.get("input[name=j_password]").invoke("attr", "value", "admin");
  cy.get("form").submit();
  cy.url().should("contain", "/cms/start.html");
};

module.exports.doneLoading = function() {
  cy.get(".loader").should("not.exist");
};

/**
 * Get URL for a specific instance type
 * @param {string} instanceType - 'standalone', 'author', or 'renderer'
 * @returns {string} the base URL for the instance
 */
module.exports.getInstanceUrl = function(instanceType = 'standalone') {
  const port = module.exports.PORTS[instanceType] || module.exports.PORTS.standalone;
  return `http://localhost:${port}`;
};

/**
 * Get Author instance URL (port 8082)
 * @returns {string} Author URL
 */
module.exports.getAuthorUrl = function() {
  return Cypress.env('AUTHOR_URL') || module.exports.getInstanceUrl('author');
};

/**
 * Get Renderer instance URL (port 8083)
 * @returns {string} Renderer URL
 */
module.exports.getRendererUrl = function() {
  return Cypress.env('RENDERER_URL') || module.exports.getInstanceUrl('renderer');
};
