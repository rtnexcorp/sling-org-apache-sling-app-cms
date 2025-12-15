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
 * JCR and Sling repository helper functions for Cypress tests
 */

/**
 * Verify that a node has the expected primary type
 * @param {string} path - JCR path to the node
 * @param {string} expectedType - Expected jcr:primaryType value
 * @param {object} auth - Optional authentication credentials
 * @returns {Cypress.Chainable}
 */
export function verifyNodeType(path, expectedType, auth = null) {
  const authConfig = auth || { username: "admin", password: "admin" };

  return cy
    .request({
      url: `${path}.0.json`,
      auth: authConfig,
    })
    .then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body["jcr:primaryType"]).to.eq(
        expectedType,
        `Node at ${path} should have primaryType ${expectedType}`
      );
      return response.body;
    });
}

/**
 * Verify that a node exists and has expected properties
 * @param {string} path - JCR path to the node
 * @param {object} expectedProps - Object with expected property key-value pairs
 * @param {object} auth - Optional authentication credentials
 * @returns {Cypress.Chainable}
 */
export function verifyNodeProperties(path, expectedProps, auth = null) {
  const authConfig = auth || { username: "admin", password: "admin" };

  return cy
    .request({
      url: `${path}.json`,
      auth: authConfig,
    })
    .then((response) => {
      expect(response.status).to.eq(200);

      Object.entries(expectedProps).forEach(([key, value]) => {
        expect(response.body).to.have.property(key, value);
      });

      return response.body;
    });
}

/**
 * Create a test page using the custom servlet
 * @param {object} options - Page creation options
 * @param {string} options.parentPath - Parent path where page will be created
 * @param {string} options.name - Page name
 * @param {string} options.title - Page title
 * @param {string} options.template - Template resource type (optional)
 * @param {object} options.additionalContent - Additional jcr:content properties (optional)
 * @param {object} options.auth - Optional authentication credentials
 * @returns {Cypress.Chainable}
 */
export function createTestPage(options) {
  const {
    parentPath,
    name,
    title,
    template = "reference/components/page/base",
    additionalContent = {},
    auth = null,
  } = options;

  const authConfig = auth || { username: "admin", password: "admin" };

  const content = {
    "jcr:primaryType": "sling:Page",
    "jcr:content": {
      "jcr:primaryType": "nt:unstructured",
      "jcr:title": title,
      "sling:resourceType": template,
      ...additionalContent,
    },
  };

  return cy
    .request({
      method: "POST",
      url: "/bin/cms/createpage",
      auth: authConfig,
      form: true,
      body: {
        parentPath: parentPath,
        ":name": name,
        ":content": JSON.stringify(content),
      },
    })
    .then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body.path).to.eq(`${parentPath}/${name}`);
      expect(response.body.primaryType).to.eq("sling:Page");
      return response.body;
    });
}

/**
 * Delete a node (page, folder, etc.) from the repository
 * @param {string} path - JCR path to delete
 * @param {object} auth - Optional authentication credentials
 * @returns {Cypress.Chainable}
 */
export function deleteNode(path, auth = null) {
  const authConfig = auth || { username: "admin", password: "admin" };

  return cy.request({
    method: "POST",
    url: path,
    auth: authConfig,
    form: true,
    body: {
      ":operation": "delete",
    },
    failOnStatusCode: false, // Don't fail if node doesn't exist
  });
}

/**
 * Create a folder in the repository
 * @param {string} path - Path where folder will be created
 * @param {string} name - Folder name
 * @param {object} auth - Optional authentication credentials
 * @returns {Cypress.Chainable}
 */
export function createFolder(path, name, auth = null) {
  const authConfig = auth || { username: "admin", password: "admin" };

  return cy.request({
    method: "POST",
    url: path,
    auth: authConfig,
    form: true,
    body: {
      ":operation": "import",
      ":contentType": "json",
      ":content": JSON.stringify({
        [name]: {
          "jcr:primaryType": "sling:Folder",
        },
      }),
    },
  });
}

/**
 * Query the repository using JCR SQL2
 * @param {string} query - SQL2 query string
 * @param {object} auth - Optional authentication credentials
 * @returns {Cypress.Chainable}
 */
export function queryRepository(query, auth = null) {
  const authConfig = auth || { username: "admin", password: "admin" };

  return cy.request({
    method: "GET",
    url: "/bin/querybuilder.json",
    auth: authConfig,
    qs: {
      query: query,
      type: "JCR-SQL2",
    },
  });
}

/**
 * Verify that a page node has the correct structure (sling:Page with jcr:content)
 * @param {string} pagePath - Path to the page node
 * @param {object} auth - Optional authentication credentials
 * @returns {Cypress.Chainable}
 */
