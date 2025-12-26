# Apache Sling CMS - Asset Management Module

This module provides Digital Asset Management (DAM) capabilities for Apache Sling CMS.

## Purpose

The Asset module consolidates all asset and DAM-related functionality, including:

- Asset metadata extraction and enrichment
- Thumbnail generation and image transformations
- Video frame extraction and processing
- Rendition management
- File type detection
- Asset delivery and optimization

## Migration from Thumbnails Module

This module is designed to receive code from the `thumbnails` module as part of the asset management consolidation effort. The migration will be done systematically to ensure:

1. All asset-related services are centralized
2. Clear separation of concerns
3. Proper OSGi service registration
4. Backward compatibility during transition

## Structure

```
asset/
├── src/main/java/org/apache/sling/cms/asset/
│   ├── models/              # Public Sling Models for assets
│   └── internal/            # Internal implementation classes
│       └── models/          # Internal Sling Models
└── src/test/java/org/apache/sling/cms/asset/
    └── ...                  # Unit tests
```

## Key Interfaces (To Be Migrated)

Future interfaces to be moved from thumbnails module:
- Asset delivery and format resolution
- Thumbnail generation
- Image transformation
- Video processing
- Metadata enrichment

## Development Guidelines

Follow the Apache Sling CMS coding standards:
- Use OSGi R7+ annotations (@Component, @Reference, @Activate)
- Prefer OSGi-compliant libraries
- Use Sling Models for resource adaptation
- Follow existing code patterns from core module
- Write unit tests using JUnit 5 + Mockito + Sling Mock

## Dependencies

- Apache Sling API
- Apache Tika (file type detection, metadata)
- Apache Commons (IO, Lang3)
- JCR API
- OSGi Declarative Services

## Future Work

1. Migrate thumbnail generation from thumbnails module
2. Migrate delivery format resolution
3. Migrate image transformation services
4. Migrate video processing capabilities
5. Consolidate metadata enrichers
6. Create unified asset API
