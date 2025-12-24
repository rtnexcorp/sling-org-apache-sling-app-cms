# JSP to HTL Migration Guide

Complete reference for migrating JSP to HTL in Apache Sling CMS. This guide covers **real patterns from this codebase** with working examples.

---

## ⚠️ Critical Migration Rules

### 1. HTL Has NO Request Scope Variables
**JSP `scope="request"` does NOT work in HTL.** You must use a Sling Model.

```jsp
<!-- ❌ JSP - This pattern CANNOT be directly converted -->
<c:set var="search" value="${...}" scope="request" />
<sling:call script="result.jsp" />  <!-- result.jsp reads ${search} -->
```

**Solution:** Create a Sling Model that provides all needed data.

### 2. HTL Has NO varStatus
**JSP `varStatus` for loop index does NOT exist in HTL.**

```jsp
<!-- ❌ JSP varStatus - No direct HTL equivalent -->
<c:forEach var="col" items="${items}" varStatus="status">
    ${status.index}, ${status.first}, ${status.last}
</c:forEach>
```

```html
<!-- ✅ HTL - Use itemList built-in -->
<div data-sly-list="${items}">
    ${itemList.index}, ${itemList.first}, ${itemList.last}
</div>
```

### 3. HTL Has NO sling:getParents/findResources/listChildren Functions
**Sling taglib EL functions do NOT exist in HTL.** You must use a Sling Model.

```jsp
<!-- ❌ JSP - These functions don't exist in HTL -->
${sling:getParents(resource, 3)}
${sling:findResources(resourceResolver, query, 'JCR-SQL2')}
${sling:listChildren(resource)}
${sling:getResource(resourceResolver, path)}
```

**Solution:** Create a Sling Model that performs these operations.

### 4. HTL Has NO c:choose/c:when/c:otherwise
**Use multiple `data-sly-test` with negation.**

```jsp
<!-- ❌ JSP c:choose -->
<c:choose>
    <c:when test="${type == 'a'}">A</c:when>
    <c:when test="${type == 'b'}">B</c:when>
    <c:otherwise>Other</c:otherwise>
</c:choose>
```

```html
<!-- ✅ HTL - Use data-sly-test with variables for complex logic -->
<sly data-sly-test.isA="${type == 'a'}">A</sly>
<sly data-sly-test.isB="${type == 'b' && !isA}">B</sly>
<sly data-sly-test="${!isA && !isB}">Other</sly>
```

### 5. HTL Has NO param Object for Query Parameters
**Use `request.requestParameterMap` or Sling Model.**

```jsp
<!-- ❌ JSP param -->
<c:if test="${not empty param.q}">
    Search: ${param.q}
</c:if>
```

```html
<!-- ✅ HTL - Access via request -->
<sly data-sly-test="${request.requestParameterMap['q']}">
    Search: ${request.requestParameterMap['q'][0].string}
</sly>

<!-- ✅ Better - Use Sling Model -->
<sly data-sly-use.model="com.example.SearchModel">
    <sly data-sly-test="${model.searchTerm}">
        Search: ${model.searchTerm}
    </sly>
</sly>
```

### 6. HTL Has NO fn:split/fn:length/fn:contains
**JSTL functions do NOT exist in HTL.** Use Sling Model.

```jsp
<!-- ❌ JSP - No HTL equivalent -->
${fn:split(properties.layout, ',')}
${fn:length(items)}
${fn:contains(text, 'search')}
```

**Solution:** Create a Sling Model with methods that return processed data.

---

## Quick Reference Table

| JSP | HTL | Notes |
|-----|-----|-------|
| `${resource}` | `${resource}` | Same |
| `${resourceResolver}` | `${resolver}` | **Different name!** |
| `${slingRequest}` | `${request}` | **Different name!** |
| `${properties.x}` | `${properties.x}` | Same |
| `${param.x}` | See Rule #5 | **Not available!** |
| `<c:if test="">` | `data-sly-test` | |
| `<c:forEach>` | `data-sly-list` | |
| `<c:forEach varStatus>` | `${itemList.index}` | See Rule #2 |
| `<c:set var="">` | `data-sly-set` | **No scope support!** |
| `<c:choose>` | Multiple `data-sly-test` | See Rule #4 |
| `<sling:adaptTo>` | `data-sly-use` | |
| `<sling:include>` | `data-sly-resource` | |
| `<sling:call script="">` | `data-sly-include` | |
| `<sling:encode>` | Automatic | |
| `<fmt:message>` | `${'key' @ i18n}` | |
| `scope="request"` | **Not available** | Use Sling Model |
| `${sling:getResource()}` | **Not available** | Use Sling Model |
| `${sling:listChildren()}` | **Not available** | Use Sling Model |
| `${fn:split()}` | **Not available** | Use Sling Model |

