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
