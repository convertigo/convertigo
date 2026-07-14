# RetailStore Flow campaign

This directory defines a reproducible fresh-agent benchmark for a Flow-native,
offline-capable retail catalog. It deliberately separates inputs from the
legacy reference implementation.

## Contents

- `retailstore-fresh-agent-prompt.md`: prompt given to the blank agent.
- `acceptance.md`: functional, authoring and runtime acceptance criteria.
- `campaign-protocol.md`: isolation, fixture staging and evidence procedure.
- `result-template.json`: result record to complete during the run.
- `fixtures/manifest.json`: public contract of the supplied seed data/assets.
- `fixtures/categories_42.xml` and `fixtures/products_42.xml`: input data.
- `fixtures/category.png` and `fixtures/product.png`: neutral fallback images.

The prompt and acceptance criteria do not disclose connector, transaction,
design-document or view names, CouchDB map/reduce code, query keys, the legacy
NGX component tree, or legacy Smart Source expressions.

## Campaign boundary

The target project must be absent before preparation. The harness creates an
empty project shell and places the fixture files under
`resources/retailstore/`, but it must not create any FullSync DBO, application
Flow, page implementation or generated frontend before the agent starts.

The agent starts from that prepared shell and must use
`flow-fullsync-scaffold(dryRun:true)` followed by the matching apply request.
It may learn safe read transaction schemas with `flow-requestable-schema`.
FullSync client behavior must use the public frontend blocks and structured
bindings; raw PouchDB calls, hand-written `fs://` calls and DBO YAML edits
invalidate the run.

## Evidence

Record one JSON result under `results/` with:

- exact Git commits for Convertigo, Flow engine, Flow MCP and frontbuilder;
- agent duration and MCP call/failure counts;
- first successful seed, sync, local view, local get and useful rendering;
- scaffold dry-run and idempotence results;
- learned schema sources and retained structured bindings;
- online and post-sync offline Playwright assertions;
- desktop/mobile overflow and browser-console results;
- remaining mocks and tooling gaps.