---

## Real Migration Examples

### Example 1: Breadcrumb Component (Uses sling:getParents)

**Original JSP** (`breadcrumb.jsp`):
```jsp
<%@include file="/libs/sling-cms/global.jsp"%>
<a href="/">
    <sling:encode value="${properties.home}" mode="HTML" />
</a>&nbsp;&#xbb;&nbsp;
<c:if test="${not empty properties.level}">
    <c:forEach var="parent" items="${sling:getParents(currentPage.resource,properties.level)}">
        <c:if test="${parent.path != page.resource.path}">
            <a href="${sling:encode(parent.path,'HTML_ATTR')}.html">
                <sling:encode value="${parent.valueMap['jcr:content/jcr:title']}" default="${parent.name}" mode="HTML" />
            </a>&nbsp;&#xbb;&nbsp;
        </c:if>
    </c:forEach>
</c:if>
```

**Step 1: Create Sling Model** (`BreadcrumbModel.java`):
```java
package org.apache.sling.cms.reference.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.Page;
import org.apache.sling.cms.PageManager;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = SlingHttpServletRequest.class)
public class BreadcrumbModel {

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue(optional = true)
    private String home;

    @ValueMapValue(optional = true)
    private Integer level;

    private List<BreadcrumbItem> items;
    private Page currentPage;

    @PostConstruct
    protected void init() {
        items = new ArrayList<>();
        Resource resource = request.getResource();
        PageManager pageManager = resource.adaptTo(PageManager.class);
        
        if (pageManager != null) {
            currentPage = pageManager.getPage();
        }

        if (level != null && level > 0 && currentPage != null) {
            Resource pageResource = currentPage.getResource();
            List<Resource> parents = new ArrayList<>();
            
            // Collect parents up to level
            Resource parent = pageResource.getParent();
            int count = 0;
            while (parent != null && count < level) {
                if (parent.getResourceType().equals("sling-cms/components/cms/page") 
                    || parent.getChild("jcr:content") != null) {
                    parents.add(parent);
                }
                parent = parent.getParent();
                count++;
            }
            
            // Reverse to get root-first order
            Collections.reverse(parents);
            
            for (Resource p : parents) {
                if (!p.getPath().equals(pageResource.getPath())) {
                    String title = p.getValueMap().get("jcr:content/jcr:title", p.getName());
                    items.add(new BreadcrumbItem(p.getPath(), title));
                }
            }
        }
    }

    public String getHome() {
        return home;
    }

    public List<BreadcrumbItem> getItems() {
        return items;
    }

    public static class BreadcrumbItem {
        private final String path;
        private final String title;

        public BreadcrumbItem(String path, String title) {
            this.path = path;
            this.title = title;
        }

        public String getPath() {
            return path;
        }

        public String getTitle() {
            return title;
        }
    }
}
```

**Step 2: Create HTL** (`breadcrumb.html`):
```html
<sly data-sly-use.model="org.apache.sling.cms.reference.models.BreadcrumbModel">
    <a href="/">${model.home}</a>&nbsp;&#xbb;&nbsp;
    <sly data-sly-list="${model.items}">
        <a href="${item.path}.html">${item.title}</a>&nbsp;&#xbb;&nbsp;
    </sly>
</sly>
```

---

### Example 2: Column Control (Uses varStatus and fn:split)

**Original JSP** (`columncontrol.jsp`):
```jsp
<%@include file="/libs/sling-cms/global.jsp"%>
<sling:adaptTo adaptable="${resource}" adaptTo="org.apache.sling.cms.ComponentPolicyManager" var="componentPolicyMgr" />
<c:set var="configRsrc" value="${componentPolicyMgr.componentPolicy.componentConfigs['reference/components/general/columncontrol']}" />
<c:choose>
    <c:when test="${properties.container == true}">
        <div class="${sling:encode(configRsrc.valueMap.containerclass,'HTML_ATTR')}">
            <div class="${sling:encode(configRsrc.valueMap.rowClass,'HTML_ATTR')}">
                <c:forEach var="col" items="${fn:split(properties.layout,',')}" varStatus="status">
                    <div class="${sling:encode(col,'HTML_ATTR')}">
                        <sling:include path="col-${status.index}" resourceType="sling-cms/components/general/container" />
                    </div>
                </c:forEach>
            </div>
        </div>
    </c:when>
    <c:otherwise>
        <div class="${sling:encode(configRsrc.valueMap.rowClass,'HTML_ATTR')}">
            <c:forEach var="col" items="${fn:split(properties.layout,',')}" varStatus="status">
                <div class="${sling:encode(col,'HTML_ATTR')}">
                    <sling:include path="col-${status.index}" resourceType="sling-cms/components/general/container" />
                </div>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>
```

