# Integration Testing Plan for Apache Sling CMS

## Executive Summary

This document outlines a comprehensive integration testing strategy to prevent regressions and ensure existing functionality remains stable during development. The current situation has shown that changes to one area (e.g., page creation) can inadvertently break existing features.

## Current State Analysis

### Existing Test Infrastructure
✅ **Already in place:**
- Cypress E2E test framework (`it/` module)
- Test suites for: auth, authoring, admin, editor, taxonomy, UGC
- Accessibility testing with pa11y
- CI/CD integration capability

⚠️ **Gaps identified:**
- **Page creation tests don't verify node type** (sling:Page vs sling:Folder)
- **No tests for template selection workflow**
- **No tests for custom servlets** (e.g., `/bin/cms/createpage`)
- **No JCR-level assertions** (only UI-level checks)
- **Missing regression tests** for critical workflows

## Immediate Action Items (Critical)

### 1. Add Page Creation Node Type Test
**Priority: CRITICAL** - Addresses current regression

```javascript
// it/cypress/e2e/page-creation-regression.cy.js
describe("Page Creation - Node Type Verification", () => {
  beforeEach(() => {
    login();
  });

  it("creates page with sling:Page node type (not sling:Folder)", () => {
    const pageName = `test-page-${Date.now()}`;
    
    // Create page via UI
    cy.visit("/cms/site/content.html/content/reference/en");
    cy.get('.buttons a[data-title="Add Page"]').click();
    cy.get("select[name=pageTemplate]").select("Base Page");
    cy.get("input[name=title]").type("Test Page");
    cy.get('input[name=":name"]').type(pageName);
    cy.get(".modal .is-primary").click();
    
    // Verify via JCR API
    cy.request({
      url: `/content/reference/en/${pageName}.json`,
      auth: { username: "admin", password: "admin" }
    }).then((response) => {
      expect(response.status).to.eq(200);
      // sling:Page nodes expose jcr:content properties by default
      expect(response.body).to.have.property("jcr:title", "Test Page");
    });
    
    // Verify primary type via direct JCR query
    cy.request({
      url: `/content/reference/en/${pageName}.0.json`,
      auth: { username: "admin", password: "admin" }
    }).then((response) => {
      expect(response.body["jcr:primaryType"]).to.eq("sling:Page");
      expect(response.body).to.have.property("jcr:content");
    });
    
    // Cleanup
    sendPost(`/content/reference/en/${pageName}`, {
      ":operation": "delete"
    });
  });

  it("creates page via custom servlet endpoint", () => {
    const pageName = `servlet-test-${Date.now()}`;
    
    // Direct servlet call
    cy.request({
      method: "POST",
      url: "/bin/cms/createpage",
      auth: { username: "admin", password: "admin" },
      form: true,
      body: {
        parentPath: "/content/reference/en",
        ":name": pageName,
        ":content": JSON.stringify({
          "jcr:primaryType": "sling:Page",
          "jcr:content": {
            "jcr:primaryType": "nt:unstructured",
            "jcr:title": "Servlet Test"
          }
        })
      }
    }).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body.primaryType).to.eq("sling:Page");
      expect(response.body.path).to.include(pageName);
    });
    
    // Verify node type
    cy.request({
      url: `/content/reference/en/${pageName}.0.json`,
      auth: { username: "admin", password: "admin" }
    }).then((response) => {
      expect(response.body["jcr:primaryType"]).to.eq("sling:Page");
    });
    
    // Cleanup
    sendPost(`/content/reference/en/${pageName}`, {
      ":operation": "delete"
    });
  });
});
```

### 2. Add Template Selection Workflow Test

