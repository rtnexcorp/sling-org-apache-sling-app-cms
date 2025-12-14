# i18ncontainer JSP to HTL Conversion - Review & Corrections

## Date: December 14, 2025

## Summary
Successfully converted `i18ncontainer.jsp` to HTL with corrections to ensure proper HTL compatibility.

---

## Initial Issues Found

### ❌ Problem 1: Invalid HTL Syntax
**Location**: Line 65 of initial HTL template

**Issue**:
```html
<sly data-sly-use.entryHelper="${'org.apache.sling.cms.core.models.I18nContainerModel$LanguageFolder' @ resource=language.resource}">
```

**Why it's wrong**:
- HTL cannot instantiate inner classes directly using `data-sly-use`
- The syntax `${' org.apache.sling.cms.core.models.I18nContainerModel$LanguageFolder' @ resource=language.resource}` is invalid
- Variable `entryHelper` was created but never used

---

### ❌ Problem 2: Method Calls with Parameters
**Location**: Line 66 of initial HTL template

**Issue**:
```html
<sly data-sly-set.entry="${language.getEntryForKey(key)}">
```

**Why it's wrong**:
- **HTL cannot call methods with parameters** - this is a fundamental limitation
- Expressions like `getEntryForKey(key)` will fail at runtime
- HTL only supports property access and parameterless methods

---

### ❌ Problem 3: Inconsistent List Iteration
**Location**: Line 64 of initial HTL template

**Issue**:
- Mixed use of `data-sly-list` and `data-sly-repeat` in nested loops
- While both work, consistency improves readability

---

## Solution Implemented

### ✅ Model Refactoring: HTL-Friendly Design

Created two new inner classes in `I18nContainerModel`:

#### 1. **KeyRow Class**
```java
public static class KeyRow {
    private final String key;
    private final List<LanguageEntry> languageEntries;
    
    public KeyRow(String key, List<LanguageFolder> languages) {
        this.key = key;
        this.languageEntries = new ArrayList<>();
        for (LanguageFolder language : languages) {
            languageEntries.add(new LanguageEntry(language, key));
        }
    }
    
    public String getKey() { return key; }
    public List<LanguageEntry> getLanguageEntries() { return languageEntries; }
}
```

**Purpose**: Represents a row in the translation table, pre-computing all language entries for a given key.

---

#### 2. **LanguageEntry Class**
```java
public static class LanguageEntry {
    private final LanguageFolder language;
    private final String key;
    private final TranslationEntry entry;
    
    public LanguageEntry(LanguageFolder language, String key) {
        this.language = language;
        this.key = key;
        this.entry = language.getEntryForKey(key); // Called in Java, not HTL
    }
    
    public String getLanguageCode() { return language.getLanguageCode(); }
    public String getLanguageName() { return language.getName(); }
    public boolean hasEntry() { return entry != null; }
    public String getEntryName() { return entry != null ? entry.getName() : key; }
    public String getMessage() { return entry != null ? entry.getMessage() : ""; }
    public String getKey() { return key; }
}
```

**Purpose**: Pre-computes whether an entry exists for a key in a specific language. HTL only needs to access properties, not call methods with parameters.

---

#### 3. **getKeyRows() Method**
```java
public List<KeyRow> getKeyRows() {
    if (keyRows == null) {
        keyRows = new ArrayList<>();
        List<String> allKeys = getKeys();
        List<LanguageFolder> allLanguages = getLanguages();
        
        for (String key : allKeys) {
            keyRows.add(new KeyRow(key, allLanguages));
        }
    }
    return keyRows;
}
```

**Purpose**: Pre-computes the entire table structure in Java, so HTL only needs simple property access.

---

### ✅ Updated HTL Template

**Before** (Incorrect):
```html
<tbody>
    <sly data-sly-list.key="${model.keys}">
        <tr>
            <td>${key}</td>
            <sly data-sly-repeat.language="${model.languages}">
                <sly data-sly-use.entryHelper="..."> <!-- WRONG -->
                    <sly data-sly-set.entry="${language.getEntryForKey(key)}"> <!-- WRONG -->
                        <td>
                            <sly data-sly-test="${entry}">...</sly>
                        </td>
                    </sly>
                </sly>
            </sly>
        </tr>
    </sly>
</tbody>
```

**After** (Correct):
```html
<tbody>
    <sly data-sly-list.row="${model.keyRows}">
        <tr>
            <td>${row.key}</td>
            <sly data-sly-list.langEntry="${row.languageEntries}">
                <td role="group" aria-labelledby="Column-${langEntry.languageCode @ context='attribute'}">
                    <sly data-sly-test="${langEntry.hasEntry}">
                        <input name="${langEntry.languageName @ context='attribute'}/${langEntry.entryName @ context='attribute'}/sling:message" 
                               class="input" type="text" 
                               value="${langEntry.message @ context='attribute'}" />
                        <input name="${langEntry.languageName @ context='attribute'}/${langEntry.entryName @ context='attribute'}/sling:key" 
                               type="hidden" value="${langEntry.key @ context='attribute'}" />
                    </sly>
                    <sly data-sly-test="${!langEntry.hasEntry}">
                        <input name="${langEntry.languageName @ context='attribute'}/${langEntry.key @ context='attribute'}/sling:message" 
                               class="input" type="text" value="" />
                        <input name="${langEntry.languageName @ context='attribute'}/${langEntry.key @ context='attribute'}/sling:key" 
                               type="hidden" value="${langEntry.key @ context='attribute'}" />
                        <input name="${langEntry.languageName @ context='attribute'}/${langEntry.key @ context='attribute'}/jcr:primaryType" 
                               type="hidden" value="sling:MessageEntry" />
                    </sly>
                </td>
            </sly>
        </tr>
    </sly>
</tbody>
```

