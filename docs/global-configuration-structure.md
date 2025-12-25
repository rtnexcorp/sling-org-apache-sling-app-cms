# Global Configuration Structure

This document describes the organization of global configurations in Apache Sling CMS (located at `/ui/src/main/resources/jcr_root/conf/global/`), organized by feature/functionality for better maintainability.

## Directory Structure

```
global/
├── .content.json                          # Root global configuration
├── files.json                             # Defines 'files' folder node type
├── dam.json                               # Defines 'dam' folder node type
├── dam/                                   # Digital Asset Management
│   ├── editors.json                       # File editors configuration
│   ├── delivery-presets.json              # Image delivery presets
│   └── transformations.json               # Image transformation configs
├── actions.json                           # Defines 'actions' folder node type
├── actions/                               # Action configurations
│   └── pageeditbar.json                   # Page edit bar actions
├── site.json                              # Defines 'site' folder node type
└── site/                                  # Site-wide configurations
    ├── policies.json                      # Component policies (Bootstrap, etc.)
    ├── rewrite.json                       # URL rewriter configuration
    ├── settings.json                      # General site settings
    ├── templates.json                     # Page templates
    ├── schemas.json                       # Defines 'schemas' folder node type
    └── schemas/                           # Content schemas
        ├── article.json                   # Article schema (blog posts, news)
        └── product.json                   # Product schema (e-commerce)
```

## Sling Content Structure Pattern

This structure follows Apache Sling's standard content loading pattern:

**Pattern**: `foldername.json` + `foldername/` directory

- **`foldername.json`**: Defines the folder's JCR node type and properties
  ```json
  {
    "jcr:primaryType": "sling:OrderedFolder",
    "jcr:content": {
      "jcr:primaryType": "nt:unstructured",
      "jcr:title": "Display Name"
    }
  }
  ```

- **`foldername/`**: Contains child configurations as separate JSON files
  - Each child is a separate `.json` file (e.g., `editors.json`, `transformations.json`)
  - Subdirectories follow the same pattern (e.g., `schemas.json` + `schemas/` folder)

**Example**: 
```
dam.json           ← Defines /conf/global/dam node
dam/
  editors.json     ← Defines /conf/global/dam/editors node
  delivery-presets.json
  transformations.json
```

**❌ DO NOT use `.content.json`** - Use the folder name as the JSON filename instead.

## Feature Organization

### 📁 Files (`files.json` + `files/`)
- **`files.json`**: Defines the files folder node type
- File editor configurations (future)
- Default file metadata fields (future)
- MIME type handling (future)

### 📁 DAM (`dam.json` + `dam/`)
**Digital Asset Management configurations:**

- **`dam.json`**: Defines the DAM folder node type with title "Default DAM"

- **`dam/editors.json`**: File editors configuration
  - Default file editor with metadata fields
  - Title, MIME type, licensing, taxonomy support
  - Publication and file metadata fields

- **`dam/delivery-presets.json`**: Image delivery presets for responsive images
  - `hero-banner`: 1920x600 WebP for page headers
  - `card-thumbnail`: 400x300 WebP for content cards
  - `avatar`: 128x128 WebP for user profiles
  - `social-share`: 1200x630 JPG for Open Graph
  - `responsive-small/medium/large`: Mobile/tablet/desktop responsive images

- **`dam/transformations.json`**: Image transformation configurations
  - `sling-cms-thumbnail`: 600x480 center crop
  - `sling-cms-thumbnail128`: 128x128 center crop
  - `sling-cms-thumbnail32`: 32x32 center crop

### 📁 Actions (`actions.json` + `actions/`)
- **`actions.json`**: Defines the actions folder node type
- **`actions/pageeditbar.json`**: Page editor toolbar actions
  - Edit Properties, Insights, References
  - Preview URL generation
  - Move/Copy, Versions, Delete

### 📁 Site (`site.json` + `site/`)
**Site-wide configurations:**

