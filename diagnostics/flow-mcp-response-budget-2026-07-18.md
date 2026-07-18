# Flow MCP response budget - 2026-07-18

## Contract

Compatible read tools use a shared cooperative response budget automatically.
The current MCP defaults are:

- 1,000 ms spent in the compatible result loop;
- 64 KiB approximate result-item size;
- at least one useful item before interruption.

These controls are intentionally absent from tools/list. LLM clients normally
see only the domain query, limit and continuation cursor. Internal overrides
remain bounded for tests and operational diagnostics.

A budget stop returns `partial: true`, an opaque revision-bound `nextCursor`,
`PARTIAL_RESULT_TIME_BUDGET` or `PARTIAL_RESULT_SIZE_BUDGET`, and
`responseBudget` metrics. A partial result cannot prove absence.

The first compatible tools are `flow-catalog` and `flow-resource-search`.
Resource search checks the deadline before each file read. Catalog checks it
before each requested descriptor projection. Mutation tools are excluded.

## Local runtime evidence

Runtime: `c8o-agent-runtime`, endpoint `lib_flow_mcp.McpServer`.

Request: `flow-resource-search`, project `lib_flow_mcp`, query `flow`,
`timeoutMs: 1`, `minItems: 1`, `limit: 10`.

- engine budget elapsed: 7 ms;
- returned items: 1;
- response size: 1,420 bytes;
- warning: `PARTIAL_RESULT_TIME_BUDGET`;
- continuation resumed at the next file and returned the next 2 paths without
  duplicating the first result.

The equivalent unbudgeted 10-item response was 4,505 bytes and took 831 ms
wall-clock on a fresh local request. This is not a pure engine speed comparison
because each request included Convertigo/session startup; it demonstrates the
response-size and early-return behavior.

After a final runtime restart, the first budgeted request took 7,199 ms wall
clock while its response loop reported 16 ms. The cooperative budget does not
preempt project/runtime loading before a compatible loop starts. It bounds the
loop once control reaches it; a separate request deadline remains necessary for
hard end-to-end cancellation.

With the automatic policy and no budget arguments, a runtime
`flow-resource-search` returned its normal 10 results in 30 ms of loop time and
about 3.43 KiB of result items, without a partial warning. Its tools/list schema
exposed only `project`, `query`/`q`, `limit` and `cursor`. Budget metrics are
omitted from complete responses and retained on partial responses.

## Phased search and progress

`flow-search` now traverses deterministic phases (`sample`, `flow`, `node`,
`block`, `type`, `schema`) and stores phase, unit and item in its opaque cursor.
A no-match search over `lib_flow_mcp` stopped after 148 ms with zero results and
`partial:true`; continuation completed without restarting at the first Flow.

`flow-app-progress` keeps its existing complete fast path under a 3,000 ms
phase budget. On a forced 50 ms local test against
`sample_HelloWorldFlowRun11`, it returned a backend checkpoint after 105 ms,
then resumed through `frontend-structure` and `frontend`. Partial checkpoints
set `complete:false`; only the full assessment sets `complete:true`.

## Validation

- `tests/response-budget.js`: time, size, minimum item, cursor resume and cursor
  mismatch tests pass under Rhino.
- `lib_flow_engine/tests/smoke.js`: passes.
- `lib_flow_mcp/tests/smoke.js`: passes and verifies tools/list schemas.

## Fresh Hello World Run12

Run12 exercised the automatic policy without exposing tuning parameters to the
agent. The project was small enough that all four `flow-app-progress` calls
completed inside the default phase budget (`complete:true`, `partial:false`),
so no cursor continuation was needed. This confirms the normal-case behavior:
complete answers remain unchanged and carry no budget noise.

The agent reached its first generated frontend after 23 MCP calls, versus 27 in
Run11, and used no broad frontend-tree inspection. The total campaign was not
faster: 521.24 s and 38 MCP calls versus 377.78 s and 40 calls in Run11. It lost
time to two generic defects and never reached Playwright itself:

- generated applications required Node 20 because they pinned Vite 8, while
  the runtime provides Node 18;
- `flow-node-output-schema` could not inspect an unpromoted FlowScript working
  copy.

Both defects were fixed after the campaign. The frontbuilder now generates
Vite 6.4.3/plugin 5.1.1 applications and builds successfully under Node 18.
Schema tools now compile a working copy when one exists. Independent
Playwright validation then passed with 60 exact cards and loaded images on
desktop and 390 px mobile, no overflow, and no browser or network errors.

The durable result is
`results/frontend-fresh-agent-run12-20260718.json`. Run12 is a functional and
no-regression proof for transparent budgets, but not a clean timing acceptance
run. The next time-saving target is avoiding repeated complete progress audits
and repeated skill reads; timeout pagination cannot improve calls that already
finish below their budget.

## Run13 and accepted Run14

Run13 verified the two-audit workflow and the working-copy schema fix, but its
618.24 s duration was invalidated by Playwright MCP extension mode waiting for
a browser extension that was not connected. Independent Playwright validation
passed all 60 cards and images on desktop and mobile.

The skill was then tightened in two places: at most two relevant sample reads
before the first draft, and full revision-checked source instead of an invented
compact `@@` hunk for small configuration resources. Run14 exercised both:

- zero sample `code-get` calls before authoring;
- first configuration patch accepted;
- two `flow-app-progress` calls, both complete and unpaginated;
- one composed frontend binding mutation;
- production build successful;
- 31 terminal MCP calls in 299.72 s.

Compared with accepted Run11 (40 calls, 377.78 s), Run14 reduced MCP calls by
22.5% and wall time by 20.7%. It therefore meets the timing goal even though the
included Playwright launch failed quickly because the configured executable was
resolved to its parent directory. A post-run Playwright MCP smoke using
headless isolated `/usr/bin/google-chrome` reached the built application with
HTTP 200 and the expected title.

The strict independent DOM check found all 60 cards, exact response values,
30/30 dark/sky alternation and no desktop/mobile overflow. NASA image requests
were affected by the host network changing during the final retries; this is
recorded separately from application correctness. Finally, bootstrap gained an
explicit instruction not to call it twice after success.

Run14 is the accepted timing campaign. Its durable result is
`results/frontend-fresh-agent-run14-20260718.json`; the next complex campaign
can return to RetailStore.
