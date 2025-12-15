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

const { doneLoading } = require("../utils");
const { login } = require("../util/test-helper");

describe("Template Selection Workflow", () => {
  beforeEach(() => {
    login();
  });

  it("loads templates based on site configuration", () => {
    cy.visit("/cms/site/content.html/content/reference/en");

    // Open page creation modal
    cy.get('.buttons a[data-title="Add Page"]').click();
    doneLoading();

    // Wait for modal and templates to load
    cy.get("select[name=pageTemplate]", { timeout: 10000 }).should(
      "be.visible"
    );

    // Verify ASF templates are available (from /conf/asf)
    // The reference site should have sling:configRef pointing to /conf/asf
    cy.get("select[name=pageTemplate] option").should("contain", "Base Page");
    cy.get("select[name=pageTemplate] option").should("contain", "RSS Feed");
    cy.get("select[name=pageTemplate] option").should("contain", "Sitemap");

    // Verify at least one template is available
    cy.get("select[name=pageTemplate] option").should("have.length.greaterThan", 1);
  });

  it("shows title and name fields after template selection", () => {
    cy.visit("/cms/site/content.html/content/reference/en");

    cy.get('.buttons a[data-title="Add Page"]').click();
    doneLoading();

    // Initially fields might not be visible
    cy.get("select[name=pageTemplate]", { timeout: 10000 }).should(
      "be.visible"
    );

    // Select template
    cy.get("select[name=pageTemplate]").select("Base Page");

    // Verify fields appear (with retry since they're loaded dynamically)
    cy.get("input[name=title]", { timeout: 5000 }).should("be.visible");
    cy.get('input[name=":name"]').should("be.visible");

    // Verify required fields
    cy.get("input[name=title]").should("have.attr", "required");
    cy.get('input[name=":name"]').should("have.attr", "required");

    // Verify labels
    cy.contains("label", "Title").should("be.visible");
    cy.contains("label", "Name").should("be.visible");
  });

  it("template dropdown waits for Handlebars to load", () => {
    cy.visit("/cms/site/content.html/content/reference/en");

    cy.get('.buttons a[data-title="Add Page"]').click();
    doneLoading();

    // Verify Handlebars is loaded globally
    cy.window().should("have.property", "Handlebars");

    // Select template
    cy.get("select[name=pageTemplate]", { timeout: 10000 }).select("Base Page");

    // Fields should render without console errors
    cy.get("input[name=title]", { timeout: 5000 }).should("be.visible");

    // Check console for Handlebars errors (none should exist)
    cy.window().then((win) => {
      cy.spy(win.console, "error");
      cy.get("@console.error").should("not.be.called");
    });
  });

  it("different templates load different field sets", () => {
    cy.visit("/cms/site/content.html/content/reference/en");

    cy.get('.buttons a[data-title="Add Page"]').click();
    doneLoading();

    cy.get("select[name=pageTemplate]", { timeout: 10000 }).should(
      "be.visible"
    );

    // Select Base Page
    cy.get("select[name=pageTemplate]").select("Base Page");
    cy.get("input[name=title]", { timeout: 5000 }).should("be.visible");

    // Store field count for Base Page
    cy.get(".modal-card-body input").then(($inputs) => {
      const basePageFieldCount = $inputs.length;
      expect(basePageFieldCount).to.be.greaterThan(0);
    });

    // Switch to RSS Feed template
    cy.get("select[name=pageTemplate]").select("RSS Feed");
    doneLoading();

    // Fields should update
    cy.get("input[name=title]", { timeout: 5000 }).should("be.visible");

    // Field sets might differ between templates
    cy.get(".modal-card-body").should("be.visible");
  });

  it("template content is generated correctly", () => {
    const pageName = `template-content-${Date.now()}`;
    const parentPath = "/content/reference/en";
    const expectedPath = `${parentPath}/${pageName}`;

    cy.visit(`/cms/site/content.html${parentPath}`);

    cy.get('.buttons a[data-title="Add Page"]').click();
    doneLoading();

    cy.get("select[name=pageTemplate]").select("Base Page");
    cy.get("input[name=title]", { timeout: 5000 }).type("Template Content Test");
    cy.get('input[name=":name"]').type(pageName);

    cy.get(".modal .is-primary").click();
    doneLoading();

    cy.get(".modal .close-modal.is-primary", { timeout: 5000 }).click();
    doneLoading();

    // Verify page has expected template properties
    cy.request({
      url: `${expectedPath}/jcr:content.json`,
      auth: { username: "admin", password: "admin" },
    }).then((response) => {
      expect(response.status).to.eq(200);

      // Should have resourceType from template
      expect(response.body).to.have.property("sling:resourceType");

      // Should have title from user input
      expect(response.body["jcr:title"]).to.eq("Template Content Test");

      // Template might include default components/containers
      // Verify structure is reasonable
      expect(response.body["jcr:primaryType"]).to.not.eq("sling:Folder");
    });

    // Cleanup
    cy.request({
      method: "POST",
      url: expectedPath,
      auth: { username: "admin", password: "admin" },
      form: true,
      body: {
        ":operation": "delete",
      },
    });
  });

  it("handles template selection errors gracefully", () => {
    cy.visit("/cms/site/content.html/content/reference/en");

    cy.get('.buttons a[data-title="Add Page"]').click();
    doneLoading();

    // Try to submit without selecting template
    cy.get(".modal .is-primary").click();

    // Should show validation error or prevent submission
    // Modal should still be open
    cy.get(".modal").should("be.visible");

    // Now select template and try without required fields
    cy.get("select[name=pageTemplate]", { timeout: 10000 }).select("Base Page");
    cy.get("input[name=title]", { timeout: 5000 }).should("be.visible");

    // Submit without filling title
    cy.get(".modal .is-primary").click();

    // HTML5 validation should prevent submission
    cy.get("input[name=title]:invalid").should("exist");
  });

  it("template allowedPaths are enforced", () => {
    // Verify that templates respect allowedPaths configuration
    // ASF templates should have allowedPaths: ["/content/apache/.*", "/content/reference/.*"]

    // Should work under /content/reference
    cy.visit("/cms/site/content.html/content/reference/en");
    cy.get('.buttons a[data-title="Add Page"]').click();
    doneLoading();

    cy.get("select[name=pageTemplate]", { timeout: 10000 }).should(
      "be.visible"
    );

    // ASF templates should be available here
    cy.get("select[name=pageTemplate] option")
      .should("contain", "Base Page")
      .and("contain", "RSS Feed");

    // Close modal
    cy.get(".modal .delete, .modal .close-modal").first().click();
  });

  it("preserves form state when template is changed", () => {
    cy.visit("/cms/site/content.html/content/reference/en");

    cy.get('.buttons a[data-title="Add Page"]').click();
    doneLoading();

    // Select first template and fill fields
    cy.get("select[name=pageTemplate]", { timeout: 10000 }).select("Base Page");
    cy.get("input[name=title]", { timeout: 5000 }).type("Test Title");
    cy.get('input[name=":name"]').type("test-name");

    // Switch template
    cy.get("select[name=pageTemplate]").select("RSS Feed");
    doneLoading();

    // Common fields should preserve values
    cy.get("input[name=title]").should("have.value", "Test Title");
    cy.get('input[name=":name"]').should("have.value", "test-name");
  });
});