- **`site.json`**: Defines the site folder node type

- **`site/policies.json`**: Component policies
  - Bootstrap CSS framework mappings
  - Form components, CTA, columns, images, lists, search

- **`site/rewrite.json`**: URL rewriter configuration
  - DOCTYPE declaration
  - Attribute rewriting rules

- **`site/settings.json`**: General site settings
  - Taxonomy root path configuration

- **`site/templates.json`**: Page templates
  - Fragment template with title/name fields
  - Policy mappings

- **`site/schemas.json`**: Defines the schemas folder node type
- **`site/schemas/article.json`**: Article schema for blog posts/news
  - 12 fields: title, summary, body, author, publishDate, expiryDate, category, tags, featuredImage, featured, viewCount, rating
  - Categories: Technology, Business, Lifestyle, News, Opinion
- **`site/schemas/product.json`**: Product schema for e-commerce
  - 8 fields: productName, SKU, description, price, stockQuantity, category, productImages, featured
  - Categories: Electronics, Clothing, Home & Garden, Sports, Books, Toys

## Benefits of This Structure

✅ **Better Organization**: Each feature area has its own file/directory
✅ **Easier Maintenance**: Find and edit specific configurations quickly
✅ **Clearer Ownership**: DAM features clearly separated from site features
✅ **Version Control**: Smaller files = better diff tracking in Git
✅ **Reduced Merge Conflicts**: Multiple developers can work on different features
✅ **JCR Compatibility**: Follows standard Sling/JCR content structure patterns

## Migration Notes

This structure replaces the previous single `global.json` file (723 lines) with organized feature-based files:
- Root configuration: `.content.json` (6 lines)
- Files folder: `files.json` (6 lines)
- DAM configs: `dam.json` + `dam/editors.json` + `dam/delivery-presets.json` + `dam/transformations.json` (~220 lines total)
- Action configs: `actions.json` + `actions/pageeditbar.json` (~60 lines total)
- Site configs: `site.json` + `site/policies.json` + `site/rewrite.json` + `site/settings.json` + `site/templates.json` + `site/schemas.json` + `site/schemas/article.json` + `site/schemas/product.json` (~420 lines total)

**Total**: 15 organized files instead of 1 monolithic file! 🎉

## Adding New Features

When adding new configuration features, follow this pattern:

### 1. **For a new top-level feature** (e.g., `workflows`):
```
Step 1: Create foldername.json
  workflows.json
  {
    "jcr:primaryType": "sling:OrderedFolder",
    "jcr:content": {
      "jcr:primaryType": "nt:unstructured",
      "jcr:title": "Workflows"
    }
  }

Step 2: Create workflows/ directory with feature files
  workflows/
    approval.json       ← Approval workflow config
    publishing.json     ← Publishing workflow config
```

### 2. **For a child feature** (e.g., adding `validators` to `dam`):
```
Step 1: Create the feature file in the parent directory
  dam/
    editors.json
    delivery-presets.json
    transformations.json
    validators.json     ← New feature file
```

### 3. **For nested features with subdirectories** (e.g., `dam/validators` with multiple validators):
```
Step 1: Create validators.json in dam/
  dam/validators.json
  {
    "jcr:primaryType": "sling:OrderedFolder",
    "jcr:content": {
      "jcr:primaryType": "nt:unstructured",
      "jcr:title": "Asset Validators"
    }
  }

Step 2: Create validators/ directory with validator files
  dam/validators/
    file-size.json      ← File size validator config
    image-dimensions.json ← Image dimension validator config
```

## Key Principles

✅ **Use folder-name.json pattern**: Always create `foldername.json` (not `.content.json`)
✅ **Separate concerns**: Each feature gets its own JSON file
✅ **Logical grouping**: Related features go in subdirectories
✅ **Consistent naming**: Use kebab-case for file/folder names
✅ **JCR node types**: Use `sling:OrderedFolder` for ordered content, `nt:unstructured` for flexible content