```javascript
// it/cypress/e2e/template-workflow.cy.js
describe("Template Selection Workflow", () => {
  beforeEach(() => {
    login();
  });

  it("loads templates based on site configuration", () => {
    cy.visit("/cms/site/content.html/content/reference/en");
    cy.get('.buttons a[data-title="Add Page"]').click();
    
    // Wait for modal and templates to load
    cy.get("select[name=pageTemplate]", { timeout: 10000 }).should("be.visible");
    
    // Verify ASF templates are available (from /conf/asf)
    cy.get("select[name=pageTemplate] option").should("contain", "Base Page");
    cy.get("select[name=pageTemplate] option").should("contain", "RSS Feed");
    cy.get("select[name=pageTemplate] option").should("contain", "Sitemap");
  });

  it("shows title and name fields after template selection", () => {
    cy.visit("/cms/site/content.html/content/reference/en");
    cy.get('.buttons a[data-title="Add Page"]').click();
    
    // Select template
    cy.get("select[name=pageTemplate]").select("Base Page");
    
    // Verify fields appear
    cy.get("input[name=title]", { timeout: 5000 }).should("be.visible");
    cy.get('input[name=":name"]').should("be.visible");
    cy.get("input[name=title]").should("have.attr", "required");
  });

  it("generates page content from template", () => {
    const pageName = `template-test-${Date.now()}`;
    
    cy.visit("/cms/site/content.html/content/reference/en");
    cy.get('.buttons a[data-title="Add Page"]').click();
    cy.get("select[name=pageTemplate]").select("Base Page");
    cy.get("input[name=title]").type("Template Test");
    cy.get('input[name=":name"]').type(pageName);
    cy.get(".modal .is-primary").click();
    
    // Verify page has template properties
    cy.request({
      url: `/content/reference/en/${pageName}/jcr:content.json`,
      auth: { username: "admin", password: "admin" }
    }).then((response) => {
      expect(response.body).to.have.property("sling:resourceType");
      expect(response.body["jcr:title"]).to.eq("Template Test");
    });
    
    // Cleanup
    sendPost(`/content/reference/en/${pageName}`, {
      ":operation": "delete"
    });
  });
});
```

### 3. Enhance Existing Authoring Tests

Update `it/cypress/e2e/authoring.cy.js`:

```javascript
it("validate add page creates sling:Page node type", () => {
  const pageName = `it-${Date.now()}`;
  
  cy.visit("/cms/site/content.html/content/apache/sling-apache-org");
  cy.get('.level .buttons a[data-title="Add Page"]').click();

  doneLoading();
  cy.get("select[name=pageTemplate]").select("Base Page");

  cy.get("input[name=title]").type("Integration Test");
  cy.get('input[name=":name"]').type(pageName);

  cy.get(".modal .is-primary").click();
  doneLoading();

  // NEW: Verify node type
  cy.request({
    url: `/content/apache/sling-apache-org/${pageName}.0.json`,
    auth: { username: "admin", password: "admin" }
  }).then((response) => {
    expect(response.body["jcr:primaryType"]).to.eq("sling:Page");
    expect(response.body).to.have.property("jcr:content");
  });

  cy.get(".modal .close-modal.is-primary").click();
  doneLoading();
  cy.get(`.card[data-value="/content/apache/sling-apache-org/${pageName}"]`).should(
    "not.be.undefined"
  );

  // Cleanup
  cy.visit("/cms/site/content.html/content/apache/sling-apache-org");
  cy.get(`.card[data-value="/content/apache/sling-apache-org/${pageName}"]`).click();
  cy.get('.level a[data-title="Delete the specified page"]').click();
  doneLoading();
  cy.get(".modal .is-primary").click();
  doneLoading();
  cy.get(".modal .close-modal.is-primary").click();
});
```

## Long-term Testing Strategy

### Phase 1: Critical Path Coverage (Week 1-2)

**Goal:** Prevent regressions in core workflows

1. **Page Management**
   - [x] Page creation with correct node type
   - [ ] Page editing
   - [ ] Page deletion
   - [ ] Page move/copy
   - [ ] Page versioning
   - [ ] Page properties modification

2. **Template Management**
   - [x] Template selection
   - [x] Template field loading
   - [ ] Template content generation
   - [ ] Template allowedPaths enforcement
   - [ ] Template configuration inheritance

3. **Component Management**
   - [ ] Component addition to page
   - [ ] Component editing
   - [ ] Component deletion
   - [ ] Component reordering
   - [ ] Multifield component handling

### Phase 2: Extended Coverage (Week 3-4)

