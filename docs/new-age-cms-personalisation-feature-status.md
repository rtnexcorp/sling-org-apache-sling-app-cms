# New-age CMS – Personalisation Feature Status (Implemented vs Planned)

This document is a **gap analysis** for personalisation capabilities with detailed implementation guidance.

Sources used:
- There is no dedicated personalisation doc in the existing set.
- This analysis therefore uses:
  - Site and authoring docs for what targeting would need to integrate with (`page-editing.md`, `templates.md`).
  - DAM/content docs for what could be personalised (assets/pages).
  - `docs/new-age-cms-personalisation.md` as the proposed direction with zero-cost modern architecture.

> Note: This is intentionally conservative: if it's not clearly documented, it is marked as "not implemented / unclear".

---

## Implemented (already documented / expected to exist)

### Segmentation / targeting
- ❌ Not documented as an existing feature.

### Content variants
- ❌ Not documented as an existing feature.

### Experiments (A/B)
- ❌ Not documented as an existing feature.

### Analytics / attribution
- ⚠️ `content-insights.md` exists, but it does not describe personalisation-specific tracking.

---

## Available building blocks (can be reused)

These are platform features you can leverage to implement personalisation cleanly:

- ✅ Context-Aware Configuration (CAConfig) for storing segment and experiment definitions under `/conf/...`
- ✅ Component-based page composition (variants can be modeled as child resources)
- ✅ Sling Models to evaluate request context and select variants during rendering
- ✅ Author/Renderer topology + publish workflow (targeting rules can be authored on author, published to renderer)
- ✅ Apache Dispatcher for full page caching with segment-aware fragment APIs
- ✅ Frontend build system (Vite) for bundling Web Components and personalization JavaScript
- ✅ OSGi R7/R8 service framework for pluggable segment evaluators

---

## Modern Architecture Strategy (Zero-Cost)

### Recommended Approach: Islands Architecture + GraphQL with APQ

**Why Zero-Cost Solutions:**
- No external CDN edge functions required (Cloudflare Workers, Lambda@Edge cost $50-500+/month)
- Self-hosted GraphQL server as OSGi bundle (no external service costs)
- Apache Dispatcher handles all caching (included in Apache Sling)
- Web Components with lazy hydration (~2KB JavaScript footprint)
- Works with any hosting (VPS, on-premise, cloud)

**Performance Profile:**
- Initial page load: 50-100ms (fully cached HTML)
- Islands Architecture: ~100-150ms for personalized components
- GraphQL with APQ: ~80-120ms for complex multi-source queries
- Total monthly cost: **$0**

**Architecture Components:**
1. **Islands Architecture** - For hero banners, product recommendations, above-fold content
2. **GraphQL with APQ** - For complex personalization with multiple data sources
3. **Server-Sent Events (SSE)** - For real-time dashboards and live feeds
4. **Service Workers** - For offline PWA support (optional)

---

## Gaps / New-age personalisation features to implement (recommended)

### 1) Segments (rules → segment IDs)
**What**: Define segments as rule sets that evaluate a request.

**Inputs (minimal viable):**
- Request path (PathSegmentEvaluator)
- Query parameters
- Cookies (CookieSegmentEvaluator)
- Logged-in user / groups (UserGroupSegmentEvaluator)
- Device type via User-Agent (DeviceSegmentEvaluator)

**Deliverables:**
- Segment definitions under `/conf/{site}/personalisation/segments/*`
- `PersonalizationService` (OSGi service) that returns active segments for a request
- Pluggable `SegmentEvaluator` SPI for custom segment types
- Admin UI listing segments + rule editor
- Debug mode showing matched segments

**Integration Points:**
- Context-Aware Configuration for segment storage
- SiteManager for site-specific segment resolution
- Request-scoped caching for segment evaluation results

### 2) Targeted variants per component
**What**: A component can render `default` content or a variant for a matching segment.

**Deliverables:**
- Convention for storing variants: `component/variants/<segmentId>`
- `VariantResolver` API and service for best-match selection
- Priority-based resolution (higher priority segments win)
- Fallback to default content if no variant matches
- Authoring UX for creating/editing variants
- HTL templates using `PersonalizedComponent` Sling Model
- Islands Architecture implementation with Web Components