---

## Key Improvements

### 1. **HTL Compatibility**
- ✅ No method calls with parameters
- ✅ Only property access (e.g., `${langEntry.hasEntry}`, `${langEntry.message}`)
- ✅ Pre-computed structure in Java model

### 2. **Performance**
- ✅ Lazy initialization with caching (`if (keyRows == null)`)
- ✅ Single traversal of keys and languages
- ✅ No repeated method calls during rendering

### 3. **Maintainability**
- ✅ Clear separation: complex logic in Java, simple display in HTL
- ✅ Consistent naming: `row`, `langEntry`
- ✅ Follows HTL best practices

### 4. **Functionality Preserved**
- ✅ Same form field names as JSP version
- ✅ Proper handling of existing vs. new entries
- ✅ Correct `jcr:primaryType` for new entries

---

## HTL Principles Applied

### 1. **No Business Logic in Templates**
All logic moved to Java model:
- Checking if entry exists: `hasEntry()` method
- Getting entry name: `getEntryName()` method
- Retrieving message: `getMessage()` method

### 2. **HTL Can Only:**
- ✅ Access properties (getters)
- ✅ Call parameterless methods
- ✅ Use simple boolean expressions
- ❌ Call methods with parameters
- ❌ Perform complex computations

### 3. **Pre-Compute in Java, Display in HTL**
- Java: Build `KeyRow` objects with all data
- HTL: Simply iterate and display

---

## Files Changed

### 1. **Core Module**
- ✅ `I18nContainerModel.java` - Added `KeyRow` and `LanguageEntry` classes
- ✅ Applied Spotless formatting
- ✅ No compilation errors

### 2. **UI Module**
- ✅ `i18ncontainer.html` - Updated HTL template
- ✅ Deleted `i18ncontainer.jsp` - No longer needed

### 3. **Deployment**
- ✅ Deployed core module
- ✅ Deployed ui module
- ✅ No errors in error.log

---

## Testing Checklist

### Manual Testing Required:
1. **Basic Functionality**
   - [ ] Navigate to http://localhost:8082/cms/i18n/content.html/etc/i18n
   - [ ] Verify translation table displays correctly
   - [ ] Verify language columns show proper headers

2. **Existing Entries**
   - [ ] Verify existing translations display in input fields
   - [ ] Verify correct values are shown

3. **New Entries**
   - [ ] Add a new entry using "+ Entry" button
   - [ ] Verify new entry appears in table
   - [ ] Verify empty input fields for new key

4. **Editing**
   - [ ] Edit an existing translation
   - [ ] Click "Save i18n Dictionary"
   - [ ] Verify changes are saved

5. **Form Field Names**
   - [ ] Inspect HTML source
   - [ ] Verify field names match pattern: `{language}/{entry}/sling:message`
   - [ ] Verify hidden fields for `sling:key` and `jcr:primaryType`

---

## Lessons Learned

### 1. **HTL Limitations**
- HTL cannot call methods with parameters like `getEntryForKey(key)`
- Solution: Pre-compute in Java model

### 2. **Inner Class Access**
- HTL cannot directly instantiate inner classes
- Solution: Provide fully computed objects from outer class

### 3. **Model Design for HTL**
- Design models to expose data as properties, not methods requiring parameters
- Use wrapper classes to pre-compute nested relationships
- Cache computed values to avoid redundant processing

### 4. **Migration Strategy**
- First convert logic to model
- Then simplify HTL to only display pre-computed data
- Always test with real data

---

## Comparison: JSP vs. HTL

| Aspect | JSP | HTL |
|--------|-----|-----|
| **Method Calls** | Can call with params: `${helper.getEntryForKey(key)}` | Cannot - must pre-compute in model |
| **Business Logic** | Mixed in template with scriptlets | Separated in Java model |
| **Type Safety** | Runtime errors | Compile-time errors in model |
| **Readability** | Complex with scriptlets | Clean, declarative |
| **Performance** | Similar | Similar (with caching) |
| **Maintainability** | Lower (logic scattered) | Higher (logic in Java) |

---

## Conclusion

✅ **Successfully converted i18ncontainer.jsp to HTL**
✅ **Fixed HTL compatibility issues**
✅ **Applied best practices for HTL model design**
✅ **No errors in deployment**
✅ **Ready for testing**

The conversion demonstrates the proper way to handle complex logic in HTL:
1. Pre-compute all data in Java model
2. Expose data as simple properties
3. Keep HTL template clean and declarative

---

## Next Steps

1. **Test i18n dictionary editor** at http://localhost:8082/cms/i18n/content.html/etc/i18n
2. **Verify form submissions** work correctly
3. **Check for any console errors**
4. **Update documentation** if needed

---

## References

- [HTL Specification](https://github.com/adobe/htl-spec)
- [HTL Expression Language Limitations](https://experienceleague.adobe.com/docs/experience-manager-htl/content/specification.html)
- Apache Sling CMS Copilot Instructions (HTL over JSP section)
