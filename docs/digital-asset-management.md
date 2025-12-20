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
3. [System Requirements & Setup](#system-requirements--setup)
4. [File Management](#file-management)
5. [Image Transformations](#image-transformations)
6. [Automatic Renditions](#automatic-renditions)
7. [Video Support](#video-support)
8. [Asset Browser](#asset-browser)
9. [Metadata Management](#metadata-management)
10. [Metadata Extraction & Enrichment](#metadata-extraction--enrichment)
11. [Configuration Options](#configuration-options)
12. [Best Practices](#best-practices)

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
- **Automatic metadata extraction** - Extract EXIF, IPTC, XMP data from images
- **OCR text extraction** - Extract text from images and PDFs for searchability

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

> **Note:** Transformation presets are configured globally under `/conf/global/dam/transformations/`. For backward compatibility, transformations under `/conf/{site}/files/transformations/` are also supported.

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

### Installing FFmpeg

FFmpeg is required for video thumbnail generation. Install it on your server:

**macOS:**
```bash
# Using Homebrew
brew install ffmpeg

# Verify installation
ffmpeg -version
```

**Ubuntu/Debian:**
```bash
# Install FFmpeg
sudo apt-get update
sudo apt-get install ffmpeg

# Verify installation
ffmpeg -version
```

**RHEL/CentOS/Fedora:**
```bash
# Enable EPEL repository (CentOS/RHEL)
sudo yum install epel-release

# Install FFmpeg
sudo yum install ffmpeg

# Or on Fedora
sudo dnf install ffmpeg

# Verify installation
ffmpeg -version
```

**Windows:**
```
1. Download FFmpeg from: https://ffmpeg.org/download.html
2. Extract to C:\ffmpeg
3. Add C:\ffmpeg\bin to system PATH environment variable
4. Restart command prompt and verify: ffmpeg -version
```

**Docker/Container Deployment:**
```dockerfile
# Add to your Dockerfile
RUN apt-get update && apt-get install -y ffmpeg

# Or for Alpine-based images
RUN apk add --no-cache ffmpeg
```

**Verifying FFmpeg is Working:**

After installation, Sling CMS should automatically detect FFmpeg. To verify:
1. Upload a video file to `/static/videos/`
2. Check if a thumbnail appears in the asset browser
3. If not, check Sling CMS logs for FFmpeg-related errors

**Custom FFmpeg Path:**

If FFmpeg is installed in a non-standard location, configure the path via OSGi console:
- Navigate to: `http://localhost:8082/system/console/configMgr`
- Find: "Video Thumbnail Configuration" (or similar)
- Set `ffmpegPath` to your custom location (e.g., `/usr/local/bin/ffmpeg`)
- Save configuration

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

### Automatic Metadata Extraction

When you upload files, Sling CMS automatically extracts technical metadata:

**For Images:**
- **EXIF data** - Camera settings, exposure, ISO, aperture
- **GPS coordinates** - Location where photo was taken
- **IPTC/XMP** - Copyright, keywords, creator information
- **Technical details** - Dimensions, color space, DPI

**For All Files:**
- **SHA256 checksum** - File integrity verification
- **MIME type** - Detected file type
- **Document properties** - Author, title, creation date (for PDFs, Office docs)

All extracted metadata is stored under `jcr:content/metadata/` and can be queried for search and filtering.

---

## Metadata Extraction & Enrichment

### Overview

Sling CMS uses a **pluggable metadata enricher architecture** that automatically extracts and enriches metadata when files are uploaded. Multiple enrichers can run in sequence, each adding different types of metadata.

### Built-in Enrichers

#### 1. Tika Metadata Enricher

**What it does:**
- Extracts EXIF, IPTC, XMP metadata from images
- Parses document properties from PDFs, Word, Excel files
- Detects MIME types and file formats
- Extracts video codec and duration information

**Metadata examples:**
- `tiff:ImageWidth`, `tiff:ImageLength` - Image dimensions
- `EXIF:DateTimeOriginal` - When photo was taken
- `geo:lat`, `geo:long` - GPS coordinates
- `tiff:Make`, `tiff:Model` - Camera information
- `dc:title`, `dc:creator` - Document metadata

**Configuration:**
- Enabled by default
- Priority: 100 (runs first)
- Configure via OSGi console: "Tika Metadata Enricher Configuration"

#### 2. OCR Text Extraction Enricher (NEW)

**What it does:**
- Extracts text content from images (scanned documents, screenshots)
- Extracts text from PDFs (including scanned PDFs)
- Makes image content searchable
- Stores extracted text in `ocr:text` metadata property

**Use cases:**
- Make scanned documents searchable
- Extract text from product images
- Index screenshots and diagrams
- Build searchable image archives

**Requirements:**
- **Optional**: Tesseract OCR engine for true OCR (see installation below)
- Uses Apache Tika text extraction by default

**Configuration:**
```
OSGi Console: "OCR Metadata Enricher Configuration"

enabled = false (disabled by default - opt-in)
priority = 50
supportedMimeTypes = ["image/png", "image/jpeg", "application/pdf"]
language = "eng" (or "fra", "deu", "eng+fra" for multi-language)
tesseractPath = "" (leave empty to use system PATH)
timeout = 120 (seconds)
maxTextLength = 10000 (characters)
```

**Installing Tesseract OCR** (optional for enhanced OCR):

Tesseract provides true OCR capabilities for extracting text from images. Without it, Sling CMS uses basic Tika text extraction.

**macOS:**
```bash
# Using Homebrew
brew install tesseract

# Install language packs (install languages you need)
brew install tesseract-lang

# Or install specific languages
brew install tesseract-lang-eng  # English
brew install tesseract-lang-fra  # French
brew install tesseract-lang-deu  # German
brew install tesseract-lang-spa  # Spanish

# Verify installation
tesseract --version
tesseract --list-langs
```

**Ubuntu/Debian:**
```bash
# Install Tesseract base
sudo apt-get update
sudo apt-get install tesseract-ocr

# Install language packs
sudo apt-get install tesseract-ocr-eng  # English
sudo apt-get install tesseract-ocr-fra  # French
sudo apt-get install tesseract-ocr-deu  # German
sudo apt-get install tesseract-ocr-spa  # Spanish

# Or install all languages (large download)
sudo apt-get install tesseract-ocr-all

# Verify installation
tesseract --version
tesseract --list-langs
```

**RHEL/CentOS/Fedora:**
```bash
# Enable EPEL repository (CentOS/RHEL)
sudo yum install epel-release

# Install Tesseract
sudo yum install tesseract

# Install language packs
sudo yum install tesseract-langpack-eng  # English
sudo yum install tesseract-langpack-fra  # French
sudo yum install tesseract-langpack-deu  # German
sudo yum install tesseract-langpack-spa  # Spanish

# On Fedora, use dnf instead of yum
sudo dnf install tesseract tesseract-langpack-eng

# Verify installation
tesseract --version
tesseract --list-langs
```

**Windows:**
```
1. Download installer from: https://github.com/UB-Mannheim/tesseract/wiki
2. Run the installer (tesseract-ocr-w64-setup-*.exe)
3. During installation, select language packs you need
4. Default install path: C:\Program Files\Tesseract-OCR
5. Add to system PATH: C:\Program Files\Tesseract-OCR
6. Restart command prompt
7. Verify: tesseract --version
```

**Docker/Container Deployment:**
```dockerfile
# Add to your Dockerfile

# Debian/Ubuntu-based images
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    tesseract-ocr-fra \
    tesseract-ocr-deu

# Alpine-based images
RUN apk add --no-cache \
    tesseract-ocr \
    tesseract-ocr-data-eng \
    tesseract-ocr-data-fra \
    tesseract-ocr-data-deu
```

**Available Language Codes:**

Common language codes for the `language` configuration:
- `eng` - English
- `fra` - French
- `deu` - German
- `spa` - Spanish
- `ita` - Italian
- `por` - Portuguese
- `rus` - Russian
- `jpn` - Japanese
- `chi_sim` - Chinese Simplified
- `chi_tra` - Chinese Traditional
- `ara` - Arabic
- `hin` - Hindi

**Multi-language OCR:**
```
# Configure multiple languages in OSGi console
language = "eng+fra"  # English and French
language = "eng+deu+fra"  # English, German, and French
```

**Verifying Tesseract Installation:**

Test Tesseract from command line:
```bash
# Test OCR on a sample image
tesseract test-image.png output -l eng

# Check output.txt for extracted text
cat output.txt

# List installed languages
tesseract --list-langs
```

**Custom Tesseract Path:**

If Tesseract is installed in a non-standard location:
1. Navigate to: `http://localhost:8082/system/console/configMgr`
2. Find: "OCR Metadata Enricher Configuration"
3. Set `tesseractPath` (e.g., `/usr/local/bin/tesseract` or `C:\Program Files\Tesseract-OCR\tesseract.exe`)
4. Save configuration

**Troubleshooting Tesseract:**

| Issue | Solution |
|-------|----------|
| "tesseract: command not found" | Add Tesseract to system PATH |
| "Language not found" | Install required language pack |
| OCR returns garbled text | Try different language setting or check image quality |
| Slow OCR processing | Reduce timeout or process smaller images |
| High memory usage | Limit concurrent OCR operations or reduce maxTextLength |

**Enabling OCR:**
1. Navigate to: `http://localhost:8082/system/console/configMgr`
2. Find: "OCR Metadata Enricher Configuration"
3. Set `enabled = true`
4. Configure language and supported MIME types
5. Save configuration

**Accessing extracted text:**
- Via JCR: `/content/dam/image.jpg/jcr:content/metadata/ocr:text`
- Via metadata editor: View "OCR Text" field
- Via search: Query for text content across all images

### Custom Metadata Enrichers

Developers can create custom enrichers for specialized metadata extraction:

**Examples:**
- **Face detection** - Identify people in photos
- **Color analysis** - Extract dominant colors and palettes
- **AI tagging** - Auto-tag images using ML models
- **Duplicate detection** - Find similar images via perceptual hashing

**Creating a custom enricher:**

```java
@Component(service = FileMetadataEnricher.class)
public class CustomEnricher implements FileMetadataEnricher {

    @Override
    public String getName() {
        return "my-enricher";
    }

    @Override
    public boolean shouldEnrich(File file) {
        // Filter by MIME type
        return file.getResource().getValueMap()
            .get("jcr:mimeType", "").startsWith("image/");
    }

    @Override
    public void enrichMetadata(File file, Map<String, Object> metadata) {
        // Extract and add custom metadata
        metadata.put("custom:property", "value");
    }

    @Override
    public int getPriority() {
        return 75; // Runs between Tika (100) and OCR (50)
    }
}
```

### Enricher Execution Order

Enrichers run in **priority order (highest to lowest)**:

1. **Priority 100** - Tika Metadata Enricher (EXIF/IPTC/XMP)
2. **Priority 75** - Your custom enrichers
3. **Priority 50** - OCR Text Extraction
4. **Always last** - SHA256 checksum generation

### Metadata Storage Location

All extracted metadata is stored at: `jcr:content/metadata/*`

**Example structure:**
```
/static/images/photo.jpg
└── jcr:content/
    └── metadata/
        ├── SHA256 = "abc123..."
        ├── tiff:ImageWidth = 3000
        ├── tiff:ImageLength = 2000
        ├── EXIF:DateTimeOriginal = "2025-01-15T10:30:00"
        ├── geo:lat = 37.7749
        ├── geo:long = -122.4194
        ├── tiff:Make = "Canon"
        ├── tiff:Model = "EOS R5"
        ├── ocr:text = "Extracted text content..." (if OCR enabled)
        └── ... (100+ possible properties)
```

### Troubleshooting Metadata Extraction

| Issue | Solution |
|-------|----------|
| No metadata extracted | Check if enrichers are enabled in OSGi console |
| OCR not working | Verify `enabled = true` and Tesseract is installed |
| Missing EXIF data | File may not contain EXIF (e.g., web-optimized images) |
| OCR text incorrect | Try different `language` setting for non-English text |
| Slow metadata extraction | Disable OCR or reduce timeout for large files |

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

1. Navigate to `/conf/global/dam/transformations/`
2. Create a new transformation node
3. Configure resize, crop, or other handlers
4. The new preset is immediately available

### Video Thumbnail Settings

| Setting | Description | Default |
|---------|-------------|---------|
| **FFmpeg Path** | Location of FFmpeg executable | `ffmpeg` |
| **Frame Position** | Seconds into video for thumbnail | `5` |
| **Timeout** | Maximum processing time | `30` seconds |

### Metadata Enricher Settings

**Tika Metadata Enricher:**

| Setting | Description | Default |
|---------|-------------|---------|
| **Enabled** | Extract EXIF/IPTC/XMP metadata | `true` |
| **Priority** | Execution order (higher runs first) | `100` |

**OCR Text Extraction Enricher:**

| Setting | Description | Default |
|---------|-------------|---------|
| **Enabled** | Extract text from images/PDFs | `false` (opt-in) |
| **Priority** | Execution order | `50` |
| **Supported MIME Types** | File types to process | `image/png`, `image/jpeg`, etc. |
| **Language** | OCR language code | `eng` |
| **Tesseract Path** | Path to tesseract binary | (system PATH) |
| **Timeout** | Max processing time per file | `120` seconds |
| **Max Text Length** | Maximum characters to store | `10000` |

**Configure via OSGi Console:**
- URL: `http://localhost:8082/system/console/configMgr`
- Search for: "Metadata Enricher"

---

## System Requirements & Setup

### Required Software

For full DAM functionality, install these components on your server:

**Java Runtime:**
- Java 21 or higher (required for Sling CMS)

**Optional but Recommended:**
- **FFmpeg** - For video thumbnail generation
- **Tesseract OCR** - For text extraction from images and PDFs

### Installation Checklist

Before deploying to production:

- [ ] Java 21+ installed and configured
- [ ] Sling CMS deployed and running
- [ ] FFmpeg installed (verify with `ffmpeg -version`)
- [ ] Tesseract installed if using OCR (verify with `tesseract --version`)
- [ ] Required Tesseract language packs installed
- [ ] FFmpeg and Tesseract in system PATH or paths configured in OSGi
- [ ] Sufficient disk space for uploaded assets and renditions
- [ ] Adequate memory for image/video processing (recommend 4GB+ heap)

### Performance Considerations

**For High-Volume Sites:**
- Increase JVM heap size for image processing
- Consider dedicated worker nodes for thumbnail generation
- Enable caching for transformed images
- Limit concurrent OCR operations to prevent memory issues
- Set reasonable timeouts for FFmpeg and Tesseract

**Resource Usage:**
- **Image transformation:** ~50-200MB memory per operation
- **Video thumbnail:** ~100-500MB memory depending on video size
- **OCR processing:** ~100-300MB memory per operation
- **Disk space:** Plan for 20-50% overhead for renditions and thumbnails

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
- **Enable OCR** for scanned documents to make them searchable
- **Review extracted metadata** and supplement with manual tags

### Searchable Content

With metadata extraction and OCR enabled:
- **Image EXIF data** is automatically searchable
- **GPS coordinates** enable location-based queries
- **OCR text** makes image content discoverable
- **Document metadata** helps organize files

---

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| Thumbnail not generating | Check file type is supported; verify FFmpeg for videos |
| Transformation not working | Verify transformation preset exists in site config |
| Upload fails | Check file size limits and folder permissions |
| Video shows generic icon | FFmpeg not installed or not in system PATH |
| No metadata extracted | Verify enrichers are enabled in OSGi console |
| OCR not working | Check `enabled = true` and Tesseract installation |
| OCR text garbled | Try different language setting or check image quality |

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

*Last Updated: December 20, 2025*
