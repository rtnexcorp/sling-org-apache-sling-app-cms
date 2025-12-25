# Global Configuration Structure

This document describes the organization of global configurations in Apache Sling CMS (located at `/ui/src/main/resources/jcr_root/conf/global/`), organized by feature/functionality for better maintainability.

## Directory Structure

```
global/
├── .content.json                          # Root global configuration
├── files/                                 # File handling configurations
│   └── .content.json
├── dam/                                   # Digital Asset Management
│   ├── .content.json                      # DAM root + editors configuration
│   ├── delivery-presets/                  # Image delivery presets
│   │   └── .content.json
│   └── transformations/                   # Image transformation configs
│       └── .content.json
├── actions/                               # Action configurations
│   ├── .content.json
│   └── pageeditbar/                       # Page edit bar actions
│       └── .content.json
└── site/                                  # Site-wide configurations
    ├── .content.json
    ├── policies/                          # Component policies (Bootstrap, etc.)
    │   └── .content.json
    ├── rewrite/                           # URL rewriter configuration
    │   └── .content.json
    ├── settings/                          # General site settings
    │   └── .content.json
    ├── templates/                         # Page templates
    │   └── .content.json
    └── schemas/                           # Content schemas (Article, Product)
        └── .content.json
```

## Feature Organization

### 📁 Files (`/files`)
- File editor configurations
- Default file metadata fields
- MIME type handling

### 📁 DAM (`/dam`)
**Digital Asset Management configurations organized into:**

- **`dam/.content.json`**: File editors configuration
  - Default file editor with metadata fields
  - Title, MIME type, licensing, taxonomy support
  - Publication and file metadata fields

- **`dam/delivery-presets/`**: Image delivery presets for responsive images
  - `hero-banner`: 1920x600 WebP for page headers
  - `card-thumbnail`: 400x300 WebP for content cards
  - `avatar`: 128x128 WebP for user profiles
  - `social-share`: 1200x630 JPG for Open Graph
  - `responsive-small/medium/large`: Mobile/tablet/desktop responsive images

- **`dam/transformations/`**: Image transformation configurations
  - `sling-cms-thumbnail`: 600x480 center crop
  - `sling-cms-thumbnail128`: 128x128 center crop
  - `sling-cms-thumbnail32`: 32x32 center crop

### 📁 Actions (`/actions`)
- **`actions/pageeditbar/`**: Page editor toolbar actions
  - Edit Properties, Insights, References
  - Preview URL generation
  - Move/Copy, Versions, Delete

### 📁 Site (`/site`)
**Site-wide configurations organized into:**

- **`site/policies/`**: Component policies
  - Bootstrap CSS framework mappings
  - Form components, CTA, columns, images, lists, search

- **`site/rewrite/`**: URL rewriter configuration
  - DOCTYPE declaration
  - Attribute rewriting rules

- **`site/settings/`**: General site settings
  - Taxonomy root path configuration

- **`site/templates/`**: Page templates
  - Fragment template with title/name fields
  - Policy mappings

- **`site/schemas/`**: Content schemas
  - **Article**: Blog posts/news (title, body, author, publish date, category, tags, etc.)
  - **Product**: E-commerce products (name, SKU, price, stock, category, images)

## Benefits of This Structure

✅ **Better Organization**: Each feature area has its own file/directory
✅ **Easier Maintenance**: Find and edit specific configurations quickly
✅ **Clearer Ownership**: DAM features clearly separated from site features
✅ **Version Control**: Smaller files = better diff tracking in Git
✅ **Reduced Merge Conflicts**: Multiple developers can work on different features
✅ **JCR Compatibility**: Follows standard Sling/JCR content structure patterns

## Migration Notes

This structure replaces the previous single `global.json` file (723 lines) with organized feature-based files:
- Root configuration: `global/.content.json` (6 lines)
- File configs: `files/.content.json` (6 lines)
- DAM configs: `dam/.content.json` + `delivery-presets/` + `transformations/` (~240 lines total)
- Action configs: `actions/pageeditbar/.content.json` (~53 lines)
- Site configs: `site/policies/`, `site/rewrite/`, `site/settings/`, `site/templates/`, `site/schemas/` (~420 lines total)

Total: Same content, 10x better organized! 🎉
