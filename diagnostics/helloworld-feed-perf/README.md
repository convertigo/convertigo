# sample_HelloWorld GetFeed performance diagnosis

Date: 2026-07-13

## Result

The reported legacy-versus-Flow gap is not caused by downloading the response
body, and raw XML parsing is too small to explain it.

On the public beta URLs, where legacy and Flow return the exact same 7,218-byte
body, Flow is 164.3 ms slower at median. Nearly all elapsed time is TTFB, and
subtracting the TLS phase still leaves 135.5 ms more server-side time in Flow.

The imported exact Flow shows two secondary costs over an empty Flow when run
against a local RSS fixture: 28.2 ms for `http.get`, then 38.9 ms for the
specialized Rhino block. The same XML parse and extraction implemented directly
with JAXP takes 1.24 ms. The Rhino delta therefore mostly represents Flow block
dispatch, template/expression adaptation, Rhino/Java conversion and result
handling, not the XML parser itself.

Flow4 has an additional, implementation-specific problem: its `list.map`
projection costs 233.9 ms at median. That cost does not exist in the exact Flow,
which parses and projects in one specialized block.

## Engine optimization progress

The final comparison replays every engine checkpoint against the same saved
`sample_HelloWorldFlowRun4.ReadNasaFeed` requestable. During each run,
`engine.yaml` is temporarily pointed to the same 46,091-byte local RSS fixture
with 60 items, then restored byte for byte. Each row contains 30 measured HTTP
requests after 5 warmups. All versions returned the same 30,058-byte body with
SHA-256 `e8b71f7f84e8107d5908c19761b72908f38d79ddc4e63bf5f2e743979429e4aa`.

| Step | Change | Saved HTTP median | Change from previous | Change from baseline |
| --- | --- | ---: | ---: | ---: |
| P0 | Unmodified engine baseline | 131.2 ms | - | - |
| P1 | Cache the validated and expanded Flow execution plan by source revision and active block catalog | 98.4 ms | -32.9 ms (-25.1%) | -32.9 ms (-25.1%) |
| P2 | Preserve structured `list.map` as one standard block and prepare its projection once | 42.2 ms | -56.2 ms (-57.1%) | -89.0 ms (-67.8%) |
| P3 | Reuse the hot block catalog before recursive filesystem fingerprinting | 35.5 ms | -6.7 ms (-16.0%) | -95.8 ms (-73.0%) |
| P4 | Cache parsed `engine.yaml` definitions by file fingerprint | 29.6 ms | -5.8 ms (-16.4%) | -101.6 ms (-77.4%) |
| P5 | Avoid the redundant deep JSON snapshot of the final result | 20.4 ms | -9.2 ms (-31.0%) | -110.8 ms (-84.4%) |

P1 removes FlowScript parsing, validation, FlowScript-to-YAML serialization,
YAML parsing and graph expansion from warm executions. The cache is bounded to
256 plans, tied to the active block catalog object, reported by `cacheInfo`, and
cleared with the other runtime caches. The standalone Rhino smoke test verifies
that a repeated execution hits this cache.

P2 changes no application block and introduces no specialized RSS code. The
FlowScript compiler now preserves `list.map({ select: { ... } })` as one
standard `list.map` node instead of expanding every item into
`forEach`/`json.object`/`json.push`. The expression service prepares the
structured projector once per block execution and directly reads simple scope
paths for each item. The isolated map increment fell from 194.2 ms in P1 to
6.7 ms in P2 (-96.5%). The plan cache fingerprint also includes the compiler
modules, preventing a parser update from reusing a stale execution plan.

P3 keeps a per-project hot catalog head for runtime execution and revalidates
it at most once per second. Authoring and validation calls still perform an
immediate fingerprint check, block writes explicitly invalidate the catalog
and compiled plans, and raw external file changes are visible to execution
after the bounded revalidation interval. This removes the recursive directory
walk from nearly every request without making Studio block edits stale.

P4 retains the parsed project and engine configuration definitions in the
runtime cache. Each request performs only a file fingerprint check; disk read,
Jackson YAML parsing and JavaScript tree conversion occur after an actual file
change.

