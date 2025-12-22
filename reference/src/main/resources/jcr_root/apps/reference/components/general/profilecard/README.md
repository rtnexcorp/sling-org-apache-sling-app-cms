# Profile Card Component - Fieldsets & Accordions Example

## Overview
The **Profile Card** component is a reference implementation that demonstrates the effective use of **Fieldsets** and **Accordions** widgets in Apache Sling CMS. It provides a structured editor interface for managing profile information organized in collapsible accordion sections, each containing organized fieldsets.

## Component Structure

### Files
```
apps/reference/components/general/profilecard/
├── .content.xml          # Component definition
├── edit.json             # Editor configuration with fieldsets & accordions
└── profilecard.html      # HTL template
```

### Supporting Files
- **Sling Model**: `core/src/main/java/.../ProfileCardModel.java`
- **Stylesheet**: `frontend/src/main/frontend/scss/_profilecard.scss`

## Editor Configuration (edit.json)

The `edit.json` file demonstrates the widget hierarchy:

```
Accordions (Containers)
└── Accordion Item
    └── Fieldset
        ├── Fieldset (Basic Details)
        │   ├── Text Field: First Name
        │   ├── Text Field: Last Name
        │   └── Text Field: Email
        └── Fieldset (Profile Photo)
            ├── Path Field: Profile Image
            └── Text Field: Image Caption
```

### Key Accordion Sections

1. **Personal Information** Accordion
   - **Basic Details** Fieldset: First/Last Name, Email
   - **Profile Photo** Fieldset: Image file, Caption

2. **Professional Information** Accordion
   - **Job Details** Fieldset: Title, Department, Company
   - **Biography** Fieldset: Extended bio text

3. **Social & Contact** Accordion
   - **Contact Information** Fieldset: Phone, Website
   - **Social Media** Fieldset: Multifield for social profiles

## Widget Relationships

### Accordions vs Fieldsets

| Feature | Accordions | Fieldsets |
|---------|-----------|-----------|
| **Purpose** | Collapsible sections (grouping accordions) | Grouped fields with legend (grouping fields) |
| **Visual** | Expandable/collapsible containers | HTML `<fieldset>` with `<legend>` |
| **Hierarchy** | Top level organization | Nested within accordion or directly |
| **Use Case** | Organize large forms into logical tabs | Group related fields visually |

### Structure in This Example
```
Accordions (Main Organization)
    └─ Fieldsets (Sub-organization)
        └─ Fields (Input elements)
```

## Sling Model Integration

The `ProfileCardModel` class demonstrates:

```java
@Model(adaptables = Resource.class)
public class ProfileCardModel {
    // Direct field injection from fieldsets
    @ValueMapValue private String firstName;
    @ValueMapValue private String lastName;
    @ValueMapValue private String email;
    
    // Multifield handling from fieldset
    public List<SocialProfile> getSocialProfiles() {
        // Process multifield data...
    }
}
```

### Key Pattern: Multifield in Fieldset
The "Social Media" fieldset contains a multifield that collects multiple social profiles. The model iterates through these:

```java
public List<SocialProfile> getSocialProfiles() {
    List<SocialProfile> profiles = new ArrayList<>();
    Resource socialProfilesResource = resource.getChild("socialProfiles");
    if (socialProfilesResource != null) {
        for (Resource profileResource : socialProfilesResource.getChildren()) {
            ValueMap profileProps = profileResource.getValueMap();
            profiles.add(new SocialProfile(
                profileProps.get("platform", String.class),
                profileProps.get("profileUrl", String.class)
            ));
        }
    }
    return profiles;
}
```

## HTL Template Pattern

The `profilecard.html` template demonstrates:

1. **Model Adaptation**: Using `data-sly-use.model` to load the Sling Model
2. **Conditional Rendering**: Using `data-sly-test` for optional fields
3. **List Rendering**: Iterating through multifield data with `data-sly-list`
4. **Semantic Classes**: Using framework-agnostic CSS classes

```html
<sly data-sly-use.model="org.apache.sling.cms.core.models.ProfileCardModel">
    <div class="profile-card">
        <div class="profile-card__header">
            <!-- Content with conditional rendering -->
            <h2>${model.firstName} ${model.lastName}</h2>
        </div>
        
        <!-- Iterate through multifield data -->
        <sly data-sly-list.social="${model.socialProfiles}">
            <a href="${social.profileUrl}">${social.platform}</a>
        </sly>
    </div>
</sly>
```

## CSS/SCSS Organization

The `_profilecard.scss` file demonstrates:

1. **Semantic Class Naming**: `.profile-card` with BEM modifier pattern
2. **Framework Abstraction**: Using variables (`$cms-primary-color`, `$cms-spacing-unit`)
3. **Responsive Design**: Mobile-first with media queries
4. **Mixin Usage**: `@include cms-card` for framework consistency

```scss
.profile-card {
  @include cms-card;  // Abstract mixin
  padding: $cms-spacing-unit * 2;  // Abstract variable
  
  &__header {
    // BEM modifier
  }
  
  @media (max-width: 768px) {
    // Responsive
  }
}
```

## Learning Outcomes

By studying this component, developers learn:

### 1. **Widget Hierarchy**
- How accordions organize fieldsets
- How fieldsets group related fields
- Multi-level form organization

### 2. **Data Binding**
- Simple fields (@ValueMapValue)
- Multifield iteration
- Resource-to-model mapping

### 3. **Template Rendering**
- Conditional field rendering
- List iteration with HTL
- Model data access

### 4. **CSS Best Practices**
- Framework-agnostic class names
- BEM naming convention
- Variable-based styling
- Responsive design patterns

### 5. **OSGi/Sling Patterns**
- @Model annotation for Sling Models
- Resource adaptation
- ValueMap property access

## Usage in CMS

To use this component:

1. **Add to Page**: In the CMS editor, add a "Profile Card" component
2. **Edit Form**: Fill in the accordion sections:
   - Expand "Personal Information" → Fill in basic details
   - Expand "Professional Information" → Add job details
   - Expand "Social & Contact" → Add social profiles
3. **View**: The component renders with semantic HTML and styled with CSS

## Configuration Files

### Component Definition (.content.xml)
```xml
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
    xmlns:cq="http://www.day.com/jcr/cq/1.0"
    xmlns:jcr="http://www.jcp.org/jcr/1.0"
    jcr:primaryType="cq:Component"
    jcr:title="Profile Card (Fieldsets + Accordions)"
    sling:resourceType="sling-cms/components/cms/form"
    componentGroup="Reference"/>
```

### Editor Configuration (edit.json)
Hierarchical structure showing:
- `accordions` widget containing
- `fieldset` widgets containing
- Field definitions (text, path, textarea, select, multifield)

## Best Practices Demonstrated

✅ **Use HTL, not JSP** - Template uses `.html` HTL file
✅ **Semantic class names** - All classes use `.cms-` prefix
✅ **Framework abstraction** - Uses variables, not Bulma classes directly
✅ **Responsive design** - Includes mobile breakpoints
✅ **Proper annotations** - OSGi R7+ @Model with DS annotations
✅ **Reusable patterns** - Follows existing Sling CMS conventions
✅ **Documentation** - Clear comments explaining structure

## Related Components

- `author` - Similar profile structure with tabs
- `accordions` - Widget implementation
- `fieldsets` - Widget implementation
- `multifield` - Widget implementation

## Further Reading

See the following documentation for detailed guidance:
- `/docs/jsp-to-htl-migration.md` - HTL best practices
- `/docs/custom-components.md` - Component creation guide
- `/docs/editor-field-types.md` - Available field types
