# Apache Sling CMS - No .content.xml Policy

**Policy Status**: ✅ Active  
**Effective Date**: January 10, 2026  
**Applies To**: All new component development

---

## Policy Statement

**Apache Sling CMS does NOT use `.content.xml` files for component definitions.**

Component metadata and configuration is handled through other mechanisms in the Sling CMS architecture. This policy helps maintain consistency and simplifies the development process.

---

## What This Means

### ❌ Do NOT Create

```
ui/src/main/resources/jcr_root/libs/sling-cms/components/cms/mycomponent/
├── .content.xml          # ❌ DO NOT CREATE THIS
└── mycomponent.html      # ✅ CREATE THIS
```

### ✅ Correct Component Structure

```
ui/src/main/resources/jcr_root/libs/sling-cms/components/cms/mycomponent/
└── mycomponent.html      # HTL template only
```

---

## Why This Policy Exists

1. **Simplified Development**: Fewer files to manage per component
2. **Convention Over Configuration**: Components work by convention
3. **Easier Maintenance**: Less boilerplate, more focus on functionality
4. **Architectural Decision**: Sling CMS handles component registration differently than AEM

---

## Component Registration

Components in Apache Sling CMS are registered through:

1. **File System Structure**: Component directory location under `/libs/sling-cms/components/`
2. **HTL Templates**: The presence of `.html` files
3. **Sling Models**: Business logic in Java classes with `@Model` annotation
4. **Resource Types**: Inferred from directory path

Example:
- Component path: `/libs/sling-cms/components/cms/contentfilter/`
- Resource type: `sling-cms/components/cms/contentfilter`
- Template: `contentfilter.html`
- Sling Model: `org.apache.sling.cms.core.models.ContentFilter`

---

## When Creating New Components

### Step-by-Step Checklist

- [ ] Create component directory: `ui/src/main/resources/jcr_root/libs/sling-cms/components/cms/<componentname>/`
- [ ] Create HTL template: `<componentname>.html`
- [ ] Create Sling Model (if needed): `core/src/main/java/org/apache/sling/cms/core/models/<ComponentName>.java`
- [ ] Create SCSS file: `frontend/src/main/frontend/scss/_<componentname>.scss`
- [ ] Import SCSS in `cms.scss`
- [ ] **DO NOT** create `.content.xml` file

### Example: Creating a "UserCard" Component

```bash
# 1. Create component directory
mkdir -p ui/src/main/resources/jcr_root/libs/sling-cms/components/cms/usercard

# 2. Create HTL template
cat > ui/src/main/resources/jcr_root/libs/sling-cms/components/cms/usercard/usercard.html <<'EOF'
<sly data-sly-use.model="org.apache.sling.cms.core.models.UserCard">
    <div class="cms-user-card">
        <h3 class="cms-user-card__name">${model.name}</h3>
        <p class="cms-user-card__email">${model.email}</p>
    </div>
</sly>
EOF

# 3. Create Sling Model
# See full Java class example below

# 4. Create SCSS
cat > frontend/src/main/frontend/scss/_usercard.scss <<'EOF'
@import 'variables';

.cms-user-card {
  padding: $cms-spacing-unit * 2;
  border-radius: $cms-border-radius;
  background-color: lighten($cms-primary-color, 45%);

  &__name {
    margin: 0 0 $cms-spacing-unit 0;
    font-weight: 600;
  }

  &__email {
    margin: 0;
    color: darken($cms-primary-color, 10%);
  }
}
EOF

# 5. Import SCSS (add to cms.scss)
echo "@import 'usercard';" >> frontend/src/main/frontend/scss/cms.scss

# 6. NO .content.xml needed! ✅
```

**Sling Model Example:**

```java
package org.apache.sling.cms.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(
    adaptables = {SlingHttpServletRequest.class, Resource.class},
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class UserCard {

    @ValueMapValue
    private String name;

    @ValueMapValue
    private String email;

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
```

---

## Migrating from AEM or Other Sling Projects

If you're coming from Adobe Experience Manager (AEM) or other Sling projects that use `.content.xml`:

### ❌ AEM/Traditional Sling Approach

```
mycomponent/
├── .content.xml              # ❌ Not needed in Sling CMS
│   └── jcr:primaryType="cq:Component"
│   └── jcr:title="My Component"
│   └── componentGroup="My Group"
└── mycomponent.html
```

### ✅ Sling CMS Approach

```
mycomponent/
└── mycomponent.html          # ✅ Only this is needed
```

**Key Differences:**

| Feature | AEM/Traditional | Sling CMS |
|---------|----------------|-----------|
| Component definition | `.content.xml` required | Not used |
| Resource type | Defined in XML | Inferred from path |
| Component title | In `.content.xml` | In code/annotations |
| Component group | In `.content.xml` | Not applicable |
| Dialog definition | In `.content.xml` | Separate mechanism |

---

## FAQ

### Q: How do I define component properties without `.content.xml`?

**A:** Use Sling Model annotations and properties:

```java
@Model(adaptables = {SlingHttpServletRequest.class, Resource.class})
public class MyComponent {
    @ValueMapValue
    private String title;
    
    @ValueMapValue
    private String description;
}
```

### Q: How do I create component dialogs without `.content.xml`?

**A:** Sling CMS uses a separate dialog configuration mechanism. Check existing components for examples.

### Q: What if I see `.content.xml` files in existing code?

**A:** Some legacy components may still have them, but:
- Do NOT create new `.content.xml` files
- Do NOT copy `.content.xml` files from old components
- When refactoring, consider removing them if they're not needed

### Q: Does this apply to ALL `.content.xml` files?

**A:** No, only component definitions. Other content structures in the `ui/` module may still use `.content.xml` for content repository definitions. This policy specifically applies to:
- Component definitions under `/libs/sling-cms/components/`
- New component creation

### Q: How do I know my component is registered correctly?

**A:** After deploying:
1. Check the Felix Console: http://localhost:8080/system/console/components
2. Verify the HTL template is accessible
3. Test the component in the CMS

---

## Documentation References

- [Copilot Instructions](../.github/copilot-instructions.md#htl-over-jsp-required)
- [Claude Context](../claude.md#htl-over-jsp-required)
- [JSP to HTL Migration Guide](./jsp-to-htl-migration.md)
- [Component Development Guide](./custom-components.md)

---

## Enforcement

This policy is enforced through:

1. **Documentation**: Clearly stated in all coding guidelines
2. **Code Review**: Reviewers should reject PRs with new `.content.xml` files in component directories
3. **AI Assistance**: GitHub Copilot and Claude are instructed to never create `.content.xml` files
4. **Developer Onboarding**: All new developers should be made aware of this policy

---

## Exceptions

There are **NO EXCEPTIONS** to this policy for new component development.

If you believe you need a `.content.xml` file for a specific use case, please:
1. Discuss with the project maintainers
2. Document the technical reason
3. Get explicit approval before proceeding

---

## Summary

✅ **Do This:**
- Create HTL templates (`.html` files)
- Create Sling Models for business logic
- Create SCSS files for styling
- Follow existing component patterns

❌ **Don't Do This:**
- Create `.content.xml` files for components
- Copy `.content.xml` from other projects
- Assume you need XML configuration

**Remember**: Sling CMS uses convention over configuration. The file system structure and naming conventions handle component registration automatically.

---

**Last Updated**: January 10, 2026  
**Policy Owner**: Apache Sling CMS Project Team