P5 returns the already validated result object to the engine response layer.
Runtime handles are still rejected before this point, and the response layer
still performs the final sanitization and serialization, so the removed
sanitize/stringify/parse cycle was a redundant deep copy rather than a safety
boundary.

### Cold-start optimization

The initial 4.784-second `GetFeed` after a local runtime restart contained two
independent compilation costs. First, input synchronization saw `_flow.outputs`
and loaded the complete project block catalog merely to establish that the Flow
declared no inputs. Commit `38e928d` now reads the top-level `_flow` object
directly and skips that fallback when metadata is present. This reduced the
pre-execution phase from 1.483 seconds to 144 ms.

Second, the exact project's FlowScript and project-local NASA block were still
cold. `sample_HelloWorld_flow.RuntimeWarmup` is an AutoStart Flow using only
standard Flow blocks: `flow.get` loads and compiles `GetFeed`, then
`nasa.imageFeedItems` parses one in-memory RSS item. It performs no external
I/O and does not execute `GetFeed`.

| Cold step after restart | First GetFeed | Prepare | Execute | Change |
| --- | ---: | ---: | ---: | ---: |
| No warmup | 4,784 ms | - | - | baseline |
| Generic engine warmup | 1,612-1,899 ms | - | - | -60% to -66% |
| Metadata fast path | 1,602 ms | 144 ms | 1,407 ms | preparation isolated |
| Project block warmup | 1,414 ms | 146 ms | 1,210 ms | -11.7% |
| Project FlowScript + block warmup | 635 ms | 159 ms | 398 ms | -55.1% |

The final cold result is 86.7% below the original restart result. The next two
calls took 72 and 64 ms. AutoStart remains asynchronous, so the benchmark waits
for both warmups to finish; process readiness alone does not guarantee a warm
Flow runtime. The remaining cold execution phase includes the first external
HTTP/TLS exchange and cannot be labelled as Flow compilation alone.

Artifact: `results/flow-cold-start-20260714.json`.

With the retained P5 engine, the exact imported
`sample_HelloWorld_flow.GetFeed` returns its expected 7,218-byte, 20-item body
from the same fixture in 19.0 ms median (30 requests after 8 warmups). The more
demanding standard-block Flow4 pipeline returns 60 sorted five-field items in
20.4 ms median. Earlier legacy server statistics attributed about 33 ms to
Convertigo transaction and sequence work after excluding the remote host, so
the optimized Flow execution envelope is now below that observed legacy work.

### Concurrency limit

The retained P5 engine still owns one mutable Rhino scope protected by a global
runtime lock. With the same local fixture, 8 concurrent clients and 80 measured
requests, median latency is 255.6 ms, P95 is 448.2 ms and throughput is 28.0
requests/s. All responses remain identical.

A runtime-per-request-thread experiment was rejected because Convertigo creates
a fresh `RequestableThread` for each request, forcing a full engine startup on
every call. A bounded pool improved concurrent throughput, but added about 10 ms
to the hot sequential median and produced unstable c8 gains, so it was also
discarded rather than committed. Removing the concurrency lock safely requires
separating immutable compiled artifacts from request-local mutable scope; it is
not part of the retained P0-P5 series.

## Beta deployment validation

The retained P5 `lib_flow_engine` at commit `1f6958c` was deployed to
`beta.convertigo.net` on 2026-07-13. The server reported
`26198-develop-8.5.0-beta`, the same Convertigo build used by the local runtime.
No production Java file differs from `origin/develop`; all P1-P5 changes are in
the Flow JavaScript project.

The exact `sample_HelloWorld_flow` archive was not redeployed because its
extracted content was identical to the beta archive. The standard-block
`sample_HelloWorldFlowRun4` project was deployed because it was previously
absent. The first post-deployment requests took 13-16 seconds while the projects
were loaded and compiled; these cold deployment starts are excluded below.

The before and confirmation runs alternate legacy and exact Flow calls. Every
measured response returned HTTP 200, 7,218 bytes and SHA-256
`c2dd0fd76f9b1ab622c372d0b7639404a36baa41b9a4228edb4cc8d2d07eae57`.

