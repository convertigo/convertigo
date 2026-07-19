# RetailStore Flow fresh-agent prompt

Campaign target: `sample_RetailStoreFlowRun9`. The harness created this blank
project and exposed no previous RetailStore Flow implementation.

Create a complete Convertigo Flow backend and Svelte frontend in the target
project named by the campaign harness.

The supplied fixture directory contains two XML datasets for one shop, two
fallback images and three progress animations. Build an idempotent server
initialization path that loads the datasets into a project-owned FullSync
database. The client is read-only: it must pull the database locally, browse
the catalog hierarchy, and show product details from the local database after
synchronization.

## Functional result

- First use shows three ordered automatic states: server initialization, a
  one-shot FullSync pull with visible progress, then local index preparation by
  executing and awaiting a real local catalog view. Use the supplied animation
  for each state, never leave synchronization active at 100/100, and remove the
  complete progress surface once the local index is ready.
- A catalog screen starts at the shop root with exactly 14 unique top-level
  departments and lets the user descend through categories until products are
  reached. Do not render the same category more than once when it has several
  fixture relationships.
- Category and product cards show a name and image when available. Keep the
  browsing cards compact: packaging and price belong to detail, not the product
  grid.
- A product detail view reads the selected local document and displays its
  name, unit price and image. Catalog and detail are distinct navigation
  states: no empty detail shell is visible in the catalog, and the catalog
  grid is not visible below an open detail.
- Selecting a product creates one browser-history entry. Browser Back and a
  visible Back action both restore the exact product grid and category path.
- The breadcrumb begins with `RAYONS`, renders every selected ancestor as its
  own action before the grid, and restores any earlier level without adding a
  history entry.
- A product can belong to more than one catalog category. Every relationship
  in the fixture must remain queryable; do not collapse a list of parent ids
  into one database key.
- The product detail quantity is bounded to at least one, changes through real
  client actions, and updates a currency-formatted total.
- After one successful synchronization, catalog browsing and product detail
  reads continue to work while browser network access is disabled.
- Empty, loading, progress and error states are visible and do not break the
  layout.
- Use a light application surface, a persistent cart/store mark and a compact
  top bar titled `SYNCHRONIZATION`, `OPTIMIZATION`, `STORE` or `PRODUCT`.
  Synchronization displays `Sync the database on the client.` and index
  preparation displays `Optimizing the database for the first time.`

Use the field and fixture contract from `fixtures/manifest.json`. The data
contains 768 category records and 3000 product records for shop `42`.

The strict acceptance path is `EPICERIE SUCREE` -> `GOUTERS & BISCUITS` ->
`BISCUITS AU CHOCOLAT`. It must render exactly 22 unique product cards. Open
`Biscuits Z'animo chocolat lait Cadbury` for the detail and return checks.

## Authoring constraints

Work autonomously until server initialization, FullSync, frontend bindings,
the production build and the real browser application are validated.

- Use only the installed Convertigo Flow skill, the `convertigo-flow` MCP
  server and Playwright for authoring/validation.
- The campaign harness has already bootstrapped the blank project with Svelte
  enabled and copied the eight declared fixtures under `resources/retailstore/`.
  The unchanged fixtures are also available to typed Flow resource blocks
  under `libs/flow/resources/retailstore/`; the JSON contract is readable
  through MCP there. Do not recreate or replace that shell.
- Provision the FullSync connector, design documents, views and standard
  transactions with `flow-fullsync-scaffold`. Run and inspect `dryRun:true`
  before applying the identical structured request.
- Use the public `FullSyncGet`, `FullSyncView` and `FullSyncSync` frontend
  blocks. Because this application is read-only, configure synchronization as
  a terminal one-shot pull. Treat results as explicit `fullsync` binding
  sources.
- Learn safe server read transaction schemas with
  the exact `schemaPending` mutation returned by `flow-app-progress`. Its
  `frontend-svelte-fullsync-schema` operation must target a safe read
  transaction and attaches the learned schema without copied schema JSON.
- Use structured `FlowValueBinding` values returned by picker/progress tools.
  Do not construct source descriptors or string paths by hand.
- Keep dataset parsing and initialization visible as Flow composition. Use
  catalogued typed blocks; report a missing primitive instead of inventing
  hidden filesystem or XML code.
- Finish with no mock blocks.
- Treat `flow-app-progress` as a guide, not proof of completion. Resolve every
  warning and verify that each data-bound iterator has visible content.
- Use Playwright MCP only to validate the rendered browser application. The
  campaign harness runs the standalone reproducible acceptance after authoring;
  do not create or execute scripts through shell, browser unsafe evaluation or
  filesystem access.
- In browser validation, use the strict acceptance labels above. Assert the root count,
  uniqueness, the 22-product count, mutually exclusive catalog/detail states,
  every breadcrumb segment, browser Back and visible Back. Do not choose an
  arbitrary first/last card or infer success from merely finding some data.
- Assert that every visible catalog and detail image has `naturalWidth > 0`.
  Use portable `resources/...` project paths and `Image.fallbackSrc` for
  fixture-backed fallbacks; a rendered but broken `img` is a failed result.
- Run the final acceptance twice with the same persistent browser profile. The
  second launch must reach the catalog and must not remain at active 100/100.
- Report the observed synchronization completion, first local view, first
  useful root render, first local get/detail and second persistent launch. The
  independent campaign harness records machine-readable browser timings.
- Treat every warning returned by `flow-fullsync-scaffold` as blocking. Use
  `Status.actionId`, `UpdateList`, `UpdateNumber`, bindable Button labels and
  formatted Text from the palette when their generic behavior matches the
  requirement; do not hand-code equivalent Svelte state.

## Fresh-context restrictions

- Do not inspect, list, read, copy or call the legacy RetailStore project or
  any prior RetailStoreFlow attempt.
- Do not inspect local repositories, prior session history, generated files or
  Convertigo DBO YAML.
- Do not use shell commands, `browser_run_code_unsafe`, `fs`, `process`,
  `child_process` or any other filesystem/process escape. A missing Flow MCP
  operation is a tooling gap to report.
- Do not use the legacy Convertigo MCP, direct CouchDB APIs, raw PouchDB code,
  hand-written SDK request strings or filesystem YAML edits.
- Do not modify shared Flow libraries, the frontbuilder or the Java engine.
  Report a tooling gap instead.
- Do not hard-code the fixture records, database query output or a static
  catalog fallback.

In the final report, state the first successful server seed, first client sync,
first local view/get, first useful browser rendering, final online/offline
validation, scaffold idempotence, schema-backed bindings used, remaining mocks
and tooling gaps. For every repeated failure, classify it as an agent mistake,
Flow tooling gap, infrastructure failure or model interruption, and state the
recovery that eventually worked.