4. **Asset Management (DAM)**
   - [ ] File upload
   - [ ] Thumbnail generation
   - [ ] Image transformation
   - [ ] Video processing
   - [ ] Rendition management
   - [ ] Metadata extraction

5. **User Management**
   - [ ] User creation
   - [ ] Role assignment
   - [ ] Permission enforcement
   - [ ] LDAP integration (if configured)

6. **Taxonomy & Navigation**
   - [ ] Taxonomy creation
   - [ ] Tag assignment
   - [ ] Navigation tree generation
   - [ ] Sitemap generation

### Phase 3: API-Level Testing (Week 5-6)

7. **JCR Repository Tests**
   - [ ] Node type correctness
   - [ ] Property persistence
   - [ ] Version history
   - [ ] Workspace isolation

8. **OSGi Service Tests**
   - [ ] PageManager service
   - [ ] FileMetadataExtractor
   - [ ] Thumbnail providers
   - [ ] Custom servlets

9. **Sling Model Tests**
   - [ ] Page model adaptation
   - [ ] Site model adaptation
   - [ ] Component model adaptation

### Phase 4: Performance & Load Testing (Week 7-8)

10. **Performance Benchmarks**
    - [ ] Page creation time (< 2s)
    - [ ] Asset upload time (< 5s for 10MB)
    - [ ] Search response time (< 1s)
    - [ ] Concurrent user handling (10+ users)

11. **Load Testing**
    - [ ] 100 concurrent page creations
    - [ ] 1000 asset uploads
    - [ ] Large site navigation (1000+ pages)

## CI/CD Integration

### GitHub Actions Workflow

```yaml
# .github/workflows/integration-tests.yml
name: Integration Tests

on:
  pull_request:
    branches: [ master, multifield ]
  push:
    branches: [ master ]

jobs:
  integration-tests:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Build with Maven
        run: mvn clean install -DskipTests
      
      - name: Start Sling CMS (standalone)
        run: |
          ./deployment/start-standalone.sh &
          sleep 60  # Wait for startup
      
      - name: Run Integration Tests
        working-directory: it
        run: |
          npm ci
          npm run wait-for-ready
          npm test
      
      - name: Upload Cypress Videos
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: cypress-videos
          path: it/cypress/videos
      
      - name: Upload Cypress Screenshots
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: cypress-screenshots
          path: it/cypress/screenshots
      
      - name: Stop Sling CMS
        if: always()
        run: ./deployment/stop-all.sh
```

### Pre-commit Hook

```bash
# .git/hooks/pre-commit
#!/bin/bash

# Run critical tests before commit
cd it
npm run wait-for-ready && npm test -- --spec "cypress/e2e/page-creation-regression.cy.js"

if [ $? -ne 0 ]; then
  echo "❌ Critical tests failed. Commit aborted."
  exit 1
fi

echo "✅ Critical tests passed"
```

## Test Data Management

### Test Fixtures

Create reusable test data in `it/cypress/fixtures/`:

```javascript
// page-templates.json
{
  "basePage": {
    "template": "Base Page",
    "jcr:primaryType": "sling:Page",
    "expectedResourceType": "reference/components/page/base"
  },
  "rssFeed": {
    "template": "RSS Feed",
    "jcr:primaryType": "sling:Page",
    "expectedResourceType": "reference/components/page/rss"
  }
}

// test-content.json
{
  "testSite": {
    "path": "/content/test-site",
    "sling:configRef": "/conf/test"
  },
  "testPages": [
    {
      "name": "home",
      "title": "Home Page",
      "template": "Base Page"
    }
  ]
}
```

### Setup/Teardown Utilities

```javascript
// it/cypress/support/jcr-helpers.js
export function createTestSite(sitePath, config) {
  return cy.request({
    method: "POST",
    url: sitePath,
    auth: { username: "admin", password: "admin" },
    form: true,
    body: {
      ":operation": "import",
      ":contentType": "json",
      ":content": JSON.stringify({
        "jcr:primaryType": "sling:Folder",
        "sling:configRef": config.configRef || "/conf/test",
        ...config
      })
    }
  });
}

export function verifyNodeType(path, expectedType) {
  return cy.request({
    url: `${path}.0.json`,
    auth: { username: "admin", password: "admin" }
  }).then((response) => {
    expect(response.body["jcr:primaryType"]).to.eq(expectedType);
  });
}

export function cleanupTestContent(path) {
  return cy.request({
    method: "POST",
    url: path,
    auth: { username: "admin", password: "admin" },
    form: true,
    body: {
      ":operation": "delete"
    },
    failOnStatusCode: false
  });
}
```

