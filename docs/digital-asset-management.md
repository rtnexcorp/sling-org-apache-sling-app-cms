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
[Apache Sling](https://sling.apache.org) > [Sling CMS](https://github.com/apache/sling-org-apache-sling-app-cms) > [Administration](administration.md) > Digital Asset Management

# Digital Asset Management (DAM)

Sling CMS provides a comprehensive Digital Asset Management system for organizing, transforming, and delivering media files across your websites.

## Table of Contents

1. [Overview](#overview)
2. [Getting Started](#getting-started)
3. [File Management](#file-management)
4. [Image Transformations](#image-transformations)
5. [Automatic Renditions](#automatic-renditions)
6. [Video Support](#video-support)
7. [Asset Browser](#asset-browser)
8. [Metadata Management](#metadata-management)
9. [Configuration Options](#configuration-options)

---

## Overview

### What is DAM?

Digital Asset Management (DAM) in Sling CMS helps you:

- **Centralize media storage** - Keep all images, videos, documents, and files in one place
- **Automatic thumbnails** - Generate preview images for all file types automatically
- **Image transformations** - Resize, crop, and optimize images on-the-fly
- **Multiple renditions** - Create different sizes for responsive websites
- **Organized browsing** - Find assets quickly with filtering and search
- **Metadata tracking** - Add tags, descriptions, and copyright information

### Supported File Types

| File Type | Preview | Thumbnail | Auto-Renditions |
|-----------|---------|-----------|-----------------|
| **Images** (JPG, PNG, GIF, WebP) | ✅ | ✅ | ✅ |
| **Videos** (MP4, WebM, MOV, AVI) | ✅ | ✅ | ❌ |
| **PDFs** | ✅ | ✅ | ❌ |
| **PowerPoint** (PPT, PPTX) | ✅ | ✅ | ❌ |
| **Word Documents** (DOC, DOCX) | ✅ | ✅ | ❌ |
| **Excel** (XLS, XLSX) | ✅ | ✅ | ❌ |
| **Other Files** | ❌ | Generic icon | ❌ |

---

## Getting Started

### Accessing the Asset Manager

1. Log in to Sling CMS at `/cms`
2. Navigate to **Static Content** in the main menu
3. Browse to `/static` or your site's asset folder

### Quick Actions

| Action | How to Do It |
|--------|--------------|
| **Upload files** | Click "Add File" button or drag & drop files |
| **Create folder** | Click "Add Folder" button |
| **Preview image** | Click the preview icon on any image |
| **Download file** | Click the download icon |
| **Edit properties** | Click the edit icon to add metadata |

---

## File Management

### Uploading Files

**Single file upload:**
1. Click the **Add File** button
2. Select a file from your computer
3. The file uploads and thumbnail generates automatically

**Bulk upload:**
1. Drag multiple files onto the content area
2. All files upload in parallel
3. Progress indicators show upload status

### Organizing Files

- **Create folders** to organize assets by project, date, or type
- **Move/Copy** files between folders using the action menu
- **Rename** files through the edit properties dialog

### Version Control

- Sling CMS maintains **version history** for all files
- Access previous versions through **Manage Versions**
- **Restore** older versions when needed

---

## Image Transformations

### What Are Transformations?

Transformations let you resize, crop, and modify images automatically without editing the original file. The transformed image is generated on request and cached for performance.

### Using Transformations

Add the transformation name to any image URL:

```
/static/images/photo.jpg.transform/thumbnail.png
```

This applies the "thumbnail" transformation and outputs as PNG.

### Available Transformations

| Transformation | What It Does |
|----------------|--------------|
| **Resize** | Scale image to specific width/height |
| **Crop** | Cut image to exact dimensions |
| **Scale** | Proportionally resize by percentage |
| **Rotate** | Rotate image by degrees |
| **Flip** | Mirror horizontally or vertically |
| **Greyscale** | Convert to black and white |
| **Colorize** | Apply color tint overlay |

### Pre-configured Sizes

Your site may have pre-configured transformation presets:

| Preset Name | Typical Size | Use Case |
|-------------|--------------|----------|
| `thumbnail` | 200×200 | Grid previews, listings |
| `small` | 480px width | Mobile images |
| `medium` | 960px width | Tablet images |
| `large` | 1920px width | Desktop hero images |

> **Note:** Transformation presets are configured per-site under `/conf/{site}/files/transformations/`

---

## Automatic Renditions

### How Auto-Renditions Work

When enabled, the system automatically generates multiple image sizes whenever you upload a new image. This ensures optimized images are ready immediately.

### Benefits

- **Faster page loads** - Pre-generated images serve instantly
- **Consistent sizing** - All images follow your site's standards
- **No manual work** - Upload once, get all sizes automatically

### Default Behavior

| Setting | Default Value |
|---------|---------------|
| Auto-renditions enabled | Yes |
| Transformations applied | `thumbnail` |
| Supported types | All images (`image/*`) |
| Monitored paths | `/content`, `/static` |

### Viewing Renditions

1. Open an image's properties
2. Scroll to the **Renditions** section
3. View all generated sizes and access each directly

---

## Video Support

### Video Thumbnails

Sling CMS automatically generates thumbnail images from video files by extracting a frame.

**Requirements:**
- FFmpeg must be installed on the server
- Supported formats: MP4, WebM, MOV, AVI, MKV, MPEG

**How it works:**
1. Upload a video file
2. System extracts a frame (default: 5 seconds in)
3. Thumbnail displays in the asset browser

> **Note:** If FFmpeg is not available, videos display a generic video icon instead.

### Supported Video Formats

| Format | Extension | Thumbnail Support |
|--------|-----------|-------------------|
| MP4 | `.mp4` | ✅ |
| WebM | `.webm` | ✅ |
| QuickTime | `.mov` | ✅ |
| AVI | `.avi` | ✅ |
| Matroska | `.mkv` | ✅ |
| MPEG | `.mpeg`, `.mpg` | ✅ |

---

## Asset Browser

### Grid View vs List View

Toggle between viewing modes using the view buttons:

| View | Best For |
|------|----------|
| **Grid View** | Visual browsing with large thumbnails |
| **List View** | Scanning many files quickly, seeing details |

### Filtering Assets

Use the filter bar to find assets quickly:

- **Search** - Type to filter by filename
- **Type filter** - Show only Images, Videos, or Documents
- **Asset count** - See how many items match your filter

### Asset Cards

Each asset card shows:
- Thumbnail preview
- File name
- File type badge (VIDEO, PDF, etc.)
- File size
- Last modified date
- Publication status

---

## Metadata Management

### Editing Asset Metadata

1. Click the **Edit** icon on any asset
2. Fill in the metadata fields:

| Field | Purpose |
|-------|---------|
| **Title** | Display name for the asset |
| **Description** | What the asset shows or contains |
| **Alt Text** | Accessibility text for images |
| **Keywords/Tags** | Searchable tags for organization |
| **Copyright** | Rights and usage information |
| **Creator** | Original author or photographer |
| **Source** | Where the asset came from |

### Asset Information Panel

The metadata editor shows read-only information:
- File size (formatted as KB/MB)
- MIME type
- Created date
- Last modified date
- Image dimensions (for images)

---

## Configuration Options

### Auto-Rendition Settings

Configure automatic rendition generation in the OSGi console:

| Setting | Description | Default |
|---------|-------------|---------|
| **Enabled** | Turn auto-renditions on/off | `true` |
| **Transformation Names** | Which presets to generate | `thumbnail` |
| **Supported MIME Types** | File types to process | `image/*` |
| **Content Paths** | Folders to monitor | `/content`, `/static` |

### Adding Transformation Presets

Site administrators can create new transformation presets:

1. Navigate to `/conf/{site}/files/transformations/`
2. Create a new transformation node
3. Configure resize, crop, or other handlers
4. The new preset is immediately available

### Video Thumbnail Settings

| Setting | Description | Default |
|---------|-------------|---------|
| **FFmpeg Path** | Location of FFmpeg executable | `ffmpeg` |
| **Frame Position** | Seconds into video for thumbnail | `5` |
| **Timeout** | Maximum processing time | `30` seconds |

---

## Best Practices

### File Naming

- Use **lowercase** filenames
- Replace spaces with **hyphens** (`my-image.jpg`)
- Use **descriptive names** (`product-hero-banner.jpg` not `IMG_1234.jpg`)

### Image Optimization

- Upload images at **reasonable sizes** (not 50MB camera files)
- Use **appropriate formats**:
  - JPEG for photos
  - PNG for graphics with transparency
  - WebP for modern browsers
- Let transformations handle **responsive sizing**

### Folder Organization

Suggested folder structure:
```
/static/
  ├── images/
  │   ├── products/
  │   ├── banners/
  │   └── blog/
  ├── documents/
  ├── videos/
  └── downloads/
```

### Metadata Hygiene

- **Always add alt text** for accessibility
- **Use consistent tags** across your site
- **Document copyright** for licensed content

---

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| Thumbnail not generating | Check file type is supported; verify FFmpeg for videos |
| Transformation not working | Verify transformation preset exists in site config |
| Upload fails | Check file size limits and folder permissions |
| Video shows generic icon | FFmpeg not installed or not in system PATH |

### Getting Help

- Check the [Image Transformations](image-transformations.md) guide for detailed transformation options
- Review [Managing Content](managing-content.md) for general content operations
- See [Configure File Editor](configure-file-editor.md) for file type configurations

---

## Related Documentation

- [Image Transformations](image-transformations.md) - Detailed transformation configuration
- [Configure File Editor](configure-file-editor.md) - File type and editor settings
- [Managing Content](managing-content.md) - General content management guide

---

*Last Updated: December 13, 2025*