**Step 1: Create Sling Model** (`ColumnControlModel.java`):
```java
package org.apache.sling.cms.reference.models;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cms.ComponentPolicyManager;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = Resource.class)
public class ColumnControlModel {

    @Self
    private Resource resource;

    @ValueMapValue(optional = true)
    private Boolean container;

    @ValueMapValue(optional = true)
    private String layout;

    private String containerClass;
    private String rowClass;
    private List<Column> columns;

    @PostConstruct
    protected void init() {
        columns = new ArrayList<>();
        
        ComponentPolicyManager policyMgr = resource.adaptTo(ComponentPolicyManager.class);
        if (policyMgr != null && policyMgr.getComponentPolicy() != null) {
            Resource configRsrc = policyMgr.getComponentPolicy()
                .getComponentConfigs().get("reference/components/general/columncontrol");
            if (configRsrc != null) {
                containerClass = configRsrc.getValueMap().get("containerclass", "");
                rowClass = configRsrc.getValueMap().get("rowClass", "");
            }
        }

        if (layout != null && !layout.isEmpty()) {
            String[] cols = layout.split(",");
            for (int i = 0; i < cols.length; i++) {
                columns.add(new Column(i, cols[i].trim()));
            }
        }
    }

    public boolean isContainer() {
        return Boolean.TRUE.equals(container);
    }

    public String getContainerClass() {
        return containerClass;
    }

    public String getRowClass() {
        return rowClass;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public static class Column {
        private final int index;
        private final String cssClass;
        private final String path;

        public Column(int index, String cssClass) {
            this.index = index;
            this.cssClass = cssClass;
            this.path = "col-" + index;
        }

        public int getIndex() {
            return index;
        }

        public String getCssClass() {
            return cssClass;
        }

        public String getPath() {
            return path;
        }
    }
}
```

**Step 2: Create HTL** (`columncontrol.html`):
```html
<sly data-sly-use.model="org.apache.sling.cms.reference.models.ColumnControlModel">
    <sly data-sly-test="${model.container}">
        <div class="${model.containerClass}">
            <div class="${model.rowClass}">
                <sly data-sly-list="${model.columns}">
                    <div class="${item.cssClass}">
                        <sly data-sly-resource="${item.path @ resourceType='sling-cms/components/general/container'}"></sly>
                    </div>
                </sly>
            </div>
        </div>
    </sly>
    <sly data-sly-test="${!model.container}">
        <div class="${model.rowClass}">
            <sly data-sly-list="${model.columns}">
                <div class="${item.cssClass}">
                    <sly data-sly-resource="${item.path @ resourceType='sling-cms/components/general/container'}"></sly>
                </div>
            </sly>
        </div>
    </sly>
</sly>
```

---

### Example 3: Login Form (Uses param and adaptTo)

**Original JSP** (`login.jsp`):
```jsp
<%@include file="/libs/sling-cms/global.jsp"%>
<sling:adaptTo adaptable="${resource}" adaptTo="org.apache.sling.cms.PageManager" var="pageManager" />
<sling:adaptTo adaptable="${resource}" adaptTo="org.apache.sling.cms.ComponentPolicyManager" var="componentPolicyMgr" />
<c:set var="formConfig" value="${componentPolicyMgr.componentPolicy.componentConfigs['reference/components/forms/form'].valueMap}" />
<form class="${formConfig.formClass}" action="${pageManager.page.path}.allowpost.html/j_security_check" method="post">  
    <c:if test="${not empty param.j_reason}">
        <div class="${formConfig.alertClass}">
            ${properties.errorMessage}
        </div>
    </c:if>
    <!-- ... form fields ... -->
</form>
```

