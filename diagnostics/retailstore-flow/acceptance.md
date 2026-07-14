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

- Replication uses `FullSyncSync` with progress observable through generated
  runtime state.
- Hierarchy reads use `FullSyncView`; detail reads use `FullSyncGet`.
- Action parameters are literals or structured source bindings. A selected item
  can feed a later FullSync action without string interpolation.
- FullSync results appear as `category: fullsync` picker sources with stable
  action identity and operation.
- Domain fields bound in the UI come from a learned requestable schema when
  available. Generic CouchDB envelopes may remain generic.
- There are no frontend binding warnings and no manually authored source paths.

## Browser behavior

- Initial online sync completes and reports progress or a determinate terminal
  state.
- The root catalog has at least one category, category navigation reaches at
  least one product, and detail matches the selected product.
- After synchronization, a Playwright browser context with network disabled can
  repeat one category view and one product get from local FullSync storage.
- Reload behavior is documented: offline reload is required only if the static
  application shell is already cached by the deployment/browser strategy.
- Desktop and 390 px mobile layouts have no horizontal document overflow.
- No uncaught browser exception, failed Svelte hydration or missing binding is
  present.

## Reproducibility metrics

- Count all MCP calls, failed calls, corrective mutations and project-local
  blocks.
- Measure time to bootstrap, scaffold apply, first seed, first sync, first local
  view, first local get, first useful rendering and final validation.
- Preserve the exact prompt, result JSON, screenshots and Playwright script.
- Keep the target project and all previous RetailStore projects hidden from the
  fresh agent.