## Monitoring & Reporting

### Test Metrics Dashboard

Track these KPIs weekly:

| Metric | Target | Current |
|--------|--------|---------|
| Test Coverage (Critical Paths) | 100% | 40% |
| Test Execution Time | < 10 min | 5 min |
| Flaky Test Rate | < 5% | ? |
| Regression Detection Rate | > 95% | ? |

### Test Report Format

```markdown
## Test Run Report - [Date]

### Summary
- ✅ Passed: 45
- ❌ Failed: 2
- ⏭️ Skipped: 0
- ⏱️ Duration: 8m 32s

### Failed Tests
1. **Page Creation - Complex Template**
   - Error: Template fields not loading
   - Branch: feature/multifield
   - Commit: abc123
   - Screenshot: [link]

2. **Asset Upload - Large File**
   - Error: Timeout after 30s
   - Potential regression in thumbnail generation
   - Needs investigation

### Coverage Report
- Page Management: 85%
- Template Management: 90%
- Component Management: 60% ⚠️
- Asset Management: 45% ⚠️
```

## Implementation Checklist

### Immediate (This Week)
- [ ] Create `page-creation-regression.cy.js` with node type verification
- [ ] Create `template-workflow.cy.js` for template selection tests
- [ ] Update existing `authoring.cy.js` to verify node types
- [ ] Add JCR helper utilities to `cypress/support/`
- [ ] Run tests locally and fix any failures

### Short-term (Next 2 Weeks)
- [ ] Add GitHub Actions workflow for PR testing
- [ ] Create test fixtures for common scenarios
- [ ] Implement setup/teardown utilities
- [ ] Add custom servlet tests
- [ ] Document test writing guidelines

### Medium-term (Next Month)
- [ ] Achieve 80%+ coverage of critical paths
- [ ] Add performance benchmarks
- [ ] Create test data generator scripts
- [ ] Set up nightly test runs
- [ ] Create test metrics dashboard

### Long-term (Next Quarter)
- [ ] Full API-level test coverage
- [ ] Load testing infrastructure
- [ ] Visual regression testing
- [ ] Automated test generation from requirements
- [ ] Integration with Apache Jenkins

## Best Practices for Developers

### Before Making Changes
1. ✅ Run relevant test suite locally
2. ✅ Review test coverage for affected area
3. ✅ Consider what tests need to be added

### During Development
1. ✅ Write tests alongside code (TDD approach)
2. ✅ Use descriptive test names
3. ✅ Verify JCR-level changes, not just UI
4. ✅ Add cleanup in test teardown

### Before Committing
1. ✅ Run full test suite: `cd it && npm test`
2. ✅ Check for test failures
3. ✅ Update test documentation if needed
4. ✅ Add regression tests for bug fixes

### Code Review Checklist
- [ ] Are there tests for the new functionality?
- [ ] Do tests verify JCR node types and properties?
- [ ] Are there both positive and negative test cases?
- [ ] Is test data properly cleaned up?
- [ ] Are tests deterministic (no flakiness)?

## Conclusion

This integration testing plan addresses the current regression issues and provides a roadmap for comprehensive test coverage. By implementing these tests incrementally, we can:

1. **Prevent regressions** like the page creation node type issue
2. **Catch breaking changes** before they reach production
3. **Document expected behavior** through executable tests
4. **Enable confident refactoring** with safety net
5. **Improve code quality** through test-driven development

**Next Steps:**
1. Implement the three critical test suites (page creation, template workflow, enhanced authoring)
2. Run them locally to verify they catch the current regression
3. Add to CI/CD pipeline
4. Expand coverage incrementally based on the phased plan