**Step 1: Create Sling Model** (`LoginFormModel.java`):
```java
package org.apache.sling.cms.reference.models;

import javax.annotation.PostConstruct;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.cms.ComponentPolicyManager;
import org.apache.sling.cms.PageManager;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = SlingHttpServletRequest.class)
public class LoginFormModel {

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @ValueMapValue(optional = true)
    private String errorMessage;

    @ValueMapValue(optional = true)
    private String usernameLabel;

    @ValueMapValue(optional = true)
    private String passwordLabel;

    @ValueMapValue(optional = true)
    private String submitLabel;

    @ValueMapValue(optional = true)
    private String successPage;

    private String formAction;
    private ValueMap formConfig;
    private boolean hasError;

    @PostConstruct
    protected void init() {
        // Check for login error
        hasError = request.getParameter("j_reason") != null 
                   && !request.getParameter("j_reason").isEmpty();

        // Get page path for form action
        PageManager pageManager = resource.adaptTo(PageManager.class);
        if (pageManager != null && pageManager.getPage() != null) {
            formAction = pageManager.getPage().getPath() + ".allowpost.html/j_security_check";
        }

        // Get form configuration from policy
        ComponentPolicyManager policyMgr = resource.adaptTo(ComponentPolicyManager.class);
        if (policyMgr != null && policyMgr.getComponentPolicy() != null) {
            Resource configRsrc = policyMgr.getComponentPolicy()
                .getComponentConfigs().get("reference/components/forms/form");
            if (configRsrc != null) {
                formConfig = configRsrc.getValueMap();
            }
        }
    }

    public boolean isHasError() {
        return hasError;
    }

    public String getFormAction() {
        return formAction;
    }

    public String getFormClass() {
        return formConfig != null ? formConfig.get("formClass", "") : "";
    }

    public String getAlertClass() {
        return formConfig != null ? formConfig.get("alertClass", "") : "";
    }

    public String getFieldGroupClass() {
        return formConfig != null ? formConfig.get("fieldGroupClass", "") : "";
    }

    public String getFieldClass() {
        return formConfig != null ? formConfig.get("fieldClass", "") : "";
    }

    public String getFieldRequiredClass() {
        return formConfig != null ? formConfig.get("fieldRequiredClass", "") : "";
    }

    public String getSubmitClass() {
        return formConfig != null ? formConfig.get("submitClass", "") : "";
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getUsernameLabel() {
        return usernameLabel;
    }

    public String getPasswordLabel() {
        return passwordLabel;
    }

    public String getSubmitLabel() {
        return submitLabel;
    }

    public String getSuccessPage() {
        return successPage;
    }
}
```

**Step 2: Create HTL** (`login.html`):
```html
<sly data-sly-use.model="org.apache.sling.cms.reference.models.LoginFormModel">
    <form class="${model.formClass}" action="${model.formAction}" method="post" data-analytics-id="Login Form">
        <sly data-sly-test="${model.hasError}">
            <div class="${model.alertClass}">
                ${model.errorMessage}
            </div>
        </sly>
        <div class="${model.fieldGroupClass}">
            <label for="j_username" class="label">${model.usernameLabel} <span class="${model.fieldRequiredClass}">*</span></label>
            <input type="text" class="${model.fieldClass}" required="required" name="j_username" />
        </div>
        <div class="${model.fieldGroupClass}">
            <label for="j_password" class="label">${model.passwordLabel} <span class="${model.fieldRequiredClass}">*</span></label>
            <input type="password" class="${model.fieldClass}" required="required" name="j_password" />
        </div>
        <input type="hidden" name="resource" value="${model.successPage}.html" />
        <input type="hidden" name="j_validate" value="true" />
        <div class="${model.fieldGroupClass}">
            <button class="${model.submitClass}">${model.submitLabel}</button>
        </div>
    </form>
</sly>
```

---

## HTL Syntax Reference

### Loop with Index (itemList)
```html
<ul data-sly-list="${items}">
    <li>
        Index: ${itemList.index}
        Count: ${itemList.count}
        First: ${itemList.first}
        Last: ${itemList.last}
        Middle: ${itemList.middle}
        Odd: ${itemList.odd}
        Even: ${itemList.even}
        Value: ${item}
    </li>
</ul>
```

### Include Resource
```html
<!-- Include child resource -->
<sly data-sly-resource="${'childpath'}"></sly>

<!-- Include with resource type override -->
<sly data-sly-resource="${'childpath' @ resourceType='myapp/components/foo'}"></sly>

<!-- Include resource object -->
<sly data-sly-resource="${myResourceObject}"></sly>
```

