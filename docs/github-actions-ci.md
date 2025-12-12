# GitHub Actions CI/CD

This project uses GitHub Actions as an alternative CI/CD system alongside the existing Jenkins pipeline. GitHub Actions provides automated builds, testing, security scanning, and release automation directly within GitHub.

## Workflows Overview

| Workflow | File | Trigger | Purpose |
|----------|------|---------|---------|
| CI Build | `ci.yml` | Push, PR | Main build and test pipeline |
| CodeQL | `codeql.yml` | Push, PR, Weekly | Security vulnerability scanning |
| Release | `release.yml` | Tags, Manual | Release builds and Docker publishing |

## CI Build Workflow (`ci.yml`)

The main CI pipeline runs on every push and pull request.

### Jobs

#### 1. Build
- **Runs on**: Ubuntu Latest with Java 21
- **Steps**:
  1. Checkout code
  2. Set up JDK 21 (Temurin distribution)
  3. Cache Maven dependencies
  4. Build with Maven (`mvn clean install -DskipTests`)
  5. Run unit tests (`mvn test`)
  6. Upload build artifacts

#### 2. Quality
- **Depends on**: Build job
- **Checks**:
  - **RAT Check**: Apache license header verification
  - **Spotless**: Code formatting validation
  - **SpotBugs**: Static code analysis for bugs

#### 3. Integration Tests
- **Depends on**: Build job
- **Runs on**: PRs and main branch only
- **Steps**:
  1. Set up JDK 21 and Node.js 20
  2. Build the project
  3. Run Cypress integration tests
  4. Upload screenshots/videos on failure

#### 4. Docker
- **Depends on**: Build job
- **Purpose**: Verify Docker image builds correctly
- **Uses**: Docker Buildx with GitHub Actions cache

#### 5. Security
- **Depends on**: Build job
- **Runs**: OWASP Dependency Check for vulnerability scanning
- **Output**: HTML report uploaded as artifact

### Build Status Badge

Add this badge to display build status:

```markdown
[![GitHub Actions](https://github.com/apache/sling-org-apache-sling-app-cms/actions/workflows/ci.yml/badge.svg)](https://github.com/apache/sling-org-apache-sling-app-cms/actions/workflows/ci.yml)
```

## CodeQL Security Analysis (`codeql.yml`)

GitHub's semantic code analysis tool for finding security vulnerabilities.

### What It Scans

| Language | Build Method |
|----------|--------------|
| Java | Maven compile |
| JavaScript | Autobuild |

### Query Suites
- `security-extended`: Extended security queries
- `security-and-quality`: Combined security and code quality checks

### Schedule
- **On Push**: Every push to master/main
- **On PR**: Every pull request
- **Weekly**: Monday at 00:00 UTC (scheduled scan)

### Viewing Results
1. Go to **Security** tab in GitHub
2. Click **Code scanning alerts**
3. Filter by tool: "CodeQL"

## Release Workflow (`release.yml`)

Automates the release process when tags are pushed.

### Triggers
- Push of tags matching `org.apache.sling.cms-*`
- Manual trigger via workflow dispatch

### Jobs

#### 1. Release Build
- Builds all modules
- Runs verification
- Uploads release artifacts (JARs, feature files)

#### 2. Docker Release
- Builds multi-architecture Docker images (amd64, arm64)
- Pushes to GitHub Container Registry (GHCR)
- Tags: version-specific and `latest`

### Creating a Release

```bash
# Tag the release
git tag org.apache.sling.cms-1.1.8

# Push the tag
git push origin org.apache.sling.cms-1.1.8
```

The workflow will automatically:
1. Build release artifacts
2. Create release notes
3. Publish Docker image to GHCR

## Dependabot Configuration (`dependabot.yml`)

Automated dependency updates are configured for:

| Ecosystem | Directory | Schedule | Grouping |
|-----------|-----------|----------|----------|
| Maven | `/` | Weekly (Monday) | Sling, Testing, Jackson, Commons |
| GitHub Actions | `/` | Weekly (Monday) | - |
| npm | `/it` | Weekly (Monday) | Cypress |
| npm | `/i18n-helper` | Weekly (Monday) | - |
| Docker | `/docker/cms` | Weekly (Monday) | - |

### Dependency Groups
Updates are grouped to reduce PR noise:
- **sling**: All `org.apache.sling*` packages
- **testing**: JUnit, Mockito, Sling testing
- **jackson**: All Jackson packages
- **apache-commons**: Commons libraries

## Local Development

### Running Workflows Locally

You can test workflows locally using [act](https://github.com/nektos/act):

```bash
# Install act
brew install act

# Run CI workflow
act push

# Run specific job
act push -j build

# Run with secrets
act push --secret-file .secrets
```

### Environment Variables

| Variable | Purpose |
|----------|---------|
| `MAVEN_OPTS` | JVM options for Maven (`-Xmx2g`) |
| `CYPRESS_RECORD_KEY` | Cypress Dashboard recording key (secret) |

## Secrets Configuration

Configure these secrets in GitHub repository settings:

| Secret | Required | Purpose |
|--------|----------|---------|
| `CYPRESS_RECORD_KEY` | Optional | Cypress Dashboard recording |
| `GITHUB_TOKEN` | Automatic | Docker registry authentication |

## Troubleshooting

### Common Issues

#### 1. Build fails with "Out of memory"
Increase Maven memory in workflow:
```yaml
env:
  MAVEN_OPTS: -Xmx4g -XX:+UseParallelGC
```

#### 2. Cypress tests timeout
Check if the Sling instance started correctly. Review the integration test logs.

#### 3. Docker build fails
Ensure the Maven build completed successfully before Docker build.

#### 4. CodeQL takes too long
CodeQL analysis can take 10-30 minutes. This is normal for large Java projects.

### Viewing Logs
1. Go to **Actions** tab
2. Click on the workflow run
3. Expand job steps to see detailed logs

### Re-running Failed Jobs
1. Go to the failed workflow run
2. Click **Re-run failed jobs** or **Re-run all jobs**

## Best Practices

1. **Keep workflows fast**: Use caching effectively
2. **Fail fast**: Set `fail-fast: true` in matrix builds when appropriate
3. **Use concurrency**: Cancel in-progress runs for the same branch
4. **Secure secrets**: Never print secrets in logs
5. **Artifact retention**: Set appropriate retention periods to save storage

## Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [CodeQL Documentation](https://codeql.github.com/docs/)
- [Dependabot Configuration](https://docs.github.com/en/code-security/dependabot)
- [GitHub Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
