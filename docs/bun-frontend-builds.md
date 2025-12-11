# Using Bun for Faster Frontend Builds

This document explains how to use [Bun](https://bun.sh/) as an alternative to Node.js/npm for building the frontend module in Apache Sling CMS.

## What is Bun?

Bun is a fast all-in-one JavaScript runtime and toolkit that includes:
- A JavaScript/TypeScript runtime (faster than Node.js)
- A package manager (faster than npm/yarn/pnpm)
- A bundler and transpiler

## Why Use Bun?

Bun provides significant speed improvements for frontend builds:

| Build Type | npm Time | Bun Time | Improvement |
|------------|----------|----------|-------------|
| Clean build (fresh install) | 10.7s | 9.4s | **~12% faster** |
| Incremental build (cached) | 8.0s | 5.3s | **~34% faster** |
| Full project build | 38.96s | 35.13s | **~10% faster** |

### Key Benefits

1. **Faster Package Installation**: Bun uses a global module cache with hard links
2. **Parallel Downloads**: Dependencies are downloaded in parallel
3. **Native Performance**: Written in Zig for maximum speed
4. **npm Compatible**: Uses the same `package.json` and works with npm packages

## Installation

### macOS / Linux

```bash
curl -fsSL https://bun.sh/install | bash
```

### macOS with Homebrew

```bash
brew install oven-sh/bun/bun
```

### Windows

```powershell
powershell -c "irm bun.sh/install.ps1 | iex"
```

### Verify Installation

```bash
bun --version
```

You should see a version number like `1.3.4` or higher.

## Using Bun with Maven

The frontend module includes Maven profiles for Bun-based builds.

### Production Build with Bun

```bash
# Build only frontend module
mvn install -pl frontend -P bun -DskipTests -Dbnd.baseline.skip=true

# Full project build with Bun
mvn clean install -P bun -DskipTests -Dbnd.baseline.skip=true
```

### Development Build with Bun

```bash
mvn install -pl frontend -P bun-dev -DskipTests -Dbnd.baseline.skip=true
```

### Deploy to Running Server with Bun

```bash
mvn install -pl frontend -P autoInstallBundle,bun -DskipTests -Dbnd.baseline.skip=true
```

## VS Code Tasks

A pre-configured VS Code task is available:

1. Open Command Palette (`Cmd+Shift+P` / `Ctrl+Shift+P`)
2. Select "Tasks: Run Task"
3. Choose "Auto Deploy - Frontend (Bun) ⚡"

This task:
- Builds the frontend module using Bun
- Deploys to the running Sling server
- Is significantly faster than the npm-based task

## Available Maven Profiles

| Profile | Description | Use Case |
|---------|-------------|----------|
| `bun` | Production build with Bun | Release builds, CI/CD |
| `bun-dev` | Development build with Bun | Local development |
| (default) | Production build with npm | When Bun is not available |
| `dev` | Development build with npm | When Bun is not available |

## Direct Bun Commands

You can also use Bun directly in the frontend directory:

```bash
cd frontend/src/main/frontend

# Install dependencies
bun install

# Development server with hot reload
bun run dev

# Production build
bun run build:prod

# Development build
bun run build
```

## Troubleshooting

### Bun Not Found

If Maven can't find Bun, ensure it's in your PATH:

```bash
# Add to ~/.zshrc or ~/.bashrc
export PATH="$HOME/.bun/bin:$PATH"
```

Then restart your terminal or run:

```bash
source ~/.zshrc  # or source ~/.bashrc
```

### Lock File Conflicts

If you switch between npm and Bun, you might see lock file issues:

```bash
cd frontend/src/main/frontend

# Clean up lock files
rm -rf node_modules package-lock.json bun.lock bun.lockb

# Reinstall with your preferred tool
bun install   # or npm ci
```

### Compatibility Issues

Bun is highly compatible with npm packages, but if you encounter issues:

1. Check [Bun's compatibility page](https://bun.sh/docs/runtime/nodejs-apis)
2. Fall back to npm: `mvn install -pl frontend -DskipTests`
3. Report issues at [Bun GitHub](https://github.com/oven-sh/bun/issues)

## Upgrading Bun

### Check Current Version

```bash
bun --version
```

### Upgrade to Latest

```bash
bun upgrade
```

### Upgrade to Specific Version

```bash
bun upgrade --version 1.3.4
```

### Check for Updates

```bash
bun upgrade --dry-run
```

## CI/CD Integration

### GitHub Actions

```yaml
- name: Setup Bun
  uses: oven-sh/setup-bun@v1
  with:
    bun-version: latest

- name: Build with Bun
  run: mvn clean install -P bun -DskipTests -Dbnd.baseline.skip=true
```

### Jenkins

```groovy
stage('Build') {
    steps {
        sh 'curl -fsSL https://bun.sh/install | bash'
        sh 'export PATH="$HOME/.bun/bin:$PATH" && mvn clean install -P bun -DskipTests'
    }
}
```

## Comparison: npm vs Bun

| Feature | npm | Bun |
|---------|-----|-----|
| Package installation | ~3-4s | ~1-2s |
| Script execution | ~1s startup | ~0.1s startup |
| Global cache | Per-project | Shared across projects |
| Lock file | `package-lock.json` | `bun.lockb` (binary) |
| Compatibility | 100% npm | 99%+ npm compatible |

## Best Practices

1. **Use Bun for development**: The speed improvement is most noticeable during active development
2. **Keep npm as fallback**: The default Maven build still uses npm for maximum compatibility
3. **Don't commit lock files**: Both `package-lock.json` and `bun.lockb` are gitignored
4. **Update regularly**: Bun is actively developed with frequent improvements

## Resources

- [Bun Documentation](https://bun.sh/docs)
- [Bun GitHub Repository](https://github.com/oven-sh/bun)
- [Bun Discord Community](https://bun.sh/discord)
- [Bun vs Node.js Benchmarks](https://bun.sh/docs/runtime/nodejs-apis)

---

*Last updated: December 11, 2025*
