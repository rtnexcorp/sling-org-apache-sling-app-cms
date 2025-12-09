# Java 21 Upgrade Summary

## Overview
The Apache Sling CMS project has been upgraded from **Java 11 to Java 21 LTS**.

## Changes Made

### 1. Core Java Version
- Updated `sling.cms.java.version` from `11` to `21` in `pom.xml`
- Added `maven.compiler.release` property for better Java 21 support

### 2. Updated Dependencies for Java 21 Compatibility

| Dependency | Old Version | New Version | Reason |
|------------|-------------|-------------|--------|
| SLF4J | 1.7.36 | 2.0.16 | Required for Java 17+ support |
| Mockito | 4.11.0 | 5.14.2 | Java 21 compatibility and latest features |
| Groovy | 4.0.10 | 4.0.24 | Bug fixes and Java 21 support |
| Guava | 32.1.3-jre | 33.3.1-jre | Java 21 compatibility |
| ASM | 9.6 | 9.7.1 | Support for Java 21 bytecode |

### 3. Updated Files
- `/pom.xml` - Main project POM with Java 21 configuration
- `/archetype/src/main/resources/archetype-resources/pom.xml` - Updated from Java 8 to Java 21
- `/docs/author-renderer-quickstart.md` - Updated documentation
- `/vagrant/src/install-slingcms.sh` - Changed from `java-11-openjdk` to `java-21-openjdk`

## Build Requirements

### Prerequisites
- **Java 21 JDK** (OpenJDK 21 recommended)
- **Maven 3.6+**
- 4GB RAM minimum

### Setting Up Java 21

#### macOS (Homebrew)
```bash
# Install OpenJDK 21
brew install openjdk@21

# Set JAVA_HOME
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

# Verify
java --version
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install openjdk-21-jdk

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

# Verify
java --version
```

#### Linux (RHEL/CentOS/Fedora)
```bash
sudo yum install java-21-openjdk-devel

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH="$JAVA_HOME/bin:$PATH"

# Verify
java --version
```

#### Windows
1. Download OpenJDK 21 from https://adoptium.net/
2. Install and set JAVA_HOME environment variable
3. Add %JAVA_HOME%\bin to PATH

## Building the Project

### Full Build
```bash
mvn clean install
```

### Skip Tests (Faster)
```bash
mvn clean install -DskipTests
```

### Parallel Build (Faster)
```bash
mvn clean install -T 1C
```

## Known Considerations

### Dependencies That May Need Attention

1. **Apache Tika (1.28.5)**
   - Currently has a known CVE vulnerability
   - Recommended upgrade to 3.2.2+ (but noted OSGi compatibility concerns)
   - Monitor for updates

2. **Servlet APIs**
   - Still using some `javax.servlet` (v2.5) - consider migrating to `jakarta.servlet-api` for full Java 21 alignment
   - Project already has some Jakarta EE dependencies

### Testing Checklist

After upgrading, test the following:

- [ ] **Build Completes**: `mvn clean install`
- [ ] **Unit Tests Pass**: `mvn test`
- [ ] **Integration Tests**: `mvn clean install -Pci`
- [ ] **OSGi Bundle Resolution**: All modules load correctly
- [ ] **CMS Startup**: Application starts without errors
- [ ] **Content Operations**: Create, read, update, delete content
- [ ] **File Operations**: File uploads and transformations (Tika, PDFBox)
- [ ] **Email Functionality**: Jakarta Mail integration works
- [ ] **JSP/JSTL Rendering**: Pages render correctly

## Rollback Instructions

If you need to rollback to Java 11:

1. In `/pom.xml`, change line 45:
   ```xml
   <sling.cms.java.version>11</sling.cms.java.version>
   ```

2. In `/pom.xml`, change line 57:
   ```xml
   <slf4j.version>1.7.36</slf4j.version>
   ```

3. Revert other dependency versions if needed

4. Rebuild: `mvn clean install`

## Benefits of Java 21

### Performance Improvements
- Better garbage collection (ZGC, Generational ZGC)
- Improved startup time
- Virtual threads for better concurrency (Project Loom)

### New Features Available
- Pattern matching for switch statements
- Record patterns
- String templates (preview)
- Sequenced collections
- Virtual threads for improved concurrency

### Security
- Latest security updates and patches
- Extended support until September 2028

## Migration Timeline

| Phase | Status | Notes |
|-------|--------|-------|
| Core Version Update | ✅ Complete | Java 21 configured |
| Dependency Updates | ✅ Complete | Key dependencies updated |
| Build Verification | ⏳ Pending | Requires Java 21 JDK |
| Testing | ⏳ Pending | Full test suite |
| Documentation | ✅ Complete | Updated requirements |

## Support

- **Apache Sling Mailing List**: dev@sling.apache.org
- **JIRA**: https://issues.apache.org/jira/browse/SLING
- **Java 21 Documentation**: https://docs.oracle.com/en/java/javase/21/

## Next Steps

1. **Install Java 21** following the instructions above
2. **Verify Maven is using Java 21**: `mvn --version`
3. **Build the project**: `mvn clean install`
4. **Run tests**: `mvn clean install -Pci`
5. **Test the application** using the checklist above

---

**Upgrade Date**: December 9, 2025  
**Previous Version**: Java 11  
**Current Version**: Java 21 LTS  
**Support Until**: September 2028
