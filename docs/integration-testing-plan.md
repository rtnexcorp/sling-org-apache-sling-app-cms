# Apache Sling CMS - Integration Testing Plan

## Overview

This document outlines the integration testing strategy for Apache Sling CMS, covering end-to-end (E2E) tests using Cypress and server-side integration tests.

---

## Table of Contents

1. [Testing Stack](#testing-stack)
2. [Test Environment Setup](#test-environment-setup)
3. [E2E Test Categories](#e2e-test-categories)
4. [Test Scenarios](#test-scenarios)
5. [Test Data Management](#test-data-management)
6. [CI/CD Integration](#cicd-integration)
7. [Execution Guidelines](#execution-guidelines)

---

## Testing Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| E2E Framework | Cypress 12.x | Browser-based integration tests |
| Assertions | Chai (via Cypress) | Test assertions |
| Performance | Lighthouse CI | Performance audits |
| API Testing | Cypress + cy.request() | REST API validation |
| Visual Testing | Percy (optional) | Visual regression |

---

## Test Environment Setup

### Default Ports

| Instance | Default Port | URL | Purpose |
|----------|--------------|-----|---------|
| Standalone | 8080 | http://localhost:8080/cms | Single instance (dev/demo) |
| **Author** | **8082** | http://localhost:8082/cms | Content authoring |
| **Renderer** | **8083** | http://localhost:8083 | Content publishing/delivery |

### Prerequisites

```bash
# Start Sling CMS standalone instance
./deployment/start-standalone.sh  # Port 8080

# Or for author-renderer setup (recommended for integration testing)
./deployment/start-author.sh      # Author: Port 8082
./deployment/start-renderer.sh    # Renderer: Port 8083
```

### Cypress Configuration

Update `cypress.config.js` to target the correct instance:

```javascript
// For standalone testing
module.exports = {
  e2e: {
    baseUrl: 'http://localhost:8080'
  }
}

// For author instance testing
module.exports = {
  e2e: {
    baseUrl: 'http://localhost:8082'  // Author default port
  }
}

// For renderer instance testing
module.exports = {
  e2e: {
    baseUrl: 'http://localhost:8083'  // Renderer default port
  }
}
```

### Environment Variables

```bash
# Override base URL for different environments
CYPRESS_BASE_URL=http://localhost:8082 npm test  # Test against author
CYPRESS_BASE_URL=http://localhost:8083 npm test  # Test against renderer
```

### Running Integration Tests

```bash
cd it/

# Install dependencies
npm install

# Run all tests (headless)
npm test

# Run with Cypress UI
npm run cypress:open

# Run specific test file
npx cypress run --spec "cypress/e2e/admin.cy.js"
```

---

## E2E Test Categories

### 1. Authentication Tests (`auth.cy.js`)

| Test ID | Test Case | Priority |
|---------|-----------|----------|
| AUTH-001 | Login with valid credentials | P0 |
| AUTH-002 | Login with invalid credentials | P0 |
| AUTH-003 | Logout functionality | P0 |
| AUTH-004 | Session timeout handling | P1 |
| AUTH-005 | Remember me functionality | P2 |
| AUTH-006 | Password reset flow | P1 |

### 2. Content Management Tests (`authoring.cy.js`)

| Test ID | Test Case | Priority |
|---------|-----------|----------|
| CM-001 | Create new site | P0 |
| CM-002 | Create new page | P0 |
| CM-003 | Edit page properties | P0 |
| CM-004 | Delete page | P0 |
| CM-005 | Move/copy page | P1 |
| CM-006 | Create folder structure | P1 |
| CM-007 | Bulk operations | P2 |
| CM-008 | Version history | P1 |
| CM-009 | Restore previous version | P1 |

### 3. Page Editor Tests (`editor.cy.js`)

| Test ID | Test Case | Priority |
|---------|-----------|----------|
| ED-001 | Open page in editor | P0 |
| ED-002 | Add component to page | P0 |
| ED-003 | Edit component dialog | P0 |
| ED-004 | Delete component | P0 |
| ED-005 | Drag and drop components | P1 |
| ED-006 | Component copy/paste | P2 |
| ED-007 | Undo/redo actions | P2 |
| ED-008 | Rich text editor | P0 |
| ED-009 | Image component | P0 |
| ED-010 | Path field autocomplete | P1 |

### 4. Digital Asset Management Tests

| Test ID | Test Case | Priority |
|---------|-----------|----------|
| DAM-001 | Upload single file | P0 |
| DAM-002 | Upload multiple files | P0 |
| DAM-003 | Create asset folder | P0 |
| DAM-004 | Edit asset metadata | P1 |
| DAM-005 | Delete asset | P0 |
| DAM-006 | Image transformations | P1 |
| DAM-007 | Generate thumbnails | P1 |
| DAM-008 | Asset search | P1 |
| DAM-009 | Asset filtering | P2 |
| DAM-010 | Bulk asset operations | P2 |

### 5. Admin Tools Tests (`admin.cy.js`)

| Test ID | Test Case | Priority |
|---------|-----------|----------|
| ADM-001 | User management - create user | P0 |
| ADM-002 | User management - edit user | P1 |
| ADM-003 | User management - delete user | P1 |
| ADM-004 | Group management | P1 |
| ADM-005 | Job management | P2 |
| ADM-006 | System configuration | P2 |
| ADM-007 | Bulk publication job | P1 |
| ADM-008 | Content insights | P2 |

### 6. Taxonomy Tests (`taxonomy.cy.js`)

| Test ID | Test Case | Priority |
|---------|-----------|----------|
| TAX-001 | Create taxonomy item | P0 |
| TAX-002 | Edit taxonomy item | P1 |
| TAX-003 | Delete taxonomy item | P1 |
| TAX-004 | Hierarchical taxonomy | P1 |
| TAX-005 | Assign taxonomy to content | P0 |
| TAX-006 | Taxonomy search | P2 |

### 7. User Generated Content Tests (`ugc.cy.js`)

| Test ID | Test Case | Priority |
|---------|-----------|----------|
| UGC-001 | Form submission | P0 |
| UGC-002 | UGC moderation | P1 |
| UGC-003 | Approve UGC | P1 |
| UGC-004 | Reject/delete UGC | P1 |
| UGC-005 | Spam protection | P2 |

### 8. Publication Tests (NEW)

| Test ID | Test Case | Priority |
|---------|-----------|----------|
| PUB-001 | Publish single page | P0 |
| PUB-002 | Unpublish page | P0 |
| PUB-003 | Bulk publish | P1 |
| PUB-004 | Content distribution | P1 |
| PUB-005 | Publication status indicator | P1 |

### 9. Author-Renderer Integration Tests (NEW)

> **Note:** These tests require both Author (port 8082) and Renderer (port 8083) instances running.

| Test ID | Test Case | Port(s) | Priority |
|---------|-----------|---------|----------|
| AR-001 | Content created on Author appears on Renderer after publish | 8082 → 8083 | P0 |
| AR-002 | Unpublished content NOT visible on Renderer | 8082, 8083 | P0 |
| AR-003 | Asset replication to Renderer | 8082 → 8083 | P0 |
| AR-004 | Template changes propagate to Renderer | 8082 → 8083 | P1 |
| AR-005 | Configuration sync between instances | 8082 ↔ 8083 | P1 |
| AR-006 | Renderer-only access restrictions | 8083 | P1 |
| AR-007 | Author CMS UI not accessible on Renderer | 8083 | P1 |
| AR-008 | Content distribution queue status | 8082 | P1 |
| AR-009 | Failed distribution retry mechanism | 8082 | P2 |
| AR-010 | Bulk content distribution | 8082 → 8083 | P2 |

#### Author-Renderer Test Examples

```javascript
// cypress/e2e/author-renderer/content-sync.cy.js
describe('Author-Renderer Content Synchronization', () => {
  const AUTHOR_URL = 'http://localhost:8082';
  const RENDERER_URL = 'http://localhost:8083';
  
  it('AR-001: Published content appears on Renderer', () => {
    // Create and publish on Author (port 8082)
    cy.visit(AUTHOR_URL + '/cms');
    cy.login('admin', 'admin');
    cy.createPage('/content/test-site', 'test-page', 'Test Page');
    cy.publishPage('/content/test-site/test-page');
    
    // Wait for content distribution
    cy.wait(3000);
    
    // Verify on Renderer (port 8083)
    cy.request({
      url: RENDERER_URL + '/content/test-site/test-page.html',
      failOnStatusCode: false
    }).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body).to.contain('Test Page');
    });
  });
  
  it('AR-002: Unpublished content NOT visible on Renderer', () => {
    // Create but don't publish on Author
    cy.visit(AUTHOR_URL + '/cms');
    cy.login('admin', 'admin');
    cy.createPage('/content/test-site', 'draft-page', 'Draft Page');
    
    // Verify NOT accessible on Renderer (port 8083)
    cy.request({
      url: RENDERER_URL + '/content/test-site/draft-page.html',
      failOnStatusCode: false
    }).then((response) => {
      expect(response.status).to.eq(404);
    });
  });
  
  it('AR-007: CMS UI not accessible on Renderer', () => {
    // Verify /cms path returns 404 or redirect on Renderer
    cy.request({
      url: RENDERER_URL + '/cms',
      failOnStatusCode: false
    }).then((response) => {
      expect(response.status).to.be.oneOf([404, 401, 403]);
    });
  });
});
```

### 10. Search Tests

| Test ID | Test Case | Priority |
|---------|-----------|----------|
| SRC-001 | Global search | P0 |
| SRC-002 | Content search | P1 |
| SRC-003 | Asset search | P1 |
| SRC-004 | Search filters | P2 |
| SRC-005 | Search suggestions | P2 |

---

## Test Scenarios

### Scenario 1: Complete Content Authoring Flow

```javascript
// cypress/e2e/workflows/content-authoring.cy.js
describe('Content Authoring Workflow', () => {
  beforeEach(() => {
    cy.login('admin', 'admin');
  });

  it('should complete full authoring workflow', () => {
    // 1. Create a new site
    cy.createSite('test-site', 'Test Site');
    
    // 2. Create a page
    cy.createPage('/content/test-site', 'home', 'Home Page');
    
    // 3. Add components
    cy.openPageEditor('/content/test-site/home');
    cy.addComponent('text', { content: 'Hello World' });
    cy.addComponent('image', { path: '/content/assets/test.jpg' });
    
    // 4. Save and preview
    cy.savePageChanges();
    cy.previewPage();
    
    // 5. Publish
    cy.publishPage('/content/test-site/home');
    
    // 6. Verify on renderer
    cy.visit('http://localhost:8083/home.html');
    cy.contains('Hello World');
  });
});
```

### Scenario 2: Asset Management Flow

```javascript
// cypress/e2e/workflows/asset-management.cy.js
describe('Asset Management Workflow', () => {
  it('should upload and manage assets', () => {
    cy.login('admin', 'admin');
    
    // Upload asset
    cy.visit('/cms/static/content.html/content/static');
    cy.uploadFile('test-image.jpg');
    
    // Verify thumbnail generated
    cy.get('[data-asset="test-image.jpg"]')
      .find('.thumbnail')
      .should('be.visible');
    
    // Edit metadata
    cy.editAssetMetadata('test-image.jpg', {
      title: 'Test Image',
      description: 'A test image'
    });
    
    // Use in page
    cy.createPage('/content/test-site', 'asset-test', 'Asset Test');
    cy.openPageEditor('/content/test-site/asset-test');
    cy.addImageComponent('/content/static/test-image.jpg');
  });
});
```

---

## Test Data Management

### Test Fixtures

```
it/cypress/fixtures/
├── users/
│   ├── admin.json
│   ├── author.json
│   └── viewer.json
├── content/
│   ├── test-site.json
│   ├── test-page.json
│   └── test-components.json
├── assets/
│   ├── test-image.jpg
│   ├── test-document.pdf
│   └── test-video.mp4
└── taxonomy/
    └── test-taxonomy.json
```

### Data Setup/Teardown

```javascript
// cypress/support/commands.js

// Setup test data
Cypress.Commands.add('setupTestData', () => {
  cy.request({
    method: 'POST',
    url: '/content/test-site',
    form: true,
    body: {
      'jcr:primaryType': 'sling:Site',
      'jcr:title': 'Test Site'
    }
  });
});

// Cleanup test data
Cypress.Commands.add('cleanupTestData', () => {
  cy.request({
    method: 'POST',
    url: '/content/test-site',
    form: true,
    body: {
      ':operation': 'delete'
    }
  });
});
```

---

## CI/CD Integration

### GitHub Actions Workflow

```yaml
# .github/workflows/integration-tests.yml
name: Integration Tests

on:
  push:
    branches: [main, development]
  pull_request:
    branches: [main]

jobs:
  integration-tests:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          
      - name: Build project
        run: mvn clean install -DskipTests
        
      - name: Start Sling CMS
        run: |
          ./deployment/start-standalone.sh &
          sleep 60
          
      - name: Wait for CMS ready
        run: |
          timeout 120 bash -c 'until curl -s http://localhost:8080/system/health; do sleep 5; done'
          
      - name: Run Cypress tests
        uses: cypress-io/github-action@v6
        with:
          working-directory: it
          wait-on: 'http://localhost:8080/cms'
          
      - name: Upload screenshots
        uses: actions/upload-artifact@v4
        if: failure()
        with:
          name: cypress-screenshots
          path: it/cypress/screenshots
```

---

## Execution Guidelines

### Test Execution Order

1. **Smoke Tests** (P0) - Run on every commit
2. **Regression Tests** (P0 + P1) - Run on PR merge
3. **Full Suite** (All) - Run nightly

### Parallel Execution

```javascript
// cypress.config.js
module.exports = defineConfig({
  e2e: {
    baseUrl: 'http://localhost:8080',
    specPattern: 'cypress/e2e/**/*.cy.js',
    supportFile: 'cypress/support/e2e.js',
    
    // Parallel execution settings
    numTestsKeptInMemory: 10,
    experimentalMemoryManagement: true,
  },
});
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `CYPRESS_BASE_URL` | CMS base URL | `http://localhost:8080` |
| `CYPRESS_ADMIN_USER` | Admin username | `admin` |
| `CYPRESS_ADMIN_PASS` | Admin password | `admin` |
| `CYPRESS_RENDERER_URL` | Renderer URL | `http://localhost:8083` |

---

## Reporting

### Test Reports

- **Cypress Dashboard**: Real-time test results
- **JUnit XML**: For CI integration
- **HTML Report**: Human-readable reports

### Metrics

- Test coverage by feature area
- Pass/fail trends
- Flaky test identification
- Execution time tracking

---

## Future Enhancements

1. **Visual Regression Testing** - Percy or Applitools integration
2. **Performance Testing** - Lighthouse CI integration
3. **Accessibility Testing** - axe-core integration
4. **Mobile Testing** - Responsive design tests
5. **Load Testing** - k6 or Artillery integration