| Beta run | Samples per target | Legacy median | Exact Flow median | Flow minus legacy |
| --- | ---: | ---: | ---: | ---: |
| Before P5 deployment | 12 | 465.7 ms | 916.3 ms | +450.6 ms (+96.8%) |
| P5 confirmation | 30 | 277.3 ms | 258.5 ms | -18.8 ms (-6.8%) |

The optimized exact Flow therefore meets the median latency target on the
deployed beta runtime in the confirmation series. External NASA latency remains
variable: P95 was 886.5 ms for Flow and 505.6 ms for legacy in that run, while
legacy had the larger maximum (1,787.6 ms versus 972.4 ms). This tail variation
cannot be attributed solely to Flow execution without server-side upstream
timing, and it does not invalidate the deterministic local P0-P5 phase gains.

Flow4 was also validated after deployment: 20/20 HTTP responses succeeded,
each containing 60 five-field items and the expected 30,058-byte body. Its
337.8 ms median is recorded as an operational check, not as a direct comparison
with the 20-item three-field legacy and exact Flow outputs.

Beta deployment artifacts:

- Before deployment: `results/beta-benchmark-20260713T123200Z-p0.csv` and
  `results/beta-summary-20260713T123200Z-p0.csv`
- P5 confirmation: `results/beta-benchmark-20260713T123700Z-p5.csv` and
  `results/beta-summary-20260713T123700Z-p5.csv`
- Three deployed requestables: `results/feed-benchmark-20260713T123512Z.csv`

## Frontend SDK startup

Playwright captures with fresh browser contexts showed that the Angular Hello
World initializes its Convertigo connection before user interaction. It sent
`POST /convertigo/services/user.Get` 241 ms after navigation. The generated
Svelte applications previously sent no SDK request during their first five
seconds; the first backend button paid for `env.json`, `user.Get` and the
requestable call in sequence.

The Svelte frontbuilder now starts SDK initialization from the root runtime,
caches one initialization promise per resolved endpoint and caches environment
loading per project. A backend action started during initialization awaits the
same promise. With `user.Get` artificially delayed by 700 ms, an immediate
button click produced one identity request and GetFeed started only after that
request completed.

Both `sample_HelloWorld_flow` and `sample_HelloWorldFlowRun4` were regenerated,
checked, built and deployed to beta with `lib_flow_frontbuilder_svelte` commit
`dda8976`. Fresh beta pages completed `env.json` and `user.Get` before the
button click and emitted no duplicate initialization. The first backend calls
immediately after project deployment took 5.8-7.1 seconds because the server
projects were cold; this is separate from the client bootstrap behavior.

Artifact: `results/frontend-sdk-bootstrap-20260713.json`.

## Frontend structured bindings

The first SmartType-style Flow binding slice is implemented without a Java
engine change. The core Flow catalog now exposes a `binding` object type backed
by a `flow-binding-editor` web component. The editor composes the existing
literal, path and expression editors and currently limits selectable source
categories to requestable/action results and lexical iterations.

The Svelte frontbuilder preserves the binding as a structured descriptor with
an explicit action or scope id and property/index path segments. Generated code
resolves those segments without parsing a dotted string. Existing string paths
remain supported for compatibility. The `CallSequence.requestable` property
continues to use the dedicated requestable picker; response consumers use the
new binding editor.

`sample_HelloWorld_flow` demonstrates the vertical slice: `ForEach` binds
`getFeed -> news`, while its `Image` and `Text` children bind the stable
`feedItems` iteration scope to `imageUrl`, `title` and `description`. Local and
beta Playwright runs clicked the real button, observed the `GetFeed` POST and
rendered all 20 feed images with no page error.

Checkpoints are `3e7688f` in `lib_flow_engine` and `040e50a` in the Svelte
frontbuilder. The engine commit is published on `origin/main`; the local
frontbuilder repository has no configured remote. Beta backups are under
`/home/nicolas/Téléchargements/convertigo-beta-backups/20260713T153504Z-flow-binding`.

Artifact: `results/frontend-structured-binding-20260713.json`.

Artifacts:

- Baseline: `results/flow-*-20260713T114000Z.*`
- P1 compiled-plan cache: `results/flow-*-20260713T115000Z.*`
- P2 prepared standard `list.map`: `results/flow-*-20260713T124500Z.*`
- P3 hot block catalog: `results/flow-*-20260713T131500Z.*`
- P4 cached engine configuration: `results/flow-*-20260713T134500Z.*`
- P5 single result serialization: `results/flow-*-20260713T141500Z.*`
- Deterministic P0-P5 replay: `results/saved-local-p*-20260713T1430*.{csv,json}`
- P5 concurrency baseline: `results/saved-concurrent-p5-c8-20260713T150500Z.json`
- Exact imported Flow on P5: `results/saved-local-p5-exact-20260713T170000Z.{csv,json}`

## Three implementations

| Version | HTTP and XML pipeline | Output work |
| --- | --- | --- |
| Legacy `sample_HelloWorld.GetFeed` | `XmlHttpTransaction` fetches and parses XML into the Convertigo document | Sequence `Iterator` selects `rss/channel/item`; XPath-backed XML steps emit `title`, `description`, `imageUrl` |
| Exact Flow `sample_HelloWorld_flow.GetFeed` | `http.get`, then `nasa.imageFeedItems`; the Rhino block uses JAXP DOM directly | One pass over `<item>`, extracts the same three fields, stops at 20 |
| Flow4 `sample_HelloWorldFlowRun4.ReadNasaFeed` | `http.get`, generic `xml.parse`, then `list.sort` by title | `list.map` evaluates five expressions per item and emits 60 items in the captured fixture |

The exact Flow source came from
`/home/nicolas/Téléchargements/sample_HelloWorld_flow.car`. It was deployed as
`sample_HelloWorld_flow`, inspected through Flow MCP, executed successfully, and
left unchanged after the temporary benchmark variants were discarded. Its
saved FlowScript SHA-256 is
`ee2e5c7cb531adcedf06ba73c8a1049bea9674856fea137f944c131870ab7bca`.

## Comparable beta timing

URLs:

- Legacy: `https://beta.convertigo.net/convertigo/projects/sample_HelloWorld/.json?__sequence=GetFeed`
- Flow: `https://beta.convertigo.net/convertigo/projects/sample_HelloWorld_flow/.json?__sequence=GetFeed`

The benchmark alternated call order and reused one session. All 24 measured
requests returned HTTP 200 and the same body SHA-256:
`c2dd0fd76f9b1ab622c372d0b7639404a36baa41b9a4228edb4cc8d2d07eae57`.

| Target | Connect | TLS | TTFB | Total | Server after TLS |
| --- | ---: | ---: | ---: | ---: | ---: |
| Legacy | 38.1 ms | 86.6 ms | 277.3 ms | 277.5 ms | 185.7 ms |
| Exact Flow | 44.0 ms | 95.4 ms | 441.5 ms | 441.8 ms | 321.2 ms |

Flow is 1.59x slower. Download after first byte is only 0.2-0.3 ms for either
version, so neither response transfer nor client networking explains the gap.

Artifacts:

- `results/beta-benchmark-20260712T214008Z.csv`
- `results/beta-summary-20260712T214008Z.csv`

## Exact Flow phase isolation

`bench_imported_flow_exact.py` temporarily replaces the working copy with
staged variants, runs them through `code-run`, and always calls `code-discard`.
All variants read the same 46,150-byte fixture over the Docker bridge.

Run: 20 measured iterations after 5 warmups.

| Variant | Median | Increment |
| --- | ---: | ---: |
| Empty Flow | 184.0 ms | baseline |
| Local fixture `http.get` | 212.3 ms | +28.2 ms |
| HTTP + `nasa.imageFeedItems`, count only | 251.1 ms | +38.9 ms |
| Full 20-item result | 247.5 ms | +63.4 ms over empty |

The negative difference between count-only and full-result variants is normal
measurement noise in the MCP `code-run` path and must not be interpreted as a
serialization saving.

The direct Java microbenchmark mirrors the specialized block's parser features
and extraction logic:

| Exact block phase | Median |
| --- | ---: |
| JAXP parse | 0.976 ms |
| Extract 20 items | 0.215 ms |
| Total | 1.240 ms |

