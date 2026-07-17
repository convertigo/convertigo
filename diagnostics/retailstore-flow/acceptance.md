# Acceptance criteria

## Server and provisioning

- The FullSync connector and all required DBOs are created through one reviewed
  structured scaffold request.
- Applying the same scaffold request twice creates no duplicate DBO and the
  second result reports only reused or unchanged objects.
- Seed initialization is idempotent and reports category/product counts.
- The database can answer hierarchy queries and single-document reads without
  requiring the frontend to know server implementation details.
- No direct CouchDB request, generated YAML edit or legacy MCP call occurs.

## Client and binding

- Replication uses `FullSyncSync` in one-shot pull mode with progress observable
  through generated runtime state. Continuous bidirectional sync is outside the
  read-only client contract.
- Hierarchy reads use `FullSyncView`; detail reads use `FullSyncGet`.
- Action parameters are literals or structured source bindings. A selected item
  can feed a later FullSync action without string interpolation.
- FullSync results appear as `category: fullsync` picker sources with stable
  action identity and operation.
- Domain fields bound in the UI come from a learned requestable schema when
  available. Generic CouchDB envelopes may remain generic.
- There are no frontend binding warnings and no manually authored source paths.
- Initialization, pull and first local view are separate observable actions.
  Each has a loading animation, error state and stable action identity.

## Browser behavior

- Initial online sync completes and reports progress or a determinate terminal
  state. It advances after active 100/100.
- A real local view completes between pull and catalog navigation so first-use
  indexing is observable rather than simulated.
- The root catalog has at least one category, category navigation reaches at
  least one product, and detail matches the selected product.
- After synchronization, a Playwright browser context with network disabled can
  repeat one category view and one product get from local FullSync storage.
- Reload behavior is documented: offline reload is required only if the static
  application shell is already cached by the deployment/browser strategy.
- Desktop and 390 px mobile layouts have no horizontal document overflow.
- No uncaught browser exception, failed Svelte hydration or missing binding is
  present.
- Two launches in one persistent browser profile both reach `/store`; the
  second launch proves that existing PouchDB checkpoints do not stall startup.
- Every breadcrumb segment is individually actionable, appears before the
  current grid and survives product detail/back round-trips.
- The light mobile shell has the persistent store mark and correct state title.
  Catalog cards emphasize image and name; product grid cards do not expose
  detail-only packaging/price fields.
- Product detail exposes `PRODUCT`, a back-icon plus `RAYONS` action, image,
  name, currency price, bounded quantity, computed quantity/price total and
  `AJOUTER AU PANIER`.

## Reproducibility metrics

- Count all MCP calls, failed calls, corrective mutations and project-local
  blocks.
- Measure time to bootstrap, scaffold dry-run/apply, first seed, first sync,
  first local view, first local get, first useful rendering and final
  validation. Record both elapsed wall time and observed active segment time.
- Timestamp every Codex JSONL event. Pair tool start/completion events to report
  per-tool total, median, p95 and maximum duration.
- Classify transport failures, structured MCP/domain failures, command failures,
  model-capacity interruptions, retries and recovered failures. Preserve the
  top repeated error signatures as campaign hard points.
- Preserve the exact prompt, result JSON, screenshots and Playwright script.
- Keep the target project and all previous RetailStore projects hidden from the
  fresh agent.
