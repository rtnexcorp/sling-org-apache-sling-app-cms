# Integration Testing - Quick Start Guide

## Running the Tests

### Prerequisites
1. **Sling CMS must be running** on port 8080 (standalone) or 8082 (author)
2. **Node.js and npm** installed
3. **Default credentials**: admin/admin

### Start Sling CMS
```bash
# From project root
./deployment/start-standalone.sh

# Or for author instance (port 8082)
./deployment/start-author.sh

# Wait for startup (check logs)
tail -f deployment/standalone/logs/error.log
```

### Run All Tests
```bash
cd it
npm ci                    # Install dependencies
npm run wait-for-ready    # Wait for Sling to be ready
npm test                  # Run all Cypress tests
```

### Run Specific Test Suites
```bash
# Run only page creation regression tests
npm test -- --spec "cypress/e2e/page-creation-regression.cy.js"

# Run only template workflow tests
npm test -- --spec "cypress/e2e/template-workflow.cy.js"

# Run both critical suites
npm test -- --spec "cypress/e2e/{page-creation-regression,template-workflow}.cy.js"
```

### Interactive Test Development
```bash
cd it
npm run cypress           # Opens Cypress UI
```

Then select tests to run and watch them execute in the browser.

## Test Structure

### Critical Regression Tests
Located in `it/cypress/e2e/`:

1. **page-creation-regression.cy.js** - Verifies pages are created with `sling:Page` node type
2. **template-workflow.cy.js** - Verifies template selection and field loading
3. **authoring.cy.js** - General authoring workflows (updated to verify node types)

### Helper Utilities
Located in `it/cypress/support/`:

- **jcr-helpers.js** - JCR/Sling repository operations
  - `verifyNodeType(path, expectedType)`
  - `createTestPage(options)`
  - `deleteNode(path)`
  - `verifyPageStructure(path)`
  - etc.

### Test Configuration
- **cypress.config.js** - Cypress configuration
  - baseUrl: http://localhost:8080 (update if using port 8082)
  - retries: 3 in CI mode
  - viewportWidth: 1000px

## Writing New Tests

### Example: Test Page Creation

```javascript
import { verifyNodeType, deleteNode } from '../support/jcr-helpers';

describe("My Feature", () => {
  beforeEach(() => {
    login();
  });

  it("creates a page correctly", () => {
    const pageName = `test-${Date.now()}`;
    const parentPath = "/content/reference/en";
    
    // ... create page via UI ...
    
    // Verify node type
    verifyNodeType(`${parentPath}/${pageName}`, "sling:Page");
    
    // Cleanup
    deleteNode(`${parentPath}/${pageName}`);
  });
});
```

### Best Practices

1. **Always verify JCR-level changes**, not just UI state
2. **Use unique names** with timestamps: `test-${Date.now()}`
3. **Clean up test data** in `afterEach` or test end
4. **Use helper functions** from `jcr-helpers.js`
5. **Add meaningful assertions** with custom error messages

## Verifying Current Regression

To verify the page creation node type fix is working:

```bash
# 1. Start Sling CMS
./deployment/start-standalone.sh

# 2. Deploy your changes
mvn clean install -P autoInstallBundle -pl core,ui -DskipTests

# 3. Run regression tests
cd it
npm test -- --spec "cypress/e2e/page-creation-regression.cy.js"

# Expected result: All tests should PASS
# If tests FAIL with "expected sling:Folder to equal sling:Page", the regression still exists
```

## Continuous Integration

### GitHub Actions
The workflow in `.github/workflows/integration-tests.yml` will:
1. Build the project
2. Start Sling CMS
3. Run all Cypress tests
4. Upload screenshots/videos on failure

### Pre-commit Hook
Install the pre-commit hook to run critical tests before committing:

```bash
# Make hook executable
chmod +x .git/hooks/pre-commit

# Or create it:
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
cd it
npm run wait-for-ready && npm test -- --spec "cypress/e2e/page-creation-regression.cy.js"
if [ $? -ne 0 ]; then
  echo "❌ Critical tests failed. Commit aborted."
  exit 1
fi
echo "✅ Critical tests passed"
EOF

chmod +x .git/hooks/pre-commit
```

## Troubleshooting

### Tests Timeout
- **Issue**: Tests timeout waiting for Sling
- **Solution**: Increase wait time in `cypress/ready.js` or start Sling manually first

### Port Conflicts
- **Issue**: Tests connect to wrong port
- **Solution**: Update `baseUrl` in `cypress.config.js`:
  ```javascript
  baseUrl: "http://localhost:8082"  // For author instance
  ```

### Authentication Failures
- **Issue**: 401 errors in test requests
- **Solution**: Verify admin/admin credentials are correct and user exists

### Node Type Assertions Fail
- **Issue**: Tests report `sling:Folder` instead of `sling:Page`
- **Solution**: This indicates the regression still exists - verify:
  1. Core module is deployed with PageCreateServlet
  2. UI module is deployed with updated pageproperties.jsp
  3. Frontend bundle has Handlebars import
  4. Browser cache is cleared

### Cypress Not Found
- **Issue**: `cypress: command not found`
- **Solution**: 
  ```bash
  cd it
  npm ci
  npm run cypress:install
  ```

## Test Coverage Goals

Current state:
- ✅ Page creation node type verification
- ✅ Template selection workflow
- ✅ Basic authoring operations

Next priorities (from integration-testing-plan.md):
- [ ] Component management
- [ ] Asset upload and processing
- [ ] Version management
- [ ] Permission enforcement
- [ ] Performance benchmarks

See `docs/integration-testing-plan.md` for the complete testing roadmap.

## Resources

- [Cypress Documentation](https://docs.cypress.io/)
- [Apache Sling Documentation](https://sling.apache.org/)
- [JCR Specification](https://developer.adobe.com/experience-manager/reference-materials/spec/jcr/2.0/index.html)
- [Integration Testing Plan](../docs/integration-testing-plan.md)
