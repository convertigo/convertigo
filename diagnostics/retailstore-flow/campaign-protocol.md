# Campaign protocol

For Run5 and later, the observable navigation contract is
[`legacy-ux-spec.md`](legacy-ux-spec.md). Give that contract to the fresh agent
without exposing the legacy project or previous runs.

## Prepare outside the fresh context

1. Record the Convertigo, Flow engine, Flow MCP and Svelte frontbuilder commit
   ids in a copy of `result-template.json`.
2. Confirm that the target project name and every previous RetailStoreFlow
   project are absent from the fresh agent's visible project list.
3. Bootstrap only the target project shell with Svelte enabled.
4. Copy the eight declared fixture files into
   `resources/retailstore/` unchanged and verify their manifest hashes. Also
   mirror the eight unchanged files under `libs/flow/resources/retailstore/`,
   because typed Flow resource blocks intentionally read from that project
   resource root. The mirror is fixture input, not an implementation.
5. Confirm that the target has no FullSync connector, design document,
   application Flow implementation or generated frontend artifact.
6. Confirm that the runtime FullSync store is reachable and persistent before
   the timed run. This is infrastructure readiness, not application setup.
7. Start a new agent context with only the fresh-agent prompt, target project
   name, fixture contract and supported Flow MCP/Playwright tools.

The preparation phase must not choose connector, design-document, view,
transaction, Flow or component names. It must not precompute CouchDB documents
or expose the legacy project.

## Capture during authoring

Pass every `codex exec --json` segment through `campaign-trace.mjs stamp` so
each event receives a wall-clock timestamp, monotonic elapsed time, run id and
segment id. Preserve one trace per initial invocation or resume; never append an
unstamped trace to a timed campaign. Count calls, failures and corrective
mutations. Save the first successful scaffold dry-run and the second idempotent
apply result. Record the first successful seed, sync, local view, local get and
useful browser render.

Use distinct segment names for preparation, authoring, each resume, independent
validation and post-run correction. A model-capacity interruption ends the
current segment; do not count the gap before resume as active agent time. The
analyzer reports both complete wall time and the sum of observed segment times.

Use the checked-in wrapper so the upstream process exit code is retained:

```bash
CAMPAIGN_STDIN_FILE=.run6-agent/PROMPT.md \
  ./diagnostics/retailstore-flow/run-campaign-segment.sh \
  run6 authoring diagnostics/retailstore-flow/results/run6-authoring.jsonl -- \
  env CODEX_HOME="$PWD/diagnostics/retailstore-flow/.run6-codex-home" \
  codex exec --json -C "$PWD/diagnostics/retailstore-flow/.run6-agent"
```

Use a new segment file for every resume. Analyze all segments together:

```bash
node diagnostics/retailstore-flow/campaign-trace.mjs analyze \
  diagnostics/retailstore-flow/results/run6-*.jsonl \
  > diagnostics/retailstore-flow/results/run6-trace-summary.json
```

Schema learning is explicit. Preserve the `schemaPending` operation emitted by
`flow-app-progress` and the corresponding
`frontend-svelte-fullsync-schema` result. A copied or manually reconstructed
schema does not satisfy the campaign.

`flow-app-progress` reaching 100% is necessary but not sufficient. Every
binding warning must be resolved, and a data-bound iterator must contain a
visible child before browser validation starts.

The scaffold dry-run must return no unresolved warning. In particular, fixture
relations may be scalar or multi-valued; the authored views must preserve every
relation rather than coercing a list into one key. This is verified from
observable branch counts, not by giving the agent view code.

## Validate the final application

Run the production generate/build actions and `svelte-check`. In Playwright,
capture one online synchronization and browse from the root to a product
detail. Disable browser network access without replacing the browser context,
then repeat one local view and one local get. Check desktop and 390 px layouts,
console errors and horizontal overflow.

Use a standalone Playwright script against the fixed production URL so browser
validation remains reproducible if an interactive browser transport closes.
Assert rendered category and product data, not only page availability or an
empty application shell.

The standalone script emits `CAMPAIGN_BROWSER_METRICS=<json>` once. Timings are
measured from its first navigation and include synchronization completion,
first local view, first useful root render, first local get/detail and second
persistent launch. Treat these as browser milestones; do not substitute MCP
authoring timestamps for runtime milestones.

The strict acceptance branch must contain exactly 22 products. Capture the
visible initialization, synchronization and indexing stages, every breadcrumb
state, the product quantity and computed total, browser Back, breadcrumb Back
and the repeated offline branch. Repeat startup in the same persistent browser
profile and reject any run that remains on active 100/100.

After the agent stops, run `campaign-trace.mjs analyze` over every segment. Add
the generated summary to the result record before inspecting the legacy
reference. The hard-point review must distinguish a product/tooling gap from an
agent mistake and an infrastructure failure; only reproducible generic product
gaps should lead to shared-library changes.

Complete the result record, retain the Playwright script/screenshots and leave
the target project available for review. Do not compare with the legacy project
until the fresh-agent run and metrics are closed.
