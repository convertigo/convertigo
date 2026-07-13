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

Artifacts:

- Baseline: `results/flow-*-20260713T114000Z.*`
- P1 compiled-plan cache: `results/flow-*-20260713T115000Z.*`
- P2 prepared standard `list.map`: `results/flow-*-20260713T124500Z.*`
- P3 hot block catalog: `results/flow-*-20260713T131500Z.*`
- P4 cached engine configuration: `results/flow-*-20260713T134500Z.*`
- P5 single result serialization: `results/flow-*-20260713T141500Z.*`
- Deterministic P0-P5 replay: `results/saved-local-p*-20260713T1430*.{csv,json}`

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