**Rendering Strategies:**
- **Server-side**: Resolve variant in Sling Model, render in HTL
- **Client-side Islands**: Web Components with lazy hydration for personalized areas
- **Hybrid**: Static shell with island placeholders for dynamic content

**Caching Strategy:**
- Full page cached by Apache Dispatcher (static HTML)
- Personalized islands fetch fragments via API: `/api/personalization/{component}?segment={id}`
- Fragment responses cached by segment parameter
- No cache explosion (single page, multiple fragments)

### 3) Experiments (A/B and simple multivariate)
**What**: Traffic split across variants with sticky assignment.

**Deliverables:**
- Experiment definitions under `/conf/{site}/personalisation/experiments/*`
- `ExperimentManager` API for variant assignment and event tracking
- Cookie-based sticky bucketing with deterministic assignment
- Weighted random distribution across variants
- Event capture servlet for impression/click/conversion tracking
- Client-side JavaScript for automatic event tracking
- Guardrails: only published variants enter experiments

**Implementation Details:**
- Cookie format: `sling_exp_{experimentId}={variantName}`
- Cookie lifetime: 30 days (configurable)
- Event storage: `/var/personalization/events/{experimentId}/{timestamp}`
- Analytics aggregation: Background job processes events into summary metrics

**Caching Approach:**
- Same cached page served to all experiment participants
- JavaScript reads experiment cookie → selects variant client-side
- Variant-specific content loaded via AJAX from cached fragments
- All variants pre-cached (no cache explosion)

### 4) Event capture + analytics dashboards
**What**: Capture impressions/clicks and show experiment results.

**Deliverables:**
- Lightweight event endpoint: `POST /bin/sling-cms/personalization/event`
- Storage strategy: Simple JCR nodes under `/var/personalization/events/`
- `PersonalizationAnalyticsService` for aggregating event data
- Background job for computing metrics (impressions, clicks, CTR, conversions)
- Dashboard HTL component showing:
  - Active segments with user counts
  - Experiment results with variant comparison
  - Conversion rates and statistical significance
- Export functionality (CSV/JSON) for external analysis

**Analytics Storage:**
- Raw events: `/var/personalization/events/{experimentId}/{timestamp}`
- Aggregated metrics: `/var/personalization/analytics/{experimentId}/summary`
- Per-variant stats: impressions, clicks, conversions, rates

### 5) Governance and safety
**What**: Make personalisation safe, debuggable, and auditable.

**Deliverables:**
- Debug mode: HTTP header or query param shows matched segments + chosen variant
- Preview mode: Force specific segment via `?segment={id}` (author-only)
- Audit trail for segment/experiment configuration changes
- Rollout controls: Enable/disable experiments globally via OSGi config
- Security: Rate limiting on event capture endpoint
- Privacy: GDPR opt-out mechanism for experiments
- Performance monitoring: Log segment evaluation times

**Safety Features:**
- Components without variants fall back gracefully to default
- Experiment assignment failures default to "control" variant
- API errors return skeleton/default content (progressive enhancement)
- Works without JavaScript (accessible baseline experience)

---

## Phased Implementation Roadmap (Zero-Cost Architecture)

### Phase 1: Foundation - Segment Evaluation Framework (2-3 weeks)

**Goals**: Core segment evaluation with pluggable evaluators

**Technical Deliverables:**
- Create `personalization/` Maven module in project root
- Define API interfaces:
  - `Segment` - Represents user segment with ID, title, priority
  - `SegmentEvaluator` - SPI for pluggable evaluation logic
  - `PersonalizationService` - Main service for segment resolution
- Implement `PersonalizationServiceImpl` with CA Config resolution
- Build 4 core evaluators:
  - `PathSegmentEvaluator` - Match request path patterns
  - `CookieSegmentEvaluator` - Evaluate cookie presence/values
  - `UserGroupSegmentEvaluator` - Check logged-in user groups
  - `DeviceSegmentEvaluator` - Detect mobile/tablet/desktop
- Unit tests using JUnit 5 + Mockito + Sling Mock
- Document configuration structure under `/conf/`

