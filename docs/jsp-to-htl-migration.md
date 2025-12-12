# JSP to HTL Migration Tracker

This document tracks the gradual migration of JSP files to HTL (Sightly) templates.

## Migration Status

- **Total JSP Files**: 218
- **Migrated to HTL**: 1
- **In Progress**: 0
- **Remaining**: 217
- **Progress**: 0.5%

## Migration Priority

### High Priority (UI Core Components)
Most frequently used components that would benefit most from HTL security features.

| Directory | Count | Status |
|-----------|-------|--------|
| `editor/scripts` | 8 | 🔴 Not Started |
| `pages/base` | 6 | 🔴 Not Started |
| `editor/fields/*` | ~15 | 🔴 Not Started |

### Medium Priority (CMS Components)
Internal CMS UI components.

| Directory | Count | Status |
|-----------|-------|--------|
| `caconfig/*` | ~20 | 🔴 Not Started |
| `cms/*` | ~30 | 🔴 Not Started |
| `publication/*` | 7 | 🔴 Not Started |

### Lower Priority (Reference App)
Reference application components.

| Directory | Count | Status |
|-----------|-------|--------|
| `reference/components/*` | ~40 | 🔴 Not Started |

## Migration Guidelines

### HTL Advantages over JSP
1. **Security**: Automatic XSS protection with context-aware escaping
2. **Separation of Concerns**: Logic stays in Sling Models, templates focus on presentation
3. **Readability**: Cleaner HTML-like syntax
4. **Maintainability**: No scriptlets, easier to understand

### Conversion Patterns

#### 1. Basic Output
```jsp
<!-- JSP -->
<%= properties.get("title", "") %>

<!-- HTL -->
${properties.title}
```

#### 2. Conditional Rendering
```jsp
<!-- JSP -->
<c:if test="${not empty title}">
    <h1>${title}</h1>
</c:if>

<!-- HTL -->
<h1 data-sly-test="${properties.title}">${properties.title}</h1>
```

#### 3. Iteration
```jsp
<!-- JSP -->
<c:forEach var="item" items="${items}">
    <li>${item.title}</li>
</c:forEach>

<!-- HTL -->
<li data-sly-list="${items}">${item.title}</li>
```

#### 4. Including Components
```jsp
<!-- JSP -->
<cq:include path="content" resourceType="myapp/components/content"/>

<!-- HTL -->
<div data-sly-resource="${'content' @ resourceType='myapp/components/content'}"></div>
```

#### 5. Use Sling Models
```jsp
<!-- JSP -->
<%
    String title = resource.getValueMap().get("title", "");
    String description = resource.getValueMap().get("description", "");
%>

<!-- HTL with Sling Model -->
<sly data-sly-use.model="com.example.MyModel">
    <h1>${model.title}</h1>
    <p>${model.description}</p>
</sly>
```

## Completed Migrations

| Date | JSP File | HTL File | Notes |
|------|----------|----------|-------|
| Dec 2025 | `components/general/textelement/textelement.jsp` | `textelement.html` | Added TextElement.java Sling Model |

## Next Candidates for Migration

1. `ui/src/main/resources/jcr_root/libs/sling-cms/components/general/container/container.jsp`
2. `ui/src/main/resources/jcr_root/libs/sling-cms/components/general/textelement/textelement.jsp`
3. `ui/src/main/resources/jcr_root/libs/sling-cms/components/general/richtext/richtext.jsp`

## Notes

- Always test thoroughly after migration
- Keep JSP as backup initially, then remove after validation
- Update component definitions if needed
- Run full test suite after each migration batch

---

*Last Updated: December 2025*