### Include Script (Template)
```html
<!-- Include another HTL file -->
<sly data-sly-include="other.html"></sly>
```

### Templates and Calls
```html
<!-- Define template -->
<template data-sly-template.myTemplate="${@ title, items}">
    <h1>${title}</h1>
    <ul data-sly-list="${items}">
        <li>${item}</li>
    </ul>
</template>

<!-- Call template -->
<sly data-sly-call="${myTemplate @ title='Hello', items=myList}"></sly>
```

### Use with Parameters
```html
<!-- Pass parameters to Sling Model constructor -->
<sly data-sly-use.model="${'com.example.MyModel' @ param1='value1', param2=someVar}">
    ${model.result}
</sly>
```

---

## Migration Checklist

Before migrating a JSP, check if it uses these patterns that **require a Sling Model**:

- [ ] `scope="request"` - **Requires Sling Model**
- [ ] `${param.xxx}` - **Requires Sling Model or request.requestParameterMap**
- [ ] `${sling:getParents()}` - **Requires Sling Model**
- [ ] `${sling:findResources()}` - **Requires Sling Model**
- [ ] `${sling:listChildren()}` - **Requires Sling Model**
- [ ] `${sling:getResource()}` - **Requires Sling Model**
- [ ] `${fn:split()}` - **Requires Sling Model**
- [ ] `${fn:length()}` - **Requires Sling Model**
- [ ] `varStatus` in loops - Use `${itemList.xxx}`
- [ ] `<c:choose>` - Use multiple `data-sly-test`
- [ ] `<sling:call script="">` - Use `data-sly-include`

### After Migration
- [ ] Test all functionality works as before
- [ ] Verify XSS protection (HTL auto-escapes)
- [ ] Check edit mode still works
- [ ] Test with different content variations
- [ ] Remove JSP file only after HTL is verified

---

## Global.jsp Analysis

The file `/libs/sling-cms/global.jsp` is included by most JSP components.

> **Important:** Sling CMS does NOT create any custom implicit objects. It only uses standard Apache Sling mechanisms.

### Variables in Global.jsp

| Variable | Source | Provider | HTL Equivalent |
|----------|--------|----------|----------------|
| `resource` | `<sling:defineObjects />` | Apache Sling | `${resource}` (automatic) |
| `resourceResolver` | `<sling:defineObjects />` | Apache Sling | `${resolver}` (automatic) |
| `slingRequest` | `<sling:defineObjects />` | Apache Sling | `${request}` (automatic) |
| `slingResponse` | `<sling:defineObjects />` | Apache Sling | `${response}` (automatic) |
| `currentNode` | `<sling:defineObjects />` | Apache Sling | `${currentNode}` (automatic) |
| `currentSession` | `<sling:defineObjects />` | Apache Sling | `${currentSession}` (automatic) |
| `log` | `<sling:defineObjects />` | Apache Sling | `${log}` (automatic) |
| `properties` | `<sling:adaptTo>` | Apache Sling | `${properties}` (automatic) |
| `slingPageContext` | `<sling:adaptTo>` | Sling CMS interface | `data-sly-use.ctx="org.apache.sling.cms.PageContext"` |
| `branding` | `<c:set>` | JSTL convenience | Use Sling Model or `data-sly-use` |

> **Key Point**: In HTL, you do NOT need `global.jsp` - all core objects are automatically available!

---

## Common Sling CMS Adaptations

```html
<!-- PageContext (site/page info) -->
<sly data-sly-use.ctx="org.apache.sling.cms.PageContext">
    ${ctx.site.title} | ${ctx.page.title}
</sly>

<!-- PageManager -->
<sly data-sly-use.pm="org.apache.sling.cms.PageManager">
    ${pm.page.title}
</sly>

<!-- ComponentConfiguration -->
<sly data-sly-use.cfg="org.apache.sling.cms.ComponentConfiguration">
    ${cfg.properties.myProp}
</sly>
```

---

## Migration Status

- **Total JSP Files**: 218
- **Migrated to HTL**: 1
- **Progress**: 0.5%

### Completed Migrations

| Date | JSP File | HTL File | Sling Model |
|------|----------|----------|-------------|
| Dec 2025 | `textelement.jsp` | `textelement.html` | `TextElement.java` |

---

*Last Updated: December 2025*