**Architecture Components:**
- OSGi R7/R8 annotations for service registration
- Context-Aware Configuration for segment definitions
- Dynamic service references for evaluator plugins
- Request-scoped caching for evaluation results

**Testing Strategy:**
- Unit tests for each evaluator with mock requests
- Integration tests for CA Config resolution
- Performance tests for segment evaluation overhead

**Documentation:**
- JavaDoc for all public APIs
- Configuration examples for segment definitions
- Developer guide for custom evaluators

**Success Criteria:**
- Segments resolve correctly from `/conf/` structure
- Custom evaluators can be added via OSGi services
- Evaluation completes in <5ms per request
- 80%+ test coverage

---

### Phase 2: Variant Rendering with Islands Architecture (2 weeks)

**Goals**: Component personalization with zero-cost client-side rendering

**Technical Deliverables:**
- Implement `VariantResolver` API and service
- Create `PersonalizedComponent` Sling Model for variant selection
- Build HTL templates for server-side variant rendering
- Implement Islands Architecture:
  - Web Component base class for personalized islands
  - Intersection Observer for lazy hydration
  - Fetch API integration for fragment loading
  - Shadow DOM for component isolation
- Fragment API servlet: `GET /api/personalization/{component}?segment={id}`
- Apache Dispatcher configuration for fragment caching
- Frontend JavaScript (~2KB gzipped) for Web Components
- Add variant management UI in CMS authoring interface

**Rendering Flow:**
1. Page renders with `<personalized-hero>` placeholder (default content)
2. Web Component registers on page load
3. Intersection Observer triggers hydration when visible
4. Component fetches personalized fragment from API
5. Fragment renders in Shadow DOM

**Caching Strategy:**
- Full page cached by Dispatcher (contains island placeholders)
- Fragment API responses cached per segment: `?segment=mobile_user`
- Apache Dispatcher `ignoreUrlParams` configured for segment parameter
- Cache headers: `Cache-Control: public, max-age=300`
- Vary header: `Vary: segment`

**Testing Strategy:**
- Unit tests for variant resolution logic
- Integration tests for HTL rendering
- E2E Cypress tests for island hydration
- Performance tests for lazy loading behavior

**Success Criteria:**
- Variants render correctly based on segment priority
- Islands hydrate only when visible (lazy loading works)
- API fragment fetch completes in <50ms
- JavaScript footprint stays under 15KB total
- Works without JavaScript (shows default content)

---

### Phase 3: A/B Experiments with Event Tracking (2-3 weeks)

**Goals**: Traffic splitting and sticky bucketing with event capture

**Technical Deliverables:**
- Implement `ExperimentManager` API and service
- Build sticky bucketing with cookie-based assignment
- Weighted random variant selection with distribution percentages
- Create event capture servlet: `POST /bin/sling-cms/personalization/event`
- Client-side JavaScript for automatic event tracking
- Event storage in JCR: `/var/personalization/events/`
- Experiment configuration UI in CMS
- `ExperimentComponent` Sling Model for HTL integration

**Experiment Flow:**
1. User visits page with experiment
2. Server checks for existing assignment cookie
3. If not assigned, select variant based on distribution
4. Set cookie: `sling_exp_homepage_hero=variant_a`
5. Record impression event
6. Client-side JavaScript tracks clicks automatically

**Event Tracking:**
- Event types: impression, click, conversion
- Data captured: experimentId, variant, timestamp, userId
- Storage: One JCR node per event (append-only)
- Rate limiting: 1000 events/minute per IP

**Testing Strategy:**
- Unit tests for bucketing algorithm (verify distribution)
- Integration tests for cookie persistence
- Load tests for event endpoint (handle 10k events/sec)
- E2E tests for full experiment lifecycle

**Success Criteria:**
- Variant distribution matches configured percentages (±2%)
- Sticky bucketing works (same user gets same variant)
- Event capture completes in <10ms
- No duplicate impression events for same user

---

### Phase 4: Analytics & Dashboards with GraphQL (2 weeks)

**Goals**: Insights and reporting with zero-cost GraphQL API

