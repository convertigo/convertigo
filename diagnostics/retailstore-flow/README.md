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
- `fixtures/init.gif`, `fixtures/sync.gif` and `fixtures/optimize.gif`: declared
  progress animations used by the black-box UX contract.
- `campaign-trace.mjs`: timestamps Codex JSONL streams and summarizes tool
  latency, retries, failures, interruptions and repeated hard points.
- `run-campaign-segment.sh`: captures one initial or resumed agent segment while
  preserving its exit status and active duration.

The prompt and acceptance criteria do not disclose connector, transaction,
design-document or view names, CouchDB map/reduce code, query keys, the legacy
NGX component tree, or legacy Smart Source expressions.

## Campaign boundary

The target project must be absent before preparation. The harness creates an
empty project shell and places the fixture files under
`resources/retailstore/`. It mirrors the unchanged fixtures under
`libs/flow/resources/retailstore/` for typed Flow resource access, but it must
not create any FullSync DBO, application Flow, page implementation or generated
frontend before the agent starts.

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

`results/run5-authoring-retrospective.json` is the untimed baseline extracted
from the old Run5 traces. Run6 must improve on its call/failure counts and, with
the new stamped traces, add comparable wall, active and per-tool durations.