Thus only about 1.2 ms of the 38.9 ms block increment is the XML algorithm
itself. The rest is the runtime boundary around that algorithm.

Artifacts:

- `results/exact-flow-variant-20260713T091500Z.csv`
- `results/exact-flow-variant-summary-20260713T091500Z.csv`
- `results/exact-flow-derived-20260713T091500Z.json`
- `results/xml-pipeline-exact-20260713T091500Z.csv`

## Flow4 phase isolation

The same staged method was run on Flow4 with a 46,091-byte local fixture and 60
RSS items.

| Variant | Median | Increment |
| --- | ---: | ---: |
| Empty Flow | 191.6 ms | baseline |
| Local fixture `http.get` | 226.2 ms | +34.6 ms |
| HTTP + `xml.parse` | 258.8 ms | +32.6 ms |
| HTTP + XML + `list.sort` | 268.6 ms | +9.8 ms |
| HTTP + XML + sort + `list.map` | 502.6 ms | +233.9 ms |
| Full 60-item result | 475.4 ms | +283.8 ms over empty |

This confirms that Flow4 is slow for a different reason than the exact Flow:
the repeated expression projection in `list.map` dominates its useful work.
The saved HTTP requestable itself had 168.9 ms median total time; the larger
`code-run` values include the MCP execution path and are useful for deltas, not
as end-user request latency.

Artifacts:

- `results/flow-variant-20260713T092000Z.csv`
- `results/flow-variant-summary-20260713T092000Z.csv`
- `results/flow-variant-derived-20260713T092000Z.json`
- `results/flow-http-20260713T092000Z.csv`
- `results/flow-http-summary-20260713T092000Z.json`

## Native local timing

The three saved requestables were also measured over local HTTP, 20 times each:

| Requestable | Median total | Response size |
| --- | ---: | ---: |
| Exact Flow `GetFeed` | 79.6 ms | 7,218 bytes |
| Flow4 `ReadNasaFeed` | 160.4 ms | 30,058 bytes |
| Legacy `GetFeed` | 267.2 ms | 16,463 bytes |

These numbers are deliberately not an apples-to-apples performance verdict.
The marketplace legacy project calls `https://apod.com/feed.rss`, Flow4 calls
NASA IOTD and returns 60 sorted five-field rows, while the exact Flow follows a
NASA redirect and returns 20 three-field rows. In the same run, direct upstream
medians were 237.0 ms for APOD, 92.2 ms for NASA IOTD and 108.4 ms for the exact
Flow's configured NASA URL. The different upstreams dominate the local ranking.

Artifact: `results/feed-benchmark-20260713T091314Z.csv`.

## Legacy breakdown

On the earlier local server-stat run, the legacy transaction median was:

| Phase | Median |
| --- | ---: |
| Remote Host | 218 ms |
| Convertigo transaction work | 20 ms |
| Transaction total | 238 ms |
| Sequence total | 251 ms |

For this local marketplace version, legacy time is overwhelmingly its APOD HTTP
call. The sequence's XML iteration/projection adds little compared with the
remote host. This does not contradict beta: beta used equivalent response
bodies and isolates the extra delay to Flow server execution.

## Schema-backed frontend picker

The requestable and lexical iteration binding slice now uses the same schema
principle as NGX and Flow Code. `lib_flow_engine` resolves the selected
requestable through `requestable.schema`, attaches typed paths to frontend
binding candidates, and derives a `ForEach` item schema from the selected array
schema. The Svelte frontbuilder only consumes these descriptors; it does not
parse XSD or Flow source itself.

On the exact Hello World authoring tree, `appTitle` sees only the preceding
`getFeed` requestable. `feedItemTitleEven` sees both `getFeed` and the lexical
`feedItems` scope. The requestable exposes `news: array` and its typed item
fields; the iteration exposes `title`, `description` and `imageUrl` as strings.
The frontbuilder now clones property definitions per node so these lexical
sources cannot leak to nodes outside the loop.

