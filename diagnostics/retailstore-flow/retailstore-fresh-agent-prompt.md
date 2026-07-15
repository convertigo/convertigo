# RetailStore Flow fresh-agent prompt

Create a complete Convertigo Flow backend and Svelte frontend in the target
project named by the campaign harness.

The supplied fixture directory contains two XML datasets for one shop and two
fallback images. Build an idempotent server initialization path that loads the
datasets into a project-owned FullSync database. The client is read-only: it
must synchronize the database locally, browse the catalog hierarchy, and show
product details from the local database after synchronization.

## Functional result

- A first-use screen initializes or verifies the server dataset, then performs
  a pull or bidirectional synchronization with visible progress and errors.
- A catalog screen starts at the shop root and lets the user descend through
  categories until products are reached.
- Category rows/cards show a name and image when available.
- Product rows/cards show a name, packaging, unit price and image when
  available.
- A product detail view reads the selected local document and displays its
  name, packaging, unit price and image.
- Back navigation preserves an understandable hierarchy.
- After one successful synchronization, catalog browsing and product detail
  reads continue to work while browser network access is disabled.
- Empty, loading, progress and error states are visible and do not break the
  layout.

Use the field and fixture contract from `fixtures/manifest.json`. The data
contains 768 category records and 3000 product records for shop `42`.

## Authoring constraints

Work autonomously until server initialization, FullSync, frontend bindings,
the production build and the real browser application are validated.

- Use only the installed Convertigo Flow skill, the `convertigo-flow` MCP
  server and Playwright for authoring/validation.
- The campaign harness has already bootstrapped the blank project with Svelte
  enabled and copied the declared fixtures under `resources/retailstore/`.
  The unchanged fixtures are also available to typed Flow resource blocks
  under `libs/flow/resources/retailstore/`; the JSON contract is readable
  through MCP there. Do not recreate or replace that shell.
- Provision the FullSync connector, design documents, views and standard
  transactions with `flow-fullsync-scaffold`. Run and inspect `dryRun:true`
  before applying the identical structured request.
- Use the public `FullSyncGet`, `FullSyncView` and `FullSyncSync` frontend
  blocks. Treat their results as explicit `fullsync` binding sources.
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
- Keep a standalone Playwright script for the final online/offline assertions;
  do not rely exclusively on an interactive browser transport.

## Fresh-context restrictions

- Do not inspect, list, read, copy or call the legacy RetailStore project or
  any prior RetailStoreFlow attempt.
- Do not inspect local repositories, prior session history, generated files or
  Convertigo DBO YAML.
- Do not use the legacy Convertigo MCP, direct CouchDB APIs, raw PouchDB code,
  hand-written SDK request strings or filesystem YAML edits.
- Do not modify shared Flow libraries, the frontbuilder or the Java engine.
  Report a tooling gap instead.
- Do not hard-code the fixture records, database query output or a static
  catalog fallback.

In the final report, state the first successful server seed, first client sync,
first local view/get, first useful browser rendering, final online/offline
validation, scaffold idempotence, schema-backed bindings used, remaining mocks
and tooling gaps.