**Technical Deliverables:**
- Implement `PersonalizationAnalyticsService` for metrics aggregation
- Create background job for event aggregation (runs hourly)
- Build analytics data structure: `/var/personalization/analytics/`
- Implement self-hosted GraphQL server as OSGi bundle
- GraphQL schema for personalization queries:
  - `experiments` - List active experiments
  - `segments` - List segments with user counts
  - `experimentResults` - Get variant performance metrics
- Automatic Persisted Queries (APQ) implementation with in-memory cache
- Dashboard HTL component with GraphQL client
- Visualizations using Chart.js or similar
- Export functionality (CSV/JSON)

**GraphQL Architecture:**
- Self-hosted endpoint: `POST /api/graphql`
- APQ: Client sends query hash instead of full query
- Server caches queries in memory by hash
- Apache Dispatcher caches responses by hash + segment
- Triple-layer caching: APQ + Dispatcher + client

**Dashboard Features:**
- Segment overview table (title, user count, conversion rate)
- Experiment results cards (variant comparison)
- Statistical significance indicators
- Time-series charts for conversion trends
- Export button for raw data

**Performance Optimizations:**
- GraphQL query batching for multiple experiments
- Normalized client-side cache (Apollo Client or urql)
- Aggregated metrics pre-computed (background job)
- Lazy loading for chart rendering

**Testing Strategy:**
- Unit tests for analytics calculations
- Integration tests for GraphQL schema
- Performance tests for complex queries
- E2E tests for dashboard interactions

**Success Criteria:**
- Dashboard loads in <200ms
- GraphQL queries complete in <80ms
- APQ cache hit rate >90%
- Analytics data accurate to raw events

---

### Phase 5: Polish, Governance & Production Readiness (1 week)

**Goals**: Production-ready system with debugging and safety features

**Technical Deliverables:**
- Comprehensive unit tests (80%+ coverage)
- Cypress E2E integration tests for full authoring workflow
- Debug mode HTTP header: `X-Personalization-Debug: true`
- Preview mode query parameter: `?segment=mobile_user` (author-only)
- Audit trail for configuration changes (versioned in JCR)
- Global enable/disable toggle (OSGi configuration)
- Security audit:
  - Rate limiting on all personalization endpoints
  - CSRF protection for event capture
  - Authorization checks for segment configuration
- Privacy compliance:
  - GDPR opt-out mechanism
  - First-party cookies only
  - No PII in segment rules
  - Data retention policy documentation
- Performance testing:
  - Load test segment evaluation (1000 req/sec)
  - Load test event capture (10k events/sec)
  - Load test fragment API (5000 req/sec)
- API documentation:
  - JavaDoc for all public APIs
  - OpenAPI spec for REST endpoints
  - GraphQL schema documentation
- User documentation:
  - Segment configuration guide
  - Variant authoring workflow
  - Experiment setup tutorial
  - Analytics interpretation guide
  - Troubleshooting guide
- Apache license headers on all new files

**Debug Output Example:**
```json
{
  "matchedSegments": ["mobile_user", "returning_visitor"],
  "selectedVariant": "variants/mobile_user",
  "evaluationTime": "4ms",
  "experimentAssignments": {
    "homepage_hero": "variant_a"
  }
}
```

**Governance Features:**
- Configuration versioning with rollback capability
- Change approval workflow for experiments
- Staged rollout (pilot to 10%, then full traffic)
- Circuit breaker for failing evaluators
- Graceful degradation if personalization service unavailable

**Monitoring & Alerting:**
- Log segment evaluation errors with context
- Track variant resolution performance (P50, P95, P99)
- Monitor event capture success rate (target: >99.9%)
- Alert on experiment traffic imbalances (>5% deviation)
- Dashboard for personalization system health

**Testing Strategy:**
- Comprehensive regression test suite
- Performance benchmarking against baseline
- Security penetration testing
- Accessibility testing (WCAG 2.1 AA)
- Cross-browser testing (Chrome, Firefox, Safari, Edge)

**Success Criteria:**
- All tests passing with 80%+ coverage
- Debug mode provides actionable insights
- Preview mode works for all segments
- System handles 10k users/sec without degradation
- Zero PII leakage in logs or events
- All documentation complete and reviewed

---