Checkpoints are `2cc3b79` in `lib_flow_engine` and `b517907` in the private
Svelte frontbuilder repository. Both are pushed. Their runtime workspaces match
all tracked files (`missing=0`, `changed=0`). The stale duplicate frontbuilder
was moved to
`/home/nicolas/Téléchargements/convertigo-workspace-backups/20260713T192204Z-frontbuilder-reconcile`
before removal, and only `lib_flow_frontbuilder_svelte` remains loaded.

The rebuilt Hello World passes `svelte-check` with no diagnostics and its
production build uses a portable sibling-project SDK path. Beta backups are in
`/home/nicolas/Téléchargements/convertigo-beta-backups/20260713T194435Z-schema-picker`;
the engine, frontbuilder and sample were redeployed successfully. The beta page
and GetFeed endpoint return HTTP 200 with 20 items. Beta cannot currently build
the Studio authoring projection because its runtime image has no `npm`
executable; the schema-backed tree itself is validated on the local Convertigo
runtime. Adding an incomplete parser fallback would lose descriptor semantics,
so this environment limitation is left explicit.

Artifact: `results/frontend-schema-picker-20260713.json`.

## Flow runtime AutoStart warmup

`lib_flow_engine.Warmup` is a deterministic Flow with `autoStart: true`. It
filters, sorts and maps three in-memory objects and returns `ready: true`. It
contains no HTTP, requestable, filesystem or other external-I/O block. Because
`Flow` extends `Sequence`, Convertigo runs it through the standard AutoStart
mechanism after all projects are loaded and after deployment.

The warmup initializes the shared `lib_flow_engine.Engine` Rhino runtime,
engine modules, core block catalogue and representative expression caches. It
does not compile the project-specific GetFeed plan and does not establish the
NASA connection. AutoStart is asynchronous, so requests arriving while the
warmup still owns the shared runtime lock can wait longer; readiness must not
be inferred from the container health check alone.

A controlled local comparison restarted the same container and waited the same
amount of time after project migration. With AutoStart disabled, the first
GetFeed took 4,784 ms. With the warmup completed, two restart runs measured
1,612 ms and 1,898 ms for the first GetFeed, followed by 63-106 ms warm calls.
Using the 1,755 ms mean of the two warmed first calls, the warmup removes about
3,029 ms, or 63%, from the first request. The remaining 1.6-1.9 s is consistent
with the project-specific plan/custom block and first outbound HTTP connection.

The warmup itself completed 4.68-4.86 s after starting. In an intentionally
racing run, GetFeed started before completion and took 2,453 ms, confirming
that AutoStart shifts work to startup but does not provide a readiness barrier.
After deployment on beta and a six-second delay, the first GetFeed still took
4,974 ms and the second 449 ms. Beta therefore needs either a completion-aware
readiness gate or per-pod warmup verification; deployment success alone does
not prove that the runtime serving the next request is warm.

Artifact: `results/flow-autostart-warmup-20260714.json`.

## Conclusion

For the exact legacy-versus-Flow comparison, the primary regression is the Flow
server execution envelope before the first byte. HTTP response transfer is
negligible. Raw XML parsing is around 1 ms and is not the cause. The exact
Flow's specialized Rhino block contributes about 39 ms including its runtime
boundary, while the remaining beta delta is consistent with fixed Flow
requestable/bridge orchestration and result handling.

Flow4 should be treated separately: its generic XML parse is moderate, but its
`list.map` expression projection adds about 234 ms and makes it roughly twice as
expensive as the specialized exact Flow in the staged `code-run` benchmark.

## Reproduction

```bash
ITERATIONS=20 WARMUP=3 diagnostics/helloworld-feed-perf/bench_feed.sh

ITERATIONS=20 WARMUP=5 diagnostics/helloworld-feed-perf/bench_imported_flow_exact.py

STAMP=20260713T092000Z ITERATIONS=20 WARMUP=5 \
  diagnostics/helloworld-feed-perf/bench_flow_phases.py

javac diagnostics/helloworld-feed-perf/XmlPipelineBench.java
ITERATIONS=200 WARMUP=20 java -cp diagnostics/helloworld-feed-perf \
  XmlPipelineBench \
  diagnostics/helloworld-feed-perf/results/nasa-lg-image-20260713T091500Z.rss
```
