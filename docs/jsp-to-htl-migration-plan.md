<!-- Licensed to the Apache Software Foundation (ASF) under one or more contributor
	license agreements. See the NOTICE file distributed with this work for additional
	information regarding copyright ownership. The ASF licenses this file to
	you under the Apache License, Version 2.0 (the "License"); you may not use
	this file except in compliance with the License. You may obtain a copy of
	the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required
	by applicable law or agreed to in writing, software distributed under the
	License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
	OF ANY KIND, either express or implied. See the License for the specific
	language governing permissions and limitations under the License. -->

# JSP to HTL Migration Plan

This document provides a step-by-step guide for migrating Sling CMS components from JSP to HTL (Sightly).

## Table of Contents

1. [Overview](#overview)
2. [Migration Principles](#migration-principles)
3. [Common Patterns](#common-patterns)
4. [Migration Tiers](#migration-tiers)
5. [Step-by-Step Migration Process](#step-by-step-migration-process)
6. [Tier 1: Simple Components (Start Here)](#tier-1-simple-components-start-here)
7. [Tier 2: Medium Complexity](#tier-2-medium-complexity)
8. [Tier 3: Complex Components](#tier-3-complex-components)
9. [Testing Checklist](#testing-checklist)
10. [Troubleshooting](#troubleshooting)

---

## Overview

### Why Migrate to HTL?

- **Security**: HTL has automatic XSS protection with context-aware escaping
- **Maintainability**: Cleaner separation of logic (Models) and presentation (HTL)
- **No Scriptlets**: No Java code in templates reduces complexity
- **Standardization**: HTL is the Adobe/Apache recommended templating language

### Current State

The Sling CMS UI module contains ~80+ JSP files under:
- `/ui/src/main/resources/jcr_root/libs/sling-cms/components/cms/`

### Migration Goal

Incrementally replace JSP files with HTL equivalents while maintaining backward compatibility.

---

## Migration Principles

### 1. One Component at a Time
Migrate and test each component individually before moving to the next.

### 2. Delete JSP After Validation
Once HTL is working, **rename** the JSP to `.jsp.disabled` (not delete) until fully validated.

### 3. Use Sling Models for Logic
Any Java logic from JSP scriptlets or complex EL expressions should move to Sling Models.

### 4. Maintain the Same HTML Output
The rendered HTML should be identical to the JSP version.

### 5. Test in Running Instance
Always test in a running Sling CMS instance, not just build verification.

---

## Common Patterns

### JSP to HTL Mapping

| JSP Pattern | HTL Equivalent |
|-------------|----------------|
| `<%@include file="/libs/sling-cms/global.jsp"%>` | Not needed - use Sling Models instead |
| `${properties.fieldName}` | `${properties.fieldName}` (same) |
| `${resource.path}` | `${resource.path}` (same) |
| `<c:if test="${condition}">` | `<sly data-sly-test="${condition}">` |
| `<c:forEach var="item" items="${list}">` | `<sly data-sly-list.item="${list}">` |
| `<sling:include resource="${res}"/>` | `<sly data-sly-resource="${res}"/>` |
| `<sling:include resourceType="..."/>` | `<sly data-sly-resource="${@ resourceType='...'}"/>` |
| `<sling:encode value="${val}" mode="HTML"/>` | `${val @ context='html'}` |
| `<sling:encode value="${val}" mode="HTML_ATTR"/>` | `${val @ context='attribute'}` |
| `<fmt:message key="key"/>` | `${'key' @ i18n}` |
| `<sling:adaptTo adaptable="${resource}" adaptTo="Model" var="m"/>` | `<sly data-sly-use.m="com.example.Model"/>` |

### Global Variables Available in HTL

These are automatically available (no need for `global.jsp`):

| Variable | Description |
|----------|-------------|
| `${resource}` | Current resource |
| `${properties}` | Resource ValueMap (auto-adapted) |
| `${request}` | SlingHttpServletRequest |
| `${response}` | SlingHttpServletResponse |
| `${currentPage}` | Not available by default - use Sling Model |
| `${wcmmode}` | Not available by default - use Sling Model |

### When You Need a Sling Model

Create a Sling Model when you need:
- Access to `branding` or other global resources
- Complex logic or calculations
- Request attributes (`actionConfig`, etc.)
- Adapted objects (e.g., `PageContext`, `EditableResource`)
- Iteration with transformation (not just display)

---

## Migration Tiers

### Tier 1: Simple Components (No Sling Model Needed)
Components that only use `${properties.*}`, simple loops, or static content.

| Component | Lines | Complexity | Dependencies |
|-----------|-------|------------|--------------|
| `blank/blank.jsp` | 17 | Trivial | None |
| `columns/static/static.jsp` | 22 | Simple | colConfig, i18n |
| `suffixproperty/suffixproperty.jsp` | 24 | Simple | properties, suffixResource |
| `tiles/tiles.jsp` | 26 | Simple | child iteration |
| `scrollcontainer/scrollcontainer.jsp` | 21 | Simple | child iteration |

### Tier 2: Medium Complexity (May Need Simple Model)
Components with `<sling:adaptTo>` or request path info access.

| Component | Lines | Complexity | Dependencies |
|-----------|-------|------------|--------------|
| `columns/localetitle/localetitle.jsp` | 22 | Medium | LocaleResource adapter |
| `columns/name/name.jsp` | 31 | Medium | EditableResource adapter |
| `contentlayout/contentlayout.jsp` | 28 | Medium | suffixResource |
| `pagewrapper/pagewrapper.jsp` | 28 | Medium | suffixResource include |

### Tier 3: Complex Components (Require Sling Model)
Components with `global.jsp` dependencies, request attributes, or complex logic.

| Component | Lines | Complexity | Dependencies |
|-----------|-------|------------|--------------|
| `pageeditbar/pageeditbar.jsp` | 39 | High | branding, actions, includes |
| `pageeditbar/actions/actions.jsp` | 24 | High | request attribute passing |
| `actions/modal/modal.jsp` | 25 | High | actionConfig request attr |
| `actions/basic/basic.jsp` | 25 | High | actionConfig request attr |

---

## Step-by-Step Migration Process

### Step 1: Analyze the JSP

```bash
# View the JSP content
cat ui/src/main/resources/jcr_root/libs/sling-cms/components/cms/<component>/<component>.jsp
```

Identify:
- [ ] What variables are used (`properties`, `resource`, `branding`, etc.)
- [ ] What adapters are used (`sling:adaptTo`)
- [ ] What includes are present (`sling:include`)
- [ ] What conditionals exist (`c:if`, `c:choose`)
- [ ] What loops exist (`c:forEach`)

### Step 2: Create the HTL File

Create `<component>.html` in the same folder as the JSP.

```html
<!--/*
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
 */-->
<!-- HTL content here -->
```

### Step 3: Create Sling Model (If Needed)

Location: `core/src/main/java/org/apache/sling/cms/core/models/`

```java
package org.apache.sling.cms.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = SlingHttpServletRequest.class)
public class MyComponentModel {

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue(optional = true)
    private String propertyName;

    // Add getters for HTL access
    public String getPropertyName() {
        return propertyName;
    }
}
```

### Step 4: Rename JSP (Don't Delete Yet)

```bash
mv <component>.jsp <component>.jsp.disabled
```

### Step 5: Build and Deploy

```bash
mvn clean install -P autoInstallBundle -pl core,ui -DskipTests -Dbnd.baseline.skip=true
```

### Step 6: Test in Browser

1. Clear browser cache
2. Navigate to a page using the component
3. Verify HTML output matches expected behavior
4. Check browser console for errors
5. Check server logs for exceptions

### Step 7: Cleanup

Once validated:
```bash
# Delete the disabled JSP
rm <component>.jsp.disabled
# Delete any backup files
rm <component>.html.bak
```

---

## Tier 1: Simple Components (Start Here)

### Example 1: `blank/blank.jsp` → `blank.html`

**Original JSP** (17 lines - license header only, no content):
```jsp
<%-- License header --%>
```

**HTL Migration** (`blank.html`):
```html
<!--/*
 * License header
 */-->
<!-- Intentionally blank component -->
```

**Steps**:
1. Create `blank.html` with just the license header
2. Rename `blank.jsp` to `blank.jsp.disabled`
3. Build and deploy
4. Test - component should render nothing

---

### Example 2: `tiles/tiles.jsp` → `tiles.html`

**Original JSP**:
```jsp
<%@include file="/libs/sling-cms/global.jsp"%>
<div class="scroll-container contentnav">
    <div class="tile is-ancestor">
        <c:forEach var="child" items="${sling:listChildren(resource)}">
            <sling:include resource="${child}" />
        </c:forEach>
    </div>
</div>
```

**HTL Migration** (`tiles.html`):
```html
<!--/*
 * License header
 */-->
<div class="scroll-container contentnav">
    <div class="tile is-ancestor">
        <sly data-sly-list.child="${resource.children}">
            <sly data-sly-resource="${child}"></sly>
        </sly>
    </div>
</div>
```

**Key Changes**:
- `<%@include file="/libs/sling-cms/global.jsp"%>` → Removed (not needed)
- `${sling:listChildren(resource)}` → `${resource.children}`
- `<c:forEach var="child">` → `<sly data-sly-list.child>`
- `<sling:include resource="${child}"/>` → `<sly data-sly-resource="${child}">`

---

### Example 3: `scrollcontainer/scrollcontainer.jsp` → `scrollcontainer.html`

**Original JSP**:
```jsp
<%@include file="/libs/sling-cms/global.jsp"%>
<div class="scroll-container">
    <c:forEach var="child" items="${sling:listChildren(resource)}">
        <sling:include resource="${child}" />
    </c:forEach>
</div>
```

**HTL Migration** (`scrollcontainer.html`):
```html
<!--/*
 * License header
 */-->
<div class="scroll-container">
    <sly data-sly-list.child="${resource.children}">
        <sly data-sly-resource="${child}"></sly>
    </sly>
</div>
```

---

### Example 4: `suffixproperty/suffixproperty.jsp` → `suffixproperty.html`

**Original JSP**:
```jsp
<%@include file="/libs/sling-cms/global.jsp"%>
<div>
    <strong>
        <sling:encode value="${properties.label}" mode="HTML" />:
    </strong><br/>
    <sling:encode value="${slingRequest.requestPathInfo.suffixResource.valueMap[properties.property]}" mode="HTML" />
</div>
```

**HTL Migration** (`suffixproperty.html`):
```html
<!--/*
 * License header
 */-->
<sly data-sly-use.model="org.apache.sling.cms.core.models.SuffixPropertyModel">
    <div>
        <strong>
            ${properties.label @ context='html'}:
        </strong><br/>
        ${model.propertyValue @ context='html'}
    </div>
</sly>
```

**Sling Model** (`SuffixPropertyModel.java`):
```java
package org.apache.sling.cms.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = SlingHttpServletRequest.class)
public class SuffixPropertyModel {

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue(optional = true)
    private String property;

    public String getPropertyValue() {
        Resource suffixResource = request.getRequestPathInfo().getSuffixResource();
        if (suffixResource == null || property == null) {
            return "";
        }
        ValueMap vm = suffixResource.getValueMap();
        return vm.get(property, "");
    }
}
```

---

### Example 5: `columns/static/static.jsp` → `static.html`

**Original JSP**:
```jsp
<%@include file="/libs/sling-cms/global.jsp"%>
<fmt:message key="${colConfig.valueMap.value}" var="colValue" />
<td title="${sling:encode(colValue,'HTML_ATTR')}">
    <sling:encode value="${colValue}" mode="HTML" />
</td>
```

**HTL Migration** (`static.html`):
```html
<!--/*
 * License header
 */-->
<sly data-sly-use.colConfig="colConfig">
    <td title="${colConfig.valueMap.value @ i18n, context='attribute'}">
        ${colConfig.valueMap.value @ i18n, context='html'}
    </td>
</sly>
```

**Note**: The `colConfig` is passed as a request attribute from the parent table component. HTL can access it via `data-sly-use`.

---

## Testing Checklist

For each migrated component:

- [ ] HTL file created with Apache license header
- [ ] Sling Model created (if needed) with proper annotations
- [ ] JSP renamed to `.jsp.disabled`
- [ ] Build succeeds without errors
- [ ] RAT license check passes
- [ ] Component renders correctly in browser
- [ ] HTML output matches JSP version
- [ ] No JavaScript console errors
- [ ] No server-side exceptions in logs
- [ ] Internationalization (i18n) works
- [ ] XSS escaping is correct

---

## Troubleshooting

### Issue: HTL File Not Being Used

**Symptom**: JSP is still rendering even though HTL exists.

**Solution**:
1. Rename JSP to `.jsp.disabled`
2. Clear script cache: restart Sling or use Felix console
3. Redeploy the UI bundle

### Issue: Model Not Found

**Symptom**: `Cannot find model class`

**Solution**:
1. Check `Sling-Model-Packages` in `core/bnd.bnd`
2. Ensure package is exported
3. Verify `@Model` annotation is correct
4. Redeploy core bundle

### Issue: Empty Output

**Symptom**: Component renders nothing

**Solution**:
1. Check for typos in HTL expressions
2. Verify model returns non-null values
3. Check `data-sly-test` conditions
4. Add debug logging to model

### Issue: Expression Not Evaluated

**Symptom**: `${...}` shows as literal text

**Solution**:
1. Ensure file extension is `.html`
2. Check HTL syntax (no spaces after `$`)
3. Verify Sling Scripting HTL bundle is active

### Issue: Request Attributes Not Available

**Symptom**: Variables from parent component not accessible

**Solution**:
1. Use Sling Model with `@SlingObject` request injection
2. Access via `request.getAttribute("attrName")`
3. Consider using `data-sly-resource` with `requestAttributes` option

---

## Progress Tracking

### Tier 1 Components

- [ ] `blank/blank.jsp`
- [ ] `tiles/tiles.jsp`
- [ ] `scrollcontainer/scrollcontainer.jsp`
- [ ] `suffixproperty/suffixproperty.jsp`
- [ ] `columns/static/static.jsp`

### Tier 2 Components

- [ ] `columns/localetitle/localetitle.jsp`
- [ ] `columns/name/name.jsp`
- [ ] `columns/text/text.jsp`
- [ ] `contentlayout/contentlayout.jsp`
- [ ] `pagewrapper/pagewrapper.jsp`

### Tier 3 Components

- [ ] `pageeditbar/pageeditbar.jsp`
- [ ] `pageeditbar/actions/actions.jsp`
- [ ] `actions/modal/modal.jsp`
- [ ] `actions/basic/basic.jsp`
- [ ] `pageproperties/pageproperties.jsp`

---

*Last Updated: December 15, 2025*