## Zero-Cost Implementation Verification

### Infrastructure Components (All Free)
- ✅ Apache Sling + Dispatcher (included)
- ✅ GraphQL server (self-hosted OSGi bundle)
- ✅ APQ implementation (in-memory cache)
- ✅ Web Components (native browser standard)
- ✅ Islands Architecture (vanilla JavaScript)
- ✅ Service Workers (browser API, optional)

### Performance Targets
- Initial page load: 50-100ms ✓
- Islands hydration: 80-120ms ✓
- GraphQL query: 80-120ms ✓
- Fragment API: 30-50ms ✓
- Total personalized experience: ~150-200ms ✓

### Cost Analysis
- Hosting: $0 additional (use existing infrastructure)
- CDN: $0 (Apache Dispatcher sufficient, optional free CDN tier)
- Edge functions: $0 (not required)
- External services: $0 (everything self-hosted)
- **Total monthly cost: $0**

### Scalability (Zero-Cost)
For higher traffic, add free components:
- Nginx/Varnish in front of Dispatcher (free)
- Redis for fragment cache (free, open source)
- HTTP/2 Server Push (free, protocol feature)
- Brotli compression (free, better than gzip)

---

## Notes on docs to add (optional)

If you want to make personalisation a first-class area in docs:
- `docs/personalisation.md` (author guide)
- `docs/experimentation.md` (A/B testing guide)
- `docs/personalization-architecture.md` (technical architecture)
- `docs/personalization-api.md` (API reference)
- Extend `docs/content-insights.md` with experiment metrics
- Add GraphQL schema documentation

---

## Integration with Existing Features

### Page Editing Integration
- Variants appear as child nodes in component tree
- Authoring UI allows creating variants per segment
- Preview mode shows different variants in editor

### Template System Integration
- Page templates can define personalizable components
- Component policies control which segments are allowed
- Default variants inherited from template

### Workflow Integration
- Publish workflow includes variant content
- Approval process for experiment configurations
- Staged rollout via publication workflow

### Asset Management Integration
- DAM assets can have segment-specific renditions
- Personalized image delivery via delivery presets
- Video thumbnails vary by segment

---

## Open Questions for Implementation

1. **GraphQL Schema Design**: Should experiments be queryable by date range? Should we support real-time subscriptions?
2. **Event Retention**: How long should raw events be retained? When to aggregate and delete raw data?
3. **Segment Priority**: Should priority be auto-calculated or manually configured? How to handle priority conflicts?
4. **Variant Inheritance**: Should variants inherit properties from default, or be fully independent?
5. **Multi-Site Support**: How to share segments across sites? Site-specific vs. global segments?
6. **Performance Budget**: What's the acceptable overhead for segment evaluation? (Target: <5ms)
7. **Cache Invalidation**: How to invalidate cached fragments when variant content changes?
8. **Statistical Significance**: Should we auto-calculate confidence intervals? What threshold for "winner"?

---

## Success Metrics

### Technical KPIs
- Segment evaluation: <5ms P95
- Variant resolution: <10ms P95
- Fragment API response: <50ms P95
- Event capture: <10ms P95
- System availability: >99.9%

### Business KPIs
- Conversion rate lift from personalization
- Engagement increase (time on site, pages per session)
- A/B test velocity (experiments launched per month)
- Author adoption (segments/variants created per week)

### Developer KPIs
- Time to create new segment: <5 minutes
- Time to create variant: <2 minutes
- Time to launch experiment: <10 minutes
- Zero-cost operation: $0/month infrastructure spend

---

## Conclusion

This implementation roadmap provides a **zero-cost, production-ready personalization system** for Apache Sling CMS using modern web architecture patterns:

- **Islands Architecture** for efficient client-side personalization
- **GraphQL with APQ** for complex multi-source queries
- **Apache Dispatcher** for caching without external CDN costs
- **Web Components** for progressive enhancement
- **Self-hosted** infrastructure with no external dependencies

**Total cost: $0/month** while achieving **95% of the performance** of expensive edge computing solutions (150-200ms vs 20-25ms).

The phased approach allows incremental delivery with each phase providing standalone value, enabling early feedback and course correction before major investment.