export function verifyPageStructure(pagePath, auth = null) {
  const authConfig = auth || { username: "admin", password: "admin" };

  return cy
    .request({
      url: `${pagePath}.0.json`,
      auth: authConfig,
    })
    .then((response) => {
      expect(response.status).to.eq(200);

      // Verify it's a page
      expect(response.body["jcr:primaryType"]).to.eq(
        "sling:Page",
        `${pagePath} must be a sling:Page node`
      );

      // Verify jcr:content exists
      expect(response.body).to.have.property(
        "jcr:content",
        `${pagePath} must have jcr:content child node`
      );

      // Verify it's NOT a folder
      expect(response.body["jcr:primaryType"]).to.not.eq("sling:Folder");
      expect(response.body["jcr:primaryType"]).to.not.eq("nt:folder");

      return response.body;
    });
}

/**
 * Get node properties at a specific depth
 * @param {string} path - JCR path
 * @param {number} depth - Depth level (0-N)
 * @param {object} auth - Optional authentication credentials
 * @returns {Cypress.Chainable}
 */
export function getNodeWithDepth(path, depth = 1, auth = null) {
  const authConfig = auth || { username: "admin", password: "admin" };

  return cy.request({
    url: `${path}.${depth}.json`,
    auth: authConfig,
  });
}

/**
 * Set properties on a node
 * @param {string} path - JCR path
 * @param {object} properties - Properties to set
 * @param {object} auth - Optional authentication credentials
 * @returns {Cypress.Chainable}
 */
export function setNodeProperties(path, properties, auth = null) {
  const authConfig = auth || { username: "admin", password: "admin" };

  return cy.request({
    method: "POST",
    url: path,
    auth: authConfig,
    form: true,
    body: properties,
  });
}

/**
 * Copy a node to a new location
 * @param {string} srcPath - Source path
 * @param {string} destPath - Destination path
 * @param {object} auth - Optional authentication credentials
 * @returns {Cypress.Chainable}
 */
export function copyNode(srcPath, destPath, auth = null) {
  const authConfig = auth || { username: "admin", password: "admin" };

  return cy.request({
    method: "POST",
    url: srcPath,
    auth: authConfig,
    form: true,
    body: {
      ":operation": "copy",
      ":dest": destPath,
    },
  });
}

/**
 * Move a node to a new location
 * @param {string} srcPath - Source path
 * @param {string} destPath - Destination path
 * @param {object} auth - Optional authentication credentials
 * @returns {Cypress.Chainable}
 */
export function moveNode(srcPath, destPath, auth = null) {
  const authConfig = auth || { username: "admin", password: "admin" };

  return cy.request({
    method: "POST",
    url: srcPath,
    auth: authConfig,
    form: true,
    body: {
      ":operation": "move",
      ":dest": destPath,
    },
  });
}

/**
 * Create a test site structure
 * @param {string} sitePath - Base path for the site
 * @param {object} config - Site configuration
 * @param {object} auth - Optional authentication credentials
 * @returns {Cypress.Chainable}
 */
export function createTestSite(sitePath, config = {}, auth = null) {
  const authConfig = auth || { username: "admin", password: "admin" };

  const siteConfig = {
    "jcr:primaryType": "sling:Folder",
    "sling:configRef": config.configRef || "/conf/test",
    "jcr:title": config.title || "Test Site",
    ...config,
  };

  return cy.request({
    method: "POST",
    url: sitePath,
    auth: authConfig,
    form: true,
    body: {
      ":operation": "import",
      ":contentType": "json",
      ":content": JSON.stringify(siteConfig),
    },
  });
}

/**
 * Wait for a node to exist (with retry)
 * @param {string} path - JCR path to wait for
 * @param {number} timeout - Maximum wait time in ms
 * @param {object} auth - Optional authentication credentials
 * @returns {Cypress.Chainable}
 */
export function waitForNode(path, timeout = 5000, auth = null) {
  const authConfig = auth || { username: "admin", password: "admin" };

  return cy.request({
    url: `${path}.json`,
    auth: authConfig,
    retryOnStatusCodeFailure: true,
    timeout: timeout,
  });
}

/**
 * Verify node does not exist
 * @param {string} path - JCR path that should not exist
 * @param {object} auth - Optional authentication credentials
 * @returns {Cypress.Chainable}
 */
export function verifyNodeDoesNotExist(path, auth = null) {
  const authConfig = auth || { username: "admin", password: "admin" };

  return cy
    .request({
      url: `${path}.json`,
      auth: authConfig,
      failOnStatusCode: false,
    })
    .then((response) => {
      expect(response.status).to.eq(404, `Node at ${path} should not exist`);
    });
}
